package nl.juiced.guhs.feature.vadsrijk.indeling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;

/**
 * T-WERELD: the procedural sea between the zones (WERELDINDELING §7.3). A chunk of the island sea (zone 1-5) WITHOUT a map tile
 * gets a sea floor and water up to y 62 here, so the zones need tiles only for their content and the sea never runs into the
 * void (the old invisible walls at the tile-grid edges are gone). Deterministic: a pure function of the position.
 * <p>U-OVERGANG: this is now the ONE open-sea model shared with the map tiles (tools/vadsrijk/zeenaad.py, same hashing and the
 * same double maths; change BOTH): the floor height and floor block ({@link #vloer}, {@link #materiaal}), zone 3's cloud banks
 * that continue past its tile box and thin out ({@link #wolkDikte}) and scattered rock stacks near the zones ({@link #rots}).
 * The seam pass tools/vadsrijk/overgang.py pulls every zone's edge tiles onto this model, so a tile edge and the chunk next to
 * it meet without an under-water cliff, a floor-colour edge or a cut-off cloud bank.
 */
public final class ZeeVulling {
    private ZeeVulling() {
    }

    /** The island sea (zone 1-5 + margin): outside it a chunk without a tile stays void (proeftuin, far tests). */
    public static final int MIN_X = -1280, MAX_X = 4672, MIN_Z = -1920, MAX_Z = 1728;   // U-Z6WERELD: MAX_X 3600 -> 4672 (zone 6 + its sea)
    public static final int ZEE = 62, DIEP = 49;

    public static boolean inZee(int x, int z) {
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }

    // ------------------------------------------------------------------------------------------------ the shared model
    /** zeenaad.hash01: a per-block hash in [0, 1). */
    static double hash01(long a, long b, int seed) {
        long v = (a * 73856093L) ^ (b * 19349663L) ^ ((long) seed * 83492791L);
        v = (v ^ (v >> 13)) * 1274126177L;
        return ((v ^ (v >> 16)) & 0xFFFF) / 65536.0;
    }

    /** zeenaad.ruis2: smooth 2D value noise in [0, 1). */
    static double ruis2(double x, double z, double cel, int seed) {
        double gx = Math.floor(x / cel), gz = Math.floor(z / cel);
        double fx = x / cel - gx, fz = z / cel - gz;
        long a = (long) gx, b = (long) gz;
        double sx = fx * fx * (3 - 2 * fx), sz = fz * fz * (3 - 2 * fz);
        return hash01(a, b, seed) * (1 - sx) * (1 - sz) + hash01(a + 1, b, seed) * sx * (1 - sz) + hash01(a, b + 1, seed) * (1 - sx) * sz
                + hash01(a + 1, b + 1, seed) * sx * sz;
    }

    /** Open-sea floor (top block y) at x/z: 47..51. */
    public static int vloer(int x, int z) {
        double n = ruis2(x, z, 56, 11);
        return DIEP + (int) Math.floor((n - 0.5) * 5.0 + 0.5);
    }

    /** Floor block kind: 0 light-blue terracotta, 1 sand, 2 gravel. */
    public static int materiaal(int x, int z) {
        double m = ruis2(x, z, 23, 12);
        return m < 0.30 ? 2 : m > 0.74 ? 1 : 0;
    }

    private static final int Z3_MIN_X = 1024, Z3_MIN_Z = -1024, Z3_MAX_X = 1727, Z3_MAX_Z = -129, STAM_X = 1404, STAM_Z = -576;

    static double kloofX(double z) {
        return 1066 + 20 * Math.sin(z / 70.0) + 9 * Math.sin(z / 23.0 + 1.3);
    }

    /** Thickness of zone 3's cloud sea at x/z (cloud blocks at y 62..62+t over open water; 0 = none). */
    public static int wolkDikte(int x, int z) {
        double r = Math.hypot(x - STAM_X, z - STAM_Z);
        if (r < 262) {
            return 0;
        }
        double n = ruis2(x, z, 46, 5) * 0.65 + ruis2(x, z, 17, 6) * 0.35;
        double dk = Math.abs(x - kloofX(z));
        double band = 40 + 16 * (2 * ruis2(x, z, 23, 7) - 1);   // the wobbly outer edge of the kloof's cloud cliffs
        double drempel = dk < 15 ? 9.0 : dk < band ? 0.30 : 0.52;
        double oost = Math.max(0, Math.min(1, (x - 1620) / 90.0));
        drempel = drempel + 0.22 * oost;
        double bx = Math.max(Math.max(Z3_MIN_X - (double) x, x - (Z3_MAX_X + 1.0)), 0), bz = Math.max(Math.max(Z3_MIN_Z - (double) z, z - (Z3_MAX_Z + 1.0)), 0);
        double uit = Math.max(0, Math.min(1, Math.hypot(bx, bz) / 70.0));
        drempel = drempel + 0.55 * uit;
        double dik = Math.max(0, Math.min(9, (n - drempel) * 26)) * (1 - 0.6 * oost);
        if (dk < band) {
            dik = dik * 1.6 + (dik > 0 ? 2 : 0);
        }
        dik = dik * (1 - 0.7 * uit);
        return (int) dik;
    }

    static int wolkSoort(int x, int z) {
        return ((int) (ruis2(x, z, 7, 9) * 4)) % 4;
    }

