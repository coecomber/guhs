package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * Attack speed as a weapon stat (fix round 1, "gevecht"). Every weapon kind has a base interval in seconds (a dagger 0.7 s, a
 * heavy hammer 3.5 s, a bow 1.1 s...), a base type may override it ({@link #TYPE}); Aanvalssnelheid% makes it shorter.
 * <p>Damage: a weapon's {@code schade + perLevel x level} is its <b>damage per second</b> rating (before the main stat). One basic
 * hit does {@code rating x class factor x interval}: slow weapons hit big, fast ones small, the DPS stays the same. The class
 * factor sets the DPS ratio melee 100 / ranged 70 / magic 85 (corrected for the classes' own crit/element stats, see
 * NOTITIES_FIX_GEVECHT.md). Skills use {@code rating x class factor} (= {@link Spelers#wapenSchade}) times their multiplier.
 */
public final class Aanvalstempo {
    /** Base seconds per basic attack, per weapon kind. */
    public static final Map<String, Double> SOORT = new HashMap<>(Map.ofEntries(Map.entry("dolk", 0.7), Map.entry("kling", 0.7),
            Map.entry("zwaard", 1.0), Map.entry("knots", 1.4), Map.entry("hamer", 2.4), Map.entry("boog", 1.1), Map.entry("kruisboog", 2.0),
            Map.entry("katapult", 1.6), Map.entry("toverstok", 0.9), Map.entry("staf", 1.3), Map.entry("boek", 1.6)));
    /** Per base type id (wins over the kind): e.g. the heavy pluishamer, the quick klodderkling. */
    public static final Map<String, Double> TYPE = new HashMap<>(Map.of("pluishamer", 3.5, "klodderkling", 0.7));
    /** Without a class weapon: a weak punch. */
    public static final double VUIST = 0.6;
    /** Ranged/magic weapon kinds (the rest is melee). */
    public static final Map<String, Schade.Soort> AFSTAND = new HashMap<>(Map.of("boog", Schade.Soort.AFSTAND, "kruisboog", Schade.Soort.AFSTAND,
            "katapult", Schade.Soort.AFSTAND, "toverstok", Schade.Soort.MAGIE, "staf", Schade.Soort.MAGIE, "boek", Schade.Soort.MAGIE));
    /** DPS share per damage kind: 70 % and 85 % of melee, divided by the ranged/magic classes' extra crit/element value (~0.967 / ~0.93). */
    public static final double FACTOR_AFSTAND = 0.68, FACTOR_MAGIE = 0.79;

    private Aanvalstempo() {
    }

    /** The base interval of this base type in seconds (without Aanvalssnelheid). */
    public static double seconden(@Nullable BasisType t) {
        if (t == null) {
            return VUIST;
        }
        Double s = TYPE.get(t.id());
        if (s == null) {
            s = SOORT.get(t.soort());
        }
        return s == null ? 1.0 : s;
    }

    public static Schade.Soort schadeSoort(@Nullable BasisType t) {
        return t == null ? Schade.Soort.MELEE : AFSTAND.getOrDefault(t.soort(), Schade.Soort.MELEE);
    }

    public static double klasseFactor(Schade.Soort s) {
        return switch (s) {
            case AFSTAND -> FACTOR_AFSTAND;
            case MAGIE -> FACTOR_MAGIE;
            default -> 1.0;
        };
    }

    public static double klasseFactor(@Nullable BasisType t) {
        return klasseFactor(schadeSoort(t));
    }

    /** Aanvalssnelheid% (clamped -50..150) as a speed factor. */
    public static double snelheid(double aanvalssnelheid) {
        return 1 + Math.max(-50, Math.min(150, aanvalssnelheid)) / 100.0;
    }

    /** This player's interval now in seconds (the held class weapon, or the punch), with an Aanvalssnelheid value. */
    public static double seconden(Player p, double aanvalssnelheid) {
        Uitrusting u = Spelers.wapen(p);
        return seconden(u == null ? null : u.basisType()) / snelheid(aanvalssnelheid);
    }

    /** Ticks between two basic attacks (at least 2). */
    public static int ticks(double seconden) {
        return Math.max(2, (int) Math.round(seconden * 20));
    }

    /** "1,1 s" for tooltips. */
    public static String tekst(double seconden) {
        return String.format(Locale.ROOT, "%.1f", seconden).replace('.', ',');
    }
}
