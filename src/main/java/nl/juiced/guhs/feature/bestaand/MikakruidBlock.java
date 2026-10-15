package nl.juiced.guhs.feature.bestaand;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mikakruid: the thorny weed the Mikas sow in the pindasaus-tuintje of the Spiesburcht. It never really stands in the
 * world: it is a block state that {@link Schijn} shows to the one player who still has to pull it ({@link Tuintje}), on a
 * spot that is air for everybody else. A click on it (either button) pulls it, for that player. It has no item and no
 * drops; one that an operator places for real is just a plant you walk through.
 */
public class MikakruidBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

    public MikakruidBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
