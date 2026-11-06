package nl.juiced.guhs.feature.vadsrijk.zone4.boot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * T-Z4BOOT: the looks of the Zeilbootje on three axes (roadmap §4.1/§10.2): the sail colour ({@link As#ZEIL}: one texture each),
 * the pennant on the mast top ({@link As#WIMPEL}) and the figurehead on the bow ({@link As#BOEG}) (a bone each in the model; the
 * renderer shows only the chosen one). Looks only: every variant sails the same.
 * Where you get one: {@code start} (you have it with the boat), {@code winkel} (buy it in the boat screen for brons), or
 * {@code beloning} (a quest / boss gives it: {@link Boot#geefVariant}).
 */
public enum BootVariant {
    ZEIL_ROZE(As.ZEIL, "roze", 0, "start", 0xFFFF8CBA),
    ZEIL_MINT(As.ZEIL, "mint", 600, "winkel", 0xFF8CE6C0),
    ZEIL_LAVENDEL(As.ZEIL, "lavendel", 600, "winkel", 0xFFC0A4F2),
    ZEIL_ZEEBLAUW(As.ZEIL, "zeeblauw", 600, "winkel", 0xFF7CC4F4),
    ZEIL_HONING(As.ZEIL, "honing", 900, "winkel", 0xFFFFCC66),
    ZEIL_STERRENNACHT(As.ZEIL, "sterrennacht", 0, "beloning", 0xFF3A3478),
    WIMPEL_HARTJE(As.WIMPEL, "hartje", 0, "start", 0xFFFF5F9F),
    WIMPEL_STER(As.WIMPEL, "ster", 300, "winkel", 0xFFFFD84A),
    WIMPEL_STREEPJES(As.WIMPEL, "streepjes", 300, "winkel", 0xFF6AB8F0),
    WIMPEL_VADSRIJK(As.WIMPEL, "vadsrijk", 0, "beloning", 0xFF8A4AD0),
    BOEG_GUHKOPJE(As.BOEG, "guhkopje", 0, "start", 0xFFFFC0D8),
    BOEG_DOLFIJNTJE(As.BOEG, "dolfijntje", 800, "winkel", 0xFF8AB4E8),
    BOEG_STERRETJE(As.BOEG, "sterretje", 800, "winkel", 0xFFFFD040),
    BOEG_DRAAKJE(As.BOEG, "draakje", 0, "beloning", 0xFFB070E0);

    public enum As {
        ZEIL, WIMPEL, BOEG;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public BootVariant standaard() {
            for (BootVariant v : BootVariant.values()) {   // (the "start" one of this axis)
                if (v.as == this && v.bron.equals("start")) {
                    return v;
                }
            }
            throw new IllegalStateException();
        }

        public List<BootVariant> varianten() {
            List<BootVariant> l = new ArrayList<>();
            for (BootVariant v : BootVariant.values()) {
                if (v.as == this) {
                    l.add(v);
                }
            }
            return l;
        }

        @Nullable
        public static As van(String id) {
            for (As a : values()) {
                if (a.id().equals(id)) {
                    return a;
                }
            }
            return null;
        }
    }

    public final As as;
    /** The short id within its axis ("mint"); {@link #id()} is the full one ("zeil_mint"). */
    public final String kort;
    /** Price in brons in the boat screen (0 = not for sale). */
    public final long prijs;
    public final String bron;
    /** The swatch colour in the screen (ARGB). */
    public final int kleur;

    BootVariant(As as, String kort, long prijs, String bron, int kleur) {
        this.as = as;
        this.kort = kort;
        this.prijs = prijs;
        this.bron = bron;
        this.kleur = kleur;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String naamKey() {
        return "vadsrijk.z4boot.variant." + id();
    }

    /** Index within its axis (synced to the entity). */
    public int index() {
        return as.varianten().indexOf(this);
    }

    public static BootVariant van(As as, int index) {
        List<BootVariant> l = as.varianten();
        return index >= 0 && index < l.size() ? l.get(index) : as.standaard();
    }

    /** By full id ("zeil_mint") or "as:kort" ("zeil:mint"). */
    @Nullable
    public static BootVariant van(String id) {
        String s = id.toLowerCase(Locale.ROOT).replace(':', '_');
        for (BootVariant v : values()) {
            if (v.id().equals(s)) {
                return v;
            }
        }
        return null;
    }
}
