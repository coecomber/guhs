package nl.juiced.guhs.feature.vadsrijk.geluid;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.stadsmuziek.StadsMuziek;

/**
 * T-GELUID: which music and which ambience belong where (pure lookups, gametested; the players are in {@code geluid.client}).
 * <ul>
 *   <li><b>Area themes</b> ({@code guhs:vr.geluid.gebied.<thema>}): one soft theme per wilderness area of zones 1-3, two for
 *       De Lampionkust (zone 4: day and evening) and two for Hoogvads (zone 5). Towns keep their own town tunes (stadsmuziek).</li>
 *   <li><b>Boss themes</b> ({@code guhs:vr.geluid.baas.<baas>}): by the boss's entity type (zones 1-3, the Grauwe Zeeslurf and
 *       de Vliegervanger of zone 4); anything else gets "algemeen".</li>
 *   <li><b>Stingers</b> ({@code guhs:vr.geluid.stinger.<soort>}): ontdekking, quest_klaar, level_op, baas_verslagen, geheim.</li>
 *   <li><b>Ambience</b>: a bed loop per area kind ({@code vr.geluid.bed.<bed>}) and now-and-then sounds
 *       ({@code vr.geluid.sfeer.<bed>}), crickets at night, caves underground. No strong wind anywhere (user wish).</li>
 * </ul>
 * Zones 4 and 5 are new: any area id starting with {@code z4_} / {@code z5_} counts, plus the W-3 boxes as a fallback (the
 * world layout of t-wereld: zone 4 x 1728..2559, z -768..-257; zone 5 above it from y 150, x 2432..3199).
 */
public final class GeluidPlan {
    private GeluidPlan() {
    }

    /** Every area theme (sound event name after {@code vr.geluid.gebied.}). */
    public static final List<String> GEBIED_THEMAS = List.of(
            "klodderweide", "sporenwoud", "druipgrotten", "zwamtempel",
            "knuffelmeer", "galmhellingen", "splinterkloof", "spiegelburcht",
            "wortelvelden", "wortelduister", "hart", "schorsstad", "stamspiraal", "takkenbanen", "kruinzee", "zeppelinwerf",
            "lampionkust_dag", "lampionkust_avond", "hoogvads", "hoogvads_markt",
            // U-Z6MUZIEK: zone 6, het Zijdeland (De Zoutvlakte has a night version)
            "roze_duinen", "palmenkloven", "zoutvlakte", "zoutvlakte_nacht", "ruinedal", "moederoase");

    /** Every boss theme (after {@code vr.geluid.baas.}). */
    public static final List<String> BAAS_THEMAS = List.of(
            "klodderreus", "koning_klodder", "opperzwam", "mika_spion",
            "boormeester", "galmwolf", "spiegelzucht",
            "baron_bleu", "nestwachter", "wortelwurger", "piepvel", "kruinroofvogel", "vahoegnir", "brokkel", "roquefort", "taleggio",
            "zeeslurf", "vliegervanger",
            // U-AUDIO2: the bosses that borrowed a theme (or got "algemeen") now have their own
            "mika_imker", "grauwe_droom", "donderbuik", "gorgonzola", "zilverstaart",
            // U-Z6MUZIEK: zone 6 - field bosses, the dungeon (De Verzonken Sterrenwacht), the raid (De Sterrendovende Toren)
            "wormelien", "zoutkolos", "sterrenwacht", "glimmertje", "verzonken_sterrenguh",
            "zandwachter", "spiegelvizier", "sterrenbeelden", "ringeling",
            "algemeen");

    /** The stingers (after {@code vr.geluid.stinger.}). */
    public static final List<String> STINGERS = List.of("ontdekking", "quest_klaar", "level_op", "baas_verslagen", "geheim",
            "baken", "moederoase");   // U-Z6MUZIEK: a star beacon lit, De Moederoase found

    /** The ambience beds (after {@code vr.geluid.bed.}); their now-and-then sounds are {@code vr.geluid.sfeer.<same name>}. */
    public static final List<String> BEDDEN = List.of("weide", "bos", "sporen", "grot", "tempel", "meer", "berg", "kloof", "wortel", "kruin",
            "honing", "kust", "krekels",
            "duinen", "oase", "bazaar", "sterren", "toren");   // U-Z6MUZIEK (soft: the dunes have only a sand hiss, no wind)

