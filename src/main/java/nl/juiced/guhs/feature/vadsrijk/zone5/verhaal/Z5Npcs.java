package nl.juiced.guhs.feature.vadsrijk.zone5.verhaal;

import static nl.juiced.guhs.entity.GuhClothes.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef.Gedrag;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPlekken;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;

/**
 * The NPC guhs of Hoogvads (zone 5): the queen, the Hoge Raad (the four suspects), the inspector, the Wolkwacht, the refugees in
 * the Grote Soepkeuken, the professor, merchants, the Ambachtsgilde and the two 4th-class teasers. They join zone 1's NPC keeper
 * ({@link Npcs#voegToe}). Names, lines and "where" texts: tools/features/vadsrijk_t_z5verhaal.py; spots: {@link Z5VerhaalPlekken}.
 * <p>Roles: {@code luchtschip_z5} (the havenmeester: travel), {@code z5_bewijsbord} (the inspector), {@code z5_vitrine} (the
 * teasers), {@code ambacht_gilde} / {@code ambacht_<id>} (the guild: bound by the professions slice when it is in the tree, else a
 * "binnenkort" line), and zone 1's shop roles (smid, winkel, kleermaker, markt, juwelier, stal).
 */
public final class Z5Npcs {
    private static boolean klaar;

    private Z5Npcs() {
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, GuhClothes... kleren) {
        npc(id, yaw, v, g, rol, 1f, kleren);
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, float maat, GuhClothes... kleren) {
        BlockPos plek = Z5VerhaalPlekken.NPC.get(id);
        if (plek == null) {
            throw new IllegalStateException("no spot for zone-5 npc " + id);
        }
        Npcs.voegToe(new NpcDef(id, plek, yaw, v, List.of(kleren), g, rol, maat));
    }

