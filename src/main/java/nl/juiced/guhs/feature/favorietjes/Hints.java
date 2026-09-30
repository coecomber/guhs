package nl.juiced.guhs.feature.favorietjes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.feature.band.FavorietSoort;
import nl.juiced.guhs.feature.band.Vriendjes;

/**
 * Warm or cold: is what a guh just tried CLOSE to its (secret) favourite? Close means the same family: a macaron when it
 * loves another macaron, an ijsje when its favourite is a milkshake, a guhmension biome when it loves another one, the
 * colour next to its favourite on the colour wheel, a disco song when it loves another disco song, a toy that plays the
 * same way, an emote of the same mood, a guh that is already its friend (or its favourite's friend). Everything else is cold.
 */
public final class Hints {
    private Hints() {
    }

    /** True = warm ("Guh kijkt nieuwsgierig..."), false = cold ("Guh snuffelt... njeg?"). Never warm for the favourite itself. */
    public static boolean warm(@Nullable MinecraftServer s, @Nullable UUID zelf, FavorietSoort soort, String favoriet, String waarde) {
        if (favoriet.equals(waarde)) {
            return false;
        }
        return switch (soort) {
            case ETEN -> etenGroep(waarde).equals(etenGroep(favoriet));
            case PLEK -> plekFamilie(waarde).equals(plekFamilie(favoriet));
            case KNUFFEL -> knuffelGroep(waarde).equals(knuffelGroep(favoriet));
            case LIEDJE -> bron(waarde).equals(bron(favoriet));
            case SPEELTJE -> speeltjeGroep(waarde).equals(speeltjeGroep(favoriet));
            case EMOTE -> emoteGroep(waarde).equals(emoteGroep(favoriet));
            case KLEUR -> KLEUR_BUREN.getOrDefault(favoriet, List.of()).contains(waarde);
            case VRIEND -> s != null && (vrienden(s, favoriet, waarde) || (zelf != null && vrienden(s, zelf.toString(), waarde)));
        };
    }

    // --- eten ---------------------------------------------------------------------------------------------------------------

    /** The family of a snack: ijskoud, gebak, snoep, fruit, frituur, knabbel, vis (or its own id). */
    public static String etenGroep(String itemId) {
        String p = itemId.substring(itemId.indexOf(':') + 1);
        if (p.startsWith("kaasijsje") || p.contains("milkshake")) {
            return "ijskoud";
        }
        if (p.contains("suikerspin") || p.contains("marshmallow")) {
            return "snoep";
        }
        if (p.contains("gefrituurd") || p.contains("sate") || p.contains("fondue")) {
            return "frituur";
        }
        if (p.contains("vis")) {
            return "vis";
        }
        if (p.equals("sweet_berries") || p.equals("knabbelbessen") || p.equals("melon_slice") || p.equals("apple") || p.equals("glow_berries")) {
            return "fruit";
        }
        if (p.contains("kaas") || p.contains("knabbels")) {
            return "knabbel";
        }
        for (String g : GEBAK) {
            if (p.contains(g)) {
                return "gebak";
            }
        }
        return p;
    }

    private static final List<String> GEBAK = List.of("cupcake", "macaron", "taart", "broodje", "vlaai", "croissant", "koek", "krakeling",
            "bolletje", "muffin", "tompouce", "donut", "wafel", "cookie", "pie", "cake", "bread");

    // --- plek ---------------------------------------------------------------------------------------------------------------

    /** The family of a biome: the guh biomes of one place belong together, the rest by kind (flowers, snow, water...). */
    public static String plekFamilie(String biome) {
        String ns = biome.substring(0, Math.max(0, biome.indexOf(':')));
        String p = biome.substring(biome.indexOf(':') + 1);
        if (ns.equals("guhs")) {
            return switch (p) {
                case "guhpolder", "knuffeldal" -> "guh_bovenwereld";
                case "vadswoud", "guh_peaks", "vads_cliffs" -> "guh_hoog";
                default -> "guhmensie";
            };
        }
        return plekGroep(biome);
    }