    /** An area "streek": which theme(s) play there and which ambience bed it has. */
    public record Streek(String id, List<String> themas, String bed) {
    }

    private static final Map<String, Streek> STREKEN = Map.ofEntries(
            s("klodderweide", "weide", "klodderweide"), s("ring", "weide", "klodderweide"),
            s("sporenwoud", "sporen", "sporenwoud"), s("druipgrotten", "grot", "druipgrotten"), s("zwamtempel", "tempel", "zwamtempel"),
            s("z2_knuffelmeer", "meer", "knuffelmeer"), s("z2_galmhellingen", "berg", "galmhellingen"), s("z2_ring", "berg", "galmhellingen"),
            s("z2_splinterkloof", "kloof", "splinterkloof"), s("z2_spiegelburcht", "tempel", "spiegelburcht"),
            s("z3_wortelvelden", "bos", "wortelvelden"), s("z3_ring", "bos", "wortelvelden"), s("z3_wortelduister", "wortel", "wortelduister"),
            s("z3_hart", "tempel", "hart"), s("z3_schorsstad", "bos", "schorsstad"), s("z3_stamspiraal", "bos", "stamspiraal"),
            s("z3_takkenbanen", "kruin", "takkenbanen"), s("z3_kruinzee", "kruin", "kruinzee"), s("z3_zeppelinwerf", "kruin", "zeppelinwerf"),
            // the towns: no area theme (their town tunes play), but zone 3's Honingtak gets the honey buzz on top of its town bed
            s("z3_honingtak", "honing"),
            // T-INT: the real zone-4 areas (t-z4wereld plan4.gebieden: z4_<island>, z4_wrak, z4_schildpad, z4_zee) and zone-5 areas
            // (t-z5wereld/t-z5wezens): own beds, the zone themes (day/evening resp. Hoogvads + the market waltz)
            z4("z4_lampionhaven", "kust"), z4("z4_knuffeltij", "kust"), z4("z4_strandfeest", "kust"), z4("z4_bloemeneiland", "weide"),
            z4("z4_koraalbaai", "meer"), z4("z4_vliegereiland", "weide"), z4("z4_vuurtoreneiland", "kust"), z4("z4_pierkop", "kust"),
            z4("z4_mysterie", "grot"), z4("z4_wrak", "grot"), z4("z4_schildpad", "kust"), z4("z4_zee", "kust"),
            z5("z5_staartvlakte", "weide"), z5("z5_wolkrand_noord", "weide"), z5("z5_wolkrand_zuid", "weide"), z5("z5_proefarena", "weide"),
            z5("z5_buikgangen", "grot"), z5("z5_schuilnest", "grot"), z5("z5d_droom", "weide"),   // (+ t-droom's Droomwolk)
            // U-Z6MUZIEK: zone 6, het Zijdeland (the ids of u-z6wereld Z6WereldPlekken.GEBIEDEN; the towns Rozenkloof and
            // Saffraanpoort play their town tunes via stadsmuziek; the grey tower has no music, only its drone)
            s("z6_roze_duinen", "duinen", "roze_duinen"), s("z6_palmenkloven", "oase", "palmenkloven"),
            s("z6_zoutvlakte", "duinen", "zoutvlakte", "zoutvlakte_nacht"), s("z6_ruinedal", "duinen", "ruinedal"),
            s("z6_moederoase", "oase", "moederoase"), s("z6_toren", "toren"), s("z6_zijdeland", "duinen", "roze_duinen"));

    private static Map.Entry<String, Streek> z4(String id, String bed) {
        return Map.entry(id, new Streek(id, List.of("lampionkust_dag", "lampionkust_avond"), bed));
    }

    private static Map.Entry<String, Streek> z5(String id, String bed) {
        return Map.entry(id, new Streek(id, List.of("hoogvads", "hoogvads_markt"), bed));
    }

    private static final Streek ZONE4 = new Streek("z4", List.of("lampionkust_dag", "lampionkust_avond"), "kust");
    private static final Streek ZONE5 = new Streek("z5", List.of("hoogvads", "hoogvads_markt"), "weide");
    /** U-Z6MUZIEK: any other zone-6 id (a dungeon, a cave, a map regen) and the zone-6 box. */
    private static final Streek ZONE6 = new Streek("z6", List.of("roze_duinen"), "duinen");

