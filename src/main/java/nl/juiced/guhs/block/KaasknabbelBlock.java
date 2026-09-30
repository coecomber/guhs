package nl.juiced.guhs.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.world.GuhPortalShape;

/**
 * Block of Kaasknabbels. Placing one that completes a portal-shaped frame immediately opens a portal to the Guhmension.
 */
public class KaasknabbelBlock extends Block {
    public static final MapCodec<KaasknabbelBlock> CODEC = simpleCodec(KaasknabbelBlock::new);

    public KaasknabbelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide() || oldState.is(state.getBlock())) {
            return;
        }
        // any empty neighbour could be the inside of a freshly completed frame
        for (Direction dir : Direction.values()) {
            BlockPos inside = pos.relative(dir);
            if (level.getBlockState(inside).isAir()) {
                var shape = GuhPortalShape.findEmptyShape(level, inside);
                if (shape.isPresent()) {
                    shape.get().createPortalBlocks();
                    return;
                }
            }
        }
    }
}
