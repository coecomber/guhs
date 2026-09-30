package nl.juiced.guhs.feature.sterrenwacht;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The guh_telescoop: a little brass telescope with two guh ears on a tripod. Right-click it at night and you look
 * through it: connect the stars into a guh constellation ({@link Sterrenkijken}). Professor Sterretje has a big one in
 * the Guh-Sterrenwacht, and sells small ones for wenssterren.
 */
public class TelescoopBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TelescoopBlock> CODEC = simpleCodec(TelescoopBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 11, 13), Block.box(4, 10, 4, 12, 16, 12));

    public TelescoopBlock(Properties properties) {
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Sterrenkijken.kijk(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** At night a little star twinkles above the lens now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0 && Sterrenkijken.donker(level)) {
            level.addParticle(SterrenwachtFeature.WENSSTER_DEELTJE.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 1.2,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
        }
    }
}
