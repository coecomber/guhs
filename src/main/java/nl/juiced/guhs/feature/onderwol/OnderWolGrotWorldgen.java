package nl.juiced.guhs.feature.onderwol;

import java.util.Optional;
import java.util.OptionalInt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.DripstoneClusterConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.PointedDripstoneConfiguration;
import nl.juiced.guhs.Guhs;

/**
 * 1.7.0 "Onder de Wol": the Java features of the new cave kinds (registered in {@link OnderWolGrot}; configured and placed
 * in tools/features/onderwol_grot.py):
 * <ul>
 *   <li>{@link KristalpegelCluster} and {@link KristalpegelPunt}: vanilla's dripstone_cluster and pointed_dripstone with
 *       kristalpegelblok and kristalpegels (no water pools);</li>
 *   <li>{@link Belletjes}: a patch of belletjeskristal on the walls, floor and ceiling around a spot of cave air, with
 *       belletjestrosjes growing out of it (an amethyst geode turned inside out, in an open cave);</li>
 *   <li>{@link Druppels}: a few rozenkwarts druppels on the ceiling (sometimes a wall) around a spot under a cave ceiling.</li>
 * </ul>
 * They only turn the rock of the layers ({@code #guhs:onderwol/laagsteen}) and vanilla's dripstone-replaceable stone.
 */
public final class OnderWolGrotWorldgen {
    /** The rock of the layers (onderwol.py: the stone, the pink wool, the pink knuffelsteen). */
    public static final TagKey<Block> LAAGSTEEN = TagKey.create(Registries.BLOCK, Guhs.id("onderwol/laagsteen"));

    /** May a cave feature turn this block into its own rock? */
    static boolean grotsteen(BlockState state) {
        return state.is(LAAGSTEEN) || state.is(BlockTags.DRIPSTONE_REPLACEABLE);
    }

    static boolean leeg(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }

    // ==================================================================================================================
    // the kristalpegels (vanilla's DripstoneUtils, DripstoneClusterFeature and PointedDripstoneFeature)
    // ==================================================================================================================
    static boolean isVoet(BlockState state) {
        return state.is(OnderWolGrot.KRISTALPEGELBLOK.get()) || grotsteen(state);
    }

