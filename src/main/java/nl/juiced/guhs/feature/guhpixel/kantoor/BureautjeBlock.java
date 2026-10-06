package nl.juiced.guhs.feature.guhpixel.kantoor;

import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Bureautje ({@code guhs:guhkantoor_bureautje}): a little desk with an old beige computer and a stool. It belongs to
 * the nearest Prikklok within {@link Kantoor#BEREIK} blocks (at most four per clock); one guh can "work" here, which means
 * it sleeps on the keyboard ({@code bezet}: the monitor fills with the same letter). Right-click opens the Prikklok's screen.
 */
public class BureautjeBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<BureautjeBlock> CODEC = simpleCodec(BureautjeBlock::new);
    public static final BooleanProperty BEZET = BooleanProperty.create("bezet");
    /** Facing north: the stool on the north side, the desk behind it, the monitor at the back. */
    private static final VoxelShape NOORD = Shapes.or(Block.box(0, 0, 5, 16, 8, 16), Block.box(4, 8, 10, 12, 15, 16), Block.box(5, 0, 0, 11, 5, 4));
    private static final Map<Direction, VoxelShape> VORMEN = Shapes.rotateHorizontal(NOORD);

    public BureautjeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BEZET, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BEZET);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORMEN.get(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BureautjeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != KantoorSlice.BUREAUTJE_BE.get()) {
            return null;
        }
        BlockEntityTicker<BureautjeBlockEntity> ticker = level.isClientSide() ? BureautjeBlockEntity::clientTick : BureautjeBlockEntity::serverTick;
        return (BlockEntityTicker<T>) ticker;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel server) {
            Kantoor.koppel(server, pos, placer instanceof ServerPlayer sp ? sp : null);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel server) {
            Kantoor.klikBureau(sp, server, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
