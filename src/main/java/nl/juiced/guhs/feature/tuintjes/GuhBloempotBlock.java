package nl.juiced.guhs.feature.tuintjes;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.VoxelShape;

/** De guh_bloempot: a small round pink pot with a little guh face; one plant in it. */
public class GuhBloempotBlock extends TuinBlock {
    public static final MapCodec<GuhBloempotBlock> CODEC = simpleCodec(GuhBloempotBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 8, 13);

    public GuhBloempotBlock(Properties properties) {
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
