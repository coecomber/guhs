package nl.juiced.guhs.feature.blub.knuffel;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.blub.BlubGeluid;

/**
 * 1.8.0: the Blub-knuffel, a plush of your own Blub-guh (like the real Mattel Blub plush): same soort, meter, tint and
 * strikje (its block entity keeps them, the client draws that Blub small). It looks at whoever puts it down; squeeze it
 * (right-click) and it squishes, with a heart. Made with the button "Knuffel laten maken" in the Blub's menu.
 */
public class BlubKnuffelBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<BlubKnuffelBlock> CODEC = simpleCodec(BlubKnuffelBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 9, 13);

    public BlubKnuffelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;   // (the plush itself is drawn by its block entity renderer)
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlubKnuffelBlockEntity(pos, state);
    }

    /** A squeeze: a squish and a heart. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            level.playSound(null, pos, BlubGeluid.SQUISH.get(), SoundSource.BLOCKS, 0.8f, 0.95f + level.getRandom().nextFloat() * 0.15f);
            server.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, 1, 0.1, 0.05, 0.1, 0.0);
        }
        return InteractionResult.SUCCESS;
    }
}
