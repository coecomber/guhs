package nl.juiced.guhs.feature.creche;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * The four slaapliedjes Juf Knuffel teaches (the Knus collection "slaapliedjes"). Each is a little tune: note block
 * pitches (0..24, 12 = the middle F#) and the ticks until the next note. The lullaby screen scrolls them past; you tap
 * along (space or click) and every hit plays its note. {@link #NODIG} percent right and the baby is asleep.
 */
public enum Slaapliedje {
    /** "Slaap, guhtje, slaap": slow and swaying. */
    STERRETJES(new int[]{6, 6, 13, 13, 15, 15, 13, 11, 11, 10, 10, 8, 8, 6}, new int[]{12, 12, 12, 12, 12, 12, 20, 12, 12, 12, 12, 12, 12, 20}),
    /** "Daar is het maantje": a soft rocking waltz. */
    MAANTJE(new int[]{11, 8, 8, 11, 8, 8, 13, 11, 10, 8, 6}, new int[]{16, 8, 16, 16, 8, 16, 16, 8, 16, 16, 24}),
    /** "Knabbeltje, knabbeltje": a little skipping tune about a kaasknabbel under your pillow. */
    KNABBELTJE(new int[]{6, 10, 13, 10, 6, 10, 13, 15, 13, 10, 8, 6}, new int[]{9, 9, 9, 9, 9, 9, 9, 9, 18, 9, 9, 18}),
    /** "Wolkje, wolkje": slowly down, like floating off to sleep. */
    WOLKJE(new int[]{18, 16, 15, 13, 11, 13, 15, 11, 10, 8, 6}, new int[]{14, 14, 14, 20, 14, 14, 14, 20, 14, 14, 28});

    /** Percent of the notes you must tap in time. */
    public static final int NODIG = 70;
    /** Ticks before the first note. */
    public static final int VOORAF = 40;

    private final int[] noten;
    private final int[] tussen;

    Slaapliedje(int[] noten, int[] tussen) {
        if (noten.length != tussen.length) {
            throw new IllegalStateException("slaapliedje " + name());
        }
        this.noten = noten;
        this.tussen = tussen;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int aantal() {
        return noten.length;
    }

    /** The pitch of note i (0..24). */
    public int noot(int i) {
        return noten[i];
    }

    /** The tick (from the start of the song) at which note i must be tapped. */
    public int tick(int i) {
        int t = VOORAF;
        for (int k = 0; k < i; k++) {
            t += tussen[k];
        }
        return t;
    }

    /** The whole song, in ticks. */
    public int lengte() {
        return tick(noten.length - 1) + tussen[noten.length - 1];
    }

    /** Note block pitch as a sound pitch. */
    public static float pitch(int noot) {
        return (float) Math.pow(2.0, (noot - 12) / 12.0);
    }

    /** Enough taps in time for the baby to fall asleep? */
    public boolean gelukt(int raak) {
        return raak * 100 >= aantal() * NODIG;
    }

    @Nullable
    public static Slaapliedje byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : null;
    }
}
