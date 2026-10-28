package nl.juiced.guhs.feature.zingendeweide;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/**
 * The meadow's features (placed features in tools/features/zingendeweide.py: FEATURES). Each looks at the part of the
 * meadow it stands in ({@link WeideDeel}) and only builds on the meadow's own ground; the shapes are in {@link WeideTeken}.
 * A feature writes at most 15 blocks from its origin (the 3 x 3 chunks a feature may touch).
 */
public final class WeideFeatures {
    private WeideFeatures() {
    }

    /** Straight into the world (a feature: its 3 x 3 chunks). Fluids get a tick, so springs flow. */
    static WeideTeken.Zet zet(WorldGenLevel level) {
        return new WeideTeken.Zet() {
            final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

            @Override
            public void zet(int x, int y, int z, BlockState state) {
                p.set(x, y, z);
                if (!level.ensureCanWrite(p)) {
                    return;
                }
                level.setBlock(p, state, Block.UPDATE_CLIENTS);
                if (!state.getFluidState().isEmpty()) {
                    level.scheduleTick(p.immutable(), state.getFluidState().getType(), 0);
                }
            }

            @Override
            public BlockState get(int x, int y, int z) {
                return level.getBlockState(p.set(x, y, z));
            }
        };
    }

    /** The common start: our part here, or null when this is not the meadow's ground. */
    @Nullable
    static WeideDeel start(FeaturePlaceContext<NoneFeatureConfiguration> c) {
        BlockPos o = c.origin();
        if (!WeideTeken.grond(c.level().getBlockState(o.below())) || !c.level().getFluidState(o).isEmpty()) {
            return null;
        }
        return WeideDeel.op(c.level(), o);
    }

    /** A random plant of the meadow (the pink grasses, now and then a singing flower) at this spot, if there is room. */
    public static void plantje(WeideTeken.Zet z, int x, int y, int zz, RandomSource r) {
        // (the dice first: a structure draws chunk by chunk, so what it asks the random must not depend on the world)
        int k = r.nextInt(100), bloem = r.nextInt(3);
        if (!z.get(x, y, zz).isAir()) {
            return;
        }
        if (k < 34) {
            z.zet(x, y, zz, ZingendeWeideFeature.ROZE_GRAS.get().defaultBlockState());
        } else if (k < 68) {
            z.zet(x, y, zz, ZingendeWeideFeature.LICHTROZE_GRAS.get().defaultBlockState());
        } else if (k < 90) {
            if (z.get(x, y + 1, zz).isAir()) {
                DoublePlantBlock b = k < 79 ? ZingendeWeideFeature.HOOG_ROZE_GRAS.get() : ZingendeWeideFeature.HOOG_LICHTROZE_GRAS.get();
                z.zet(x, y, zz, b.defaultBlockState());
                z.zet(x, y + 1, zz, b.defaultBlockState().setValue(DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
            }
        } else {
            Block b = switch (bloem) {
                case 0 -> ZingendeWeideFeature.ZINGBLOEMETJE_ROZE.get();
                case 1 -> ZingendeWeideFeature.ZINGBLOEMETJE_GEEL.get();
                default -> ZingendeWeideFeature.ZINGBLOEMETJE_BLAUW.get();
            };
            z.zet(x, y, zz, b.defaultBlockState());
        }
    }

    /** (bonemeal on the meadow's grass) */
    static void plantje(net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource r) {
        plantje(new WeideTeken.Zet() {
            @Override
            public void zet(int x, int y, int z, BlockState state) {
                level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
            }

            @Override
            public BlockState get(int x, int y, int z) {
                return level.getBlockState(new BlockPos(x, y, z));
            }
        }, pos.getX(), pos.getY(), pos.getZ(), r);
    }

    private static boolean kans(RandomSource r, double p) {
        return r.nextDouble() < p;
    }

    /** Rock pillars: many in the Zangweide and the gorges, a few in the wood; varied heights, some caps, ears and springs. */
    public static class Pilaar extends Feature<NoneFeatureConfiguration> {
        public Pilaar() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            WeideDeel d = start(c);
            RandomSource r = c.random();
            if (d == null || d == WeideDeel.WOLKJESBOS && !kans(r, 0.3) || d == WeideDeel.WIEGWEIDE && !kans(r, 0.5)) {
                return false;
            }
            BlockPos o = c.origin();
            boolean reus = kans(r, 0.15);
            int h = reus ? 34 + r.nextInt(18) : 12 + r.nextInt(16);
            double rad = reus ? 3.2 + r.nextDouble() * 1.4 : 1.6 + r.nextDouble() * 1.8;
            boolean scheef = kans(r, 0.22);
            double dx = scheef ? (r.nextDouble() - 0.5) * 8 : (r.nextDouble() - 0.5) * 1.5, dz = scheef ? (r.nextDouble() - 0.5) * 8 : (r.nextDouble() - 0.5) * 1.5;
            int kap = kans(r, 0.45) ? 2 + r.nextInt(reus ? 5 : 3) : 0;
            WeideTeken.pilaar(zet(c.level()), r, o.getX(), o.getY(), o.getZ(), h, rad, dx, dz, kap, kans(r, 0.28), kans(r, 0.2));
            return true;
        }
    }

    /** The guh-ear arch (rare, never in the wood). */
    public static class Boog extends Feature<NoneFeatureConfiguration> {
        public Boog() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            WeideDeel d = start(c);
            if (d == null || d == WeideDeel.WOLKJESBOS) {
                return false;
            }
            RandomSource r = c.random();
            BlockPos o = c.origin();
            WeideTeken.boog(zet(c.level()), r, o.getX(), o.getY(), o.getZ(), 14 + r.nextInt(8), 10 + r.nextInt(6), r.nextBoolean());
            return true;
        }
    }

