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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Prikklok ({@code guhs:guhkantoor_prikklok}): the heart of a Guhkantoor. Up to four Bureautjes within
 * {@link Kantoor#BEREIK} blocks connect to it; a right-click opens the screen where you put guhs to work (asleep), send
 * them home and collect their loonstrookjes. Breaking it sends every guh of its desks home.
 */
public class PrikklokBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<PrikklokBlock> CODEC = simpleCodec(PrikklokBlock::new);
    private static final VoxelShape NOORD = Shapes.or(Block.box(4, 0, 4, 12, 1, 12), Block.box(7, 1, 7, 9, 8, 9), Block.box(3, 8, 5, 13, 16, 11));
    private static final Map<Direction, VoxelShape> VORMEN = Shapes.rotateHorizontal(NOORD);

    public PrikklokBlock(Properties properties) {
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORMEN.get(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrikklokBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel server) {
            Kantoor.klokGeplaatst(server, pos, placer instanceof ServerPlayer sp ? sp : null);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel server) {
            Kantoor.open(sp, server, pos, null);
        }
        return InteractionResult.SUCCESS;
    }
}
