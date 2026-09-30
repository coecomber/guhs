package nl.juiced.guhs.feature.kapper;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * The eight hair dyes (items {@code haarverf_<kleur>}, also the collection entries). The colour is set with
 * GuhEntity.setHaarkleur and tints the HAAR piece; REGENBOOG keeps changing colour ({@link KapperHaar#regenboog}).
 */
public enum Haarverf {
    ROZE(0xFF8FCB),
    MINT(0x8FF0C4),
    CITROEN(0xFFF07A),
    LAVENDEL(0xC7A6FF),
    HEMELSBLAUW(0x8CCBFF),
    PERZIK(0xFFB47F),
    ZILVER(0xD8DCE6),
    REGENBOOG(0xFF9AD5);

    /** The tint (RGB); REGENBOOG starts pink and then runs through all the colours. */
    public final int rgb;

    Haarverf(int rgb) {
        this.rgb = rgb;
    }

    /** "haarverf_roze": the item id and the collection entry. */
    public String id() {
        return "haarverf_" + kleur();
    }

    /** "roze" (lang: gui.guhs.kapper.verf.&lt;kleur&gt;). */
    public String kleur() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static Haarverf byId(String id) {
        for (Haarverf v : values()) {
            if (v.id().equals(id) || v.kleur().equals(id)) {
                return v;
            }
        }
        return null;
    }
}
