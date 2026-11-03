package nl.juiced.guhs.feature.vadsrijk.kern.mob;

import java.util.Locale;

/**
 * Elite affixes (ronde 13). Kern applies GLOEIEND (its hits burn), SNEL (+40 % speed) and SCHILDEND (a Knuffelschild every 10 s);
 * SPLITSEND is for the mob class (wezens: override {@link VadsrijkMob#opPlof} to split).
 */
public enum Affix {
    GLOEIEND, SNEL, SPLITSEND, SCHILDEND;

    public String key() {
        return "vadsrijk.kern.affix." + name().toLowerCase(Locale.ROOT);
    }
}