    private static Map.Entry<String, Streek> s(String id, String bed, String... themas) {
        return Map.entry(id, new Streek(id, List.of(themas), bed));
    }

    /** The streek of an area id (+ position, for the zone-4/5 fallback boxes); null = no music, no extra ambience. */
    @Nullable
    public static Streek streek(@Nullable String gebied, double x, double y, double z) {
        if (gebied != null) {
            Streek s = STREKEN.get(gebied);
            if (s != null) {
                return s;
            }
            if (gebied.startsWith("z4_")) {
                return ZONE4;
            }
            if (gebied.startsWith("z5_")) {
                return ZONE5;
            }
            if (gebied.startsWith("z6") && StadsMuziek.stadVan(gebied) == null) {   // U-Z6MUZIEK (z6_, z6raid_, ...)
                return ZONE6;
            }
            if (StadsMuziek.stadVan(gebied) != null) {
                return null;
            }
            // a new area of zone 1-3 (a map regen, a dungeon): the zone's general streek
            String zone = gebied.startsWith("z3_") ? "z3_ring" : gebied.startsWith("z2_") ? "z2_ring" : null;
            if (zone != null) {
                return STREKEN.get(zone);
            }
        }
        if (inZone5(x, y, z)) {
            return ZONE5;
        }
        if (inZone4(x, z)) {
            return ZONE4;
        }
        if (inZone6(x, z)) {
            return ZONE6;
        }
        return null;
    }

    /** The streek at a position (kern areas, layers included). */
    @Nullable
    public static Streek streekOp(double x, double y, double z) {
        Gebieden.Gebied g = Gebieden.op(x, y, z);
        return streek(g == null ? null : g.id(), x, y, z);
    }

    /** The area themes of a streek that is not a town (empty in towns and outside the Vadsrijk areas). */
    public static List<String> themas(@Nullable Streek s) {
        return s == null ? List.of() : s.themas();
    }

    public static boolean inZone4(double x, double z) {
        return x >= 1728 && x < 2560 && z >= -768 && z < -256;
    }

    public static boolean inZone5(double x, double y, double z) {
        return y >= 150 && x >= 2432 && x < 3200 && z >= -768 && z < -256;
    }

    /** U-Z6MUZIEK: the Zijdeland box (u-z6wereld z6_zijdeland: x 3264..4287, z -1152..-385, widened to the coast at x 3236). */
    public static boolean inZone6(double x, double z) {
        return x >= 3236 && x < 4288 && z >= -1152 && z < -384;
    }

    /** The boss theme for a boss entity type path ("vr_z3_baas_bleu" -> "baron_bleu"); "algemeen" for unknown bosses. */
    public static String baasThema(@Nullable String typePad) {
        if (typePad == null || typePad.isEmpty()) {
            return "algemeen";
        }
        String p = typePad;
        if (p.contains("zeeslurf")) {
            return "zeeslurf";
        }
        if (p.contains("vliegervanger")) {
            return "vliegervanger";
        }
        if (p.contains("imker_mika") || p.equals("vr_z3_imker")) {   // U-AUDIO2: vr_z3_imker_mika
            return "mika_imker";
        }
        if (p.startsWith("vr_z6raid_baas_ster_")) {   // U-Z6MUZIEK: the three constellations share one theme
            return "sterrenbeelden";
        }
        for (String pre : new String[] {"vr_z6raid_baas_", "vr_z6_baas_", "vr_z5_baas_", "vr_z4_baas_", "vr_z3_baas_", "vr_z2_baas_", "vr_baas_", "vr_"}) {
            if (p.startsWith(pre)) {
                p = p.substring(pre.length());
                break;
            }
        }
        return switch (p) {
            case "bleu" -> "baron_bleu";
            case "spiegelbeeld" -> "spiegelzucht";
            case "kk_klodder" -> "koning_klodder";
            case "astrolabiumwachter" -> "sterrenwacht";   // U-Z6MUZIEK (u-z6wezens Z6Muziek uses the same names)
            case "sterrenguh" -> "verzonken_sterrenguh";
            default -> BAAS_THEMAS.contains(p) ? p : "algemeen";
        };
    }

    /** Is this bed one with birds (at night it gives way to the crickets)? */
    public static boolean vogelBed(String bed) {
        return switch (bed) {
            case "weide", "bos", "sporen", "kruin", "berg", "honing", "duinen" -> true;   // (U-Z6MUZIEK: the dunes' day bed too)
            default -> false;
        };
    }

