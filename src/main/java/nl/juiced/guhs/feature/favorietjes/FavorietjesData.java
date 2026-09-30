package nl.juiced.guhs.feature.favorietjes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * The generated data of the favorietjes (tools/features/favorietjes.py writes {@code data/guhs/favorietjes/favorietjes.json}):
 * the clothes -&gt; colour table ("kleuren") and how many variants every wist-je-datje has ("wistjedat"). Read once from the
 * mod's own resources (it is not a datapack registry: the texts that go with it are in the same jar).
 */
final class FavorietjesData {
    private static final Logger LOG = LogUtils.getLogger();
    static final String PAD = "/data/guhs/favorietjes/favorietjes.json";
    private static JsonObject data;

    private FavorietjesData() {
    }

    static synchronized JsonObject get() {
        if (data == null) {
            try (InputStream in = FavorietjesData.class.getResourceAsStream(PAD)) {
                if (in == null) {
                    LOG.warn("Favorietjes: {} is missing (run tools/make_resources.py)", PAD);
                    data = new JsonObject();
                } else {
                    try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        data = JsonParser.parseReader(r).getAsJsonObject();
                    }
                }
            } catch (Exception e) {
                LOG.warn("Favorietjes: could not read {}", PAD, e);
                data = new JsonObject();
            }
        }
        return data;
    }

    static JsonObject deel(String naam) {
        JsonObject d = get();
        return d.has(naam) && d.get(naam).isJsonObject() ? d.getAsJsonObject(naam) : new JsonObject();
    }
}
