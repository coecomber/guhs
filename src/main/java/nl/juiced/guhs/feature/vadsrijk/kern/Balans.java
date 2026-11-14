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
    /**
     * 1.10.3: [bazen] leven 1.25 -> 1.9 (+50 % boss HP on top of 1.10.2; zone-2 bosses fell in seconds), then the schade-audit
     * (SCHADE_1103_AUDIT.md, config versie 5): 1.9 -> 3.4, together with the [gevecht] rendement below (geared lv-35 players did
     * ~10x the modelled damage; the Galmwolf died in 5-16 s).
     */
    public static final double BAZEN_LEVEN_STANDAARD = 3.4, BAZEN_SCHADE_STANDAARD = 1.2, BAZEN_LEVEN_V3 = 1.25, BAZEN_LEVEN_V4 = 1.9;

    /**
     * 1.10.3 schade-audit, {@code [gevecht]}: how much of a player's offensive stats counts ({@link Spelers#stats} applies it last,
     * so the character screen shows what really counts).
     * <ul>
     *   <li>{@code hoofd_rendement}: the share of Kracht/Behendigheid/Wijsheid ABOVE the balance model ({@link Schade#modelHoofd} of
     *   your class level) that counts (0.35: armour main stat grows 15 %/lv and a lv-35 mage reached 331 Wijsheid vs the model 126);</li>
     *   <li>{@code kritiekschade_rendement}, {@code elementschade_rendement}, {@code aanvalssnelheid_rendement}: the share of those %
     *   stats ABOVE your class's own base value that counts (gear + tree + spec + sets stacked to +125 % crit damage, +48 % speed);</li>
     *   <li>{@code kritiek_max}: Kritiek (crit chance %) is capped here.</li>
     * </ul>
     * Without gear (the balance model, klassen.Balans.dps) nothing changes.
     */
    public static final double HOOFD_RENDEMENT_STANDAARD = 0.35, PROCENT_RENDEMENT_STANDAARD = 0.4, KRITIEK_MAX_STANDAARD = 40;

    private static volatile double hoofdRendement = HOOFD_RENDEMENT_STANDAARD;
    private static volatile double kritiekschadeRendement = PROCENT_RENDEMENT_STANDAARD;
    private static volatile double elementschadeRendement = PROCENT_RENDEMENT_STANDAARD;
    private static volatile double aanvalssnelheidRendement = PROCENT_RENDEMENT_STANDAARD;
    private static volatile double kritiekMax = KRITIEK_MAX_STANDAARD;

    /** From KlasseBalans ([gevecht], file/command): the shares clamped to 0..1, the crit cap to 0..100. */
    public static void zetGevecht(double hoofd, double kritiekschade, double elementschade, double aanvalssnelheid, double kritMax) {
        hoofdRendement = Math.max(0, Math.min(1, hoofd));
        kritiekschadeRendement = Math.max(0, Math.min(1, kritiekschade));
        elementschadeRendement = Math.max(0, Math.min(1, elementschade));
        aanvalssnelheidRendement = Math.max(0, Math.min(1, aanvalssnelheid));
        kritiekMax = Math.max(0, Math.min(100, kritMax));
    }

    /** One stat after its rendement: {@code basis} counts in full, what is above it at {@code rendement}. Pure (tests). */
    public static double rendement(double waarde, double basis, double rendement) {
        return waarde <= basis ? waarde : basis + (waarde - basis) * rendement;
    }

    /**
     * The [gevecht] rendement on a player's finished stats (pure: tests and {@link Spelers#stats}). {@code klasseBasis} = the
     * class's own growth at its level (Klasse.basis), {@code klasseLevel} for the main-stat model.
     */
    public static void pasRendementToe(Stats s, Stats klasseBasis, int klasseLevel) {
        double model = Schade.modelHoofd(klasseLevel);
        for (Stat st : new Stat[] {Stat.KRACHT, Stat.BEHENDIGHEID, Stat.WIJSHEID}) {
            s.zet(st, rendement(s.get(st), model, hoofdRendement));
        }
        s.zet(Stat.KRITIEKSCHADE, rendement(s.get(Stat.KRITIEKSCHADE), klasseBasis.get(Stat.KRITIEKSCHADE), kritiekschadeRendement));
        s.zet(Stat.ELEMENTSCHADE, rendement(s.get(Stat.ELEMENTSCHADE), klasseBasis.get(Stat.ELEMENTSCHADE), elementschadeRendement));
        s.zet(Stat.AANVALSSNELHEID, rendement(s.get(Stat.AANVALSSNELHEID), klasseBasis.get(Stat.AANVALSSNELHEID), aanvalssnelheidRendement));
        s.zet(Stat.KRITIEK, Math.min(kritiekMax, s.get(Stat.KRITIEK)));
    }

    private static volatile double mobSchade = MOB_SCHADE_STANDAARD;
    private static volatile double mobLeven = MOB_LEVEN_STANDAARD;
    private static volatile double bazenLeven = BAZEN_LEVEN_STANDAARD;
    private static volatile double bazenSchade = BAZEN_SCHADE_STANDAARD;

    private Balans() {
    }

    /** From KernRegistry.register (kept for the call site; the file is KlasseBalans's). */
    static void registreer() {
    }

    /** 1102-balans: the boss/helper HP multiplier (default 3.4 since the 1.10.3 schade-audit). */
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
