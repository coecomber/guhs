package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Groep;
import nl.juiced.guhs.feature.vadsrijk.ambacht.PlekWereld.Spot;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.10.4 "Grondstoffen zie je nu echt": every gathering spot (except hives, which stay per-player stands: {@link Imkeren}) is ONE
 * real block in the world, the same for everyone ({@link GrondstofBlok}):
 * <ul>
 *   <li>ore: the floor rock under the spot becomes an ore vein; wood: a chopping stump beside the tree; herbs: a bush on the grass;
 *   fishing: a bobber with ripples in the water; diving: a coral on the sea floor (the last two waterlogged);</li>
 *   <li>a harvest empties it for everybody ({@link #geoogst}) for the group's respawn time; then it comes back full, usually at
 *   another of its {@link AmbachtPlekken#POSITIES} positions: the old position gets back the block that was there before (kept per
 *   node: {@link Node#oud});</li>
 *   <li>placing is lazy: once per second the spots near players are checked ({@link #tick}, at most {@link #PLAATS_PER_RONDE}
 *   placements a round), only in loaded chunks (never a chunk generated for this), never on a block entity, never where the
 *   terrain is not natural any more (a build); a node whose block is gone (built over, an admin) is placed again elsewhere;</li>
 *   <li>state ({@code guhs:vr_grondstof_nodes}): per spot the position, the original block, the empty-until time and the rare flag;
 *   the block itself lives in the chunk, so an unloaded node keeps its look, and on the next check it follows the saved timer;
 *   a node that has to go while its chunk is not loaded is remembered as a "wees" and restored when that chunk is loaded.</li>
 * </ul>
 * The state of a spot exists without a block too (the gametests harvest spots that were never placed).
 */
public final class Grondstoffen extends SavedData {
    public static final SavedDataType<Grondstoffen> TYPE = GuhSavedData.tagType("vr_grondstof_nodes", Grondstoffen::new, Grondstoffen::load,
            Grondstoffen::save);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    private static final Map<NodeSoort, DeferredBlock<GrondstofBlok>> BLOK = new EnumMap<>(NodeSoort.class);
    /** Placements (and moves) per round (a round = once per second): no lag spike on the first start of a big server. */
    public static final int PLAATS_PER_RONDE = 48;
    /** Old positions restored per round once their chunk is loaded. */
    public static final int WEES_PER_RONDE = 32;
    /** A node that found no position tries again after this many ticks. */
    public static final int OPNIEUW = 20 * 60;
    /** Block updates: tell the clients, no neighbour shape updates (no falling gravel, no popping plants). */
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    static {
        for (NodeSoort s : NodeSoort.values()) {
            if (s != NodeSoort.KORF) {
                BLOK.put(s, BLOCKS.registerBlock("vr_grondstof_" + s.id, p -> new GrondstofBlok(p, s), () -> eigenschappen(s)));
            }
        }
    }

    /** One spot's shared state. */
    public static final class Node {
        int idx;
        long klaar;
        boolean zeldzaam;
        /** Where the block is now (null: not placed). */
        @Nullable
        BlockPos pos;
        /** The block that was there before (restored when the node leaves this position). */
        @Nullable
        BlockState oud;
        long probeerNa;

        public long klaar() {
            return klaar;
        }

        public boolean zeldzaam() {
            return zeldzaam;
        }

        @Nullable
        public BlockPos pos() {
            return pos;
        }

        @Nullable
        public BlockState oud() {
            return oud;
        }
    }

    /** A position whose original block still has to come back (its chunk was not loaded). */
    record Wees(BlockPos pos, BlockState oud) {
    }

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final List<Wees> wezen = new ArrayList<>();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static BlockBehaviour.Properties eigenschappen(NodeSoort s) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable().pushReaction(PushReaction.BLOCK)
                .isValidSpawn((st, l, pos, e) -> false);
        return switch (s) {
            case ERTS -> p.mapColor(MapColor.STONE).sound(SoundType.STONE);
            case HOUT -> p.mapColor(MapColor.WOOD).sound(SoundType.WOOD).noOcclusion();
            case KRUID -> p.mapColor(MapColor.PLANT).sound(SoundType.SWEET_BERRY_BUSH).noCollision().noOcclusion();
            case VIS -> p.mapColor(MapColor.WATER).sound(SoundType.LILY_PAD).noCollision().noOcclusion();
            default -> p.mapColor(MapColor.COLOR_PINK).sound(SoundType.CORAL_BLOCK).noCollision().noOcclusion();
        };
    }

    @Nullable
    public static GrondstofBlok blok(NodeSoort s) {
        DeferredBlock<GrondstofBlok> b = BLOK.get(s);
        return b == null ? null : b.get();
    }

    /** True: this state is a node block of this kind. */
    public static boolean isNode(BlockState st, NodeSoort s) {
        return st.getBlock() instanceof GrondstofBlok b && b.soort == s;
    }

    /** The node block state of a kind, tier and fullness (fishing and dive spots waterlogged). */
    public static BlockState staat(NodeSoort s, int tier, boolean vol) {
        GrondstofBlok b = blok(s);
        if (b == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return b.defaultBlockState().setValue(GrondstofBlok.TIER, Math.max(1, Math.min(6, tier))).setValue(GrondstofBlok.VOL, vol)
                .setValue(GrondstofBlok.WATERLOGGED, s == NodeSoort.VIS || s == NodeSoort.DUIK);
    }

    public static Grondstoffen get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // ---- the state ------------------------------------------------------------------------------------------------------------

    public Node node(Spot s) {
        return nodes.computeIfAbsent(s.id(), k -> {
            Node n = new Node();
            n.idx = Math.floorMod(s.id().hashCode() * 31, Math.max(1, s.posities().size()));
            setDirty();
            return n;
        });
    }

    @Nullable
    public Node bekend(String id) {
        return nodes.get(id);
    }

    /** Where a node of this kind sits for spot position {@code p}: ore in the floor under it, everything else at it. */
    public static BlockPos blokPos(NodeSoort s, BlockPos p) {
        return s == NodeSoort.ERTS ? p.below() : p;
    }

    /** The block position of a spot now (placed: where the block is; else where it will come). */
    public BlockPos positie(Spot s) {
        Node n = node(s);
        if (n.pos != null) {
            return n.pos;
        }
        return blokPos(s.groep().soort(), s.posities().get(Math.min(n.idx, s.posities().size() - 1)));
    }

    public boolean vol(ServerLevel sl, Spot s) {
        return sl.getGameTime() >= node(s).klaar;
    }

    /** A harvest: empty until {@code klaar} for everyone ({@code zeldzaam}: the next fill glitters). */
    public void geoogst(ServerLevel sl, Spot s, long klaar, boolean zeldzaam) {
        Node n = node(s);
        n.klaar = klaar;
        n.zeldzaam = zeldzaam;
        setDirty();
        if (n.pos != null && sl.isLoaded(n.pos)) {
            BlockState st = sl.getBlockState(n.pos);
            if (isNode(st, s.groep().soort()) && st.getValue(GrondstofBlok.VOL)) {
                sl.setBlock(n.pos, st.setValue(GrondstofBlok.VOL, false), FLAGS);
            }
        }
    }

    /** Dev/tests: this spot is full now (and rare when asked). */
    public void maakVol(Spot s, boolean zeldzaam) {
        Node n = node(s);
        n.klaar = 0;
        n.zeldzaam = zeldzaam;
        setDirty();
    }

    /** Tests: the empty-until time of this spot. */
    public void zetKlaar(Spot s, long klaar) {
        node(s).klaar = klaar;
        setDirty();
    }

    /** Tests: forget a spot's state without touching the world. */
    public void wis(String id) {
        nodes.remove(id);
        setDirty();
    }

    // ---- placing and checking --------------------------------------------------------------------------------------------------

    /** Once a second: the nodes of the spots near players in Het Vadsrijk are placed / follow their timers; old positions restored. */
    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.players().isEmpty()) {
            return;
        }
        Grondstoffen g = get(server);
        Map<String, Spot> spots = new LinkedHashMap<>();
        for (ServerPlayer p : sl.players()) {
            for (Spot s : Verzamelen.inBuurt(p, Verzamelen.ZICHT)) {
                if (s.groep().soort() != NodeSoort.KORF && s.posities().size() > 0) {
                    spots.putIfAbsent(s.id(), s);
                }
            }
        }
        int[] budget = {PLAATS_PER_RONDE};
        g.herstelWezen(sl, WEES_PER_RONDE);
        for (Spot s : spots.values()) {
            g.bijwerk(sl, s, budget);
        }
    }

    /**
     * Brings one spot's block in line with its state (public for the gametests): places it when it has none yet (in a loaded chunk,
     * at a free natural position), follows the timer (full again: usually moved to another position), notices a block that is gone.
     * {@code budget[0]}: placements left this round.
     */
    public void bijwerk(ServerLevel sl, Spot s, int[] budget) {
        Groep g = s.groep();
        NodeSoort soort = g.soort();
        if (soort == NodeSoort.KORF || s.posities().isEmpty()) {
            return;
        }
        Node n = node(s);
        long nu = sl.getGameTime();
        boolean vol = nu >= n.klaar;
        if (n.pos != null) {
            if (!hoort(s, n.pos)) {
                // the group was scanned again (its anchor moved, or /ambacht scan): the old position goes back to what it was
                weg(sl, n, soort);
            } else if (sl.isLoaded(n.pos)) {
                BlockState st = sl.getBlockState(n.pos);
                if (!isNode(st, soort)) {
                    // its block is gone (an admin, a world patch): forget it (that block stays) and find a place again
                    n.pos = null;
                    n.oud = null;
                    setDirty();
                } else if (vol && !st.getValue(GrondstofBlok.VOL)) {
                    if (!verhuis(sl, s, n, budget)) {
                        sl.setBlock(n.pos, staat(soort, g.tier(), true), FLAGS);
                    }
                    return;
                } else if (st.getValue(GrondstofBlok.VOL) != vol || st.getValue(GrondstofBlok.TIER) != Math.max(1, Math.min(6, g.tier()))) {
                    sl.setBlock(n.pos, staat(soort, g.tier(), vol), FLAGS);
                    return;
                }
            }
        }
        if (n.pos == null && budget[0] > 0 && nu >= n.probeerNa) {
            int k = s.posities().size();
            boolean nietGeladen = false;
            for (int j = 0; j < k; j++) {
                int i = (n.idx + j) % k;
                BlockPos p = s.posities().get(i);
                if (!geladen(sl, soort, p)) {
                    nietGeladen = true;
                    continue;
                }
                if (magHier(sl, soort, p)) {
                    plaats(sl, s, n, i, vol);
                    budget[0]--;
                    return;
                }
            }
            if (!nietGeladen) {
                n.probeerNa = nu + OPNIEUW;   // (nothing free now: try again in a minute)
            }
        }
    }

    /** Full again: to another free position (true), the old one gets its block back. */
    private boolean verhuis(ServerLevel sl, Spot s, Node n, int[] budget) {
        int k = s.posities().size();
        if (k < 2 || budget[0] <= 0 || n.pos == null) {
            return false;
        }
        NodeSoort soort = s.groep().soort();
        int start = sl.getRandom().nextInt(k);
        for (int j = 0; j < k; j++) {
            int i = (start + j) % k;
            BlockPos p = s.posities().get(i);
            if (i == n.idx || blokPos(soort, p).equals(n.pos) || !geladen(sl, soort, p) || !magHier(sl, soort, p)) {
                continue;
            }
            herstel(sl, n.pos, n.oud, soort);
            plaats(sl, s, n, i, true);
            budget[0]--;
            return true;
        }
        return false;
    }

    private void plaats(ServerLevel sl, Spot s, Node n, int i, boolean vol) {
        NodeSoort soort = s.groep().soort();
        BlockPos t = blokPos(soort, s.posities().get(i)).immutable();
        n.oud = sl.getBlockState(t);
        n.pos = t;
        n.idx = i;
        sl.setBlock(t, staat(soort, s.groep().tier(), vol), FLAGS);
        setDirty();
    }

    /** The node leaves its position: the original block comes back now, or when its chunk is loaded. */
    private void weg(ServerLevel sl, Node n, NodeSoort soort) {
        if (n.pos != null && n.oud != null) {
            if (sl.isLoaded(n.pos)) {
                herstel(sl, n.pos, n.oud, soort);
            } else {
                wezen.add(new Wees(n.pos, n.oud));
            }
        }
        n.pos = null;
        n.oud = null;
        setDirty();
    }

    /** Puts the original block back, only where our node still is (a block somebody put there since is left alone). */
    static void herstel(ServerLevel sl, BlockPos pos, @Nullable BlockState oud, @Nullable NodeSoort soort) {
        BlockState st = sl.getBlockState(pos);
        if (st.getBlock() instanceof GrondstofBlok b && (soort == null || b.soort == soort)) {
            sl.setBlock(pos, oud == null ? Blocks.AIR.defaultBlockState() : oud, FLAGS);
        }
    }

    private void herstelWezen(ServerLevel sl, int max) {
        if (wezen.isEmpty()) {
            return;
        }
        int n = 0;
        for (var it = wezen.iterator(); it.hasNext() && n < max;) {
            Wees w = it.next();
            if (sl.isLoaded(w.pos())) {
                herstel(sl, w.pos(), w.oud(), null);
                it.remove();
                n++;
                setDirty();
            }
        }
    }

    /** Dev ({@code /ambacht scan}): every placed node leaves the world (original blocks back, or later); the state is forgotten. */
    public void allesWeg(ServerLevel sl) {
        for (Node n : nodes.values()) {
            if (n.pos != null && n.oud != null) {
                if (sl.isLoaded(n.pos)) {
                    herstel(sl, n.pos, n.oud, null);
                } else {
                    wezen.add(new Wees(n.pos, n.oud));
                }
            }
        }
        nodes.clear();
        setDirty();
    }

    /** True: {@code pos} is (the block position of) one of this spot's positions. */
    private static boolean hoort(Spot s, BlockPos pos) {
        for (BlockPos p : s.posities()) {
            if (blokPos(s.groep().soort(), p).equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean geladen(ServerLevel sl, NodeSoort soort, BlockPos p) {
        return sl.isLoaded(blokPos(soort, p)) && sl.isLoaded(p);
    }

    /**
     * May a node of this kind come at spot position {@code p} now? Never on a block entity or another node; the terrain the scan
     * found must still be there (natural rock for ore, a free spot on a natural floor for wood/herbs, open still water for fishing,
     * water on the sea floor for diving): a player's build or a changed terrain means "not here".
     */
    public static boolean magHier(ServerLevel sl, NodeSoort soort, BlockPos p) {
        BlockPos t = blokPos(soort, p);
        BlockState st = sl.getBlockState(t);
        if (st.hasBlockEntity() || st.getBlock() instanceof GrondstofBlok) {
            return false;
        }
        switch (soort) {
            case ERTS -> {
                return st.isCollisionShapeFullBlock(sl, t) && st.getFluidState().isEmpty() && PlekWereld.natuur(st) && !st.is(Blocks.BEDROCK)
                        && vrij(sl, p);
            }
            case HOUT, KRUID -> {
                BlockState onder = sl.getBlockState(p.below());
                return vrij(sl, p) && !(st.getBlock() instanceof DoublePlantBlock) && onder.isCollisionShapeFullBlock(sl, p.below())
                        && PlekWereld.natuur(onder) && !(onder.getBlock() instanceof GrondstofBlok);
            }
            case VIS -> {
                return st.is(Blocks.WATER) && sl.getFluidState(t).isSource() && sl.getBlockState(t.above()).isAir();
            }
            case DUIK -> {
                BlockState onder = sl.getBlockState(t.below());
                return st.is(Blocks.WATER) && sl.getFluidState(t).isSource() && onder.isCollisionShapeFullBlock(sl, t.below())
                        && !(onder.getBlock() instanceof GrondstofBlok);
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean vrij(ServerLevel sl, BlockPos p) {
        BlockState s = sl.getBlockState(p);
        return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty() && s.getCollisionShape(sl, p).isEmpty() && !s.hasBlockEntity());
    }

    // ---- saving --------------------------------------------------------------------------------------------------------------

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        CompoundTag nt = new CompoundTag();
        nodes.forEach((id, n) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("idx", n.idx);
            c.putLong("klaar", n.klaar);
            c.putBoolean("zeldzaam", n.zeldzaam);
            if (n.pos != null && n.oud != null) {
                c.putLong("pos", n.pos.asLong());
                c.put("oud", NbtUtils.writeBlockState(n.oud));
            }
            nt.put(id, c);
        });
        t.put("nodes", nt);
        ListTag w = new ListTag();
        for (Wees x : wezen) {
            CompoundTag c = new CompoundTag();
            c.putLong("pos", x.pos().asLong());
            c.put("oud", NbtUtils.writeBlockState(x.oud()));
            w.add(c);
        }
        t.put("wezen", w);
        return t;
    }

    static Grondstoffen load(CompoundTag t) {
        Grondstoffen g = new Grondstoffen();
        CompoundTag nt = t.getCompoundOrEmpty("nodes");
        for (String id : nt.keySet()) {
            CompoundTag c = nt.getCompoundOrEmpty(id);
            Node n = new Node();
            n.idx = c.getIntOr("idx", 0);
            n.klaar = c.getLongOr("klaar", 0L);
            n.zeldzaam = c.getBooleanOr("zeldzaam", false);
            if (c.contains("pos")) {
                n.pos = BlockPos.of(c.getLongOr("pos", 0L));
                n.oud = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, c.getCompoundOrEmpty("oud"));
            }
            int i = id.lastIndexOf('_');
            if (i > 0 && AmbachtPlekken.groep(id.substring(0, i)) == null && n.pos != null && n.oud != null) {
                g.wezen.add(new Wees(n.pos, n.oud));   // (a group that no longer exists: its block goes back)
                continue;
            }
            g.nodes.put(id, n);
        }
        for (net.minecraft.nbt.Tag x : t.getListOrEmpty("wezen")) {
            if (x instanceof CompoundTag c) {
                g.wezen.add(new Wees(BlockPos.of(c.getLongOr("pos", 0L)), NbtUtils.readBlockState(BuiltInRegistries.BLOCK, c.getCompoundOrEmpty("oud"))));
            }
        }
        return g;
    }

    /** Tests: a save + load round trip (what a restart does to this data). */
    public static Grondstoffen rondje(Grondstoffen g) {
        return load(g.save());
    }

    /** Tests/dev: how many old positions still wait for their chunk. */
    public int wezen() {
        return wezen.size();
    }

    /** Dev: the number of placed nodes. */
    public int geplaatst() {
        int n = 0;
        for (Node x : nodes.values()) {
            if (x.pos != null) {
                n++;
            }
        }
        return n;
    }

    /** Dev ({@code /ambacht plekken}): placed nodes per kind. */
    public Map<NodeSoort, Integer> perSoort() {
        Map<NodeSoort, Integer> m = new HashMap<>();
        nodes.forEach((id, n) -> {
            int i = id.lastIndexOf('_');
            Groep gr = i > 0 ? AmbachtPlekken.groep(id.substring(0, i)) : null;
            if (gr != null && n.pos != null) {
                m.merge(gr.soort(), 1, Integer::sum);
            }
        });
        return m;
    }
}
