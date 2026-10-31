package nl.juiced.guhs.feature.snackbar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One tile of a wide sign of the Guhsnackbar: a thin glowing panel against the back of its block. {@link #TEGEL} says which
 * part of which sign it shows (see tools/features/snackbar.py, BORDEN); it only comes with the template, there is no item.
 */
public class SnackbarBordBlock extends HorizontalDirectionalBlock {
    /** Every tile of every sign: the menu 11x4, the name 17x4, GUHSNACKBAR 10x2. */
    public static final int TEGELS = 132;
    public static final IntegerProperty TEGEL = IntegerProperty.create("tegel", 0, TEGELS - 1);
    public static final MapCodec<SnackbarBordBlock> CODEC = simpleCodec(SnackbarBordBlock::new);
    private static final VoxelShape NOORD = Block.box(0, 0, 0, 16, 16, 1), ZUID = Block.box(0, 0, 15, 16, 16, 16),
            WEST = Block.box(0, 0, 0, 1, 16, 16), OOST = Block.box(15, 0, 0, 16, 16, 16);

    public SnackbarBordBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(TEGEL, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TEGEL);
    }

    /** FACING is where the text looks: the panel hangs against the opposite side. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> ZUID;
            case WEST -> OOST;
            case EAST -> WEST;
            default -> NOORD;
        };
    }
}
