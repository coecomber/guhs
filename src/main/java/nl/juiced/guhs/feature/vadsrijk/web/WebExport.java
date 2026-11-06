package nl.juiced.guhs.feature.vadsrijk.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.ToLongFunction;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.ArenaData;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.momenten.Karavaan;
import nl.juiced.guhs.feature.vadsrijk.momenten.KaravaanStaat;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.VahoegModus;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Contracten;

/**
 * (S-SITE) The live data of guhs.nl/vadsrijk: a small server-side service that writes JSON files into a folder that Caddy serves
 * as {@code live.guhs.nl} (Cloudflare-cached). The site only READS these files; nothing ever comes back in.
 * <ul>
 *   <li>{@code status.json} (every {@code status_s}): online, player count, names online, Guhs version, uptime, time stamp;</li>
 *   <li>{@code wereld.json} (every {@code wereld_s}): boss timers per arena, the caravan (phase, stop, route position as a 0..1
 *       fraction + its map x/z: the cart is a public world object, not a player), the Vahoeg week seal, today's contracts, the
 *       06:00 reset;</li>
 *   <li>{@code hiscores/<lijst>.json} (every {@code hiscores_s}): the whole list (small server), from {@link WebStats};</li>
 *   <li>{@code spelers/<naam>.json} + {@code spelers/index.json} (with the hiscores): every player's public profile.</li>
 * </ul>
 * <b>Privacy (user decision W1):</b> everything is public, but the export is an ALLOWLIST ({@link #snapshot}): never an IP, never a
 * position of a player, never chat, coins or trades; "last seen" is a date only.
 * <p><b>Threads:</b> the data is copied on the server thread into plain JSON objects; serialising and writing happen on one
 * background thread, each file atomically (write {@code .tmp}, then move/rename).
 * <p><b>Config:</b> {@code config/guhs-vadsrijk-web.properties}, written with the defaults on the first start: {@code aan=false}
 * (disabled by default), {@code map=web/live} (relative to the server folder, or absolute, e.g. {@code /opt/guhs/web/live}),
 * the intervals in seconds. Dev: {@code /guhs vadsrijk web nu} writes everything at once, {@code web wikiexport} the static dump.
 */
public final class WebExport {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String CONFIG = "guhs-vadsrijk-web.properties";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Pattern VEILIGE_NAAM = Pattern.compile("[A-Za-z0-9_]{1,16}");
    /** The lists on the site (the ids are also the file names). */
    public static final List<String> LIJSTEN = List.of("generiek", "knuffelridder", "pluisjager", "vadsmagier", "opgevrolijkt", "bazen",
            "verkenning");

    static boolean aan;
    /**
     * T-INT: extra hiscore lists of other slices (e.g. t-ambacht's {@code hiscores/ambacht_<id>.json}), written with the hiscores
     * (every {@code hiscores_s} and by {@code /guhs vadsrijk web nu}) via {@link #schrijf}, same format: {lijst, tijd, rijen[plek, naam,
     * waarde, xp?]}.
     */
    public static final List<java.util.function.Consumer<MinecraftServer>> EXTRA_HISCORES = new java.util.concurrent.CopyOnWriteArrayList<>();
    static Path map = Path.of("web", "live");
    static int statusS = 60, wereldS = 30, hiscoresS = 300, spelersS = 600;
    static int positiesS = 10;   // U-SITE: posities.json (live map)
    private static long start;
    private static int tick;
    @Nullable
    private static ExecutorService schrijver;

    private WebExport() {
    }

    // ---- config ------------------------------------------------------------------------------------------------------------

