package nl.juiced.guhs.feature.safari.gedrag;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Round 3 (design ch. 5, ronde 40): the park's viewpoints and their favourite spots, from {@code data/guhs/safari/kijkplekken.json}
 * (written by the park generator, R3-A: {@code park.kijkplek(...)}, {@code park.favoriet(...)}), plus named point lists
 * for behaviours ({@code punten}: the badger tunnel, the penguin slide, the beaver dam and lodge).
 * <p>
 * Read leniently (SAFARI_CONTRACT.md "J1 leest"): the root is an object with {@code kijkplekken} (a list) and optional
 * {@code punten} (name to a list of [x,y,z]), or just the list. A viewpoint has {@code naam}, {@code zone}, {@code oog}
 * [x,y,z], a direction ({@code richting}: degrees like a mob's yaw, "north"/"east"/..., [dx,dz] or [dx,dy,dz]; or
 * {@code kijk_naar} [x,y,z]; else toward its favourites), {@code stoel} and {@code favorieten}
 * ({@code naam}, {@code pos}, {@code soorten} (empty: any), {@code straal}). A viewpoint may carry its own {@code punten}.
 * Missing file: no viewpoints, no points (the animals simply stroll at home).
 */
public final class Kijkplekken {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PAD = "/data/guhs/safari/kijkplekken.json";
    /** How far a viewpoint's view cone reaches, and half its opening (radians). */
    public static final double KEGEL_VER = 28, KEGEL_HALF = Math.toRadians(50);

    public record Favoriet(String naam, BlockPos pos, Set<String> soorten, int straal) {
        public boolean voor(String soort) {
            return soorten.isEmpty() || soorten.contains(soort);
        }
    }

    public record Kijkplek(String naam, String zone, Vec3 oog, Vec3 richting, @Nullable BlockPos stoel, List<Favoriet> favorieten) {
        /** Does this spot lie in what you see from here (within reach, inside the cone)? */
        public boolean ziet(double x, double y, double z) {
            Vec3 d = new Vec3(x - oog.x, 0, z - oog.z);
            double l = d.length();
            if (l > KEGEL_VER || l < 0.5) {
                return l < 0.5;
            }
            double cos = (d.x * richting.x + d.z * richting.z) / l;
            return cos >= Math.cos(KEGEL_HALF);
        }
    }

    private static volatile Kijkplekken data;

    public final List<Kijkplek> kijkplekken;
    public final Map<String, List<BlockPos>> punten;

    Kijkplekken(List<Kijkplek> kijkplekken, Map<String, List<BlockPos>> punten) {
        this.kijkplekken = List.copyOf(kijkplekken);
        this.punten = Map.copyOf(punten);
    }

    public static Kijkplekken get() {
        Kijkplekken d = data;
        if (d == null) {
            synchronized (Kijkplekken.class) {
                if (data == null) {
                    data = laad();
                }
                d = data;
            }
        }
        return d;
    }

    /** Forget the cached data (a dev reload, or tests that install their own with {@link #zet}). */
    public static synchronized void vergeet() {
        data = null;
    }

    /** (Tests) use these viewpoints and points instead of the file. */
    public static synchronized void zet(List<Kijkplek> kijkplekken, Map<String, List<BlockPos>> punten) {
        data = new Kijkplekken(kijkplekken, punten);
    }

    public List<BlockPos> punten(String naam) {
        return punten.getOrDefault(naam, List.of());
    }