    /** A biome's group for the dagboekje and the hints (strand, sneeuw, bloemen, ...); "overig" when nothing fits. */
    public static String plekGroep(String biome) {
        String p = biome.substring(biome.indexOf(':') + 1);
        if (biome.startsWith("guhs:")) {
            return switch (p) {
                case "knuffeldal" -> "knuffeldal";
                case "guhpolder" -> "guhpolder";
                case "vadswoud" -> "vadswoud";
                case "sneeuwguhtoendra" -> "sneeuwguhtoendra";   // 3.0
                case "guhwaii" -> "guhwaii";                     // 3.0
                default -> "guhmensie";
            };
        }
        if (p.contains("beach") || p.equals("stony_shore")) {
            return "strand";
        }
        if (p.contains("snow") || p.contains("frozen") || p.contains("ice") || p.equals("grove") || p.equals("jagged_peaks")) {
            return "sneeuw";
        }
        if (p.contains("ocean") || p.equals("river")) {
            return "zee";
        }
        if (p.contains("desert") || p.contains("badlands")) {
            return "woestijn";
        }
        if (p.contains("flower") || p.equals("cherry_grove") || p.equals("meadow") || p.contains("sunflower")) {
            return "bloemen";
        }
        if (p.contains("mushroom")) {
            return "paddenstoel";
        }
        if (p.contains("jungle") || p.contains("bamboo")) {
            return "jungle";
        }
        if (p.contains("swamp")) {
            return "moeras";
        }
        if (p.contains("cave") || p.equals("deep_dark")) {
            return "grot";
        }
        if (p.contains("peaks") || p.contains("slopes") || p.contains("hills") || p.contains("windswept")) {
            return "bergen";
        }
        if (p.contains("forest") || p.contains("taiga") || p.contains("birch")) {
            return "bos";
        }
        if (p.contains("plains") || p.contains("savanna")) {
            return "wei";
        }
        return "overig";
    }

    // --- knuffel, liedje, speeltje, emote -------------------------------------------------------------------------------------

    /** Plush families: the colourful everyday guhs, the magic ones, the funny animal-ish ones. */
    public static String knuffelGroep(String knuffel) {
        return switch (knuffel) {
            case "ghost", "ender", "vahoege_ender", "koning", "wolk", "zeemeerguh", "starry" -> "sprookje";
            case "brontosaurus", "teckel", "pinguh", "kaasmoerasguh", "asguh" -> "beestje";
            default -> "kleurtje";
        };
    }

    private static String bron(String liedje) {
        int i = liedje.indexOf(':');
        return i < 0 ? liedje : liedje.substring(0, i);
    }

    /** Toys that are played the same way: running ones (ball, tunnel) and climbing/swinging ones (slide, seesaw). */
    public static String speeltjeGroep(String speeltje) {
        return switch (speeltje) {
            case "knabbelbal", "tunnel" -> "rennen";
            case "glijbaantje", "wip_schommel" -> "zwieren";
            default -> speeltje;
        };
    }

    /** The mood of an emote: bouncy ones and cosy ones. */
    public static String emoteGroep(String emote) {
        return switch (emote) {
            case "zwaaien", "dansen", "vahoeg", "rollen", "zingen", "knuffeldansje" -> "vrolijk";
            default -> "knus";
        };
    }

    // --- kleur ---------------------------------------------------------------------------------------------------------------

    /** The colour wheel: the neighbours of every colour (warm). */
    static final Map<String, List<String>> KLEUR_BUREN = Map.ofEntries(
            Map.entry("roze", List.of("rood", "paars", "wit")),
            Map.entry("rood", List.of("roze", "oranje", "bruin")),
            Map.entry("oranje", List.of("rood", "geel", "bruin", "goud")),
            Map.entry("geel", List.of("oranje", "goud", "groen")),
            Map.entry("groen", List.of("geel", "mint")),
            Map.entry("mint", List.of("groen", "blauw", "wit")),
            Map.entry("blauw", List.of("mint", "paars")),
            Map.entry("paars", List.of("blauw", "roze")),
            Map.entry("wit", List.of("roze", "mint", "zwart")),
            Map.entry("zwart", List.of("wit", "bruin")),
            Map.entry("bruin", List.of("oranje", "rood", "zwart")),
            Map.entry("goud", List.of("geel", "oranje")));

    // --- vriend ---------------------------------------------------------------------------------------------------------------

    private static boolean vrienden(MinecraftServer s, String a, String b) {
        try {
            return Vriendjes.vrienden(s, UUID.fromString(a), UUID.fromString(b));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
