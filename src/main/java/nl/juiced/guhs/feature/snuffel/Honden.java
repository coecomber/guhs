package nl.juiced.guhs.feature.snuffel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * The dogs of Het Snuffeleiland as DATA: the breeds (with their coats and how tall they are), the island's named
 * residents (breed + coat + puppy flag; their accessories are part of their own model) and the three companions. Read
 * once, on both sides, from the mod's own resources: {@code data/guhs/snuffel/honden.json}, written by
 * tools/features/snuffel.py from the approved generator (tools/features/snuffel_modellen.py: RASSEN, VACHTEN, BEWONERS,
 * MAATJES). Nothing here is a registry: a breed, coat or resident that the file does not know simply does not exist.
 * <p>
 * Model names (assets/guhs/geckolib/models|animations/entity, textures/entity): a breed is {@code snuffelhond_<ras>}
 * (puppy: {@code snuffelhond_<ras>_pup}) with one texture per coat {@code <model>_<kleur>}; a resident is
 * {@code snuffel_<id>} (model, animations and texture of its own); a companion is
 * {@code snuffel_maatje_<a|b|c>_<blij|ondeugend>}; the tree is {@code snuffel_boompje_<1..4>}.
 */
public final class Honden {
    private static final Logger LOG = LogUtils.getLogger();
    static final String PAD = "/data/guhs/snuffel/honden.json";
    /** Every dog is drawn at this scale (16 model pixels = 0.8 block), the companion at half size. */
    public static final float SCHAAL = 0.8f, MAATJE_SCHAAL = 0.5f;
    /** The box of a player in dog form is this wide (a player's own width, so nothing changes sideways). */
    public static final float BREEDTE = 0.6f;
    /** The three companions, in the order of the choice screen. */
    public static final List<String> MAATJES = List.of("a", "b", "c");

    /**
     * A breed: its coats (ids, in the order of the choice screen), whether a player may pick it, and how tall the dog and
     * its puppy are in blocks (the top of the head, the eyes).
     */
    public record Ras(String id, List<String> kleuren, boolean speelbaar, boolean heeftPup, float hoogte, float oog, float pupHoogte, float pupOog) {
        public Component naam() {
            return Component.translatable("gui.guhs.snuffel.ras." + id);
        }

        public Component kleurNaam(String kleur) {
            return Component.translatable("gui.guhs.snuffel.kleur." + id + "." + kleur);
        }

        public float hoogte(boolean pup) {
            return pup && heeftPup ? pupHoogte : hoogte;
        }

        public float oog(boolean pup) {
            return pup && heeftPup ? pupOog : oog;
        }
    }

    /** A named resident of the island: which breed and coat it is (for its size) and whether it is a puppy. */
    public record Bewoner(String id, String ras, String kleur, boolean pup) {
        public Component naam() {
            return Component.translatable("entity.guhs.snuffel_bewoner." + id);
        }
    }

    private static Map<String, Ras> rassen;
    private static Map<String, Bewoner> bewoners;

    private Honden() {
    }

    private static synchronized void laad() {
        if (rassen != null) {
            return;
        }
        Map<String, Ras> r = new LinkedHashMap<>();
        Map<String, Bewoner> b = new LinkedHashMap<>();
        try (InputStream in = Honden.class.getResourceAsStream(PAD)) {
            if (in == null) {
                LOG.error("Snuffeleiland: {} is missing (run tools/make_resources.py)", PAD);
            } else {
                try (Reader lezer = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject o = JsonParser.parseReader(lezer).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("rassen").entrySet()) {
                        JsonObject x = e.getValue().getAsJsonObject();
                        List<String> kleuren = new ArrayList<>();
                        x.getAsJsonArray("kleuren").forEach(k -> kleuren.add(k.getAsString()));
                        r.put(e.getKey(), new Ras(e.getKey(), List.copyOf(kleuren), x.get("speelbaar").getAsBoolean(), x.get("pup").getAsBoolean(), x.get("hoogte").getAsFloat(),
                                x.get("oog").getAsFloat(), x.get("pup_hoogte").getAsFloat(), x.get("pup_oog").getAsFloat()));
                    }
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("bewoners").entrySet()) {
                        JsonObject x = e.getValue().getAsJsonObject();
                        b.put(e.getKey(), new Bewoner(e.getKey(), x.get("ras").getAsString(), x.get("kleur").getAsString(), x.get("pup").getAsBoolean()));
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Snuffeleiland: could not read {}", PAD, e);
        }
        bewoners = b;
        rassen = r;
    }

    /** Every breed, the playable ones first (the order of the generator). */
    public static List<Ras> rassen() {
        laad();
        return List.copyOf(rassen.values());
    }

    /** The breeds a player may choose. */
    public static List<Ras> speelbaar() {
        return rassen().stream().filter(Ras::speelbaar).toList();
    }

    @Nullable
    public static Ras ras(@Nullable String id) {
        laad();
        return id == null ? null : rassen.get(id);
    }

    /** Is this a breed with this coat? */
    public static boolean bestaat(String ras, String kleur) {
        Ras r = ras(ras);
        return r != null && r.kleuren().contains(kleur);
    }

    /** The named residents, in the order of the generator's list. */
    public static List<Bewoner> bewoners() {
        laad();
        return List.copyOf(bewoners.values());
    }

    @Nullable
    public static Bewoner bewoner(@Nullable String id) {
        laad();
        return id == null ? null : bewoners.get(id);
    }

    /** The model / animation / texture name of a dog: a resident's own, else the breed's (with the coat for the texture). */
    public static String model(String bewoner, String ras, boolean pup) {
        if (!bewoner.isEmpty() && bewoner(bewoner) != null) {
            return "snuffel_" + bewoner;
        }
        Ras r = ras(ras);
        return r == null ? "snuffelhond_shiba" : "snuffelhond_" + ras + (pup && r.heeftPup() ? "_pup" : "");
    }

    public static String textuur(String bewoner, String ras, String kleur, boolean pup) {
        if (!bewoner.isEmpty() && bewoner(bewoner) != null) {
            return "snuffel_" + bewoner;
        }
        Ras r = ras(ras);
        if (r == null) {
            return "snuffelhond_shiba_rood";
        }
        return "snuffelhond_" + ras + (pup && r.heeftPup() ? "_pup" : "") + "_" + (r.kleuren().contains(kleur) ? kleur : r.kleuren().get(0));
    }

    public static Component maatjeNaam(String maatje) {
        return Component.translatable("gui.guhs.snuffel.maatje." + maatje);
    }
}
