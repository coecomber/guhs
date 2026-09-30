package nl.juiced.guhs.feature.onderwater;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
/**
 * A reuzenschelp: a giant pink guh shell. Open, it holds a pearl: right-click it and the pearl is yours (the shell
 * closes). A closed shell grows a new pearl by itself now and then (random ticks, about 1 in {@link #REGROW_CHANCE}),
 * so the Guhbubbel never runs out of pearls, it just takes patience.
 */
public class ReuzenschelpBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<ReuzenschelpBlock> CODEC = simpleCodec(ReuzenschelpBlock::new);
    public static final BooleanProperty PAREL = BooleanProperty.create("parel");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final int REGROW_CHANCE = 4;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 7, 16);

    public ReuzenschelpBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PAREL, true).setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PAREL, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(PAREL);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PAREL) && random.nextInt(REGROW_CHANCE) == 0) {
            regrow(level, pos, state);
        }
    }

    /** A new pearl: the shell opens again. */
    public static void regrow(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(PAREL, true), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.01);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 1.3f);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            take(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            take(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Takes the pearl out of the shell at pos (it closes); false (and a hint) when the shell is still closed. */
    public static boolean take(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ReuzenschelpBlock)) {
            return false;
        }
        if (!state.getValue(PAREL)) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.onderwater.shell_closed").withStyle(ChatFormatting.AQUA));
            level.playSound(null, pos, SoundEvents.BONE_BLOCK_HIT, SoundSource.BLOCKS, 0.8f, 1.2f);
            return false;
        }
        level.setBlock(pos, state.setValue(PAREL, false), Block.UPDATE_ALL);
        ItemStack pearl = new ItemStack(OnderwaterFeature.PAREL.get());
        if (!player.getInventory().add(pearl)) {
            player.drop(pearl, false);
        }
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 10, 0.25, 0.2, 0.25, 0.03);
        level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.8f, 1.4f);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        player.sendOverlayMessage(Component.translatable("quest.guhs.onderwater.shell_pearl").withStyle(ChatFormatting.AQUA));
        return true;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
