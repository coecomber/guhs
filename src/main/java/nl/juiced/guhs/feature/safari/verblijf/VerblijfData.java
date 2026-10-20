package nl.juiced.guhs.feature.safari.verblijf;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

/**
 * Op Safari! step 2 (S5): what a species needs at home, from {@code data/guhs/safari/verblijf.json} (written by
 * tools/features/safari_verblijf.py): the price class and price of its verblijfsbord, its food, the {@link Eis requirements}
 * that make it happy, its gifts, its toy, its friends ({@code *}: everyone), specials (riding, eggs, deliveries, water) and the
 * helper tasks shown in the Bouwopdracht tab (the building itself comes with the Bouwkeet slice).
 */
public final class VerblijfData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BESTAND = "/data/guhs/safari/verblijf.json";

    /** One requirement: {@code soort} "blok" (these blocks), "tag" (a block tag) or "water", at least {@code min} in the home. */
    public record Eis(String id, String soort, List<String> blokken, String tag, int min) {
    }

    public record Dier(String soort, String klasse, int prijs, List<String> voer, List<Eis> eisen, List<String> cadeaus, String speeltje,
                       List<String> vrienden, List<String> taken, boolean rijdier, boolean ei, boolean bezorger, int waterNodig) {
        /** Is {@code ander} a friend of this species? */
        public boolean vriend(String ander) {
            return !ander.equals(soort) && (vrienden.contains("*") || vrienden.contains(ander));
        }

        /** The full-dex reward (the gouden dikdik): never sold in the shop. */
        public boolean beloning() {
            return klasse.equals("beloning");
        }
    }

    private static volatile Map<String, Dier> dieren;

    public static Map<String, Dier> alle() {
        Map<String, Dier> d = dieren;
        if (d == null) {
            synchronized (VerblijfData.class) {
                if (dieren == null) {
                    dieren = laad();
                }
                d = dieren;
            }
        }
        return d;
    }

    @Nullable
    public static Dier dier(String soort) {
        return alle().get(soort);
    }

    public static Set<String> soorten() {
        return alle().keySet();
    }

    private static Map<String, Dier> laad() {
        Map<String, Dier> uit = new LinkedHashMap<>();
        try (InputStream in = VerblijfData.class.getResourceAsStream(BESTAND)) {
            if (in == null) {
                LOGGER.warn("Safari: {} is missing (run tools/features/safari_verblijf.py)", BESTAND);
                return uit;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject soorten = root.getAsJsonObject("soorten");
            for (Map.Entry<String, JsonElement> e : soorten.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                List<Eis> eisen = new ArrayList<>();
                for (JsonElement el : o.getAsJsonArray("eisen")) {
                    JsonObject eo = el.getAsJsonObject();
                    eisen.add(new Eis(eo.get("id").getAsString(), eo.get("soort").getAsString(), lijst(eo.getAsJsonArray("blokken")),
                            eo.has("tag") ? eo.get("tag").getAsString() : "", eo.get("min").getAsInt()));
                }
                uit.put(e.getKey(), new Dier(e.getKey(), o.get("klasse").getAsString(), o.get("prijs").getAsInt(), lijst(o.getAsJsonArray("voer")),
                        List.copyOf(eisen), lijst(o.getAsJsonArray("cadeaus")), o.get("speeltje").getAsString(), lijst(o.getAsJsonArray("vrienden")),
                        lijst(o.getAsJsonArray("taken")), bool(o, "rijdier"), bool(o, "ei"), bool(o, "bezorger"),
                        o.has("water_nodig") ? o.get("water_nodig").getAsInt() : 0));
            }
        } catch (Exception ex) {
            LOGGER.error("Safari: can't read {}", BESTAND, ex);
        }
        return uit;
    }

    private static boolean bool(JsonObject o, String k) {
        return o.has(k) && o.get(k).getAsBoolean();
    }

    private static List<String> lijst(@Nullable JsonArray a) {
        List<String> uit = new ArrayList<>();
        if (a != null) {
            a.forEach(x -> uit.add(x.getAsString()));
        }
        return List.copyOf(uit);
    }

    private VerblijfData() {
    }
}