    public static synchronized void init() {
        if (klaar) {
            return;
        }
        klaar = true;
        NpcPlekken.NPC.putAll(Z5VerhaalPlekken.NPC);
        NpcPlekken.POI.putAll(Z5VerhaalPlekken.POI);
        QuestPlek.POI.putAll(Z5VerhaalPlekken.POI);
        QuestPlek.GEBIED.putAll(Z5VerhaalPlekken.GEBIED_DOEL);
        QuestPlek.BAAS.putAll(Z5VerhaalPlekken.BAAS);
        vulTracker("GEBIED_DOEL", Z5VerhaalPlekken.GEBIED_DOEL);
        vulTracker("KAMPVUUR", Z5VerhaalPlekken.KAMPVUUR);
        vulTracker("BAAS", Z5VerhaalPlekken.BAAS);

        // --- the story ---------------------------------------------------------------------------------------------------------------
        npc("z5_koningin", 90, GuhVariant.SNOW, Gedrag.STAAN, null, 1.2f, KONING_KROON, KONING_MANTEL, KNUS_SJAALTJE);
        npc("z5_inspecteur", 180, GuhVariant.CHOCO, Gedrag.STAAN, "z5_bewijsbord", 1.1f, DETECTIVE_PET, DETECTIVE_JAS, DETECTIVE_VERGROOTGLAS);
        npc("z5_havenmeester", 0, GuhVariant.NORMAL, Gedrag.STAAN, "luchtschip_z5", BALLONPET, BALLONBRIL, VADSPARADE_JASJE);
        npc("z5_veerkracht", 180, GuhVariant.GOLDEN, Gedrag.STAAN, null, 1.15f, KNIGHT_HELMET, KNIGHT_ARMOUR, WOLKENKRAAG);
        npc("z5_plukje", 90, GuhVariant.MINT, Gedrag.STAAN, null, 0.85f, VADSPARADE_SJAKO, VADSPARADE_JASJE);
        npc("z5_professor", 90, GuhVariant.STERRENKIJKERGUH, Gedrag.STAAN, null, 1.05f, STERRENKIJKERSMUTS, BIEB_LEESBRIL, STERRENCAPE);
        npc("z5_kok", 270, GuhVariant.BAKKERGUH, Gedrag.STAAN, null, 1.2f, CHEF_HAT, CHEF_JACKET, SMUL_SCHORT);
        npc("z5_mientje", 0, GuhVariant.MAGER, Gedrag.ZITTEN, null, 0.9f, KNUS_SJAALTJE, CAMPINGMARKT_HALSDOEK);
        // --- the Hoge Raad (the suspects) -------------------------------------------------------------------------------------------
        npc("z5_gouddraad", 0, GuhVariant.GOLDEN, Gedrag.STAAN, null, 1.1f, MONOCLE, BURGEMEESTERSSJERP, KONING_KETTING);
        npc("z5_inktvlek", 90, GuhVariant.BIBLIOTHECARIS, Gedrag.STAAN, null, BIEB_HOED, BIEB_LEESBRIL, BIEB_VEST);
        npc("z5_donsveer", 180, GuhVariant.SNOW, Gedrag.STAAN, null, 1.2f, VADSPARADE_SJAKO, KNIGHT_ARMOUR, MIKAMEPPER_MEDAILLE);
        npc("z5_zachtpels", 0, GuhVariant.NORMAL, Gedrag.STAAN, null, 1.1f, ROYAL_CAPE, MONOCLE, BLACK_BOWTIE);
        // --- the refugees (De Uitgevadsten) -----------------------------------------------------------------------------------------
        npc("z5_pluisje", 180, GuhVariant.MAGER, Gedrag.SLENTER, null, 0.7f, BABYMUTSJE, KNUFFELTJE);
        npc("z5_grauwsnor", 90, GuhVariant.MAGER, Gedrag.ZITTEN, null, 1.05f, WINTER_SCARF, CAMPINGMARKT_RUGZAK);
        GuhClothes[] vk = {WINTER_SCARF, KNUS_SJAALTJE, CAMPINGMARKT_HALSDOEK, SLAAPMUTSJE};
        for (int i = 1; i <= 4; i++) {
            npc("z5_vluchteling_" + i, 0, GuhVariant.MAGER, Gedrag.STAAN, null, 0.9f + 0.05f * (i % 2), vk[i - 1], CAMPINGMARKT_RUGZAK);
        }
        // --- the city -----------------------------------------------------------------------------------------------------------------
        npc("z5_zuster", 270, GuhVariant.SNOW, Gedrag.STAAN, null, KOORSTRIKJE, HEMEL_AUREOOLTJE, WOLKENKRAAG);
        npc("z5_hofdame", 180, GuhVariant.BLOESEMGUH, Gedrag.STAAN, null, SHOWSTER_TIARA, KNUS_SJAALTJE);
        npc("z5_stekje", 90, GuhVariant.BOERINNEGUH, Gedrag.SLENTER, null, TUINHOEDJE, TUINSCHORTJE);
        npc("z5_lientje", 0, GuhVariant.NORMAL, Gedrag.STAAN, "markt", KERMIS_HOED, KERMIS_JASJE, KERMIS_STRIK);
        npc("z5_bakker", 0, GuhVariant.BAKKERGUH, Gedrag.STAAN, "winkel", BAKKERSMUTSJE, BAKKERSSCHORTJE, MEELSTRIKJE);
        npc("z5_kleermaker", 180, GuhVariant.KAPPERGUH, Gedrag.STAAN, "kleermaker", THEEMUTSJE, KAPPERSCAPE, MONOCLE);
        npc("z5_smid", 180, GuhVariant.CHOCO, Gedrag.STAAN, "smid", 1.15f, BUILDER_HELMET, GRILL_SCHORT);
        npc("z5_juwelier", 0, GuhVariant.MINT, Gedrag.STAAN, "juwelier", MONOCLE, SHOWSTER_STRIK);
        npc("z5_stalmeester", 270, GuhVariant.CHOCO, Gedrag.STAAN, "stal", JOCKEY_PET, JOCKEY_JASJE);
        npc("z5_kadet_dons", 90, GuhVariant.SNOW, Gedrag.STAAN, null, 0.8f, VADSPARADE_SJAKO, KNUS_SJAALTJE);
        // --- the Ambachtsgilde ---------------------------------------------------------------------------------------------------------
        npc("z5_gildemeester", 180, GuhVariant.BURGEMEESTERGUH, Gedrag.STAAN, "ambacht_gilde", 1.15f, BIEB_HOED, BURGEMEESTERSSJERP, TIMMER_GEREEDSCHAPSRIEM);
        npc("z5_leermeester_koken", 0, GuhVariant.GRILLGUH, Gedrag.STAAN, "ambacht_koken", GRILL_KOKSMUTS, GRILL_SCHORT);
        npc("z5_leermeester_brouwen", 0, GuhVariant.THEEGUH, Gedrag.STAAN, "ambacht_brouwen", THEEMUTSJE, WIZARD_ROBE);
        npc("z5_leermeester_smeden", 180, GuhVariant.CHOCO, Gedrag.STAAN, "ambacht_smeden", 1.1f, TIMMER_HELMPJE, GRILL_SCHORT);
        npc("z5_leermeester_naaien", 180, GuhVariant.KAPPERGUH, Gedrag.STAAN, "ambacht_naaien", KAPSEL_KNOTJES, KAPPERSCAPE);
        npc("z5_leermeester_imkeren", 270, GuhVariant.GOLDEN, Gedrag.STAAN, "ambacht_imkeren", BZG_STROHOED, BZG_OVERALL);
        // --- the 4th-class teasers (not playable) ---------------------------------------------------------------------------------
        npc("z5_vadszanger", 90, GuhVariant.RAINBOW, Gedrag.STAAN, "z5_vitrine", DISCO_KOPTELEFOONTJE, VADSPARADE_JASJE, VADSPARADE_TROMMELTJE);
        npc("z5_pluisninja", 180, GuhVariant.STARRY, Gedrag.STAAN, "z5_vitrine", JAPAN_HACHIMAKI, JAPAN_KIMONO, PLUISSLOFJES);
        // --- citizens -------------------------------------------------------------------------------------------------------------------
        GuhVariant[] bk = {GuhVariant.NORMAL, GuhVariant.GOLDEN, GuhVariant.MINT, GuhVariant.WOLK, GuhVariant.CHOCO, GuhVariant.SNOW};
        GuhClothes[][] bc = {{FEESTHOEDJE, SLINGER_SJAALTJE}, {ZONNEHOEDJE, RED_BOWTIE}, {WOLKENMUTS, WOLKENKRAAG}, {OORSTRIKJE_ROZE, KNUS_SJAALTJE},
                {PARTY_HAT, STRIPED_SWEATER}, {BALLONPET, CAMPINGMARKT_RUGZAK}};
        for (int i = 1; i <= 6; i++) {
            npc("z5_burger_" + i, i * 60, bk[i - 1], Gedrag.SLENTER, null, i == 4 ? 0.75f : 1f, bc[i - 1]);
        }
        // --- the Wolkwacht (elite: helmet, cloud collar, a feather) ----------------------------------------------------------------
        GuhVariant[] wk = {GuhVariant.SNOW, GuhVariant.GOLDEN, GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.SNOW};
        for (int i = 1; i <= 6; i++) {
            npc("z5_wacht_" + i, 90 * (i % 4), wk[i - 1], Gedrag.STAAN, "kampwacht", 1.05f, KNIGHT_HELMET, KNIGHT_ARMOUR, WOLKENKRAAG);
        }
    }

    @SuppressWarnings("unchecked")
    private static void vulTracker(String veld, Map<String, BlockPos> m) {
        try {
            Field f = NpcPlekken.class.getField(veld);
            ((Map<String, BlockPos>) f.get(null)).putAll(m);
        } catch (ReflectiveOperationException | ClassCastException e) {
            // (not in this tree)
        }
    }
}
