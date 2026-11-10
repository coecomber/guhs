package nl.juiced.guhs.feature.vadsrijk.ftb;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Y-FTB: a reward of the FTB chapter "Het Vadsrijk": a little decoration piece for your own world (tools/features/vadsrijk_y_ftb.py
 * has the models: vanilla textures, no new pictures). Pure deco: it faces the one who puts it down, some give light, nothing to
 * click. It cannot be put down in the Vadsrijk itself (the Vadsrijk keeps its own bag anyway; the Knuffelkwartier, with your home
 * inventory, is fine).
 */
public class TrofeeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TrofeeBlock> CODEC = simpleCodec(p -> new TrofeeBlock(p, Block.box(2, 0, 2, 14, 16, 14)));
    private final VoxelShape vorm;

    public TrofeeBlock(Properties properties, VoxelShape vorm) {
        super(properties);
        this.vorm = vorm;
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vorm;
    }

    /** Not in the Vadsrijk (null = it is not placed; the server says why). */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (magNietHier(context)) {
            if (context.getPlayer() instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("vadsrijk.ftb.plaats.vadsrijk").withStyle(ChatFormatting.GOLD));
            }
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public static boolean magNietHier(BlockPlaceContext context) {
        return magNietIn(context.getLevel().dimension());
    }

    /** Not in the Vadsrijk; everywhere else (the Knuffelkwartier too) it may stand. */
    public static boolean magNietIn(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim) {
        return dim == Vadsrijk.DIM;
    }
}
