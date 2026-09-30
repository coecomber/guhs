package nl.juiced.guhs.feature.guhpolder;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.knus.KnusTags;

/**
 * The guh-molentje: a little white windmill with a guh face and ears on its cap; its sails turn all day (faster when it
 * snows or rains, fastest in a storm; drawn by client.MolentjeRenderer). Right-click with knabbelgraan to pour it in; the
 * molentje grinds it into knabbelmeel ({@link MolentjeBlockEntity}). Right-click again to take the meel out (sneak with an
 * empty hand: take the graan back). Hoppers work too (graan in from the top/sides, meel out at the bottom).
 */
public class MolentjeBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<MolentjeBlock> CODEC = simpleCodec(MolentjeBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 10, 12), Block.box(3.5, 10, 3.5, 12.5, 14, 12.5));

    public MolentjeBlock(Properties properties) {
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
        // the sails (and the guh face) look at the player who puts it down
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MolentjeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != GuhpolderFeature.GUH_MOLENTJE_BE.get()) {
            return null;
        }
        return level.isClientSide() ? (BlockEntityTicker<T>) (BlockEntityTicker<MolentjeBlockEntity>) (l, p, s, be) -> be.clientTick()
                : (BlockEntityTicker<T>) (BlockEntityTicker<MolentjeBlockEntity>) (l, p, s, be) -> be.serverTick();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(KnusTags.KNABBELGRAAN)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof MolentjeBlockEntity molen)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            int in = molen.stort(stack);
            if (in > 0) {
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.8f, 1.2f);
                player.sendOverlayMessage(Component.translatable("gui.guhs.guhpolder.molentje.gestort", in).withStyle(ChatFormatting.AQUA));
            } else {
                player.sendOverlayMessage(Component.translatable("gui.guhs.guhpolder.molentje.vol").withStyle(ChatFormatting.GOLD));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MolentjeBlockEntity molen)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack meel = molen.neemMeel();
            if (!meel.isEmpty()) {
                int n = meel.getCount();
                if (!player.addItem(meel)) {
                    player.drop(meel, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 0.9f);
                player.sendOverlayMessage(Component.translatable("gui.guhs.guhpolder.molentje.meel", n).withStyle(ChatFormatting.AQUA));
            } else if (player.isShiftKeyDown() && !molen.graan().isEmpty()) {
                ItemStack graan = molen.neemGraan();
                if (!player.addItem(graan)) {
                    player.drop(graan, false);
                }
            } else {
                player.sendOverlayMessage(molen.graan().isEmpty()
                        ? Component.translatable("gui.guhs.guhpolder.molentje.leeg").withStyle(ChatFormatting.GRAY)
                        : Component.translatable("gui.guhs.guhpolder.molentje.maalt", molen.graan().getCount()).withStyle(ChatFormatting.AQUA));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MolentjeBlockEntity molen) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), molen.graan().copy());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), molen.meel().copy());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
