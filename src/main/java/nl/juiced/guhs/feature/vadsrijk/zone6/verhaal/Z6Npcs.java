package nl.juiced.guhs.feature.vadsrijk.zone6.verhaal;

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
 * The NPC guhs of the Zijdeland (zone 6): Rozenkloof (havenmeester, kloofmoeder, Oma Dadel's caravan, the bultguh stable, the
 * carpet shop, the hanging gardens, the post), Saffraanpoort (the sultane, her grand vizier, the mirror polisher, the astronomer,
 * the bazaar, the karavanserai, the norias, the canal quarter, the veiled lady), the profession teachers, the hub NPCs of u-huizen
 * ({@code kk_poort_<stad>}) and u-gilden ({@code bond_bord_<stad>}), and the people out in the five areas. They join zone 1's NPC
 * keeper ({@link Npcs#voegToe}). Names, lines and "where" texts: tools/features/vadsrijk_u_z6verhaal.py; spots: {@link Z6VerhaalPlekken}.
 * <p>Roles: {@code luchtschip_z5} (Havenmeester Zandloper: the same travel role as Hoogvads' havenmeester, its list is his own),
 * zone 1's shop roles (winkel, markt, juwelier, smid, stal), {@code kampwacht}, {@code z6_tapijtwinkel}.
 */
public final class Z6Npcs {
    private static boolean klaar;

    private Z6Npcs() {
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, GuhClothes... kleren) {
        npc(id, yaw, v, g, rol, 1f, kleren);
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, float maat, GuhClothes... kleren) {
        BlockPos plek = Z6VerhaalPlekken.NPC.get(id);
        if (plek == null) {
            throw new IllegalStateException("no spot for zone-6 npc " + id);
        }
        Npcs.voegToe(new NpcDef(id, plek, yaw, v, List.of(kleren), g, rol, maat));
    }

    public static synchronized void init() {
        if (klaar) {
            return;
        }
        klaar = true;
        NpcPlekken.NPC.putAll(Z6VerhaalPlekken.NPC);
        // (POIs only for the tracker: Z6Haken counts them itself, in story order, with its own radius)
        QuestPlek.POI.putAll(Z6VerhaalPlekken.POI);
        QuestPlek.GEBIED.putAll(Z6VerhaalPlekken.GEBIED_DOEL);
        vulTracker("GEBIED_DOEL", Z6VerhaalPlekken.GEBIED_DOEL);
        vulTracker("KAMPVUUR", Z6VerhaalPlekken.KAMPVUUR);

        // --- Rozenkloof --------------------------------------------------------------------------------------------------------------
        npc("z6_zandloper", 0, GuhVariant.CHOCO, Gedrag.STAAN, "luchtschip_z5", BALLONPET, BALLONBRIL, VADSPARADE_JASJE);
        npc("z6_roosje", 90, GuhVariant.OPA_GUH, Gedrag.STAAN, null, 1.1f, HANDDOEK_TULBAND, KNUS_SJAALTJE, KONING_KETTING);
        npc("z6_dadel", 270, GuhVariant.BOERINNEGUH, Gedrag.STAAN, null, 1.05f, HANDDOEK_TULBAND, CAMPINGMARKT_HALSDOEK, CAMPINGMARKT_RUGZAK);
        npc("z6_hobbel", 90, GuhVariant.CHOCO, Gedrag.STAAN, "stal", 1.1f, JOCKEY_PET, JOCKEY_JASJE);
        npc("z6_franje", 180, GuhVariant.KAPPERGUH, Gedrag.STAAN, "z6_tapijtwinkel", HANDDOEK_TULBAND, KAPPERSCAPE);
        npc("z6_roosmarijn", 0, GuhVariant.BLOESEMGUH, Gedrag.SLENTER, null, TUINHOEDJE, TUINSCHORTJE);
        npc("z6_stempel", 0, GuhVariant.STEMPELGUH, Gedrag.STAAN, null, 0.9f, BALLONPET, CAMPINGMARKT_RUGZAK);
        npc("z6_bankier_kloof", 180, GuhVariant.GOLDEN, Gedrag.STAAN, null, MONOCLE, BLACK_BOWTIE, HANDDOEK_TULBAND);
        npc("kk_poort_rozenkloof", 0, GuhVariant.SNOW, Gedrag.STAAN, null, 1.05f, KNIGHT_HELMET, ROYAL_CAPE, MONOCLE);
        npc("bond_bord_rozenkloof", 0, GuhVariant.MINT, Gedrag.STAAN, null, BIEB_HOED, BIEB_VEST);
        // --- Saffraanpoort: the palace -----------------------------------------------------------------------------------------------
        npc("z6_rozijntje", 0, GuhVariant.GOLDEN, Gedrag.STAAN, null, 1.15f, SHOWSTER_TIARA, KONING_MANTEL, GOUDEN_HARTJESHALSBANDJE);
        npc("z6_kaneelstok", 0, GuhVariant.CHOCO, Gedrag.STAAN, null, 1.1f, HANDDOEK_TULBAND, ROYAL_CAPE, BURGEMEESTERSSJERP);
        npc("z6_glans", 90, GuhVariant.SNOW, Gedrag.STAAN, null, MONOCLE, SMUL_SCHORT);
        npc("z6_twinkel", 270, GuhVariant.STERRENKIJKERGUH, Gedrag.STAAN, null, 1.05f, STERRENKIJKERSMUTS, BIEB_LEESBRIL, STERRENCAPE);
        npc("z6_dadel_serai", 90, GuhVariant.BOERINNEGUH, Gedrag.ZITTEN, null, 1.05f, HANDDOEK_TULBAND, CAMPINGMARKT_HALSDOEK);
        // --- Saffraanpoort: the city ---------------------------------------------------------------------------------------------------
        npc("z6_knoopje", 90, GuhVariant.KAPPERGUH, Gedrag.STAAN, null, KAPSEL_KNOTJES, KAPPERSCAPE);
        npc("z6_kardemom", 180, GuhVariant.THEEGUH, Gedrag.STAAN, "winkel", HANDDOEK_TULBAND, SMUL_SCHORT);
        npc("z6_pistache", 180, GuhVariant.MINT, Gedrag.STAAN, "markt", HANDDOEK_TULBAND, KERMIS_JASJE);
        npc("z6_lokum", 0, GuhVariant.BLOESEMGUH, Gedrag.STAAN, "juwelier", MONOCLE, SHOWSTER_STRIK);
        npc("z6_koperkop", 90, GuhVariant.CHOCO, Gedrag.STAAN, "smid", 1.15f, BUILDER_HELMET, GRILL_SCHORT);
        npc("z6_bankier_poort", 180, GuhVariant.GOLDEN, Gedrag.STAAN, null, MONOCLE, BURGEMEESTERSSJERP);
        npc("z6_knars", 270, GuhVariant.OPA_GUH, Gedrag.STAAN, null, TIMMER_HELMPJE, TIMMER_GEREEDSCHAPSRIEM);
        npc("z6_pluk", 90, GuhVariant.NORMAL, Gedrag.SLENTER, null, 0.7f, ZONNEHOEDJE, KNUFFELTJE);
        npc("z6_ringeling", 180, GuhVariant.KRUIMEL_MIKA, Gedrag.STAAN, null, 1.05f, SHOWSTER_TIARA, ROYAL_CAPE);
        npc("kk_poort_saffraanpoort", 90, GuhVariant.SNOW, Gedrag.STAAN, null, 1.05f, KNIGHT_HELMET, ROYAL_CAPE, MONOCLE);
        npc("bond_bord_saffraanpoort", 0, GuhVariant.MINT, Gedrag.STAAN, null, BIEB_HOED, BIEB_VEST);
        // --- profession teachers -------------------------------------------------------------------------------------------------------
        npc("z6_leermeester_smeden", 90, GuhVariant.CHOCO, Gedrag.STAAN, null, 1.1f, TIMMER_HELMPJE, GRILL_SCHORT);
        npc("z6_leermeester_naaien", 90, GuhVariant.KAPPERGUH, Gedrag.STAAN, null, HANDDOEK_TULBAND, KAPPERSCAPE);
        npc("z6_leermeester_koken", 90, GuhVariant.GRILLGUH, Gedrag.STAAN, null, GRILL_KOKSMUTS, GRILL_SCHORT);
        npc("z6_leermeester_brouwen", 0, GuhVariant.THEEGUH, Gedrag.STAAN, null, THEEMUTSJE, WIZARD_ROBE);
        npc("z6_leermeester_imkeren", 270, GuhVariant.GOLDEN, Gedrag.STAAN, null, BZG_STROHOED, BZG_OVERALL);
        // --- out in the land -----------------------------------------------------------------------------------------------------------
        npc("z6_dadelpit", 0, GuhVariant.BOSWACHTERGUH, Gedrag.STAAN, null, TUINHOEDJE, SAFARI_VESTJE);
        npc("z6_spetter", 90, GuhVariant.VISGUH, Gedrag.STAAN, null, 0.9f, BALLONPET, CAMPINGMARKT_RUGZAK);
        npc("z6_korrel", 0, GuhVariant.SNOW, Gedrag.STAAN, null, SAFARI_HOEDJE, SMUL_SCHORT);
        npc("z6_rimpel", 180, GuhVariant.BOERINNEGUH, Gedrag.STAAN, null, HANDDOEK_TULBAND, SAFARI_VESTJE);
        npc("z6_stof", 90, GuhVariant.BIBLIOTHECARIS, Gedrag.STAAN, null, SAFARI_HOEDJE, BIEB_LEESBRIL, SAFARI_VESTJE);
        npc("z6_bron", 270, GuhVariant.ZEEMEERGUH, Gedrag.ZITTEN, null, 1.4f);
        npc("z6_mientje", 90, GuhVariant.MAGER, Gedrag.STAAN, null, 0.9f, KNUS_SJAALTJE, SMUL_SCHORT);
        GuhClothes[] vk = {WINTER_SCARF, CAMPINGMARKT_HALSDOEK, SLAAPMUTSJE};
        for (int i = 1; i <= 3; i++) {
            npc("z6_vluchteling_" + i, 270, GuhVariant.MAGER, Gedrag.ZITTEN, null, 0.9f, vk[i - 1], CAMPINGMARKT_RUGZAK);
        }
        // --- citizens --------------------------------------------------------------------------------------------------------------------
        GuhVariant[] bk = {GuhVariant.NORMAL, GuhVariant.BLOESEMGUH, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.GOLDEN, GuhVariant.SNOW};
        GuhClothes[][] bc = {{HANDDOEK_TULBAND, KNUS_SJAALTJE}, {ZONNEHOEDJE, OORSTRIKJE_ROZE}, {HANDDOEK_TULBAND, SLINGER_SJAALTJE},
                {SAFARI_HOEDJE, RED_BOWTIE}, {TUINHOEDJE, TUINSCHORTJE}, {HANDDOEK_TULBAND, CAMPINGMARKT_RUGZAK}};
        for (int i = 1; i <= 6; i++) {
            npc("z6_burger_" + i, i * 60, bk[i - 1], Gedrag.SLENTER, null, bc[i - 1]);
        }
        // --- the guards at the campfires -------------------------------------------------------------------------------------------------
        GuhVariant[] wk = {GuhVariant.CHOCO, GuhVariant.GOLDEN, GuhVariant.NORMAL, GuhVariant.SNOW, GuhVariant.MINT, GuhVariant.CHOCO};
        for (int i = 1; i <= 6; i++) {
            npc("z6_wacht_" + i, 90 * (i % 4), wk[i - 1], Gedrag.STAAN, "kampwacht", 1.05f, HANDDOEK_TULBAND, KNIGHT_ARMOUR);
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
