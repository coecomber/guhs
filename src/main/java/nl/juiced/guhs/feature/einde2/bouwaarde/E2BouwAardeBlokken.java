package nl.juiced.guhs.feature.einde2.bouwaarde;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The blocks of Ba Sing Guh that are not a plain cube (registered in {@link E2BouwAardeFeature}). */
public final class E2BouwAardeBlokken {
    /** guhs:einde2_bouw_aarde_kool: a round head of kool, a little smaller than a block. It does nothing ("MIJN KOLEN!"). */
    public static class Kool extends Block {
        private static final VoxelShape VORM = Block.box(2, 0, 2, 14, 11, 14);

        public Kool(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }
    }

    private E2BouwAardeBlokken() {
    }
}
