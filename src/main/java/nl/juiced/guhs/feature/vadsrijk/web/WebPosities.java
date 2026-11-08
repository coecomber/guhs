package nl.juiced.guhs.feature.vadsrijk.web;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.gilden.Vallei;
import nl.juiced.guhs.feature.vadsrijk.huizen.Kwartier;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kleding.Zweven;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Dienstregeling;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Lijnen;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Tijd;

/**
 * U-SITE: live player positions for the guhs.nl/vadsrijk map ({@code posities.json}, every {@code posities_s} = 10 s, only while
 * the export is on). User decision 2026-11-06 (replaces W1's "never a live position"): ON by default with an opt-out
 * ({@code /guhs vadsrijk kaart verberg|toon}, stored in {@link WebStats}).
 * <ul>
 *   <li>{@code spelers}: players in the Vadsrijk dimension who did not opt out and are not in a hidden place: name, x, z, class id,
 *   level, area id. Never an IP, a uuid or anything else.</li>
 *   <li>{@code status}: players in a hidden place (dungeon/boss arena, a Knuffelkwartier house, the Bondenvallei): name + a status
 *   text, NO coordinates. Opted-out players appear nowhere.</li>
 *   <li>{@code schepen}: airships that are flying right now (line letter, ship 0/1, x, z, yaw): public world objects.</li>
 * </ul>
 * The site's profile page gets the same status as {@code waar} (see {@link #WAAR}).
 */
public final class WebPosities {
    private static final Pattern VEILIGE_NAAM = Pattern.compile("[A-Za-z0-9_]{1,16}");
    /** Other slices: more hidden places (return a status text like "in een kerker", or null). The raid can hook in here. */
    public static final List<Function<ServerPlayer, String>> VERBORGEN_PLEKKEN = new CopyOnWriteArrayList<>();
    /** name (lower case) -> the last status text of an online player (for spelers/<naam>.json "waar"). */
    static final Map<String, String> WAAR = new ConcurrentHashMap<>();

    public static final String KERKER = "in een kerker", HUISJE = "in een huisje", VALLEI = "in de Bondenvallei", RIJK = "in het Vadsrijk";
    /** U-SITE: a player in the raid tower (zone 6, hooked in by WebRegistry). */
    public static final String RAID = "in de raid";

    /** U-SITE: inside z6wezens' underground dungeon rooms (De Verzonken Sterrenwacht)? */
    public static boolean inZ6Kerker(double x, double y, double z) {
        return x >= 3936 && x < 4066 && z >= -988 && z < -954 && y >= 24 && y < 50;
    }

    /** The harbour zone code of Lijnen.Haven -> the town the ship flies to. */
    private static final Map<String, String> STAD = Map.of("kamp", "knuffelkamp", "z1", "zwamzoet", "z2", "kandijkom", "z3", "honingtak",
            "z4", "lampionhaven", "z5", "hoogvads", "z6", "rozenkloof");   // U-SITE: + line E

    private WebPosities() {
    }

    /** zone1..zone5 of a kern area id (z2_/z3_/z4_/z5_/z5d_ prefix; the rest is zone 1). */
    static String zoneVan(String gebied) {
        java.util.regex.Matcher m = Pattern.compile("^z([2-9])[a-z]?_").matcher(gebied);
        return m.find() ? "zone" + m.group(1) : "zone1";
    }

