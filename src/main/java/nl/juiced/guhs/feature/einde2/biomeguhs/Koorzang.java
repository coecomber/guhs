package nl.juiced.guhs.feature.einde2.biomeguhs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;

/**
 * einde2, slice biomeguhs: a tamed Koorguh sings along with a jukebox ({@link E2Vlaggen#TRUC}: the client throws its head
 * back). While a jukebox plays within {@link #BEREIK} blocks it sings a note every second or so, with music notes above
 * its head. (Every guh already dances at a jukebox, see GuhEmotes: the Koorguh does both.)
 * <p>
 * hook fase C: the Disco-dynamo counts a Koorguh double, {@link Biomeguhs#discoFactor}.
 */
public final class Koorzang {
    /** A playing jukebox this close makes it sing (the same range as the jukebox dance). */
    public static final int BEREIK = GuhEmotes.JUKEBOX_RANGE;
    /** It listens every this many ticks. */
    public static final int STAP = 20;

    public static boolean zingt(GuhEntity guh) {
        return guh.getVariant() == GuhVariant.KOORGUH && Trucs.doet(guh);
    }

    /** (GuhHooks.tick, server) */
    static void serverTick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % STAP != 0) {
            return;
        }
        boolean muziek = Trucs.tam(guh) && !guh.isPassenger() && !GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT)
                && GuhEmotes.playingJukebox(guh.level(), guh.blockPosition(), BEREIK) != null;
        if (muziek != Trucs.doet(guh)) {
            Trucs.zet(guh, muziek);
            ServerPlayer baas = muziek ? Trucs.baasBij(guh, 24) : null;
            if (baas != null) {
                GidsFeature.grant(baas, "guheinde/einde2_biomeguhs_koor");
                Trucs.meld(baas, guh, "zingt");
            }
        }
        if (muziek && guh.level() instanceof ServerLevel level) {
            Trucs.geluid(guh, E2BiomeguhsFeature.KOORGUH_ZING.get(), 0.8f, guh.getVoicePitch());
            // (count 0: the "speed" is the note's colour)
            level.sendParticles(ParticleTypes.NOTE, guh.getX(), guh.getY() + guh.getBbHeight() + 0.5, guh.getZ(), 0, guh.getRandom().nextInt(24) / 24.0, 0, 0, 1);
        }
    }

    private Koorzang() {
    }
}
