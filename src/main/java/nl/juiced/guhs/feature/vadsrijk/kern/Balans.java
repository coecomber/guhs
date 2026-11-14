package nl.juiced.guhs.feature.vadsrijk.kern;

/**
 * The Vadsrijk balance knobs of kern (balans 2026-11, second playtest): {@code mobs.schade} = a global multiplier on the hits of
 * normal zone mobs and elites (bosses are not touched), default 2.028 (1102; 1.69 in 1.10.1, 1.3 in 1.10.0), and {@code mobs.leven}
 * on their max Vads, default 1.75 (1102; 1.4 in 1.10.1). Bosses + helpers: {@code bazen.leven} / {@code bazen.schade} (1102).
 * <p>g-int: there is ONE file {@code config/guhs-vadsrijk-balans.toml}, read and written by klassen's {@code KlasseBalans}
 * (section {@code [mobs]}, key {@code schade}, live reload + {@code /guhs vadsrijk balans mobs schade <x>}); it pushes the value
 * here. (g-balans' own ModConfigSpec on the same file name was dropped: NeoForge would have rewritten the file without the class
 * sections.)
 */
public final class Balans {
    public static final String BESTAND = "guhs-vadsrijk-balans.toml";
    /** 1102-balans: 1.69 x 1.2 = 2.028 (was 1.69 in 1.10.1, 1.3 in 1.10.0). The old defaults stay known for the config migration. */
    public static final double MOB_SCHADE_STANDAARD = 2.028;
    /** The 1.10.0 default (config versie 1). */
    public static final double MOB_SCHADE_OUD = 1.3;
    /** The 1.10.1 defaults (config versie 2). */
    public static final double MOB_SCHADE_V2 = 1.69, MOB_LEVEN_V2 = 1.4;
    /** 1102-balans: {@code [mobs] leven}, max Vads of normal zone mobs + elites, 1.4 x 1.25 = 1.75 (bosses not: see bazenLeven). */
    public static final double MOB_LEVEN_STANDAARD = 1.75;
    /**
     * 1102-balans: {@code [bazen] leven} / {@code [bazen] schade}: every boss (VadsrijkBaas: zone bosses, kerker end bosses, raid
     * bosses) and every boss helper (a Vadsrijk mob without a loot table) gets x leven on its max Vads (on top of the 1.10.1
     * main-stat compensation); bosses' hits x schade on the final damage (after Verdediging). Helpers already hit through
     * [mobs] schade, so they don't get [bazen] schade (no double count).
     */
    public static final double BAZEN_LEVEN_STANDAARD = 1.25, BAZEN_SCHADE_STANDAARD = 1.2;

    private static volatile double mobSchade = MOB_SCHADE_STANDAARD;
    private static volatile double mobLeven = MOB_LEVEN_STANDAARD;
    private static volatile double bazenLeven = BAZEN_LEVEN_STANDAARD;
    private static volatile double bazenSchade = BAZEN_SCHADE_STANDAARD;

    private Balans() {
    }

    /** From KernRegistry.register (kept for the call site; the file is KlasseBalans's). */
    static void registreer() {
    }

    /** 1102-balans: the boss/helper HP multiplier (default 1.25). */
    public static double bazenLeven() {
        return bazenLeven;
    }

    /** From KlasseBalans (file/command), clamped to 0.1..10. */
    public static void zetBazenLeven(double v) {
        bazenLeven = Math.max(0.1, Math.min(10.0, v));
    }

    /** 1102-balans: the boss final-damage multiplier (default 1.2). */
    public static double bazenSchade() {
        return bazenSchade;
    }

    /** From KlasseBalans (file/command), clamped to 0.1..5. */
    public static void zetBazenSchade(double v) {
        bazenSchade = Math.max(0.1, Math.min(5.0, v));
    }

    /** The mob HP multiplier (normal mobs + elites; default 1.75). */
    public static double mobLeven() {
        return mobLeven;
    }

    /** From KlasseBalans (file/command), clamped to 0.1..10. */
    public static void zetMobLeven(double v) {
        mobLeven = Math.max(0.1, Math.min(10.0, v));
    }

    /** The mob damage multiplier (default 2.028 until the balance file is read). */
    public static double mobSchade() {
        return mobSchade;
    }

    /** From KlasseBalans (file/command), clamped to 0.1..5. */
    public static void zetMobSchade(double v) {
        mobSchade = Math.max(0.1, Math.min(5.0, v));
    }
}
