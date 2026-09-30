package nl.juiced.guhs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.registry.ModSounds;
import org.joml.Vector3f;

/**
 * A glowing guh crystal (a little crystal guh head), growing on the walls of the crystal mines. Now and then, when
 * you're close, one gives a soft happy "guh".
 */
public class GuhKristalClusterBlock extends AmethystClusterBlock {
    private static final double SING_RANGE = 8;
    private static final DustParticleOptions SPARKLE = new DustParticleOptions(0xFF99D9 /* 1, 0.6, 0.85 */, 0.6f);

    public GuhKristalClusterBlock(Properties properties) {
        super(7f, 3f, properties);
    }

    /** Client side, for blocks around the player: sparkles, and now and then a song if the player is close. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(SPARKLE, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SING_RANGE, false);
        if (player != null && random.nextInt(400) == 0) {
            // a soft, happy little "guh" with a tiny chime: quiet, and not too often
            level.playLocalSound(pos, ModSounds.KRISTAL_SING.get(), SoundSource.BLOCKS, 0.18f, 1.25f + random.nextFloat() * 0.15f, false);
            level.playLocalSound(pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.12f, 1.4f + random.nextFloat() * 0.3f, false);
        }
    }
}
