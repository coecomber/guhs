package nl.juiced.guhs.feature.huisje;

import java.util.Locale;

/**
 * The three Guhhuisjes: how many residents fit (guhs, muisjes, Schilly and Poepschilly together), the footprint
 * (breedte x breedte blocks, hoogte blocks tall) and the scale of the guh-head model (32 model pixels wide at scale 1).
 */
public enum HuisjeMaat {
    KLEIN(3, 2, 2, 1.0f),
    MEDIUM(5, 3, 3, 1.5f),
    GROOT(8, 4, 4, 2.0f);

    private final int plekken, breedte, hoogte;
    private final float schaal;

    HuisjeMaat(int plekken, int breedte, int hoogte, float schaal) {
        this.plekken = plekken;
        this.breedte = breedte;
        this.hoogte = hoogte;
        this.schaal = schaal;
    }

    /** How many residents fit. */
    public int plekken() {
        return plekken;
    }

    public int breedte() {
        return breedte;
    }

    public int hoogte() {
        return hoogte;
    }

    public float schaal() {
        return schaal;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The block / item id: guhhuisje_klein, guhhuisje_medium, guhhuisje_groot. */
    public String blokId() {
        return "guhhuisje_" + id();
    }

    public static HuisjeMaat byId(String id) {
        for (HuisjeMaat m : values()) {
            if (m.id().equals(id)) {
                return m;
            }
        }
        return KLEIN;
    }
}
