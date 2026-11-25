package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * A group of gathering spots: {@code aantal} spots of kind {@code soort} (its ambacht: {@link NodeSoort#ambacht}) within
     * {@code straal} of the POI, each with {@link #POSITIES} possible positions it moves between. {@code honing} = the honey kind
     * (hives only). {@code leegMin}/{@code leegMax}: seconds a harvested spot stays empty (1.10.4: per group; 60-120 by default,
     * the Ambachtseiland 20-30).
     */
    public record Groep(String id, int zone, String poi, NodeSoort soort, int tier, int aantal, int straal, Zoek zoek, String honing, int leegMin,
            int leegMax) {
        @Nullable
        public BlockPos anker() {
            return plek(zone, poi);
        }

        /** A group with the default empty time (60-120 s). */
        public Groep(String id, int zone, String poi, NodeSoort soort, int tier, int aantal, int straal, Zoek zoek, String honing) {
            this(id, zone, poi, soort, tier, aantal, straal, zoek, honing, LEEG_MIN, LEEG_MAX);
        }

        /** The ambacht that levels on this group (Verzamelen or Vissen). */
        public Ambacht ambacht() {
            return soort.ambacht;
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
    /** The default empty time of a harvested spot (seconds). */
    public static final int LEEG_MIN = 60, LEEG_MAX = 120;
    public static final List<Groep> GROEPEN = new ArrayList<>();
    public static final List<Werkplaats> WERKPLAATSEN = new ArrayList<>();
    /** 1.10.4: workshop guhs that stand exactly on their POI (the Ambachtseiland huts) instead of in a ring round a hub square. */
    public static final java.util.Set<String> VAST = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /**
     * 1.10.4: workshop guh -> the POI (same zone) it looks at when it is placed (the islet's hut masters face the square: {@link #kijk});
     * without one a guh looks at its hub's centre.
     */
    public static final Map<String, String> KIJK = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        // ---- zone 1 (T1): Klodderweide herbs, Sporenwoud wood, Druipgrotten ore, ponds + harbour fish; a starter patch at the camp
        g("z1_pluk_kamp", 1, "kampvuur_kamp", NodeSoort.KRUID, 1, 6, 30, Zoek.GROND);
        g("z1_pluk_weide_1", 1, "kampvuur_klodderweide_1", NodeSoort.KRUID, 1, 8, 40, Zoek.GROND);
        g("z1_pluk_weide_2", 1, "kampvuur_klodderweide_2", NodeSoort.KRUID, 1, 8, 40, Zoek.GROND);
        g("z1_pluk_weide_3", 1, "kampvuur_klodderweide_3", NodeSoort.KRUID, 1, 8, 40, Zoek.GROND);
        g("z1_hout_bos_1", 1, "kampvuur_sporenwoud_1", NodeSoort.HOUT, 1, 8, 40, Zoek.BOOM);
        g("z1_hout_bos_2", 1, "kampvuur_sporenwoud_2", NodeSoort.HOUT, 1, 8, 40, Zoek.BOOM);
        g("z1_hout_bos_3", 1, "kampvuur_sporenwoud_3", NodeSoort.HOUT, 1, 8, 40, Zoek.BOOM);
        g("z1_mijn_grot_1", 1, "kampvuur_druipgrotten_1", NodeSoort.ERTS, 1, 8, 36, Zoek.STEEN);
        g("z1_mijn_grot_2", 1, "kampvuur_druipgrotten_2", NodeSoort.ERTS, 1, 8, 36, Zoek.STEEN);
        g("z1_mijn_grot_3", 1, "kampvuur_druipgrotten_3", NodeSoort.ERTS, 1, 8, 36, Zoek.STEEN);
        g("z1_mijn_tempel", 1, "kampvuur_zwamtempel_1", NodeSoort.ERTS, 1, 6, 36, Zoek.STEEN);
        g("z1_vis_vijver", 1, "vijver", NodeSoort.VIS, 1, 5, 24, Zoek.WATER);
        g("z1_vis_waterval", 1, "waterval", NodeSoort.VIS, 1, 5, 28, Zoek.WATER);
        g("z1_vis_haven", 1, "haven_z1_kamp_vertrek", NodeSoort.VIS, 1, 6, 40, Zoek.WATER);   // 1.10.12: the quay (the campfire stands in the village, 40+ blocks from the water)
        g("z1_vis_druipmeer", 1, "druipschacht_meer", NodeSoort.VIS, 1, 3, 16, Zoek.WATER);   // (a secret fishing spot)
        // ---- zone 2 (T2): Galmhellingen herbs, Kristalbos wood, Splinterkloof ore, Knuffelmeer fish
        g("z2_pluk_helling_1", 2, "kampvuur_galmhellingen_1", NodeSoort.KRUID, 2, 8, 40, Zoek.GROND);
        g("z2_pluk_helling_2", 2, "kampvuur_galmhellingen_2", NodeSoort.KRUID, 2, 8, 40, Zoek.GROND);
        g("z2_pluk_helling_3", 2, "kampvuur_galmhellingen_3", NodeSoort.KRUID, 2, 8, 40, Zoek.GROND);
        g("z2_hout_kristalbos_1", 2, "kristalbos_1", NodeSoort.HOUT, 2, 8, 40, Zoek.BOOM);
        g("z2_hout_kristalbos_2", 2, "kristalbos_5", NodeSoort.HOUT, 2, 8, 40, Zoek.BOOM);
        g("z2_hout_kristalbos_3", 2, "kristalbos_9", NodeSoort.HOUT, 2, 8, 40, Zoek.BOOM);
        g("z2_mijn_kloof_1", 2, "kampvuur_splinterkloof_1", NodeSoort.ERTS, 2, 8, 40, Zoek.STEEN);
        g("z2_mijn_kloof_2", 2, "kampvuur_splinterkloof_3", NodeSoort.ERTS, 2, 8, 40, Zoek.STEEN);
        g("z2_mijn_kloof_3", 2, "kampvuur_splinterkloof_5", NodeSoort.ERTS, 2, 8, 40, Zoek.STEEN);
        g("z2_mijn_ijsgrot", 2, "geheim_ijsgrot", NodeSoort.ERTS, 2, 3, 16, Zoek.STEEN);   // (a secret vein)
        g("z2_vis_meer_1", 2, "kampvuur_knuffelmeer_1", NodeSoort.VIS, 2, 6, 40, Zoek.WATER);
        g("z2_vis_meer_2", 2, "kampvuur_knuffelmeer_3", NodeSoort.VIS, 2, 6, 40, Zoek.WATER);
        g("z2_vis_bron", 2, "warmwaterbron", NodeSoort.VIS, 2, 4, 24, Zoek.WATER);
        // ---- zone 3 (T3): the Wortelvelden on the ground, the Wortelduister caves, the honey ponds; the bee meadows
        g("z3_pluk_kever", 3, "lichtkeverbos", NodeSoort.KRUID, 3, 8, 40, Zoek.GROND);
        g("z3_pluk_bloesemdal", 3, "paddenbloesemdal", NodeSoort.KRUID, 3, 8, 40, Zoek.GROND);
        g("z3_pluk_mosvoet", 3, "kampvuur_mosvoet", NodeSoort.KRUID, 3, 8, 40, Zoek.GROND);
        g("z3_hout_kever", 3, "lichtkeverbos", NodeSoort.HOUT, 3, 8, 44, Zoek.BOOM);
        g("z3_hout_wortelweg", 3, "kampvuur_wortelweg_1", NodeSoort.HOUT, 3, 8, 44, Zoek.BOOM);
        g("z3_hout_bloesemdal", 3, "paddenbloesemdal", NodeSoort.HOUT, 3, 8, 44, Zoek.BOOM);
        g("z3_mijn_gewelf", 3, "wortelgewelf", NodeSoort.ERTS, 3, 8, 40, Zoek.STEEN);
        g("z3_mijn_wortelgang_1", 3, "kampvuur_wortelgang_1", NodeSoort.ERTS, 3, 8, 36, Zoek.STEEN);
        g("z3_mijn_wortelgang_2", 3, "kampvuur_wortelgang_2", NodeSoort.ERTS, 3, 8, 36, Zoek.STEEN);
        g("z3_vis_moeras", 3, "honingmoeras", NodeSoort.VIS, 3, 6, 40, Zoek.WATER);
        g("z3_vis_waterrad", 3, "mosvoet_waterrad", NodeSoort.VIS, 3, 5, 100, Zoek.WATER);   // 1.10.19: the wheel is dry: the brook lies 74-100 blocks south
        g("z3_vis_honingvijver", 3, "honingvijver_1", NodeSoort.VIS, 3, 4, 24, Zoek.WATER);
        // ---- zone 4 (T4, De Lampionkust, t-z4wereld POIs; U-AMBACHT2): the islands' meadows, driftwood + kite bamboo, the cliffs;
        // fishing everywhere round the islands (every zone-4 catch also reports z4_vis/gevangen, T-INT) + coral diving in the Koraalbaai
        g("z4_pluk_bloemeneiland", 4, "kampvuur_bloemeneiland", NodeSoort.KRUID, 4, 8, 40, Zoek.GROND);
        g("z4_pluk_knuffeltij", 4, "kampvuur_knuffeltij", NodeSoort.KRUID, 4, 8, 40, Zoek.GROND);
        g("z4_pluk_strandfeest", 4, "kampvuur_strandfeest", NodeSoort.KRUID, 4, 6, 36, Zoek.GROND);
        g("z4_hout_vliegereiland", 4, "kampvuur_vliegereiland", NodeSoort.HOUT, 4, 8, 44, Zoek.BOOM);
        g("z4_hout_vuurtoren", 4, "kampvuur_vuurtoreneiland", NodeSoort.HOUT, 4, 6, 40, Zoek.BOOM);
        g("z4_hout_mysterie", 4, "kampvuur_mysterie", NodeSoort.HOUT, 4, 6, 40, Zoek.BOOM);
        g("z4_mijn_pierkop", 4, "kampvuur_pierkop", NodeSoort.ERTS, 4, 8, 36, Zoek.STEEN);
        g("z4_mijn_vuurtoren", 4, "eiland_vuurtoreneiland", NodeSoort.ERTS, 4, 8, 40, Zoek.STEEN);
        g("z4_mijn_mysterie", 4, "mysterie_strand", NodeSoort.ERTS, 4, 6, 36, Zoek.STEEN);
        g("z4_vis_haven", 4, "werkplaats_visplek", NodeSoort.VIS, 4, 6, 32, Zoek.WATER);
        g("z4_vis_kade", 4, "steiger_2_eind", NodeSoort.VIS, 4, 5, 28, Zoek.WATER);
        g("z4_vis_koraal", 4, "kampvuur_koraalbaai", NodeSoort.VIS, 4, 6, 40, Zoek.WATER);
        g("z4_vis_wrak", 4, "boot_dok_wrak", NodeSoort.VIS, 4, 5, 32, Zoek.WATER);
        g("z4_vis_vuurtoren", 4, "boot_dok_vuurtoreneiland", NodeSoort.VIS, 4, 5, 32, Zoek.WATER);
        g("z4_duik_koraalbaai", 4, "duiksteiger", NodeSoort.DUIK, 4, 6, 30, Zoek.KORAAL);
        g("z4_duik_grotjes", 4, "eiland_koraalbaai", NodeSoort.DUIK, 4, 5, 36, Zoek.KORAAL);
        // ---- zone 5 (T5, Hoogvads on Moeder Wolkwal, t-z5wereld POIs; U-AMBACHT2): the gardens, the park trees, the whale's fins
        // (rock-hard skin), the pond, the market fountain and the blowhole
        g("z5_pluk_staartpark", 5, "staartpark_prieel", NodeSoort.KRUID, 5, 8, 36, Zoek.GROND);
        g("z5_pluk_ooggaarde", 5, "ooggaarde_tuin", NodeSoort.KRUID, 5, 8, 30, Zoek.GROND);
        g("z5_pluk_wolkbessen", 5, "wolkbes_3", NodeSoort.KRUID, 5, 6, 30, Zoek.GROND);
        g("z5_hout_staartpark_1", 5, "staartpark_boom_1", NodeSoort.HOUT, 5, 6, 26, Zoek.BOOM);
        g("z5_hout_staartpark_2", 5, "staartpark_boom_3", NodeSoort.HOUT, 5, 6, 26, Zoek.BOOM);
        g("z5_hout_ooggaarde", 5, "kampvuur_ooggaarde", NodeSoort.HOUT, 5, 6, 30, Zoek.BOOM);
        g("z5_mijn_noordvin", 5, "z5w_noordvin", NodeSoort.ERTS, 5, 8, 30, Zoek.STEEN);
        g("z5_mijn_zuidvin", 5, "z5w_zuidvin", NodeSoort.ERTS, 5, 8, 30, Zoek.STEEN);
        g("z5_mijn_staartvin", 5, "staartvin_top", NodeSoort.ERTS, 5, 6, 26, Zoek.STEEN);
        g("z5_vis_staartvijver", 5, "staartpark_vijver", NodeSoort.VIS, 5, 5, 20, Zoek.WATER);
        g("z5_vis_fontein", 5, "markt_fontein", NodeSoort.VIS, 5, 3, 12, Zoek.WATER);
        g("z5_vis_spuitgat", 5, "spuitgat_midden", NodeSoort.VIS, 5, 4, 20, Zoek.WATER);
        // Imkeren (A3: own hives in the zones from zone 3 on; the bees fetch nectar from the flowers round them)
        korf("z3_korf_bloesemdal", "paddenbloesemdal", "bloesemhoning");
        korf("z3_korf_raatweide", "raatweide_plein", "schemerhoning");
        korf("z3_korf_kever", "lichtkeverbos", "wortelhoning");
        // U-AMBACHT2: zone 4 (the Bijenboerderij on the Bloemeneiland, the dunes by the beach party) and zone 5 (the Staartpark, the Ooggaarde)
        korf("z4_korf_bloemeneiland", 4, "bijenkorven", "lampionhoning");
        korf("z4_korf_duinen", 4, "kampvuur_strandfeest", "duinhoning");
        korf("z5_korf_staartpark", 5, "staartpark_prieel", "wolkenhoning");
        korf("z5_korf_ooggaarde", 5, "ooggaarde_tuin", "hemelhoning");

        // ---- the workshops (1.10.4: one per ambacht: smidse, keuken + the masters of Verzamelen and Vissen in Zwamzoet) + the boards
        w("amb_smid_z1", 1, "plein", Ambacht.SMEDEN);
        w("amb_kok_z1", 1, "plein", Ambacht.KOKEN);
        w("amb_verzamel_z1", 1, "plein", Ambacht.VERZAMELEN);
        w("amb_vis_z1", 1, "plein", Ambacht.VISSEN);
        w("amb_bord_z1", 1, "plein", null);
        w("amb_smid_z2", 2, "plein", Ambacht.SMEDEN);
        w("amb_kok_z2", 2, "plein", Ambacht.KOKEN);
        w("amb_bord_z2", 2, "plein", null);
        w("amb_smid_z3", 3, "markt", Ambacht.SMEDEN);
        w("amb_kok_z3", 3, "markt", Ambacht.KOKEN);
        w("amb_bord_z3", 3, "markt", null);
        // U-AMBACHT2: a board guh in Lampionhaven and in Hoogvads (by the Ambachtsgilde): Bestelbord, Opdrachtenbord, Knutselwinkel
        w("amb_bord_z4", 4, "plein", null);
        w("amb_bord_z5", 5, "gilde_ingang", null);
    }

    private AmbachtPlekken() {
    }

    private static void g(String id, int zone, String poi, NodeSoort s, int tier, int aantal, int straal, Zoek z) {
        voeg(new Groep(id, zone, poi, s, tier, aantal, straal, z, "", LEEG_MIN, LEEG_MAX));
    }

    private static void korf(String id, String poi, String honing) {
        korf(id, 3, poi, honing);
    }

    /** A bee meadow of zone {@code zone} (tier = the zone: the hive level gate is {@link Imkeren#korfLevel}). */
    private static void korf(String id, int zone, String poi, String honing) {
        voeg(new Groep(id, zone, poi, NodeSoort.KORF, zone, 4, 36, Zoek.BLOEM, honing, LEEG_MIN, LEEG_MAX));
    }

    private static synchronized void voeg(Groep g) {
        if (groep(g.id()) != null) {
            throw new IllegalArgumentException("ambacht group id twice: " + g.id());
        }
        GROEPEN.add(g);
    }

    // ---- 1.10.4: the API for other slices (the Ambachtseiland adds its gathering groups at its own POIs) ---------------------------

    /**
     * Adds a gathering group at a POI of another slice (call it from your registry's register, before players join): {@code aantal}
     * spots of {@code soort} (Verzamelen: ERTS/HOUT/KRUID, Vissen: VIS/DUIK; hives: {@link #korfGroep}) of tier {@code tier} within
     * {@code straal} blocks of POI {@code poi} of zone {@code zone} (resolved by {@link #plek}, so it follows the map), found in
     * the terrain the soort's default way ({@link NodeSoort#zoek}); a harvested spot comes back after {@code leegMin}-{@code leegMax}
     * seconds. The group shows up in the Verkenningsboek page and the Plekken tab by itself; name it with the lang key
     * {@code vadsrijk.ambacht.groep.<id>}. Throws on a duplicate id. Returns the group.
     */
    public static Groep groep(String id, int zone, String poi, NodeSoort soort, int tier, int aantal, int straal, int leegMin, int leegMax) {
        return groep(id, zone, poi, soort, tier, aantal, straal, soort.zoek, leegMin, leegMax);
    }

    /** {@link #groep(String, int, String, NodeSoort, int, int, int, int, int)} with an explicit terrain search. */
    public static Groep groep(String id, int zone, String poi, NodeSoort soort, int tier, int aantal, int straal, Zoek zoek, int leegMin, int leegMax) {
        if (soort == NodeSoort.KORF) {
            throw new IllegalArgumentException("hives need a honey kind: use korfGroep");
        }
        Groep g = new Groep(id, zone, poi, soort, Math.max(1, Math.min(AmbachtXp.MAX_TIER, tier)), Math.max(1, aantal), Math.max(4, straal), zoek, "",
                Math.max(5, leegMin), Math.max(Math.max(5, leegMin), leegMax));
        voeg(g);
        return g;
    }

    /** A bee meadow at a POI (honey kind {@code honing}: a material id of {@link Materialen} with kind KORF). */
    public static Groep korfGroep(String id, int zone, String poi, int tier, String honing, int aantal, int straal) {
        Groep g = new Groep(id, zone, poi, NodeSoort.KORF, Math.max(1, Math.min(AmbachtXp.MAX_TIER, tier)), Math.max(1, aantal), Math.max(4, straal),
                Zoek.BLOEM, honing, LEEG_MIN, LEEG_MAX);
        voeg(g);
        return g;
    }

    /**
     * 1.10.4: adds a workshop guh of another slice: {@code npc} (lang {@code vadsrijk.npc.naam.<npc>}, role from the ambacht: the id
     * must read {@code amb_<rol>_...}), at POI {@code poi} of zone {@code zone}; {@code vast} = exactly on that POI (else in the ring
     * round it, like the hub masters). Throws on a duplicate id.
     */
    public static synchronized Werkplaats werkplaats(String npc, int zone, String poi, @Nullable Ambacht a, boolean vast) {
        if (werkplaats(npc) != null) {
            throw new IllegalArgumentException("ambacht workshop id twice: " + npc);
        }
        Werkplaats w = new Werkplaats(npc, zone, poi, a);
        WERKPLAATSEN.add(w);
        if (vast) {
            VAST.add(npc);
        }
        return w;
    }

    /** 1.10.4: workshop guh {@code npc} looks at POI {@code poi} of its own zone when it is placed (also after every restart). */
    public static void kijk(String npc, String poi) {
        KIJK.put(npc, poi);
    }

    /** Where workshop guh {@code w} looks: its {@link #KIJK} POI, else its hub's centre (null: not in this map). */
    @Nullable
    public static BlockPos kijkNaar(Werkplaats w) {
        String k = KIJK.get(w.npc());
        BlockPos b = k == null ? null : plek(w.zone(), k);
        return b != null ? b : w.anker();
    }

    /** The honey kinds that exist now (one per bee meadow, in order). */
    public static List<String> honingSoorten() {
        List<String> l = new ArrayList<>();
        for (Groep g : GROEPEN) {
            if (g.soort() == NodeSoort.KORF && !l.contains(g.honing())) {
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

    /** A POI of zone 1..6 (null: not in this map). */
    @Nullable
    public static BlockPos plek(int zone, String id) {
        if (id.startsWith("grot_")) {
            return nl.juiced.guhs.feature.vadsrijk.grotten.Grotten.plek(id);   // U-GROTTEN: the cave spots (any zone)
        }
        return switch (zone) {
            case 1 -> WereldPlekken.plek(id);
            case 2 -> Z2WereldPlekken.plek(id);
            case 3 -> Z3WereldPlekken.plek(id);
            case 4 -> nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.plek(id);   // T-INT
            case 5 -> nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken.plek(id);   // U-AMBACHT2
            case 6 -> nl.juiced.guhs.feature.vadsrijk.zone6.wereld.Z6WereldPlekken.plek(id);   // 1.10.4: T6 (Zone6Haken)
            default -> null;
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
