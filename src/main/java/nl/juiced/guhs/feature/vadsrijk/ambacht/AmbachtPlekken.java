package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;

/**
 * WHERE the Ambachten live, as data. Nothing here is a fixed coordinate: every place is a POI id of the generated map
 * ({@code WereldPlekken}/{@code Z2WereldPlekken}/{@code Z3WereldPlekken.plek(id)}), so when t-wereld shifts a zone (Knuffelkamp
 * -384, zone 2 +704/+1344) the spots and workshops move along. The exact spot positions are found by scanning the terrain round the
 * anchor at runtime ({@link PlekWereld}: grass for herbs, rock for ore, beside a trunk for wood, shallow water by a shore for fish,
 * flowers nearby for hives), cached per anchor and re-scanned when the anchor moved.
 */
public final class AmbachtPlekken {
    /** How a group's spots are found in the terrain. */
    public enum Zoek {
        GROND, STEEN, BOOM, WATER, BLOEM,
        /** U-AMBACHT2: coral diving (zone 4): a sea floor under 2-8 blocks of water (the diver reaches it, and comes up again). */
        KORAAL
    }

    /**
     * A group of gathering spots: {@code aantal} spots within {@code straal} of the POI, each with {@link #POSITIES} possible
     * positions it moves between. {@code honing} = the honey kind (hives only).
     */
    public record Groep(String id, int zone, String poi, Ambacht ambacht, int tier, int aantal, int straal, Zoek zoek, String honing) {
        @Nullable
        public BlockPos anker() {
            return plek(zone, poi);
        }
    }

    /** A workshop/board guh in a hub: its NPC id, the hub's POI, the workshop (null = the Bestel- en Ambachtsbord). */
    public record Werkplaats(String npc, int zone, String poi, @Nullable Ambacht ambacht) {
        @Nullable
        public BlockPos anker() {
            return plek(zone, poi);
        }
    }

    public static final int POSITIES = 4;
    public static final List<Groep> GROEPEN = new ArrayList<>();
    public static final List<Werkplaats> WERKPLAATSEN = new ArrayList<>();

