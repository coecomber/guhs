package nl.juiced.guhs.feature.techbuis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The block entity of a Richtingstuk (and, as {@link FilterBlockEntity}, of a Filterstuk): the only thing of the
 * Knabbelbuizen that ticks.
 * <p>
 * Every {@link Buizen#TEMPO} ticks the piece takes a bite: of what came back ({@link #terug}), of what a hopper pushed into
 * its back ({@link #handler}), or of what is behind it (any item capability), and sends it to the best place its tubes
 * reach ({@link BuisRoutes}: first the places behind a Filterstuk that asks for this item, then the nearest where it
 * fits). The item is taken out for real and kept here as a ride ({@link Rit}) until it arrives; then it goes into its
 * place. If the place is full or gone by then, or the tubes were cut, it comes back here and is sent again, or put back
 * where it came from; a piece that cannot get rid of something stops biting until it can. Nothing is ever lost: rides and
 * what came back are saved, and fall out when the piece is broken.
 * <p>
 * A piece with a tube behind it never bites: it is a one-way valve that others send through ({@link #laatDoor}).
 */
public class BuisStukBlockEntity extends BlockEntity {
    /**
     * An item on its way.
     *
     * @param stack    what rolls
     * @param doel     the block it goes into
     * @param kant     the side of that block
     * @param aankomst the game time at which it is there
     */
    public record Rit(ItemStack stack, BlockPos doel, Direction kant, long aankomst) {
        public static final Codec<Rit> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(Rit::stack),
                BlockPos.CODEC.fieldOf("doel").forGetter(Rit::doel),
                Direction.CODEC.fieldOf("kant").forGetter(Rit::kant),
                Codec.LONG.fieldOf("aankomst").forGetter(Rit::aankomst)).apply(i, Rit::new));
    }

    private final List<Rit> onderweg = new ArrayList<>();
    /** What could not be delivered: sent again before anything new is taken. */
    private final List<ItemStack> terug = new ArrayList<>();
    /** One stack that a hopper (or anything else that pushes) put into the back of the piece. */
    private final ItemStacksResourceHandler buffer = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
            rust = 0;
        }
    };
    private final ResourceHandler<ItemResource> invoer = new Invoer();

    @Nullable
    private List<BuisRoutes.Route> routes;
    private int routesVersie;
    private long routesTijd;
    private int rust;
    private int zoekVan;
    private boolean opSlot;
    private boolean slotBekend;
    private boolean verstopt;

    public BuisStukBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // =====================================================================================================================
    // what a Filterstuk does differently
    // =====================================================================================================================

    /** How many items one bite is. */
    protected int hap() {
        return Buizen.HAP_RICHTING;
    }

    /** May the piece work at all right now? (The Filterstuk: only with vadskracht.) */
    protected boolean magWerken() {
        return !opSlot;
    }

    /** May this item be taken out of what is behind the piece (the Filterstuk: its list)? */
    protected boolean magEruit(ItemStack stack) {
        return true;
    }

    /** How many of each kind of item stay behind in what the piece takes from. */
    protected int bewaar() {
        return 0;
    }

    /** May an item that somebody else sends pass through this piece? */
    public boolean laatDoor(ItemStack stack) {
        return !opSlot;
    }

    /** Does this piece ask for particular items (a Filterstuk with a list)? What is behind it gets those first. */
    public boolean isGericht() {
        return false;
    }

    /** Called every server tick before the bite (the Filterstuk: its face). */
    protected void voorTick(ServerLevel level) {
    }

    // =====================================================================================================================
    // state
    // =====================================================================================================================

    public Direction voor() {
        return getBlockState().getValue(BuisStukBlock.FACING);
    }

    /** The items on their way (read only). */
    public List<Rit> onderweg() {
        return List.copyOf(onderweg);
    }

    /** What came back and waits to be sent again (read only). */
    public List<ItemStack> terug() {
        return List.copyOf(terug);
    }

    /** Locked by a redstone signal? */
    public boolean opSlot() {
        return opSlot;
    }

    /** Could it not get rid of something at its last bite? */
    public boolean verstopt() {
        return verstopt;
    }

    /** The piece was turned around: its routes are no longer right. */
    void gedraaid() {
        routes = null;
        rust = 0;
    }

    /** A neighbour changed: is there a redstone signal now? */
    void kijkSlot() {
        if (level != null) {
            opSlot = Buizen.redstone(level, worldPosition);
            slotBekend = true;
        }
    }

    /** What is behind the piece, when that is something to take items out of (not a tube: then the piece is a valve). */
    @Nullable
    private ResourceHandler<ItemResource> bron() {
        Direction voor = voor();
        BlockPos achter = worldPosition.relative(voor.getOpposite());
        if (level == null || !level.isLoaded(achter)) {
            return null;
        }
        BlockState state = level.getBlockState(achter);
        return state.isAir() || Buizen.isBuis(state) || Buizen.isStuk(state) ? null : Kisten.van(level, achter, voor);
    }

    /** Where this piece can send to, nearest first (looked up again when the tubes changed). */
    public List<BuisRoutes.Route> routes() {
        if (level == null) {
            return List.of();
        }
        long nu = level.getGameTime();
        int versie = Buizen.versie(level);
        if (routes == null || versie != routesVersie || nu - routesTijd > Buizen.ROUTE_GELDIG || nu < routesTijd) {
            routes = BuisRoutes.zoek(level, worldPosition, voor());
            routesVersie = versie;
            routesTijd = nu;
        }
        return routes;
    }

    // =====================================================================================================================
    // the tick
    // =====================================================================================================================

    final void serverTick() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        long nu = server.getGameTime();
        if (!slotBekend) {
            kijkSlot();
        }
        if (!onderweg.isEmpty()) {
            lever(server, nu);
        }
        voorTick(server);
        if (rust > 0) {
            rust--;
            return;
        }
        if (Math.floorMod(nu + worldPosition.hashCode(), Buizen.TEMPO) != 0 || !magWerken()) {
            return;
        }
        if (!hapje(server, nu)) {
            rust = Buizen.RUST;
        }
    }

    /** The rides that are over: into their place, or back here. */
    private void lever(ServerLevel server, long nu) {
        for (Iterator<Rit> it = onderweg.iterator(); it.hasNext();) {
            Rit rit = it.next();
            if (rit.aankomst > nu) {
                continue;
            }
            it.remove();
            ItemStack rest = rit.stack;
            if (bereikbaar(rit)) {
                ResourceHandler<ItemResource> doel = Kisten.van(server, rit.doel, rit.kant);
                if (doel != null) {
                    rest = Kisten.stop(doel, rit.stack);
                }
            }
            if (!rest.isEmpty()) {
                komtTerug(rest);
            }
            setChanged();
        }
    }

    /** Do the tubes still lead to where this ride goes? (Somebody may have broken a tube under the item.) */
    private boolean bereikbaar(Rit rit) {
        for (BuisRoutes.Route route : routes()) {
            if (route.doel().equals(rit.doel) && route.kant() == rit.kant) {
                return true;
            }
        }
        return false;
    }

    private void komtTerug(ItemStack stack) {
        for (ItemStack ligt : terug) {
            if (ItemStack.isSameItemSameComponents(ligt, stack)) {
                ligt.grow(stack.getCount());
                return;
            }
        }
        terug.add(stack.copy());
        rust = 0;
    }

    /** One bite. False: there was nothing to do (the piece rests a moment). */
    private boolean hapje(ServerLevel server, long nu) {
        if (onderweg.size() >= Buizen.MAX_ONDERWEG) {
            return true;
        }
        // 1. what came back
        if (!terug.isEmpty()) {
            ItemStack stack = terug.get(0);
            int weg = stuur(server, nu, stack, hap());
            if (weg > 0) {
                stack.shrink(weg);
            } else {
                // nowhere to go: back into what it came out of, when that takes it
                ResourceHandler<ItemResource> bron = bron();
                ItemStack rest = bron == null ? stack : Kisten.stop(bron, stack);
                if (rest.getCount() == stack.getCount()) {
                    verstopt = true;
                    return false;
                }
                stack.setCount(rest.getCount());
            }
            if (stack.isEmpty()) {
                terug.remove(0);
            }
            verstopt = false;
            setChanged();
            return true;
        }
        // 2. what was pushed into the back of the piece
        if (buffer.getAmountAsInt(0) > 0) {
            ItemResource soort = buffer.getResource(0);
            int aantal = buffer.getAmountAsInt(0);
            int weg = stuur(server, nu, soort.toStack(aantal), hap());
            if (weg > 0) {
                buffer.set(0, soort, aantal - weg);
            }
            verstopt = weg == 0;
            return weg > 0;
        }
        // 3. out of what is behind the piece
        ResourceHandler<ItemResource> bron = bron();
        if (bron == null) {
            verstopt = false;
            return false;
        }
        int vakken = bron.size();
        if (vakken <= 0) {
            verstopt = false;
            return false;
        }
        int kijk = Math.min(vakken, Buizen.MAX_ZOEK);
        boolean ietsGevonden = false;
        Set<ItemResource> geprobeerd = new HashSet<>();
        Map<ItemResource, Long> totaal = null;   // (only counted when something must stay behind, once per bite)
        for (int k = 0; k < kijk; k++) {
            int vak = Math.floorMod(zoekVan + k, vakken);
            ItemResource soort = bron.getResource(vak);
            if (soort.isEmpty() || !geprobeerd.add(soort)) {
                continue;
            }
            ItemStack voorbeeld = soort.toStack(1);
            if (Features.isLoaned(voorbeeld) || !magEruit(voorbeeld)) {
                continue;
            }
            int beschikbaar = bron.getAmountAsInt(vak);
            if (bewaar() > 0) {
                if (totaal == null) {
                    totaal = telAlles(bron);
                }
                beschikbaar = (int) Math.min(beschikbaar, totaal.getOrDefault(soort, 0L) - bewaar());
            }
            if (beschikbaar <= 0) {
                continue;
            }
            // can it be taken at all? (a machine's in slot, a Bank Guh without its upgrade: no)
            ItemStack proef = Kisten.neem(bron, s -> ItemStack.isSameItemSameComponents(s, voorbeeld), Math.min(hap(), beschikbaar), true);
            if (proef.isEmpty()) {
                continue;
            }
            ietsGevonden = true;
            Plek plek = zoekPlek(server, proef, proef.getCount());
            if (plek == null) {
                continue;
            }
            ItemStack echt = Kisten.neem(bron, s -> ItemStack.isSameItemSameComponents(s, voorbeeld), plek.aantal);
            if (echt.isEmpty()) {
                continue;
            }
            vertrek(server, nu, echt, plek.route);
            zoekVan = vak;
            verstopt = false;
            return true;
        }
        if (vakken > kijk) {
            zoekVan = Math.floorMod(zoekVan + kijk, vakken);   // (a very big store: go on from here next time)
        }
        verstopt = ietsGevonden;
        return false;
    }

    /**
     * How many of each kind there are in the whole store: one walk over its slots (a Bank Guh can have thousands, and
     * a kind may lie in several slots of a chest).
     */
    private static Map<ItemResource, Long> telAlles(ResourceHandler<ItemResource> bron) {
        Map<ItemResource, Long> uit = new HashMap<>();
        for (int vak = 0, n = bron.size(); vak < n; vak++) {
            ItemResource soort = bron.getResource(vak);
            if (!soort.isEmpty()) {
                uit.merge(soort, bron.getAmountAsLong(vak), Long::sum);
            }
        }
        return uit;
    }

    private record Plek(BuisRoutes.Route route, int aantal) {
    }

    /** Sends up to max of this stack to the best place; returns how many went. */
    private int stuur(ServerLevel server, long nu, ItemStack stack, int max) {
        Plek plek = zoekPlek(server, stack, Math.min(max, stack.getCount()));
        if (plek == null) {
            return 0;
        }
        vertrek(server, nu, stack.copyWithCount(plek.aantal), plek.route);
        return plek.aantal;
    }

    /**
     * The best place for up to {@code wil} of this item: first the places behind a piece that asks for it, then the others,
     * each time the nearest where at least one fits (counting what is already rolling there from here).
     */
    @Nullable
    private Plek zoekPlek(ServerLevel server, ItemStack stack, int wil) {
        List<BuisRoutes.Route> lijst = routes();
        if (lijst.isEmpty() || wil <= 0) {
            return null;
        }
        int[] door = new int[lijst.size()];
        for (int i = 0; i < door.length; i++) {
            door[i] = doorlaat(server, lijst.get(i), stack);
        }
        for (int ronde = 2; ronde >= 1; ronde--) {
            for (int i = 0; i < door.length; i++) {
                if (door[i] != ronde) {
                    continue;
                }
                BuisRoutes.Route route = lijst.get(i);
                ResourceHandler<ItemResource> doel = Kisten.van(server, route.doel(), route.kant());
                if (doel == null) {
                    continue;
                }
                int rolt = 0;
                for (Rit rit : onderweg) {
                    if (rit.doel.equals(route.doel()) && rit.kant == route.kant() && ItemStack.isSameItemSameComponents(rit.stack, stack)) {
                        rolt += rit.stack.getCount();
                    }
                }
                ItemStack proef = stack.copyWithCount(wil + rolt);
                int past = proef.getCount() - Kisten.stop(doel, proef, true).getCount() - rolt;
                if (past > 0) {
                    return new Plek(route, Math.min(past, wil));
                }
            }
        }
        return null;
    }

    /** 0: a piece on the way stops this item; 1: it gets through; 2: it gets through and a piece on the way asks for it. */
    private static int doorlaat(ServerLevel server, BuisRoutes.Route route, ItemStack stack) {
        int uit = 1;
        for (BlockPos pos : route.stukken()) {
            if (!(server.getBlockEntity(pos) instanceof BuisStukBlockEntity stuk) || !stuk.laatDoor(stack)) {
                return 0;
            }
            if (stuk.isGericht()) {
                uit = 2;
            }
        }
        return uit;
    }

    /** The item leaves: it is a ride now, and the players nearby see it roll. */
    private void vertrek(ServerLevel server, long nu, ItemStack stack, BuisRoutes.Route route) {
        onderweg.add(new Rit(stack, route.doel(), route.kant(), nu + (long) route.lengte() * Buizen.TIKKEN_PER_BLOK));
        setChanged();
        BuisPayloads.Rol rol = BuisPayloads.Rol.van(route, stack);
        double ver = Buizen.ZICHT * Buizen.ZICHT;
        for (ServerPlayer speler : server.players()) {
            if (speler.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < ver) {
                ModNetworking.sendTo(speler, rol);
            }
        }
    }

    // =====================================================================================================================
    // what a player reads
    // =====================================================================================================================

    /** One line about what the piece is doing right now. */
    public Component stand() {
        String k = "gui.guhs.techbuis.stand.";
        if (opSlot) {
            return Component.translatable(k + "op_slot").withStyle(ChatFormatting.GOLD);
        }
        if (verstopt || !terug.isEmpty()) {
            return Component.translatable(k + "verstopt").withStyle(ChatFormatting.GOLD);
        }
        int plekken = routes().size();
        if (plekken == 0) {
            return Component.translatable(k + "geen_plek").withStyle(ChatFormatting.GOLD);
        }
        int rollen = 0;
        for (Rit rit : onderweg) {
            rollen += rit.stack.getCount();
        }
        return rollen > 0 ? Component.translatable(k + "onderweg", plekken, rollen).withStyle(ChatFormatting.GREEN)
                : Component.translatable(k + "plekken", plekken).withStyle(ChatFormatting.GREEN);
    }

    /** Everything about the piece (the hover readout of the Filterstuk adds its list). */
    public void regels(Consumer<Component> regels) {
        regels.accept(stand());
        if (level != null && bron() != null) {
            BlockPos achter = worldPosition.relative(voor().getOpposite());
            regels.accept(Component.translatable("gui.guhs.techbuis.stand.hapt_uit", level.getBlockState(achter).getBlock().getName())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    // =====================================================================================================================
    // the item capability: a hopper may push into the back of the piece
    // =====================================================================================================================

    /** What a hopper (or another mod's pipe) gets at this side: at the back things can be put in, nothing comes out. */
    @Nullable
    public ResourceHandler<ItemResource> handler(@Nullable Direction kant) {
        return kant == null || kant == voor().getOpposite() ? invoer : null;
    }

    private final class Invoer implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            return buffer.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return buffer.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return buffer.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            ItemStack stack = resource.toStack(1);
            return !Features.isLoaned(stack) && magEruit(stack);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return isValid(index, resource) ? buffer.insert(index, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }

    // =====================================================================================================================
    // removal, saving
    // =====================================================================================================================

    /** Broken: everything the piece still holds falls out here (the rides too: nothing is lost). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) {
            return;
        }
        List<ItemStack> alles = new ArrayList<>(terug);
        for (Rit rit : onderweg) {
            alles.add(rit.stack);
        }
        alles.addAll(buffer.copyToList());
        onderweg.clear();
        terug.clear();
        for (ItemStack stack : alles) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        onderweg.clear();
        in.read("Onderweg", Rit.CODEC.listOf()).ifPresent(onderweg::addAll);
        terug.clear();
        in.read("Terug", ItemStack.CODEC.listOf()).ifPresent(lijst -> lijst.forEach(s -> terug.add(s.copy())));
        buffer.deserialize(in.childOrEmpty("Buffer"));
        zoekVan = in.getIntOr("ZoekVan", 0);
        routes = null;
        slotBekend = false;
    }

    @Override
    protected void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        if (!onderweg.isEmpty()) {
            uit.store("Onderweg", Rit.CODEC.listOf(), onderweg);
        }
        if (!terug.isEmpty()) {
            uit.store("Terug", ItemStack.CODEC.listOf(), terug);
        }
        buffer.serialize(uit.child("Buffer"));
        uit.putInt("ZoekVan", zoekVan);
    }
}
