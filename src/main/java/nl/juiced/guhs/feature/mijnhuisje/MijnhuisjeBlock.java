package nl.juiced.guhs.feature.mijnhuisje;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0 (ronde 37-40): the Mijn-guhhuisje, a little guh head with a miner's helmet (see {@link MijnhuisjeBlockEntity}). A click
 * opens its menu (the owner; others hear whose it is); a click with a picked-up guh lets it move in (it comes out of the
 * door, at most four); only the owner (or an op) breaks it.
 */
public class MijnhuisjeBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<MijnhuisjeBlock> CODEC = simpleCodec(MijnhuisjeBlock::new);
    private static final VoxelShape VORM = Shapes.or(Block.box(2, 0, 2, 14, 10, 14), Block.box(2.5, 10, 2.5, 13.5, 14, 13.5));

    public MijnhuisjeBlock(Properties properties) {
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
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MijnhuisjeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != MijnhuisjeFeature.BE.get() ? null
                : (l, p, s, be) -> MijnhuisjeBlockEntity.serverTick(l, p, s, (MijnhuisjeBlockEntity) be);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player p && level.getBlockEntity(pos) instanceof MijnhuisjeBlockEntity be) {
            be.zetEigenaar(p);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof MijnhuisjeBlockEntity be)) {
            return InteractionResult.SUCCESS;
        }
        if (be.eigenaar() == null) {
            be.zetEigenaar(sp);
        }
        if (!be.mag(sp)) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.mijnhuisje.van", be.eigenaarNaam()).withStyle(ChatFormatting.GRAY));
            return InteractionResult.CONSUME;
        }
        open(sp, be);
        return InteractionResult.CONSUME;
    }

    /** Opens the menu (with the names of the residents in the open buffer). */
    static void open(ServerPlayer sp, MijnhuisjeBlockEntity be) {
        if (!be.isRemoved()) {
            sp.openMenu(be, buf -> MijnhuisjeMenu.schrijf(buf, be));
        }
    }

    /** With a picked-up guh in your hand: it moves in (out of the door). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!stack.is(ModItems.PICKED_UP_GUH.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof MijnhuisjeBlockEntity be)) {
            return InteractionResult.SUCCESS;
        }
        if (be.eigenaar() == null) {
            be.zetEigenaar(sp);
        }
        if (!be.mag(sp)) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.mijnhuisje.van", be.eigenaarNaam()).withStyle(ChatFormatting.GRAY));
            return InteractionResult.CONSUME;
        }
        if (be.isVol()) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.mijnhuisje.vol", MijnhuisjeBlockEntity.MAX_BEWONERS).withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.CONSUME;
        }
        Direction voor = state.getValue(FACING);
        BlockPos d = pos.relative(voor);
        Entity e = PickedUpGuhItem.release(sl, PickedUpGuhItem.guhData(stack), d.getX() + 0.5, d.getY(), d.getZ() + 0.5, voor.toYRot());
        if (e == null) {
            return InteractionResult.CONSUME;
        }
        stack.consume(1, sp);
        if (be.trekIn(e)) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.mijnhuisje.trekt_in", e.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.mijnhuisje.niet_jouw_guh").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    /** Only the owner (or an op) even starts breaking it. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MijnhuisjeBlockEntity be && !be.mag(player)) {
            return 0f;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
