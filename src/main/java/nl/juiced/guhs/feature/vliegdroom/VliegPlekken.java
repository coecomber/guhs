package nl.juiced.guhs.feature.vliegdroom;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.dromen.DroomEilanden;

/**
 * 1.8.0 uitbreiding: the spots of the Vliegdroom (data/guhs/dromen/vlieg_plekken.json, written by tools/features/vliegdroom.py):
 * the rings of the parcours (in order, relative to the island's centre, each with its radius), the Rookguhs of the stal you
 * fly yourself, the Rookguh that takes you back down, and the little Rookguh-steiger on the plein (relative to the plein).
 */
public final class VliegPlekken {
    private static final Logger LOG = LogUtils.getLogger();

    /** One ring of the parcours: its centre (relative to the island's centre) and how wide it is. */
    public record Ring(Vec3 midden, double straal) {
    }

    /** Everything at once (read once). */
    public record Plekken(List<Ring> ringen, List<Vec3> stal, Vec3 terug, Vec3 pleinSteiger) {
    }

    private static Plekken cache;

    private VliegPlekken() {
    }

    public static synchronized Plekken get() {
        if (cache == null) {
            cache = lees();
        }
        return cache;
    }

    private static Plekken lees() {
        try (InputStream in = VliegPlekken.class.getResourceAsStream("/data/guhs/dromen/vlieg_plekken.json")) {
            if (in != null) {
                return lees(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
            }
        } catch (Exception e) {
            LOG.error("Vliegdroom: vlieg_plekken.json is broken: {}", e.toString());
        }
        return new Plekken(List.of(), List.of(new Vec3(0, 2, -14)), new Vec3(-6, 2, 14), new Vec3(12.5, 1, -17.5));
    }

    static Plekken lees(JsonObject o) {
        List<Ring> ringen = new ArrayList<>();
        for (var e : o.getAsJsonArray("ringen")) {
            JsonArray a = e.getAsJsonArray();
            ringen.add(new Ring(new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()), a.get(3).getAsDouble()));
        }
        List<Vec3> stal = new ArrayList<>();
        for (var e : o.getAsJsonArray("stal")) {
            stal.add(punt(e.getAsJsonArray()));
        }
        return new Plekken(List.copyOf(ringen), List.copyOf(stal), punt(o.getAsJsonArray("terug")), punt(o.getAsJsonArray("plein_steiger")));
    }

    private static Vec3 punt(JsonArray a) {
        return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }

    /** A spot of the Vliegdroom in the world. */
    public static Vec3 vlieg(Vec3 rel) {
        return DroomEilanden.wereld(DroomEilanden.VLIEG, rel);
    }

    /** A spot of the plein in the world. */
    public static Vec3 plein(Vec3 rel) {
        return DroomEilanden.wereld(DroomEilanden.PLEIN, rel);
    }

    /** Where the Rookguh puts you down on the Vliegdroom (its arrival spot). */
    public static Vec3 aankomst() {
        return vlieg(DroomEilanden.opzet(DroomEilanden.VLIEG).aankomst().plek());
    }

    /** Where the Rookguh puts you down on the plein: right next to its steigertje. */
    public static Vec3 pleinLanding() {
        return plein(get().pleinSteiger()).add(0, 0, 2);
    }
}
