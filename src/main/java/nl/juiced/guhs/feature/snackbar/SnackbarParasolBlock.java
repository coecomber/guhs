package nl.juiced.guhs.feature.snackbar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A little striped parasol for on a table of the terrace: pink, or yellow ({@link #GEEL}). The canopy hangs over the neighbours. */
public class SnackbarParasolBlock extends Block {
    public static final BooleanProperty GEEL = BooleanProperty.create("geel");
    private static final VoxelShape STOK = Block.box(7, 0, 7, 9, 16, 9);

    public SnackbarParasolBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GEEL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GEEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return STOK;
    }
}
