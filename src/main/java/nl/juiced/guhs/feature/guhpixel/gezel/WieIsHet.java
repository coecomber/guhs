package nl.juiced.guhs.feature.guhpixel.gezel;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.RandomSource;

/**
 * Wie is het, guh-editie: pure game logic. A board of {@link #KAARTEN} guhs, each a mix of fur colour, hat, glasses, bow
 * and personality (no two the same). Both sides secretly hold one of them; in turn they ask a yes/no question
 * ({@link #VRAGEN}) and the guhs that do not fit the answer are flipped down for the asker ({@link #wegstrepen}). Who
 * guesses the other's guh first wins; a wrong guess loses.
 */
public final class WieIsHet {
    public static final int KAARTEN = 20;
    /** Fur colours: lang gui.guhs.gezel.kleur.<id>; the screen uses the RGB. */
    public static final String[] KLEUREN = {"roze", "blauw", "geel", "groen", "paars", "wit", "bruin", "mint"};
    public static final int[] KLEUR_RGB = {0xF4A6C8, 0x8CB8F0, 0xF6DC7A, 0x9FD98A, 0xC4A2EC, 0xF4F0EA, 0xB08462, 0x9EE6D2};
    /** Hats: 0 = none (lang gui.guhs.gezel.hoed.<id>). */
    public static final String[] HOEDEN = {"geen", "slaapmuts", "kroontje", "koksmuts", "strohoed"};
    /** Personalities (lang gui.guhs.gezel.aard.<id>). */
    public static final String[] AARDEN = {"vadsig", "lief", "stoer", "slaperig"};
    /** Names on the cards (no lang: names are names). */
    public static final String[] NAMEN = {"Pien", "Bolle", "Snuf", "Kaasje", "Doerak", "Mopje", "Fleur", "Knabbel", "Pluis", "Sjors",
            "Bobbie", "Wiebel", "Dotje", "Guusje", "Snoetje", "Tijger", "Vlokje", "Bram", "Lientje", "Kruimel", "Moppie", "Flip"};

    public record Kaart(String naam, int kleur, int hoed, boolean bril, boolean strik, int aard) {
    }

    /** A yes/no question. soort: kleur (arg = colour), hoed (any hat), hoedsoort (arg = hat), bril, strik, aard (arg). */
    public record Vraag(String soort, int arg) {
        public boolean geldt(Kaart k) {
            return switch (soort) {
                case "kleur" -> k.kleur() == arg;
                case "hoed" -> k.hoed() != 0;
                case "hoedsoort" -> k.hoed() == arg;
                case "bril" -> k.bril();
                case "strik" -> k.strik();
                case "aard" -> k.aard() == arg;
                default -> false;
            };
        }

        /** The lang key of the question; colour, hat and personality questions take one argument (the word). */
        public String sleutel() {
            return "gui.guhs.gezel.vraag." + soort;
        }

        public String argSleutel() {
            return switch (soort) {
                case "kleur" -> "gui.guhs.gezel.kleur." + KLEUREN[arg];
                case "hoedsoort" -> "gui.guhs.gezel.hoed." + HOEDEN[arg];
                case "aard" -> "gui.guhs.gezel.aard." + AARDEN[arg];
                default -> "";
            };
        }
    }

    /** Every question, in the order of the buttons. */
    public static final List<Vraag> VRAGEN = maakVragen();

    private static List<Vraag> maakVragen() {
        List<Vraag> l = new ArrayList<>();
        for (int i = 0; i < KLEUREN.length; i++) {
            l.add(new Vraag("kleur", i));
        }
        l.add(new Vraag("hoed", 0));
        for (int i = 1; i < HOEDEN.length; i++) {
            l.add(new Vraag("hoedsoort", i));
        }
        l.add(new Vraag("bril", 0));
        l.add(new Vraag("strik", 0));
        for (int i = 0; i < AARDEN.length; i++) {
            l.add(new Vraag("aard", i));
        }
        return List.copyOf(l);
    }

    /** A board from a seed: {@link #KAARTEN} different guhs with different names. */
    public static List<Kaart> bord(long seed) {
        RandomSource rnd = RandomSource.create(seed);
        List<Kaart> l = new ArrayList<>();
        Set<String> gezien = new HashSet<>();
        List<String> namen = new ArrayList<>(List.of(NAMEN));
        int poging = 0;
        while (l.size() < KAARTEN && poging++ < 10_000) {
            Kaart k = new Kaart("", rnd.nextInt(KLEUREN.length), rnd.nextInt(3) == 0 ? 0 : 1 + rnd.nextInt(HOEDEN.length - 1),
                    rnd.nextInt(3) == 0, rnd.nextInt(3) == 0, rnd.nextInt(AARDEN.length));
            String sleutel = k.kleur() + "/" + k.hoed() + "/" + k.bril() + "/" + k.strik() + "/" + k.aard();
            if (gezien.add(sleutel)) {
                String naam = namen.remove(rnd.nextInt(namen.size()));
                l.add(new Kaart(naam, k.kleur(), k.hoed(), k.bril(), k.strik(), k.aard()));
            }
        }
        return List.copyOf(l);
    }

    /** All cards still up at the start. */
    public static BitSet alles() {
        BitSet b = new BitSet(KAARTEN);
        b.set(0, KAARTEN);
        return b;
    }

    /** The cards that stay up after `vraag` was answered with `antwoord` (the others are flipped down). */
    public static BitSet wegstrepen(List<Kaart> bord, BitSet open, Vraag vraag, boolean antwoord) {
        BitSet b = new BitSet(KAARTEN);
        for (int i = open.nextSetBit(0); i >= 0; i = open.nextSetBit(i + 1)) {
            if (i < bord.size() && vraag.geldt(bord.get(i)) == antwoord) {
                b.set(i);
            }
        }
        return b;
    }

    /** The bot's question: the one that splits its open cards most evenly; -1 when nothing splits (time to guess). */
    public static int botVraag(List<Kaart> bord, BitSet open, RandomSource rnd) {
        int beste = -1, besteMax = Integer.MAX_VALUE;
        int n = open.cardinality();
        for (int v = 0; v < VRAGEN.size(); v++) {
            int ja = wegstrepen(bord, open, VRAGEN.get(v), true).cardinality();
            if (ja == 0 || ja == n) {
                continue;
            }
            int max = Math.max(ja, n - ja);
            if (max < besteMax || (max == besteMax && rnd.nextBoolean())) {
                besteMax = max;
                beste = v;
            }
        }
        return beste;
    }

    private WieIsHet() {
    }
}
