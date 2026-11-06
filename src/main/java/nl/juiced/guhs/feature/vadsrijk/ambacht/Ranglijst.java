package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import net.minecraft.util.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The Ambachten rankings (A2: "een Ambachtsbord in de hubs + de website"). SavedData {@code guhs:vr_ambacht_ranglijst} keeps every
 * player's ambacht xp (updated on every xp change, so offline players stay on the list) and when they reached their current level
 * (ties: who was first, OSRS-style). Never a chat announcement (ronde 18 spirit): only the board and the website.
 * <p>Website export (VOORSTEL_WEBSITE §5.2 "hiscores/&lt;lijst&gt;.json", the s-site slice reads these): every 5 min, at logout and on
 * {@code /guhs vadsrijk ambacht export}, written atomically (.tmp then rename, off the server thread) to
 * {@code <server>/guhs-web/live/hiscores/} (override: system property {@code guhs.web.map}). Files: {@code ambacht_<id>.json} per
 * ambacht + {@code ambacht_totaal.json}:
 * <pre>
 * {"lijst":"ambacht_mijnen","naam":"Mijnen","bijgewerkt":"2026-11-06T03:00:00Z","cap":50,
 *  "spelers":[{"plek":1,"naam":"Juiced","uuid":"...","level":42,"xp":123456,"sinds":1762390000000}, ...]}
 * </pre>
 */
public final class Ranglijst extends SavedData {
    private static final Logger LOG = LogUtils.getLogger();
    public static final SavedDataType<Ranglijst> TYPE = GuhSavedData.tagType("vr_ambacht_ranglijst", Ranglijst::new, Ranglijst::load, Ranglijst::save);
    public static final String TOTAAL = "totaal";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile MinecraftServer server;

    /** One player's row: name, xp per ambacht, and when each level was reached (ms). */
    static final class Rij {
        String naam = "?";
        final Map<String, Long> xp = new HashMap<>();
        final Map<String, Long> sinds = new HashMap<>();
    }

    private final Map<UUID, Rij> rijen = new HashMap<>();

    public static Ranglijst get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    static void koppel(MinecraftServer s) {
        server = s;
    }

    /** Updates this player's row (after any xp change). */
    static void bij(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        if (s == null) {
            return;
        }
        Ranglijst r = get(s);
        Rij rij = r.rijen.computeIfAbsent(p.getUUID(), k -> new Rij());
        rij.naam = p.getGameProfile().name();
        long totaal = 0;
        for (Ambacht a : Ambacht.values()) {
            long xp = AmbachtData.xp(p, a);
            Long oud = rij.xp.put(a.id, xp);
            if (oud == null || AmbachtXp.levelVan(oud) != AmbachtXp.levelVan(xp)) {
                rij.sinds.put(a.id, AmbachtData.nu());
            }
            totaal += xp;
        }
        Long oudT = rij.xp.put(TOTAAL, totaal);
        if (oudT == null || oudT != totaal) {
            rij.sinds.put(TOTAAL, AmbachtData.nu());
        }
        r.setDirty();
    }

    /** One line of a list. */
    public record Plek(UUID uuid, String naam, int level, long xp, long sinds) {
    }

    /** The full ranking of an ambacht (or {@link #TOTAAL}: sum of levels, then xp). */
    public List<Plek> lijst(String id) {
        List<Plek> l = new ArrayList<>();
        rijen.forEach((u, r) -> {
            long xp = r.xp.getOrDefault(id, 0L);
            int lv;
            if (id.equals(TOTAAL)) {
                lv = 0;
                for (Ambacht a : Ambacht.values()) {
                    lv += AmbachtXp.levelVan(r.xp.getOrDefault(a.id, 0L));
                }
            } else {
                lv = AmbachtXp.levelVan(xp);
            }
            if (xp > 0) {
                l.add(new Plek(u, r.naam, lv, xp, r.sinds.getOrDefault(id, 0L)));
            }
        });
        l.sort(Comparator.comparingInt(Plek::level).reversed().thenComparing(Comparator.comparingLong(Plek::xp).reversed())
                .thenComparingLong(Plek::sinds));
        return l;
    }

