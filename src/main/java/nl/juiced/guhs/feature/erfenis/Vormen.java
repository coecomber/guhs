package nl.juiced.guhs.feature.erfenis;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * De Erfenis (POC): the look of every building, as simple shapes in code. Pure (no level): the server stamps them
 * ({@link Stempel}), the client uses {@link #hoogte} for its ghosts. Local coordinates: (lx, y, lz) with 0 &lt;= lx, lz &lt; maat
 * and y = 0 the grass, y &gt;= 1 above it; the front is +z (south) at rotation 0. {@link #vorm} turns them.
 * <p>
 * The material ladder (stadhuis, walls, and the levels of the others): L1 oak, L2 cobblestone, L3 iron, L4 gold,
 * L5 diamond; every level also grows (higher walls, towers, a flag).
 */
public final class Vormen {
    /** Receives one block of a shape (already turned): x, z on the plot relative to the building's min corner. */
    @FunctionalInterface
    public interface Zet {
        void zet(int x, int y, int z, BlockState state);
    }

    private Vormen() {
    }

    public static BlockState mat(int level) {
        return switch (Math.max(1, Math.min(5, level))) {
            case 1 -> Blocks.OAK_PLANKS.defaultBlockState();
            case 2 -> Blocks.COBBLESTONE.defaultBlockState();
            case 3 -> Blocks.IRON_BLOCK.defaultBlockState();
            case 4 -> Blocks.GOLD_BLOCK.defaultBlockState();
            default -> Blocks.DIAMOND_BLOCK.defaultBlockState();
        };
    }

    static BlockState hoek(int level) {
        return switch (Math.max(1, Math.min(5, level))) {
            case 1 -> Blocks.OAK_LOG.defaultBlockState();
            case 2 -> Blocks.STONE_BRICKS.defaultBlockState();
            case 3 -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case 4 -> Blocks.RAW_GOLD_BLOCK.defaultBlockState();
            default -> Blocks.QUARTZ_PILLAR.defaultBlockState();
        };
    }

    static BlockState dakSlab(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> Blocks.CHERRY_SLAB.defaultBlockState();
            case 2 -> Blocks.COBBLESTONE_SLAB.defaultBlockState();
            default -> Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
        };
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /**
     * Gives every block of this building's shape at this level to {@code zet}, turned by {@code rot} quarter turns.
     * Level 0 is the foundation of a building whose first build runs; {@code steiger} adds scaffolding (an upgrade runs).
     */
    public static void vorm(GebouwType type, int level, int rot, boolean steiger, Zet zet) {
        int m = type.maat;
        Rotation r = switch (rot & 3) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        Zet gedraaid = (lx, y, lz, st) -> {
            int x, z;
            switch (rot & 3) {
                case 1 -> {
                    x = m - 1 - lz;
                    z = lx;
                }
                case 2 -> {
                    x = m - 1 - lx;
                    z = m - 1 - lz;
                }
                case 3 -> {
                    x = lz;
                    z = m - 1 - lx;
                }
                default -> {
                    x = lx;
                    z = lz;
                }
            }
            zet.zet(x, y, z, st.rotate(r));
        };
        if (level <= 0) {
            fundering(m, gedraaid);
            return;
        }
        switch (type) {
            case STADHUIS -> stadhuis(level, gedraaid);
            case MOLEN -> molen(level, gedraaid);
            case SCHUURTJE -> schuurtje(level, gedraaid);
            case MUUR -> muur(level, gedraaid);
            case BOOMPJE -> boompje(gedraaid);
        }
        if (steiger) {
            int h = hoogte(type, level, false);
            if (m == 1) {
                gedraaid.zet(0, h + 1, 0, s(Blocks.SCAFFOLDING));
                gedraaid.zet(0, h + 2, 0, s(Blocks.SCAFFOLDING));
            } else {
                for (int y = 1; y <= Math.min(h + 1, Plot.MAX_HOOGTE); y++) {
                    gedraaid.zet(0, y, 0, s(Blocks.SCAFFOLDING));
                    gedraaid.zet(m - 1, y, 0, s(Blocks.SCAFFOLDING));
                    gedraaid.zet(0, y, m - 1, s(Blocks.SCAFFOLDING));
                    gedraaid.zet(m - 1, y, m - 1, s(Blocks.SCAFFOLDING));
                }
            }
        }
    }

    /** The highest y (above the grass) this shape reaches. */
    public static int hoogte(GebouwType type, int level, boolean steiger) {
        int[] max = {1};
        vorm(type, level, 0, steiger, (x, y, z, st) -> max[0] = Math.max(max[0], y));
        return max[0];
    }

    // =====================================================================================================================

    private static void fundering(int m, Zet z) {
        for (int x = 0; x < m; x++) {
            for (int q = 0; q < m; q++) {
                z.zet(x, 0, q, s(Blocks.COARSE_DIRT));
            }
        }
        if (m == 1) {
            z.zet(0, 1, 0, s(Blocks.SCAFFOLDING));
            return;
        }
        for (int y = 1; y <= 3; y++) {
            z.zet(0, y, 0, s(Blocks.SCAFFOLDING));
            z.zet(m - 1, y, 0, s(Blocks.SCAFFOLDING));
            z.zet(0, y, m - 1, s(Blocks.SCAFFOLDING));
            z.zet(m - 1, y, m - 1, s(Blocks.SCAFFOLDING));
        }
        z.zet(m / 2, 1, m / 2, s(Blocks.OAK_PLANKS));
        z.zet(m / 2, 2, m / 2, s(Blocks.OAK_SLAB));
    }

    /** 5x5: a house of 5x4 with a porch row in front (the sign of opa-guh); higher, towers and a flag per level. */
    private static void stadhuis(int l, Zet z) {
        int h = l + 1;
        BlockState mat = mat(l), hoek = hoek(l), glas = s(Blocks.PINK_STAINED_GLASS);
        for (int y = 1; y <= h; y++) {
            for (int x = 0; x < 5; x++) {
                for (int q = 0; q < 4; q++) {
                    boolean rand = x == 0 || x == 4 || q == 0 || q == 3;
                    boolean isHoek = (x == 0 || x == 4) && (q == 0 || q == 3);
                    z.zet(x, y, q, !rand ? s(Blocks.AIR) : isHoek ? hoek : mat);
                }
            }
        }
        z.zet(2, 1, 3, s(Blocks.AIR));   // the door (two high)
        z.zet(2, 2, 3, s(Blocks.AIR));
        if (h >= 3) {
            for (int q = 1; q <= 2; q++) {
                z.zet(0, 2, q, glas);
                z.zet(4, 2, q, glas);
            }
            z.zet(1, 2, 3, glas);
            z.zet(3, 2, 3, glas);
            z.zet(2, 2, 0, glas);
        }
        // the roof
        for (int x = 0; x < 5; x++) {
            for (int q = 0; q < 4; q++) {
                z.zet(x, h + 1, q, l <= 2 ? dakSlab(l) : (x == 0 || x == 4 || q == 0 || q == 3) ? mat : s(Blocks.PINK_CONCRETE));
            }
        }
        if (l == 2) {   // a chimney
            z.zet(3, h + 2, 1, s(Blocks.BRICKS));
            z.zet(3, h + 3, 1, s(Blocks.BRICKS));
        }
        if (l >= 3) {
            int toren = l - 2;
            for (int[] c : new int[][] {{0, 0}, {4, 0}, {0, 3}, {4, 3}}) {
                for (int t = 1; t <= toren; t++) {
                    z.zet(c[0], h + 1 + t, c[1], hoek);
                }
                z.zet(c[0], h + 2 + toren, c[1], s(Blocks.PINK_WOOL));
            }
            int paal = l;   // the flag
            for (int t = 1; t <= paal; t++) {
                z.zet(2, h + 1 + t, 1, s(Blocks.CHERRY_FENCE));
            }
            z.zet(3, h + 1 + paal, 1, s(Blocks.PINK_WOOL));
            z.zet(3, h + paal, 1, s(Blocks.PINK_WOOL));
            if (l >= 4) {
                z.zet(4, h + 1 + paal, 1, s(Blocks.MAGENTA_WOOL));
            }
            if (l >= 5) {
                z.zet(2, h + 2 + paal, 1, s(Blocks.PEARLESCENT_FROGLIGHT));
            }
        }
        // the porch: planks, a lantern, flowers, and opa-guh's sign
        for (int x = 0; x < 5; x++) {
            z.zet(x, 0, 4, s(Blocks.CHERRY_PLANKS));
        }
        z.zet(0, 1, 4, s(Blocks.POTTED_PINK_TULIP));
        z.zet(1, 1, 4, s(Blocks.LANTERN));
        z.zet(3, 1, 4, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 0));
        z.zet(4, 1, 4, s(Blocks.POTTED_ALLIUM));
    }

    /** 3x3: a base, a tower and four (later eight) sails at the front. */
    private static void molen(int l, Zet z) {
        BlockState mat = mat(l);
        for (int x = 0; x < 3; x++) {
            for (int q = 0; q < 3; q++) {
                z.zet(x, 1, q, mat);
            }
        }
        int top = 3 + l;
        for (int y = 2; y <= top; y++) {
            z.zet(1, y, 1, hoek(l));
        }
        z.zet(1, top + 1, 1, s(Blocks.PINK_WOOL));
        BlockState zeil = s(l == 1 ? Blocks.WHITE_WOOL : l == 2 ? Blocks.PINK_WOOL : Blocks.MAGENTA_WOOL);
        z.zet(1, top, 2, s(Blocks.STRIPPED_CHERRY_WOOD));
        z.zet(0, top, 2, zeil);
        z.zet(2, top, 2, zeil);
        z.zet(1, top + 1, 2, zeil);
        z.zet(1, top - 1, 2, zeil);
        if (l >= 2) {
            z.zet(0, top + 1, 2, zeil);
            z.zet(2, top - 1, 2, zeil);
        }
        if (l >= 3) {
            z.zet(2, top + 1, 2, zeil);
            z.zet(0, top - 1, 2, zeil);
        }
        z.zet(0, 2, 0, s(Blocks.HAY_BLOCK));
    }

    /** 3x3: a little shed with a barrel inside; one higher per level. */
    private static void schuurtje(int l, Zet z) {
        int h = 1 + l;
        BlockState mat = mat(l), hoek = hoek(l);
        for (int y = 1; y <= h; y++) {
            for (int x = 0; x < 3; x++) {
                for (int q = 0; q < 3; q++) {
                    boolean midden = x == 1 && q == 1;
                    boolean isHoek = (x == 0 || x == 2) && (q == 0 || q == 2);
                    z.zet(x, y, q, midden ? s(Blocks.AIR) : isHoek ? hoek : mat);
                }
            }
        }
        z.zet(1, 1, 2, s(Blocks.AIR));
        z.zet(1, 2, 2, s(Blocks.AIR));
        z.zet(1, 1, 1, s(Blocks.BARREL));
        for (int x = 0; x < 3; x++) {
            for (int q = 0; q < 3; q++) {
                z.zet(x, h + 1, q, dakSlab(l));
            }
        }
        z.zet(1, h + 1, 1, s(Blocks.PINK_WOOL));
    }

    /** 1x1: two high (L1-2) or three (L3-5) of the level's material. */
    private static void muur(int l, Zet z) {
        int h = l <= 2 ? 2 : 3;
        for (int y = 1; y <= h; y++) {
            z.zet(0, y, 0, mat(l));
        }
    }

    /** 2x2: a little blossom tree with petals under it. */
    private static void boompje(Zet z) {
        BlockState blad = Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        z.zet(0, 1, 0, s(Blocks.CHERRY_LOG));
        z.zet(0, 2, 0, s(Blocks.CHERRY_LOG));
        z.zet(1, 1, 1, s(Blocks.PINK_PETALS));
        for (int x = 0; x < 2; x++) {
            for (int q = 0; q < 2; q++) {
                z.zet(x, 3, q, blad);
                if (x + q > 0) {
                    z.zet(x, 2, q, blad);
                }
            }
        }
        z.zet(0, 4, 0, blad);
        z.zet(1, 4, 1, blad);
    }
}
