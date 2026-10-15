package nl.juiced.guhs.feature.bio.bouwwolk2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;

/**
 * Het onweerswolkje: a little dark cloud for your own base. It floats where you put it (it needs nothing under it) and
 * rains softly on what stands below, a flower pot for instance: drops on the client only, down to the first block within
 * {@link #BEREIK}. It never really strikes.
 */
public class OnweerswolkjeBlock extends DecoBlock {
    public static final int BEREIK = 3;

    public OnweerswolkjeBlock(Properties properties, VoxelShape noord) {
        super(properties, noord);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FALLING_WATER, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.45,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0, 0, 0);
        }
        if (random.nextInt(6) != 0) {
            return;
        }
        for (int d = 1; d <= BEREIK; d++) {
            BlockPos onder = pos.below(d);
            BlockState daar = level.getBlockState(onder);
            if (!daar.isAir()) {
                double top = daar.getShape(level, onder).max(net.minecraft.core.Direction.Axis.Y);
                level.addParticle(ParticleTypes.SPLASH, onder.getX() + 0.5, onder.getY() + Math.max(0.1, top) + 0.02, onder.getZ() + 0.5, 0, 0, 0);
                return;
            }
        }
    }
}
