package nl.juiced.guhs.feature.safari.lobby;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The spots of test round 3's experience (slice J3), written by the park generator:
 * <ul>
 *   <li>{@code data/guhs/safari/beleving.json} (tools/features/safari_park/zone_zz_j3.py): the guh-speeltuin and its toys,
 *       the kampvuur and its storyteller, the penguin parade's walk through the village, and the lookout chairs found in the
 *       park (the fallback for the kijk-guhs);</li>
 *   <li>{@code data/guhs/safari/kijkplekken.json} (the river pipeline R3-A, when it exists): the park's viewpoints; every
 *       viewpoint with a {@code stoel} gets a kijk-guh. Read leniently: a missing file or field is no error.</li>
 * </ul>
 */
public final class Beleving {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** A toy of the speeltuin: "schommel", "glijbaantje" or "wip", its controller block and the way it faces. */
    public record Speelgoed(String soort, BlockPos pos, Direction facing) {
    }

    /** A chair of a lookout for a kijk-guh: the chair block, the way the guh looks (yaw), its zone ("" unknown). */
    public record Stoel(BlockPos pos, float yaw, String zone) {
    }

    public final List<Speelgoed> speelgoed = new ArrayList<>();
    @Nullable
    public BlockPos speeltuin;
    public int speeltuinStraal = 7;
    @Nullable
    public BlockPos kampvuur;
    @Nullable
    public BlockPos verteller;
    public float vertellerYaw;
    public final List<BlockPos> paradeDorp = new ArrayList<>();
    public final List<Stoel> stoelen = new ArrayList<>();

    private static volatile Beleving data;

    public static Beleving get() {
        Beleving d = data;
        if (d == null) {
            synchronized (Beleving.class) {
                if (data == null) {
                    data = laad();
                }
                d = data;
            }
        }
        return d;
    }

    /** (Tests) forget the cached data. */
    static synchronized void vergeet() {
        data = null;
    }

    private static BlockPos pos(JsonElement e) {
        JsonArray a = e.getAsJsonArray();
        return new BlockPos((int) Math.floor(a.get(0).getAsDouble()), (int) Math.floor(a.get(1).getAsDouble()), (int) Math.floor(a.get(2).getAsDouble()));
    }

    /** Minecraft yaw of a way to look: a compass word ("north"...), a [dx, dz] step, or a number (a yaw already). */
    static float yaw(@Nullable JsonElement e, float anders) {
        if (e == null || e.isJsonNull()) {
            return anders;
        }
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            return e.getAsFloat();
        }
        if (e.isJsonPrimitive()) {
            Direction d = Direction.byName(e.getAsString().toLowerCase(Locale.ROOT));
            return d != null && d.getAxis().isHorizontal() ? d.toYRot() : anders;
        }
        if (e.isJsonArray() && e.getAsJsonArray().size() >= 2) {
            JsonArray a = e.getAsJsonArray();
            double dx = a.get(0).getAsDouble(), dz = a.get(a.size() == 3 ? 2 : 1).getAsDouble();
            return dx == 0 && dz == 0 ? anders : (float) Math.toDegrees(Math.atan2(-dx, dz));
        }
        return anders;
    }

    @Nullable
    private static JsonElement json(String pad) {
        try (InputStream in = Beleving.class.getResourceAsStream(pad)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.error("Safari beleving: could not read {}", pad, e);
            return null;
        }
    }

    private static Beleving laad() {
        Beleving b = new Beleving();
        JsonElement el = json("/data/guhs/safari/beleving.json");
        List<Stoel> terugval = new ArrayList<>();
        try {
            if (el != null && el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                if (o.has("speeltuin")) {
                    JsonObject s = o.getAsJsonObject("speeltuin");
                    b.speeltuin = pos(s.get("centrum"));
                    JsonArray mn = s.getAsJsonArray("min"), mx = s.getAsJsonArray("max");
                    b.speeltuinStraal = Math.max(mx.get(0).getAsInt() - mn.get(0).getAsInt(), mx.get(1).getAsInt() - mn.get(1).getAsInt()) / 2 + 1;
                    for (JsonElement t : s.getAsJsonArray("speelgoed")) {
                        JsonObject to = t.getAsJsonObject();
                        Direction f = Direction.byName(to.get("facing").getAsString());
                        b.speelgoed.add(new Speelgoed(to.get("soort").getAsString(), pos(to.get("pos")), f == null ? Direction.NORTH : f));
                    }
                }
                if (o.has("kampvuur")) {
                    JsonObject k = o.getAsJsonObject("kampvuur");
                    b.kampvuur = pos(k.get("centrum"));
                    b.verteller = pos(k.get("verteller"));
                    b.vertellerYaw = k.get("yaw").getAsFloat();
                }
                if (o.has("parade_dorp")) {
                    for (JsonElement p : o.getAsJsonArray("parade_dorp")) {
                        b.paradeDorp.add(pos(p));
                    }
                }
                if (o.has("kijkstoelen")) {
                    for (JsonElement p : o.getAsJsonArray("kijkstoelen")) {
                        JsonObject so = p.getAsJsonObject();
                        terugval.add(new Stoel(pos(so.get("pos")), yaw(so.get("facing"), 0f), so.has("zone") ? so.get("zone").getAsString() : ""));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Safari beleving: beleving.json is broken", e);
        }
        // the viewpoints of the river pipeline (R3-A), when it wrote them: they win over the chairs found by the generator
        JsonElement kp = json("/data/guhs/safari/kijkplekken.json");
        try {
            JsonArray lijst = kp == null ? null : kp.isJsonArray() ? kp.getAsJsonArray()
                    : kp.isJsonObject() && kp.getAsJsonObject().has("kijkplekken") ? kp.getAsJsonObject().getAsJsonArray("kijkplekken") : null;
            if (lijst != null) {
                for (JsonElement p : lijst) {
                    JsonObject v = p.getAsJsonObject();
                    if (!v.has("stoel") || v.get("stoel").isJsonNull()) {
                        continue;
                    }
                    b.stoelen.add(new Stoel(pos(v.get("stoel")), yaw(v.get("richting"), 0f), v.has("zone") ? v.get("zone").getAsString() : ""));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Safari beleving: kijkplekken.json is broken", e);
        }
        if (b.stoelen.isEmpty()) {
            b.stoelen.addAll(terugval);
        }
        return b;
    }

    private Beleving() {
    }
}
