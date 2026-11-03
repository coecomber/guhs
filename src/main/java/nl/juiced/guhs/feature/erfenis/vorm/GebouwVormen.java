package nl.juiced.guhs.feature.erfenis.vorm;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import nl.juiced.guhs.feature.erfenis.GebouwType;
import nl.juiced.guhs.feature.erfenis.Plot;

// STUB-vormen: the vormen slice owns this class (the real shapes from the approved renders); the lead replaces this file at
// the merge. This stand-in only makes the kern runnable: a simple box per building in its palette, flowers in the unused
// part of the footprint, simple overgrown patches and paths. minHoek = the footprint's min corner AT THE GRASS (y = Plot.Y);
// buildings stand from minHoek.above().
public final class GebouwVormen {
    private static final int VLAG = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private GebouwVormen() {
    }

    public static void stempel(ServerLevel lvl, String gebouwId, int level, int palet, BlockPos minHoek, int rot) {
        GebouwType t = GebouwType.van(gebouwId);
        if (t == null) {
            return;
        }
        int b = t.b(rot), d = t.d(rot);
        wis(lvl, minHoek, b, d);
        Random rng = new Random(minHoek.asLong() ^ gebouwId.hashCode());
        BlockState[] bloem = {Blocks.PINK_TULIP.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(),
                Blocks.SHORT_GRASS.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState()};
        // the building grows inside its claim with its level (L1 small, L10 the whole footprint minus a rim)
        int groei = level <= 0 ? 0 : Math.min(10, level);
        int bw = Math.max(2, Math.round((b - 2) * (0.45f + 0.055f * groei))), bd = Math.max(2, Math.round((d - 2) * (0.45f + 0.055f * groei)));
        int x0 = (b - bw) / 2, z0 = (d - bd) / 2;
        for (int x = 0; x < b; x++) {
            for (int z = 0; z < d; z++) {
                boolean binnen = x >= x0 && x < x0 + bw && z >= z0 && z < z0 + bd;
                BlockPos g = minHoek.offset(x, 0, z);
                if (!binnen) {
                    zet(lvl, g, Blocks.MOSS_BLOCK.defaultBlockState());
                    if (rng.nextInt(3) == 0) {
                        zet(lvl, g.above(), bloem[rng.nextInt(bloem.length)]);
                    }
                }
            }
        }
        if (level <= 0) {   // a building site: scaffolding at the corners, coarse dirt
            for (int x = x0; x < x0 + bw; x++) {
                for (int z = z0; z < z0 + bd; z++) {
                    zet(lvl, minHoek.offset(x, 0, z), Blocks.COARSE_DIRT.defaultBlockState());
                }
            }
            for (int[] c : new int[][] {{x0, z0}, {x0 + bw - 1, z0}, {x0, z0 + bd - 1}, {x0 + bw - 1, z0 + bd - 1}}) {
                for (int y = 1; y <= 3; y++) {
                    zet(lvl, minHoek.offset(c[0], y, c[1]), Blocks.SCAFFOLDING.defaultBlockState());
                }
            }
            return;
        }
        BlockState muur = muur(palet), hoek = hoek(palet), dak = dak(palet);
        int h = t.hoogte(level) - 2;
        for (int x = x0; x < x0 + bw; x++) {
            for (int z = z0; z < z0 + bd; z++) {
                boolean rand = x == x0 || z == z0 || x == x0 + bw - 1 || z == z0 + bd - 1;
                boolean isHoek = (x == x0 || x == x0 + bw - 1) && (z == z0 || z == z0 + bd - 1);
                zet(lvl, minHoek.offset(x, 0, z), Blocks.STONE_BRICKS.defaultBlockState());
                for (int y = 1; y <= h; y++) {
                    zet(lvl, minHoek.offset(x, y, z), !rand ? Blocks.AIR.defaultBlockState() : isHoek ? hoek : muur);
                }
                zet(lvl, minHoek.offset(x, h + 1, z), dak);
            }
        }
        // a door in the front (south at rot 0)
        int dx = x0 + bw / 2, dz = z0 + bd - 1;
        zet(lvl, minHoek.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
        zet(lvl, minHoek.offset(dx, 2, dz), Blocks.AIR.defaultBlockState());
        if (level >= 5) {   // a little tower: the higher tiers have one
            int tx = x0 + bw / 2, tz = z0 + bd / 2;
            for (int y = h + 1; y <= h + 1 + level / 2; y++) {
                zet(lvl, minHoek.offset(tx, y, tz), hoek);
            }
            zet(lvl, minHoek.offset(tx, h + 2 + level / 2, tz), Blocks.LANTERN.defaultBlockState());
        }
    }

    public static void wis(ServerLevel lvl, BlockPos minHoek, int breedte, int diepte) {
        for (int x = 0; x < breedte; x++) {
            for (int z = 0; z < diepte; z++) {
                BlockPos g = minHoek.offset(x, 0, z);
                zet(lvl, g, Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 1; y <= Plot.MAX_HOOGTE; y++) {
                    zet(lvl, g.above(y), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    public static int[] maat(String gebouwId) {
        GebouwType t = GebouwType.van(gebouwId);
        return t == null ? new int[] {1, 1} : new int[] {t.breedte, t.diepte};
    }

    /** RENDERS.md "Level -> vorm": the shape tier per level, by the tier the building opens in. */
    public static int vormTier(String gebouwId, int level) {
        GebouwType t = GebouwType.van(gebouwId);
        if (t == null) {
            return 1;
        }
        int L = Math.max(1, Math.min(10, level));
        if (t == GebouwType.HOEVE) {
            return (L + 1) / 2;
        }
        int opentTier = (t.opent + 1) / 2;
        int[][] tabel = {{1, 1, 3, 3, 5, 5}, {2, 2, 3, 4, 5, 5}, {3, 3, 4, 4, 5, 5}, {4, 4, 4, 5, 5, 5}, {5, 5, 5, 5, 5, 5}};
        int stap = L <= 4 ? (L == 1 ? 0 : 1) : L == 5 ? 2 : L <= 8 ? 3 : L == 9 ? 4 : 5;
        if (t == GebouwType.MAMMOETWEI) {
            return 5;
        }
        return tabel[Math.max(1, Math.min(5, opentTier)) - 1][stap];
    }

    public static void stempelPlukje(ServerLevel lvl, BlockPos minHoek, int soort) {
        wis(lvl, minHoek, 8, 8);
        Random rng = new Random(minHoek.asLong() * 31 + soort);
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                BlockPos g = minHoek.offset(x, 0, z);
                zet(lvl, g, rng.nextInt(4) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
                if (rng.nextInt(3) > 0) {
                    zet(lvl, g.above(), rng.nextBoolean() ? Blocks.SHORT_GRASS.defaultBlockState() : Blocks.FERN.defaultBlockState());
                }
            }
        }
        BlockState blad = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        switch (soort) {
            case 0 -> {   // struik
                for (int i = 0; i < 4; i++) {
                    int x = 1 + rng.nextInt(5), z = 1 + rng.nextInt(5);
                    zet(lvl, minHoek.offset(x, 1, z), blad);
                    zet(lvl, minHoek.offset(x + 1, 1, z), blad);
                    zet(lvl, minHoek.offset(x, 2, z), blad);
                }
            }
            case 1 -> {   // omgevallen boom
                BlockState stam = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                for (int x = 1; x < 7; x++) {
                    zet(lvl, minHoek.offset(x, 1, 3), stam);
                }
                zet(lvl, minHoek.offset(6, 2, 3), blad);
                zet(lvl, minHoek.offset(6, 1, 4), blad);
            }
            case 2 -> {   // rotsblok
                for (int x = 2; x < 6; x++) {
                    for (int z = 2; z < 6; z++) {
                        zet(lvl, minHoek.offset(x, 1, z), rng.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                        if (x > 2 && x < 5 && z > 2 && z < 5) {
                            zet(lvl, minHoek.offset(x, 2, z), Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                        }
                    }
                }
            }
            case 3 -> {   // brandnetels
                for (int x = 0; x < 8; x++) {
                    for (int z = 0; z < 8; z++) {
                        if (rng.nextInt(2) == 0) {
                            zet(lvl, minHoek.offset(x, 1, z), Blocks.TALL_GRASS.defaultBlockState());
                            zet(lvl, minHoek.offset(x, 2, z), Blocks.TALL_GRASS.defaultBlockState()
                                    .setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
                        }
                    }
                }
            }
            case 4 -> {   // oude kar
                for (int x = 2; x < 6; x++) {
                    zet(lvl, minHoek.offset(x, 1, 3), Blocks.SPRUCE_PLANKS.defaultBlockState());
                    zet(lvl, minHoek.offset(x, 1, 4), Blocks.SPRUCE_PLANKS.defaultBlockState());
                }
                zet(lvl, minHoek.offset(3, 2, 3), Blocks.BARREL.defaultBlockState());
                zet(lvl, minHoek.offset(1, 1, 3), Blocks.SPRUCE_FENCE.defaultBlockState());
            }
            default -> {   // molshoopjes
                for (int i = 0; i < 6; i++) {
                    zet(lvl, minHoek.offset(rng.nextInt(8), 1, rng.nextInt(8)), Blocks.ROOTED_DIRT.defaultBlockState());
                }
            }
        }
    }

    public static void stempelDeco(ServerLevel lvl, String decoId, BlockPos pos, int rot) {
        zet(lvl, pos.above(), Blocks.FLOWER_POT.defaultBlockState());
    }

    public static int[] decoMaat(String decoId) {
        return new int[] {1, 1};
    }

    public static void pad(ServerLevel lvl, BlockPos pos, String padSoort, int verbindingen) {
        zet(lvl, pos, "knarspad".equals(padSoort) ? Blocks.GRAVEL.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState());
    }

    private static BlockState muur(int palet) {
        return switch (palet) {
            case 2 -> Blocks.BRICKS.defaultBlockState();
            case 3 -> Blocks.WHITE_TERRACOTTA.defaultBlockState();
            case 4 -> Blocks.YELLOW_TERRACOTTA.defaultBlockState();
            case 5 -> Blocks.PINK_CONCRETE.defaultBlockState();
            default -> Blocks.OAK_PLANKS.defaultBlockState();
        };
    }

    private static BlockState hoek(int palet) {
        return switch (palet) {
            case 2 -> Blocks.STONE_BRICKS.defaultBlockState();
            case 3 -> Blocks.DARK_OAK_LOG.defaultBlockState();
            case 4 -> Blocks.GOLD_BLOCK.defaultBlockState();
            case 5 -> Blocks.AMETHYST_BLOCK.defaultBlockState();
            default -> Blocks.SPRUCE_LOG.defaultBlockState();
        };
    }

    private static BlockState dak(int palet) {
        return switch (palet) {
            case 2 -> Blocks.PINK_TERRACOTTA.defaultBlockState();
            case 3 -> Blocks.CHERRY_PLANKS.defaultBlockState();
            case 4 -> Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            case 5 -> Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
            default -> Blocks.HAY_BLOCK.defaultBlockState();
        };
    }

    private static void zet(ServerLevel lvl, BlockPos p, BlockState st) {
        if (lvl.getBlockState(p) != st) {
            lvl.setBlock(p, st, VLAG);
        }
    }
}
