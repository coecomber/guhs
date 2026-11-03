package nl.juiced.guhs.feature.vadsrijk.economie;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Vadsrijk trophy/deco block (cosmetic, bought with Vadsmunten and delivered home): mini Koning Klodder, the Opperzwam-beeldje, the
 * Klodderreus-knuffel and the Vadsrijk-vaandel. Purely decorative: it faces the player who places it; its looks are a block model
 * from tools/features/vadsrijk_economie.py.
 */
public class DecoBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DecoBlock> CODEC = simpleCodec(p -> new DecoBlock(p, Block.box(2, 0, 2, 14, 14, 14)));
    private final VoxelShape vorm;

    public DecoBlock(Properties p, VoxelShape vorm) {
        super(p);
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

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vorm;
    }
}
