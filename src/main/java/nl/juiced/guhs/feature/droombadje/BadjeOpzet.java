package nl.juiced.guhs.feature.droombadje;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.world.phys.Vec3;

/**
 * The Wolkenbadje's own data (data/guhs/dromen/badje_route.json, written by tools/features/droombadje.py), every position
 * relative to the island's centre block like part A's island json: the bubble pad, the float route (feet positions), the
 * three stops (which point of the route, the bath thing, where you hover), Snoes' spot and his bath.
 */
public record BadjeOpzet(Vec3 start, List<Vec3> route, List<Stop> stops, Vec3 snoes, Vec3 bad) {
    private static final Logger LOG = LogUtils.getLogger();

    /** A cloud with a bath thing: the route point it waits at ({@code index}), the block, the hover spot. */
    public record Stop(BadspulletjeBlock.Soort soort, int index, Vec3 spulletje, Vec3 hover) {
    }

    @Nullable
    private static BadjeOpzet cache;

    /** The route of the island (read once; null if the json is missing: then nothing floats). */
    @Nullable
    public static synchronized BadjeOpzet get() {
        if (cache == null) {
            try (InputStream in = BadjeOpzet.class.getResourceAsStream("/data/guhs/dromen/badje_route.json")) {
                if (in != null) {
                    cache = lees(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
                }
            } catch (Exception e) {
                LOG.error("Wolkenbadje: badje_route.json is broken: {}", e.toString());
            }
        }
        return cache;
    }

    public static BadjeOpzet lees(JsonObject o) {
        List<Vec3> route = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("route")) {
            route.add(punt(e.getAsJsonArray()));
        }
        List<Stop> stops = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("stops")) {
            JsonObject s = e.getAsJsonObject();
            stops.add(new Stop(BadspulletjeBlock.Soort.van(s.get("soort").getAsString()), s.get("index").getAsInt(),
                    punt(s.getAsJsonArray("spulletje")), punt(s.getAsJsonArray("hover"))));
        }
        return new BadjeOpzet(punt(o.getAsJsonArray("start")), List.copyOf(route), List.copyOf(stops), punt(o.getAsJsonArray("snoes")),
                punt(o.getAsJsonArray("bad")));
    }

    /** The stop at this route point, or null. */
    @Nullable
    public Stop stopBij(int index) {
        for (Stop s : stops) {
            if (s.index() == index) {
                return s;
            }
        }
        return null;
    }

    private static Vec3 punt(JsonArray a) {
        return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }
}
