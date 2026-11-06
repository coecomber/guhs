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
 *   <li>WATER: a still water surface with land within 2 blocks (fished from the shore);</li>
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
        for (int pass = 0; pass < 2 && gevonden.size() < nodig; pass++) {
            boolean streng = pass == 0;
            for (int poging = 0; poging < nodig * 40 && gevonden.size() < nodig; poging++) {
                double hoek = r.nextDouble() * Math.PI * 2, afstand = 3 + r.nextDouble() * (g.straal() - 3);
                int x = anker.getX() + (int) Math.round(Math.cos(hoek) * afstand), z = anker.getZ() + (int) Math.round(Math.sin(hoek) * afstand);
                BlockPos p = zoekY(l, x, z, anker.getY(), g.zoek(), streng);
                if (p == null || teDichtbij(gevonden, p)) {
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

    private static boolean teDichtbij(List<BlockPos> l, BlockPos p) {
        for (BlockPos q : l) {
            if (q.distSqr(p) < 9) {
                return true;
            }
        }
        return false;
    }

    /** Coral diving: the deepest a dive spot lies under the surface. */
    public static final int DUIK_DIEP = 8;

    /** The position of a spot in this column near {@code ankerY} (the free block above the floor; WATER: the water block). */
    @Nullable
    static BlockPos zoekY(Level l, int x, int z, int ankerY, Zoek zoek, boolean streng) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = ankerY + 20; y >= ankerY - 30; y--) {
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
                    if (s.is(BlockTags.LOGS) || s.is(Blocks.MUSHROOM_STEM)) {
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

    private static boolean oever(Level l, BlockPos p) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos q = p.offset(dx, dy, dz);
                    BlockState s = l.getBlockState(q);
                    if (s.getFluidState().isEmpty() && s.isCollisionShapeFullBlock(l, q) && vrij(l, q.above())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

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

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
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

    private static PlekWereld load(CompoundTag t) {
        PlekWereld w = new PlekWereld();
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
            w.opgelost.put(id, new Opgelost(c.getLongOr("anker", 0L), l));
        }
        return w;
    }
}
