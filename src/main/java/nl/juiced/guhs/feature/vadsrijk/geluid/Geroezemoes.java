package nl.juiced.guhs.feature.vadsrijk.geluid;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * U-GEROEZEMOES: the soft guh crowd murmur of the busy places of Het Vadsrijk (pure logic, gametested; the player is
 * {@code geluid.client.GeroezemoesSpeler}). It REPLACES u-audio2's small "markt" murmur of {@link WereldBron} (that one only
 * played a guh take now and then at a cluster of 3+ NPCs) and keeps its budget rules: the one-shots share the world sounds'
 * global pause, the bed is soft and stays underneath the town music.
 * <ul>
 *   <li><b>Three beds</b> ({@code vr.geroezemoes.bed.<rustig|druk|markt>}): loops of many pitched, muffled existing guh takes
 *       (tools/vadsrijk/geluid/maak_geroezemoes.py). The intensity 0..1 picks the mix: {@link #bedMix}.</li>
 *   <li><b>One-shots</b> ({@code vr.geroezemoes.<soort>}): now and then a clear "njeg", a giggle, a guh, an "oeh?", a
 *       pieppiepmuisje squeak or a blub, at a guh of the crowd: {@link #kiesLos}, {@link #pauze}.</li>
 *   <li><b>Intensity</b> = how busy the PLACE is ({@link #PLEKKEN}: Hoogvads busiest, then the Saffraanpoort bazaar, the Kandijkom
 *       market, ...) x how many guhs are near x day/night: {@link #intensiteit}. Outside these places a group of 3+ NPC guhs still
 *       murmurs a little (what the old "markt" did).</li>
 * </ul>
 */
public final class Geroezemoes {
    public static final List<String> BEDDEN = List.of("rustig", "druk", "markt");
    public static final List<String> LOS = List.of("njeg", "lach", "guh", "oeh", "piep", "blub");
    /** How busy a place is (0..1). Order of the user: Hoogvads, Saffraanpoort, Kandijkom, Zwamzoet, Lampionhaven, Honingtak,
     *  Rozenkloof, Knuffelkamp (+ De Haven of zone 1, quieter). */
    public static final Map<String, Float> PLEKKEN = Map.of(
            "hoogvads", 1.0f, "saffraanpoort", 0.9f, "kandijkom", 0.8f, "zwamzoet", 0.72f, "lampionhaven", 0.66f,
            "honingtak", 0.6f, "rozenkloof", 0.54f, "knuffelkamp", 0.48f, "haven", 0.42f);
    /** A place that is not on the list but has a group of guhs (3+ within 6 blocks): a little murmur. */
    public static final float ELDERS = 0.35f;
    /** This many guhs within {@link #STRAAL} blocks = the place at its fullest. */
    public static final int VOL = 14;
    public static final double STRAAL = 22;
    public static final float NACHT = 0.4f;

    private Geroezemoes() {
    }

    public static String bedId(String bed) {
        return "vr.geroezemoes.bed." + bed;
    }

    public static String losId(String soort) {
        return "vr.geroezemoes." + soort;
    }

    /**
     * The busy place of a kern area (id + its town flag), or null. Hoogvads = every TOWN area of zone 5 (its wijken: Marktlaan,
     * Luchthaven, Kathedraal, ...).
     */
    @Nullable
    public static String plekVan(@Nullable String gebied, boolean stad) {
        if (gebied == null) {
            return null;
        }
        if (gebied.startsWith("z5_") && stad) {
            return "hoogvads";
        }
        return switch (gebied) {
            case "z6_saffraanpoort" -> "saffraanpoort";
            case "z2_kandijkom", "z2_kandijkom_dok" -> "kandijkom";
            case "zwamzoet", "knuffelkamp", "haven" -> gebied;
            case "z4_lampionhaven" -> "lampionhaven";
            case "z3_honingtak" -> "honingtak";
            case "z6_rozenkloof" -> "rozenkloof";
            default -> null;
        };
    }

    /** A market spot inside a place (the busiest bed may play there): Hoogvads' Marktlaan, the bazaar, the Kandijkom market. */
    public static boolean marktWijk(@Nullable String gebied) {
        return "z5_marktlaan".equals(gebied) || "z6_saffraanpoort".equals(gebied) || "z2_kandijkom".equals(gebied);
    }

    /**
     * The murmur intensity 0..1.
     * @param plek    the busy place (plekVan) or null
     * @param guhs    NPC guhs (story NPCs, townsfolk props, riders) within {@link #STRAAL} blocks
     * @param groep   there is a group of 3+ guhs within 6 blocks of each other
     * @param markt   standing in a market spot (marktWijk)
     * @param nacht   night (13000..23000)
     */
    public static float intensiteit(@Nullable String plek, int guhs, boolean groep, boolean markt, boolean nacht) {
        float basis;
        float drukte = Math.min(1f, Math.max(0, guhs) / (float) VOL);
        if (plek != null) {
            basis = PLEKKEN.getOrDefault(plek, ELDERS);
            // a town never goes completely silent (people indoors, a street further on), the guhs near you fill it up
            float i = basis * (0.3f + 0.7f * drukte);
            if (markt) {
                i = Math.min(1f, i * 1.15f + 0.05f);
            }
            return clamp(nacht ? i * NACHT : i);
        }
        if (!groep) {
            return 0f;
        }
        float i = ELDERS * (0.5f + 0.5f * drukte);
        return clamp(nacht ? i * NACHT : i);
    }

    /**
     * The three bed volumes (rustig, druk, markt) for an intensity: rustig alone up to 0.35, then it crossfades into druk
     * (0.35..0.65), then druk into markt (0.65..0.95). The total also grows with the intensity (soft at the quiet end).
     */
    public static float[] bedMix(float i) {
        i = clamp(i);
        float[] m = new float[3];
        if (i <= 0.001f) {
            return m;
        }
        float luid = 0.35f + 0.65f * i;   // the loudness follows the intensity, the density follows the bed
        if (i < 0.35f) {
            m[0] = 1;
            luid *= i / 0.35f;   // fades in from nothing
        } else if (i < 0.65f) {
            float t = (i - 0.35f) / 0.3f;
            m[0] = 1 - t;
            m[1] = t;
        } else {
            float t = Math.min(1f, (i - 0.65f) / 0.3f);
            m[1] = 1 - t;
            m[2] = t;
        }
        for (int k = 0; k < 3; k++) {
            m[k] *= luid;
        }
        return m;
    }

    /** Ticks until the next one-shot: ~25-40 s when quiet, ~8-14 s at a full market (r = 0..1). Never below the world pause. */
    public static int pauze(float i, double r) {
        i = clamp(i);
        double min = 500 - 340 * i, extra = 300 - 180 * i;
        return (int) Math.max(120, min + extra * r);
    }

    /** Which one-shot (r = 0..1): njeg 28%, guh 26%, giggle 18%, oeh 12%, squeak 9%, blub 7% (blub 18% on the Lampionkust). */
    public static String kiesLos(@Nullable String plek, double r) {
        double blub = "lampionhaven".equals(plek) ? 0.18 : 0.07;
        double[] kans = {0.28, 0.26, 0.18, 0.12, 0.09};
        double rest = 1 - blub;
        double schaal = rest / 0.93;
        double a = 0;
        for (int k = 0; k < kans.length; k++) {
            a += kans[k] * schaal;
            if (r < a) {
                return LOS.get(k);
            }
        }
        return "blub";
    }

    private static float clamp(float f) {
        return Math.max(0f, Math.min(1f, f));
    }
}
