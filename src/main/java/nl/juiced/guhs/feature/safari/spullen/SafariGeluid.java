package nl.juiced.guhs.feature.safari.spullen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * All safari sounds (S6; the OGGs: tools/features/safari_geluid.py, sounds.json: tools/features/safari_blokken.py).
 * <ul>
 *   <li>{@code guhs:safari.dier.<groep>}: an animal call per group; soorten.json {@code "geluid"} says which group a species
 *       uses ({@link #dier}).</li>
 *   <li>{@code guhs:safari.sfeer.<zone>}: an ambience loop per park zone (client {@code SafariSfeer} plays it).</li>
 *   <li>{@code guhs:safari.muziek.lobby}: the cheerful lobby loop (played in the entreedorp).</li>
 *   <li>{@code guhs:safari.camera}, {@code safari.munt}, {@code safari.truck.motor}, {@code safari.truck.toeter},
 *       {@code safari.kano}: for the camera (S1), the Safarimunten (S0), the truck and the canoe (S3).</li>
 * </ul>
 */
public final class SafariGeluid {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final List<String> DIER_GROEPEN = List.of("grote_kat", "olifant", "grazer", "klein", "vogel", "groot_vogel", "pinguin",
            "zee", "beer", "aap", "reptiel", "nijlpaard", "geit", "ezel", "cavia",
            "kriebel", "hommel", "vis", "kikker", "vleermuis");     // (1.6.0 F12: the Kriebelhuis' small folk)
    public static final List<String> ZONES = List.of("savanne", "delta", "regenwoud", "bamboe", "ijskust", "oertijd", "europa", "australie",
            "entree", "kriebelhuis_kriebel", "kriebelhuis_nacht", "kriebelhuis_diepzee");   // (F12: the Kriebelhuis' halls)

    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> DIER = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> SFEER = new LinkedHashMap<>();

    static {
        for (String g : DIER_GROEPEN) {
            DIER.put(g, sound("safari.dier." + g));
        }
        for (String z : ZONES) {
            SFEER.put(z, sound("safari.sfeer." + z));
        }
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> MUZIEK_LOBBY = sound("safari.muziek.lobby");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAMERA = sound("safari.camera");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUNT = sound("safari.munt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRUCK_MOTOR = sound("safari.truck.motor");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRUCK_TOETER = sound("safari.truck.toeter");
    public static final DeferredHolder<SoundEvent, SoundEvent> KANO = sound("safari.kano");
    /** Test round 4: the tune of the pinguïnparade, four phrases of 4 seconds (flute and drum, tools/features/safari_parade_geluid.py). */
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PARADE = List.of(sound("safari.parade.1"), sound("safari.parade.2"),
            sound("safari.parade.3"), sound("safari.parade.4"));

    /** F12: the Beverbeek's brook (the biome's ambient loop) and its beaver gnawing now and then (the biome's additions). */
    public static final DeferredHolder<SoundEvent, SoundEvent> BEVERBEEK_SFEER = sound("beverbeek.sfeer");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEVERBEEK_KNAAG = sound("beverbeek.knaag");
    /** F12: De Eeuwige Knabbelvelden's dreamy ambient loop (the biome's loop). */
    public static final DeferredHolder<SoundEvent, SoundEvent> KNABBELVELDEN_SFEER = sound("knabbelvelden.sfeer");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    private static volatile Map<String, String> perSoort;

    /** The call of a species (soorten.json "geluid", read once from the jar); null for an unknown species. */
    public static SoundEvent voorSoort(String soort) {
        Map<String, String> m = perSoort;
        if (m == null) {
            m = new java.util.HashMap<>();
            try (java.io.InputStream in = SafariGeluid.class.getResourceAsStream("/data/guhs/safari/soorten.json")) {
                if (in != null) {
                    com.google.gson.JsonObject o = com.google.gson.JsonParser.parseReader(
                            new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    for (Map.Entry<String, com.google.gson.JsonElement> e : o.entrySet()) {
                        if (e.getValue().isJsonObject() && e.getValue().getAsJsonObject().has("geluid")) {
                            m.put(e.getKey(), e.getValue().getAsJsonObject().get("geluid").getAsString());
                        }
                    }
                }
            } catch (Exception ignored) {
                // no calls then (the animals stay quiet)
            }
            perSoort = m;
        }
        return dier(m.get(soort));
    }

    /** The call of a species, from its soorten.json "geluid" (e.g. "safari.dier.grote_kat"); null when unknown. */
    public static SoundEvent dier(String event) {
        if (event == null || event.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.SOUND_EVENT.getValue(Guhs.id(event));
    }

    private SafariGeluid() {
    }
}
