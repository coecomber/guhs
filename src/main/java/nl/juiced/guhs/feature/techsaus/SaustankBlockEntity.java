package nl.juiced.guhs.feature.techsaus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.8.0 (vadskracht): the Grote saustank: a {@link SausvatBlockEntity} of {@link SausGetallen#TANK} mB (64 buckets) instead of
 * 16. Everything else is the vat's: hoses and pipes in and out on every side, buckets, the meter on its front, the comparator,
 * and the sauce stays in the item when you break it.
 */
public class SaustankBlockEntity extends SausvatBlockEntity {
    public SaustankBlockEntity(BlockPos pos, BlockState state) {
        super(TechsausFeature.SAUSTANK_BE.get(), pos, state, SausGetallen.TANK);
    }
}