    static {
        // ---- zone 1 (T1): Klodderweide herbs, Sporenwoud wood, Druipgrotten ore, ponds + harbour fish; a starter patch at the camp
        g("z1_pluk_kamp", 1, "kampvuur_kamp", Ambacht.PLUKKEN, 1, 6, 30, Zoek.GROND);
        g("z1_pluk_weide_1", 1, "kampvuur_klodderweide_1", Ambacht.PLUKKEN, 1, 8, 40, Zoek.GROND);
        g("z1_pluk_weide_2", 1, "kampvuur_klodderweide_2", Ambacht.PLUKKEN, 1, 8, 40, Zoek.GROND);
        g("z1_pluk_weide_3", 1, "kampvuur_klodderweide_3", Ambacht.PLUKKEN, 1, 8, 40, Zoek.GROND);
        g("z1_hout_bos_1", 1, "kampvuur_sporenwoud_1", Ambacht.HOUTHAKKEN, 1, 8, 40, Zoek.BOOM);
        g("z1_hout_bos_2", 1, "kampvuur_sporenwoud_2", Ambacht.HOUTHAKKEN, 1, 8, 40, Zoek.BOOM);
        g("z1_hout_bos_3", 1, "kampvuur_sporenwoud_3", Ambacht.HOUTHAKKEN, 1, 8, 40, Zoek.BOOM);
        g("z1_mijn_grot_1", 1, "kampvuur_druipgrotten_1", Ambacht.MIJNEN, 1, 8, 36, Zoek.STEEN);
        g("z1_mijn_grot_2", 1, "kampvuur_druipgrotten_2", Ambacht.MIJNEN, 1, 8, 36, Zoek.STEEN);
        g("z1_mijn_grot_3", 1, "kampvuur_druipgrotten_3", Ambacht.MIJNEN, 1, 8, 36, Zoek.STEEN);
        g("z1_mijn_tempel", 1, "kampvuur_zwamtempel_1", Ambacht.MIJNEN, 1, 6, 36, Zoek.STEEN);
        g("z1_vis_vijver", 1, "vijver", Ambacht.VISSEN, 1, 5, 24, Zoek.WATER);
        g("z1_vis_waterval", 1, "waterval", Ambacht.VISSEN, 1, 5, 28, Zoek.WATER);
        g("z1_vis_haven", 1, "kampvuur_haven", Ambacht.VISSEN, 1, 6, 40, Zoek.WATER);
        g("z1_vis_druipmeer", 1, "druipschacht_meer", Ambacht.VISSEN, 1, 3, 16, Zoek.WATER);   // (a secret fishing spot)
        // ---- zone 2 (T2): Galmhellingen herbs, Kristalbos wood, Splinterkloof ore, Knuffelmeer fish
        g("z2_pluk_helling_1", 2, "kampvuur_galmhellingen_1", Ambacht.PLUKKEN, 2, 8, 40, Zoek.GROND);
        g("z2_pluk_helling_2", 2, "kampvuur_galmhellingen_2", Ambacht.PLUKKEN, 2, 8, 40, Zoek.GROND);
        g("z2_pluk_helling_3", 2, "kampvuur_galmhellingen_3", Ambacht.PLUKKEN, 2, 8, 40, Zoek.GROND);
        g("z2_hout_kristalbos_1", 2, "kristalbos_1", Ambacht.HOUTHAKKEN, 2, 8, 40, Zoek.BOOM);
        g("z2_hout_kristalbos_2", 2, "kristalbos_5", Ambacht.HOUTHAKKEN, 2, 8, 40, Zoek.BOOM);
        g("z2_hout_kristalbos_3", 2, "kristalbos_9", Ambacht.HOUTHAKKEN, 2, 8, 40, Zoek.BOOM);
        g("z2_mijn_kloof_1", 2, "kampvuur_splinterkloof_1", Ambacht.MIJNEN, 2, 8, 40, Zoek.STEEN);
        g("z2_mijn_kloof_2", 2, "kampvuur_splinterkloof_3", Ambacht.MIJNEN, 2, 8, 40, Zoek.STEEN);
        g("z2_mijn_kloof_3", 2, "kampvuur_splinterkloof_5", Ambacht.MIJNEN, 2, 8, 40, Zoek.STEEN);
        g("z2_mijn_ijsgrot", 2, "geheim_ijsgrot", Ambacht.MIJNEN, 2, 3, 16, Zoek.STEEN);   // (a secret vein)
        g("z2_vis_meer_1", 2, "kampvuur_knuffelmeer_1", Ambacht.VISSEN, 2, 6, 40, Zoek.WATER);
        g("z2_vis_meer_2", 2, "kampvuur_knuffelmeer_3", Ambacht.VISSEN, 2, 6, 40, Zoek.WATER);
        g("z2_vis_bron", 2, "warmwaterbron", Ambacht.VISSEN, 2, 4, 24, Zoek.WATER);
        // ---- zone 3 (T3): the Wortelvelden on the ground, the Wortelduister caves, the honey ponds; the bee meadows
        g("z3_pluk_kever", 3, "lichtkeverbos", Ambacht.PLUKKEN, 3, 8, 40, Zoek.GROND);
        g("z3_pluk_bloesemdal", 3, "paddenbloesemdal", Ambacht.PLUKKEN, 3, 8, 40, Zoek.GROND);
        g("z3_pluk_mosvoet", 3, "kampvuur_mosvoet", Ambacht.PLUKKEN, 3, 8, 40, Zoek.GROND);
        g("z3_hout_kever", 3, "lichtkeverbos", Ambacht.HOUTHAKKEN, 3, 8, 44, Zoek.BOOM);
        g("z3_hout_wortelweg", 3, "kampvuur_wortelweg_1", Ambacht.HOUTHAKKEN, 3, 8, 44, Zoek.BOOM);
        g("z3_hout_bloesemdal", 3, "paddenbloesemdal", Ambacht.HOUTHAKKEN, 3, 8, 44, Zoek.BOOM);
        g("z3_mijn_gewelf", 3, "wortelgewelf", Ambacht.MIJNEN, 3, 8, 40, Zoek.STEEN);
        g("z3_mijn_wortelgang_1", 3, "kampvuur_wortelgang_1", Ambacht.MIJNEN, 3, 8, 36, Zoek.STEEN);
        g("z3_mijn_wortelgang_2", 3, "kampvuur_wortelgang_2", Ambacht.MIJNEN, 3, 8, 36, Zoek.STEEN);
        g("z3_vis_moeras", 3, "honingmoeras", Ambacht.VISSEN, 3, 6, 40, Zoek.WATER);
        g("z3_vis_waterrad", 3, "mosvoet_waterrad", Ambacht.VISSEN, 3, 5, 32, Zoek.WATER);
        g("z3_vis_honingvijver", 3, "honingvijver_1", Ambacht.VISSEN, 3, 4, 24, Zoek.WATER);
        // ---- zone 4 (T4, De Lampionkust, t-z4wereld POIs; U-AMBACHT2): the islands' meadows, driftwood + kite bamboo, the cliffs;
        // fishing everywhere round the islands (every zone-4 catch also reports z4_vis/gevangen, T-INT) + coral diving in the Koraalbaai
        g("z4_pluk_bloemeneiland", 4, "kampvuur_bloemeneiland", Ambacht.PLUKKEN, 4, 8, 40, Zoek.GROND);
        g("z4_pluk_knuffeltij", 4, "kampvuur_knuffeltij", Ambacht.PLUKKEN, 4, 8, 40, Zoek.GROND);
        g("z4_pluk_strandfeest", 4, "kampvuur_strandfeest", Ambacht.PLUKKEN, 4, 6, 36, Zoek.GROND);
        g("z4_hout_vliegereiland", 4, "kampvuur_vliegereiland", Ambacht.HOUTHAKKEN, 4, 8, 44, Zoek.BOOM);
        g("z4_hout_vuurtoren", 4, "kampvuur_vuurtoreneiland", Ambacht.HOUTHAKKEN, 4, 6, 40, Zoek.BOOM);
        g("z4_hout_mysterie", 4, "kampvuur_mysterie", Ambacht.HOUTHAKKEN, 4, 6, 40, Zoek.BOOM);
        g("z4_mijn_pierkop", 4, "kampvuur_pierkop", Ambacht.MIJNEN, 4, 8, 36, Zoek.STEEN);
        g("z4_mijn_vuurtoren", 4, "eiland_vuurtoreneiland", Ambacht.MIJNEN, 4, 8, 40, Zoek.STEEN);
        g("z4_mijn_mysterie", 4, "mysterie_strand", Ambacht.MIJNEN, 4, 6, 36, Zoek.STEEN);
        g("z4_vis_haven", 4, "werkplaats_visplek", Ambacht.VISSEN, 4, 6, 32, Zoek.WATER);
        g("z4_vis_kade", 4, "steiger_2_eind", Ambacht.VISSEN, 4, 5, 28, Zoek.WATER);
        g("z4_vis_koraal", 4, "kampvuur_koraalbaai", Ambacht.VISSEN, 4, 6, 40, Zoek.WATER);
        g("z4_vis_wrak", 4, "boot_dok_wrak", Ambacht.VISSEN, 4, 5, 32, Zoek.WATER);
        g("z4_vis_vuurtoren", 4, "boot_dok_vuurtoreneiland", Ambacht.VISSEN, 4, 5, 32, Zoek.WATER);
        g("z4_duik_koraalbaai", 4, "duiksteiger", Ambacht.VISSEN, 4, 6, 30, Zoek.KORAAL);
        g("z4_duik_grotjes", 4, "eiland_koraalbaai", Ambacht.VISSEN, 4, 5, 36, Zoek.KORAAL);
        // ---- zone 5 (T5, Hoogvads on Moeder Wolkwal, t-z5wereld POIs; U-AMBACHT2): the gardens, the park trees, the whale's fins
        // (rock-hard skin), the pond, the market fountain and the blowhole
        g("z5_pluk_staartpark", 5, "staartpark_prieel", Ambacht.PLUKKEN, 5, 8, 36, Zoek.GROND);
        g("z5_pluk_ooggaarde", 5, "ooggaarde_tuin", Ambacht.PLUKKEN, 5, 8, 30, Zoek.GROND);
        g("z5_pluk_wolkbessen", 5, "wolkbes_3", Ambacht.PLUKKEN, 5, 6, 30, Zoek.GROND);
        g("z5_hout_staartpark_1", 5, "staartpark_boom_1", Ambacht.HOUTHAKKEN, 5, 6, 26, Zoek.BOOM);
        g("z5_hout_staartpark_2", 5, "staartpark_boom_3", Ambacht.HOUTHAKKEN, 5, 6, 26, Zoek.BOOM);
        g("z5_hout_ooggaarde", 5, "kampvuur_ooggaarde", Ambacht.HOUTHAKKEN, 5, 6, 30, Zoek.BOOM);
        g("z5_mijn_noordvin", 5, "z5w_noordvin", Ambacht.MIJNEN, 5, 8, 30, Zoek.STEEN);
        g("z5_mijn_zuidvin", 5, "z5w_zuidvin", Ambacht.MIJNEN, 5, 8, 30, Zoek.STEEN);
        g("z5_mijn_staartvin", 5, "staartvin_top", Ambacht.MIJNEN, 5, 6, 26, Zoek.STEEN);
        g("z5_vis_staartvijver", 5, "staartpark_vijver", Ambacht.VISSEN, 5, 5, 20, Zoek.WATER);
        g("z5_vis_fontein", 5, "markt_fontein", Ambacht.VISSEN, 5, 3, 12, Zoek.WATER);
        g("z5_vis_spuitgat", 5, "spuitgat_midden", Ambacht.VISSEN, 5, 4, 20, Zoek.WATER);
        // Imkeren (A3: own hives in the zones from zone 3 on; the bees fetch nectar from the flowers round them)
        korf("z3_korf_bloesemdal", "paddenbloesemdal", "bloesemhoning");
        korf("z3_korf_raatweide", "raatweide_plein", "schemerhoning");
        korf("z3_korf_kever", "lichtkeverbos", "wortelhoning");
        // U-AMBACHT2: zone 4 (the Bijenboerderij on the Bloemeneiland, the dunes by the beach party) and zone 5 (the Staartpark, the Ooggaarde)
        korf("z4_korf_bloemeneiland", 4, "bijenkorven", "lampionhoning");
        korf("z4_korf_duinen", 4, "kampvuur_strandfeest", "duinhoning");
        korf("z5_korf_staartpark", 5, "staartpark_prieel", "wolkenhoning");
        korf("z5_korf_ooggaarde", 5, "ooggaarde_tuin", "hemelhoning");

        // ---- the workshops (A3: smidse, naaiatelier, keuken, brouwerij, bijenstal) + the masters of the gathering ambachten
        w("amb_smid_z1", 1, "plein", Ambacht.SMEDEN);
        w("amb_naai_z1", 1, "plein", Ambacht.NAAIEN);
        w("amb_kok_z1", 1, "plein", Ambacht.KOKEN);
        w("amb_brouw_z1", 1, "plein", Ambacht.BROUWEN);
        w("amb_mijn_z1", 1, "plein", Ambacht.MIJNEN);
        w("amb_hout_z1", 1, "plein", Ambacht.HOUTHAKKEN);
        w("amb_pluk_z1", 1, "plein", Ambacht.PLUKKEN);
        w("amb_vis_z1", 1, "plein", Ambacht.VISSEN);
        w("amb_bord_z1", 1, "plein", null);
        w("amb_smid_z2", 2, "plein", Ambacht.SMEDEN);
        w("amb_naai_z2", 2, "plein", Ambacht.NAAIEN);
        w("amb_kok_z2", 2, "plein", Ambacht.KOKEN);
        w("amb_brouw_z2", 2, "plein", Ambacht.BROUWEN);
        w("amb_bord_z2", 2, "plein", null);
        w("amb_smid_z3", 3, "markt", Ambacht.SMEDEN);
        w("amb_naai_z3", 3, "markt", Ambacht.NAAIEN);
        w("amb_kok_z3", 3, "markt", Ambacht.KOKEN);
        w("amb_brouw_z3", 3, "markt", Ambacht.BROUWEN);
        w("amb_imker_z3", 3, "raatweide_plein", Ambacht.IMKEREN);
        w("amb_bord_z3", 3, "markt", null);
        // U-AMBACHT2: a board guh in Lampionhaven and in Hoogvads (by the Ambachtsgilde): Bestelbord, Opdrachtenbord, Knutselwinkel
        w("amb_bord_z4", 4, "plein", null);
        w("amb_bord_z5", 5, "gilde_ingang", null);
    }

