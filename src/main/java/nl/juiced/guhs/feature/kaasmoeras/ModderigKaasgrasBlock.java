package nl.juiced.guhs.feature.kaasmoeras;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Modderig kaasgras: the soggy, yellow-green grass of the kaasmoeras (always a little cheesy). Like grass it turns
 * back into kaasmodder under a solid block, and slowly creeps over kaasmodder next to it in the light.
 */
public class ModderigKaasgrasBlock extends Block {
    public static final MapCodec<ModderigKaasgrasBlock> CODEC = simpleCodec(ModderigKaasgrasBlock::new);

    public ModderigKaasgrasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState above = level.getBlockState(pos.above());
        if (above.isSolidRender(level, pos.above())) {
            level.setBlockAndUpdate(pos, KaasmoerasFeature.KAASMODDER.get().defaultBlockState());
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9) {
            BlockPos next = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (level.getBlockState(next).is(KaasmoerasFeature.KAASMODDER.get()) && !level.getBlockState(next.above()).isSolidRender(level, next.above())) {
                level.setBlockAndUpdate(next, defaultBlockState());
            }
        }
    }
}
