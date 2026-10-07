package nl.juiced.guhs.feature.guhpixel.reisbureau;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;

/**
 * A souvenir that stands on the floor and does a little something: the badkuipje steams, the kampvuurtje burns, the
 * lantaarn's glimguhtjes float about, golden things twinkle now and then. Only particles and a soft sound; nothing burns
 * or hurts.
 */
public class SouvenirBlock extends DecoBlock {
    private final Souvenirs.Effect effect;

    public SouvenirBlock(Properties properties, VoxelShape noord, Souvenirs.Effect effect) {
        super(properties, noord);
        this.effect = effect;
    }

    public Souvenirs.Effect effect() {
        return effect;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        switch (effect) {
            case STOOM -> {
                if (random.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.WHITE_SMOKE, x + (random.nextDouble() - 0.5) * 0.5, y + 0.5, z + (random.nextDouble() - 0.5) * 0.3,
                            0, 0.02 + random.nextDouble() * 0.02, 0);
                }
            }
            case VUUR -> {
                if (random.nextInt(2) == 0) {
                    level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.15, y + 0.45, z + (random.nextDouble() - 0.5) * 0.15,
                            0, 0.004, 0);
                }
                if (random.nextInt(6) == 0) {
                    level.addParticle(ParticleTypes.SMOKE, x, y + 0.6, z, 0, 0.02, 0);
                }
                if (random.nextInt(30) == 0) {
                    level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.25f, 1.2f + random.nextFloat() * 0.4f, false);
                }
            }
            case GLIM -> {
                if (random.nextInt(8) == 0) {
                    level.addParticle(ParticleTypes.GLOW, x + (random.nextDouble() - 0.5) * 0.9, y + 0.3 + random.nextDouble() * 0.6,
                            z + (random.nextDouble() - 0.5) * 0.9, 0, 0.005, 0);
                }
            }
            case MUZIEK -> {
                if (random.nextInt(50) == 0) {
                    level.addParticle(ParticleTypes.NOTE, x, y + 0.95, z, random.nextInt(25) / 24.0, 0, 0);
                }
            }
            case GLINSTER -> {
                if (random.nextInt(24) == 0) {
                    level.addParticle(ParticleTypes.WAX_ON, x + (random.nextDouble() - 0.5) * 0.6, y + 0.2 + random.nextDouble() * 0.6,
                            z + (random.nextDouble() - 0.5) * 0.6, 0, 0, 0);
                }
            }
            default -> {
            }
        }
    }
}
