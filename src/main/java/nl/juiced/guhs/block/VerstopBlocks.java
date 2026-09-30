package nl.juiced.guhs.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The blocks of the verstopguh house (hide-and-seek): one-way glass, the hidden spot markers and the way out. */
public final class VerstopBlocks {

    /**
     * One-way glass: seen from above it's pink glass you look straight through; seen from below it's a plain pink
     * ceiling. (Its model has a see-through top and an opaque bottom face, and a face is only drawn from the side it
     * faces, so from below you never see the top and from above never the bottom.) Walkable like any block.
     */
    public static class Eenrichtingsglas extends Block {
        public static final MapCodec<Eenrichtingsglas> CODEC = simpleCodec(Eenrichtingsglas::new);

        public Eenrichtingsglas(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected boolean skipRendering(BlockState state, BlockState neighbour, Direction side) {
            return neighbour.is(this) || super.skipRendering(state, neighbour, side);
        }

        @Override
        protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1.0f;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }
    }

    /**
     * An invisible marker in the verstopguh house: a spot where a guh can hide, or where the seekers start. You can't
     * see, touch or break it (like a barrier, but without the particles).
     */
    public static class Marker extends Block {
        public static final MapCodec<Marker> CODEC = simpleCodec(Marker::new);

        public Marker(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }
    }

    /** The way out of the verstopguh house: step on it and you're back on the roof (and out of the game). */
    public static class Uitgang extends Block {
        public static final MapCodec<Uitgang> CODEC = simpleCodec(Uitgang::new);
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

        public Uitgang(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (!level.isClientSide && entity instanceof ServerPlayer player && !player.isPassenger()) {
                nl.juiced.guhs.quest.VerstopGame.walkOut(player, pos);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 0.2,
                    pos.getZ() + random.nextDouble(), 0, 0.3, 0);
        }
    }

    private VerstopBlocks() {
    }
}
