package nl.juiced.guhs.feature.safari.quests;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A pootafdruk: a paper-thin layer of paw prints on the ground, on the trails from the paths to the homes of the hiders
 * (placed by the park generator, tools/features/safari_park/zone_zz_s4.py). You walk through it; seen through a spyglass
 * or the verrekijker the prints light up ({@code client.SafariQuestsClient}).
 */
public class PootafdrukBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PootafdrukBlock> CODEC = simpleCodec(PootafdrukBlock::new);
    private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 0.25, 16);

    public PootafdrukBlock(Properties properties) {
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
