package nl.juiced.guhs.feature.vadsrijk.bazen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * DEV ONLY ({@code /guhs vadsrijk bazen bouw ...}): crude stand-in builds so the bosses can be tested before the wereld slice's
 * map is in. Only ever fills AIR, never replaces anything. The real arenas/temple/caps are wereld's (see POI_VERZOEKEN.md).
 */
public final class BazenBouw {
    private BazenBouw() {
    }

    private static int zet(ServerLevel sl, BlockPos p, BlockState s) {
        if (sl.getBlockState(p).isAir()) {
            sl.setBlock(p, nl.juiced.guhs.feature.vadsrijk.verbind.Verbind.vorm(sl, p, s), 2);   // U-HEKKEN: fences/walls connected
            return 1;
        }
        return 0;
    }

    public static int schijf(ServerLevel sl, BlockPos c, int r, BlockState s) {
        int n = 0;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z <= r * r) {
                    n += zet(sl, c.offset(x, 0, z), s);
                }
            }
        }
        return n;
    }

    public static int koning(ServerLevel sl) {
        return schijf(sl, BazenPlekken.KK_CENTRUM.below(), BazenPlekken.KK_STRAAL + 6, Blocks.GRASS_BLOCK.defaultBlockState());
    }

    public static int mika(ServerLevel sl) {
        int n = schijf(sl, BazenPlekken.MIKA_KRAAM.below(), 12, Blocks.PINK_CONCRETE.defaultBlockState());
        for (int[] h : BazenPlekken.MIKA_HOEDEN) {
            n += schijf(sl, new BlockPos(h[0], h[2] - 1, h[1]), BazenPlekken.MIKA_HOED_STRAAL + 2, Blocks.PINK_CONCRETE.defaultBlockState());
        }
        return n;
    }

    private static int doos(ServerLevel sl, int x0, int x1, int y0, int y1, int z0, int z1, BlockState s, boolean hol) {
        int n = 0;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    boolean rand = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                    if (!hol || rand) {
                        n += zet(sl, new BlockPos(x, y, z), s);
                    }
                }
            }
        }
        return n;
    }

    /** The whole temple as plain boxes: hall + 3 rooms + boss hall, openings where the gates and the door go. */
    public static int tempel(ServerLevel sl) {
        int v = BazenPlekken.TEMPEL_VLOER;
        BlockState muur = Blocks.MUSHROOM_STEM.defaultBlockState(), vloer = Blocks.SMOOTH_STONE.defaultBlockState();
        // keep the gate and door openings free: put a marker there first (structure void would be cleaner; air is enough)
        int n = 0;
        n += doos(sl, 312, 348, v - 1, v - 1, -484, -350, vloer, false);
        // entrance hall z -361..-372 with the door wall at -360
        n += wand(sl, 316, 344, v, v + 20, -360, BazenPlekken.TEMPELDEUR_MIN, BazenPlekken.TEMPELDEUR_MAX, Blocks.PINK_TERRACOTTA.defaultBlockState());
        n += zijmuren(sl, 316, 344, v, v + 20, -372, -361, muur);
        n += zijmuren(sl, 318, 342, v, v + 10, -393, -373, muur);
        n += wand(sl, 318, 342, v, v + 10, -394, BazenPlekken.K1_POORT_MIN, BazenPlekken.K1_POORT_MAX, null);
        n += zijmuren(sl, 318, 342, v, v + 10, -415, -395, muur);
        n += wand(sl, 318, 342, v, v + 10, -416, BazenPlekken.K2_POORT_MIN, BazenPlekken.K2_POORT_MAX, null);
        n += zijmuren(sl, 314, 346, v, v + 12, -441, -417, muur);
        n += wand(sl, 314, 346, v, v + 12, -442, BazenPlekken.K3_POORT_MIN, BazenPlekken.K3_POORT_MAX, null);
        // boss hall: a box around the arena, ceiling at +13
        n += zijmuren(sl, 310, 350, v, v + 13, -482, -443, muur);
        n += wand(sl, 310, 350, v, v + 13, -483, null, null, null);
        n += doos(sl, 310, 350, v + 13, v + 13, -483, -443, Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState(), false);
        n += doos(sl, 316, 344, v + 21, v + 21, -372, -361, muur, false);
        n += doos(sl, 318, 342, v + 11, v + 11, -415, -373, muur, false);
        n += doos(sl, 314, 346, v + 13, v + 13, -441, -417, muur, false);
        n += zet(sl, BazenPlekken.TEMPELDEUR_VOOR.below(), vloer);
        return n;
    }

    /** The east and west walls of a room (x0 and x1), from z0 to z1. */
    private static int zijmuren(ServerLevel sl, int x0, int x1, int y0, int y1, int z0, int z1, BlockState s) {
        return doos(sl, x0, x0, y0, y1, z0, z1, s, false) + doos(sl, x1, x1, y0, y1, z0, z1, s, false);
    }

    /** A wall across x0..x1 at z, with an opening [a..b] filled with {@code deur} (or left open when deur is null). */
    private static int wand(ServerLevel sl, int x0, int x1, int y0, int y1, int z, BlockPos a, BlockPos b, BlockState deur) {
        int n = 0;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                boolean gat = a != null && x >= a.getX() && x <= b.getX() && y >= a.getY() && y <= b.getY();
                if (gat && deur == null) {
                    continue;
                }
                n += zet(sl, new BlockPos(x, y, z), gat ? deur : Blocks.MUSHROOM_STEM.defaultBlockState());
            }
        }
        return n;
    }
}