    /** A giant wolkjesboom: always in the Wolkjesbos, now and then elsewhere. */
    public static class Reuzenboom extends Feature<NoneFeatureConfiguration> {
        public Reuzenboom() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            BlockPos o = c.origin();
            RandomSource r = c.random();
            // (from a sapling the meadow test is skipped: the ground just has to hold it)
            if (!c.level().getBlockState(o.below()).isSolid()) {
                return false;
            }
            WeideDeel d = WeideDeel.op(c.level(), o);
            if (d != null && d != WeideDeel.WOLKJESBOS && !kans(r, 0.25)) {
                return false;
            }
            WeideTeken.reuzenboom(zet(c.level()), r, o.getX(), o.getY(), o.getZ(), 16 + r.nextInt(12), kans(r, 0.4));
            return true;
        }
    }

    /** One small wolkjesboom (the sapling's tree, and the meadow's). */
    public static class Boompje extends Feature<NoneFeatureConfiguration> {
        public Boompje() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            BlockPos o = c.origin();
            if (!c.level().getBlockState(o.below()).isSolid()) {
                return false;
            }
            WeideTeken.boompje(zet(c.level()), c.random(), o.getX(), o.getY(), o.getZ());
            return true;
        }
    }

    /** Small wolkjesbomen: a grove in the Wolkjesbos, one now and then elsewhere. */
    public static class Boompjes extends Feature<NoneFeatureConfiguration> {
        public Boompjes() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            WeideDeel d = start(c);
            RandomSource r = c.random();
            if (d == null || d == WeideDeel.SCHILDJESZAND || d != WeideDeel.WOLKJESBOS && !kans(r, 0.25)) {
                return false;
            }
            BlockPos o = c.origin();
            WeideTeken.boompje(zet(c.level()), r, o.getX(), o.getY(), o.getZ());
            return true;
        }
    }

    /** Pastel crystal clusters (more in the gorges). */
    public static class Kristallen extends Feature<NoneFeatureConfiguration> {
        public Kristallen() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            WeideDeel d = start(c);
            if (d == null || d != WeideDeel.ZINGKLOOFJES && !kans(c.random(), 0.5)) {
                return false;
            }
            BlockPos o = c.origin();
            WeideTeken.kristallen(zet(c.level()), c.random(), o.getX(), o.getY(), o.getZ());
            return true;
        }
    }

    /** A bubbelsap pool (not in the sand). */
    public static class Poeltje extends Feature<NoneFeatureConfiguration> {
        public Poeltje() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            WeideDeel d = start(c);
            if (d == null || d == WeideDeel.SCHILDJESZAND) {
                return false;
            }
            RandomSource r = c.random();
            BlockPos o = c.origin();
            WeideTeken.poel(zet(c.level()), r, o.getX(), o.getY(), o.getZ(), 3 + r.nextDouble() * 4, 3 + r.nextDouble() * 4, 2 + r.nextInt(2));
            return true;
        }
    }

    /** A Blub hole (in the green parts and the Zangweide). */
    public static class Holletje extends Feature<NoneFeatureConfiguration> {
        public Holletje() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            WeideDeel d = start(c);
            if (d == null || d == WeideDeel.SCHILDJESZAND || d == WeideDeel.ZINGKLOOFJES) {
                return false;
            }
            BlockPos o = c.origin();
            WeideTeken.holletje(zet(c.level()), c.random(), o.getX(), o.getY(), o.getZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
            return true;
        }
    }

    /** Shell rocks (only the Schildjeszand). */
    public static class Schildrots extends Feature<NoneFeatureConfiguration> {
        public Schildrots() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            if (start(c) != WeideDeel.SCHILDJESZAND) {
                return false;
            }
            BlockPos o = c.origin();
            WeideTeken.schildrots(zet(c.level()), c.random(), o.getX(), o.getY(), o.getZ());
            return true;
        }
    }

    /** The singing gorges (only their own part). */
    public static class Kloof extends Feature<NoneFeatureConfiguration> {
        public Kloof() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            if (start(c) != WeideDeel.ZINGKLOOFJES) {
                return false;
            }
            RandomSource r = c.random();
            BlockPos o = c.origin();
            WeideTeken.kloof(zet(c.level()), r, o.getX(), o.getY(), o.getZ(), Direction.Plane.HORIZONTAL.getRandomDirection(r), 18 + r.nextInt(10));
            return true;
        }
    }

    /** The lief pink growth: patches of the pink and light pink grasses, a few singing flowers. Sparse on the sand. */
    public static class Begroeiing extends Feature<NoneFeatureConfiguration> {
        public Begroeiing() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            WeideDeel d = start(c);
            if (d == null) {
                return false;
            }
            RandomSource r = c.random();
            WorldGenLevel level = c.level();
            WeideTeken.Zet z = zet(level);
            int pogingen = d == WeideDeel.SCHILDJESZAND ? 4 : 28;
            BlockPos o = c.origin();
            for (int i = 0; i < pogingen; i++) {
                int x = o.getX() + r.nextInt(9) - 4, zz = o.getZ() + r.nextInt(9) - 4;
                for (int y = o.getY() + 3; y >= o.getY() - 3; y--) {
                    BlockState onder = z.get(x, y - 1, zz);
                    if (z.get(x, y, zz).isAir() && onder.is(net.minecraft.tags.BlockTags.DIRT) && level.getFluidState(new BlockPos(x, y, zz)).getType() == Fluids.EMPTY) {
                        plantje(z, x, y, zz, r);
                        break;
                    }
                }
            }
            return true;
        }
    }
}
