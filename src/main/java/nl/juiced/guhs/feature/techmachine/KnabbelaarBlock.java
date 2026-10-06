package nl.juiced.guhs.feature.techmachine;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * De Knabbelaar: a guh head with two big teeth that gnaws away the block in front of its snoet
 * ({@link KnabbelaarBlockEntity}). The teeth really go up and down ({@code client.MachineRenderer}).
 */
public class KnabbelaarBlock extends TechBlock {
    public static final MapCodec<KnabbelaarBlock> CODEC = simpleCodec(KnabbelaarBlock::new);

    public KnabbelaarBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KnabbelaarBlockEntity(pos, state);
    }
}
