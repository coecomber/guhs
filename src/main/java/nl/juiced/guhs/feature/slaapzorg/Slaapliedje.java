package nl.juiced.guhs.feature.slaapzorg;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.blub.BlubGeluid;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;

/**
 * A lullaby (the Blub-guhs' slaapliedje, one stream of about 24 seconds): every guh around yawns, one by one, and a moment
 * later curls up and falls asleep on the spot ({@link Slapen#slaapIn}: until the morning in the evening and at night, a
 * nap by day). The Muziekdoosje and the Mobiel play it.
 */
public final class Slaapliedje {
    /** How long the song lasts. */
    public static final int DUUR = BlubGuhEntity.LIEDJE_TICKS;

    private Slaapliedje() {
    }

    /** Starts the song at {@code pos} and sings the guhs within {@code bereik} asleep; returns how many guhs listen. */
    public static int zing(ServerLevel level, BlockPos pos, double bereik, float volume) {
        level.playSound(null, pos, BlubGeluid.LIEDJE.get(), SoundSource.BLOCKS, volume, 1f);
        int n = 0;
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, new AABB(pos).inflate(bereik), g -> g.isAlive() && !Slapen.slaapt(g))) {
            int wacht = 40 + guh.getRandom().nextInt(DUUR / 2);
            if (GuhEmotes.canStart(guh) && guh.emotes.current() == null) {
                guh.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);
                wacht = Math.max(wacht, Emote.GAPEN.onceTicks + 10);
            }
            Slapen.slaapStraksIn(guh, wacht);
            n++;
        }
        return n;
    }

    /** The notes of a playing song (every second or so while it plays). */
    public static void noten(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 1, 0.25, 0.1, 0.25, level.getRandom().nextDouble());
    }
}
