package nl.juiced.guhs.feature.vadsrijk.klassen;

import nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;

/**
 * Fix round 1 (gevecht): the skill damage multipliers of the 3 classes in one place (units: the class's weapon damage per
 * second, {@code Spelers.wapenSchade}), and the sustained single-target DPS table the gametest checks
 * (target melee 100 / ranged 70 / magic 85, see NOTITIES_FIX_GEVECHT.md).
 */
public final class Balans {
    // Knuffelridder
    public static final double KUSSENKLAP = 1.5, STORMSPRONG = 1.3, VAHOEGE_PLOF = 3;
    // Pluisjager
    public static final double PLUIZENREGEN = 0.6, KNUFFELPIJL = 2.0, STERRENSCHOT = 4, SALVO = 0.7, PLAKBOM = 0.8;
    // Vadsmagier
    public static final double VUURBAL = 2.8, GLITTERBLINK = 1.2, METEOOR = 5;
    public static final int VUURBAL_CD = 90;

    private Balans() {
    }

    /**
     * Sustained single-target DPS units (1 = only basic attacks) of a class: basic + the damaging non-ult skills' multiplier per
     * second of their cooldown (energy and ult ignored; AoE counted once). Rolsalto = one guaranteed crit (+~0.55 of a bow
     * shot) per 8 s.
     */
    public static double eenheden(String klasse) {
        return switch (klasse) {
            case Klassen.JAGER -> 1 + 3 * PLUIZENREGEN / 6.0 + KNUFFELPIJL / 7.0 + 0.55 * 1.1 / 8.0;
            case Klassen.MAGIER -> 1 + VUURBAL / (VUURBAL_CD / 20.0);
            default -> 1 + KUSSENKLAP / 3.0 + STORMSPRONG / 8.0;
        };
    }

    /**
     * Expected single-target DPS of a class at a level with a Gewoon weapon of that level (no gear stats, no tree): weapon
     * rating x class factor x main stat x expected crit x element damage x {@link #eenheden}.
     */
    public static double dps(String klasse, String wapenType, int level) {
        Klasse k = Klassen.ofRidder(klasse);
        BasisType t = BasisTypes.van(wapenType);
        Stats s = k.basis(level).kopie().plus(t.statsOp(level));
        Schade.Soort soort = Aanvalstempo.schadeSoort(t);
        double hoofd = switch (soort) {
            case AFSTAND -> s.get(Stat.BEHENDIGHEID);
            case MAGIE -> s.get(Stat.WIJSHEID);
            default -> s.get(Stat.KRACHT);
        };
        double krit = Math.min(100, s.get(Stat.KRITIEK)) / 100.0 * (0.5 + s.get(Stat.KRITIEKSCHADE) / 100.0);
        double element = t.element() == nl.juiced.guhs.feature.vadsrijk.kern.Element.GEEN ? 1 : 1 + s.get(Stat.ELEMENTSCHADE) / 100.0;
        return t.schadeOp(level) * Aanvalstempo.klasseFactor(soort) * (1 + 0.02 * hoofd) * (1 + krit) * element * eenheden(klasse);
    }
}