    /** Is this a bed under the ground / indoors (no crickets, no birds)? */
    public static boolean binnenBed(String bed) {
        return switch (bed) {
            case "grot", "wortel", "tempel", "toren" -> true;   // (U-Z6MUZIEK: the grey tower: only its drone, day and night)
            default -> false;
        };
    }

    /** U-Z6MUZIEK: the night bed for this bed: the Zijdeland gets crickets + star twinkles + singing salt, the rest crickets. */
    public static String nachtBed(@Nullable String bed) {
        return bed != null && switch (bed) {
            case "duinen", "oase", "sterren" -> true;
            default -> false;
        } ? "sterren" : "krekels";
    }

    /**
     * U-Z6MUZIEK: area themes with a night version ("x" + "x_nacht", De Zoutvlakte): which one plays at this time of day.
     * Returns null when the list has no night pair.
     */
    @Nullable
    public static String dagNacht(List<String> themas, boolean nacht) {
        for (String t : themas) {
            if (t.endsWith("_nacht") && themas.contains(t.substring(0, t.length() - 6))) {
                return nacht ? t : t.substring(0, t.length() - 6);
            }
        }
        return null;
    }

    // ---- U-Z6MUZIEK: boss music ids of other slices that map onto our themes ---------------------------------------------

    /** A foreign boss music id read as one of our themes + whether it names the boss's last (intense) phase. */
    public record Alias(String thema, boolean laatsteFase) {
    }

    /**
     * u-z6raid's {@code Z6RaidMuziek} ids: {@code guhs:vr.z6raid.muziek.<baas>} (phase 1) and {@code <baas>_2} / Ringeling's
     * {@code _3}: they all play our {@code vr.geluid.baas.<baas>} theme; the last numbered phase (Ringeling 3, the others 2;
     * for the constellations "_2" = their re-light window) crossfades into its {@code _fase} loop, a lower one back out.
     * Null for anything else.
     */
    @Nullable
    public static Alias alias(@Nullable String id) {
        if (id == null) {
            return null;
        }
        int i = id.indexOf("vr.z6raid.muziek.");
        if (i < 0) {
            return null;
        }
        String r = id.substring(i + "vr.z6raid.muziek.".length());
        int fase = 1;
        int u = r.lastIndexOf('_');
        if (u > 0 && r.substring(u + 1).chars().allMatch(Character::isDigit) && u + 1 < r.length()) {
            fase = Integer.parseInt(r.substring(u + 1));
            r = r.substring(0, u);
        }
        if (!BAAS_THEMAS.contains(r) || r.equals("algemeen")) {
            return null;
        }
        int laatste = r.equals("ringeling") ? 3 : 2;
        return new Alias(r, fase >= laatste);
    }

    // ---- sound ids
    public static String gebiedId(String thema) {
        return "vr.geluid.gebied." + thema;
    }

    public static String baasId(String thema) {
        return "vr.geluid.baas." + thema;
    }

    /** U-AUDIO2: the last-phase loop of a boss theme (same notes + the intensified layers, same length: crossfaded in). */
    public static String faseId(String thema) {
        return "vr.geluid.baas." + thema + "_fase";
    }

    /** U-AUDIO2: the boss's own short victory fanfare. */
    public static String fanfareId(String thema) {
        return "vr.geluid.fanfare." + thema;
    }

    /** U-AUDIO2: the theme name inside a boss sound id ("guhs:vr.geluid.baas.zeeslurf" -> "zeeslurf"), or null for a foreign id. */
    @Nullable
    public static String themaVanId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        int i = id.indexOf("vr.geluid.baas.");
        if (i < 0) {
            Alias a = alias(id);   // U-Z6MUZIEK: the raid's own ids
            return a == null ? null : a.thema();
        }
        String t = id.substring(i + "vr.geluid.baas.".length());
        return BAAS_THEMAS.contains(t) ? t : null;
    }

    public static String stingerId(String soort) {
        return "vr.geluid.stinger." + soort;
    }

    public static String bedId(String bed) {
        return "vr.geluid.bed." + bed;
    }

    public static String sfeerId(String bed) {
        return "vr.geluid.sfeer." + bed;
    }
}
