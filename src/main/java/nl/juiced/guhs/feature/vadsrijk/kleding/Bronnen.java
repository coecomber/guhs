package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;

/**
 * T-KLEDING: where cosmetics come from. Unlocks are EARNED, never bought. Two ways in:
 * <ul>
 *   <li><b>checked</b> every {@link KledingRegistry#CHECK} ticks for players in the Vadsrijk ({@link #check}): "start", "binnen",
 *   "vlag:&lt;story flag&gt;" (npc Questlog flags: zone1_klaar, zone2_klaar, zone3_klaar), "boek:&lt;zone&gt;:&lt;pct&gt;"
 *   (Verkenningsboek percentage), "level:&lt;n&gt;" (current class level), "veren:&lt;n&gt;" (Gouden Veren collected: the counter, or
 *   already traded for the gold Pluimguh), "kills:&lt;n&gt;" (account-wide cheer-ups), "alleboeken" (every registered zone 100 %),
 *   and "verhaal:vahoegnir" also falls back on the zone3_klaar flag (old saves);</li>
 *   <li><b>given</b> directly by a hook: "verhaal:vahoegnir" (z3-bazen moment), "vahoeg" (a rare Vahoeg boss drop), "vahoegkill"
 *   (the first Vahoeg boss), "geheim:&lt;id&gt;" ({@link Kledingkast#geheim}).</li>
 * </ul>
 * "later:..." never unlocks (a zone that isn't built yet; the screen says "komt later").
 */
public final class Bronnen {
    private Bronnen() {
    }

    /** One check pass: every cosmetic whose checked source is met is unlocked (with a message). Returns how many were new. */
    public static int check(ServerPlayer p) {
        Map<String, Integer> boek = new HashMap<>();
        int nieuw = 0;
        for (Cosmetica.Ding d : Cosmetica.alle()) {
            if (Kledingkast.heeft(p, d.id())) {
                continue;
            }
            if (gehaald(p, d.bron(), boek)) {
                if (Kledingkast.ontgrendel(p, d.id(), true)) {
                    nieuw++;
                }
            }
        }
        return nieuw;
    }

    /** Is this (checked) source met? Direct sources (hooks) answer false here. */
    static boolean gehaald(ServerPlayer p, String bron, Map<String, Integer> boekCache) {
        try {
            if (bron.equals("start")) {
                return true;
            }
            if (bron.equals("binnen")) {
                return nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.in(p) || !Spelers.data(p).xp.isEmpty();
            }
            if (bron.equals("verhaal:vahoegnir")) {
                return Questlog.vlag(p, "zone3_klaar");
            }
            String[] t = bron.split(":");
            switch (t[0]) {
                case "vlag":
                    return Questlog.vlag(p, t[1]);
                case "boek":
                    return procent(p, t[1], boekCache) >= Integer.parseInt(t[2]);
                case "level":
                    return Spelers.level(p) >= Integer.parseInt(t[1]);
                case "veren":
                    return nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren.aantal(p) >= Integer.parseInt(t[1])
                            || nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren.heeftGoud(p);
                case "kills":
                    return Kledingkast.kills(p) >= Integer.parseInt(t[1]);
                case "alleboeken": {
                    var zones = Voltooiing.zones();
                    if (zones.isEmpty()) {
                        return false;
                    }
                    for (Voltooiing.ZoneBoek z : zones) {
                        if (procent(p, z.id(), boekCache) < 100) {
                            return false;
                        }
                    }
                    return true;
                }
                default:
                    return false;
            }
        } catch (RuntimeException e) {
            return false;   // (a slice that isn't there or broke never crashes the wardrobe)
        }
    }

    /** The Verkenningsboek percentage of a zone (-1 when that zone has no book yet), cached for one pass. */
    static int procent(ServerPlayer p, String zone, Map<String, Integer> cache) {
        return cache.computeIfAbsent(zone, z -> {
            Voltooiing.ZoneBoek b = Voltooiing.zone(z);
            if (b == null) {
                return -1;
            }
            Voltooiing.Telling t = Voltooiing.tel(p, b);
            return t.klaar() == 0 ? 0 : t.procent();
        });
    }
}
