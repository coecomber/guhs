package nl.juiced.guhs.feature.einde2.audio;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * einde2 phase B (CONTRACT_E2_B 3, 6), owner: slice audio. The music and the sounds of the Guhvatar story: every name of
 * 6.1 as {@code guhs:einde2_audio.<naam>}, the epic moments under a cutscene, the stingers. Skelet stand-in: a vanilla
 * sound for every name, and the moments and stingers do nothing. {@link #BPM} is final already: the rhythm games beat on
 * it from tick 0 of the sound.
 */
public final class E2Audio {
    /** The beats per minute of the tunes the rhythm games follow (names without the {@code einde2_audio.} prefix). */
    public static final Map<String, Integer> BPM = Map.of("deuntje.dansfeest", 100, "deuntje.waaierdans", 90, "deuntje.ritmeduel", 80);

    private E2Audio() {
    }

    /** The sound of a name of 6.1 (without the {@code einde2_audio.} prefix). */
    public static Supplier<SoundEvent> geluid(String naam) {
        return () -> SoundEvents.NOTE_BLOCK_CHIME.value();
    }

    /** An epic moment for this player (the part after {@code moment.}): replaces the background music, fades in, loops. */
    public static void moment(ServerPlayer p, String id) {
    }

    /** Ends this player's moment, fading out over this many ticks. */
    public static void stopMoment(ServerPlayer p, int fadeTicks) {
    }

    /** A stinger (a name of 6.1) for this player only. */
    public static void stinger(ServerPlayer p, String naam) {
    }

    /** A stinger at a spot: every player within 32 blocks hears it. */
    public static void stinger(ServerLevel level, Vec3 plek, String naam) {
    }
}
