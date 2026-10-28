package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * De Knuffelsteen-generator ({@code guhs:knuffelsteen_generator}, 1.8.0): one block. Kaassaus and water (through Sausslangen,
 * a bucket, or a water source block right next to it) become knuffelsteen on vadskracht, like a cobblestone generator but
 * softer ({@link KnuffelsteenGeneratorBlockEntity}). Simple begane-grond tech of the Guhtechniek-beurs.
 */
public class KnuffelsteenGeneratorBlock extends SausMachineBlock {
    public static final MapCodec<KnuffelsteenGeneratorBlock> CODEC = simpleCodec(KnuffelsteenGeneratorBlock::new);

    public KnuffelsteenGeneratorBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KnuffelsteenGeneratorBlockEntity(pos, state);
    }
}
