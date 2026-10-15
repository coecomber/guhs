package nl.juiced.guhs.feature.techsaus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.SausTank;

/**
 * The block entity of a machine that holds sauce: a {@link MachineBlockEntity} (vadskracht, the face, the item slots, the
 * owner) with one or two {@link SausTank}s.
 * <ul>
 *   <li><b>Hoses</b> ({@link Slangen}): every {@link SausGetallen#SLANG_TIKKEN} ticks, while it has vadskracht, the machine
 *       slurps the sauce each tank is for ({@link #wens}) out of the Sausvaten and pumps it is joined to. What a hose sees
 *       ({@link #sausHandler}) only lets sauce in.</li>
 *   <li><b>By hand</b>, without a screen (like the Guhbrouwketel and the frying pan): a bucket pours in or scoops out, an item
 *       that belongs in an in slot goes in, any other click takes what is ready; an empty hand on an empty machine tells how
 *       it is doing, and sneaking takes the ingredients back out.</li>
 *   <li><b>The hover readout</b> shows the tanks and what the machine waits for ({@link #stand}), under the vadskracht lines.</li>
 * </ul>
 * A subclass makes its tanks in its constructor ({@link #maakTank}) and fills in {@link #wens}, {@link #wachtOp} and the three
 * methods of the machine itself.
 */
public abstract class SausMachineBlockEntity extends MachineBlockEntity {
    private final List<SausTank> tanks = new ArrayList<>(2);
    private final List<FluidResource> hoort = new ArrayList<>(2);
    @Nullable
    private ResourceHandler<FluidResource> alle;
    @Nullable
    private ResourceHandler<FluidResource> buiten;
    /** (not saved) the advancements this machine gave its owner already. */
    private final java.util.Set<String> beloond = new java.util.HashSet<>(2);

