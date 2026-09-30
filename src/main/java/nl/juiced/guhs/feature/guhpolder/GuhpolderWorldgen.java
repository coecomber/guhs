package nl.juiced.guhs.feature.guhpolder;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * The Guhpolder's own worldgen features (placed by the biome, see tools/features/guhpolder.py):
 * <ul>
 *   <li>{@link Knotwilg}: a knotwilg (pollard willow): a short thick trunk with a knobbly head and a crown of thin upright
 *       twigs, the top ones with a snow cap;</li>
 *   <li>{@link Sloot}: a straight frozen ditch of polderijs flush with the ground, frosty sprigs on its banks and often a
 *       row of knotwilgen along it (very Dutch);</li>
 *   <li>{@link Sneeuwguhheuvel}: a little snow hill with a sneeuwpopguh (or a family of two) on top;</li>
 *   <li>{@link LosMolentje}: a guh-molentje on a knotwilg post, turning in the polder wind.</li>
 * </ul>
 * All of them stay out of buildings ({@link BouwRuimte#inBuilding}) and only build on flat polder ground.
 */
public final class GuhpolderWorldgen {
    private GuhpolderWorldgen() {
    }

    /** Polder ground where these features may build (rijpgras, dirt, snow). */
    static boolean grond(BlockState state) {
        return GuhpolderBlocks.poldergrond(state);
    }

    /** The ground block under the first air (or plant) at x, z. */
    static BlockPos grondOnder(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
    }

    /** Can a plant or snow layer here make room? */
    static boolean vrij(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(GuhpolderFeature.RIJPSPRIETJES.get())
                || state.is(GuhpolderFeature.GUH_IJSBLOEMPJE.get()) || state.is(Blocks.SNOW);
    }

    // =================================================================================================================
    // the knotwilg
    // =================================================================================================================

    public static class Knotwilg extends Feature<NoneFeatureConfiguration> {
        public Knotwilg(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            BlockPos o = context.origin();
            return boom(context.level(), context.random(), grondOnder(context.level(), o.getX(), o.getZ()));
        }
    }

    /**
     * A knotwilg standing on {@code ground} (the ground block): 2-3 logs of trunk, a knobbly head with 1-3 stubby side
     * knobs, and 6-10 thin twigs (knotwilg_bladeren) of 2-4 blocks fanning up from the head; the top of every twig has a
     * snow cap. Returns false (and builds nothing) when there's no room.
     */
    public static boolean boom(WorldGenLevel level, RandomSource random, BlockPos ground) {
        if (!grond(level.getBlockState(ground)) || BouwRuimte.inBuilding(level, ground.above())) {
            return false;
        }
        int trunk = 2 + random.nextInt(2);
        for (int dy = 1; dy <= trunk + 5; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockState s = level.getBlockState(ground.offset(dx, dy, dz));
                    if (!vrij(s) && !s.is(GuhpolderFeature.KNOTWILG_BLADEREN.get())) {
                        return false;
                    }
                }
            }
        }
        Map<BlockPos, Boolean> twijgen = new HashMap<>();   // pos -> is the top of a twig (snow cap)
        java.util.List<BlockPos> hout = new java.util.ArrayList<>();
        BlockState stam = GuhpolderFeature.KNOTWILG_STAM.get().defaultBlockState();
        for (int dy = 1; dy <= trunk; dy++) {
            hout.add(ground.above(dy));
        }
        BlockPos kop = ground.above(trunk);
        // the knobs: short horizontal stubs around the head
        java.util.List<Direction> kanten = new java.util.ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        java.util.Collections.shuffle(kanten, new java.util.Random(random.nextLong()));
        int knobs = 1 + random.nextInt(3);
        for (int i = 0; i < knobs; i++) {
            hout.add(kop.relative(kanten.get(i)));
        }
        // the twigs: thin shoots from the head and the knobs, straight up or leaning out
        int n = 6 + random.nextInt(5);
        for (int i = 0; i < n; i++) {
            BlockPos from = hout.get(trunk - 1 + random.nextInt(hout.size() - trunk + 1)).above();
            int len = 2 + random.nextInt(3);
            BlockPos p = from;
            Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            for (int k = 0; k < len; k++) {
                twijgen.put(p, false);
                if (k == len / 2 && random.nextInt(3) == 0) {   // leaning out (through the side, so the twig stays connected)
                    p = p.relative(lean);
                    twijgen.put(p, false);
                }
                p = p.above();
            }
            twijgen.put(p, false);
        }
        // the tops of the twigs get a snow cap
        for (BlockPos p : java.util.List.copyOf(twijgen.keySet())) {
            if (!twijgen.containsKey(p.above())) {
                twijgen.put(p, true);
            }
        }
        for (BlockPos p : hout) {
            boolean liggend = !p.equals(kop) && p.getY() == kop.getY();
            BlockState s = liggend ? stam.setValue(RotatedPillarBlock.AXIS, p.getX() != kop.getX() ? Direction.Axis.X : Direction.Axis.Z) : stam;
            level.setBlock(p, s, Block.UPDATE_CLIENTS);
            twijgen.remove(p);
        }
        // leaves: their distance to the wood, so they don't fall off (and decay once the tree is chopped)
        Map<BlockPos, Integer> afstand = new HashMap<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        for (BlockPos p : hout) {
            afstand.put(p, 0);
            todo.add(p);
        }
        while (!todo.isEmpty()) {
            BlockPos p = todo.poll();
            int d = afstand.get(p);
            for (Direction dir : Direction.values()) {
                BlockPos q = p.relative(dir);
                if (twijgen.containsKey(q) && !afstand.containsKey(q)) {
                    afstand.put(q, d + 1);
                    todo.add(q);
                }
            }
        }
        BlockState blad = GuhpolderFeature.KNOTWILG_BLADEREN.get().defaultBlockState();
        for (var e : twijgen.entrySet()) {
            if (!vrij(level.getBlockState(e.getKey()))) {
                continue;
            }
            int d = Math.min(LeavesBlock.DECAY_DISTANCE, afstand.getOrDefault(e.getKey(), LeavesBlock.DECAY_DISTANCE));
            level.setBlock(e.getKey(), blad.setValue(LeavesBlock.DISTANCE, Math.max(1, d)).setValue(GuhpolderBlocks.SNEEUW, e.getValue()),
                    Block.UPDATE_CLIENTS);
        }
        // a bit of snow at its foot
        if (random.nextBoolean()) {
            BlockPos voet = ground.above().relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            if (level.getBlockState(voet).isAir() && grond(level.getBlockState(voet.below()))) {
                level.setBlock(voet, Blocks.SNOW.defaultBlockState(), Block.UPDATE_CLIENTS);
                sneeuwig(level, voet.below());
            }
        }
        return true;
    }

    /** Rijpgras under snow shows its snowy sides. */
    static void sneeuwig(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.hasProperty(SnowyBlock.SNOWY)) {
            level.setBlock(pos, s.setValue(SnowyBlock.SNOWY, true), Block.UPDATE_CLIENTS);
        }
    }

    // =================================================================================================================
    // the sloot
    // =================================================================================================================

    public static class Sloot extends Feature<NoneFeatureConfiguration> {
        /** How far a sloot reaches from its middle (it stays within the chunks next to its own). */
        public static final int HALF = 10;

        public Sloot(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos o = context.origin();
            BlockPos mid = grondOnder(level, o.getX(), o.getZ());
            if (!grond(level.getBlockState(mid))) {
                return false;
            }
            Direction along = random.nextBoolean() ? Direction.EAST : Direction.SOUTH;
            Direction side = along.getClockWise();
            int width = random.nextInt(3) == 0 ? 2 : 1;
            int from = -(6 + random.nextInt(HALF - 5)), to = 6 + random.nextInt(HALF - 5);
            int wilgSide = random.nextBoolean() ? width : -1;
            boolean wilgen = random.nextInt(3) != 0;
            int placed = 0;
            for (int step = 0; step <= to; step++) {
                if (!graaf(level, random, mid, along, side, width, step, wilgen, wilgSide)) {
                    break;
                }
                placed++;
            }
            for (int step = -1; step >= from; step--) {
                if (!graaf(level, random, mid, along, side, width, step, wilgen, wilgSide)) {
                    break;
                }
                placed++;
            }
            return placed > 0;
        }

        /** One cross-section of the ditch; false where the ground isn't flat polder ground any more (the ditch ends). */
        private static boolean graaf(WorldGenLevel level, RandomSource random, BlockPos mid, Direction along, Direction side, int width, int step,
                                     boolean wilgen, int wilgSide) {
            BlockPos base = mid.relative(along, step);
            for (int w = 0; w < width; w++) {
                BlockPos g = grondOnder(level, base.relative(side, w).getX(), base.relative(side, w).getZ());
                BlockState at = level.getBlockState(g);
                if (Math.abs(g.getY() - mid.getY()) > 0 && !at.is(GuhpolderFeature.POLDERIJS.get())) {
                    return false;
                }
                if (!grond(at) && !at.is(GuhpolderFeature.POLDERIJS.get()) || BouwRuimte.inBuilding(level, g)) {
                    return false;
                }
            }
            for (int w = 0; w < width; w++) {
                BlockPos p = base.relative(side, w);
                BlockPos g = new BlockPos(p.getX(), mid.getY(), p.getZ());
                level.setBlock(g, GuhpolderFeature.POLDERIJS.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                BlockState boven = level.getBlockState(g.above());
                if (!boven.isAir() && vrij(boven)) {
                    level.setBlock(g.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            // the banks: frosty sprigs, and every few steps a knotwilg (on one side)
            for (int bank : new int[]{-1, width}) {
                BlockPos b = new BlockPos(base.relative(side, bank).getX(), mid.getY(), base.relative(side, bank).getZ());
                if (!grond(level.getBlockState(b)) || !level.getBlockState(b.above()).isAir()) {
                    continue;
                }
                if (wilgen && bank == wilgSide && Math.floorMod(step, 6) == 0 && random.nextInt(4) != 0) {
                    boom(level, random, b.relative(side, bank < 0 ? -1 : 1));
                } else if (random.nextInt(4) == 0) {
                    level.setBlock(b.above(), GuhpolderFeature.RIJPSPRIETJES.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            return true;
        }
    }

    // =================================================================================================================
    // the sneeuwguh-heuveltje
    // =================================================================================================================

    public static class Sneeuwguhheuvel extends Feature<NoneFeatureConfiguration> {
        public Sneeuwguhheuvel(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos mid = grondOnder(level, context.origin().getX(), context.origin().getZ());
            int r = 2 + random.nextInt(3);
            // only on flat polder ground, outside buildings
            for (int dx = -r - 1; dx <= r + 1; dx++) {
                for (int dz = -r - 1; dz <= r + 1; dz++) {
                    BlockPos g = grondOnder(level, mid.getX() + dx, mid.getZ() + dz);
                    if (Math.abs(g.getY() - mid.getY()) > 1 || BouwRuimte.inBuilding(level, g.above())) {
                        return false;
                    }
                }
            }
            if (!grond(level.getBlockState(mid))) {
                return false;
            }
            int top = 0;
            for (int dx = -r - 1; dx <= r + 1; dx++) {
                for (int dz = -r - 1; dz <= r + 1; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    double h = (r + 0.8 - d) * 0.75;   // a soft round dome
                    if (h <= 0) {
                        continue;
                    }
                    BlockPos g = new BlockPos(mid.getX() + dx, mid.getY(), mid.getZ() + dz);
                    int full = (int) Math.floor(h);
                    int layers = (int) Math.round((h - full) * 8);
                    for (int y = 1; y <= full; y++) {
                        if (vrij(level.getBlockState(g.above(y)))) {
                            level.setBlock(g.above(y), Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                    if (layers > 0 && vrij(level.getBlockState(g.above(full + 1)))) {
                        level.setBlock(g.above(full + 1), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, Math.min(7, layers)),
                                Block.UPDATE_CLIENTS);
                    }
                    if (full == 0) {
                        sneeuwig(level, g);
                    }
                    if (dx == 0 && dz == 0) {
                        top = full;
                    }
                }
            }
            // the sneeuwpopguh on top (sometimes a little family of two)
            Direction kijk = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            sneeuwpop(level, mid.above(top + 1), kijk);
            if (r >= 3 && random.nextInt(3) == 0) {
                BlockPos kind = mid.relative(kijk.getClockWise(), 2);
                int h = (int) Math.floor((r + 0.8 - 2) * 0.75);
                sneeuwpop(level, kind.above(h + 1), kijk);
            }
            if (random.nextInt(3) == 0) {   // an ijspegelguh-kristal peeks out of the snow
                BlockPos k = mid.relative(kijk.getOpposite(), r).above();
                if (level.getBlockState(k).isAir() && level.getBlockState(k.below()).isFaceSturdy(level, k.below(), Direction.UP)) {
                    level.setBlock(k, GuhpolderFeature.IJSPEGELGUH_KRISTAL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            return true;
        }

        /** A sneeuwpopguh (two blocks) standing at {@code pos}, when there is room. */
        static void sneeuwpop(WorldGenLevel level, BlockPos pos, Direction facing) {
            BlockState lower = level.getBlockState(pos);
            if (!vrij(lower) || !vrij(level.getBlockState(pos.above()))) {
                return;
            }
            BlockState pop = KnuffeldalFeature.SNEEUWPOPGUH.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            level.setBlock(pos, pop.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
            level.setBlock(pos.above(), pop.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        }
    }

    // =================================================================================================================
    // a patch of snow
    // =================================================================================================================

    /** A soft patch of snow (one or two layers, a roundish blob) on the rijpgras, which shows its snowy sides under it. */
    public static class Sneeuwplek extends Feature<NoneFeatureConfiguration> {
        public Sneeuwplek(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos o = context.origin();
            double r = 2 + random.nextInt(3) + random.nextDouble();
            double wobble = random.nextDouble() * Math.PI * 2;
            int n = 0;
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz) / (r * (1 + 0.25 * Math.sin(Math.atan2(dz, dx) * 3 + wobble)));
                    if (d > 1) {
                        continue;
                    }
                    BlockPos g = grondOnder(level, o.getX() + dx, o.getZ() + dz);
                    BlockPos boven = g.above();
                    if (!level.getBlockState(g).is(GuhpolderFeature.RIJPGRAS.get()) || !level.getBlockState(boven).isAir()
                            || BouwRuimte.inBuilding(level, boven)) {
                        continue;
                    }
                    int layers = d < 0.5 && random.nextInt(3) == 0 ? 2 : 1;
                    level.setBlock(boven, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers), Block.UPDATE_CLIENTS);
                    sneeuwig(level, g);
                    n++;
                }
            }
            return n > 0;
        }
    }

    // =================================================================================================================
    // a loose molentje
    // =================================================================================================================

    public static class LosMolentje extends Feature<NoneFeatureConfiguration> {
        public LosMolentje(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos g = grondOnder(level, context.origin().getX(), context.origin().getZ());
            if (!grond(level.getBlockState(g)) || !vrij(level.getBlockState(g.above())) || !level.getBlockState(g.above(2)).isAir()
                    || !level.getBlockState(g.above(3)).isAir() || BouwRuimte.inBuilding(level, g.above())) {
                return false;
            }
            level.setBlock(g.above(), GuhpolderFeature.KNOTWILG_STAM.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(g.above(2), GuhpolderFeature.GUH_MOLENTJE.get().defaultBlockState()
                    .setValue(MolentjeBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), Block.UPDATE_CLIENTS);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos p = g.relative(d);
                if (random.nextInt(3) == 0 && grond(level.getBlockState(p)) && level.getBlockState(p.above()).isAir()) {
                    level.setBlock(p.above(), (random.nextInt(4) == 0 ? GuhpolderFeature.GUH_IJSBLOEMPJE : GuhpolderFeature.RIJPSPRIETJES).get()
                            .defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            return true;
        }
    }
}
