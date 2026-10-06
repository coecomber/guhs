package nl.juiced.guhs.feature.ringsausuman;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * De Ringenbakker: the face (with the lever) of Sausuman's great machine, the one he wants to bake his own Knabbelring with.
 * A click is the lever ({@link Bakkerij#klikBakker}): with the three ingredients in a player's pockets it bakes, in a
 * cutscene, and what rolls out is an onion ring. The block itself never changes; every player bakes their own.
 */
public class RingenbakkerBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RingenbakkerBlock> CODEC = simpleCodec(RingenbakkerBlock::new);

    public RingenbakkerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Bakkerij.klikBakker(sp, pos, state);
        }
        return InteractionResult.SUCCESS;
    }

    /** It never quite stands still: a wisp of smoke from its mouth now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) {
            return;
        }
        Direction kijkt = state.getValue(FACING);
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + kijkt.getStepX() * 0.55 + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 0.3,
                pos.getZ() + 0.5 + kijkt.getStepZ() * 0.55 + (random.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
    }
}
