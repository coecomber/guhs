package nl.juiced.guhs.feature.bio.blokkenwolk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * What a wolkenstroom block shows (called from WolkenstroomBlock.animateTick, client side): a column you can read from a
 * distance. Puffs that travel a few blocks, one in three of them a little arrow (the client makes that choice), white and
 * fast in a lift (up), pink and slow in a stream down; the arrow points the way.
 * <p>
 * The game calls animateTick for a fixed number of random blocks around the player every tick (dense within 16 blocks, thin
 * up to 32), whatever is loaded: more columns never cost more calls, only a bigger share of them. One call makes two or
 * three particles here (it was one in three calls).
 */
public final class WolkenstroomPluis {
    /** Blocks per tick of the puffs: a lift rushes up, a stream down drifts down. */
    public static final double OMHOOG = 0.26, OMLAAG = -0.11;

    public static void animeer(boolean omlaag, Level level, BlockPos pos, RandomSource random) {
        double v = omlaag ? OMLAAG - random.nextDouble() * 0.04 : OMHOOG + random.nextDouble() * 0.1;
        int pluis = 2 + random.nextInt(2);
        for (int i = 0; i < pluis; i++) {
            level.addParticle(BlokkenWolkSlice.WOLKENSTROOM_PLUIS.get(), pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, v, 0);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, omlaag ? -0.05 : 0.08, 0);
        }
    }

    private WolkenstroomPluis() {
    }
}
