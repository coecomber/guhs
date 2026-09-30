package nl.juiced.guhs.feature.balto;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * The Sneeuwguhtoendra's own worldgen features (placed by the biome, see tools/features/balto.py; all of them stay out of
 * buildings):
 * <ul>
 *   <li>{@link Sneeuwguhspar}: a spruce-like guh tree: a straight trunk with one sleepy guh face at eye height, and a cone
 *       of dark blue-green needle layers, every top covered with a snow cap (also what the sapling grows);</li>
 *   <li>{@link Sneeuwduin}: a long soft snow drift of snow layers, piled up by the wind;</li>
 *   <li>{@link Sneeuwkei}: a round boulder with a thick snow hat.</li>
 * </ul>
 */
public final class BaltoWorldgen {
    private BaltoWorldgen() {
    }

    /** Ground a tree or boulder may stand on in the tundra. */
    static boolean grond(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL);
    }

    /** Can a tree or snow make room here? */
    static boolean vrij(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(Blocks.SNOW);
    }

    static BlockPos grondOnder(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
    }

    // =================================================================================================================
    // the sneeuwguhspar
    // =================================================================================================================

    public static class Sneeuwguhspar extends Feature<NoneFeatureConfiguration> {
        public Sneeuwguhspar(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos o = context.origin();
            BlockPos ground = o.below();
            // from a sapling the origin is the sapling's own spot; from the biome the first free spot above the ground
            if (!grond(level.getBlockState(ground))) {
                ground = grondOnder(level, o.getX(), o.getZ());
            }
            return boom(level, context.random(), ground, context.random().nextInt(4) == 0);
        }
    }

    /**
     * A sneeuwguhspar on {@code ground}: trunk 6-8 (groot: 10-13) with a face at 2 blocks, needle layers every block from
     * the 3rd log up (radius waving 1-2-1-3-2-... wider towards the bottom, like a spruce), a single tip, snow caps on the
     * needles that have nothing above them and a snow layer on the tip. False (nothing built) without room.
     */
    public static boolean boom(WorldGenLevel level, RandomSource random, BlockPos ground, boolean groot) {
        if (!grond(level.getBlockState(ground)) || BouwRuimte.inBuilding(level, ground.above())) {
            return false;
        }
        int hoog = groot ? 10 + random.nextInt(4) : 6 + random.nextInt(3);
        int breed = groot ? 4 : 3;
        for (int dy = 1; dy <= hoog + 1; dy++) {
            int r = dy < 3 ? 0 : 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (!vrij(level.getBlockState(ground.offset(dx, dy, dz))) && !level.getBlockState(ground.offset(dx, dy, dz)).is(BlockTags.LEAVES)) {
                        return false;
                    }
                }
            }
        }
        BlockState stam = BaltoFeature.SPAR_STAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState naald = BaltoFeature.SPAR_NAALDEN.get().defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
        Direction kijk = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        for (int dy = 1; dy <= hoog; dy++) {
            BlockState s = dy == 2 ? BaltoFeature.SPAR_GEZICHT.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, kijk) : stam;
            level.setBlock(ground.above(dy), s, Block.UPDATE_CLIENTS);
        }
        java.util.List<BlockPos> naalden = new java.util.ArrayList<>();
        int bodem = 3;
        for (int dy = bodem; dy <= hoog + 1; dy++) {
            int vanTop = hoog + 1 - dy;
            int r;
            if (vanTop == 0) {
                r = 0;
            } else {
                // a wavy cone: wider towards the bottom, every other layer a step in
                float f = (float) vanTop / (hoog + 1 - bodem);
                r = Math.max(1, Math.round(f * breed + (vanTop % 2 == 0 ? -0.6f : 0.4f)));
            }
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0 && dy <= hoog) {
                        continue;
                    }
                    float d = Mth.sqrt(dx * dx + dz * dz);
                    if (d > r + 0.35f || (d > r - 0.4f && r >= 2 && random.nextInt(4) == 0)) {
                        continue;
                    }
                    BlockPos p = ground.offset(dx, dy, dz);
                    if (vrij(level.getBlockState(p))) {
                        level.setBlock(p, naald, Block.UPDATE_CLIENTS);
                        naalden.add(p);
                    }
                }
            }
        }
        BlockPos top = ground.above(hoog + 2);
        if (vrij(level.getBlockState(top))) {
            level.setBlock(top, naald, Block.UPDATE_CLIENTS);
            naalden.add(top);
        }
        for (BlockPos p : naalden) {
            if (level.getBlockState(p.above()).isAir()) {
                level.setBlock(p, level.getBlockState(p).setValue(BaltoBlocks.SNEEUW, true), Block.UPDATE_CLIENTS);
            }
        }
        if (level.getBlockState(top.above()).isAir()) {
            level.setBlock(top.above(), Blocks.SNOW.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    // =================================================================================================================
    // snow drifts and boulders
    // =================================================================================================================

    /** A long soft snow drift: an ellipse of snow layers, highest in the middle (up to 5 layers), along the wind. */
    public static class Sneeuwduin extends Feature<NoneFeatureConfiguration> {
        public Sneeuwduin(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos o = context.origin();
            if (BouwRuimte.inBuilding(level, o)) {
                return false;
            }
            float len = 3 + random.nextFloat() * 4, wid = 1.5f + random.nextFloat() * 1.5f;
            float hoek = 0.35f + random.nextFloat() * 0.5f;          // (the wind blows from the north-west here)
            float cos = Mth.cos(hoek), sin = Mth.sin(hoek);
            boolean iets = false;
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    float u = (dx * cos + dz * sin) / len, v = (-dx * sin + dz * cos) / wid;
                    float d = u * u + v * v;
                    if (d > 1) {
                        continue;
                    }
                    int lagen = Mth.clamp(Math.round((1 - d) * 5.4f), 1, 5);
                    BlockPos g = grondOnder(level, o.getX() + dx, o.getZ() + dz);
                    BlockState onder = level.getBlockState(g);
                    BlockPos p = g.above();
                    if (onder.is(Blocks.SNOW)) {
                        p = g;
                        onder = level.getBlockState(g.below());
                    }
                    if (!grond(onder) || !vrij(level.getBlockState(p)) || BouwRuimte.inBuilding(level, p)) {
                        continue;
                    }
                    level.setBlock(p, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, lagen), Block.UPDATE_CLIENTS);
                    iets = true;
                }
            }
            return iets;
        }
    }

    /** A round snowy boulder (stone and andesite, a thick snow hat and a few snow layers around it). */
    public static class Sneeuwkei extends Feature<NoneFeatureConfiguration> {
        public Sneeuwkei(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos g = grondOnder(level, context.origin().getX(), context.origin().getZ());
            if (!grond(level.getBlockState(g)) || BouwRuimte.inBuilding(level, g.above())) {
                return false;
            }
            float r = 1.4f + random.nextFloat() * 1.2f;
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    for (int dy = -1; dy <= ri; dy++) {
                        float d = Mth.sqrt(dx * dx + dz * dz + dy * dy * 1.6f);
                        if (d > r) {
                            continue;
                        }
                        BlockPos p = g.offset(dx, dy, dz);
                        BlockState s = level.getBlockState(p);
                        if (vrij(s) || grond(s)) {
                            level.setBlock(p, random.nextInt(3) == 0 ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
            // the snow hat: the top blocks of the boulder become snow, with a layer on top
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    for (int dy = ri; dy >= -1; dy--) {
                        BlockPos p = g.offset(dx, dy, dz);
                        BlockState s = level.getBlockState(p);
                        if (s.is(Blocks.STONE) || s.is(Blocks.ANDESITE)) {
                            if (dy >= 0 && random.nextInt(3) != 0) {
                                level.setBlock(p, Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                            }
                            if (vrij(level.getBlockState(p.above()))) {
                                level.setBlock(p.above(), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + random.nextInt(2)),
                                        Block.UPDATE_CLIENTS);
                            }
                            break;
                        }
                    }
                }
            }
            return true;
        }
    }
}
