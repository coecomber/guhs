package nl.juiced.guhs.feature.erfenis.sherlock;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Who says which line of a dialogue. tools/features/erfenis_sherlock.py is the one source of the dialogues: it writes the
 * texts to the lang files ({@code sherlock.<groep>.<i>}) and the speakers per group to
 * {@code data/guhs/erfenis/sherlock/regels.json} ({@code {"zaak.eieren.intro": ["sherlock", "vadson", ...]}}), read here once.
 */
public final class Regels {
    private static final String PAD = "/data/guhs/erfenis/sherlock/regels.json";
    private static Map<String, List<String>> regels;

    private Regels() {
    }

    /** The speakers of a group (one per line), empty when the group doesn't exist. */
    public static List<String> sprekers(String groep) {
        return alle().getOrDefault(groep, List.of());
    }

    public static synchronized Map<String, List<String>> alle() {
        if (regels == null) {
            Map<String, List<String>> m = new HashMap<>();
            try (InputStream in = Regels.class.getResourceAsStream(PAD)) {
                if (in != null) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                        List<String> l = new ArrayList<>();
                        e.getValue().getAsJsonArray().forEach(x -> l.add(x.getAsString()));
                        m.put(e.getKey(), List.copyOf(l));
                    }
                }
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().error("Sherlock: could not read {}", PAD, e);
            }
            regels = m;
        }
        return regels;
    }
}
