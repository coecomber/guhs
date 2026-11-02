package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.Random;

/**
 * De Erfenis, wereldkaart: cute guh names for NPC villages and their owners, composed from word lists ("Knabbelhoeve van
 * Oma Pluis", "Vadsdorp", "Kaaskorstje", "Het Wiebelige Nestje", "Snoetwijk aan de Kaas"). Deterministic for a {@link Random}.
 * The names are Dutch on purpose (also in English): they are place names.
 */
public final class NpcNamen {
    private static final String[] TITEL = {"Oma", "Opa", "Tante", "Ome", "Juf", "Meester", "Boer", "Boerin", "Dikke", "Kleine",
            "Ouwe", "Sir", "Mevrouw", "Prinses", "Kapitein", "Bakker", "Dokter", "Burgemeester", "Neef", "Nicht"};
    private static final String[] NAAM = {"Pluis", "Snoet", "Wiebel", "Knabbel", "Snurk", "Vads", "Njeg", "Vahoeg", "Kaasje", "Oortje",
            "Pootje", "Bolletje", "Knuffel", "Snuffel", "Piep", "Mikkie", "Dotje", "Flapje", "Guhrt", "Guhrda", "Brammetje", "Snoes",
            "Muisje", "Hapje", "Wolkje", "Plof", "Kruimel", "Bibber", "Tuut", "Mopje"};
    /** First halves (joined straight onto a second half: "Knabbel" + "hoeve"). */
    private static final String[] VOOR = {"Knabbel", "Vads", "Kaas", "Snoet", "Pluis", "Wiebel", "Knuffel", "Snurk", "Njeg", "Guh",
            "Kruimel", "Oortjes", "Pootjes", "Snuffel", "Bolle", "Hapjes", "Piep", "Wolkjes", "Tukkie", "Vahoeg", "Kriebel", "Slaapjes",
            "Koekjes", "Dekentjes", "Blub"};
    private static final String[] ACHTER = {"dorp", "hoeve", "korstje", "stee", "veld", "nest", "burcht", "hoek", "dal", "wijk",
            "heuvel", "holletje", "rade", "broek", "dijk", "kamp", "molen", "poort", "huizen", "gat", "stad", "oord", "hof"};
    private static final String[] BIJV = {"Wiebelige", "Knusse", "Vadsige", "Slaperige", "Kaasachtige", "Pluizige", "Ronde", "Gezellige",
            "Snurkende", "Kleine", "Dikke", "Zachte", "Giechelende", "Knabbelende", "Roze"};
    private static final String[] DING = {"Nestje", "Hoekje", "Holletje", "Heuveltje", "Dorpje", "Kaasje", "Kussentje", "Molentje",
            "Dekentje", "Pannenkoekje"};
    private static final String[] WATER = {"de Kaas", "de Vla", "de Snoet", "de Plas", "het Knabbelmeer", "de Pluisbeek", "Zee",
            "de Slaapsloot", "de Melkweg", "de Guhs"};

    private NpcNamen() {
    }

    /** An owner: "Oma Pluis". */
    public static String eigenaar(Random r) {
        return TITEL[r.nextInt(TITEL.length)] + " " + NAAM[r.nextInt(NAAM.length)];
    }

    /** A village name; {@code eigenaar} is used in some of the patterns ("Knabbelhoeve van Oma Pluis"). */
    public static String dorp(Random r, String eigenaar) {
        String kort = VOOR[r.nextInt(VOOR.length)] + ACHTER[r.nextInt(ACHTER.length)];
        return switch (r.nextInt(10)) {
            case 0, 1 -> kort + " van " + eigenaar;
            case 2 -> "Het " + BIJV[r.nextInt(BIJV.length)] + " " + DING[r.nextInt(DING.length)];
            case 3 -> kort + " aan " + WATER[r.nextInt(WATER.length)];
            case 4 -> (r.nextBoolean() ? "Groot-" : "Klein-") + kort;
            case 5 -> "Sint-" + NAAM[r.nextInt(NAAM.length)] + ACHTER[r.nextInt(ACHTER.length)];
            default -> kort;
        };
    }

    /** A soft flag colour (opaque ARGB) from a pastel palette. */
    public static int kleur(Random r) {
        return PALET[r.nextInt(PALET.length)];
    }

    static final int[] PALET = {0xFFF58BB0, 0xFFF7A35C, 0xFFEAD35A, 0xFF8FD16A, 0xFF5EC9B3, 0xFF6FB6F0, 0xFF9C8CF0, 0xFFD58CF0,
            0xFFF07A7A, 0xFFB5895E, 0xFF7FD6E8, 0xFFFFB3C7};
}
