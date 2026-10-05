package nl.juiced.guhs.feature.band;

import java.util.Locale;

/**
 * Where a guh (or maatje) is, for "Waar is mijn guh?" in its dagboekje (lang {@code gui.guhs.band.plek.<id>}, with the
 * args: detail, dimension name, x, y, z). Append-only.
 */
public enum PlekSoort {
    /** Walking around. */
    WERELD,
    /** Sitting (you told it to). */
    ZIT,
    /** Living at its Guhhuisje (detail: the huisje's name). */
    HUISJE,
    /** Asleep inside its Guhhuisje (detail: the huisje's name). */
    SLAAPT_IN_HUISJE,
    /** Its owner rides it. */
    RIJDT_OP,
    /** It rides along on something (detail: what). */
    RIJDT_MEE,
    /** In a player's pockets (detail: the player's name). */
    ITEM_SPELER,
    /** In a chest (detail: what kind). */
    ITEM_KIST,
    /** In a guh's backpack (detail: that guh's name). */
    ITEM_RUGZAK,
    /** In a Bank Guh. */
    ITEM_BANK,
    /** Dropped on the ground as an item. */
    ITEM_GROND,
    /** Running in a Guh Wheel. */
    GUHWIEL,
    /** In the Guhkamer in your Guhmaag. */
    GUHKAMER,
    /** On a player's shoulder (detail: the player's name). */
    SCHOUDER,
    /** Inside a guh (a turtle during its poetsbeurt; detail: the guh's name). */
    IN_GUH,
    /** Unknown (never seen yet, or gone). */
    ONBEKEND,
    /** 3.0: it died: "In de wolkjes... njeg" (the Knuffelhart can bring it back, see Wolkjes). */
    IN_DE_WOLKJES,
    /** 1.2.5: just called over with "Roep naar mij" in the Guhdex (detail: the owner's name). */
    BIJ_JOU,
    /** guhpixel: away on a trip of the Reisbureau, stored as data (detail: the destination). */
    OP_VAKANTIE,
    /** guhpixel: at work (asleep) on a Guhkantoor, stored as data. */
    OP_KANTOOR;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PlekSoort byId(String id) {
        for (PlekSoort s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return ONBEKEND;
    }
}
