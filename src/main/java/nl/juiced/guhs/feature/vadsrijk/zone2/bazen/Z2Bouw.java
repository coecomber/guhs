package nl.juiced.guhs.feature.vadsrijk.zone2.bazen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * DEV ONLY ({@code /guhs vadsrijk z2bazen bouw ...}, also the AutoCheck): crude stand-in builds of the three zone-2 boss spots
 * so they can be tested before z2-wereld's map exists (zone 2 is still void). Only ever fills AIR, never replaces anything. The
 * real arenas, the Spiegelburcht shell and the drill site are wereld's (POI_VERZOEKEN_Z2.md, section z2-bazen): the bosses and
 * the burcht logic place their own blocks (pillars, mirrors, eyes, locks, gates) at runtime.
 */
public final class Z2Bouw {
    private Z2Bouw() {
    }

    private static int zet(ServerLevel sl, BlockPos p, BlockState s) {
        if (sl.getBlockState(p).isAir()) {
            sl.setBlock(p, s, 2);
            return 1;
        }
        return 0;
    }

    private static void laad(ServerLevel sl, BlockPos c, int r) {
        for (int x = (c.getX() - r) >> 4; x <= (c.getX() + r) >> 4; x++) {
            for (int z = (c.getZ() - r) >> 4; z <= (c.getZ() + r) >> 4; z++) {
                sl.getChunk(x, z);
            }
        }
    }

