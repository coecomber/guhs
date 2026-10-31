package nl.juiced.guhs.feature.kermiskraam;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 1.8.1: the seven stalls of the guh kermis. Food stalls sell for Kaas Knabbels (a {@link Waar} per offer); game stalls start a
 * round of {@link KraamSpel} and pay out in kermisbonnen. {@link #hoogste} is the top score of a game stall.
 */
public enum Kraam {
    CHURROS("churros", 0, List.of(new Waar("churros", 15), new Waar("churros_guhtella", 18))),
    SUIKERSPIN("suikerspin", 0, List.of(new Waar("suikerspin", 8))),
    POFFERTJES("poffertjes", 0, List.of(new Waar("poffertjes", 10))),
    OLIEBOLLEN("oliebollen", 0, List.of(new Waar("oliebollen", 12))),
    VISSEN("vissen", 3, List.of()),
    BLIKKEN("blikken", 6, List.of()),
    SCHIETEN("schieten", 10, List.of());

    /** One thing on sale: item id (guhs:) and its price in Kaas Knabbels. */
    public record Waar(String item, int prijs) {
    }

    public final String id;
    public final int hoogste;
    public final List<Waar> waren;

    Kraam(String id, int hoogste, List<Waar> waren) {
        this.id = id;
        this.hoogste = hoogste;
        this.waren = waren;
    }

    public boolean isSpel() {
        return hoogste > 0;
    }

    /** The block id of this stall. */
    public String blok() {
        return "kermiskraam_" + id;
    }

    @Nullable
    public static Kraam van(String id) {
        for (Kraam k : values()) {
            if (k.id.equals(id)) {
                return k;
            }
        }
        return null;
    }
}
