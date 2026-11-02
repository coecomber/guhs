package nl.juiced.guhs.feature.erfenis;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * De Erfenis (POC): every kind of building, data-driven in one table. To add a building: add a constant here (id, footprint,
 * costs/times per level, limits per stadhuis level, Knusheid per level, icon), give it a shape in {@link Stempel#vorm} and
 * texts in tools/features/erfenis.py ({@code erfenis.gebouw.<id>} and {@code erfenis.gebouw.<id>.uitleg}).
 * <p>
 * Arrays indexed by level: index 0 is unused (a building is never level 0 except while its first build runs), index L is
 * "to reach level L". The limit arrays are indexed by stadhuis level (index 0 unused).
 */
public enum GebouwType {
    /** The heart of the base: its level unlocks everything else. Working name. */
    STADHUIS("stadhuis", 5, Items.BELL, false,
            new int[] {0, 0, 150, 400, 800, 1400},
            new int[] {0, 0, 20, 30, 40, 50},
            new int[] {0, 1, 1, 1, 1, 1},
            new int[] {0, 1, 2, 3, 4, 5},
            new int[] {0, 10, 20, 30, 40, 50}),
    /** Makes Bouwspul every real minute, kept inside until you collect it. */
    MOLEN("molen", 3, Items.WHEAT, false,
            new int[] {0, 50, 120, 300},
            new int[] {0, 10, 20, 30},
            new int[] {0, 1, 2, 2, 3, 3},
            new int[] {0, 1, 2, 2, 3, 3},
            new int[] {0, 2, 4, 6}),
    /** Raises the most Bouwspul you can hold. */
    SCHUURTJE("schuurtje", 3, Items.BARREL, false,
            new int[] {0, 80, 150, 350},
            new int[] {0, 10, 20, 30},
            new int[] {0, 0, 1, 2, 2, 3},
            new int[] {0, 0, 2, 2, 3, 3},
            new int[] {0, 2, 4, 6}),
    /** A wall piece: placed at once (no builder), upgraded by the builder. */
    MUUR("muur", 1, Items.COBBLESTONE_WALL, true,
            new int[] {0, 5, 10, 25, 50, 100},
            new int[] {0, 0, 10, 20, 30, 40},
            new int[] {0, 10, 20, 35, 50, 75},
            new int[] {0, 1, 2, 3, 4, 5},
            new int[] {0, 1, 1, 1, 1, 1}),
    /** Decoration: no levels, placed at once, gives Knusheid. */
    BOOMPJE("boompje", 2, Items.CHERRY_SAPLING, true,
            new int[] {0, 20},
            new int[] {0, 0},
            new int[] {0, 3, 6, 10, 15, 20},
            new int[] {0, 1, 1, 1, 1, 1},
            new int[] {0, 3});

    public static final int MAX_STADHUIS = 5;

    /** The id in saved data and lang keys. */
    public final String id;
    /** Footprint: maat x maat tiles. */
    public final int maat;
    private final Item icoon;
    /** Placing a new one is instant (no builder needed); its upgrades still use the builder. */
    public final boolean direct;
    private final int[] kosten, tijd, aantal, maxLevel, knus;

    GebouwType(String id, int maat, Item icoon, boolean direct, int[] kosten, int[] tijd, int[] aantal, int[] maxLevel, int[] knus) {
        this.id = id;
        this.maat = maat;
        this.icoon = icoon;
        this.direct = direct;
        this.kosten = kosten;
        this.tijd = tijd;
        this.aantal = aantal;
        this.maxLevel = maxLevel;
        this.knus = knus;
    }

    /** The highest level this type has at all. */
    public int topLevel() {
        return kosten.length - 1;
    }

    /** Bouwspul to reach this level (level 1 = to build a new one). */
    public int kosten(int level) {
        return level >= 1 && level < kosten.length ? kosten[level] : 0;
    }

    /** Seconds of building to reach this level (0 = instant). */
    public int tijdSec(int level) {
        return level >= 1 && level < tijd.length ? tijd[level] : 0;
    }

    /** How many of these a base may have at this stadhuis level. */
    public int maxAantal(int stadhuisLevel) {
        return aantal[clamp(stadhuisLevel)];
    }

    /** The highest level these may have at this stadhuis level (for the stadhuis itself: its top level). */
    public int maxLevel(int stadhuisLevel) {
        return Math.min(topLevel(), maxLevel[clamp(stadhuisLevel)]);
    }

    /** The Knusheid one of these gives at this level. */
    public int knus(int level) {
        return level >= 1 && level < knus.length ? knus[Math.min(level, knus.length - 1)] : 0;
    }

    /** The first stadhuis level at which one more of these may be placed beyond {@code heeft}, or -1 (never). */
    public int ontgrendeltAantal(int heeft) {
        for (int s = 1; s <= MAX_STADHUIS; s++) {
            if (aantal[s] > heeft) {
                return s;
            }
        }
        return -1;
    }

    /** The first stadhuis level at which these may reach {@code level}, or -1. */
    public int ontgrendeltLevel(int level) {
        if (this == STADHUIS) {
            return level <= topLevel() ? 1 : -1;
        }
        for (int s = 1; s <= MAX_STADHUIS; s++) {
            if (maxLevel[s] >= level) {
                return s;
            }
        }
        return -1;
    }

    /** Bouwspul per real minute a molen makes at this level. */
    public static int productiePerMinuut(int level) {
        return switch (level) {
            case 1 -> 6;
            case 2 -> 12;
            case 3 -> 20;
            default -> 0;
        };
    }

    /** How much a molen of this level holds before it stops (collect it!). */
    public static int molenOpslag(int level) {
        return switch (level) {
            case 1 -> 60;
            case 2 -> 120;
            case 3 -> 200;
            default -> 0;
        };
    }

    /** The extra Bouwspul room a schuurtje of this level gives. */
    public static int schuurOpslag(int level) {
        return switch (level) {
            case 1 -> 150;
            case 2 -> 300;
            case 3 -> 500;
            default -> 0;
        };
    }

    /** The Bouwspul room the stadhuis itself gives. */
    public static int stadhuisOpslag(int level) {
        return level <= 0 ? 0 : 200 + 100 * (level - 1);
    }

    public ItemStack icoon() {
        return new ItemStack(icoon);
    }

    public String naamKey() {
        return "erfenis.gebouw." + id;
    }

    public String uitlegKey() {
        return "erfenis.gebouw." + id + ".uitleg";
    }

    @Nullable
    public static GebouwType van(String id) {
        for (GebouwType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    private static int clamp(int stadhuisLevel) {
        return Math.max(1, Math.min(MAX_STADHUIS, stadhuisLevel));
    }
}
