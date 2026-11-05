package nl.juiced.guhs.feature.vadsrijk.zone4.verhaal;

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
 * The NPC guhs of zone 4 (Lampionhaven + the islands): sunny coast guhs in striped sailor jumpers, straw hats, swim rings and
 * lantern-orange; the refugees from the east are {@link GuhVariant#MAGER} guhs (thin and grey, a little pinker every day).
 * They join zone 1's NPC keeper ({@link Npcs#voegToe}). Names, lines and "where" texts: tools/features/vadsrijk_t_z4verhaal.py;
 * spots: {@link Z4VerhaalPlekken}.
 */
public final class Z4Npcs {
    private static boolean klaar;

    private Z4Npcs() {
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, GuhClothes... kleren) {
        npc(id, yaw, v, g, rol, 1f, kleren);
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, float maat, GuhClothes... kleren) {
        BlockPos plek = Z4VerhaalPlekken.NPC.get(id);
        if (plek == null) {
            throw new IllegalStateException("no spot for zone-4 npc " + id);
        }
        Npcs.voegToe(new NpcDef(id, plek, yaw, v, List.of(kleren), g, rol, maat));
    }

    public static synchronized void init() {
        if (klaar) {
            return;
        }
        klaar = true;
        NpcPlekken.NPC.putAll(Z4VerhaalPlekken.NPC);
        NpcPlekken.POI.putAll(Z4VerhaalPlekken.POI);
        QuestPlek.POI.putAll(Z4VerhaalPlekken.POI);
        QuestPlek.GEBIED.putAll(Z4VerhaalPlekken.GEBIED_DOEL);
        QuestPlek.BAAS.putAll(Z4VerhaalPlekken.BAAS);
        vulTracker("GEBIED_DOEL", Z4VerhaalPlekken.GEBIED_DOEL);
        vulTracker("KAMPVUUR", Z4VerhaalPlekken.KAMPVUUR);
        vulTracker("BAAS", Z4VerhaalPlekken.BAAS);

        // --- Lampionhaven: the story + travel -------------------------------------------------------------------------------------
        npc("z4_havenmeester", 90, GuhVariant.NORMAL, Gedrag.STAAN, null, 1.15f, PIRATE_HAT, STRIPED_SWEATER, RED_BOWTIE);
        npc("z4_zeilverhuur", 90, GuhVariant.MINT, Gedrag.STAAN, "z4_zeilboot", VISSERSHOEDJE, STRIPED_SWEATER, ZWEMBAND);
        npc("z4_luchthaven", 270, GuhVariant.GOLDEN, Gedrag.STAAN, "luchtschip_z4", BALLONPET, BALLONBRIL, STRIPED_SWEATER);
        // --- shops and services ----------------------------------------------------------------------------------------------------
        npc("z4_lampionmaker", 90, GuhVariant.GOLDEN, Gedrag.STAAN, null, 1.05f, LICHTJES_PADDENSTOELHOEDJE, STRIPED_SWEATER, TUINSCHORTJE);
        npc("z4_bankier", 180, GuhVariant.NORMAL, Gedrag.STAAN, "bank", 1.1f, MONOCLE, STRIPED_SWEATER, BLACK_BOWTIE);
        npc("z4_werkplaats", 90, GuhVariant.CHOCO, Gedrag.STAAN, "werkplaats", TIMMER_HELMPJE, STRIPED_SWEATER, TIMMER_GEREEDSCHAPSRIEM);
        npc("z4_winkel", 0, GuhVariant.MINT, Gedrag.STAAN, "winkel", ZONNEHOEDJE, STRIPED_SWEATER, CAMPINGMARKT_RUGZAK);
        npc("z4_smid", 90, GuhVariant.CHOCO, Gedrag.STAAN, "smid", 1.15f, BUILDER_HELMET, STRIPED_SWEATER, GRILL_SCHORT);
        npc("z4_wapens", 0, GuhVariant.GOLDEN, Gedrag.STAAN, "wapenwinkel", SCHAT_PIRATENHOEDJE, STRIPED_SWEATER, KNIGHT_ARMOUR);
        npc("z4_kleermaker", 180, GuhVariant.NORMAL, Gedrag.STAAN, "kleermaker", THEEMUTSJE, STRIPED_SWEATER, KNUS_SJAALTJE);
        npc("z4_markt", 0, GuhVariant.MINT, Gedrag.STAAN, "markt", KERMIS_HOED, STRIPED_SWEATER, KERMIS_STRIK);
        npc("z4_kok", 180, GuhVariant.VISGUH, Gedrag.STAAN, "z4_vissoep", 1.1f, CHEF_HAT, CHEF_JACKET);
        npc("z4_visser", 90, GuhVariant.VISGUH, Gedrag.ZITTEN, null, VISSERSHOEDJE, VISVEST, VIS_AAN_DE_HAAK);
        npc("z4_duinwachter", 180, GuhVariant.GOLDEN, Gedrag.STAAN, "kampwacht", 1.1f, KNIGHT_HELMET, STRIPED_SWEATER, GUHWAII_BLOEMENKRANS);
        // --- citizens ----------------------------------------------------------------------------------------------------------------
        npc("z4_oma_zeeschuim", 90, GuhVariant.SNOW, Gedrag.ZITTEN, null, BADMUTSJE, STRIPED_SWEATER, BIEB_LEESBRIL);
        npc("z4_kind_lampje", 270, GuhVariant.GOLDEN, Gedrag.SLENTER, null, 0.7f, OORSTRIKJE_GEEL, STRIPED_SWEATER, ZWEMBAND);
        npc("z4_kind_golfje", 0, GuhVariant.MINT, Gedrag.SLENTER, null, 0.7f, DUIKBRIL, SNORKEL, ZWEMBAND);
        npc("z4_postguh", 180, GuhVariant.NORMAL, Gedrag.SLENTER, null, POLICE_CAP, STRIPED_SWEATER, CAMPINGMARKT_RUGZAK);
        npc("z4_danseres", 270, GuhVariant.ZEEMEERGUH, Gedrag.SLENTER, null, GUHWAII_BLOEMENKRANS, GUHWAII_HULAROKJE);
        npc("z4_schipper", 90, GuhVariant.CHOCO, Gedrag.STAAN, null, 1.1f, PIRATE_HAT, RAINCOAT, WINTER_SCARF);
        // --- Knuffeltij-strand: the shelter and the refugees (thin grey guhs, slowly getting their colour back) ------------------
        npc("z4_opvang", 180, GuhVariant.NORMAL, Gedrag.STAAN, null, 1.15f, THEEMUTSJE, SAMEN_KNUFFELTRUITJE, SAMEN_HARTJESSPELDJE);
        npc("z4_wasguh", 90, GuhVariant.MINT, Gedrag.STAAN, null, HANDDOEK_TULBAND, BADJASJE);
        npc("z4_soepguh", 270, GuhVariant.GOLDEN, Gedrag.STAAN, "z4_soep", SMUL_BAKKERSMUTS, SMUL_SCHORT);
        npc("z4_grijsje", 0, GuhVariant.MAGER, Gedrag.ZITTEN, null, 0.7f, KNUS_SJAALTJE, KNUFFELTJE);
        npc("z4_opa_dun", 270, GuhVariant.MAGER, Gedrag.ZITTEN, null, SLAAPMUTSJE, KNUS_SJAALTJE);
        npc("z4_vaaltje", 180, GuhVariant.MAGER, Gedrag.STAAN, null, 0.85f, KNUS_SJAALTJE);
        npc("z4_mama_pluis", 90, GuhVariant.MAGER, Gedrag.STAAN, null, BABYMUTSJE, KNUS_SJAALTJE);
        // --- the islands -------------------------------------------------------------------------------------------------------------
        npc("z4_limonade", 0, GuhVariant.GOLDEN, Gedrag.STAAN, "z4_limonade", SUNGLASSES, GUHWAII_HULAROKJE, GUHWAII_BLOEMENKRANS);
        npc("z4_volleybal", 270, GuhVariant.CHOCO, Gedrag.STAAN, null, SPELEN_ZWEETBANDJE, SPELEN_SPORTSHIRTJE, SPELEN_FLUITJE);
        npc("z4_feestguh", 180, GuhVariant.RAINBOW, Gedrag.SLENTER, null, FEESTHOEDJE, SLINGER_SJAALTJE, HEART_GLASSES);
        npc("z4_vliegermaker", 90, GuhVariant.NORMAL, Gedrag.STAAN, null, STRAW_HAT, STRIPED_SWEATER, TIMMER_GEREEDSCHAPSRIEM);
        npc("z4_windmeester", 180, GuhVariant.SNOW, Gedrag.STAAN, null, 1.1f, BALLONPET, RACEBRIL, SPELEN_FLUITJE);
        npc("z4_duikmeester", 0, GuhVariant.ZEEMEERGUH, Gedrag.STAAN, null, DUIKBRIL, SNORKEL, ZWEMBAND);
        npc("z4_wachter", 270, GuhVariant.SNOW, Gedrag.ZITTEN, null, 1.1f, VISSERSHOEDJE, RAINCOAT, BIEB_LEESBRIL);
        npc("z4_imker", 180, GuhVariant.GOLDEN, Gedrag.STAAN, null, BZG_STROHOED, BZG_OVERALL, TUINSCHORTJE);
        npc("z4_madelief", 0, GuhVariant.BLOESEMGUH, Gedrag.SLENTER, null, BLOESEMKRANSJE, PLUKMANDJE);
        npc("z4_schelpie", 90, GuhVariant.NORMAL, Gedrag.ZITTEN, null, 0.9f, STRAW_HAT, OVERALLS, GUHWAII_SURFPLANKJE);
        npc("z4_nevel", 270, GuhVariant.MAGER, Gedrag.STAAN, null, RAINCOAT, KNUS_SJAALTJE);
        npc("z4_pierwachter", 90, GuhVariant.MINT, Gedrag.STAAN, null, KARRETJES_CONDUCTEURSPETJE, STRIPED_SWEATER, RED_BOWTIE);
        npc("z4_hemelkapitein", 90, GuhVariant.WOLK, Gedrag.STAAN, null, 1.2f, BALLONPET, BALLONBRIL, WOLKENKRAAG);
        // --- the Kustwacht: knights in sailor jumpers with a flower garland on the helmet ------------------------------------------
        GuhVariant[] kleuren = {GuhVariant.GOLDEN, GuhVariant.MINT, GuhVariant.NORMAL, GuhVariant.CHOCO};
        GuhClothes[] extra = {GUHWAII_BLOEMENKRANS, KNUS_SJAALTJE, RED_BOWTIE};
        int w = 0;
        for (String id : Z4VerhaalPlekken.NPC.keySet()) {
            if (id.startsWith("z4_wacht_")) {
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