    private static int schijf(ServerLevel sl, BlockPos c, int r, BlockState s) {
        int n = 0;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z <= r * r + r) {
                    n += zet(sl, c.offset(x, 0, z), s);
                }
            }
        }
        return n;
    }

    private static int ring(ServerLevel sl, BlockPos c, double r, int hoog, BlockState s) {
        int n = 0;
        int ri = (int) Math.ceil(r) + 1;
        for (int x = -ri; x <= ri; x++) {
            for (int z = -ri; z <= ri; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d >= r - 0.5 && d < r + 0.5) {
                    for (int y = 0; y < hoog; y++) {
                        n += zet(sl, c.offset(x, y, z), s);
                    }
                }
            }
        }
        return n;
    }

    private static int doos(ServerLevel sl, int x0, int x1, int y0, int y1, int z0, int z1, BlockState s) {
        int n = 0;
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    n += zet(sl, new BlockPos(x, y, z), s);
                }
            }
        }
        return n;
    }

    public static int galmwolf(ServerLevel sl) {
        BlockPos c = Z2BazenPlekken.GALM_CENTRUM;
        int r = Z2BazenPlekken.GALM_STRAAL + 5;
        laad(sl, c, r + 2);
        int n = schijf(sl, c.below(), r, Blocks.SNOW_BLOCK.defaultBlockState());
        n += ring(sl, c, r, 2, Blocks.PACKED_ICE.defaultBlockState());
        for (int i = 0; i < 18; i++) {   // the crystal needles that "sing back" around the edge
            double a = i * Math.PI * 2 / 18;
            BlockPos p = c.offset((int) Math.round(Math.cos(a) * (r - 2)), 0, (int) Math.round(Math.sin(a) * (r - 2)));
            for (int y = 0; y < 3 + (i % 3); y++) {
                n += zet(sl, p.above(y), (y % 2 == 0 ? Blocks.BLUE_ICE : Blocks.LIGHT_BLUE_STAINED_GLASS).defaultBlockState());
            }
        }
        return n;
    }

    public static int boor(ServerLevel sl) {
        BlockPos c = Z2BazenPlekken.BOOR_CENTRUM;
        int r = Z2BazenPlekken.BOOR_STRAAL + 3;
        laad(sl, c, r + 2);
        int n = schijf(sl, c.below(), r, Blocks.STONE.defaultBlockState());
        n += ring(sl, c, r, 4, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        for (int i = 0; i < 8; i++) {   // scaffolding posts and lamps
            double a = i * Math.PI / 4;
            BlockPos p = c.offset((int) Math.round(Math.cos(a) * (r - 3)), 0, (int) Math.round(Math.sin(a) * (r - 3)));
            for (int y = 0; y < 4; y++) {
                n += zet(sl, p.above(y), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
            }
            n += zet(sl, p.above(4), Blocks.LANTERN.defaultBlockState());
        }
        return n;
    }

    /** The Spiegelburcht shell (wereld's layout, POI_Z2.md): 4 rooms with gate openings (x -2..2, 5 high), the round hall with a glass dome. */
    public static int burcht(ServerLevel sl) {
        int y = Z2BazenPlekken.BURCHT_Y;
        int bx = Z2BazenPlekken.BURCHT_X, dz = Z2BazenPlekken.DZ;
        BlockState vloer = Blocks.SMOOTH_QUARTZ.defaultBlockState(), muur = Blocks.CALCITE.defaultBlockState(), rand = Blocks.POLISHED_TUFF.defaultBlockState();
        BlockPos hal = Z2BazenPlekken.SPIEGEL_CENTRUM;
        for (int z = Z2BazenPlekken.BURCHT_DEUR_Z + 8; z >= hal.getZ() - 28; z -= 16) {
            laad(sl, new BlockPos(bx, y, z), 30);
        }
        int n = 0;
        // rooms: x half width, z0 (north), z1 (south)
        int[][] kamers = {{16, -2879 + dz, -2857 + dz}, {14, -2899 + dz, -2881 + dz}, {9, -2917 + dz, -2901 + dz}, {14, -2937 + dz, -2919 + dz}};
        for (int[] k : kamers) {
            int kx = k[0] + 1;
            n += doos(sl, bx - kx, bx + kx, y - 1, y - 1, k[1] - 1, k[2] + 1, vloer);
            n += doos(sl, bx - kx, bx - kx, y, y + 11, k[1] - 1, k[2] + 1, muur);
            n += doos(sl, bx + kx, bx + kx, y, y + 11, k[1] - 1, k[2] + 1, muur);
            n += doos(sl, bx - kx, bx + kx, y + 12, y + 12, k[1] - 1, k[2] + 1, rand);
        }
        // cross walls with their openings (the front door 7 wide and 9 high; the gates 5 x 5)
        int[][] muren = {{Z2BazenPlekken.BURCHT_DEUR_Z, 17, 3, 8}, {-2880 + dz, 17, 2, 4}, {-2900 + dz, 15, 2, 4}, {-2918 + dz, 15, 2, 4}, {-2938 + dz, 15, 2, 4}};
        for (int[] m : muren) {
            for (int x = -m[1]; x <= m[1]; x++) {
                for (int yy = 0; yy <= 11; yy++) {
                    if (Math.abs(x) <= m[2] && yy <= m[3]) {
                        continue;
                    }
                    n += zet(sl, new BlockPos(bx + x, y + yy, m[0]), yy % 4 == 0 ? rand : muur);
                }
            }
            n += doos(sl, bx - m[2], bx + m[2], y - 1, y - 1, m[0], m[0], vloer);
        }
        n += doos(sl, bx - 4, bx + 4, y - 1, y - 1, Z2BazenPlekken.BURCHT_DEUR_Z + 1, Z2BazenPlekken.BURCHT_DEUR_Z + 8, vloer);   // the forecourt
        // the hall: floor, a wall ring (open towards the prismazaal), a flat glass dome at +14
        int hr = Z2BazenPlekken.SPIEGEL_STRAAL;
        n += schijf(sl, hal.below(), hr + 1, vloer);
        int ri = hr + 2;
        for (int x = -ri; x <= ri; x++) {
            for (int z = -ri; z <= ri; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d >= hr + 0.5 && d < hr + 1.5 && !(Math.abs(x) <= 2 && z > 0)) {
                    for (int yy = 0; yy < Z2BazenPlekken.KOEPEL_ONDER; yy++) {
                        n += zet(sl, hal.offset(x, yy, z), yy == 4 || yy == 5 ? Blocks.TINTED_GLASS.defaultBlockState() : muur);
                    }
                }
            }
        }
        n += schijf(sl, hal.above(Z2BazenPlekken.KOEPEL_ONDER), hr + 1, Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState());
        // light
        for (int z = -2860 + dz; z >= -2934 + dz; z -= 6) {
            for (BlockPos l : new BlockPos[] {new BlockPos(bx - 8, y + 11, z), new BlockPos(bx + 8, y + 11, z)}) {
                n += zet(sl, l, Blocks.SEA_LANTERN.defaultBlockState());
            }
        }
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            n += zet(sl, hal.offset((int) Math.round(Math.cos(a) * 12), Z2BazenPlekken.KOEPEL_ONDER - 1, (int) Math.round(Math.sin(a) * 12)), Blocks.SEA_LANTERN.defaultBlockState());
        }
        return n;
    }
}
