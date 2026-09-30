package nl.juiced.guhs.feature.boerderij;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * De guh_voerbak: a pink trough with a guh face on the front. Fill it with knabbelvoer (up to {@link #MAX} portions): the
 * farm animals around that haven't eaten today walk to it and eat a portion by themselves (that counts as feeding them).
 */
public class GuhVoerbakBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<GuhVoerbakBlock> CODEC = simpleCodec(GuhVoerbakBlock::new);
    public static final int MAX = 4;
    public static final IntegerProperty VOER = IntegerProperty.create("voer", 0, MAX);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 2, 15), Block.box(1, 2, 1, 15, 8, 2.5), Block.box(1, 2, 13.5, 15, 8, 15),
            Block.box(1, 2, 2.5, 2.5, 8, 13.5), Block.box(13.5, 2, 2.5, 15, 8, 13.5));

    public GuhVoerbakBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(VOER, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VOER);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(BoerderijFeature.KNABBELVOER.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.getValue(VOER) >= MAX) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("gui.guhs.boerderij.voerbak.vol").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return ItemInteractionResult.CONSUME;
        }
        if (!level.isClientSide) {
            vul(level, pos, (ServerPlayer) player);
            stack.consume(1, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** One portion of knabbelvoer in (a chore for Boerin Hooibaal). */
    public static void vul(Level level, BlockPos pos, @Nullable ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(VOER, Math.min(MAX, state.getValue(VOER) + 1)), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1f, 1.2f);
        if (player != null) {
            Hooibaal.voerbakGevuld(player);
        }
    }

    public static int voer(BlockState state) {
        return state.getBlock() instanceof GuhVoerbakBlock ? state.getValue(VOER) : 0;
    }

    /** An animal eats a portion here: true when there was something in it. */
    public static boolean eet(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (voer(state) <= 0) {
            return false;
        }
        level.setBlock(pos, state.setValue(VOER, state.getValue(VOER) - 1), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.8f, 1.2f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                    4, 0.3, 0.1, 0.3, 0.0);
        }
        return true;
    }

    /** The nearest voerbak with something in it (within r blocks sideways, 3 up and down), or null. */
    @Nullable
    public static BlockPos vindGevuld(Level level, BlockPos from, int r) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-r, -3, -r), from.offset(r, 3, r))) {
            if (voer(level.getBlockState(p)) > 0) {
                double d = p.distSqr(from);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(VOER) * 3;
    }
}
