package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * einde2, slice sponsguh_wezens: the order table of De Krokante Knabbel (block {@code guhs:besteltafel},
 * {@link SponsguhBlokken.BesteltafelBlock}): a right-click rings its bell.
 * <p>
 * hook fase B: the story hears an order through {@link #BIJ_BEL}.
 */
public final class Besteltafel {
    /** Called when a player rings the bell of a besteltafel, with the player and the table's spot. */
    public static final List<BiConsumer<ServerPlayer, BlockPos>> BIJ_BEL = new CopyOnWriteArrayList<>();

    /** Rings the bell of the table at this spot for this player: the sound, a note, and everybody who listens. */
    public static void bel(ServerLevel level, BlockPos pos, ServerPlayer player) {
        level.playSound(null, pos, E2SponsguhFeature.BESTELBEL.get(), SoundSource.BLOCKS, 0.9f, 1.0f + level.getRandom().nextFloat() * 0.1f);
        level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        BIJ_BEL.forEach(h -> h.accept(player, pos.immutable()));
    }

    private Besteltafel() {
    }
}
