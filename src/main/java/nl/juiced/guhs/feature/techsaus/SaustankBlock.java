package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * De Grote saustank ({@code guhs:saustank}, 1.8.0): a {@link SausvatBlock} that holds 64 buckets ({@link SaustankBlockEntity}).
 * It shows its level and sauce like the vat ({@link SausvatBlock#SAUS}, {@link SausvatBlock#NIVEAU}).
 */
public class SaustankBlock extends SausvatBlock {
    public static final MapCodec<SaustankBlock> CODEC = simpleCodec(SaustankBlock::new);

    public SaustankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SaustankBlockEntity(pos, state);
    }
}
