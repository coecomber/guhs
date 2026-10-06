package nl.juiced.guhs.feature.techmachine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * The Knutselmachine's inside. Slot {@link #TEKENING} holds a Bouwtekening, the nine slots {@link #VOORRAAD}..
 * {@link #VOORRAAD_TOT} the supplies (a pool: it does not matter which slot holds what), the three slots {@link #UIT}..
 * {@link #UIT_TOT} what it made. When the pool holds everything one craft of the drawing needs, {@link #TIJD} ticks of
 * work later the ingredients are gone and the result lies in the out slots, together with what crafting leaves behind
 * (empty buckets...). A leftover that is itself an ingredient (a recipe card) goes back into the pool.
 * <p>
 * The pool only takes what the drawing asks for, and of each ingredient at most {@link #BUFFER} crafts' worth (never more
 * than a stack; of things that do not stack exactly what one craft needs): however a pipe feeds it, one ingredient can
 * never crowd out another. The drawing is exact: it crafts with precisely the items that were drawn.
 */
public class KnutselmachineBlockEntity extends TechBlockEntity {
    public static final int TEKENING = 0, VOORRAAD = 1, VOORRAAD_TOT = 9, UIT = 10, UIT_TOT = 12;
    /** Ticks of work per craft. */
    public static final int TIJD = 40;
    /** How many crafts' worth of one ingredient the pool takes from a pipe. */
    public static final int BUFFER = 8;
    /** {@link #standCode}: fine / no drawing / something is missing / the out slots are full / the recipe is gone. */
    public static final int GOED = 0, GEEN_TEKENING = 1, MIST = 2, VOL = 3, ONBEKEND = 4;

    private boolean vol;
    /** The drawing's recipe no longer exists (until the drawing is swapped). */
    private boolean onbekend;

    public KnutselmachineBlockEntity(BlockPos pos, BlockState state) {
        super(TechmachineFeature.KNUTSELMACHINE_BE.get(), pos, state, VadsGetallen.KNUTSELMACHINE, 13);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.KNUTSELMACHINE;
    }

    @Override
    public int duur() {
        return TIJD;
    }

    /** The drawing in the machine (null: none). */
    @Nullable
    public Bouwtekening tekening() {
        return vakken().getAmountAsInt(TEKENING) > 0 ? Bouwtekeningen.lees(vakken().getResource(TEKENING).toStack()) : null;
    }

    /** Is this an ingredient of the drawing on that stack? (Also what the client's screen asks.) */
    public static boolean hoortBij(ItemStack tekening, ItemStack wat) {
        Bouwtekening t = Bouwtekeningen.lees(tekening);
        return t != null && t.nodig(wat) > 0;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        if (vak == TEKENING) {
            return Bouwtekeningen.lees(wat.toStack()) != null;
        }
        Bouwtekening t = tekening();
        return t != null && t.nodig(wat.toStack()) > 0;
    }

    /** How many of this item (item + components) the pool holds. */
    public int inVoorraad(ItemStack wat) {
        int n = 0;
        for (int i = VOORRAAD; i <= VOORRAAD_TOT; i++) {
            if (vakken().getAmountAsInt(i) > 0 && vakken().getResource(i).matches(wat)) {
                n += vakken().getAmountAsInt(i);
            }
        }
        return n;
    }

    /** How many of this ingredient the pool takes from a pipe at most. */
    public static int plafond(Bouwtekening t, ItemStack wat) {
        int nodig = t.nodig(wat), stapel = wat.getMaxStackSize();
        return stapel <= 1 ? nodig : Math.max(nodig, Math.min(nodig * BUFFER, stapel));
    }

    /** The first ingredient the pool has too few of (its count = how many are missing); empty when one craft is covered. */
    public ItemStack mist(Bouwtekening t) {
        for (ItemStack wat : t.ingredienten()) {
            int heeft = inVoorraad(wat);
            if (heeft < wat.getCount()) {
                return wat.copyWithCount(wat.getCount() - heeft);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean kanWerken() {
        Bouwtekening t = tekening();
        if (t == null || onbekend || !mist(t).isEmpty()) {
            voortgang = 0;
            return false;
        }
        return !vol;
    }

    @Override
    protected boolean isVol() {
        return vol;
    }

    @Override
    protected int standCode() {
        Bouwtekening t = tekening();
        return t == null ? GEEN_TEKENING : onbekend ? ONBEKEND : vol ? VOL : mist(t).isEmpty() ? GOED : MIST;
    }

    @Override
    protected void werk() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (voortgang % 10 == 0) {
            server.playSound(null, worldPosition, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.35f, 1.3f + server.getRandom().nextFloat() * 0.3f);
        }
        if (++voortgang < TIJD) {
            return;
        }
        Bouwtekening t = tekening();
        if (t == null) {
            voortgang = 0;
            return;
        }
        RecipeHolder<CraftingRecipe> recept = Bouwtekeningen.recept(server, t);
        if (recept == null) {
            onbekend = true;
            voortgang = 0;
            return;
        }
        if (knutsel(t, recept.value())) {
            voortgang = 0;
            server.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4f, 0.7f);
            beloon("geknutseld");
        } else {
            vol = true;   // (nothing was taken; tried again as soon as something is taken out)
            voortgang = TIJD - 1;
        }
    }

    /** One craft in one go: the ingredients out of the pool, the result and the leftovers in. False: no room, nothing changed. */
    private boolean knutsel(Bouwtekening t, CraftingRecipe recept) {
        CraftingInput invoer = t.invoer();
        ItemStack resultaat = recept.assemble(invoer);
        if (resultaat.isEmpty()) {
            return false;
        }
        ResourceHandler<ItemResource> voorraad = vakken(VOORRAAD, VOORRAAD_TOT), uit = vakken(UIT, UIT_TOT);
        try (Transaction tx = Transaction.openRoot()) {
            for (ItemStack wat : t.ingredienten()) {
                if (voorraad.extract(ItemResource.of(wat), wat.getCount(), tx) < wat.getCount()) {
                    return false;
                }
            }
            List<ItemStack> eruit = new ArrayList<>();
            eruit.add(resultaat);
            for (ItemStack rest : recept.getRemainingItems(invoer)) {
                if (rest.isEmpty()) {
                    continue;
                }
                // a leftover that is an ingredient again (a recipe card) stays in the machine
                eruit.add(t.nodig(rest) > 0 ? Kisten.stop(voorraad, rest) : rest);
            }
            for (ItemStack stack : eruit) {
                if (!Kisten.stop(uit, stack).isEmpty()) {
                    return false;
                }
            }
            tx.commit();
            return true;
        }
    }

    @Override
    protected void vakkenVeranderd() {
        vol = false;
        onbekend = false;
    }

    /**
     * What pipes get: like every machine (in only into the pool, out only of the out slots), but the pool takes of each
     * ingredient no more than {@link #plafond}.
     */
    @Override
    public ResourceHandler<ItemResource> handler(@Nullable Direction kant) {
        ResourceHandler<ItemResource> basis = super.handler(kant);
        return new ResourceHandler<>() {
            @Override
            public int size() {
                return basis.size();
            }

            @Override
            public ItemResource getResource(int index) {
                return basis.getResource(index);
            }

            @Override
            public long getAmountAsLong(int index) {
                return basis.getAmountAsLong(index);
            }

            @Override
            public long getCapacityAsLong(int index, ItemResource resource) {
                return basis.getCapacityAsLong(index, resource);
            }

            @Override
            public boolean isValid(int index, ItemResource resource) {
                return index != TEKENING && basis.isValid(index, resource);
            }

            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                Bouwtekening t = tekening();
                if (index < VOORRAAD || index > VOORRAAD_TOT || t == null || resource.isEmpty() || amount <= 0) {
                    return 0;   // (a pipe never swaps the drawing)
                }
                ItemStack wat = resource.toStack();
                int ruimte = plafond(t, wat) - inVoorraad(wat);
                return ruimte <= 0 ? 0 : basis.insert(index, resource, Math.min(amount, ruimte), transaction);
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return basis.extract(index, resource, amount, transaction);
            }
        };
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        Bouwtekening t = tekening();
        if (t == null) {
            regels.accept(Component.translatable("gui.guhs.techmachine.knutselmachine.geen_tekening").withStyle(ChatFormatting.GRAY));
            return;
        }
        regels.accept(Component.translatable("gui.guhs.techmachine.knutselmachine.maakt", t.resultaat().getCount(), t.resultaat().getHoverName())
                .withStyle(ChatFormatting.AQUA));
        ItemStack mist = mist(t);
        if (onbekend) {
            regels.accept(Component.translatable("gui.guhs.techmachine.knutselmachine.onbekend").withStyle(ChatFormatting.GOLD));
        } else if (vol) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        } else if (!mist.isEmpty()) {
            regels.accept(Component.translatable("gui.guhs.techmachine.knutselmachine.mist", mist.getCount(), mist.getHoverName()).withStyle(ChatFormatting.GOLD));
        }
    }
}