    /** Hidden place of a player (status text) or null when the position may be shown. Only for players in the Vadsrijk/Kwartier. */
    @Nullable
    public static String verborgenPlek(ServerPlayer p) {
        if (Kwartier.in(p)) {
            return HUISJE;
        }
        double x = p.getX(), y = p.getY(), z = p.getZ();
        if (Vallei.in(x, z)) {
            return VALLEI;
        }
        if (Arenas.op(x, y, z) != null) {
            return KERKER;
        }
        Gebieden.Gebied g = Gebieden.op(x, y, z);
        if (g != null && Zweven.GEEN_ZWEEF_GEBIEDEN.contains(g.id())) {   // the dungeons (Spiegelburcht, Werf, Hart, Wrak, Buikgangen...)
            return KERKER;
        }
        for (Function<ServerPlayer, String> f : VERBORGEN_PLEKKEN) {
            String s = f.apply(p);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    /** posities.json for these players; {@code inRijk} decides who counts as "in the Vadsrijk" (tests: everyone). */
    static JsonObject bouw(MinecraftServer server, List<ServerPlayer> spelers, Predicate<ServerPlayer> inRijk) {
        return bouw(server, spelers, inRijk, p -> p.getName().getString());
    }

    /** (tests: {@code naamVan} gives the mock players real-looking names) */
    static JsonObject bouw(MinecraftServer server, List<ServerPlayer> spelers, Predicate<ServerPlayer> inRijk, Function<ServerPlayer, String> naamVan) {
        WebStats st = WebStats.get(server);
        JsonObject o = new JsonObject();
        o.addProperty("tijd", System.currentTimeMillis());   // epoch ms (the site's format)
        o.addProperty("interval_s", WebExport.positiesS);
        JsonArray zichtbaar = new JsonArray(), status = new JsonArray();
        Map<String, String> waar = new java.util.HashMap<>();
        for (ServerPlayer p : spelers) {
            String naam = naamVan.apply(p);
            if (!VEILIGE_NAAM.matcher(naam).matches() || st.verborgen(p.getUUID())) {
                continue;   // opted out: nowhere, not even a status
            }
            boolean rijk = inRijk.test(p);
            if (!rijk && !Kwartier.in(p)) {
                continue;
            }
            String plek = verborgenPlek(p);
            if (plek != null) {
                JsonObject s = new JsonObject();
                s.addProperty("naam", naam);
                s.addProperty("waar", plek);
                status.add(s);
                waar.put(naam.toLowerCase(java.util.Locale.ROOT), plek);
                continue;
            }
            SpelerData d = Spelers.data(p);
            JsonObject r = new JsonObject();
            r.addProperty("naam", naam);
            r.addProperty("x", (int) Math.round(p.getX()));
            r.addProperty("z", (int) Math.round(p.getZ()));
            r.addProperty("klasse", d.klasse == null ? "" : d.klasse);
            r.addProperty("level", d.level());
            Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
            r.addProperty("zone", g == null ? "" : zoneVan(g.id()));
            if (g != null) {
                r.addProperty("gebied", g.id());
            }
            zichtbaar.add(r);
            waar.put(naam.toLowerCase(java.util.Locale.ROOT), RIJK);
        }
        o.add("spelers", zichtbaar);
        o.add("status", status);
        o.add("schepen", schepen(server));
        WAAR.clear();
        WAAR.putAll(waar);
        return o;
    }

    static JsonObject bouw(MinecraftServer server) {
        return bouw(server, server.getPlayerList().getPlayers(), Vadsrijk::in);
    }

    /** The airships that are in the air right now (both ships of every real line). */
    static JsonArray schepen(MinecraftServer server) {
        JsonArray a = new JsonArray();
        ServerLevel sl;
        try {
            sl = Vadsrijk.level(server);
        } catch (RuntimeException e) {
            sl = null;
        }
        if (sl == null) {
            return a;
        }
        for (Lijnen.Lijn l : Lijnen.ALLE) {
            if (l.test() || !l.actief()) {
                continue;
            }
            double t = Tijd.nu(sl, l);
            for (int k = 0; k < 2; k++) {
                Dienstregeling.Stand s = Dienstregeling.stand(l, k, t);
                if (!s.fase().vliegt()) {
                    continue;
                }
                JsonObject j = new JsonObject();
                j.addProperty("id", "lijn_" + l.letter().toLowerCase(java.util.Locale.ROOT) + (k == 1 ? "_2" : ""));
                j.addProperty("lijn", l.letter());
                j.addProperty("x", (int) Math.round(s.pos().x));
                j.addProperty("z", (int) Math.round(s.pos().z));
                j.addProperty("richting", Math.round(s.yaw()));
                j.addProperty("naar", STAD.getOrDefault((s.fase() == Dienstregeling.Fase.VLIEGT_AB ? l.b() : l.a()).zone(), ""));
                a.add(j);
            }
        }
        return a;
    }
}
