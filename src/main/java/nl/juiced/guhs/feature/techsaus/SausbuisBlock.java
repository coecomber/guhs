package nl.juiced.guhs.feature.techsaus;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;

/**
 * De dikke Sausbuis ({@code guhs:sausbuis}, 1.8.0): a {@link SausslangBlock} for long trunk lines. It joins and works exactly
 * like a hose (pumps push through it, machines slurp through it, {@link Slangen}), but it counts for only 1/16 of a hose
 * against the hose limit, so one net can run {@link SausGetallen#SLANG_MAX} * 16 pieces of pipe far across a base.
 */
public class SausbuisBlock extends SausslangBlock {
    public static final MapCodec<SausbuisBlock> CODEC = simpleCodec(SausbuisBlock::new);

    public SausbuisBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
