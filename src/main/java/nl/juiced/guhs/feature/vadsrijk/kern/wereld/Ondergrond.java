package nl.juiced.guhs.feature.vadsrijk.kern.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * What a chunk without a map tile gets: nothing (void), except the kern <b>proeftuin</b> (x/z 3000..3191), which is stamped
 * here by code: a flat pink-green plain with a pond, a little Knuffelkamp (tents + an unlit campfire = checkpoint) and the boss
 * arena of the Grauwe Klodderreus. Deterministic (pure function of the position).
 */
public final class Ondergrond {
    public static final int X0 = 3000, Z0 = 3000, MAAT = 192;
    /** Proeftuin landmarks. */
    public static final BlockPos KAMP = new BlockPos(3096, Vadsrijk.GROND + 1, 3160), KAMPVUUR = new BlockPos(3096, Vadsrijk.GROND + 1, 3140),
            ARENA = new BlockPos(3096, Vadsrijk.GROND + 1, 3060), ARENA_DEUR = new BlockPos(3096, Vadsrijk.GROND + 1, 3084),
            ARENA_BORD = new BlockPos(3100, Vadsrijk.GROND + 1, 3086), VIJVER = new BlockPos(3060, Vadsrijk.GROND, 3125);
    public static final int ARENA_STRAAL = 20;

    private Ondergrond() {
    }

    public static boolean inProeftuin(int x, int z) {
        return x >= X0 && x < X0 + MAAT && z >= Z0 && z < Z0 + MAAT;
    }

    public static void vul(WorldGenLevel level, ChunkPos cp) {
        if (nl.juiced.guhs.feature.vadsrijk.indeling.ZeeVulling.vul(level, cp)) {   // T-WERELD: the sea between the zones
            return;
        }
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        if (!inProeftuin(x0, z0) && !inProeftuin(x0 + 15, z0 + 15)) {
            return;
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int g = Vadsrijk.GROND;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (!inProeftuin(x, z)) {
                    continue;
                }
                for (int y = g - 8; y < g; y++) {
                    level.setBlock(m.set(x, y, z), y < g - 3 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
                }
                double vd = Math.hypot(x - VIJVER.getX(), z - VIJVER.getZ());
                double ad = Math.hypot(x - ARENA.getX(), z - ARENA.getZ());
                if (vd < 9) {
                    level.setBlock(m.set(x, g, z), Blocks.WATER.defaultBlockState(), 2);
                    level.setBlock(m.set(x, g - 1, z), vd < 6 ? Blocks.WATER.defaultBlockState() : Blocks.SAND.defaultBlockState(), 2);
                    continue;
                }
                if (ad <= ARENA_STRAAL + 0.5) {
                    boolean ring = ((int) Math.floor(ad)) % 6 == 0;
                    level.setBlock(m.set(x, g, z), ring ? Blocks.PINK_TERRACOTTA.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                    continue;
                }
                if (ad <= ARENA_STRAAL + 2.5) {   // the arena wall, open to the south (the door)
                    level.setBlock(m.set(x, g, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    boolean deur = Math.abs(x - ARENA.getX()) <= 3 && z > ARENA.getZ();
                    if (!deur) {
                        level.setBlock(m.set(x, g + 1, z), Blocks.PINK_CONCRETE.defaultBlockState(), 2);
                        if ((x + z) % 4 == 0) {
                            level.setBlock(m.set(x, g + 2, z), Blocks.PINK_CONCRETE.defaultBlockState(), 2);
                            level.setBlock(m.set(x, g + 3, z), Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), 2);
                        }
                    }
                    continue;
                }
                level.setBlock(m.set(x, g, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                int h = hash(x, z);
                BlockState bloem = switch (h % 23) {
                    case 0 -> Blocks.PINK_TULIP.defaultBlockState();
                    case 1 -> Blocks.ALLIUM.defaultBlockState();
                    case 2, 3 -> Blocks.SHORT_GRASS.defaultBlockState();
                    case 4 -> Blocks.PINK_PETALS.defaultBlockState();
                    default -> null;
                };
                if (bloem != null && Math.abs(x - KAMP.getX()) > 14 && Math.abs(z - KAMP.getZ()) > 14) {
                    level.setBlock(m.set(x, g + 1, z), bloem, 2);
                }
            }
        }
        // the Knuffelkamp: tents and the campfire (only the parts inside this chunk)
        tent(level, cp, KAMP.getX() - 10, KAMP.getZ() - 4, Blocks.PINK_WOOL.defaultBlockState());
        tent(level, cp, KAMP.getX() + 6, KAMP.getZ() - 4, Blocks.WHITE_WOOL.defaultBlockState());
        tent(level, cp, KAMP.getX() - 2, KAMP.getZ() + 6, Blocks.MAGENTA_WOOL.defaultBlockState());
        zet(level, cp, KAMPVUUR, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        for (int i = -1; i <= 1; i += 2) {
            zet(level, cp, KAMPVUUR.offset(i * 2, 0, 0), Blocks.OAK_LOG.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Z));
        }
    }

    private static void tent(WorldGenLevel level, ChunkPos cp, int x, int z, BlockState wol) {
        int g = Vadsrijk.GROND + 1;
        for (int dz = 0; dz < 5; dz++) {
            for (int h = 0; h < 3; h++) {
                zet(level, cp, new BlockPos(x + h, g + h, z + dz), wol);
                zet(level, cp, new BlockPos(x + 4 - h, g + h, z + dz), wol);
            }
        }
    }

    private static void zet(WorldGenLevel level, ChunkPos cp, BlockPos p, BlockState s) {
        if (p.getX() >= cp.getMinBlockX() && p.getX() <= cp.getMaxBlockX() && p.getZ() >= cp.getMinBlockZ() && p.getZ() <= cp.getMaxBlockZ()) {
            level.setBlock(p, s, 2);
        }
    }

    private static int hash(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        h ^= h >>> 13;
        return Math.abs(h * 0x5bd1e995);
    }

    /** Is this in the proeftuin area of {@link Gebieden}? */
    public static boolean proeftuin(BlockPos p) {
        return inProeftuin(p.getX(), p.getZ());
    }
}
