package nl.juiced.guhs.feature.techsaus;

import nl.juiced.guhs.feature.vadskracht.Sauzen;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * Every tunable number of the sauce machines (bbq2, tech-vloeistof), in ONE place: tools/features/tech_vloeistof.py reads
 * this file for its texts ({@code getal("POMP_PER_TIK")}), so the texts never disagree with the code. What the machines ask in
 * vadskracht comes from {@link VadsGetallen} (the Grillkoolpers has no number there, so it has one here).
 * Amounts are mB (1000 = a bucket, {@link Sauzen#EMMER}), times are game ticks.
 */
public final class SausGetallen {
    // --- vadskracht ---
    public static final int POMP = VadsGetallen.POMP, BROUWAUTOMAAT = VadsGetallen.BROUWKETEL, FRITUURAUTOMAAT = VadsGetallen.FRITUUR;
    /** The Grillkoolpers: it squeezes as hard as a brouwketel bubbles. */
    public static final int GRILLKOOLPERS = 8;

    // --- the Sauspomp ---
    /** What the pump lifts per tick of work (a bucket per two seconds). */
    public static final int POMP_PER_TIK = 25;
    public static final int POMP_TANK = 4000;

    // --- the Sausslang ---
    /** Every this many ticks a pump pushes and a machine slurps through the hoses. */
    public static final int SLANG_TIKKEN = 5;
    /** What goes through per push or slurp (so 500 mB a second for each pump and each machine). */
    public static final int SLANG_PER_KEER = 125;
    /** The longest hose: this many Sausslang blocks from a pump or a machine. Further on nothing comes out. */
    public static final int SLANG_MAX = 256;
    /** A hose net that was looked up is trusted this long (it is looked up again at once when a hose changes). */
    public static final int SLANG_ONTHOUD = 100;

    // --- the Sausvat ---
    public static final int VAT = 16_000;

    // --- the Brouwautomaat (one pan: a bucket of kaassaus + one ingredient + three bottles = three Guhdrankjes) ---
    public static final int BROUW_TANK = 4000, BROUW_SAUS = 1000, BROUW_FLESJES = 3, BROUW_TIKKEN = 200;

    // --- the Frituurautomaat ---
    public static final int FRITUUR_TANK = 4000;
    /** Frituursaus per fried snack (a bucket fries a hundred). */
    public static final int FRITUUR_SAUS = 10;
    public static final int FRITUUR_TIKKEN = 10;

    // --- the Grillkoolpers (a bucket of frituursaus + a bucket of water = one block of grillkool, as in the world) ---
    public static final int PERS_TANK = 4000, PERS_SAUS = 1000, PERS_WATER = 1000, PERS_TIKKEN = 100;

    private SausGetallen() {
    }
}
