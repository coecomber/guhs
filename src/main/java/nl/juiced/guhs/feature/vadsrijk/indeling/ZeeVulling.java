package nl.juiced.guhs.feature.vadsrijk.indeling;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * T-WERELD: the procedural sea between the zones (WERELDINDELING §7.3). A chunk of the island sea (zone 1-5) WITHOUT a map tile
 * gets a sea floor and water up to y 62 here, so the zones need tiles only for their content and the sea never runs into the
 * void (the old invisible walls at the tile-grid edges are gone). Deep (floor 49, matching zone 1's deepened sea) out in the
 * open, shallow (floor 60, gravel) round zone 2's cloud sea. Deterministic: a pure function of the position.
 */
public final class ZeeVulling {
    private ZeeVulling() {
    }

    /** The island sea (zone 1-5 + margin): outside it a chunk without a tile stays void (proeftuin, far tests). */
    public static final int MIN_X = -1280, MAX_X = 3600, MIN_Z = -1920, MAX_Z = 1728;
    public static final int ZEE = 62, DIEP = 49, ONDIEP = 60;

    public static boolean inZee(int x, int z) {
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }

    /** Sea floor (top block y) at x/z. */
    public static int vloer(int x, int z) {
        var z2 = VadsrijkZones.Zone.Z2;
        double dx = Math.max(Math.max(z2.minX - x, x - z2.maxX), 0), dz = Math.max(Math.max(z2.minZ - z, z - z2.maxZ), 0);
        double d = Math.hypot(dx, dz);
        double t = Math.max(0, Math.min(1, 1 - d / 120.0));
        int h = hash(x >> 3, z >> 3) % 3 - 1;
        return (int) Math.round(DIEP + (ONDIEP - DIEP) * t * t * (3 - 2 * t)) + (t < 0.5 ? h : 0);
    }

    /** Fills this chunk with sea when it lies in the island sea; returns whether it did. */
    public static boolean vul(WorldGenLevel level, ChunkPos cp) {
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        if (!inZee(x0, z0) || !inZee(x0 + 15, z0 + 15)) {
            return false;
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState water = Blocks.WATER.defaultBlockState(), steen = Blocks.STONE.defaultBlockState();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int f = vloer(x, z);
                BlockState bodem = f >= 57 ? Blocks.GRAVEL.defaultBlockState()
                        : (hash(x, z) % 7 == 0 ? Blocks.SAND.defaultBlockState() : Blocks.LIGHT_BLUE_TERRACOTTA.defaultBlockState());
                for (int y = f - 4; y < f; y++) {
                    level.setBlock(m.set(x, y, z), steen, 2);
                }
                level.setBlock(m.set(x, f, z), bodem, 2);
                for (int y = f + 1; y <= ZEE; y++) {
                    level.setBlock(m.set(x, y, z), water, 2);
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

    private static int hash(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        h ^= h >>> 13;
        return Math.abs(h * 0x5bd1e995 % 1000003);
    }
}
