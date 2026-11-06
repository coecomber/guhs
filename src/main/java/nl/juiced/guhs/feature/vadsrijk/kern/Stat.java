package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Locale;

/** Het Vadsrijk: the compact stats (ronde 13). Main stats first, then the side stats that rolls can give. */
public enum Stat {
    VADS(false), KRACHT(false), BEHENDIGHEID(false), WIJSHEID(false), VERDEDIGING(false),
    KRITIEK(true), KRITIEKSCHADE(true), AANVALSSNELHEID(true), LOOPSNELHEID(true), COOLDOWNREDUCTIE(true),
    ELEMENTSCHADE(true), ELEMENTWEERSTAND(true), LEVENSROOF(true), HEALKRACHT(true),
    // T-ITEMS (itemisatie 2.0, R4: clear names): "Verdediging negeren %", "Schade tegen bazen %", "Leven per treffer" (flat Vads),
    // "Kans op effect %". The combat side lives in feature/vadsrijk/items/NieuweStats.
    VERDEDIGING_NEGEREN(true), BAZENSCHADE(true), LEVEN_PER_TREFFER(false), KANS_OP_EFFECT(true);

    /** Shown as a percentage (the value is in percent points: 5 = 5 %). */
    public final boolean procent;

    Stat(boolean procent) {
        this.procent = procent;
    }

    public boolean hoofd() {
        return ordinal() <= VERDEDIGING.ordinal();
    }

    public String key() {
        return "vadsrijk.kern.stat." + name().toLowerCase(Locale.ROOT);
    }

    public static Stat van(String naam) {
        return valueOf(naam.toUpperCase(Locale.ROOT));
    }
}
