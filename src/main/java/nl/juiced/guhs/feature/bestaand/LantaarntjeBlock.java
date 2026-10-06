package nl.juiced.guhs.feature.bestaand;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Het Zielig lantaarntje (the soul lantern parody, the Wachter-guh's reward): a little lantern with guh ears and a
 * pitiful face in its turquoise glass. It stands on a block or hangs under one, like a lantern. Pat it (right-click) and
 * it cheers up: a happy face and a warmer, brighter light ({@code blij}); pat it again and it goes back to looking
 * pitiful, as it should.
 */
public class LantaarntjeBlock extends Block {
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final BooleanProperty BLIJ = BooleanProperty.create("blij");
    private static final VoxelShape STAAND = Block.box(3.5, 0, 3.5, 12.5, 13, 12.5);
    private static final VoxelShape HANGEND = Block.box(3.5, 2, 3.5, 12.5, 16, 12.5);

    public LantaarntjeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HANGING, false).setValue(BLIJ, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HANGING, BLIJ);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis() == Direction.Axis.Y) {
                BlockState state = defaultBlockState().setValue(HANGING, direction == Direction.UP);
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? HANGEND : STAAND;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction steun = state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
        return Block.canSupportCenter(level, pos.relative(steun), steun.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Direction steun = state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
        return directionToNeighbour == steun && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** A pat: it cheers up (or goes back to pitiful). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            boolean blij = !state.getValue(BLIJ);
            level.setBlock(pos, state.setValue(BLIJ, blij), Block.UPDATE_ALL);
            double y = pos.getY() + (state.getValue(HANGING) ? 0.6 : 0.5);
            if (blij) {
                server.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, y + 0.4, pos.getZ() + 0.5, 3, 0.25, 0.15, 0.25, 0.0);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, 1.5f);
            } else {
                server.sendParticles(ParticleTypes.FALLING_WATER, pos.getX() + 0.5, y, pos.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0.0);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, 0.7f);
            }
            if (player instanceof ServerPlayer p) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.lantaarntje." + (blij ? "blij" : "zielig"))
                        .withStyle(blij ? ChatFormatting.GOLD : ChatFormatting.AQUA));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