    /** Dev/tests: forget everybody. */
    public void wis() {
        rijen.clear();
        setDirty();
    }

    // ---- the website export ----------------------------------------------------------------------------------------------------

    /** T-INT: the hiscores folder of s-site's live export ({@code WebExport} config {@code map}, + "hiscores"). */
    public static Path map(MinecraftServer s) {
        return nl.juiced.guhs.feature.vadsrijk.web.WebExport.map().resolve("hiscores");
    }

    static void tick(MinecraftServer s) {
        // (T-INT: written with s-site's hiscores, WebExport.EXTRA_HISCORES; no timer of its own)
    }

    /**
     * Writes every list through s-site's {@code WebExport} (T-INT: same folder, format and on/off switch as the other hiscores:
     * {lijst, tijd, naam, cap, rijen[plek, naam, waarde = level, xp]}; no uuid: the export's allowlist is names + numbers).
     * Nothing is written while the live export is off ({@code /guhs vadsrijk web nu} writes once). Returns the folder.
     */
    public static Path exporteer(MinecraftServer s) {
        Ranglijst r = get(s);
        String nu = Instant.ofEpochMilli(AmbachtData.nu()).toString();
        List<String> ids = new ArrayList<>();
        for (Ambacht a : Ambacht.values()) {
            ids.add(a.id);
        }
        ids.add(TOTAAL);
        for (String id : ids) {
            JsonObject o = new JsonObject();
            o.addProperty("lijst", "ambacht_" + id);
            Ambacht a = Ambacht.van(id);
            o.addProperty("naam", a == null ? "Totaal" : a.naam);
            o.addProperty("tijd", nu);
            o.addProperty("cap", AmbachtXp.CAP);
            JsonArray l = new JsonArray();
            int plek = 1;
            for (Plek p : r.lijst(id)) {
                if (!p.naam().matches("[A-Za-z0-9_]{1,16}")) {
                    continue;
                }
                JsonObject e = new JsonObject();
                e.addProperty("plek", plek++);
                e.addProperty("naam", p.naam());
                e.addProperty("waarde", p.level());
                e.addProperty("xp", p.xp());
                l.add(e);
            }
            o.add("rijen", l);
            nl.juiced.guhs.feature.vadsrijk.web.WebExport.schrijf("hiscores/ambacht_" + id + ".json", o);
        }
        return map(s);
    }

    // ---- saving --------------------------------------------------------------------------------------------------------------

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        rijen.forEach((u, r) -> {
            CompoundTag c = new CompoundTag();
            c.store("uuid", UUIDUtil.CODEC, u);
            c.putString("naam", r.naam);
            CompoundTag xp = new CompoundTag(), sinds = new CompoundTag();
            r.xp.forEach(xp::putLong);
            r.sinds.forEach(sinds::putLong);
            c.put("xp", xp);
            c.put("sinds", sinds);
            l.add(c);
        });
        t.put("rijen", l);
        return t;
    }

    private static Ranglijst load(CompoundTag t) {
        Ranglijst r = new Ranglijst();
        ListTag l = t.getListOrEmpty("rijen");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompoundOrEmpty(i);
            UUID u = c.read("uuid", UUIDUtil.CODEC).orElse(null);
            if (u == null) {
                continue;
            }
            Rij rij = new Rij();
            rij.naam = c.getStringOr("naam", "?");
            CompoundTag xp = c.getCompoundOrEmpty("xp"), sinds = c.getCompoundOrEmpty("sinds");
            for (String k : xp.keySet()) {
                rij.xp.put(k, xp.getLongOr(k, 0L));
            }
            for (String k : sinds.keySet()) {
                rij.sinds.put(k, sinds.getLongOr(k, 0L));
            }
            r.rijen.put(u, rij);
        }
        return r;
    }
}