    static void laadConfig(MinecraftServer server) {
        Path f = server.getServerDirectory().resolve("config").resolve(CONFIG);
        Properties p = new Properties();
        p.setProperty("aan", "false");
        p.setProperty("map", "web/live");
        p.setProperty("status_s", "60");
        p.setProperty("wereld_s", "30");
        p.setProperty("hiscores_s", "300");
        p.setProperty("spelers_s", "600");
        p.setProperty("posities_s", "10");   // U-SITE
        try {
            if (Files.exists(f)) {
                try (InputStream in = Files.newInputStream(f)) {
                    p.load(in);
                }
            } else {
                Files.createDirectories(f.getParent());
                try (OutputStream out = Files.newOutputStream(f)) {
                    p.store(out, "Guhs Vadsrijk web export (guhs.nl/vadsrijk live data). aan=true to write JSON into 'map' (served as live.guhs.nl).");
                }
            }
        } catch (IOException e) {
            LOG.warn("[vadsrijk web] config {} not readable: {}", f, e.toString());
        }
        aan = Boolean.parseBoolean(p.getProperty("aan", "false").trim());
        Path m = Path.of(p.getProperty("map", "web/live").trim());
        map = m.isAbsolute() ? m : server.getServerDirectory().resolve(m);
        statusS = getal(p, "status_s", 60);
        wereldS = getal(p, "wereld_s", 30);
        hiscoresS = getal(p, "hiscores_s", 300);
        spelersS = getal(p, "spelers_s", 600);
        positiesS = getal(p, "posities_s", 10);   // U-SITE
        LOG.info("[vadsrijk web] export {} -> {}", aan ? "AAN" : "uit", map);
    }

    private static int getal(Properties p, String k, int standaard) {
        try {
            return Math.max(5, Integer.parseInt(p.getProperty(k, "" + standaard).trim()));
        } catch (NumberFormatException e) {
            return standaard;
        }
    }

    // ---- lifecycle ---------------------------------------------------------------------------------------------------------

    static void gestart(MinecraftServer server) {
        start = System.currentTimeMillis();
        tick = 0;
        laadConfig(server);
        if (aan) {
            schrijver = maakSchrijver();
            alles(server);
        }
    }

