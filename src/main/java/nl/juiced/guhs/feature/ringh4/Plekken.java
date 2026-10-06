package nl.juiced.guhs.feature.ringh4;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * bbq2 (ring-h4): every spot of the tree city the code uses, in template coordinates of the WHOLE build (as Kopieen wants
 * them for a tiled build), and the path of the elf boats. The single source is the builder: tools/features/ring_h4_bouw.py
 * writes {@code data/guhs/ringh4/plekken.json} next to the tiles (read straight from the jar, like GlijPad and NomguhRoute:
 * both sides, no datapack reload). A spot that the builder moves moves here by itself.
 */
public final class Plekken {
    private static final String PAD = "/data/guhs/ringh4/plekken.json";
    /** The anchor of the build (the block every tile turns around; it stands in the middle of the start chunk). */
    public static final BlockPos ANKER;
    /** The size of the build. */
    public static final BlockPos GROOTTE;
    private static final Map<String, Vec3> PLEKKEN = new HashMap<>();
    /** The boat's path from the harbour of the glade to the landing (the middle of the boat; template coordinates). */
    public static final List<Vec3> ROUTE;

    static {
        try (InputStream in = Plekken.class.getResourceAsStream(PAD)) {
            if (in == null) {
                throw new IllegalStateException("ringh4: " + PAD + " is missing (run tools/make_resources.py)");
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            ANKER = BlockPos.containing(punt(root.getAsJsonArray("anker")));
            GROOTTE = BlockPos.containing(punt(root.getAsJsonArray("grootte")));
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("plekken").entrySet()) {
                PLEKKEN.put(e.getKey(), punt(e.getValue().getAsJsonArray()));
            }
            List<Vec3> route = new ArrayList<>();
            for (JsonElement e : root.getAsJsonArray("route")) {
                route.add(punt(e.getAsJsonArray()));
            }
            ROUTE = List.copyOf(route);
        } catch (IOException e) {
            throw new IllegalStateException("ringh4: can't read " + PAD, e);
        }
    }

    private static Vec3 punt(JsonArray a) {
        return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }

    public static boolean heeft(String naam) {
        return PLEKKEN.containsKey(naam);
    }

    /** The spot as the builder wrote it: a block's corner for a block, the exact point for a boat. */
    public static Vec3 punt(String naam) {
        Vec3 p = PLEKKEN.get(naam);
        if (p == null) {
            throw new IllegalArgumentException("ringh4: no spot '" + naam + "' in " + PAD);
        }
        return p;
    }

    /** The block of a spot. */
    public static BlockPos blok(String naam) {
        return BlockPos.containing(punt(naam));
    }

    private Plekken() {
    }
}