    private AmbachtPlekken() {
    }

    private static void g(String id, int zone, String poi, Ambacht a, int tier, int aantal, int straal, Zoek z) {
        GROEPEN.add(new Groep(id, zone, poi, a, tier, aantal, straal, z, ""));
    }

    private static void korf(String id, String poi, String honing) {
        korf(id, 3, poi, honing);
    }

    /** A bee meadow of zone {@code zone} (tier = the zone: the hive level gate is {@link Imkeren#korfLevel}). */
    private static void korf(String id, int zone, String poi, String honing) {
        GROEPEN.add(new Groep(id, zone, poi, Ambacht.IMKEREN, zone, 4, 36, Zoek.BLOEM, honing));
    }

    /** The honey kinds that exist now (one per bee meadow, in order). */
    public static List<String> honingSoorten() {
        List<String> l = new ArrayList<>();
        for (Groep g : GROEPEN) {
            if (g.ambacht() == Ambacht.IMKEREN && !l.contains(g.honing())) {
                l.add(g.honing());
            }
        }
        return l;
    }

    /** The highest zone with gathering spots (the book pages loop to here). */
    public static int maxZone() {
        int z = 1;
        for (Groep g : GROEPEN) {
            z = Math.max(z, g.zone());
        }
        return z;
    }

    private static void w(String npc, int zone, String poi, @Nullable Ambacht a) {
        WERKPLAATSEN.add(new Werkplaats(npc, zone, poi, a));
    }

    /** A POI of zone 1..5 (null: not in this map). */
    @Nullable
    public static BlockPos plek(int zone, String id) {
        return switch (zone) {
            case 1 -> WereldPlekken.plek(id);
            case 2 -> Z2WereldPlekken.plek(id);
            case 3 -> Z3WereldPlekken.plek(id);
            case 4 -> nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.plek(id);   // T-INT
            case 5 -> nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken.plek(id);   // U-AMBACHT2
            default -> null;   // (zone 6 volgt: Zone6Haken)
        };
    }

    @Nullable
    public static Groep groep(String id) {
        for (Groep g : GROEPEN) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    public static Werkplaats werkplaats(String npc) {
        for (Werkplaats w : WERKPLAATSEN) {
            if (w.npc().equals(npc)) {
                return w;
            }
        }
        return null;
    }
}
