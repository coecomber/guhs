package nl.juiced.guhs.feature.band;

import java.util.Locale;

/**
 * Why a guh gets hearts: the default amount per event and the cap per Minecraft day per guh (fundament enforces the caps).
 * Order is append-only (saved by ordinal).
 */
public enum Reden {
    VOEREN(2, 40),
    AAIEN(1, 20),
    KNUFFELEN(5, 50),
    SAMEN_TIJD(1, 60),
    REIZEN(1, 50),
    KLUSJE(2, 40),
    MINIGAME(15, 100),
    RECORD(10, 50),
    FAVORIET_ONTDEKT(50, 400),
    FAVORIET(5, 30),
    SPEELGOED(3, 30),
    VRIENDJE(2, 20),
    OVERIG(1, 50);

    private final int standaard, dagMax;

    Reden(int standaard, int dagMax) {
        this.standaard = standaard;
        this.dagMax = dagMax;
    }

    /** The usual amount for one such event. */
    public int standaard() {
        return standaard;
    }

    /** At most this many hearts per Minecraft day per guh for this reason. */
    public int dagMax() {
        return dagMax;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Quiet reasons (time together, travelling) only show a heart, no "+1" in the action bar every minute. */
    public boolean stil() {
        return this == SAMEN_TIJD || this == REIZEN;
    }
}
