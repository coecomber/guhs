package nl.juiced.guhs.feature.vadsrijk.kern;

/**
 * The Vadsrijk balance knobs of kern (balans 2026-11, second playtest): {@code mobs.schade} = a global multiplier on the hits of
 * normal zone mobs and elites (bosses are not touched), default 1.3 (+30 %).
 * <p>g-int: there is ONE file {@code config/guhs-vadsrijk-balans.toml}, read and written by klassen's {@code KlasseBalans}
 * (section {@code [mobs]}, key {@code schade}, live reload + {@code /guhs vadsrijk balans mobs schade <x>}); it pushes the value
 * here. (g-balans' own ModConfigSpec on the same file name was dropped: NeoForge would have rewritten the file without the class
 * sections.)
 */
public final class Balans {
    public static final String BESTAND = "guhs-vadsrijk-balans.toml";
    public static final double MOB_SCHADE_STANDAARD = 1.3;

    private static volatile double mobSchade = MOB_SCHADE_STANDAARD;

    private Balans() {
    }

    /** From KernRegistry.register (kept for the call site; the file is KlasseBalans's). */
    static void registreer() {
    }

    /** The mob damage multiplier (default 1.3 until the balance file is read). */
    public static double mobSchade() {
        return mobSchade;
    }

    /** From KlasseBalans (file/command), clamped to 0.1..5. */
    public static void zetMobSchade(double v) {
        mobSchade = Math.max(0.1, Math.min(5.0, v));
    }
}
