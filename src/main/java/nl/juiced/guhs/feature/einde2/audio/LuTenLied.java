package nl.juiced.guhs.feature.einde2.audio;

import java.util.List;

import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * einde2, slice audio: the words of Miroh's own lullaby under {@code moment.lu_ten} (CONTRACT_E2_B 6.3: an OWN slow
 * lullaby, own Dutch words about a young guh soldier coming home; nothing of the series). The tune is 3/4 at 72 BPM, one bar
 * = 50 ticks: a hummed start (bars 0-3), four sung lines (bars 4-11), a hummed middle (12-15), the four lines again with new
 * words (16-23), a hummed end (24-30), 81 s in all. The ticks below count from the first tick of the moment (the tune and
 * these captions start together when the scene calls {@link E2Audio#moment} right before it plays).
 * <p>
 * For h3_aarde's {@code lu_ten} scene (a helper, not part of the cross-slice API: copying the numbers works just as well):
 * {@code LuTenLied.zing(builder, 0)} adds every line as a {@code Builder.lied} caption that does not wait for a click.
 * The texts are {@code gui.guhs.einde2_audio.lied.lu_ten.<n>} (tools/features/einde2_audio.py).
 */
public final class LuTenLied {
    public record Regel(String key, int tick, int ticks) {
    }

    public static final int TICKS_PER_MAAT = 50;
    /** The whole tune, in ticks (81.5 s). */
    public static final int DUUR = 1630;
    private static final String K = "gui.guhs.einde2_audio.lied.lu_ten.";

    /** The captions in order: the hums and the eight sung lines (tools/features/einde2_audio_geluid.py REGELS = the same bars). */
    public static final List<Regel> REGELS = List.of(
            new Regel(K + "neuriet", 0, 4 * TICKS_PER_MAAT - 10),
            new Regel(K + "1", 4 * TICKS_PER_MAAT, 2 * TICKS_PER_MAAT),
            new Regel(K + "2", 6 * TICKS_PER_MAAT, 2 * TICKS_PER_MAAT),
            new Regel(K + "3", 8 * TICKS_PER_MAAT, 2 * TICKS_PER_MAAT),
            new Regel(K + "4", 10 * TICKS_PER_MAAT, 2 * TICKS_PER_MAAT),
            new Regel(K + "neuriet", 12 * TICKS_PER_MAAT, 4 * TICKS_PER_MAAT - 10),
            new Regel(K + "5", 16 * TICKS_PER_MAAT, 2 * TICKS_PER_MAAT),
            new Regel(K + "6", 18 * TICKS_PER_MAAT, 2 * TICKS_PER_MAAT),
            new Regel(K + "7", 20 * TICKS_PER_MAAT, 2 * TICKS_PER_MAAT),
            new Regel(K + "8", 22 * TICKS_PER_MAAT, 2 * TICKS_PER_MAAT),
            new Regel(K + "neuriet", 24 * TICKS_PER_MAAT, 6 * TICKS_PER_MAAT));

    private LuTenLied() {
    }

    /** Adds every line of the song to a scene as a caption that runs on, the first one at {@code start}. */
    public static Cutscene.Builder zing(Cutscene.Builder b, int start) {
        for (Regel r : REGELS) {
            b.lied(start + r.tick(), r.key(), r.ticks());
        }
        return b;
    }
}