    // ------------------------------------------------------------------------------------------------ rocks
    private static final int[][] LAND = {VadsrijkZones.LAND_KAMP, VadsrijkZones.LAND_Z1, VadsrijkZones.LAND_Z2, VadsrijkZones.LAND_Z3,
            VadsrijkZones.LAND_Z4};
    /** Every tile box (zone 1 grid incl. the camp, the Splinterwand row, zone 2, 3, 4 + 5). */
    private static final int[][] TEGELVAKKEN = {{-704, -704, 767, 1215}, {-128, -768, 1023, -705}, {384, -1472, 1023, -769},
            {1024, -1024, 1727, -129}, {1728, -768, 3199, -257}};
    private static final int ROTS_CEL = 72;

    private static double doosAfstand(double x, double z, int[] b) {
        double dx = Math.max(Math.max(b[0] - x, x - b[2]), 0), dz = Math.max(Math.max(b[1] - z, z - b[3]), 0);
        return Math.hypot(dx, dz);
    }

    /** The rock of cell (cx, cz): {x, z, radius, top} or null (zeenaad.rots_in_cel). */
    static double[] rotsInCel(long cx, long cz) {
        if (hash01(cx, cz, 41) >= 0.45) {
            return null;
        }
        double x = cx * ROTS_CEL + 12 + hash01(cx, cz, 42) * 48, z = cz * ROTS_CEL + 12 + hash01(cx, cz, 43) * 48;
        double r = 3 + hash01(cx, cz, 44) * 5;
        int top = 63 + (int) (hash01(cx, cz, 45) * 8);
        double land = Double.MAX_VALUE, tegel = Double.MAX_VALUE;
        for (int[] b : LAND) {
            land = Math.min(land, doosAfstand(x, z, b));
        }
        if (land < 30 || land > 260) {
            return null;
        }
        for (int[] b : TEGELVAKKEN) {
            tegel = Math.min(tegel, doosAfstand(x, z, b));
        }
        if (tegel < r + 6) {
            return null;
        }
        return new double[] {x, z, r, top};
    }

    /** Top y of the rock stack at x/z, or -1 (a rock never leaves its own 72-block cell). */
    public static int rots(int x, int z) {
        double[] r = rotsInCel(Math.floorDiv(x, ROTS_CEL), Math.floorDiv(z, ROTS_CEL));
        if (r == null) {
            return -1;
        }
        double w = 1 + 0.5 * (ruis2(x, z, 5, 46) - 0.5);
        double d = Math.hypot(x - r[0], z - r[1]) * w / r[2];
        if (d >= 1) {
            return -1;
        }
        int g = vloer(x, z);
        int h = (int) Math.floor(g + (r[3] - g) * Math.pow(Math.max(0, 1 - d * d), 0.6) + 0.5);
        return h > g ? h : -1;
    }

    // ------------------------------------------------------------------------------------------------ filling a chunk
    private static Block guhs(String id, Block terug) {
        Block b = BuiltInRegistries.BLOCK.getValue(Guhs.id(id));
        return b == null || b == Blocks.AIR ? terug : b;
    }

    /** Fills this chunk with sea when it lies in the island sea; returns whether it did. */
    public static boolean vul(WorldGenLevel level, ChunkPos cp) {
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        if (!inZee(x0, z0) || !inZee(x0 + 15, z0 + 15)) {
            return false;
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState water = Blocks.WATER.defaultBlockState(), steen = Blocks.STONE.defaultBlockState();
        BlockState[] bodem = {Blocks.LIGHT_BLUE_TERRACOTTA.defaultBlockState(), Blocks.SAND.defaultBlockState(), Blocks.GRAVEL.defaultBlockState()};
        BlockState[] wolk = {Blocks.WHITE_WOOL.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState(),
                guhs("pluiswolblok", Blocks.WHITE_WOOL).defaultBlockState(), Blocks.WHITE_CONCRETE_POWDER.defaultBlockState()};
        BlockState[] rots = {steen, Blocks.ANDESITE.defaultBlockState(), Blocks.TUFF.defaultBlockState(), Blocks.CALCITE.defaultBlockState()};
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int f = vloer(x, z);
                for (int y = f - 4; y < f; y++) {
                    level.setBlock(m.set(x, y, z), steen, 2);
                }
                level.setBlock(m.set(x, f, z), bodem[materiaal(x, z)], 2);
                int r = rots(x, z);
                if (r > f) {
                    for (int y = f; y <= r; y++) {
                        int band = Math.floorMod((y + (int) (3 * Math.sin(y * 0.2 + x * 0.05))) / 3 + (int) (hash01(x, z, 47) * 1.5), rots.length);
                        level.setBlock(m.set(x, y, z), rots[band], 2);
                    }
                    for (int y = r + 1; y <= ZEE; y++) {
                        level.setBlock(m.set(x, y, z), water, 2);
                    }
                    continue;
                }
                for (int y = f + 1; y <= ZEE; y++) {
                    level.setBlock(m.set(x, y, z), water, 2);
                }
                int t = wolkDikte(x, z);
                if (t >= 1) {
                    BlockState w = wolk[wolkSoort(x, z)];
                    for (int y = ZEE; y <= ZEE + t; y++) {
                        level.setBlock(m.set(x, y, z), w, 2);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Chunks with a map tile that has no sea of its own (zone 5 Hoogvads floats above the sea east of zone 4): the sea first,
     * then the tile over it. Only for the zone-5 tile columns outside zone 4 (zone 5's content starts at y 150).
     */
    public static boolean zeeOnderTegel(ChunkPos cp) {
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        return VadsrijkZones.Zone.Z5.inVak(x0, z0) && !VadsrijkZones.Zone.Z4.inVak(x0, z0);
    }
}
