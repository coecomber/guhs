package nl.juiced.guhs.feature.techmachine;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * De Knutselmachine: a workbench with a guh face and two little hammers on top. It crafts what the Bouwtekening in it
 * shows, from what pipes, guhs and you put in ({@link KnutselmachineBlockEntity}).
 */
public class KnutselmachineBlock extends TechBlock {
    public static final MapCodec<KnutselmachineBlock> CODEC = simpleCodec(KnutselmachineBlock::new);

    public KnutselmachineBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KnutselmachineBlockEntity(pos, state);
    }
}
