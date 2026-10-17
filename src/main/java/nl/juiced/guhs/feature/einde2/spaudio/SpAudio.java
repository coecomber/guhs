package nl.juiced.guhs.feature.einde2.spaudio;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 3, 6, 7.9), slice sp_audio: the sound API of the Sponsguh story (the skelet wrote this file
 * with stand-in bodies; sp_audio fills in the bodies and keeps names and signatures). Every name of {@link #NAMEN} becomes the
 * sound event {@code guhs:einde2_sp_audio.<naam>} (subtitle {@code subtitles.guhs.einde2_sp_audio.<naam>}).
 * <ul>
 *   <li>{@link #moment}: the music under a big scene (call it right before {@code Cutscenes.speel}, {@link #stopMoment} in the
 *       scene's {@code daarna});</li>
 *   <li>{@link #stinger}: a short sound (a tune of a minigame, a joke sting, the narrator's card);</li>
 *   <li>{@link #lied}: the SUNG captions of one of the three songs ({@link #LIEDEREN}), put into a scene as run-on captions
 *       ({@code Cutscene.Builder.lied}) on the song's own timing; returns the song's length in ticks. The lyrics are
 *       sp_audio's (keys {@code scene.guhs.<scene id>.lied.<n>}, its chunk); the scene owner only calls this;</li>
 *   <li>{@link #BPM}: the beat of the rhythm games, from tick 0 of the tune (fixed here, so the games build before the music).</li>
 * </ul>
 * Stand-ins: a vanilla sound for every name, moments and stingers do nothing, {@link #lied} puts the four draft lines of the
 * song 60 ticks apart and returns 240.
 */
public final class SpAudio {
    /** Background music (music category, 2-4 min, silence between: the areas bikinibodem, schelpenstad, loopgraaf). */
    public static final List<String> MUZIEK = List.of("muziek.bikinibodem_1", "muziek.bikinibodem_2", "muziek.schelpenstad", "muziek.loopgraaf");
    /** The big moments (moment ids = the part after "moment."). */
    public static final List<String> MOMENTEN = List.of("moment.zoete_zege", "moment.guhfy_guhber", "moment.monsters", "moment.plan_z",
            "moment.bevroren", "moment.cycloop", "moment.hasselguh", "moment.dennis");
    /** Tunes of the minigames and places. */
    public static final List<String> DEUNTJES = List.of("deuntje.bakken", "deuntje.kwallen", "deuntje.examen", "deuntje.sluipen",
            "deuntje.trommel", "deuntje.kroeg", "deuntje.ijssalon", "deuntje.emmers");
    /** Short stings. */
    public static final List<String> STINGERS = List.of("stinger.verteller", "stinger.klarinet", "stinger.karen", "stinger.bevries",
            "stinger.snor", "stinger.opgelost", "stinger.mislukt", "stinger.lach");
    public static final List<String> NAMEN = java.util.stream.Stream.of(MUZIEK, MOMENTEN, DEUNTJES, STINGERS).flatMap(List::stream).toList();
    /** The three songs: the scene each plays in. */
    public static final Map<String, String> LIEDEREN = Map.of("zoete_zege", "einde2_sp_d1_recept_zoete_zege",
            "guhfy_guhber", "einde2_sp_d2_finale_guhfy_guhber", "monsters", "einde2_sp_d2_weg_monsters");
    /** The beat of the rhythm games (beats per minute from tick 0 of the tune). */
    public static final Map<String, Integer> BPM = Map.of("deuntje.trommel", 112, "deuntje.kroeg", 96);

    private SpAudio() {
    }

    /** The sound event of a name of {@link #NAMEN} (stand-in: a vanilla note). */
    public static Supplier<SoundEvent> geluid(String naam) {
        return () -> SoundEvents.NOTE_BLOCK_CHIME.value();
    }

    /** Starts the music of a big moment for this player ({@code id} without "moment."). */
    public static void moment(ServerPlayer p, String id) {
    }

    /** Stops the moment's music with a fade. */
    public static void stopMoment(ServerPlayer p, int fadeTicks) {
    }

    /** A short sound for this player ({@code naam} of {@link #DEUNTJES} / {@link #STINGERS}). */
    public static void stinger(ServerPlayer p, String naam) {
    }

    /** A short sound at a spot, for the players within 32 blocks. */
    public static void stinger(ServerLevel level, Vec3 plek, String naam) {
    }

    /**
     * Puts the sung captions of a song ({@link #LIEDEREN} key) into the builder of its scene from tick {@code vanaf} on;
     * returns how long the song runs (ticks). The scene id of the builder must be the song's scene.
     */
    public static int lied(Cutscene.Builder b, String lied, int vanaf) {
        for (int i = 1; i <= 4; i++) {
            b.lied(vanaf + (i - 1) * 60, "lied." + i, 60);
        }
        return 240;
    }
}
