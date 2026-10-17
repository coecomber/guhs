package nl.juiced.guhs.feature.einde2.spd1start;

import java.util.List;
import java.util.Locale;

/**
 * einde2 "Sponsguh", slice sp_d1_start, step 4: the rules of the grill game, without a world (CONTRACT_E2_C 7.3). Five orders,
 * one at a time; each is a short list of parts from the bottom up (always a bun, 1 or 2 things, a bun). A part put on the grill
 * in the right order stays; a wrong part makes the burger flop: that order starts over, nothing else is lost (the orders
 * already done stay done). There is no clock: the time is only a score ({@link Bakken}).
 */
public final class GrillSpel {
    /** The parts, each from its own crate. */
    public enum Deel {
        BROODJE("minecraft:bread"), BURGER("minecraft:cooked_beef"), KAAS("guhs:kaas_knabbels"), ZEESLA("minecraft:kelp"),
        AUGURK("minecraft:sea_pickle");

        /** The item that shows it (in the crate and as a layer on the grill). */
        public final String item;

        Deel(String item) {
            this.item = item;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** What a part did. */
    public enum Uitslag {
        /** Right part: it is on the burger now. */
        GOED,
        /** Wrong part: the burger flops, this order starts over. */
        FOUT,
        /** The last part of an order: the order is done (the next one follows). */
        BESTELLING_KLAAR,
        /** The last part of the last order: the game is won. */
        ALLES_KLAAR,
        /** Nothing to do (the game was over already). */
        NIETS
    }

    /** The five orders (order 5 is Patguh's: a double). */
    public static final List<List<Deel>> BESTELLINGEN = List.of(
            List.of(Deel.BROODJE, Deel.BURGER, Deel.BROODJE),
            List.of(Deel.BROODJE, Deel.BURGER, Deel.KAAS, Deel.BROODJE),
            List.of(Deel.BROODJE, Deel.ZEESLA, Deel.BURGER, Deel.BROODJE),
            List.of(Deel.BROODJE, Deel.BURGER, Deel.AUGURK, Deel.BROODJE),
            List.of(Deel.BROODJE, Deel.BURGER, Deel.BURGER, Deel.BROODJE));
    public static final int AANTAL = BESTELLINGEN.size();

    private int klaar, gelegd, fouten;

    /** A game that starts at order {@code klaar + 1} (the orders done before a break stay done). */
    public GrillSpel(int klaar) {
        this.klaar = Math.max(0, Math.min(AANTAL, klaar));
    }

    /** Puts a part on the grill. */
    public Uitslag leg(Deel deel) {
        if (klaar >= AANTAL) {
            return Uitslag.NIETS;
        }
        List<Deel> b = BESTELLINGEN.get(klaar);
        if (b.get(gelegd) != deel) {
            gelegd = 0;
            fouten++;
            return Uitslag.FOUT;
        }
        gelegd++;
        if (gelegd < b.size()) {
            return Uitslag.GOED;
        }
        gelegd = 0;
        klaar++;
        return klaar >= AANTAL ? Uitslag.ALLES_KLAAR : Uitslag.BESTELLING_KLAAR;
    }

    /** The order on the grill now (null: all done). */
    public List<Deel> bestelling() {
        return klaar >= AANTAL ? null : BESTELLINGEN.get(klaar);
    }

    /** The part that comes next (null: all done). */
    public Deel volgende() {
        List<Deel> b = bestelling();
        return b == null ? null : b.get(gelegd);
    }

    /** How many orders are done. */
    public int klaar() {
        return klaar;
    }

    /** How many parts of the order on the grill are on already. */
    public int gelegd() {
        return gelegd;
    }

    /** How many times a burger flopped (for the end line only; it never counts against you). */
    public int fouten() {
        return fouten;
    }

    public boolean allesKlaar() {
        return klaar >= AANTAL;
    }
}
