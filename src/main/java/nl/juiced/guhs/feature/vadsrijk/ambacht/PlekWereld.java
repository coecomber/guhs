package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Groep;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Zoek;
import nl.juiced.guhs.feature.vadsrijk.grotten.GrotData;
import nl.juiced.guhs.feature.vadsrijk.grotten.Grotten;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The gathering spots in the real terrain: for every {@link Groep} the terrain round its anchor POI is scanned once (when a player
 * is near, so the chunks are loaded), deterministically (seed = group id + anchor), and the result is kept in SavedData
 * {@code guhs:vr_ambacht_plekken} with the anchor it was made for: when t-wereld moves the zone, the anchor changes and the group is
 * scanned again at its new place. Rules (walkability: a spot is never on a path, in a building or in deep water):
 * <ul>
 *   <li>GROND: a natural floor (grass, moss, podzol, snow...) with 2 free blocks above;</li>
 *   <li>STEEN: a rock floor (stone kinds, dripstone, calcite...), caves included;</li>
 *   <li>BOOM: a natural floor right beside a trunk;</li>
 *   <li>WATER: a still water surface with land within 2 blocks (fished from the shore; 1.10.12: or from a pier/quay up to 3 higher);</li>
 *   <li>BLOEM: a natural floor with flowers within 6 blocks (the hives; the flowers are kept: the bees fly to them);</li>
 *   <li>KORAAL (U-AMBACHT2, coral diving): a sea floor block with still water above it and the surface 2-{@link #DUIK_DIEP} blocks
 *   up (walkability: a diver always comes up within a few blocks; never in a cave without air above). The spot is the water
 *   block on the floor.</li>
 * </ul>
 * A strict pass first; if it finds too little (an unusual zone), a relaxed pass (any solid non-building floor) fills up.
 */
public final class PlekWereld extends SavedData {
    public static final SavedDataType<PlekWereld> TYPE = GuhSavedData.tagType("vr_ambacht_plekken", PlekWereld::new, PlekWereld::load, PlekWereld::save);

    /** One spot: its id, the positions it moves between, and (hives) the flowers round it. */
    public record Spot(String id, Groep groep, List<BlockPos> posities, List<BlockPos> bloemen) {
    }

    record Opgelost(long anker, List<Spot> spots) {
    }

    private final Map<String, Opgelost> opgelost = new HashMap<>();

    public static PlekWereld get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The spots of a group, scanning when needed and possible (null: its area is not loaded yet / no anchor in this map). */
    @Nullable
    public List<Spot> spots(ServerLevel sl, Groep g) {
        BlockPos a = g.anker();
        if (a == null) {
            return null;
        }
        Opgelost o = opgelost.get(g.id());
        if (o != null && o.anker() == a.asLong()) {
            return o.spots();
        }
        if (!geladen(sl, a, g.straal())) {
            return null;
        }
        List<Spot> s = scan(sl, a, g);
        opgelost.put(g.id(), new Opgelost(a.asLong(), s));
        setDirty();
        return s;
    }

    /** Only what is cached now (no scan). */
    @Nullable
    public List<Spot> bekend(Groep g) {
        BlockPos a = g.anker();
        Opgelost o = opgelost.get(g.id());
        return o == null || a == null || o.anker() != a.asLong() ? null : o.spots();
    }

    /** Gametests: this world's scans as a tag, and replacing them (the real SavedData is shared by every test). */
    public CompoundTag saveVoorTest() {
        return save();
    }

    public void zetVoorTest(PlekWereld w) {
        opgelost.clear();
        opgelost.putAll(w.opgelost);
    }

    /** Dev: forget every scan (everything is scanned again when players come near). */
    public void vergeet() {
        opgelost.clear();
        setDirty();
    }

    static boolean geladen(Level l, BlockPos a, int r) {
        for (int dx = -r; dx <= r; dx += Math.max(8, r)) {
            for (int dz = -r; dz <= r; dz += Math.max(8, r)) {
                if (!l.hasChunk(SectionPos.blockToSectionCoord(a.getX() + dx), SectionPos.blockToSectionCoord(a.getZ() + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    // ---- the scan ------------------------------------------------------------------------------------------------------------

    /** Scans the terrain round {@code anker} for a group (public for the gametests). */
    public static List<Spot> scan(Level l, BlockPos anker, Groep g) {
        int nodig = g.aantal() * AmbachtPlekken.POSITIES;
        RandomSource r = RandomSource.create(g.id().hashCode() * 31L ^ anker.asLong());
        List<BlockPos> gevonden = new ArrayList<>();
        Map<BlockPos, List<BlockPos>> bloemen = new HashMap<>();
        // 1.10.13: a group whose anchor lies in one of the five caves only looks at that cave's floor (the anchor is floor - 3)
        GrotData.Grot grot = Grotten.op(anker.getX() + 0.5, anker.getY() + 3.5, anker.getZ() + 0.5);
        // 1.10.14: a group inside a small closed room (the secret IJsgrot dome) only takes floor spots a player can walk to
        java.util.Set<Long> binnen = BINNEN.contains(g.id()) ? bereikbaar(l, anker, g.straal()) : null;
        boolean honing = HONING.contains(g.id());   // 1.10.18: the honey ponds are fished in honey
        // 1.10.18: fishing positions may lie 2 blocks apart (only one node per spot stands at a time; shores are narrow strips)
        int afstand2 = g.zoek() == Zoek.WATER ? 4 : 9;
        for (int pass = 0; pass < 2 && gevonden.size() < nodig; pass++) {
            boolean streng = pass == 0;
            for (int poging = 0; poging < nodig * 40 && gevonden.size() < nodig; poging++) {
                double hoek = r.nextDouble() * Math.PI * 2, afstand = 3 + r.nextDouble() * (g.straal() - 3);
                int x = anker.getX() + (int) Math.round(Math.cos(hoek) * afstand), z = anker.getZ() + (int) Math.round(Math.sin(hoek) * afstand);
                BlockPos p = grot != null ? zoekGrotY(l, x, z, anker.getY(), g.zoek(), streng, grot)
                        : binnen != null ? zoekBinnenY(l, x, z, anker.getY(), g.zoek(), streng, binnen)
                        : zoekY(l, x, z, anker.getY(), g.zoek(), streng);
                if (p == null && honing) {
                    p = zoekHoningY(l, x, z, anker.getY());
                }
                if (p == null || teDichtbij(gevonden, p, afstand2)) {
                    continue;
                }
                if (g.zoek() == Zoek.BLOEM) {
                    List<BlockPos> b = bloemenBij(l, p, 6);
                    if (streng && b.size() < 2) {
                        continue;
                    }
                    bloemen.put(p, b);
                }
                gevonden.add(p);
            }
        }
        // 1.10.18: fishing groups whose water lies off-centre (a swamp at the edge of the circle, a brook far from the anchor) found
        // only a few spots by random tries (live: the Honingmoeras 1 of 6, zone 3 had 1 fishing spot in all): then every column of the
        // circle is tried, in a fixed shuffled order
        if (g.zoek() == Zoek.WATER && grot == null && binnen == null && gevonden.size() < nodig) {
            List<int[]> kolommen = new ArrayList<>();
            int s = g.straal();
            for (int dx = -s; dx <= s; dx++) {
                for (int dz = -s; dz <= s; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 >= 9 && d2 <= s * s) {
                        kolommen.add(new int[] {anker.getX() + dx, anker.getZ() + dz});
                    }
                }
            }
            for (int i = kolommen.size() - 1; i > 0; i--) {   // (deterministic: the group's own random)
                int j = r.nextInt(i + 1);
                int[] t = kolommen.get(i);
                kolommen.set(i, kolommen.get(j));
                kolommen.set(j, t);
            }
            for (int[] k : kolommen) {
                if (gevonden.size() >= nodig) {
                    break;
                }
                BlockPos p = zoekY(l, k[0], k[1], anker.getY(), Zoek.WATER, true);
                if (p == null && honing) {
                    p = zoekHoningY(l, k[0], k[1], anker.getY());
                }
                if (p != null && !teDichtbij(gevonden, p, afstand2)) {
                    gevonden.add(p);
                }
            }
        }
        // spot i gets positions i*POSITIES.. (the list is in random order, so a spot wanders over the whole group area)
        int n = Math.max(gevonden.isEmpty() ? 0 : 1, gevonden.size() / AmbachtPlekken.POSITIES);
        n = Math.min(n, g.aantal());
        List<Spot> uit = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<BlockPos> pos = new ArrayList<>();
            for (int k = i; k < gevonden.size() && pos.size() < AmbachtPlekken.POSITIES; k += n) {
                pos.add(gevonden.get(k));
            }
            List<BlockPos> bl = new ArrayList<>();
            for (BlockPos p : pos) {
                bl.addAll(bloemen.getOrDefault(p, List.of()));
            }
            uit.add(new Spot(g.id() + "_" + i, g, List.copyOf(pos), List.copyOf(bl.subList(0, Math.min(8, bl.size())))));
        }
        return uit;
    }

    private static boolean teDichtbij(List<BlockPos> l, BlockPos p, int afstand2) {
        for (BlockPos q : l) {
            if (q.distSqr(p) < afstand2) {
                return true;
            }
        }
        return false;
    }

    /** Coral diving: the deepest a dive spot lies under the surface. */
    public static final int DUIK_DIEP = 8;

    /**
     * 1.10.13: a cave spot: searched from the bottom up, only from the floor - 2 to the floor + 3 (anchor + 1..6), and only inside the
     * cave's box. Before, the normal top-down scan (anchor + 20 down) found air pockets in the rock above the cave first: the ore of the
     * Zwamgrot's second spot sat 8-14 blocks above the floor, inside the ceiling, where nobody could reach it.
     */
    @Nullable
    static BlockPos zoekGrotY(Level l, int x, int z, int ankerY, Zoek zoek, boolean streng, GrotData.Grot grot) {
        if (!grot.bevat(x + 0.5, ankerY + 3.5, z + 0.5)) {
            return null;
        }
        for (int y = ankerY + 1; y <= ankerY + 6; y++) {
            if (!grot.bevat(x + 0.5, y + 0.5, z + 0.5)) {
                continue;
            }
            BlockPos p = zoekY(l, x, z, y, y, zoek, streng);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    /** 1.10.18: fishing groups in honey (the honey ponds in the Kruinzee are honey blocks, not water: 0 spots on live). */
    public static final Set<String> HONING = Set.of("z3_vis_honingvijver");

    /** 1.10.18: a honey block with room above: a honey-fishing spot (the node replaces it; the honey comes back when it moves). */
    public static boolean honing(Level l, BlockPos p) {
        return l.getBlockState(p).is(Blocks.HONEY_BLOCK) && vrij(l, p.above()) && vrij(l, p.above(2));
    }

    @Nullable
    static BlockPos zoekHoningY(Level l, int x, int z, int ankerY) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = ankerY + 20; y >= ankerY - 30; y--) {
            m.set(x, y, z);
            if (honing(l, m)) {
                return m.immutable();
            }
        }
        return null;
    }

    /**
     * 1.10.14: groups inside a small closed room whose anchor is the free block above its floor. The normal top-down scan found the
     * roof of the secret IJsgrot (zone 2) and a sealed hollow under its floor: on live none of its 12 positions could be reached.
     */
    public static final Set<String> BINNEN = Set.of("z2_mijn_ijsgrot");

    /** 1.10.14: a room spot: floor - 1 .. floor + 1 around the anchor height, and only where a player can walk from the anchor. */
    @Nullable
    static BlockPos zoekBinnenY(Level l, int x, int z, int ankerY, Zoek zoek, boolean streng, java.util.Set<Long> bereik) {
        for (int y = ankerY - 1; y <= ankerY + 1; y++) {
            BlockPos p = zoekY(l, x, z, y, y, zoek, streng);
            if (p != null && bereik.contains(p.asLong())) {
                return p;
            }
        }
        return null;
    }

    /**
     * 1.10.14: every open block a player can reach from the anchor without breaking anything (air, plants, carpets, scaffolding,
     * water), within the group radius and from 3 below to 6 above the anchor. Starts at the open blocks right round the anchor.
     */
    static java.util.Set<Long> bereikbaar(Level l, BlockPos anker, int straal) {
        java.util.Set<Long> gezien = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> rij = new java.util.ArrayDeque<>();
        for (BlockPos p : BlockPos.betweenClosed(anker.offset(-2, -1, -2), anker.offset(2, 3, 2))) {
            if (open(l, p) && gezien.add(p.asLong())) {
                rij.add(p.immutable());
            }
        }
        int r = straal + 2;
        while (!rij.isEmpty() && gezien.size() < 200_000) {
            BlockPos p = rij.poll();
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                BlockPos q = p.relative(d);
                if (Math.abs(q.getX() - anker.getX()) > r || Math.abs(q.getZ() - anker.getZ()) > r || q.getY() < anker.getY() - 3
                        || q.getY() > anker.getY() + 6 || gezien.contains(q.asLong()) || !open(l, q)) {
                    continue;
                }
                gezien.add(q.asLong());
                rij.add(q);
            }
        }
        return gezien;
    }

    private static boolean open(Level l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        return s.isAir() || s.is(Blocks.SCAFFOLDING) || s.getCollisionShape(l, p).isEmpty();
    }

    /** The position of a spot in this column near {@code ankerY} (the free block above the floor; WATER: the water block). */
    @Nullable
    static BlockPos zoekY(Level l, int x, int z, int ankerY, Zoek zoek, boolean streng) {
        return zoekY(l, x, z, ankerY + 20, ankerY - 30, zoek, streng);
    }

    /** The highest spot position in this column from {@code hoog} down to {@code laag} (both included). */
    @Nullable
    static BlockPos zoekY(Level l, int x, int z, int hoog, int laag, Zoek zoek, boolean streng) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = hoog; y >= laag; y--) {
            m.set(x, y, z);
            BlockState onder = l.getBlockState(m.below());
            if (zoek == Zoek.KORAAL) {
                if (l.getBlockState(m).is(Blocks.WATER) && l.getFluidState(m).isSource() && onder.isCollisionShapeFullBlock(l, m.below())
                        && onder.getFluidState().isEmpty() && (!streng || natuur(onder))) {
                    int diep = oppervlak(l, m);
                    if (diep >= 2 && diep <= DUIK_DIEP) {
                        return m.immutable();
                    }
                }
                continue;
            }
            if (zoek == Zoek.WATER) {
                FluidState f = l.getFluidState(m);
                if (f.is(FluidTags.WATER) && f.isSource() && l.getBlockState(m.above()).isAir() && l.getBlockState(m).is(Blocks.WATER) && oever(l, m)) {
                    return m.immutable();
                }
                if (wak(l, m)) {
                    return m.immutable();   // 1.10.13: a frozen lake: fished through a hole in the ice
                }
                continue;
            }
            if (!vrij(l, m) || !vrij(l, m.above()) || !onder.isCollisionShapeFullBlock(l, m.below()) || !l.getFluidState(m).isEmpty()) {
                continue;
            }
            boolean ok = switch (zoek) {
                case STEEN -> streng ? steen(onder) : natuur(onder);
                case GROND, BLOEM -> streng ? grond(onder) : natuur(onder);
                case BOOM -> (streng ? grond(onder) : natuur(onder)) && stamBij(l, m);
                default -> false;
            };
            if (ok) {
                return m.immutable();
            }
        }
        return null;
    }

    /**
     * 1.10.13: ice right on top of still water, with room above: an ice-fishing hole (the node replaces the ice; the ice comes back
     * when the spot moves). The Knuffelmeer in zone 2 froze over completely, so the open-water rule found 0 spots there.
     */
    public static boolean wak(Level l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        if (!s.is(Blocks.ICE) && !s.is(Blocks.PACKED_ICE) && !s.is(Blocks.BLUE_ICE) && !s.is(Blocks.FROSTED_ICE)) {
            return false;
        }
        FluidState onder = l.getFluidState(p.below());
        return onder.is(FluidTags.WATER) && onder.isSource() && vrij(l, p.above()) && vrij(l, p.above(2));
    }

    private static boolean vrij(Level l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty() && s.getCollisionShape(l, p).isEmpty());
    }

    private static final Set<Block> GROND = Set.of(Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.ROOTED_DIRT,
            Blocks.DIRT, Blocks.SNOW_BLOCK, Blocks.PALE_MOSS_BLOCK, Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS, Blocks.CLAY);
    private static final Set<Block> PAD = Set.of(Blocks.DIRT_PATH, Blocks.GRAVEL, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.STONE_BRICKS,
            Blocks.MOSSY_STONE_BRICKS, Blocks.BRICKS, Blocks.SMOOTH_STONE, Blocks.POLISHED_ANDESITE, Blocks.POLISHED_GRANITE, Blocks.POLISHED_DIORITE,
            Blocks.HAY_BLOCK, Blocks.COARSE_DIRT, Blocks.PACKED_MUD, Blocks.MUD_BRICKS, Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.QUARTZ_BLOCK,
            Blocks.CAMPFIRE, Blocks.FARMLAND, Blocks.GLASS, Blocks.TINTED_GLASS, Blocks.BOOKSHELF, Blocks.CRAFTING_TABLE);

    static boolean grond(BlockState s) {
        return GROND.contains(s.getBlock());
    }

    static boolean steen(BlockState s) {
        if (s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.STONE_ORE_REPLACEABLES) || s.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return true;
        }
        Block b = s.getBlock();
        return b == Blocks.DRIPSTONE_BLOCK || b == Blocks.CALCITE || b == Blocks.AMETHYST_BLOCK || b == Blocks.SMOOTH_BASALT || b == Blocks.BASALT
                || b == Blocks.PACKED_ICE || b == Blocks.BLUE_ICE || b == Blocks.COBBLED_DEEPSLATE || b == Blocks.TERRACOTTA;
    }

    /** Any natural solid floor: not a path, not a building (planks, slabs, stairs, wool, glass...), not leaves. */
    static boolean natuur(BlockState s) {
        return !PAD.contains(s.getBlock()) && !s.is(BlockTags.PLANKS) && !s.is(BlockTags.SLABS) && !s.is(BlockTags.STAIRS) && !s.is(BlockTags.WOOL)
                && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS) && !s.is(BlockTags.FENCES) && !s.is(BlockTags.WALLS)
                && !s.is(BlockTags.TERRACOTTA) && !s.is(BlockTags.DOORS) && !s.is(BlockTags.TRAPDOORS) && !s.isAir();
    }

    private static boolean stamBij(Level l, BlockPos p) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) == 0) {
                    continue;
                }
                for (int dy = 0; dy <= 2; dy++) {
                    BlockState s = l.getBlockState(p.offset(dx, dy, dz));
                    // (1.10.13: the crystal trees of the Kristalbos have trunks of guh_kristal_blok)
                    if (s.is(BlockTags.LOGS) || s.is(Blocks.MUSHROOM_STEM) || s.is(nl.juiced.guhs.registry.ModBlocks.GUH_KRISTAL_BLOK.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** How many water blocks are above this water block until air (0: none / no open surface within DUIK_DIEP + 2). */
    static int oppervlak(Level l, BlockPos p) {
        for (int dy = 1; dy <= DUIK_DIEP + 2; dy++) {
            BlockPos q = p.above(dy);
            BlockState s = l.getBlockState(q);
            if (s.isAir()) {
                return dy;
            }
            if (!s.is(Blocks.WATER)) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Land to fish from beside this water block: a full block at the water line (+0..1) with room above, or (1.10.12) a standable
     * floor up to {@link #OEVER_HOOG} above the water (a pier, a quay: planks and slabs too) with 2 free blocks above; the water itself
     * must be open up to that height (never a spot under a pier deck).
     */
    static boolean oever(Level l, BlockPos p) {
        for (int k = 2; k <= OEVER_HOOG; k++) {
            if (!vrij(l, p.above(k))) {
                return false;
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                for (int dy = 0; dy <= OEVER_HOOG; dy++) {
                    BlockPos q = p.offset(dx, dy, dz);
                    BlockState s = l.getBlockState(q);
                    if (!s.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (dy <= 1 && s.isCollisionShapeFullBlock(l, q) && vrij(l, q.above())) {
                        return true;
                    }
                    if (!s.getCollisionShape(l, q).isEmpty() && !s.is(BlockTags.LEAVES) && vrij(l, q.above()) && vrij(l, q.above(2))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 1.10.12: how high above the water a pier or quay may lie and still count as a shore. */
    public static final int OEVER_HOOG = 3;

    static List<BlockPos> bloemenBij(Level l, BlockPos p, int r) {
        List<BlockPos> uit = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos q = p.offset(dx, dy, dz);
                    BlockState s = l.getBlockState(q);
                    if (s.is(BlockTags.FLOWERS) || s.is(Blocks.PINK_PETALS) || s.is(Blocks.FLOWERING_AZALEA) || s.is(Blocks.SPORE_BLOSSOM)) {
                        uit.add(q.immutable());
                    }
                }
            }
        }
        return uit;
    }

    // ---- saving --------------------------------------------------------------------------------------------------------------

    /**
     * 1.10.12: the scan rules' version. Loading an older save drops the cached non-hive groups that found fewer spots than they want,
     * so they are scanned again with the new rules (the shore rule found nothing at piers and high quays). Complete groups and hive
     * meadows (players' hives are keyed on spot ids) keep their spots.
     * 1.10.13 (version 3): the cave groups ({@code grot_*}) are scanned again on the cave floor (see {@link #zoekGrotY}); their old
     * nodes go back to the original block by themselves ({@code Grondstoffen.bijwerk}: the position no longer belongs to the spot).
     * Incomplete non-hive groups are dropped once more too: frozen lakes ({@link #wak}) and the Kristalbos (crystal trunks) find
     * spots now. Nodes of spots that no longer exist are cleaned up by {@code Grondstoffen.opruimen}.
     * 1.10.14 (version 4): the groups in {@link #BINNEN} (the IJsgrot) are scanned again on their floor.
     * 1.10.18 (version 5): incomplete non-hive groups once more (fishing: the full-circle pass, honey ponds, the Mosvoet brook).
     */
    public static final int VERSIE = 5;
    static final String VERSIE_SLEUTEL = "_versie";

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt(VERSIE_SLEUTEL, VERSIE);
        opgelost.forEach((id, o) -> {
            CompoundTag g = new CompoundTag();
            g.putLong("anker", o.anker());
            ListTag spots = new ListTag();
            for (Spot s : o.spots()) {
                CompoundTag c = new CompoundTag();
                c.put("pos", lijst(s.posities()));
                c.put("bloem", lijst(s.bloemen()));
                spots.add(c);
            }
            g.put("spots", spots);
            t.put(id, g);
        });
        return t;
    }

    private static ListTag lijst(List<BlockPos> l) {
        ListTag t = new ListTag();
        for (BlockPos p : l) {
            t.add(LongTag.valueOf(p.asLong()));
        }
        return t;
    }

    private static List<BlockPos> posities(ListTag t) {
        List<BlockPos> l = new ArrayList<>();
        for (net.minecraft.nbt.Tag x : t) {
            if (x instanceof LongTag lt) {
                l.add(BlockPos.of(lt.longValue()));
            }
        }
        return l;
    }

    static PlekWereld load(CompoundTag t) {
        PlekWereld w = new PlekWereld();
        int versie = t.getIntOr(VERSIE_SLEUTEL, 1);
        for (String id : t.keySet()) {
            Groep g = AmbachtPlekken.groep(id);
            if (g == null) {
                continue;
            }
            CompoundTag c = t.getCompoundOrEmpty(id);
            ListTag spots = c.getListOrEmpty("spots");
            List<Spot> l = new ArrayList<>();
            for (int i = 0; i < spots.size(); i++) {
                CompoundTag s = spots.getCompoundOrEmpty(i);
                l.add(new Spot(id + "_" + i, g, posities(s.getListOrEmpty("pos")), posities(s.getListOrEmpty("bloem"))));
            }
            if (versie < 5 && g.soort() != NodeSoort.KORF && l.size() < g.aantal()) {
                continue;   // 1.10.12 / 1.10.13: scanned again with the new rules (when a player comes near)
            }
            if (versie < 4 && BINNEN.contains(id)) {
                continue;   // 1.10.14: the IJsgrot scanned again on its floor
            }
            if (versie < 3 && g.soort() != NodeSoort.KORF && id.startsWith("grot_")) {
                continue;   // 1.10.13: cave groups scanned again on the cave floor
            }
            w.opgelost.put(id, new Opgelost(c.getLongOr("anker", 0L), l));
        }
        return w;
    }
}
