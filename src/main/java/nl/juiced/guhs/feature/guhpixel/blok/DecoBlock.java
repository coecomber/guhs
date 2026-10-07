package nl.juiced.guhs.feature.guhpixel.blok;

import java.util.Map;

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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small decoration block that stands on the floor and faces the player who places it (keepsakes, souvenirs, shop
 * decoration). The shape is given for facing north and turned with the block. Register with
 * {@code BLOCKS.registerBlock("n_x", p -> new DecoBlock(p, SHAPE), () -> DecoBlock.props())}; the model, blockstate, item
 * and loot table come from tools/features/guhpixel_lib.py {@code deco(...)}.
 */
public class DecoBlock extends HorizontalDirectionalBlock {
    private final VoxelShape noord;
    private final Map<Direction, VoxelShape> vormen;
    private final MapCodec<DecoBlock> codec;

    public DecoBlock(Properties properties, VoxelShape noord) {
        super(properties);
        this.noord = noord;
        this.vormen = Shapes.rotateHorizontal(noord);
        this.codec = simpleCodec(p -> new DecoBlock(p, noord));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The usual properties of a decoration: breaks at once, drops itself, no full cube. */
    public static Properties props() {
        return Properties.of().strength(0.4f).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY);
    }

    public VoxelShape noord() {
        return noord;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
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
        return vormen.get(state.getValue(FACING));
    }
}
