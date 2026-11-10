package nl.juiced.guhs.feature.vadsrijk.vitrine;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Y-VITRINE: the Vadsrijk-vitrine block (see {@link Vitrine}). A glass case on a pedestal (block model from
 * tools/features/vadsrijk_y_vitrine.py); the statue inside and the floating text are drawn by the client renderer. Faces the
 * placer; the placer becomes the owner. Refuses to be placed in Het Vadsrijk / Het Knuffelkwartier. Click = a chat summary.
 */
public class VitrineBlock extends BaseEntityBlock {
    public static final MapCodec<VitrineBlock> CODEC = simpleCodec(VitrineBlock::new);
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 16, 15);

    public VitrineBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (!Vitrine.magHier(ctx.getLevel())) {
            if (ctx.getPlayer() instanceof ServerPlayer p) {
                p.sendOverlayMessage(Component.translatable(Vitrine.K + "niet_hier").withStyle(ChatFormatting.GOLD));
            }
            return null;
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer p && level.getBlockEntity(pos) instanceof VitrineBlockEntity be) {
            be.zetEigenaar(p);
            p.sendSystemMessage(Component.translatable(Vitrine.K + "geplaatst").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p && level.getBlockEntity(pos) instanceof VitrineBlockEntity be) {
            be.klik(p);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem && !player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VitrineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Vitrine.BE.get(), VitrineBlockEntity::serverTick);
    }

    /** The item: a tooltip that says what it is and where it may stand. */
    public static final class VitrineItem extends BlockItem {
        public VitrineItem(Block b, Item.Properties p) {
            super(b, p.useBlockDescriptionPrefix());
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
            t.accept(Component.translatable("block.guhs." + Vitrine.ID + ".tip").withStyle(ChatFormatting.GRAY));
            t.accept(Component.translatable("block.guhs." + Vitrine.ID + ".tip2").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
