package nl.juiced.guhs.feature.barbecuether;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Barbecuether's own worldgen features (configured and placed in data/guhs/worldgen, see tools/features/barbecuether.py):
 * giant saté skewers (Satébos), giant sausages (Worstenwoud), grill pillars with grates (Rookdelta), gloeikool hanging
 * from the ceiling (everywhere) and charred guh skeletons (Asdal).
 */
public final class BarbecueWorldgen {
    static boolean free(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        // (plants count as free: a sprout that grows into a giant skewer or sausage makes room for it)
        return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty()) || s.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock;   // (1.21.1 BushBlock)
    }

    static boolean solid(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return !s.isAir() && s.getFluidState().isEmpty() && s.isSolid();
    }

    static void put(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (free(level, pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    static BlockState axis(BlockState log, Direction.Axis axis) {
        return log.hasProperty(RotatedPillarBlock.AXIS) ? log.setValue(RotatedPillarBlock.AXIS, axis) : log;
    }

    /** How much room is free straight up from pos (at most max). */
    static int room(WorldGenLevel level, BlockPos pos, int max) {
        int h = 0;
        while (h < max && free(level, pos.above(h))) {
            h++;
        }
        return h;
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A giant saté skewer stuck in the ground: a tall stick with chunks of grilled meat, dripping with peanut sauce. */
    public static class SateSpies extends Feature<NoneFeatureConfiguration> {
        public SateSpies() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos pos = ctx.origin();
            RandomSource random = ctx.random();
            if (!free(level, pos) || !solid(level, pos.below())) {
                return false;
            }
            int height = Math.min(7 + random.nextInt(10), room(level, pos, 24) - 2);
            if (height < 6) {
                return false;
            }
            BlockState stick = axis(BarbecuetherFeature.SATE_STAM.get().defaultBlockState(), Direction.Axis.Y);
            BlockState meat = BarbecuetherFeature.SATE_VLEES.get().defaultBlockState();
            BlockState sauce = BarbecuetherFeature.PINDASAUSPLASJE.get().defaultBlockState();
            BlockState light = BarbecuetherFeature.UIENLICHT.get().defaultBlockState();
            for (int y = 0; y < height; y++) {
                level.setBlock(pos.above(y), stick, 2);
            }
            // the chunks: from a bit above the ground up to just under the tip
            int wide = random.nextInt(3) == 0 ? 2 : 1;
            int lastTop = -1;
            for (int y = 2 + random.nextInt(2); y + 1 < height - 1; y += 3) {
                int thick = random.nextInt(4) == 0 ? 1 : 2;
                for (int dy = 0; dy < thick; dy++) {
                    for (int dx = -wide; dx <= wide; dx++) {
                        for (int dz = -wide; dz <= wide; dz++) {
                            boolean corner = Math.abs(dx) == wide && Math.abs(dz) == wide;
                            if ((dx == 0 && dz == 0) || (corner && (wide == 2 || random.nextInt(3) == 0))) {
                                continue;
                            }
                            put(level, pos.offset(dx, y + dy, dz), meat);
                        }
                    }
                }
                lastTop = y + thick;
                // a grilled onion ring (light) stuck to a chunk now and then
                if (random.nextInt(3) == 0) {
                    Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                    put(level, pos.above(y).relative(d, wide + 1), light);
                }
                // peanut sauce on top of the chunk
                for (int dx = -wide; dx <= wide; dx++) {
                    for (int dz = -wide; dz <= wide; dz++) {
                        if ((dx != 0 || dz != 0) && random.nextInt(3) > 0 && free(level, pos.offset(dx, lastTop, dz))
                                && level.getBlockState(pos.offset(dx, lastTop - 1, dz)).is(meat.getBlock())) {
                            level.setBlock(pos.offset(dx, lastTop, dz), sauce, 2);
                        }
                    }
                }
            }
            // a pool of peanut sauce dripped on the ground around it
            for (int i = 0; i < 10; i++) {
                BlockPos p = pos.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
                if (free(level, p) && level.getBlockState(p).isAir() && solid(level, p.below())) {
                    level.setBlock(p, sauce, 2);
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A giant sausage standing up like a mushroom (sometimes bent, sometimes lying down), with mustard zigzags. */
    public static class Braadworst extends Feature<NoneFeatureConfiguration> {
        public Braadworst() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos pos = ctx.origin();
            RandomSource random = ctx.random();
            if (!free(level, pos) || !solid(level, pos.below())) {
                return false;
            }
            BlockState worst = BarbecuetherFeature.WORST_STAM.get().defaultBlockState();
            BlockState mosterd = BarbecuetherFeature.MOSTERD_BLOK.get().defaultBlockState();
            if (random.nextInt(5) == 0) {
                return lying(level, pos, random, worst, mosterd);
            }
            int height = Math.min(8 + random.nextInt(11), room(level, pos, 26) - 1);
            if (height < 6) {
                return false;
            }
            float r = random.nextInt(3) == 0 ? 2.2f : 1.5f;
            Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            boolean bent = random.nextBoolean();
            float twist = random.nextFloat() * 6.28f;
            for (int y = 0; y < height; y++) {
                float rr = y >= height - 2 ? r - (y - (height - 3)) * 0.7f : r;     // a rounded end on top
                int shift = bent ? (int) (Math.pow(y / (float) height, 2) * 3) : 0;
                BlockPos c = pos.above(y).relative(lean, shift);
                int ri = (int) Math.ceil(rr);
                for (int dx = -ri; dx <= ri; dx++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        if (dx * dx + dz * dz <= rr * rr + 0.2f) {
                            BlockPos p = c.offset(dx, 0, dz);
                            if (free(level, p)) {
                                level.setBlock(p, axis(worst, Direction.Axis.Y), 2);
                            }
                        }
                    }
                }
                // the mustard: a zigzag on the outside
                double a = twist + (y % 6 < 3 ? y % 3 : 3 - y % 3) * 0.9;
                BlockPos m = c.offset((int) Math.round(Math.cos(a) * (rr + 1)), 0, (int) Math.round(Math.sin(a) * (rr + 1)));
                if (y > 1 && y < height - 1) {
                    put(level, m, mosterd);
                }
            }
            // a blob of mustard on top
            BlockPos top = pos.above(height).relative(lean, bent ? 3 : 0);
            put(level, top, mosterd);
            if (random.nextBoolean()) {
                put(level, top.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random)), mosterd);
            }
            return true;
        }

        private boolean lying(WorldGenLevel level, BlockPos pos, RandomSource random, BlockState worst, BlockState mosterd) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int length = 6 + random.nextInt(6);
            BlockState log = axis(worst, dir.getAxis());
            for (int i = 0; i < length; i++) {
                BlockPos c = pos.relative(dir, i);
                if (!free(level, c)) {
                    return i > 3;
                }
                boolean end = i == 0 || i == length - 1;
                level.setBlock(c, log, 2);
                if (!end) {
                    put(level, c.above(), log);
                    put(level, c.relative(dir.getClockWise()), log);
                    put(level, c.relative(dir.getCounterClockWise()), log);
                    if (i % 3 == 1) {
                        put(level, c.above(2), mosterd);
                    }
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A cluster of black grill-iron pillars, some connected by a grill grate: the Rookdelta is one big barbecue. */
    public static class Roosterpilaren extends Feature<NoneFeatureConfiguration> {
        public Roosterpilaren() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos origin = ctx.origin();
            RandomSource random = ctx.random();
            if (!free(level, origin) || !solid(level, origin.below())) {
                return false;
            }
            BlockState pillar = axis(BarbecuetherFeature.ROOSTERIJZER_PILAAR.get().defaultBlockState(), Direction.Axis.Y);
            BlockState bars = BarbecuetherFeature.ROOSTERIJZER_TRALIES.get().defaultBlockState();
            int n = 2 + random.nextInt(3);
            BlockPos prev = null;
            int prevH = 0;
            for (int i = 0; i < n; i++) {
                BlockPos p = origin.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
                // find the floor there
                int tries = 0;
                while (!free(level, p) && tries++ < 4) {
                    p = p.above();
                }
                while (free(level, p.below()) && tries++ < 8) {
                    p = p.below();
                }
                if (!free(level, p) || !solid(level, p.below())) {
                    continue;
                }
                int max = room(level, p, 30);
                int h = max <= 16 && random.nextBoolean() ? max : Math.min(max, 3 + random.nextInt(9));   // sometimes all the way up
                for (int y = 0; y < h; y++) {
                    level.setBlock(p.above(y), pillar, 2);
                }
                if (prev != null && Math.min(h, prevH) >= 3 && prev.distManhattan(p) <= 10) {
                    // a grill grate between two pillars
                    int gy = Math.min(Math.min(h, prevH) - 1, 2 + random.nextInt(3));
                    BlockPos b = p.above(gy);
                    BlockPos a = new BlockPos(prev.getX(), b.getY(), prev.getZ());
                    int steps = Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
                    boolean alongX = Math.abs(a.getX() - b.getX()) >= Math.abs(a.getZ() - b.getZ());
                    BlockState grate = bars
                            .setValue(net.minecraft.world.level.block.CrossCollisionBlock.EAST, alongX)
                            .setValue(net.minecraft.world.level.block.CrossCollisionBlock.WEST, alongX)
                            .setValue(net.minecraft.world.level.block.CrossCollisionBlock.NORTH, !alongX)
                            .setValue(net.minecraft.world.level.block.CrossCollisionBlock.SOUTH, !alongX);
                    for (int s = 1; s < steps; s++) {
                        BlockPos q = new BlockPos(a.getX() + (b.getX() - a.getX()) * s / steps, b.getY(), a.getZ() + (b.getZ() - a.getZ()) * s / steps);
                        put(level, q, grate);
                    }
                }
                prev = p;
                prevH = h;
            }
            return prev != null;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Glowing coal hanging from the ceiling (the Barbecuether's glowstone), grown like vanilla's glowstone blobs. */
    public static class GloeikoolKlomp extends Feature<NoneFeatureConfiguration> {
        public GloeikoolKlomp() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos pos = ctx.origin();
            RandomSource random = ctx.random();
            if (!level.isEmptyBlock(pos) || !solid(level, pos.above()) || level.getBlockState(pos.above()).is(Blocks.BEDROCK) && random.nextBoolean()) {
                return false;
            }
            BlockState coal = BarbecuetherFeature.GLOEIKOOL.get().defaultBlockState();
            level.setBlock(pos, coal, 2);
            for (int i = 0; i < 1500; i++) {
                BlockPos p = pos.offset(random.nextInt(8) - random.nextInt(8), -random.nextInt(12), random.nextInt(8) - random.nextInt(8));
                if (level.getBlockState(p).isAir()) {
                    int n = 0;
                    for (Direction d : Direction.values()) {
                        if (level.getBlockState(p.relative(d)).is(coal.getBlock())) {
                            n++;
                        }
                        if (n > 1) {
                            break;
                        }
                    }
                    if (n == 1) {
                        level.setBlock(p, coal, 2);
                    }
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A charred guh skeleton, half sunk into the ash: a big skull with eye holes and ears, a spine, ribs and a tail. */
    public static class GuhFossiel extends Feature<NoneFeatureConfiguration> {
        public GuhFossiel() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos origin = ctx.origin();
            RandomSource random = ctx.random();
            if (!free(level, origin) || !solid(level, origin.below())) {
                return false;
            }
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            Direction side = dir.getClockWise();
            BlockPos base = origin.below(random.nextInt(2));          // half sunk
            BlockState bone = BarbecuetherFeature.VERKOOLD_GUHBOT.get().defaultBlockState();
            BlockState spine = axis(bone, dir.getAxis());
            BlockState rib = axis(bone, Direction.Axis.Y);
            BlockState across = axis(bone, side.getAxis());
            int length = 5 + random.nextInt(4);
            boolean skullOnly = random.nextInt(4) == 0;
            // the spine and the ribs
            if (!skullOnly) {
                for (int i = 0; i < length; i++) {
                    BlockPos s = base.relative(dir, -i).above(2);
                    set(level, s, spine);
                    if (i % 2 == 1 && i < length - 2) {
                        for (Direction d : new Direction[]{side, side.getOpposite()}) {
                            set(level, s.relative(d), across);
                            set(level, s.relative(d, 2).below(), rib);
                            set(level, s.relative(d, 2).below(2), rib);
                        }
                    }
                }
                // the tail, curling up
                BlockPos t = base.relative(dir, -length).above(2);
                set(level, t, spine);
                set(level, t.relative(dir.getOpposite()).above(), rib);
                set(level, t.relative(dir.getOpposite()).above(2), rib);
            }
            // the skull: 5 wide, 4 high, 3 deep, with two eye holes looking forward and two ears
            BlockPos head = base.relative(dir, 2);
            for (int w = -2; w <= 2; w++) {
                for (int h = 0; h < 4; h++) {
                    for (int d = 0; d < 3; d++) {
                        BlockPos p = head.relative(side, w).above(h).relative(dir, d);
                        boolean edge = Math.abs(w) == 2 || h == 0 || h == 3 || d == 0 || d == 2;
                        boolean eye = d == 2 && h == 2 && Math.abs(w) == 1;
                        boolean corner = Math.abs(w) == 2 && (h == 0 || h == 3) && random.nextBoolean();
                        if (edge && !eye && !corner) {
                            set(level, p, bone);
                        }
                    }
                }
            }
            for (int w : new int[]{-2, 2}) {
                set(level, head.relative(side, w).above(4).relative(dir, 1), rib);
                set(level, head.relative(side, w + (w < 0 ? -1 : 1)).above(5).relative(dir, 1), rib);
            }
            // ash piled up around it
            for (int i = 0; i < 14; i++) {
                BlockPos p = origin.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
                if (level.getBlockState(p).isAir() && solid(level, p.below())) {
                    level.setBlock(p.below(), (random.nextBoolean() ? BarbecuetherFeature.AS_BLOK : BarbecuetherFeature.AS_AARDE).get().defaultBlockState(), 2);
                }
            }
            return true;
        }

        private static void set(WorldGenLevel level, BlockPos p, BlockState state) {
            BlockState here = level.getBlockState(p);
            if (!here.is(Blocks.BEDROCK) && here.getFluidState().isEmpty()) {
                level.setBlock(p, state, 2);
            }
        }
    }

    private BarbecueWorldgen() {
    }
}
