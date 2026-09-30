package nl.juiced.guhs.feature.barbecuether;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** The small block classes of the Barbecuether (the big ones - portal, sauce - have their own files). */
public final class BarbecueBlocks {
    /** Houtskoolsteen: the netherrack of the Barbecuether. A fire on top burns forever, in every dimension. */
    public static class Houtskoolsteen extends Block {
        public Houtskoolsteen(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
            return direction == Direction.UP;
        }
    }

    /**
     * Pindasaus- and mosterd-nylium: houtskoolsteen with a crust of sauce. Covered up it turns back into houtskoolsteen;
     * bone meal grows the biome's plants around it (like crimson / warped nylium).
     */
    public static class GrillNylium extends Block implements BonemealableBlock {
        private final boolean mosterd;

        public GrillNylium(boolean mosterd, Properties properties) {
            super(properties);
            this.mosterd = mosterd;
        }

        private static boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
            BlockPos above = pos.above();
            BlockState up = level.getBlockState(above);
            int light = LightEngine.getLightBlockInto(level, state, pos, up, above, Direction.UP, up.getLightBlock(level, above));
            return light < level.getMaxLightLevel();
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!canStay(state, level, pos)) {
                level.setBlockAndUpdate(pos, BarbecuetherFeature.HOUTSKOOLSTEEN.get().defaultBlockState());
            }
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return level.getBlockState(pos.above()).isAir();
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            Block plant = (mosterd ? BarbecuetherFeature.MOSTERDSCHEUTJES : BarbecuetherFeature.PINDASCHEUTJES).get();
            Block fungus = (mosterd ? BarbecuetherFeature.WORST_ZWAMMETJE : BarbecuetherFeature.SATE_ZWAMMETJE).get();
            for (int i = 0; i < 24; i++) {
                BlockPos p = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1 + 1, random.nextInt(7) - 3);
                BlockState below = level.getBlockState(p.below());
                if (level.getBlockState(p).isAir() && below.is(BlockTags.NYLIUM)) {
                    level.setBlockAndUpdate(p, (random.nextInt(8) == 0 ? fungus : plant).defaultBlockState());
                }
            }
        }

        @Override
        public BonemealableBlock.Type getType() {
            return BonemealableBlock.Type.NEIGHBOR_SPREADER;
        }
    }

    /** Little plants that grow on nylium, houtskoolsteen and ash (pindascheutjes, mosterdscheutjes, smeulkooltjes). */
    public static class GrillPlant extends BushBlock {
        public static final MapCodec<GrillPlant> CODEC = simpleCodec(GrillPlant::new);
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

        public GrillPlant(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BushBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(BlockTags.NYLIUM) || state.is(BarbecuetherFeature.HOUTSKOOLSTEEN.get()) || state.is(BarbecuetherFeature.AS_AARDE.get())
                    || state.is(BarbecuetherFeature.AS_BLOK.get()) || state.is(BlockTags.DIRT) || super.mayPlaceOn(state, level, pos);
        }
    }

    /** Smeulkooltjes: little glowing embers on the ground; they spark now and then. */
    public static class Smeulkooltjes extends GrillPlant {
        private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(1.0f, 0.45f, 0.08f), 0.7f);

        public Smeulkooltjes(Properties properties) {
            super(properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) == 0) {
                level.addParticle(EMBER, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.3, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                        0, 0.02, 0);
            }
            if (random.nextInt(30) == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0, 0.01, 0);
            }
        }
    }

    /** A puddle of pindasaus: sticky like honey, you wade through it slowly. */
    public static class Pindasausplasje extends CarpetBlock {
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

        public Pindasausplasje(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (level.isClientSide && level.random.nextInt(20) == 0) {
                level.addParticle(ParticleTypes.FALLING_HONEY, entity.getX(), pos.getY() + 0.1, entity.getZ(), 0, 0, 0);
            }
            super.stepOn(level, pos, state, entity);
        }
    }

    /** A smoke vent of the Rookdelta: a grate in the ground that puffs thick columns of barbecue smoke. */
    public static class Rookgat extends Block {
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

        public Rookgat(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextFloat() < 0.35f) {
                for (int i = 0; i < random.nextInt(2) + 1; i++) {
                    CampfireBlock.makeParticles(level, pos, true, random.nextInt(3) == 0);
                }
            }
            if (random.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 0, 0, 0);
            }
        }
    }

    private BarbecueBlocks() {
    }
}
