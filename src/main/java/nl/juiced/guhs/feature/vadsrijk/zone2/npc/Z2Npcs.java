package nl.juiced.guhs.feature.vadsrijk.zone2.npc;

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
 * The NPC guhs of zone 2 (Kandijkom + the Splinterpieken), all in knitted jumpers / winter clothes (the existing guh clothes).
 * They join zone 1's NPC keeper ({@link Npcs#voegToe}): same spawning, idle behaviour, markers, dialogue and questlog. Names,
 * lines and "where" texts: tools/features/vadsrijk_z2_npc.py; spots: {@link Z2NpcPlekken}.
 * <p>Roles (what a click does without quest business, bound in {@link Z2NpcRegistry}): luchtschip_z2, juwelier, gondel,
 * gondel_terug, chocomelk, and zone 1's smid/winkel/wapenwinkel/kleermaker/markt/ordetempel/kampwacht.
 */
public final class Z2Npcs {
    private static boolean klaar;

    private Z2Npcs() {
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, GuhClothes... kleren) {
        npc(id, yaw, v, g, rol, 1f, kleren);
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, float maat, GuhClothes... kleren) {
        BlockPos plek = Z2NpcPlekken.NPC.get(id);
        if (plek == null) {
            throw new IllegalStateException("no spot for zone-2 npc " + id);
        }
        Npcs.voegToe(new NpcDef(id, plek, yaw, v, List.of(kleren), g, rol, maat));
    }

