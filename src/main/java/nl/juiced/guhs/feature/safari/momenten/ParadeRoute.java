package nl.juiced.guhs.feature.safari.momenten;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.lobby.Beleving;
import nl.juiced.guhs.feature.safari.rit.Pad;
import nl.juiced.guhs.feature.safari.rit.RitData;

/**
 * The two walks of the pinguïnparade (test round 4, design ronde 47):
 * <ul>
 *   <li>the IJskust: fixed, the longest stretch of the truck route that lies in the IJskust;</li>
 *   <li>the village: data driven. The lobby generator writes {@code data/guhs/safari/parade.json}:
 *       <pre>{"dorp": [[x, y, z], [x, y, z], ...]}</pre>
 *       (block positions on the walking surface, in walking order; a point may also be {"x":..,"y":..,"z":..}).
 *       Without that file (or with fewer than two points) the old village loop of beleving.json
 *       ({@link Beleving#paradeDorp}) is used.</li>
 * </ul>
 */
public final class ParadeRoute {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String BESTAND = "/data/guhs/safari/parade.json";

    private static List<Vec3> dorp;

    /** The IJskust walk: the longest run of the truck route inside the IJskust, every 1.5 blocks, in route order. */
    static List<Vec3> ijskust() {
        List<Vec3> uit = new ArrayList<>();
        Pad route = RitData.get().route;
        ParkData d = ParkData.get();
        int zone = d.zoneIndex("ijskust");
        if (route == null || zone < 0 || route.lengte() <= 0) {
            return uit;
        }
        // (the loop may start in the middle of the IJskust: walk it twice and keep the longest run)
        int n = (int) Math.floor(route.lengte() / 1.5);
        List<Vec3> run = new ArrayList<>();
        for (int i = 0; i < n * 2 && i < 6000; i++) {
            Vec3 p = route.op((i % n) * 1.5);
            if (d.zone((int) Math.floor(p.x), (int) Math.floor(p.z)) == zone) {
                run.add(p);
            } else {
                if (run.size() > uit.size()) {
                    uit = run;
                }
                run = new ArrayList<>();
            }
        }
        return run.size() > uit.size() ? run : uit;
    }

    /** The village walk: parade.json of the lobby, else the old loop of beleving.json. */
    static synchronized List<Vec3> dorp() {
        if (dorp == null) {
            dorp = laad();
            if (dorp.size() < 2) {
                dorp = new ArrayList<>();
                for (BlockPos p : Beleving.get().paradeDorp) {
                    dorp.add(Vec3.atBottomCenterOf(p));
                }
            }
        }
        return dorp;
    }

    private static List<Vec3> laad() {
        List<Vec3> uit = new ArrayList<>();
        try (InputStream in = ParadeRoute.class.getResourceAsStream(BESTAND)) {
            if (in == null) {
                return uit;
            }
            JsonElement el = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (el.isJsonObject() && el.getAsJsonObject().has("dorp")) {
                for (JsonElement p : el.getAsJsonObject().getAsJsonArray("dorp")) {
                    uit.add(punt(p));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Safari parade: {} is broken, using the village loop of beleving.json", BESTAND, e);
            uit.clear();
        }
        return uit;
    }

    static Vec3 punt(JsonElement p) {
        if (p.isJsonArray()) {
            JsonArray a = p.getAsJsonArray();
            return new Vec3(a.get(0).getAsInt() + 0.5, a.get(1).getAsInt(), a.get(2).getAsInt() + 0.5);
        }
        JsonObject o = p.getAsJsonObject();
        return new Vec3(o.get("x").getAsInt() + 0.5, o.get("y").getAsInt(), o.get("z").getAsInt() + 0.5);
    }

    private ParadeRoute() {
    }
}
