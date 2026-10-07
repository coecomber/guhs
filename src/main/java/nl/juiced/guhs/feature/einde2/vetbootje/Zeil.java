package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.Locale;

/**
 * einde2, slice vetbootje: the pictures a vetbootje's sail can show. The order is the order of the sail layers
 * (textures/entity/vetbootje_zeil_&lt;id&gt;.png, tools/features/einde2_vetbootje_modellen.py ZEILEN): append only.
 */
public enum Zeil {
    MIKA, KNABBEL, STREEP, HART, STER, KRUIMEL;

    /** The lower-case id used in texture names and lang keys (gui.guhs.einde2_vetbootje.zeil.&lt;id&gt;). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** By saved name; an unknown name gives the Mika sail. */
    public static Zeil vanNaam(String naam) {
        for (Zeil z : values()) {
            if (z.name().equals(naam)) {
                return z;
            }
        }
        return MIKA;
    }

    public static Zeil vanNummer(int nr) {
        return values()[Math.floorMod(nr, values().length)];
    }
}
