package nl.juiced.guhs.feature.tuintjes;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.VoxelShape;

/** De guh_moestuinbak: a wooden raised bed full of soil, a guh face painted on its front; a whole row of plants in it. */
public class GuhMoestuinbakBlock extends TuinBlock {
    public static final MapCodec<GuhMoestuinbakBlock> CODEC = simpleCodec(GuhMoestuinbakBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 10, 16);

    public GuhMoestuinbakBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape vorm() {
        return SHAPE;
    }
}
