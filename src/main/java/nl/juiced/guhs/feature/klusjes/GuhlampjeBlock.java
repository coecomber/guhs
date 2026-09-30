package nl.juiced.guhs.feature.klusjes;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.level.ScheduledTickAccess;
/**
 * Het guhlampje (klusjes): a little standing lamp shaped like a guh head: two fluffy ears on its cap and a glowing
 * snoet-face in the glass. Right-click switches it on or off; huisje residents with the chore "lampjes" switch it on in the
 * evening and off in the morning (with a big yawn). In the block tag guhs:klusjes/lampjes (candles too).
 */
public class GuhlampjeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<GuhlampjeBlock> CODEC = simpleCodec(GuhlampjeBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 2, 12), Block.box(5, 2, 5, 11, 10, 11),
            Block.box(4, 10, 4, 12, 12, 12));

    public GuhlampjeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbour, random);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            zet(level, pos, !state.getValue(LIT));
        }
        return InteractionResult.SUCCESS;
    }

    /** Switches any lamp with a LIT property (a guhlampje, a candle...): true when it changed. */
    public static boolean zet(Level level, BlockPos pos, boolean aan) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(LIT) || state.getValue(LIT) == aan) {
            return false;
        }
        if (aan && state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return false;                                            // (a candle under water stays out)
        }
        level.setBlock(pos, state.setValue(LIT, aan), Block.UPDATE_ALL);
        level.playSound(null, pos, KlusjesFeature.LAMPJE.get(), SoundSource.BLOCKS, 0.6f, aan ? 1.4f : 1.0f);
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.5 + random.nextDouble() * 0.3,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.004, 0);
        }
    }
}
