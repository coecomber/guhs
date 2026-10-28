package nl.juiced.guhs.feature.fabriek;

/**
 * 1.8.0 (vadskracht, de Fabriekshal): every tunable number of the wool and block factory machines in ONE place (VK per
 * second, ticks per job, how much comes out), like VadsGetallen does for the older machines.
 */
public final class FabriekGetallen {
    /** What the machines ask, in VK per second. */
    public static final int FABRIEK = 12, SPINMACHINE = 6, VERFBAD = 6, KNUFFELRECYCLER = 6, SCHEERSTAL = 8;
    /** Ticks per job: one batch of blocks, one wool, one dye job, one shredded thing. */
    public static final int FABRIEK_TICKS = 10, SPIN_TICKS = 30, VERF_TICKS = 20, RECYCLE_TICKS = 20;
    /** The Spinmachine: this much pluis (or wolkenpluis) is one white wool. */
    public static final int PLUIS_PER_WOL = 2;
    /** The Scheerstal: how far it reaches (blocks, around it) and how often it looks for a fluffy wolkenschaapje (ticks). */
    public static final int SCHEERSTAL_STRAAL = 6, SCHEERSTAL_TICKS = 100;
    /** The Bouwblokken-fabriek: how far a linked chest may stand (blocks). */
    public static final int KOPPEL_BEREIK = 16;
    /** How long a player has to click a chest after sneak-clicking a fabriek (ticks). */
    public static final int KOPPEL_TIJD = 20 * 30;
    /** The Knuffelrecycler: pluis per wool block, carpet, knuffel, piece of guh clothing, cushion. */
    public static final int PLUIS_WOL = 2, PLUIS_TAPIJT = 1, PLUIS_KNUFFEL = 4, PLUIS_KLEDING = 3, PLUIS_KUSSEN = 3;

    /** The Bouwblokken-fabriek: how many recipes it does per job (bulk). */
    public static final int FABRIEK_PER_KEER = 4;
    /** De Kleurmolen (tier 2): VK, ticks per flower, and the bonus: a flower gives its recipe's dye + 1 + (0 or 1). */
    public static final int KLEURMOLEN = 8, MOLEN_TICKS = 20, MOLEN_BONUS = 1;
    /** De Regenboogpers (tier 3): VK, ticks per job; one job is one pluis + PERS_SAUS mB kaassaus = PERS_KLEURSTOF dye. */
    public static final int REGENBOOGPERS = 20, PERS_TICKS = 20, PERS_SAUS = 100, PERS_KLEURSTOF = 2;
    /** The pers's own kaassaus tank (mB) and how much it slurps from a Sausvat / sausbuis next to it per job. */
    public static final int PERS_TANK = 4000, PERS_SLURP = 500;

    private FabriekGetallen() {
    }
}
