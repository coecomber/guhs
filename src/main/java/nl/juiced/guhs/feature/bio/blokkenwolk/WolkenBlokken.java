package nl.juiced.guhs.feature.bio.blokkenwolk;

import java.util.function.Function;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The wolkenblok, its slab and its stairs. Cloud is soft:
 * <ul>
 *   <li>you sink {@link #ZAK} pixels into every top surface (the collision shape is the shape lowered by that much, the way
 *       soul sand does it; what you see, click and build on stays the whole shape);</li>
 *   <li>landing on it never hurts, from any height, and it does not bounce.</li>
 * </ul>
 * Cheap on purpose (natural cloud banks are made of thousands of these): no block entity, no ticks, no state of its own, and
 * a solid cube that culls against its neighbours like any stone. Because the collision shape is never a whole cube, nothing
 * can suffocate in cloud.
 */
public final class WolkenBlokken {
    /** How far you sink in, in pixels (soul sand: 2). */
    public static final double ZAK = 2;
    /** From this fall on, landing makes a puff and a sound. */
    public static final double PLOF_VANAF = 3;

    /** The shape with every top surface {@link #ZAK} pixels lower: what is both in the shape and in the shape moved down. */
    public static VoxelShape zacht(VoxelShape vorm) {
        return Shapes.join(vorm, vorm.move(0, -ZAK / 16.0, 0), BooleanOp.AND).optimize();
    }

    /** Landing on cloud (or rainbow): no damage at all, a puff of cloud from a real fall. */
    public static void zachtLanden(Level level, BlockPos pos, Entity entity, double valhoogte) {
        if (valhoogte < PLOF_VANAF || !(entity instanceof LivingEntity) || !(level instanceof ServerLevel server)) {
            return;
        }
        int n = (int) Math.min(14, 4 + valhoogte);
        server.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.1, entity.getZ(), n, 0.3, 0.05, 0.3, 0.02);
        server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), BlokkenWolkSlice.WOLK_STAP.get(), SoundSource.BLOCKS,
                (float) Math.min(1.0, 0.4 + valhoogte * 0.04), 0.8f);
    }

    public static class Blok extends Block {
        public static final MapCodec<Blok> CODEC = simpleCodec(Blok::new);
        private static final VoxelShape ZACHT = zacht(Shapes.block());

        public Blok(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return ZACHT;
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
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            zachtLanden(level, pos, entity, fallDistance);
        }
    }

    public static class Plaat extends SlabBlock {
        public static final MapCodec<Plaat> CODEC = simpleCodec(Plaat::new);
        private final Function<BlockState, VoxelShape> zacht;

        public Plaat(Properties properties) {
            super(properties);
            this.zacht = getShapeForEachState(s -> WolkenBlokken.zacht(super.getShape(s, null, null, null)), WATERLOGGED);
        }

        @Override
        public MapCodec<? extends SlabBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return zacht.apply(state);
        }

        @Override
        protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
            return getShape(state, level, pos, CollisionContext.empty());
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            zachtLanden(level, pos, entity, fallDistance);
        }
    }

    public static class Trap extends StairBlock {
        private final Function<BlockState, VoxelShape> zacht;

        public Trap(BlockState baseState, Properties properties) {
            super(baseState, properties);
            this.zacht = getShapeForEachState(s -> WolkenBlokken.zacht(super.getShape(s, null, null, null)), WATERLOGGED);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return zacht.apply(state);
        }

        @Override
        protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
            return getShape(state, level, pos, CollisionContext.empty());
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            zachtLanden(level, pos, entity, fallDistance);
        }
    }

    private WolkenBlokken() {
    }
}
