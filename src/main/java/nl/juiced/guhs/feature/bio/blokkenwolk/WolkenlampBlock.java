package nl.juiced.guhs.feature.bio.blokkenwolk;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The wolkenlamp: a little cloud with a warm glow inside. It floats, so it needs nothing to stand on or hang from: put it on
 * the floor, on a table or in mid-air. Soft light ({@link #LICHT}, a lantern gives 15). Nothing ticks.
 */
public class WolkenlampBlock extends Block {
    public static final MapCodec<WolkenlampBlock> CODEC = simpleCodec(WolkenlampBlock::new);
    public static final int LICHT = 12;
    private static final VoxelShape VORM = Block.box(2, 3, 2, 14, 13, 14);

    public WolkenlampBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