    /** Adds the zone-2 NPCs and spots to the shared npc system (once, at mod construction). */
    public static synchronized void init() {
        if (klaar) {
            return;
        }
        klaar = true;
        // the shared maps: NPC spots (tracker, /tp), POIs (poi objectives through NpcHaken), explicit quest targets (QuestPlek)
        NpcPlekken.NPC.putAll(Z2NpcPlekken.NPC);
        NpcPlekken.POI.putAll(Z2NpcPlekken.POI);
        QuestPlek.POI.putAll(Z2NpcPlekken.POI);
        QuestPlek.GEBIED.putAll(Z2NpcPlekken.GEBIED_DOEL);
        QuestPlek.BAAS.putAll(Z2NpcPlekken.BAAS);
        // the quest tracker of fix round 1 (f-quests: NpcPlekken.GEBIED_DOEL / KAMPVUUR / BAAS) when it is in this tree
        vulTracker("GEBIED_DOEL", Z2NpcPlekken.GEBIED_DOEL);
        vulTracker("KAMPVUUR", Z2NpcPlekken.KAMPVUUR);
        vulTracker("BAAS", Z2NpcPlekken.BAAS);

        // --- Kandijkom: travel, the order, the guard -------------------------------------------------------------------------------
        npc("z2_kapitein", 0, GuhVariant.SNOW, Gedrag.STAAN, "luchtschip_z2", BALLONPET, BALLONBRIL, WATERSTAM_PARKA, ELFTOCHT_SJAAL);
        npc("z2_cafe", 90, GuhVariant.CHOCO, Gedrag.STAAN, "chocomelk", 1.1f, SMUL_BAKKERSMUTS, CHRISTMAS_SWEATER, SMUL_SCHORT);
        npc("z2_burgemeester", 180, GuhVariant.GOLDEN, Gedrag.STAAN, null, 1.15f, BALTO_SNEEUWMUTS, SAMEN_KNUFFELTRUITJE, BURGEMEESTERSSJERP);
        npc("z2_kwarts", 180, GuhVariant.SNOW, Gedrag.STAAN, null, 1.2f, BESTAAND_WACHTERSHELM, ELFTOCHT_TRUITJE, WINTER_SCARF);
        npc("z2_ordemeester", 180, GuhVariant.SNOW, Gedrag.STAAN, "ordetempel", 1.15f, STERRENKIJKERSMUTS, STERRENCAPE, SAMEN_KNUFFELTRUITJE);
        // --- Kandijkom: shops and services ------------------------------------------------------------------------------------------
        npc("z2_juwelier", 90, GuhVariant.MINT, Gedrag.STAAN, "juwelier", MONOCLE, CHRISTMAS_SWEATER, KNUS_SJAALTJE);
        npc("z2_smid", 90, GuhVariant.CHOCO, Gedrag.STAAN, "smid", 1.15f, BUILDER_HELMET, GOLF_TRUI, GRILL_SCHORT);
        npc("z2_winkelier", 270, GuhVariant.NORMAL, Gedrag.STAAN, "winkel", ELFTOCHT_OORWARMERS, STRIPED_SWEATER, BALTO_WANTJES);
        npc("z2_wapenhandelaar", 270, GuhVariant.SNOW, Gedrag.STAAN, "wapenwinkel", EYEPATCH, ELFTOCHT_TRUITJE, KNIGHT_ARMOUR);
        npc("z2_breister", 180, GuhVariant.MINT, Gedrag.STAAN, "kleermaker", THEEMUTSJE, SAMEN_KNUFFELTRUITJE, ELFTOCHT_SJAAL);
        npc("z2_veilingmeester", 180, GuhVariant.NORMAL, Gedrag.STAAN, "markt", KERMIS_HOED, CHRISTMAS_SWEATER, KERMIS_STRIK);
        npc("z2_schaatsjuf", 0, GuhVariant.SNOW, Gedrag.SLENTER, null, ELFTOCHT_SCHAATSMUTS, ELFTOCHT_TRUITJE, ELFTOCHT_SJAAL);
        npc("z2_gondelier", 270, GuhVariant.CHOCO, Gedrag.STAAN, "gondel", KARRETJES_CONDUCTEURSPETJE, GOLF_TRUI, WINTER_SCARF);
        // --- Kandijkom: citizens ------------------------------------------------------------------------------------------------------
        npc("z2_opa_sneeuwbal", 0, GuhVariant.SNOW, Gedrag.ZITTEN, null, SANTA_HAT, STRIPED_SWEATER, BIEB_LEESBRIL);
        npc("z2_bakker", 270, GuhVariant.NORMAL, Gedrag.STAAN, null, BAKKERSMUTSJE, CHRISTMAS_SWEATER, BAKKERSSCHORTJE);
        npc("z2_vlokje", 180, GuhVariant.MINT, Gedrag.SLENTER, null, 0.75f, BALTO_SNEEUWMUTS, ELFTOCHT_TRUITJE, BALTO_WANTJES);
        npc("z2_uitvinder", 90, GuhVariant.CHOCO, Gedrag.STAAN, null, RACEBRIL, GOLF_TRUI, TIMMER_GEREEDSCHAPSRIEM);
        npc("z2_galmpje", 270, GuhVariant.NORMAL, Gedrag.STAAN, null, KOORSTRIKJE, SAMEN_KNUFFELTRUITJE, ELFTOCHT_SJAAL);
        npc("z2_postguh", 0, GuhVariant.NORMAL, Gedrag.SLENTER, null, POLICE_CAP, STRIPED_SWEATER, CAMPINGMARKT_RUGZAK);
        npc("z2_sneeuwpop", 180, GuhVariant.SNOW, Gedrag.SLENTER, null, ELFTOCHT_OORWARMERS, CHRISTMAS_SWEATER, BALTO_WANTJES);
        npc("z2_kasguh", 90, GuhVariant.MINT, Gedrag.SLENTER, null, TUINHOEDJE, GOLF_TRUI, TUINSCHORTJE);
        npc("z2_winterslaap", 180, GuhVariant.CHOCO, Gedrag.ZITTEN, null, SLAAPMUTSJE, SAMEN_KNUFFELTRUITJE, PLUISSLOFJES);
        npc("z2_poollicht", 180, GuhVariant.SNOW, Gedrag.STAAN, null, MAANMUTSJE, STERRENCAPE, ELFTOCHT_TRUITJE);
        // --- out in the zone -----------------------------------------------------------------------------------------------------------
        npc("z2_gondel_boven", 90, GuhVariant.NORMAL, Gedrag.STAAN, "gondel_terug", KARRETJES_CONDUCTEURSPETJE, ELFTOCHT_TRUITJE, WINTER_SCARF);
        npc("z2_berggids", 90, GuhVariant.CHOCO, Gedrag.STAAN, null, BALTO_SNEEUWMUTS, WATERSTAM_PARKA, CAMPINGMARKT_RUGZAK);
        npc("z2_visser", 0, GuhVariant.SNOW, Gedrag.ZITTEN, null, VISSERSHOEDJE, GOLF_TRUI, VIS_AAN_DE_HAAK);
        npc("z2_beeldhouwer", 180, GuhVariant.NORMAL, Gedrag.STAAN, null, PIET_BERET, STRIPED_SWEATER, TIMMER_GEREEDSCHAPSRIEM);
        npc("z2_touwtje", 0, GuhVariant.MINT, Gedrag.STAAN, null, MIJNLAMPJE, WATERSTAM_PARKA, ELFTOCHT_SJAAL);
        npc("z2_prisma", 180, GuhVariant.SNOW, Gedrag.STAAN, null, 1.1f, HEMEL_AUREOOLTJE, STERRENCAPE, KNUS_SJAALTJE);
        // --- the Kristalwacht: a guard at every campfire (POI ridder_*) + two on the plaza; knitted jumpers under the helmet ------------
        GuhVariant[] kleuren = {GuhVariant.NORMAL, GuhVariant.CHOCO, GuhVariant.SNOW, GuhVariant.MINT};
        GuhClothes[] truien = {STRIPED_SWEATER, GOLF_TRUI, ELFTOCHT_TRUITJE, CHRISTMAS_SWEATER};
        GuhClothes[] sjaals = {WINTER_SCARF, ELFTOCHT_SJAAL, KNUS_SJAALTJE};
        int w = 0;
        for (String id : Z2NpcPlekken.NPC.keySet()) {
            if (id.startsWith("z2_wacht_")) {
                npc(id, 180, kleuren[w % kleuren.length], Gedrag.STAAN, "kampwacht", KNIGHT_HELMET, truien[w % truien.length], sjaals[w % sjaals.length]);
                w++;
            }
        }
        // --- the petrified guhs (crystal look, no AI: Z2Haken keeps them still; freed per player in z2_hq06) --------------------------
        npc("z2_versteend_zwier", 30, GuhVariant.KRISTALGUH, Gedrag.STAAN, "versteend", ELFTOCHT_SCHAATSMUTS, ELFTOCHT_TRUITJE);
        npc("z2_versteend_snoek", 120, GuhVariant.KRISTALGUH, Gedrag.STAAN, "versteend", VISSERSHOEDJE, VIS_AAN_DE_HAAK);
        npc("z2_versteend_pluk", 200, GuhVariant.KRISTALGUH, Gedrag.STAAN, "versteend", THEEMUTSJE, SAMEN_KNUFFELTRUITJE);
        npc("z2_versteend_ijsje", 300, GuhVariant.KRISTALGUH, Gedrag.STAAN, "versteend", 0.7f, BALTO_SNEEUWMUTS, BALTO_WANTJES);
    }

    public static boolean versteend(String id) {
        return id.startsWith("z2_versteend_");
    }

    @SuppressWarnings("unchecked")
    private static void vulTracker(String veld, Map<String, BlockPos> z2) {
        try {
            Field f = NpcPlekken.class.getField(veld);
            ((Map<String, BlockPos>) f.get(null)).putAll(z2);
        } catch (ReflectiveOperationException | ClassCastException e) {
            // (not in this tree: the f-quests tracker merges later and then finds them here)
        }
    }
}
