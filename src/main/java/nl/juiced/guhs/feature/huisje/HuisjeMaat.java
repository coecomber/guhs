package nl.juiced.guhs.feature.huisje;

import java.util.Locale;

/**
 * The Guhhuisjes: how many residents fit (guhs, muisjes, Schilly and Poepschilly together), the footprint
 * (breedte x breedte blocks, hoogte blocks tall) and the scale of the guh-head model (32 model pixels wide at scale 1).
 * 1.8.0: the Guhvilla (20 residents, one block taller: its roof is a second model, see HuisjeRenderer), and per size the
 * range of the home base: the default and how far the owner may turn the slider of the huisje screen
 * ({@link Huisje#zetBereik}): klein/medium/groot up to 10/20/40% more than the old 16 blocks, the villa up to twice that.
 */
public enum HuisjeMaat {
    KLEIN(3, 2, 2, 1.0f, 16, 17),
    MEDIUM(5, 3, 3, 1.5f, 16, 19),
    GROOT(8, 4, 4, 2.0f, 16, 22),
    VILLA(20, 4, 5, 2.0f, 20, 32);

    private final int plekken, breedte, hoogte, bereik, maxBereik;
    private final float schaal;

    HuisjeMaat(int plekken, int breedte, int hoogte, float schaal, int bereik, int maxBereik) {
        this.plekken = plekken;
        this.breedte = breedte;
        this.hoogte = hoogte;
        this.schaal = schaal;
        this.bereik = bereik;
        this.maxBereik = maxBereik;
    }

    /** The home base's range (blocks around the middle) of a new huisje of this size. */
    public int standaardBereik() {
        return bereik;
    }

    /** How far the owner may turn the range up. */
    public int maxBereik() {
        return maxBereik;
    }

    /** How far the owner may turn the range down (half the default). */
    public int minBereik() {
        return Math.max(4, bereik / 2);
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

    /** The block / item id: guhhuisje_klein, guhhuisje_medium, guhhuisje_groot, guhhuisje_villa. */
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