    /** Is x,z inside the view cone of any viewpoint (verstoppers never hide there)? */
    public boolean inZicht(double x, double y, double z) {
        for (Kijkplek k : kijkplekken) {
            if (k.ziet(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    // --- loading -------------------------------------------------------------------------------------------------------------

    private static Kijkplekken laad() {
        List<Kijkplek> lijst = new ArrayList<>();
        Map<String, List<BlockPos>> punten = new HashMap<>();
        try (InputStream in = Kijkplekken.class.getResourceAsStream(PAD)) {
            if (in == null) {
                return new Kijkplekken(lijst, punten);
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(r);
                JsonArray kp = root.isJsonArray() ? root.getAsJsonArray()
                        : root.isJsonObject() && root.getAsJsonObject().has("kijkplekken") ? root.getAsJsonObject().getAsJsonArray("kijkplekken") : new JsonArray();
                if (root.isJsonObject() && root.getAsJsonObject().has("punten")) {
                    leesPunten(root.getAsJsonObject().get("punten"), punten);
                }
                for (JsonElement e : kp) {
                    if (!e.isJsonObject()) {
                        continue;
                    }
                    Kijkplek k = kijkplek(e.getAsJsonObject(), punten);
                    if (k != null) {
                        lijst.add(k);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Safari: could not read {}", PAD, e);
        }
        LOGGER.info("Safari: {} viewpoints, {} favourite spots, point lists {}", lijst.size(),
                lijst.stream().mapToInt(k -> k.favorieten().size()).sum(), punten.keySet());
        return new Kijkplekken(lijst, punten);
    }

    @Nullable
    private static Kijkplek kijkplek(JsonObject o, Map<String, List<BlockPos>> punten) {
        Vec3 oog = vec(o.get("oog"));
        if (oog == null) {
            return null;
        }
        List<Favoriet> fav = new ArrayList<>();
        if (o.has("favorieten") && o.get("favorieten").isJsonArray()) {
            for (JsonElement f : o.getAsJsonArray("favorieten")) {
                if (!f.isJsonObject()) {
                    continue;
                }
                JsonObject fo = f.getAsJsonObject();
                BlockPos p = pos(fo.get("pos"));
                if (p == null) {
                    continue;
                }
                Set<String> s = new HashSet<>();
                if (fo.has("soorten") && fo.get("soorten").isJsonArray()) {
                    fo.getAsJsonArray("soorten").forEach(x -> s.add(x.getAsString()));
                }
                fav.add(new Favoriet(str(fo, "naam", ""), p, Set.copyOf(s), fo.has("straal") ? fo.get("straal").getAsInt() : 3));
            }
        }
        if (o.has("punten")) {
            leesPunten(o.get("punten"), punten);
        }
        Vec3 r = richting(o, oog, fav);
        return new Kijkplek(str(o, "naam", ""), str(o, "zone", ""), oog, r, pos(o.get("stoel")), fav);
    }

    private static Vec3 richting(JsonObject o, Vec3 oog, List<Favoriet> fav) {
        JsonElement e = o.get("richting");
        if (e != null && e.isJsonPrimitive()) {
            if (e.getAsJsonPrimitive().isNumber()) {
                double yaw = Math.toRadians(e.getAsDouble());       // like a mob: 0 = south (+z), 90 = west (-x)
                return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            }
            switch (e.getAsString().toLowerCase(java.util.Locale.ROOT)) {
                case "north", "noord" -> {
                    return new Vec3(0, 0, -1);
                }
                case "south", "zuid" -> {
                    return new Vec3(0, 0, 1);
                }
                case "east", "oost" -> {
                    return new Vec3(1, 0, 0);
                }
                case "west" -> {
                    return new Vec3(-1, 0, 0);
                }
                default -> {
                }
            }
        }
        if (e != null && e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            double dx = a.get(0).getAsDouble(), dz = a.get(a.size() == 3 ? 2 : 1).getAsDouble();
            Vec3 v = new Vec3(dx, 0, dz);
            if (v.lengthSqr() > 1e-6) {
                return v.normalize();
            }
        }
        Vec3 naar = vec(o.get("kijk_naar"));
        if (naar == null && !fav.isEmpty()) {
            double x = 0, z = 0;
            for (Favoriet f : fav) {
                x += f.pos().getX() + 0.5;
                z += f.pos().getZ() + 0.5;
            }
            naar = new Vec3(x / fav.size(), oog.y, z / fav.size());
        }
        if (naar != null) {
            Vec3 v = new Vec3(naar.x - oog.x, 0, naar.z - oog.z);
            if (v.lengthSqr() > 1e-6) {
                return v.normalize();
            }
        }
        return new Vec3(0, 0, 1);
    }

    private static void leesPunten(JsonElement e, Map<String, List<BlockPos>> uit) {
        if (e == null || !e.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> p : e.getAsJsonObject().entrySet()) {
            List<BlockPos> l = new ArrayList<>();
            JsonElement v = p.getValue();
            if (v.isJsonArray()) {
                JsonArray a = v.getAsJsonArray();
                if (a.size() == 3 && a.get(0).isJsonPrimitive()) {
                    BlockPos b = pos(a);
                    if (b != null) {
                        l.add(b);
                    }
                } else {
                    for (JsonElement x : a) {
                        BlockPos b = pos(x);
                        if (b != null) {
                            l.add(b);
                        }
                    }
                }
            } else if (v.isJsonObject()) {
                leesPunten(v, uit);       // nested groups (e.g. "europa": {"dassentunnel": [...]}) flatten by their own names
                continue;
            }
            uit.computeIfAbsent(p.getKey(), k -> new ArrayList<>()).addAll(l);
        }
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
    }

    @Nullable
    private static Vec3 vec(@Nullable JsonElement e) {
        if (e == null || !e.isJsonArray() || e.getAsJsonArray().size() < 3) {
            return null;
        }
        JsonArray a = e.getAsJsonArray();
        return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }

    @Nullable
    private static BlockPos pos(@Nullable JsonElement e) {
        Vec3 v = vec(e);
        return v == null ? null : BlockPos.containing(v);
    }
}
