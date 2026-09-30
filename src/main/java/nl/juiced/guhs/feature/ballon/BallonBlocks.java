package nl.juiced.guhs.feature.ballon;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The blocks of the Ballonfestival. */
public final class BallonBlocks {
    private BallonBlocks() {
    }

    /**
     * The ballonsteiger: the launch platform (planks with a striped festival edge and a little guh face). A balloon waits
     * on it and lands on it again; Kapitein Wolkje fetches one to it if there's none.
     */
    public static class Ballonsteiger extends Block {
        public static final MapCodec<Ballonsteiger> CODEC = simpleCodec(Ballonsteiger::new);

        public Ballonsteiger(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("gui.guhs.ballon.praat_met_wolkje").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** A mini luchtballon: a little guh-head balloon over a tiny basket, glowing softly (its burner). */
    public static class MiniLuchtballon extends Block {
        public static final MapCodec<MiniLuchtballon> CODEC = simpleCodec(MiniLuchtballon::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(6, 0, 6, 10, 3, 10), Block.box(3, 5, 3, 13, 15, 13));

        public MiniLuchtballon(Properties properties) {
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
        protected boolean isPathfindable(BlockState state, PathComputationType type) {
            return false;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(5) == 0) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, 0, 0.01, 0);
            }
        }
    }
}
