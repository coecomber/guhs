package nl.juiced.guhs.feature.knabbelspelen;

import java.util.Locale;

import net.minecraft.network.chat.Component;

/**
 * The six events of De Knabbelspelen, each with its own play field around the circus tent (Speelvelden), its own
 * Highscores board ({@code spelen_<id>}) and its part of the Grote Zeskamp (0-1000 points: {@link #zeskamp}).
 * Point events: more is better; time events: faster is better, with a fixed "perfect" time that's worth 1000.
 */
public enum Onderdeel {
    /** Kaasknabbels swing on strings: bite as many as you can (golden ones count 3). */
    KNABBELHAPPEN(false, 20 * 45, 30),
    /** Hop in a guh sack over the bumpy course to the finish. */
    ZAKLOPEN(true, 20 * 60, 20 * 7),
    /** Throw pluisballen at the tins with Mika faces. */
    BLIKGOOIEN(false, 20 * 45, 120),
    /** A knabbelei on a spoon through the flags: too wild and it drops, back to the last flag. */
    EIERLOPEN(true, 20 * 90, 20 * 11),
    /** Lower the knabbelspijker on its string into three kaasmelk bottles. */
    SPIJKERPOEPEN(true, 20 * 120, 20 * 20),
    /** Blindfolded, spun three times: pin the tail on the guh. */
    GUHGUHTJE_PRIK(false, 20 * 30, 1000);

    /** Time event (ticks, lower is better)? */
    public final boolean tijd;
    /** The longest an event lasts (ticks). */
    public final int maxTicks;
    /** What's worth a full 1000 in the zeskamp (points, or ticks for a time event). */
    public final int perfect;

    Onderdeel(boolean tijd, int maxTicks, int perfect) {
        this.tijd = tijd;
        this.maxTicks = maxTicks;
        this.perfect = perfect;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The Highscores / Scorebord board: spelen_&lt;id&gt;. */
    public String board() {
        return "spelen_" + id();
    }

    public Component naam() {
        return Component.translatable("gui.guhs.knabbelspelen.onderdeel." + id());
    }

    /**
     * The zeskamp points (0..1000) for a result: points relative to the perfect score, times the other way round
     * (perfect time or faster = 1000). Not finished (a time event, score &lt; 0) = 0.
     */
    public int zeskamp(int score) {
        if (score < 0) {
            return 0;
        }
        if (tijd) {
            return score <= perfect ? 1000 : (int) Math.round(1000.0 * perfect / score);
        }
        return (int) Math.min(1000, Math.round(1000.0 * score / perfect));
    }

    public static Onderdeel of(int i) {
        return values()[Math.max(0, Math.min(values().length - 1, i))];
    }

    /** The Highscores board of the whole zeskamp (total 0..6000). */
    public static final String ZESKAMP_BOARD = "spelen_zeskamp";
}
