package nl.juiced.guhs.feature.guhpixel.bioscoop;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.Guhs;

/**
 * What the SERVER knows of a film: how long it lasts and at which moments the guhs in the seats react. The pictures
 * themselves are client resources ({@code assets/guhs/guhbioscoop/films/<id>.json}, drawn by client.ProjectorRenderer);
 * the generator (tools/features/guhpixel_bioscoop_films.py) writes both from one source, so they always agree. This
 * index is {@code data/guhs/guhbioscoop/films.json}:
 * <pre>{"films": {"skyblok": {"duur": 900, "cues": [[120, "lach"], [300, "schrik"]]}}}</pre>
 */
public record FilmInfo(String id, int duur, List<Cue> cues) {
    /** A moment the audience reacts to: {@code soort} is one of {@link #CUE_SOORTEN}. */
    public record Cue(int t, String soort) {
    }

    /** The kinds of cue the seats understand (the generator checks the same list). */
    public static final List<String> CUE_SOORTEN = List.of("lach", "schrik", "juich", "snik", "slaap", "wakker");

    private static final Identifier BESTAND = Guhs.id("guhbioscoop/films.json");
    @Nullable
    private static Map<String, FilmInfo> alle;

    /** Every film of the index (read once per server start). */
    public static Map<String, FilmInfo> alle(MinecraftServer server) {
        Map<String, FilmInfo> m = alle;
        if (m == null) {
            m = new LinkedHashMap<>();
            try {
                var res = server.getResourceManager().getResource(BESTAND);
                if (res.isPresent()) {
                    try (Reader r = res.get().openAsReader()) {
                        JsonObject films = JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("films");
                        for (Map.Entry<String, JsonElement> e : films.entrySet()) {
                            JsonObject o = e.getValue().getAsJsonObject();
                            List<Cue> cues = new ArrayList<>();
                            for (JsonElement c : o.getAsJsonArray("cues")) {
                                JsonArray a = c.getAsJsonArray();
                                cues.add(new Cue(a.get(0).getAsInt(), a.get(1).getAsString()));
                            }
                            m.put(e.getKey(), new FilmInfo(e.getKey(), o.get("duur").getAsInt(), List.copyOf(cues)));
                        }
                    }
                }
            } catch (Exception e) {
                LogUtils.getLogger().warn("Guhbioscoop: cannot read {}", BESTAND, e);
            }
            alle = m;
        }
        return m;
    }

    @Nullable
    public static FilmInfo van(MinecraftServer server, String id) {
        return alle(server).get(id);
    }

    /** (Server start / stop) read the index again next time. */
    static void vergeet() {
        alle = null;
    }
}
