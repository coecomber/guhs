package nl.juiced.guhs.feature.einde2.biomes;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.biomeguhs.Biomeguhs;

/**
 * einde2, slice biomes: the block classes of the outer islands that do more than stand there (registered in
 * {@link E2BiomesFeature}): the small plants, the crumb sand and its layers, the two clouds, the kristalkaas cluster and
 * the lichtstaafje. The knabbelkoor has its own file ({@link Knabbelkoor}).
 */
public final class E2BiomesBlokken {
    /** Does a plant of the outer islands stand on this? (The ground of the islands, or plain earth in somebody's garden.) */
    public static boolean grond(BlockState state) {
        return state.is(Einde2Ids.EILANDGROND) || state.is(BlockTags.DIRT);
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A small plant on the ground of the islands: korstgras, the dorre korststruik, the schimmelpaddenstoel. */
    public static class Plantje extends VegetationBlock {
        public static final MapCodec<Plantje> CODEC = simpleCodec(p -> new Plantje(12, 10, p));
        private final VoxelShape shape;

        public Plantje(int breed, int hoog, BlockBehaviour.Properties properties) {
            super(properties);
            this.shape = Block.column(breed, 0, hoog);
        }

        @Override
        protected MapCodec<? extends VegetationBlock> codec() {
            return CODEC;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return grond(state) || state.is(E2BiomesFeature.KRUIMELZAND.get());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return this.shape.move(state.getOffset(pos));
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Crumb sand: falls like sand, and you sink in a little (it is a touch lower than a block) and wade slowly. A Duinguh stands on top. */
    public static class Kruimelzand extends ColoredFallingBlock {
        private static final VoxelShape ZAK = Block.column(16, 0, 14);

        public Kruimelzand(ColorRGBA stof, BlockBehaviour.Properties properties) {
            super(stof, properties);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            // (the Duinguh never sinks in: Biomeguhs.zaktNiet only looks at the synced variant, so both sides agree)
            if (context instanceof EntityCollisionContext ecc && ecc.getEntity() != null && Biomeguhs.zaktNiet(ecc.getEntity())) {
                return Shapes.block();
            }
            return ZAK;
        }

        @Override
        protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
            return Shapes.block();
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.block();
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType type) {
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** One to eight layers of crumb sand (the dunes): a snow layer that never melts and lies on anything solid. */
    public static class Kruimelzandlaag extends SnowLayerBlock {
        public static final MapCodec<SnowLayerBlock> CODEC = simpleCodec(Kruimelzandlaag::new);

        public Kruimelzandlaag(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<SnowLayerBlock> codec() {
            return CODEC;
        }

        @Override
        protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
            BlockState onder = level.getBlockState(pos.below());
            return Block.isFaceFull(onder.getCollisionShape(level, pos.below()), Direction.UP) || onder.is(this) && onder.getValue(LAYERS) == 8
                    || onder.is(E2BiomesFeature.KRUIMELZAND.get());
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            // (crumbs do not melt)
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * A crumb cloud. Nobody stands on it: you sink through it slowly and land softly (no fall damage). Only whoever
     * {@link Wolken#draagt} names walks on it like on a floor (and still drops through it by sneaking).
     */
    public static class Kruimelwolk extends Block {
        /** The slowest you sink while inside a cloud (blocks per tick, downwards). */
        public static final double ZAKKEN = -0.07;

        public Kruimelwolk(BlockBehaviour.Properties properties) {
            super(properties);
        }

        /** The carrying rule on its own (the game tests ask it): does this cloud hold up this entity right now? */
        public static boolean houdtTegen(Entity entity, boolean erboven, boolean daalt) {
            return entity instanceof FallingBlockEntity || Wolken.draagt(entity) && erboven && !daalt;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (!context.isPlacement() && context instanceof EntityCollisionContext ecc && ecc.getEntity() != null
                    && houdtTegen(ecc.getEntity(), context.isAbove(Shapes.block(), pos, false), context.isDescending())) {
                return Shapes.block();
            }
            return Shapes.empty();
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean skipRendering(BlockState state, BlockState neighbour, Direction direction) {
            return neighbour.is(this) || super.skipRendering(state, neighbour, direction);
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0f;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
            if (entity instanceof FallingBlockEntity) {
                return;
            }
            // a soft landing: whoever falls in is braked to a slow sink, with a puff
            if (entity.fallDistance > 3.0 && level instanceof ServerLevel server) {
                server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), E2BiomesFeature.WOLK_PLOF.get(), SoundSource.BLOCKS, 0.7f,
                        0.9f + server.getRandom().nextFloat() * 0.3f);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.2, entity.getZ(), 8, 0.4, 0.2, 0.4, 0.02);
            }
            Vec3 v = entity.getDeltaMovement();
            if (v.y < ZAKKEN && !entity.isNoGravity()) {
                entity.setDeltaMovement(v.x, ZAKKEN, v.z);
            }
            entity.resetFallDistance();
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            // (whoever the cloud carries lands softly too)
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType type) {
            return type == PathComputationType.AIR;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Pressed cloud: a solid building block that breaks every fall. */
    public static class DichteWolk extends Block {
        public DichteWolk(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            // (no fall damage)
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A cluster of kristalkaas: an amethyst cluster in gold, on any side of a block. */
    public static class Kristal extends AmethystClusterBlock {
        public Kristal(BlockBehaviour.Properties properties) {
            super(7.0f, 10.0f, properties);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** The lichtstaafje: an end rod that sheds warm pink sparks. */
    public static class Lichtstaafje extends EndRodBlock {
        private static final DustParticleOptions VONK = new DustParticleOptions(0xFFC8E6, 0.6f);

        public Lichtstaafje(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(6) != 0) {
                return;
            }
            Direction d = state.getValue(FACING);
            double r = 0.4 - (random.nextFloat() + random.nextFloat()) * 0.4;
            level.addParticle(VONK, pos.getX() + 0.5 + d.getStepX() * r + (random.nextFloat() - 0.5) * 0.1,
                    pos.getY() + 0.5 + d.getStepY() * r + (random.nextFloat() - 0.5) * 0.1,
                    pos.getZ() + 0.5 + d.getStepZ() * r + (random.nextFloat() - 0.5) * 0.1, 0, 0.01, 0);
        }
    }

    private E2BiomesBlokken() {
    }
}
