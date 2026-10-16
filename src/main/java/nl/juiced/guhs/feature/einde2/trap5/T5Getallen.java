package nl.juiced.guhs.feature.einde2.trap5;

/**
 * einde2 tier 5 (trap5_machines): every tunable number of the slice in ONE place, like main's {@code VadsGetallen}: what a
 * machine asks (VK per second), what a source gives, ranges and costs. tools/features/einde2_trap5_machines.py reads the
 * VK numbers from this file for the tooltips and the FTB texts (so they never disagree).
 */
public final class T5Getallen {
    // --- what the machines ask (VK per second while switched on) ---
    public static final int ZENDER = 4, TERMINAL = 6, BESTELBALIE = 8, POORTBUIS = 6, LUCHTBLAZER = 8, WATERDRAGER = 6, KORSTDUWER = 10,
            VUURKORF = 12;
    // --- what the two new sources give ---
    public static final int ENDERGUHRAD = 50, KOMEETKERN = 500;

    /** How far (blocks, straight line) a Vadsontvanger hears its Vadszender. */
    public static final int ZENDER_BEREIK = 32;
    /** How many Vadsontvangers one Vadszender feeds. */
    public static final int ZENDER_MAX = 8;

    /** How far (blocks) a Bestelbalie looks for Knutselmachines. */
    public static final int BALIE_BEREIK = 12;
    /** An order that makes no progress for this many ticks is given up (what is moved stays where it is). */
    public static final int BALIE_GEDULD = 2400;
    /** The most crafts one order may plan (all steps together). */
    public static final int BALIE_MAX_KEER = 512;

    /** How long (ticks) a Knabbelpoort-buis waits between two items, and how many items per jump. */
    public static final int POORTBUIS_TIK = 8, POORTBUIS_STAPEL = 8;

    /** What one jump of a Thuispoort costs: the base plus this much for every OTHER poort of its owner. */
    public static final int THUIS_BASIS = 200, THUIS_PER_POORT = 50;
    /** The battery of one Thuispoort (VK); the net charges it with what it has left over. */
    public static final long THUIS_BATTERIJ = 4000;
    /** Guhs on a lead within this many blocks of the player jump along. */
    public static final int THUIS_LIJN = 12;

    /** Luchtblazer: how far it blows (blocks) and how hard. */
    public static final int BLAZER_BEREIK = 8;
    /** Waterdrager: the farmland it waters (blocks from it, sideways) and how far down. */
    public static final int DRAGER_BEREIK = 4;
    /** Korstduwer: how many blocks it pushes at most, and the pause between two pushes (ticks). */
    public static final int DUWER_MAX = 6, DUWER_TIK = 30;
    /** Vuurkorf: how far it warms (blocks) and how long an item lies in its glow before it is baked (ticks). */
    public static final int KORF_BEREIK = 3, KORF_BAK = 100;

    /** The Werktuigguh's door: how close (blocks) a player must come for the door scene. */
    public static final int DEUR_AFSTAND = 7;
    /** The Komeetscherf: how close (blocks) a player must come to pick it up. */
    public static final double SCHERF_AFSTAND = 2.5;

    private T5Getallen() {
    }
}
