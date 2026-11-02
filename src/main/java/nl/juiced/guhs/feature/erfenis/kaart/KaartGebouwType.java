package nl.juiced.guhs.feature.erfenis.kaart;

import javax.annotation.Nullable;

/**
 * De Erfenis, wereldkaart: the building types an NPC village is made of, a COPY of the numbers in the core's
 * {@code feature/erfenis/GebouwType} (branch 190-erfenis-poc: ids, footprints, counts and max levels per stadhuis level,
 * Knusheid per level). The ids are the same, so a plan maps 1:1 onto the core's {@code Base.Plan}.
 * <p>
 * MERGE: replace this enum by {@code GebouwType} (or keep it and map by {@link #id}; {@code KaartBronErfenis} does that).
 * Arrays: index = stadhuis level (0 unused) for the limits, index = level for Knusheid.
 */
public enum KaartGebouwType {
    STADHUIS("stadhuis", 5, new int[] {0, 1, 1, 1, 1, 1}, new int[] {0, 1, 2, 3, 4, 5}, new int[] {0, 10, 20, 30, 40, 50}),
    MOLEN("molen", 3, new int[] {0, 1, 2, 2, 3, 3}, new int[] {0, 1, 2, 2, 3, 3}, new int[] {0, 2, 4, 6}),
    SCHUURTJE("schuurtje", 3, new int[] {0, 0, 1, 2, 2, 3}, new int[] {0, 0, 2, 2, 3, 3}, new int[] {0, 2, 4, 6}),
    MUUR("muur", 1, new int[] {0, 10, 20, 35, 50, 75}, new int[] {0, 1, 2, 3, 4, 5}, new int[] {0, 1, 1, 1, 1, 1}),
    BOOMPJE("boompje", 2, new int[] {0, 3, 6, 10, 15, 20}, new int[] {0, 1, 1, 1, 1, 1}, new int[] {0, 3});

    public static final int MAX_STADHUIS = 5;
    /** The plot is MAAT x MAAT tiles (core: Plot.MAAT). */
    public static final int PLOT = 64;

    public final String id;
    public final int maat;
    private final int[] aantal, maxLevel, knus;

    KaartGebouwType(String id, int maat, int[] aantal, int[] maxLevel, int[] knus) {
        this.id = id;
        this.maat = maat;
        this.aantal = aantal;
        this.maxLevel = maxLevel;
        this.knus = knus;
    }

    public int topLevel() {
        return knus.length - 1;
    }

    public int maxAantal(int stadhuisLevel) {
        return aantal[clamp(stadhuisLevel)];
    }

    public int maxLevel(int stadhuisLevel) {
        return this == STADHUIS ? MAX_STADHUIS : Math.min(topLevel(), maxLevel[clamp(stadhuisLevel)]);
    }

    public int knus(int level) {
        return level >= 1 ? knus[Math.min(level, knus.length - 1)] : 0;
    }

    public String naamKey() {
        return "erfenis.kaart.gebouw." + id;
    }

    @Nullable
    public static KaartGebouwType van(String id) {
        for (KaartGebouwType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    private static int clamp(int s) {
        return Math.max(1, Math.min(MAX_STADHUIS, s));
    }
}
