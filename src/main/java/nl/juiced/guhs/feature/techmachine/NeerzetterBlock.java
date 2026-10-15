package nl.juiced.guhs.feature.techmachine;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * De Neerzetter: a blue guh machine with a hatch for a mouth; a little arm comes out of it and puts the blocks from its
 * tummy down in front of its snoet ({@link NeerzetterBlockEntity}).
 */
public class NeerzetterBlock extends TechBlock {
    public static final MapCodec<NeerzetterBlock> CODEC = simpleCodec(NeerzetterBlock::new);

    public NeerzetterBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NeerzetterBlockEntity(pos, state);
    }
}
