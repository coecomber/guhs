package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

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
 * The NPC guhs of zone 3 (Honingtak + De Schemerwortel): cosy beekeeper guhs in striped jumpers with straw hats and veils
 * (existing guh clothes: STRIPED_SWEATER, BZG_STROHOED, ZONNEHOEDJE, TUINSCHORTJE, BLOESEMKRANSJE, ...), the stall guhs of the
 * Pluimstal, and the Honingwacht at the campfires. They join zone 1's NPC keeper ({@link Npcs#voegToe}). Names, lines and "where"
 * texts: tools/features/vadsrijk_z3_npc.py; spots: {@link Z3NpcPlekken}.
 */
public final class Z3Npcs {
    private static boolean klaar;

    private Z3Npcs() {
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, GuhClothes... kleren) {
        npc(id, yaw, v, g, rol, 1f, kleren);
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, float maat, GuhClothes... kleren) {
        BlockPos plek = Z3NpcPlekken.NPC.get(id);
        if (plek == null) {
            throw new IllegalStateException("no spot for zone-3 npc " + id);
        }
        Npcs.voegToe(new NpcDef(id, plek, yaw, v, List.of(kleren), g, rol, maat));
    }

    public static synchronized void init() {
        if (klaar) {
            return;
        }
        klaar = true;
        NpcPlekken.NPC.putAll(Z3NpcPlekken.NPC);
        NpcPlekken.POI.putAll(Z3NpcPlekken.POI);
        QuestPlek.POI.putAll(Z3NpcPlekken.POI);
        QuestPlek.GEBIED.putAll(Z3NpcPlekken.GEBIED_DOEL);
        QuestPlek.BAAS.putAll(Z3NpcPlekken.BAAS);
        vulTracker("GEBIED_DOEL", Z3NpcPlekken.GEBIED_DOEL);
        vulTracker("KAMPVUUR", Z3NpcPlekken.KAMPVUUR);
        vulTracker("BAAS", Z3NpcPlekken.BAAS);

        // --- Honingtak: travel and the story ---------------------------------------------------------------------------------------
        npc("z3_kapitein", 90, GuhVariant.GOLDEN, Gedrag.STAAN, "luchtschip_z3", BALLONPET, BALLONBRIL, STRIPED_SWEATER, RED_BOWTIE);
        npc("z3_raatje", 180, GuhVariant.GOLDEN, Gedrag.STAAN, null, 1.15f, BZG_STROHOED, STRIPED_SWEATER, BURGEMEESTERSSJERP);
        npc("z3_wasja", 180, GuhVariant.NORMAL, Gedrag.STAAN, "ordetempel", 1.15f, STERRENKIJKERSMUTS, STERRENCAPE, STRIPED_SWEATER);
        npc("z3_veertje", 270, GuhVariant.CHOCO, Gedrag.STAAN, "stal", 1.1f, JOCKEY_PET, JOCKEY_JASJE, BOERDERIJ_ZAKDOEK);
        npc("z3_cafe", 90, GuhVariant.GOLDEN, Gedrag.STAAN, "honingthee", 1.1f, SMUL_BAKKERSMUTS, STRIPED_SWEATER, SMUL_SCHORT);
        npc("z3_jager", 180, GuhVariant.CHOCO, Gedrag.STAAN, "premiebord", 1.1f, SAFARI_HOEDJE, SAFARI_VESTJE, SAFARI_VERREKIJKER);
        // --- shops and services -----------------------------------------------------------------------------------------------------
        npc("z3_smid", 90, GuhVariant.CHOCO, Gedrag.STAAN, "smid", 1.15f, BUILDER_HELMET, STRIPED_SWEATER, GRILL_SCHORT);
        npc("z3_juwelier", 90, GuhVariant.MINT, Gedrag.STAAN, "juwelier", MONOCLE, STRIPED_SWEATER, KNUS_SJAALTJE);
        npc("z3_winkel", 270, GuhVariant.NORMAL, Gedrag.STAAN, "honingkraam", ZONNEHOEDJE, STRIPED_SWEATER, TUINSCHORTJE);
        npc("z3_wapens", 270, GuhVariant.GOLDEN, Gedrag.STAAN, "wapenwinkel", EYEPATCH, STRIPED_SWEATER, KNIGHT_ARMOUR);
        npc("z3_kleermaker", 90, GuhVariant.MINT, Gedrag.STAAN, "kleermaker", THEEMUTSJE, STRIPED_SWEATER, BLOESEMKRANSJE);
        npc("z3_markt", 0, GuhVariant.NORMAL, Gedrag.STAAN, "markt", KERMIS_HOED, STRIPED_SWEATER, KERMIS_STRIK);
        npc("z3_stalguh", 90, GuhVariant.NORMAL, Gedrag.SLENTER, "stal", BOERDERIJ_HOEDJE, OVERALLS, BOERDERIJ_ZAKDOEK);
        npc("z3_liftguh", 0, GuhVariant.CHOCO, Gedrag.STAAN, "honinglift", KARRETJES_CONDUCTEURSPETJE, STRIPED_SWEATER, TIMMER_GEREEDSCHAPSRIEM);
        // --- citizens ----------------------------------------------------------------------------------------------------------------
        npc("z3_opa_bast", 90, GuhVariant.GOLDEN, Gedrag.ZITTEN, null, BZG_STROHOED, STRIPED_SWEATER, BIEB_LEESBRIL);
        npc("z3_zoemer", 0, GuhVariant.GOLDEN, Gedrag.SLENTER, null, 0.7f, OORSTRIKJE_GEEL, STRIPED_SWEATER, HEMEL_WOLKENVLEUGELTJES);
        npc("z3_honingbij", 180, GuhVariant.GOLDEN, Gedrag.SLENTER, null, 0.75f, GLIM_ANTENNETJES, STRIPED_SWEATER, HEMEL_WOLKENVLEUGELTJES);
        npc("z3_raatwever", 90, GuhVariant.NORMAL, Gedrag.STAAN, null, THEEMUTSJE, STRIPED_SWEATER, TUINSCHORTJE);
        npc("z3_bloesem", 270, GuhVariant.BLOESEMGUH, Gedrag.SLENTER, null, BLOESEMKRANSJE, STRIPED_SWEATER);
        npc("z3_druppel", 180, GuhVariant.GOLDEN, Gedrag.SLENTER, null, FEESTHOEDJE, STRIPED_SWEATER, SLINGER_SJAALTJE);
        npc("z3_lampion", 270, GuhVariant.MINT, Gedrag.STAAN, null, LICHTJES_PADDENSTOELHOEDJE, STRIPED_SWEATER, KNUS_SJAALTJE);
        npc("z3_imker_1", 0, GuhVariant.GOLDEN, Gedrag.SLENTER, null, BZG_STROHOED, STRIPED_SWEATER, TUINSCHORTJE);
        npc("z3_imker_2", 180, GuhVariant.CHOCO, Gedrag.SLENTER, null, ZONNEHOEDJE, STRIPED_SWEATER, TUINSCHORTJE);
        npc("z3_imker_3", 90, GuhVariant.NORMAL, Gedrag.SLENTER, null, BZG_STROHOED, BZG_OVERALL, KNUS_SJAALTJE);
        npc("z3_kuikenhoeder", 270, GuhVariant.MINT, Gedrag.STAAN, null, BOERDERIJ_HOEDJE, STRIPED_SWEATER, PLUKMANDJE);
        npc("z3_kind_1", 90, GuhVariant.GOLDEN, Gedrag.SLENTER, null, 0.7f, OORSTRIKJE_ROZE, STRIPED_SWEATER, HEMEL_WOLKENVLEUGELTJES);
        npc("z3_kind_2", 270, GuhVariant.CHOCO, Gedrag.SLENTER, null, 0.7f, BABYMUTSJE, STRIPED_SWEATER, HEMEL_WOLKENVLEUGELTJES);
        npc("z3_postguh", 0, GuhVariant.NORMAL, Gedrag.SLENTER, null, POLICE_CAP, STRIPED_SWEATER, CAMPINGMARKT_RUGZAK);
        // --- out in the zone ---------------------------------------------------------------------------------------------------------
        npc("z3_bladwachter", 180, GuhVariant.GOLDEN, Gedrag.STAAN, "kampwacht", 1.1f, KNIGHT_HELMET, STRIPED_SWEATER, BLOESEMKRANSJE);
        npc("z3_lusje", 90, GuhVariant.MINT, Gedrag.STAAN, null, DETECTIVE_PET, STRIPED_SWEATER, DOOLHOF_KOMPAS);
        npc("z3_herbergier", 90, GuhVariant.CHOCO, Gedrag.STAAN, "winkel", GRILL_KOKSMUTS, STRIPED_SWEATER, GRILL_SCHORT);
        npc("z3_knoest", 270, GuhVariant.BOSWACHTERGUH, Gedrag.STAAN, null, 0.8f, LICHTJES_PADDENSTOELHOEDJE, GLIM_ANTENNETJES);
        npc("z3_wortelien", 180, GuhVariant.BEVERGUH, Gedrag.STAAN, null, 1.1f, TUINHOEDJE, STRIPED_SWEATER, BURGEMEESTERSSJERP);
        npc("z3_molenaar", 90, GuhVariant.BEVERGUH, Gedrag.STAAN, null, BOERDERIJ_HOEDJE, OVERALLS, TIMMER_GEREEDSCHAPSRIEM);
        npc("z3_theeguh", 0, GuhVariant.KIKKERGUH, Gedrag.STAAN, "honingthee", THEEMUTSJE, STRIPED_SWEATER, KNUS_SJAALTJE);
        npc("z3_gloei", 0, GuhVariant.CHOCO, Gedrag.STAAN, "kampwacht", 1.1f, MIJNLAMPJE, STRIPED_SWEATER, KNIGHT_ARMOUR);
        npc("z3_nestwachter", 180, GuhVariant.KORSTGUH, Gedrag.STAAN, null, 1.6f, GUHRIOBELONING_SCHILD, BLOESEMKRANSJE);
        // --- (P-VERHUIS) visible life: beekeepers in Raatweide + Molenhoek, travellers at the camps, guhs on the bridges -----------
        npc("z3_imker_raatweide", 180, GuhVariant.GOLDEN, Gedrag.SLENTER, null, BZG_STROHOED, BZG_OVERALL, TUINSCHORTJE);
        npc("z3_imker_molenhoek", 90, GuhVariant.BEVERGUH, Gedrag.SLENTER, null, BOERDERIJ_HOEDJE, OVERALLS, KNUS_SJAALTJE);
        npc("z3_reiziger_1", 0, GuhVariant.NORMAL, Gedrag.ZITTEN, null, ZONNEHOEDJE, STRIPED_SWEATER, CAMPINGMARKT_RUGZAK);
        npc("z3_reiziger_2", 90, GuhVariant.MINT, Gedrag.ZITTEN, null, DETECTIVE_PET, STRIPED_SWEATER, DOOLHOF_KOMPAS);
        npc("z3_reiziger_3", 270, GuhVariant.CHOCO, Gedrag.ZITTEN, null, TUINHOEDJE, STRIPED_SWEATER, CAMPINGMARKT_RUGZAK);
        npc("z3_reiziger_4", 180, GuhVariant.BLOESEMGUH, Gedrag.ZITTEN, null, JOCKEY_PET, JOCKEY_JASJE, KNUS_SJAALTJE);
        GuhVariant[] brugKleur = {GuhVariant.GOLDEN, GuhVariant.NORMAL, GuhVariant.CHOCO, GuhVariant.MINT, GuhVariant.GOLDEN, GuhVariant.NORMAL};
        for (int i = 1; i <= 6; i++) {
            npc("z3_brug_" + i, i * 60, brugKleur[i - 1], Gedrag.STAAN, null, i % 2 == 0 ? ZONNEHOEDJE : BZG_STROHOED, STRIPED_SWEATER,
                    i % 3 == 0 ? KNUS_SJAALTJE : RED_BOWTIE);
        }
        // --- the Honingwacht: knights in striped jumpers with a blossom wreath on the helmet ----------------------------------------
        GuhVariant[] kleuren = {GuhVariant.GOLDEN, GuhVariant.CHOCO, GuhVariant.NORMAL, GuhVariant.MINT};
        GuhClothes[] extra = {BLOESEMKRANSJE, KNUS_SJAALTJE, RED_BOWTIE};
        int w = 0;
        for (String id : Z3NpcPlekken.NPC.keySet()) {
            if (id.startsWith("z3_wacht_")) {
                npc(id, 180, kleuren[w % kleuren.length], Gedrag.STAAN, "kampwacht", KNIGHT_HELMET, STRIPED_SWEATER, extra[w % extra.length]);
                w++;
            }
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
