package nl.juiced.guhs.feature.vadsrijk.kern.klasse;

import java.util.List;
import java.util.Set;

import nl.juiced.guhs.feature.vadsrijk.kern.Stats;

/**
 * A class (FF14 model: each class has its own level; switch at the ordetempel). Register with {@link Klassen#registreer}.
 * Skills: exactly 4, in the order R, V, G, Z (Z = the ult).
 */
public interface Klasse {
    String id();

    default String naamKey() {
        return "vadsrijk.klassen.klasse." + id();
    }

    /** The class's own stats at this level (automatic growth; gear, tree and statuses come on top). */
    Stats basis(int level);

    /** R, V, G, Z. */
    List<Vaardigheid> vaardigheden();

    /** The skill tree (~12 nodes); empty until the klassen slice fills it. */
    default List<BoomKnoop> boom() {
        return List.of();
    }

    /** Weapon kinds this class can use (ids of {@code kern.loot.BasisType#soort}). */
    Set<String> wapenSoorten();

    /** Max Vadsenergie at this level. */
    default double maxEnergie(int level) {
        return 100 + 2 * (level - 1);
    }

    /** Energy regained per second. */
    default double energieRegen(int level) {
        return 6;
    }

    /** The job title at this level (job advancement at 10 and 20 is klassen-slice logic). */
    default String titelKey(int level) {
        return naamKey();
    }
}