    private static ExecutorService maakSchrijver() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "guhs-vadsrijk-web");
            t.setDaemon(true);
            return t;
        });
    }

    /** Dev: write every live file now, also when the export is off (to look at the output). Returns the folder. */
    public static Path nu(MinecraftServer server) {
        if (schrijver == null) {
            laadConfig(server);
            schrijver = maakSchrijver();
        }
        alles(server);
        return map;
    }

    static void stopt(MinecraftServer server) {
        if (schrijver != null) {
            // the last status: offline
            JsonObject s = status(server);
            s.addProperty("online", false);
            s.add("spelers", new JsonArray());
            s.addProperty("aantal", 0);
            schrijf("status.json", s);
            schrijver.shutdown();
            try {
                schrijver.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS);   // (daemon thread: let the last files land)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            schrijver = null;
        }
    }

    /** Every server tick (Post). */
    static void tick(MinecraftServer server) {
        if (!aan || schrijver == null) {
            return;
        }
        tick++;
        if (tick % (statusS * 20) == 0) {
            schrijf("status.json", status(server));
        }
        if (tick % (wereldS * 20) == 0) {
            schrijf("wereld.json", wereld(server));
        }
        if (tick % (positiesS * 20) == 0) {   // U-SITE: the live map
            schrijf("posities.json", WebPosities.bouw(server));
        }
        if (tick % (spelersS * 20) == 0) {
            server.getPlayerList().getPlayers().forEach(p -> bewaar(server, p));
        }
        if (tick % (hiscoresS * 20) == 0) {
            hiscores(server);
        }
    }

    /** Everything at once (start, dev command). */
    public static void alles(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(p -> bewaar(server, p));
        schrijf("posities.json", WebPosities.bouw(server));   // U-SITE (first, so the profiles get "waar")
        schrijf("status.json", status(server));
        schrijf("wereld.json", wereld(server));
        hiscores(server);
    }

    static void bewaar(MinecraftServer server, ServerPlayer p) {
        WebStats.get(server).zet(p.getUUID(), snapshot(p));
    }

    // ---- the allowlist -----------------------------------------------------------------------------------------------------

    /** The ONLY player fields that ever leave the server. */
    public static CompoundTag snapshot(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        CompoundTag t = new CompoundTag();
        t.putString("naam", p.getName().getString());
        t.putString("uuid", p.getUUID().toString());   // (public anyway: for the skin head on the site)
        t.putInt("lv", d.level());
        t.putLong("xp", d.generiekXp);
        t.putString("klasse", d.klasse);
        CompoundTag kl = new CompoundTag();
        for (Klasse k : Klassen.alle()) {
            kl.putInt(k.id(), d.level(k.id()));
            kl.putLong(k.id() + "_xp", d.xp(k.id()));
        }
        t.put("klassen", kl);
        t.putInt("opgevrolijkt", d.opgevrolijkt);
        CompoundTag boek = new CompoundTag();
        int som = 0, n = 0;
        for (Voltooiing.ZoneBoek z : Voltooiing.zones()) {
            Voltooiing.Telling tel = Voltooiing.tel(p, z);
            boek.putInt(z.id(), tel.procent());
            som += tel.klaar();
            n += tel.totaal();
        }
        t.put("boek", boek);
        t.putInt("boek_totaal", n == 0 ? 0 : (int) Math.floor(som * 100.0 / n));
        t.putInt("huisdieren", Huisdieren.aantal(p));
        t.putInt("huisdieren_max", Huisdieren.alle().size());
        int rd = 0;
        for (RijdierSoort s : RijdierSoort.values()) {
            if (Rijdieren.heeft(p, s)) {
                rd++;
            }
        }
        t.putInt("rijdieren", rd);
        t.putInt("rijdieren_max", RijdierSoort.values().length);
        t.putString("titel", Spelers.titelKey(p));
        CompoundTag gear = new CompoundTag();
        gear(gear, "wapen", Spelers.wapen(p));
        for (Slot s : Slot.values()) {
            if (s == Slot.WAPEN) {
                continue;
            }
            ItemStack st = s.vanilla != null ? p.getItemBySlot(s.vanilla) : Sieraden.get(p, s);
            gear(gear, s.name().toLowerCase(Locale.ROOT), st == null || st.isEmpty() ? null : Rollen.van(st));
        }
        t.put("gear", gear);
        t.putString("gezien", LocalDate.now(ZoneId.of("Europe/Amsterdam")).toString());   // a DAY, never a time or a place
        if (d.level() >= 50) {
            t.putLong("eerste_50", System.currentTimeMillis());   // (WebStats keeps the first one)
        }
        return t;
    }

    private static void gear(CompoundTag gear, String slot, @Nullable Uitrusting u) {
        if (u == null) {
            return;
        }
        CompoundTag g = new CompoundTag();
        g.putString("type", u.type());
        g.putString("zeldzaamheid", u.zeldzaamheid().name().toLowerCase(Locale.ROOT));
        g.putInt("lv", u.level());
        g.putInt("sterren", u.sterren());
        g.putInt("flavour", u.flavour());
        g.putInt("sokjes", u.sokjes().size());
        gear.put(slot, g);
    }

    // ---- the files ---------------------------------------------------------------------------------------------------------

    static JsonObject status(MinecraftServer server) {
        JsonObject o = new JsonObject();
        o.addProperty("online", true);
        o.addProperty("tijd", Instant.now().toString());
        o.addProperty("versie", versie());
        o.addProperty("uptime_s", (System.currentTimeMillis() - start) / 1000);
        List<ServerPlayer> ps = server.getPlayerList().getPlayers();
        o.addProperty("aantal", ps.size());
        o.addProperty("max", server.getMaxPlayers());
        JsonArray namen = new JsonArray();
        int inRijk = 0;
        for (ServerPlayer p : ps) {
            namen.add(p.getName().getString());
            if (Vadsrijk.in(p)) {
                inRijk++;
            }
        }
        o.add("spelers", namen);
        o.addProperty("in_vadsrijk", inRijk);
        return o;
    }

    static JsonObject wereld(MinecraftServer server) {
        JsonObject o = new JsonObject();
        o.addProperty("tijd", Instant.now().toString());
        ZonedDateTime nu = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam"));
        ZonedDateTime reset = nu.toLocalDate().atTime(6, 0).atZone(nu.getZone());
        if (!reset.isAfter(nu)) {
            reset = reset.plusDays(1);
        }
        o.addProperty("dagreset", reset.toInstant().toString());
        o.addProperty("weekzegel", VahoegModus.zegel().key());
        JsonArray c = new JsonArray();
        try {
            Contracten.vandaag().forEach(id -> c.add(Contracten.naamKey(id)));
        } catch (RuntimeException e) {
            // (before zone 3's data is ready)
        }
        o.add("contracten", c);
        JsonArray bazen = new JsonArray();
        ServerLevel sl = Vadsrijk.level(server);
        long nuTick = sl == null ? 0 : sl.getGameTime();
        ArenaData ad = ArenaData.get(server);
        for (String id : Arenas.ids()) {
            Arena a = Arenas.van(id);
            if (a == null || id.startsWith("test")) {
                continue;
            }
            JsonObject b = new JsonObject();
            b.addProperty("arena", id);
            b.addProperty("titel", a.titelKey());
            long op = ad.respawnOp(id);
            long over = op < 0 ? 0 : Math.max(0, (op - nuTick) / 20);
            b.addProperty("terug_over_s", over);
            b.addProperty("staat", over > 0 ? "verslagen" : "wacht");
            b.addProperty("respawn_s", a.respawnSeconden());
            bazen.add(b);
        }
        o.add("bazen", bazen);
        if (sl != null) {
            KaravaanStaat k = Karavaan.staat(sl.getRandom());
            JsonObject kv = new JsonObject();
            kv.addProperty("fase", k.fase().name().toLowerCase(Locale.ROOT));
            kv.addProperty("stop", k.stop() == null ? "" : k.stop());
            kv.addProperty("richting", k.richting());
            kv.addProperty("route_fractie", Math.round(k.s() / Math.max(1, k.route().lengte()) * 1000) / 1000.0);
            Vec3 v = k.route().op(k.s());
            kv.addProperty("x", (int) Math.round(v.x));
            kv.addProperty("z", (int) Math.round(v.z));
            if (k.fase() == KaravaanStaat.Fase.STOP) {
                kv.addProperty("vertrekt_over_s", Math.max(0, k.timer() / 20));
            }
            o.add("karavaan", kv);
        }
        return o;
    }

    static void hiscores(MinecraftServer server) {
        Map<UUID, CompoundTag> alle = new LinkedHashMap<>(WebStats.get(server).spelers);
        List<CompoundTag> rijen = new ArrayList<>();
        alle.values().forEach(t -> {
            if (VEILIGE_NAAM.matcher(t.getStringOr("naam", "")).matches()) {
                rijen.add(t.copy());
            }
        });
        String tijd = Instant.now().toString();
        lijst("generiek", rijen, t -> t.getLongOr("xp", 0L) + t.getIntOr("lv", 0) * 0L, t -> t.getIntOr("lv", 0), tijd);
        for (Klasse k : Klassen.alle()) {
            lijst(k.id(), rijen, t -> t.getCompoundOrEmpty("klassen").getLongOr(k.id() + "_xp", 0L),
                    t -> t.getCompoundOrEmpty("klassen").getIntOr(k.id(), 0), tijd);
        }
        lijst("opgevrolijkt", rijen, t -> t.getIntOr("opgevrolijkt", 0), t -> t.getIntOr("opgevrolijkt", 0), tijd);
        lijst("bazen", rijen, WebExport::bazenTotaal, WebExport::bazenTotaal, tijd);
        lijst("verkenning", rijen, t -> t.getIntOr("boek_totaal", 0), t -> t.getIntOr("boek_totaal", 0), tijd);
        JsonArray index = new JsonArray();
        for (CompoundTag t : rijen) {
            String naam = t.getStringOr("naam", "");
            schrijf("spelers/" + naam.toLowerCase(Locale.ROOT) + ".json", profiel(t));
            JsonObject r = new JsonObject();
            r.addProperty("naam", naam);
            r.addProperty("lv", t.getIntOr("lv", 0));
            index.add(r);
        }
        JsonObject idx = new JsonObject();
        idx.addProperty("tijd", tijd);
        idx.add("spelers", index);
        schrijf("spelers/index.json", idx);
        for (java.util.function.Consumer<MinecraftServer> extra : EXTRA_HISCORES) {   // T-INT
            try {
                extra.accept(server);
            } catch (RuntimeException e) {
                LOG.warn("[vadsrijk web] extra hiscores: {}", e.toString());
            }
        }
    }

    /** T-INT: the live folder (config {@code map}). */
    public static Path map() {
        return map;
    }

    private static long bazenTotaal(CompoundTag t) {
        CompoundTag b = t.getCompoundOrEmpty("bazen");
        long n = 0;
        for (String k : b.keySet()) {
            n += b.getIntOr(k, 0);
        }
        return n;
    }

    /** One list: sorted by the score, ties by who got there first (the earliest "eerste_50", then the name). */
    private static void lijst(String id, List<CompoundTag> rijen, ToLongFunction<CompoundTag> score, ToLongFunction<CompoundTag> toon, String tijd) {
        List<CompoundTag> l = new ArrayList<>(rijen);
        l.sort(Comparator.<CompoundTag>comparingLong(score).reversed()
                .thenComparingLong(t -> t.getLongOr("eerste_50", Long.MAX_VALUE))
                .thenComparing(t -> t.getStringOr("naam", "")));
        JsonObject o = new JsonObject();
        o.addProperty("lijst", id);
        o.addProperty("tijd", tijd);
        JsonArray a = new JsonArray();
        int plek = 0;
        for (CompoundTag t : l) {
            long s = score.applyAsLong(t);
            if (s <= 0) {
                continue;
            }
            JsonObject r = new JsonObject();
            r.addProperty("plek", ++plek);
            r.addProperty("naam", t.getStringOr("naam", ""));
            r.addProperty("waarde", toon.applyAsLong(t));
            if (!id.equals("opgevrolijkt") && !id.equals("bazen") && !id.equals("verkenning")) {
                r.addProperty("xp", s);
            }
            a.add(r);
            if (plek >= 500) {
                break;
            }
        }
        o.add("rijen", a);
        schrijf("hiscores/" + id + ".json", o);
    }

    private static JsonObject profiel(CompoundTag t) {
        JsonObject o = new JsonObject();
        for (String k : List.of("naam", "uuid", "klasse", "titel", "gezien")) {
            o.addProperty(k, t.getStringOr(k, ""));
        }
        for (String k : List.of("lv", "opgevrolijkt", "boek_totaal", "huisdieren", "huisdieren_max", "rijdieren", "rijdieren_max")) {
            o.addProperty(k, t.getIntOr(k, 0));
        }
        o.addProperty("bazen_totaal", bazenTotaal(t));
        String waar = WebPosities.WAAR.get(t.getStringOr("naam", "").toLowerCase(Locale.ROOT));   // U-SITE: online + not hidden
        if (waar != null) {
            o.addProperty("waar", waar);
        }
        o.add("klassen", GSON.toJsonTree(map(t.getCompoundOrEmpty("klassen"), false)));
        o.add("boek", GSON.toJsonTree(map(t.getCompoundOrEmpty("boek"), false)));
        o.add("bazen", GSON.toJsonTree(map(t.getCompoundOrEmpty("bazen"), false)));
        JsonObject gear = new JsonObject();
        CompoundTag g = t.getCompoundOrEmpty("gear");
        for (String slot : g.keySet()) {
            CompoundTag s = g.getCompoundOrEmpty(slot);
            JsonObject j = new JsonObject();
            j.addProperty("type", s.getStringOr("type", ""));
            j.addProperty("zeldzaamheid", s.getStringOr("zeldzaamheid", "gewoon"));
            j.addProperty("lv", s.getIntOr("lv", 0));
            j.addProperty("sterren", s.getIntOr("sterren", 0));
            j.addProperty("sokjes", s.getIntOr("sokjes", 0));
            gear.add(slot, j);
        }
        o.add("gear", gear);
        long e50 = t.getLongOr("eerste_50", 0L);
        if (e50 > 0) {
            o.addProperty("eerste_50", LocalDateTime.ofInstant(Instant.ofEpochMilli(e50), ZoneId.of("Europe/Amsterdam")).toLocalDate().toString());
        }
        return o;
    }

    private static Map<String, Long> map(CompoundTag t, boolean metXp) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String k : t.keySet()) {
            if (!metXp && k.endsWith("_xp")) {
                continue;
            }
            m.put(k, t.getLongOr(k, 0L));
        }
        return m;
    }

    private static String versie() {
        return net.neoforged.fml.ModList.get().getModContainerById(nl.juiced.guhs.Guhs.MODID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("?");
    }

    // ---- writing (background thread, atomic) -------------------------------------------------------------------------------

    /** Writes one live file (atomic, on the writer thread); nothing while the export is off (T-INT: public for extra lists). */
    public static void schrijf(String naam, JsonObject o) {
        ExecutorService s = schrijver;
        if (s == null) {
            return;
        }
        Path doel = map.resolve(naam);
        s.execute(() -> {
            try {
                schrijfAtomisch(doel, GSON.toJson(o));
            } catch (IOException e) {
                LOG.warn("[vadsrijk web] {}: {}", doel, e.toString());
            }
        });
    }

    static void schrijfAtomisch(Path doel, String tekst) throws IOException {
        Files.createDirectories(doel.getParent());
        Path tmp = doel.resolveSibling(doel.getFileName() + ".tmp");
        Files.writeString(tmp, tekst, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, doel, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, doel, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
