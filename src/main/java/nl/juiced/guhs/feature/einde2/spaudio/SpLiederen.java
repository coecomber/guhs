package nl.juiced.guhs.feature.einde2.spaudio;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 6.2), slice sp_audio: the three own songs, their sung lines and the ticks those lines
 * start at, counted from the first tick of the recording. tools/features/einde2_sp_audio_liederen.py holds the same numbers
 * (with the Dutch words); the chain module einde2_sp_audio.py fails when the two differ, and einde2_sp_audio_geluid.py builds
 * the recordings on exactly these beats.
 * <ul>
 *   <li>{@code zoete_zege} "Zoete Knabbelzege": a power ballad that becomes a stadium anthem, 100 BPM (12 ticks a beat),
 *       26 bars, 62.4 s;</li>
 *   <li>{@code guhfy_guhber} "Ik ben een Guhfy Guhber": the rock song, 150 BPM (8 ticks a beat), 34 bars, 54.4 s;</li>
 *   <li>{@code monsters} "Het lied van de loopgraaf": a hummed start, then a sweet waltz in low voices (the scene has the
 *       monsters roar before it), 3/4 at 120 BPM (10 ticks a beat), 20 bars, 30 s.</li>
 * </ul>
 * The captions are {@code scene.guhs.<scene>.lied.<key>} ({@link SpAudio#lied} puts them into the scene).
 */
public final class SpLiederen {
    /** One sung line: lang key part, first tick (from the start of the song), how long it stays on screen. */
    public record Regel(String key, int tick, int ticks) {
    }

    /** A song: the scene it belongs to, its length in ticks, its lines in order. */
    public record Lied(String naam, String scene, int duur, List<Regel> regels) {
    }

    public static final Lied ZOETE_ZEGE = new Lied("zoete_zege", "einde2_sp_d1_recept_zoete_zege", 1248, List.of(
            new Regel("intro", 0, 96),
            new Regel("1", 96, 96), new Regel("2", 192, 96), new Regel("3", 288, 96), new Regel("4", 384, 96),
            new Regel("5", 480, 96),
            new Regel("6", 576, 96), new Regel("7", 672, 96), new Regel("8", 768, 96), new Regel("9", 864, 96),
            new Regel("10", 960, 96), new Regel("11", 1056, 192)));

    public static final Lied GUHFY_GUHBER = new Lied("guhfy_guhber", "einde2_sp_d2_finale_guhfy_guhber", 1088, List.of(
            new Regel("intro", 0, 128),
            new Regel("1", 128, 64), new Regel("2", 192, 64), new Regel("3", 256, 64), new Regel("4", 320, 64),
            new Regel("5", 384, 64), new Regel("6", 448, 64), new Regel("7", 512, 64), new Regel("8", 576, 64),
            new Regel("9", 640, 64), new Regel("10", 704, 64),
            new Regel("11", 768, 64), new Regel("12", 832, 64), new Regel("13", 896, 64), new Regel("14", 960, 128)));

    public static final Lied MONSTERS = new Lied("monsters", "einde2_sp_d2_weg_monsters", 600, List.of(
            new Regel("intro", 0, 60),
            new Regel("1", 60, 120), new Regel("2", 180, 120), new Regel("3", 300, 120), new Regel("4", 420, 120),
            new Regel("slot", 540, 60)));

    public static final Map<String, Lied> ALLE = Map.of(ZOETE_ZEGE.naam(), ZOETE_ZEGE, GUHFY_GUHBER.naam(), GUHFY_GUHBER,
            MONSTERS.naam(), MONSTERS);

    /** Where a song starts in its scene: (scene id -> song and tick), filled by {@link SpAudio#lied} (both sides register scenes). */
    public record Start(String lied, int vanaf) {
    }

    static final Map<String, Start> IN_SCENE = new ConcurrentHashMap<>();

    private SpLiederen() {
    }

    @Nullable
    public static Lied van(String naam) {
        return ALLE.get(naam);
    }

    /** The song that starts in this scene, and at which tick (null: none, or the scene was not registered with {@link SpAudio#lied}). */
    @Nullable
    public static Start inScene(@Nullable String sceneId) {
        return sceneId == null ? null : IN_SCENE.get(sceneId);
    }
}
