package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;

/** All base types. Register them from your registry class's register() (kern: 3 weapons + 2 armour pieces). */
public final class BasisTypes {
    private static final Map<String, BasisType> ALLE = new LinkedHashMap<>();

    private BasisTypes() {
    }

    /** Armour Verdediging (base + growth) x this, for every armour base type of every slice (balans 2026-11: -25 %). */
    public static final double HARNAS_VERDEDIGING = 0.75;

    public static synchronized BasisType registreer(BasisType t) {
        if (t.slot().harnas()) {
            t = metMinderVerdediging(t);
        }
        ALLE.put(t.id(), t);
        return t;
    }

    /** Balans 2026-11: the Verdediging of an armour base type (base and per level) x {@link #HARNAS_VERDEDIGING}, one decimal. */
    static BasisType metMinderVerdediging(BasisType t) {
        Stats b = t.basis().kopie(), g = t.perLevel().kopie();
        schaal(b);
        schaal(g);
        return new BasisType(t.id(), t.itemId(), t.slot(), t.soort(), t.klasse(), t.element(), t.schade(), t.schadePerLevel(), b, g, t.flavours());
    }

    private static void schaal(Stats s) {
        double v = s.get(Stat.VERDEDIGING);
        if (v != 0) {
            s.plus(Stat.VERDEDIGING, Math.round(v * HARNAS_VERDEDIGING * 100) / 100.0 - v);
        }
    }

    @Nullable
    public static BasisType van(String id) {
        return ALLE.get(id);
    }

    public static Collection<BasisType> alle() {
        return Collections.unmodifiableCollection(ALLE.values());
    }
}