    static boolean zetBlok(LevelAccessor level, BlockPos pos) {
        if (grotsteen(level.getBlockState(pos))) {
            level.setBlock(pos, OnderWolGrot.KRISTALPEGELBLOK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }

    private static BlockState pegel(Direction richting, DripstoneThickness dikte) {
        return OnderWolGrot.KRISTALPEGEL.get().defaultBlockState().setValue(Kristalpegel.TIP_DIRECTION, richting).setValue(Kristalpegel.THICKNESS, dikte);
    }

    /** A column of lengte pegels from start in the direction punt (base, middles, frustum, tip), if start has a foot. */
    static void groeiPegel(LevelAccessor level, BlockPos start, Direction punt, int lengte, boolean samen) {
        if (lengte <= 0 || !isVoet(level.getBlockState(start.relative(punt.getOpposite())))) {
            return;
        }
        BlockPos.MutableBlockPos p = start.mutable();
        for (int i = 0; i < lengte; i++) {
            int rest = lengte - i;   // (blocks left including this one)
            DripstoneThickness dikte = rest == 1 ? (samen ? DripstoneThickness.TIP_MERGE : DripstoneThickness.TIP)
                    : rest == 2 ? DripstoneThickness.FRUSTUM : i == 0 ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
            level.setBlock(p, pegel(punt, dikte).setValue(Kristalpegel.WATERLOGGED, level.isWaterAt(p)), Block.UPDATE_CLIENTS);
            p.move(punt);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A cluster of kristalpegels in a patch of kristalpegelblok, on the floor and the ceiling (vanilla's dripstone_cluster). */
    public static class KristalpegelCluster extends Feature<DripstoneClusterConfiguration> {
        public KristalpegelCluster() {
            super(DripstoneClusterConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<DripstoneClusterConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos origin = context.origin();
            DripstoneClusterConfiguration c = context.config();
            RandomSource random = context.random();
            if (!leeg(level.getBlockState(origin))) {
                return false;
            }
            int hoogte = c.height.sample(random);
            float dichtheid = c.density.sample(random);
            int rx = c.radius.sample(random), rz = c.radius.sample(random);
            for (int dx = -rx; dx <= rx; dx++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    int rand = Math.min(rx - Math.abs(dx), rz - Math.abs(dz));
                    double kans = Mth.clampedMap((float) rand, 0.0f, (float) c.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn,
                            c.chanceOfDripstoneColumnAtMaxDistanceFromCenter, 1.0f);
                    kolom(level, random, origin.offset(dx, 0, dz), dx, dz, kans, hoogte, dichtheid, c);
                }
            }
            return true;
        }

        private void kolom(WorldGenLevel level, RandomSource random, BlockPos pos, int dx, int dz, double kans, int clusterHoogte, float dichtheid,
                           DripstoneClusterConfiguration c) {
            Optional<Column> scan = Column.scan(level, pos, c.floorToCeilingSearchRange, OnderWolGrotWorldgen::leeg, s -> !leeg(s));
            if (scan.isEmpty()) {
                return;
            }
            Column kolom = scan.get();
            OptionalInt plafond = kolom.getCeiling(), vloer = kolom.getFloor();
            if (plafond.isEmpty() && vloer.isEmpty()) {
                return;
            }
            int hangend = 0;
            if (plafond.isPresent() && random.nextDouble() < kans && !level.getBlockState(pos.atY(plafond.getAsInt())).is(Blocks.LAVA)) {
                laag(level, pos.atY(plafond.getAsInt()), c.dripstoneBlockLayerThickness.sample(random), Direction.UP);
                int max = vloer.isPresent() ? Math.min(clusterHoogte, plafond.getAsInt() - vloer.getAsInt()) : clusterHoogte;
                hangend = lengte(random, dx, dz, dichtheid, max, c);
            }
            int staand = 0;
            if (vloer.isPresent() && random.nextDouble() < kans && !level.getBlockState(pos.atY(vloer.getAsInt())).is(Blocks.LAVA)) {
                laag(level, pos.atY(vloer.getAsInt()), c.dripstoneBlockLayerThickness.sample(random), Direction.DOWN);
                staand = plafond.isPresent()
                        ? Math.max(0, hangend + Mth.randomBetweenInclusive(random, -c.maxStalagmiteStalactiteHeightDiff, c.maxStalagmiteStalactiteHeightDiff))
                        : lengte(random, dx, dz, dichtheid, clusterHoogte, c);
            }
            if (plafond.isPresent() && vloer.isPresent() && plafond.getAsInt() - hangend <= vloer.getAsInt() + staand) {
                // they would overlap: meet somewhere in between
                int v = vloer.getAsInt(), p = plafond.getAsInt();
                int laagste = Math.max(p - hangend, v + 1), hoogste = Math.min(v + staand, p - 1);
                int onderkant = Mth.randomBetweenInclusive(random, laagste, hoogste + 1);
                hangend = p - onderkant;
                staand = onderkant - 1 - v;
            }
            boolean samen = random.nextBoolean() && hangend > 0 && staand > 0 && kolom.getHeight().isPresent()
                    && hangend + staand == kolom.getHeight().getAsInt();
            if (plafond.isPresent()) {
                groeiPegel(level, pos.atY(plafond.getAsInt() - 1), Direction.DOWN, hangend, samen);
            }
            if (vloer.isPresent()) {
                groeiPegel(level, pos.atY(vloer.getAsInt() + 1), Direction.UP, staand, samen);
            }
        }

        private static int lengte(RandomSource random, int dx, int dz, float dichtheid, int max, DripstoneClusterConfiguration c) {
            if (random.nextFloat() > dichtheid) {
                return 0;
            }
            float gemiddeld = (float) Mth.clampedMap(Math.abs(dx) + Math.abs(dz), 0.0, c.maxDistanceFromCenterAffectingHeightBias, max / 2.0, 0.0);
            return (int) ClampedNormalFloat.sample(random, gemiddeld, c.heightDeviation, 0.0f, max);
        }

        private static void laag(WorldGenLevel level, BlockPos eerste, int aantal, Direction richting) {
            BlockPos.MutableBlockPos p = eerste.mutable();
            for (int i = 0; i < aantal && zetBlok(level, p); i++) {
                p.move(richting);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** One or two loose kristalpegels with a little patch of kristalpegelblok (vanilla's pointed_dripstone). */
    public static class KristalpegelPunt extends Feature<PointedDripstoneConfiguration> {
        public KristalpegelPunt() {
            super(PointedDripstoneConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<PointedDripstoneConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos pos = context.origin();
            RandomSource random = context.random();
            PointedDripstoneConfiguration c = context.config();
            boolean boven = isVoet(level.getBlockState(pos.above())), onder = isVoet(level.getBlockState(pos.below()));
            if (!boven && !onder) {
                return false;
            }
            Direction punt = boven && onder ? (random.nextBoolean() ? Direction.DOWN : Direction.UP) : boven ? Direction.DOWN : Direction.UP;
            BlockPos wortel = pos.relative(punt.getOpposite());
            zetBlok(level, wortel);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (random.nextFloat() <= c.chanceOfDirectionalSpread) {
                    BlockPos p1 = wortel.relative(d);
                    zetBlok(level, p1);
                    if (random.nextFloat() <= c.chanceOfSpreadRadius2) {
                        BlockPos p2 = p1.relative(Direction.getRandom(random));
                        zetBlok(level, p2);
                        if (random.nextFloat() <= c.chanceOfSpreadRadius3) {
                            zetBlok(level, p2.relative(Direction.getRandom(random)));
                        }
                    }
                }
            }
            int lengte = random.nextFloat() < c.chanceOfTallerDripstone && leeg(level.getBlockState(pos.relative(punt))) ? 2 : 1;
            groeiPegel(level, pos, punt, lengte, false);
            return true;
        }
    }

    // ==================================================================================================================
    // the belletjes
    // ==================================================================================================================
    /**
     * Around a spot of cave air: the exposed rock in a rough ball (radius 3-5) becomes belletjeskristal, and belletjestrosjes
     * grow out of it into the air, on every side.
     */
    public static class Belletjes extends Feature<NoneFeatureConfiguration> {
        private static final float KRISTAL_KANS = 0.85f, TROSJE_KANS = 0.2f;

        public Belletjes() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos origin = context.origin();
            RandomSource random = context.random();
            if (!level.getBlockState(origin).isAir()) {
                return false;
            }
            int r = 3 + random.nextInt(3);
            BlockState kristal = OnderWolGrot.BELLETJESKRISTAL.get().defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int gezet = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        double rr = r + random.nextDouble() - 0.5;
                        if (dx * dx + dy * dy + dz * dz > rr * rr) {
                            continue;
                        }
                        p.setWithOffset(origin, dx, dy, dz);
                        if (grotsteen(level.getBlockState(p)) && aanLucht(level, p) && random.nextFloat() < KRISTAL_KANS) {
                            level.setBlock(p, kristal, Block.UPDATE_CLIENTS);
                            gezet++;
                        }
                    }
                }
            }
            if (gezet == 0) {
                return false;
            }
            BlockState trosje = OnderWolGrot.BELLETJESTROSJE.get().defaultBlockState();
            for (int dx = -r - 1; dx <= r + 1; dx++) {
                for (int dy = -r - 1; dy <= r + 1; dy++) {
                    for (int dz = -r - 1; dz <= r + 1; dz++) {
                        p.setWithOffset(origin, dx, dy, dz);
                        if (!level.getBlockState(p).isAir() || random.nextFloat() >= TROSJE_KANS) {
                            continue;
                        }
                        Direction d = Direction.getRandom(random);
                        for (int i = 0; i < 6; i++, d = Direction.from3DDataValue((d.get3DDataValue() + 1) % 6)) {
                            if (level.getBlockState(p.relative(d)).is(OnderWolGrot.BELLETJESKRISTAL.get())) {
                                level.setBlock(p, trosje.setValue(AmethystClusterBlock.FACING, d.getOpposite()), Block.UPDATE_CLIENTS);
                                break;
                            }
                        }
                    }
                }
            }
            return true;
        }

        private static boolean aanLucht(WorldGenLevel level, BlockPos pos) {
            for (Direction d : Direction.values()) {
                if (level.getBlockState(pos.relative(d)).isAir()) {
                    return true;
                }
            }
            return false;
        }
    }

    // ==================================================================================================================
    // the rozenkwarts druppels
    // ==================================================================================================================
    /** A few rozenkwarts druppels around a spot under a cave ceiling: mostly on the ceiling, sometimes on a wall. */
    public static class Druppels extends Feature<NoneFeatureConfiguration> {
        private static final int POGINGEN = 10;

        public Druppels() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos origin = context.origin();
            RandomSource random = context.random();
            BlockState druppel = OnderWolGrot.ROZENKWARTS_DRUPPEL.get().defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean iets = false;
            int aantal = 2 + random.nextInt(4);
            for (int i = 0; i < POGINGEN && aantal > 0; i++) {
                p.setWithOffset(origin, random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
                if (!level.getBlockState(p).isAir()) {
                    continue;
                }
                // the ceiling first (two in three), else a wall
                Direction d = random.nextInt(3) > 0 ? Direction.UP : Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos achter = p.relative(d);
                BlockState s = level.getBlockState(achter);
                if (grotsteen(s) && s.isFaceSturdy(level, achter, d.getOpposite())) {
                    level.setBlock(p, druppel.setValue(AmethystClusterBlock.FACING, d.getOpposite()), Block.UPDATE_CLIENTS);
                    iets = true;
                    aantal--;
                }
            }
            return iets;
        }
    }

    private OnderWolGrotWorldgen() {
    }
}
