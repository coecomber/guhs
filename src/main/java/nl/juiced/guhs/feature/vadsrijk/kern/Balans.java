package nl.juiced.guhs.feature.vadsrijk.kern;

/**
 * The Vadsrijk balance knobs of kern (balans 2026-11, second playtest): {@code mobs.schade} = a global multiplier on the hits of
 * normal zone mobs and elites (bosses are not touched), default 1.69 (1101-balans; was 1.3), and {@code mobs.leven} on their max
 * Vads, default 1.4 (1101-balans).
 * <p>g-int: there is ONE file {@code config/guhs-vadsrijk-balans.toml}, read and written by klassen's {@code KlasseBalans}
 * (section {@code [mobs]}, key {@code schade}, live reload + {@code /guhs vadsrijk balans mobs schade <x>}); it pushes the value
 * here. (g-balans' own ModConfigSpec on the same file name was dropped: NeoForge would have rewritten the file without the class
 * sections.)
 */
public final class Balans {
    public static final String BESTAND = "guhs-vadsrijk-balans.toml";
    /** 1101-balans: 1.3 -> 1.69 (+30 % on top). The old default stays known for the config migration. */
    public static final double MOB_SCHADE_STANDAARD = 1.69;
    public static final double MOB_SCHADE_OUD = 1.3;
    /** 1101-balans: {@code [mobs] leven}, max Vads of normal zone mobs + elites x1.4 (bosses not). */
    public static final double MOB_LEVEN_STANDAARD = 1.4;

    private static volatile double mobSchade = MOB_SCHADE_STANDAARD;
    private static volatile double mobLeven = MOB_LEVEN_STANDAARD;

    private Balans() {
    }

    /** From KernRegistry.register (kept for the call site; the file is KlasseBalans's). */
    static void registreer() {
    }

    /** The mob HP multiplier (normal mobs + elites; default 1.4). */
    public static double mobLeven() {
        return mobLeven;
    }

    /** From KlasseBalans (file/command), clamped to 0.1..10. */
    public static void zetMobLeven(double v) {
        mobLeven = Math.max(0.1, Math.min(10.0, v));
    }

    /** The mob damage multiplier (default 1.69 until the balance file is read). */
    public static double mobSchade() {
        return mobSchade;
    }

    /** From KlasseBalans (file/command), clamped to 0.1..5. */
    public static void zetMobSchade(double v) {
        mobSchade = Math.max(0.1, Math.min(5.0, v));
    }
}
