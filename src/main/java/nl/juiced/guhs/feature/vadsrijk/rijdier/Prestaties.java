package nl.juiced.guhs.feature.vadsrijk.rijdier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;

/**
 * T-RIJDIER: the conditions behind the "prestatie" and "geheim" sources of mount looks and mounts ({@link Uiterlijk.Bron}). Each id
 * maps to a check; the built-in ones are parsed from the id:
 * <ul>
 *   <li>{@code rijdiermeester}: any mount at level 10; {@code lv10_<soort>}: that mount at level 10;</li>
 *   <li>{@code huisdieren_<n>}: at least n pets;</li>
 *   <li>{@code verkenner_<zone>_<procent>}: the Verkenningsboek page of that zone (zone1, zone2, ...) at least that far;</li>
 *   <li>{@code knuffels_<n>}, {@code schatten_<n>}, {@code aanwijzingen_<n>}: that many hidden knuffels / treasures / clues found in
 *       all zones together (the npc maps of zone 1, 2 and 3; {@link #GEHEIM_MAPPEN} for later zones).</li>
 * </ul>
 * Other slices can add their own: {@link #registreer}.
 */
public final class Prestaties {
    private static final Map<String, Predicate<ServerPlayer>> EIGEN = new ConcurrentHashMap<>();
    /** The npc "found" maps per kind of secret (zone 1, 2, 3; a later zone adds its own map names). */
    public static final Map<String, java.util.List<String>> GEHEIM_MAPPEN = new ConcurrentHashMap<>(Map.of(
            "knuffels", new java.util.concurrent.CopyOnWriteArrayList<>(java.util.List.of("knuffels", "z2_knuffels", "z3_knuffels")),
            "schatten", new java.util.concurrent.CopyOnWriteArrayList<>(java.util.List.of("schatten", "z2_schatten", "z3_schatten")),
            "aanwijzingen", new java.util.concurrent.CopyOnWriteArrayList<>(java.util.List.of("aanwijzingen", "z2_aanwijzingen", "z3_aanwijzingen"))));

    private Prestaties() {
    }

    public static void registreer(String id, Predicate<ServerPlayer> test) {
        EIGEN.put(id, test);
    }

    /** True when the player meets the condition (unknown ids: false). Never throws. */
    public static boolean gehaald(ServerPlayer p, String id) {
        try {
            Predicate<ServerPlayer> e = EIGEN.get(id);
            if (e != null) {
                return e.test(p);
            }
            if (id.equals("rijdiermeester")) {
                for (RijdierSoort s : RijdierSoort.values()) {
                    if (Rijdieren.heeft(p, s) && Rijdieren.level(p, s) >= RijdierSoort.MAX_LEVEL) {
                        return true;
                    }
                }
                return false;
            }
            if (id.startsWith("lv10_")) {
                RijdierSoort s = RijdierSoort.van(id.substring(5));
                return s != null && Rijdieren.heeft(p, s) && Rijdieren.level(p, s) >= RijdierSoort.MAX_LEVEL;
            }
            int n = getal(id);
            if (id.startsWith("huisdieren_")) {
                return Huisdieren.aantal(p) >= n;
            }
            if (id.startsWith("verkenner_")) {
                String[] d = id.split("_");
                Voltooiing.ZoneBoek z = Voltooiing.zone(d[1]);
                return z != null && Voltooiing.tel(p, z).procent() >= n;
            }
            int u = id.lastIndexOf('_');
            if (u > 0 && GEHEIM_MAPPEN.containsKey(id.substring(0, u))) {
                return geheimen(p, id.substring(0, u)) >= n;
            }
        } catch (RuntimeException e) {
            return false;
        }
        return false;
    }

    /** How many secrets of a kind ("knuffels", "schatten", "aanwijzingen") the player found in all zones. */
    public static int geheimen(ServerPlayer p, String soort) {
        int n = 0;
        for (String map : GEHEIM_MAPPEN.getOrDefault(soort, java.util.List.of())) {
            CompoundTag t = Questlog.map(p, map);
            for (String k : t.keySet()) {
                if (t.getBooleanOr(k, false)) {
                    n++;
                }
            }
        }
        return n;
    }

    /** The first-kill key of a boss type is set (kern). */
    public static boolean baasVerslagen(ServerPlayer p, String type) {
        return Spelers.data(p).gezien.contains("baaskill:" + type);
    }

    /** Progress text for the screen: {have, need} (need 0 = not countable). */
    public static int[] voortgang(ServerPlayer p, String id) {
        int n = getal(id);
        int u = id.lastIndexOf('_');
        if (u > 0 && GEHEIM_MAPPEN.containsKey(id.substring(0, u))) {
            return new int[] {geheimen(p, id.substring(0, u)), n};
        }
        if (id.startsWith("huisdieren_")) {
            return new int[] {Huisdieren.aantal(p), n};
        }
        return new int[] {0, 0};
    }

    private static int getal(String id) {
        int u = id.lastIndexOf('_');
        try {
            return u < 0 ? 0 : Integer.parseInt(id.substring(u + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