    protected SausMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int vraag, int vakken) {
        super(type, pos, state, vraag, vakken);
    }

    // =====================================================================================================================
    // the tanks
    // =====================================================================================================================

    /** A tank for exactly this sauce (EMPTY: any sauce of the Guh-technologie, one at a time). Call it in the constructor. */
    protected final SausTank maakTank(int mb, FluidResource wat) {
        SausTank tank = new SausTank(mb, wat.isEmpty() ? nl.juiced.guhs.feature.vadskracht.Sauzen::isTechniek : wat::equals, this::tankVeranderd);
        tanks.add(tank);
        hoort.add(wat);
        return tank;
    }

    private void tankVeranderd() {
        setChanged();
    }

    public final List<SausTank> tanks() {
        return tanks;
    }

    /** All tanks as one handler, in and out: for the machine itself and for a bucket in a hand. */
    public final ResourceHandler<FluidResource> alleTanks() {
        if (alle == null) {
            alle = tanks.size() == 1 ? tanks.get(0) : new CombinedResourceHandler<>(tanks);
        }
        return alle;
    }

    /** What a hose (or a pipe of another mod) sees: sauce goes in, never out. Register it for {@code Capabilities.Fluid.BLOCK}. */
    public ResourceHandler<FluidResource> sausHandler(@Nullable Direction kant) {
        if (buiten == null) {
            buiten = new Eenrichting(alleTanks(), slangIn(), slangUit());
        }
        return buiten;
    }

    protected boolean slangIn() {
        return true;
    }

    protected boolean slangUit() {
        return false;
    }

    /** Which sauce this tank slurps out of the hoses (null: none; the pump). Default: what the tank was made for. */
    @Nullable
    protected FluidResource wens(int tank) {
        FluidResource wat = hoort.get(tank);
        return wat.isEmpty() ? null : wat;
    }

    /** 0 (every tank empty) .. 15 (a tank is full): for a comparator. */
    public final int tankSignaal() {
        int hoogst = 0;
        for (SausTank tank : tanks) {
            if (!tank.isLeeg()) {
                hoogst = Math.max(hoogst, 1 + (int) (tank.vulling() * 14));
            }
        }
        return hoogst;
    }

    // =====================================================================================================================
    // the hoses
    // =====================================================================================================================

    /** Every server tick, before the machine's own tick (also without vadskracht). */
    void sausTick() {
        if (!(level instanceof ServerLevel server) || Math.floorMod(server.getGameTime() + worldPosition.asLong(), (long) SausGetallen.SLANG_TIKKEN) != 0) {
            return;
        }
        if (heeftKracht() && aan()) {
            slang(server);
        }
    }

    /** One turn at the hoses (only with vadskracht): slurp what the tanks are for. */
    protected void slang(ServerLevel server) {
        for (int i = 0; i < tanks.size(); i++) {
            SausTank tank = tanks.get(i);
            FluidResource wat = wens(i);
            if (wat != null && tank.ruimte() > 0) {
                Slangen.haal(server, worldPosition, sausHandler(null), tank, wat, Math.min(SausGetallen.SLANG_PER_KEER, tank.ruimte()));
            }
        }
    }

    // =====================================================================================================================
    // the item slots (for the machine's own work)
    // =====================================================================================================================

    /** How many of this item still fit in the slots van (inclusive) .. tot (exclusive). */
    protected final int ruimte(ItemStack soort, int van, int tot) {
        if (soort.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (int vak = van; vak < tot; vak++) {
            int erin = vakken().getAmountAsInt(vak);
            if (erin == 0) {
                n += soort.getMaxStackSize();
            } else if (vakken().getResource(vak).matches(soort)) {
                n += Math.max(0, soort.getMaxStackSize() - erin);
            }
        }
        return n;
    }

    /** Lays the stack in the slots van .. tot (first on a pile of the same, then in an empty slot). Returns what did not fit. */
    protected final int leg(ItemStack stack, int van, int tot) {
        int rest = stack.getCount();
        ItemResource soort = ItemResource.of(stack);
        for (int ronde = 0; ronde < 2 && rest > 0; ronde++) {
            for (int vak = van; vak < tot && rest > 0; vak++) {
                int erin = vakken().getAmountAsInt(vak);
                if (ronde == 0 ? erin > 0 && vakken().getResource(vak).matches(stack) : erin == 0) {
                    int n = Math.min(rest, stack.getMaxStackSize() - erin);
                    if (n > 0) {
                        vakken().set(vak, soort, erin + n);
                        rest -= n;
                    }
                }
            }
        }
        return rest;
    }

    /** Takes n items out of this slot (it holds at least that many). */
    protected final void pak(int vak, int n) {
        int erin = vakken().getAmountAsInt(vak);
        ItemResource soort = vakken().getResource(vak);
        vakken().set(vak, erin - n <= 0 ? ItemResource.EMPTY : soort, Math.max(0, erin - n));
    }

    /** No out slot (van .. tot) is free and none can take one more of what it holds. */
    protected final boolean propvol(int van, int tot) {
        for (int vak = van; vak < tot; vak++) {
            int erin = vakken().getAmountAsInt(vak);
            if (erin == 0 || erin < vakken().getResource(vak).getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    // =====================================================================================================================
    // by hand
    // =====================================================================================================================

    /** (client and server) would a click with this do something other than taking the output? */
    public boolean wil(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemResource wat = ItemResource.of(stack);
        for (int vak = 0; vak < vakken().size(); vak++) {
            if (isInvoer(vak) && past(vak, wat)) {
                return true;
            }
        }
        return !tanks.isEmpty() && ItemAccess.forStack(stack).oneByOne().getCapability(Capabilities.Fluid.ITEM) != null;
    }

    /** A right-click with the item in this hand. False: this item means nothing here (the click then takes the output). */
    public boolean klik(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!wil(stack) || level == null) {
            return false;
        }
        ItemResource wat = ItemResource.of(stack);
        boolean hoortErin = false;
        for (int vak = 0; vak < vakken().size(); vak++) {
            hoortErin |= isInvoer(vak) && past(vak, wat);
        }
        if (hoortErin) {
            ItemStack rest = Kisten.stop(handler(null), stack);
            int erin = stack.getCount() - rest.getCount();
            if (erin <= 0) {
                zeg(player, "vol", ChatFormatting.GRAY);
                return true;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(erin);
            }
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
            return true;
        }
        // a bucket: scoop out of the tanks, or pour in
        if (!FluidUtil.interactWithFluidHandler(player, hand, worldPosition, alleTanks(), null)) {
            zeg(player, "emmer_past_niet", ChatFormatting.GRAY);
        }
        return true;
    }

    /** A right-click that put nothing in: take what is ready; nothing ready: sneaking takes the ingredients back, else the status. */
    public void klikLeeg(ServerPlayer player) {
        if (level == null) {
            return;
        }
        if (geef(player, true) || player.isShiftKeyDown() && geef(player, false)) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5f, 1.3f);
            return;
        }
        List<Component> regels = new ArrayList<>();
        stand(regels::add);
        MutableComponent regel = Component.empty();
        for (int i = 0; i < regels.size(); i++) {
            regel.append(i == 0 ? "" : " · ").append(regels.get(i));
        }
        player.sendOverlayMessage(regel);
    }

    /** Hands the player everything in the out slots (or the in slots). True: there was something. */
    private boolean geef(ServerPlayer player, boolean uitvoer) {
        boolean iets = false;
        for (int vak = 0; vak < vakken().size(); vak++) {
            if (uitvoer ? !isUitvoer(vak) : !isInvoer(vak)) {
                continue;
            }
            int n = vakken().getAmountAsInt(vak);
            if (n > 0) {
                ItemStack stack = vakken().getResource(vak).toStack(n);
                vakken().set(vak, ItemResource.EMPTY, 0);
                if (uitvoer) {
                    geoogst(player, stack);
                }
                Minigames.give(player, stack);
                iets = true;
            }
        }
        return iets;
    }

    /** A player took this out of an out slot by hand. */
    protected void geoogst(ServerPlayer player, ItemStack stack) {
    }

    protected final void zeg(ServerPlayer player, String sleutel, ChatFormatting kleur) {
        player.sendOverlayMessage(Component.translatable(SausTekst.K + sleutel).withStyle(kleur));
    }

    /**
     * {@link SausBeloning#geef} for whoever placed this machine: it did its work while its owner is online. Asked once per
     * loaded machine.
     */
    protected final void beloonEigenaar(String naam) {
        if (beloond.contains(naam) || !(level instanceof ServerLevel server) || eigenaar() == null) {
            return;
        }
        ServerPlayer speler = server.getServer().getPlayerList().getPlayer(eigenaar());
        if (speler != null) {
            beloond.add(naam);
            SausBeloning.geef(speler, naam);
        }
    }

    // =====================================================================================================================
    // what you read
    // =====================================================================================================================

    /** What the machine waits for (null: nothing, it runs or it is full). */
    @Nullable
    protected abstract Component wachtOp();

    /** The lines about this machine: its tanks, then what it waits for. */
    protected void stand(Consumer<Component> regels) {
        for (int i = 0; i < tanks.size(); i++) {
            regels.accept(SausTekst.tank(tanks.get(i), hoort.get(i)).withStyle(ChatFormatting.YELLOW));
        }
        Component wacht = isVol() ? Component.translatable(SausTekst.K + "wacht.vol") : wachtOp();
        if (wacht != null) {
            regels.accept(Component.translatable(SausTekst.K + "wacht", wacht).withStyle(ChatFormatting.GRAY));
        }
        if (level instanceof ServerLevel server && Slangen.teLang(server, worldPosition)) {
            regels.accept(Component.translatable(SausTekst.K + "slang_te_lang", SausGetallen.SLANG_MAX).withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        stand(regels);
    }

    // =====================================================================================================================
    // saving
    // =====================================================================================================================

    @Override
    protected void opslaan(ValueOutput uit) {
        for (int i = 0; i < tanks.size(); i++) {
            tanks.get(i).opslaan(uit, "Tank" + i);
        }
    }

    @Override
    protected void laden(ValueInput in) {
        for (int i = 0; i < tanks.size(); i++) {
            tanks.get(i).laden(in, "Tank" + i);
        }
    }
}
