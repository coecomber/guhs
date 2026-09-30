package nl.juiced.guhs.feature.baltoslee;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * 3.0: the Nomguh sled route (CONTRACT_30 §4.9): the file {@code assets/guhs/nomguh/route.json} that balto writes from the same
 * python data it builds Nomguh with (template coordinates; Nomguh is placed unrotated, so world = anchor + offset).
 * {@code heen} runs from the stable to the berghut, {@code terug} back (the same line reversed unless the file says otherwise).
 * FUNDAMENT: the record and a working loader; balto-slee owns it (may add more).
 */
public record NomguhRoute(BlockPos anker, List<Vec3> heen, List<Vec3> terug, List<Double> breedte, BlockPos stal, BlockPos ziekenhuis,
                          BlockPos berghut, List<Integer> rust, List<int[]> ijsbrug, List<Lawine> lawine, int dieptepunt, double lengte,
                          Map<String, Integer> tijd) {
    public record Lawine(int van, int tot, boolean links) {
    }

    public static final String PAD = "/assets/guhs/nomguh/route.json";
    private static volatile NomguhRoute geladen;

    /** The route from the jar (both sides), cached; offsets are relative to {@link #anker} (template coordinates). */
    public static NomguhRoute laad() {
        NomguhRoute r = geladen;
        if (r == null) {
            try (InputStream in = NomguhRoute.class.getResourceAsStream(PAD)) {
                if (in == null) {
                    throw new IllegalStateException("no " + PAD);
                }
                r = lees(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            geladen = r;
        }
        return r;
    }

    /** Reads the route file format of CONTRACT_30 §4.9. */
    public static NomguhRoute lees(JsonObject o) {
        JsonArray a = o.getAsJsonArray("anker");
        BlockPos anker = new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
        List<Vec3> heen = new ArrayList<>();
        List<Double> breedte = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("punten")) {
            JsonArray p = e.getAsJsonArray();
            heen.add(new Vec3(p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble()));
            breedte.add(p.size() > 3 ? p.get(3).getAsDouble() : 2.0);
        }
        List<Vec3> terug = new ArrayList<>();
        JsonElement t = o.get("terug");
        if (t != null && t.isJsonArray()) {
            for (JsonElement e : t.getAsJsonArray()) {
                JsonArray p = e.getAsJsonArray();
                terug.add(new Vec3(p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble()));
            }
        } else {
            terug.addAll(heen);
            java.util.Collections.reverse(terug);
        }
        List<Integer> rust = new ArrayList<>();
        if (o.has("rust")) {
            o.getAsJsonArray("rust").forEach(e -> rust.add(e.getAsInt()));
        }
        List<int[]> ijsbrug = new ArrayList<>();
        if (o.has("ijsbrug")) {
            o.getAsJsonArray("ijsbrug").forEach(e -> ijsbrug.add(new int[]{e.getAsJsonArray().get(0).getAsInt(), e.getAsJsonArray().get(1).getAsInt()}));
        }
        List<Lawine> lawine = new ArrayList<>();
        if (o.has("lawine")) {
            o.getAsJsonArray("lawine").forEach(e -> {
                JsonArray l = e.getAsJsonArray();
                lawine.add(new Lawine(l.get(0).getAsInt(), l.get(1).getAsInt(), l.size() < 3 || "links".equals(l.get(2).getAsString())));
            });
        }
        Map<String, Integer> tijd = new java.util.LinkedHashMap<>();
        if (o.has("tijd_ticks")) {
            o.getAsJsonObject("tijd_ticks").entrySet().forEach(e -> tijd.put(e.getKey(), e.getValue().getAsInt()));
        }
        double lengte = o.has("lengte") ? o.get("lengte").getAsDouble() : lengte(heen);
        return new NomguhRoute(anker, List.copyOf(heen), List.copyOf(terug), List.copyOf(breedte), pos(o, "stal", anker), pos(o, "ziekenhuis", anker),
                pos(o, "berghut", anker), List.copyOf(rust), List.copyOf(ijsbrug), List.copyOf(lawine),
                o.has("dieptepunt") ? o.get("dieptepunt").getAsInt() : Math.max(0, heen.size() / 2), lengte, Map.copyOf(tijd));
    }

    private static BlockPos pos(JsonObject o, String key, BlockPos standaard) {
        if (!o.has(key)) {
            return standaard;
        }
        JsonArray a = o.getAsJsonArray(key);
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    private static double lengte(List<Vec3> punten) {
        double d = 0;
        for (int i = 1; i < punten.size(); i++) {
            d += punten.get(i).distanceTo(punten.get(i - 1));
        }
        return d;
    }

    /** This route in world coordinates, for a Nomguh whose anchor block is at wereldAnker. */
    public NomguhRoute in(BlockPos wereldAnker) {
        Vec3 d = Vec3.atLowerCornerOf(wereldAnker.subtract(anker));
        BlockPos b = wereldAnker.subtract(anker);
        return new NomguhRoute(wereldAnker, heen.stream().map(p -> p.add(d)).toList(), terug.stream().map(p -> p.add(d)).toList(), breedte,
                stal.offset(b), ziekenhuis.offset(b), berghut.offset(b), rust, ijsbrug, lawine, dieptepunt, lengte, tijd);
    }

    /** A synthetic route along these points (gametests): width 3, back = the same line reversed, rests every 5 points. */
    public static NomguhRoute test(List<Vec3> punten) {
        List<Vec3> terug = new ArrayList<>(punten);
        java.util.Collections.reverse(terug);
        List<Double> breedte = punten.stream().map(p -> 3.0).toList();
        List<Integer> rust = new ArrayList<>();
        for (int i = 5; i < punten.size() - 1; i += 5) {
            rust.add(i);
        }
        BlockPos start = BlockPos.containing(punten.get(0)), eind = BlockPos.containing(punten.get(punten.size() - 1));
        return new NomguhRoute(start, List.copyOf(punten), List.copyOf(terug), breedte, start, start, eind, List.copyOf(rust), List.of(), List.of(),
                Math.max(0, punten.size() / 2), lengte(punten), Map.of("makkelijk", 2400, "medium", 1800, "lastig", 1400));
    }
}
