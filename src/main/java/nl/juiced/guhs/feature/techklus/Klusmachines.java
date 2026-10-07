package nl.juiced.guhs.feature.techklus;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.guhoven.GuhOvenBlockEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.Voorraad;
import nl.juiced.guhs.feature.techmachine.Bouwtekening;
import nl.juiced.guhs.feature.techmachine.KnutselmachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * What the machines chore ({@link MachineKlus}) knows about a machine, all of it through the machine's item capability,
 * the way a hopper sees it: things go IN through the top ({@link #in}) and come OUT through the bottom ({@link #uit}). So
 * it works for every block of the tag {@code guhs:techklus/machines} without knowing its class: the Guh Oven (a furnace),
 * the Knutselmachine, the Brouw- and Frituurautomaat, the Vadsmolen, the Grillkoolpers, the Oogster, the Knabbelaar, the
 * Neerzetter, the Opzuiger (and whatever a datapack adds).
 * <p>
 * <b>What goes in?</b> Never just anything the machine would accept (a Guh Oven accepts everything, and nobody wants a
 * chest full of oak logs turned into charcoal). Residents only bring what the machine was <i>shown</i>:
 * <ul>
 *   <li>a Knutselmachine: exactly the ingredients of the Bouwtekening in it;</li>
 *   <li>every other machine: per in slot the kind of item that lay in it the last time somebody looked ({@link #leer}:
 *       once a second for the machines around a huisje with residents, and whenever a chore looks). The owner puts a
 *       little stack in once, by hand; from then on the residents keep that slot filled with the same kind from the
 *       huisje's stock. Another kind in the slot = that kind from now on. The memory is kept on the machine itself (its
 *       persistent data, {@link #WENS}), so it is gone when the machine is broken.</li>
 * </ul>
 * <b>When?</b> A slot is topped up when it is empty of that kind, at most half full, or has room for {@link #DREMPEL}
 * more: no walking up and down for one item.
 * <p>
 * <b>Whose?</b> Residents only serve machines their huisje's owner placed (or that nobody placed: a structure, a
 * command; a Guh Oven keeps no owner): they never carry a neighbour's iron bars into their own Bank Guh ({@link #mag}).
 */
public final class Klusmachines {
    /** Key in a machine's persistent data: slot number -> item id (what lay there, see {@link #leer}). */
    public static final String WENS = "guhs_techklus_wens";
    /** A slot that is not empty is only topped up when at least this many fit (or when it is at most half full). */
    public static final int DREMPEL = 8;

    /** One kind of item a machine wants: the exact item with its components (a drawing), or any stack of that item. */
    public record Wens(ItemStack voorbeeld, boolean precies) {
        public boolean past(ItemStack stack) {
            return precies ? ItemStack.isSameItemSameComponents(stack, voorbeeld) : stack.is(voorbeeld.getItem());
        }
    }

    /** A wish and how many of it would be brought now. */
    public record Vraag(Wens wens, int aantal) {
    }

    private Klusmachines() {
    }

    /** Where things go in: the handler of the machine's top (a hopper above it). */
    @Nullable
    public static ResourceHandler<ItemResource> in(ServerLevel level, BlockPos pos) {
        return Kisten.van(level, pos, Direction.UP);
    }

    /** Where things come out: the handler of the machine's bottom (a hopper under it). */
    @Nullable
    public static ResourceHandler<ItemResource> uit(ServerLevel level, BlockPos pos) {
        return Kisten.van(level, pos, Direction.DOWN);
    }

    /** Is the block here a machine the residents of this huisje serve? */
    public static boolean mag(ServerLevel level, Huisje h, BlockPos pos) {
        if (!level.isLoaded(pos) || !level.getBlockState(pos).is(TechklusFeature.MACHINES)) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be.isRemoved()) {
            return false;
        }
        return !(be instanceof MachineBlockEntity machine) || machine.eigenaar() == null || machine.eigenaar().equals(h.eigenaar());
    }

    /** The machines of this home base its residents serve (from the chores' scan). */
    public static List<BlockPos> rond(ServerLevel level, Huisje h) {
        List<BlockPos> uit = new ArrayList<>();
        for (BlockPos p : KlusGebied.van(level, h, TechklusFeature.MACHINE)) {
            if (mag(level, h, p)) {
                uit.add(p);
            }
        }
        return uit;
    }

    /** Does something lie ready in the machine's out slots? */
    public static boolean heeftUitvoer(ServerLevel level, BlockPos pos) {
        ResourceHandler<ItemResource> uit = uit(level, pos);
        return uit != null && !Kisten.neem(uit, s -> true, 64, true).isEmpty();
    }

    // =====================================================================================================================
    // what a machine was shown
    // =====================================================================================================================

    /**
     * Looks into the machine's in slots and remembers per slot the kind of item that lies there now (an empty slot keeps
     * what it knew). A Knutselmachine is skipped: its drawing says it all. A Guh Oven only learns what it can bake.
     */
    public static void leer(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be instanceof KnutselmachineBlockEntity) {
            return;
        }
        ResourceHandler<ItemResource> in = in(level, pos);
        if (in == null) {
            return;
        }
        CompoundTag wens = null;
        for (int i = 0; i < in.size() && i < 27; i++) {
            ItemResource soort = in.getResource(i);
            if (soort.isEmpty() || in.getAmountAsLong(i) <= 0 || !in.isValid(i, soort)) {
                continue;
            }
            if (be instanceof GuhOvenBlockEntity && level.recipeAccess()
                    .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(soort.toStack()), level).isEmpty()) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(soort.getItem()).toString();
            CompoundTag nu = wens != null ? wens : be.getPersistentData().getCompoundOrEmpty(WENS);
            if (!id.equals(nu.getStringOr(Integer.toString(i), ""))) {
                if (wens == null) {
                    wens = nu.copy();
                }
                wens.putString(Integer.toString(i), id);
            }
        }
        if (wens != null) {
            be.getPersistentData().put(WENS, wens);
            be.setChanged();
        }
    }

    /** What this machine wants (see the class text): the drawing's ingredients, or the kinds its in slots were shown. */
    public static List<Wens> wensen(ServerLevel level, BlockPos pos) {
        List<Wens> uit = new ArrayList<>();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return uit;
        }
        if (be instanceof KnutselmachineBlockEntity knutsel) {
            Bouwtekening tekening = knutsel.tekening();
            if (tekening != null) {
                for (ItemStack wat : tekening.ingredienten()) {
                    uit.add(new Wens(wat.copyWithCount(1), true));
                }
            }
            return uit;
        }
        leer(level, pos);
        CompoundTag wens = be.getPersistentData().getCompoundOrEmpty(WENS);
        for (String vak : new TreeSet<>(wens.keySet())) {
            Identifier id = Identifier.tryParse(wens.getStringOr(vak, ""));
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
            if (item != Items.AIR && uit.stream().noneMatch(w -> w.voorbeeld().is(item))) {
                uit.add(new Wens(new ItemStack(item), false));
            }
        }
        return uit;
    }

    /** Makes a machine forget what it was shown (tests, the op command). */
    public static void vergeet(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null && be.getPersistentData().contains(WENS)) {
            be.getPersistentData().remove(WENS);
            be.setChanged();
        }
    }

    /** How many of this wish the machine would take right now (at most a stack). */
    private static int ruimte(ResourceHandler<ItemResource> in, Wens wens) {
        int n = Math.min(64, wens.voorbeeld().getMaxStackSize());
        return n - Kisten.stop(in, wens.voorbeeld().copyWithCount(n), true).getCount();
    }

    /** How many of this wish lie in the machine's in slots. */
    private static long ligtErin(ResourceHandler<ItemResource> in, Wens wens) {
        long n = 0;
        for (int i = 0; i < in.size(); i++) {
            ItemResource soort = in.getResource(i);
            if (!soort.isEmpty() && soort.test(wens::past) && in.isValid(i, soort)) {
                n += in.getAmountAsLong(i);
            }
        }
        return n;
    }

    /** The wishes of this machine that should be topped up now, with how many fit (whether the huisje has them or not). */
    public static List<Vraag> tekort(ServerLevel level, BlockPos pos) {
        List<Vraag> uit = new ArrayList<>();
        ResourceHandler<ItemResource> in = in(level, pos);
        if (in == null) {
            return uit;
        }
        for (Wens wens : wensen(level, pos)) {
            int ruimte = ruimte(in, wens);
            if (ruimte <= 0) {
                continue;
            }
            long ligt = ligtErin(in, wens);
            if (ligt == 0 || ruimte >= DREMPEL || ligt <= ruimte) {
                uit.add(new Vraag(wens, ruimte));
            }
        }
        return uit;
    }

    /** What a resident of this huisje would bring to this machine now: {@link #tekort}, as far as the huisje's stock has it. */
    public static List<Vraag> teVullen(ServerLevel level, Huisje h, BlockPos pos) {
        List<Vraag> uit = new ArrayList<>();
        for (Vraag vraag : tekort(level, pos)) {
            long voorraad = Voorraad.tel(level, h, vraag.wens()::past);
            if (voorraad > 0) {
                uit.add(new Vraag(vraag.wens(), (int) Math.min(vraag.aantal(), voorraad)));
            }
        }
        return uit;
    }

    // =====================================================================================================================
    // looking once a second
    // =====================================================================================================================

    /**
     * Once a second: every machine around a huisje that has residents (in loaded chunks) is looked at ({@link #leer}), so a
     * stack the owner puts in is seen before the machine has used it up. Cheap: only the block entities of the home base's
     * chunks are walked, no blocks are scanned.
     */
    static void kijk(MinecraftServer server) {
        for (Huisje h : Huisjes.alle(server)) {
            ServerLevel level = server.getLevel(h.dim());
            if (level == null || h.bewoners().isEmpty() || !level.isLoaded(h.pos())) {
                continue;
            }
            AABB box = h.gebied();
            for (int cx = ((int) Math.floor(box.minX)) >> 4; cx <= ((int) Math.floor(box.maxX)) >> 4; cx++) {
                for (int cz = ((int) Math.floor(box.minZ)) >> 4; cz <= ((int) Math.floor(box.maxZ)) >> 4; cz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) {
                        continue;
                    }
                    for (BlockEntity be : List.copyOf(chunk.getBlockEntities().values())) {
                        if (!be.isRemoved() && be.getBlockState().is(TechklusFeature.MACHINES) && h.inGebied(be.getBlockPos())) {
                            leer(level, be.getBlockPos());
                        }
                    }
                }
            }
        }
    }
}
