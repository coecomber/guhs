package nl.juiced.guhs.feature.vadsrijk.dialoog;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * T-DIALOOG: the mood of one page in the Vadsrijk talking screen (the portrait hops, droops, jumps or shakes, with a little
 * sign next to it). Explicit per line ({@link RijkDialoog#emotie}, quest data {@code "npc:blij"}), else guessed from the text
 * the reader sees ({@link #raad}).
 */
public enum Emotie {
    NEUTRAAL, BLIJ, VERDRIETIG, VERRAST, BOOS;

    /** "blij", "happy", "verdrietig", "sad"... (null/unknown: null). */
    @Nullable
    public static Emotie van(@Nullable String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.toLowerCase(Locale.ROOT).trim()) {
            case "blij", "happy", "vrolijk" -> BLIJ;
            case "verdrietig", "sad", "droevig", "bedroefd" -> VERDRIETIG;
            case "verrast", "surprised", "verbaasd", "schrik" -> VERRAST;
            case "boos", "angry", "kwaad" -> BOOS;
            case "neutraal", "neutral", "gewoon" -> NEUTRAAL;
            default -> null;
        };
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * A guess from what a line says (Dutch, and the English of en_us): a cry of joy is happy, "?!" or "Ooh" surprised, "grr"
     * angry, "helaas" or a trailing "..." sad. Most lines stay neutral: the portrait then only breathes.
     */
    public static Emotie raad(@Nullable String tekst) {
        if (tekst == null || tekst.isBlank()) {
            return NEUTRAAL;
        }
        String t = tekst.toLowerCase(Locale.ROOT).trim();
        if (bevat(t, "grr", "grrr", "hmpf", "boos", "kwaad", "angry", "woest", "hoe durf", "how dare")) {
            return BOOS;
        }
        if (t.contains("?!") || t.contains("!?") || begint(t, "ooh", "oh!", "oh,", "huh", "wat?", "wauw", "wow", "whoa", "nee maar", "hé", "he!")) {
            return VERRAST;
        }
        if (bevat(t, "helaas", "snik", "huil", "verdrietig", "jammer", "sniff", "sob", "sadly", "sad ", "alas") || t.endsWith("...") || t.endsWith("…")) {
            return VERDRIETIG;
        }
        if (bevat(t, "hoera", "joepie", "jippie", "jeej", "yay", "hooray", "dankjewel", "dank je", "bedankt", "thank", "haha", "hihi", "vahoeg!", "♥")) {
            return BLIJ;
        }
        return NEUTRAAL;
    }

    private static boolean bevat(String t, String... woorden) {
        for (String w : woorden) {
            if (t.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private static boolean begint(String t, String... woorden) {
        for (String w : woorden) {
            if (t.startsWith(w)) {
                return true;
            }
        }
        return false;
    }
}
