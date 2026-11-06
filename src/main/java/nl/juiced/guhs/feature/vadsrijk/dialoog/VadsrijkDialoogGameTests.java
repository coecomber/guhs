package nl.juiced.guhs.feature.vadsrijk.dialoog;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-DIALOOG: the server half of the Vadsrijk talking screen. Moods (explicit + guessed), factions and voices, the "Rijk" flag
 * (Vadsrijk only, the old screen elsewhere and on request), the option icons' kinds, quest data "npc:blij", and the "Wat moet ik
 * doen?" objective through the real QuestMotor click. Run: {@code runGameTestServer -Pgt=VadsrijkDialoog}.
 */
public class VadsrijkDialoogGameTests {
    private static final String B = "vadsrijk_dialoog";
    /** What the last Praat conversation of a player carried (after RijkDialoog's decoration). */
    private static final Map<UUID, CompoundTag> GEZIEN = new ConcurrentHashMap<>();
    private static boolean luistert;

    private static void luister() {
        if (!luistert) {
            luistert = true;
            Praat.versierder((p, sleutel, spreker, data) -> GEZIEN.put(p.getUUID(), data.copy()));
        }
    }

    @GuhTest(template = "empty", batch = B)
    public static void emoties(GameTestHelper helper) {
        helper.assertTrue(Emotie.van("blij") == Emotie.BLIJ && Emotie.van("SAD") == Emotie.VERDRIETIG && Emotie.van("verrast") == Emotie.VERRAST
                && Emotie.van("boos") == Emotie.BOOS && Emotie.van("??") == null && Emotie.van(null) == null, "mood words (nl + en)");
        helper.assertTrue(Emotie.raad("Hoera, je bent er!") == Emotie.BLIJ && Emotie.raad("Dankjewel, ridder.") == Emotie.BLIJ, "joy");
        helper.assertTrue(Emotie.raad("Wat?! Een draak?") == Emotie.VERRAST && Emotie.raad("Ooh, kijk eens!") == Emotie.VERRAST, "surprise");
        helper.assertTrue(Emotie.raad("Grr, die Mika's!") == Emotie.BOOS, "anger");
        helper.assertTrue(Emotie.raad("Helaas is alles grauw.") == Emotie.VERDRIETIG && Emotie.raad("Ik mis hem zo...") == Emotie.VERDRIETIG, "sadness");
        helper.assertTrue(Emotie.raad("Loop maar naar het plein.") == Emotie.NEUTRAAL && Emotie.raad("") == Emotie.NEUTRAAL, "most lines: neutral");
        RijkDialoog.emotie("test.t_dialoog.x", "boos");
        helper.assertTrue(RijkDialoog.emotieVan("test.t_dialoog.x") == Emotie.BOOS && RijkDialoog.emotieVan("vadsrijk.npc.q.iets.klaar.0") == Emotie.BLIJ
                && RijkDialoog.emotieVan("vadsrijk.npc.q.iets.klaar.1") == null, "explicit moods, and a hand-in's first line is glad");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void factiesEnStemmen(GameTestHelper helper) {
        helper.assertTrue(RijkDialoog.kenmerk(true, "vadsrijk.npc.jij", "player", false, null).equals(new RijkDialoog.Kenmerk("speler", "speler")), "you");
        helper.assertTrue(RijkDialoog.kenmerk(false, "vadsrijk.npc.verteller", "", false, null).factie().equals("verteller")
                && RijkDialoog.kenmerk(false, "vadsrijk.npc.aanwijzing.naam", "", false, null).stem().equals("verteller"), "the narrator, a sign");
        helper.assertTrue(RijkDialoog.kenmerk(false, "vadsrijk.z3npc.imker.naam", "guhs:mika", true, null).equals(new RijkDialoog.Kenmerk("mika", "mika")),
                "the Mika-imker (a Mika portrait)");
        helper.assertTrue(RijkDialoog.kenmerk(false, "", "vr_vahoegnir", true, null).stem().equals("draak"), "the dragon");
        helper.assertTrue(RijkDialoog.kenmerk(false, "", "guh", true, Npcs.van("z2_poollicht")).equals(new RijkDialoog.Kenmerk("z2", "guh")), "a zone-2 guh");
        helper.assertTrue(RijkDialoog.kenmerk(false, "", "guh", true, Npcs.van("z3_imker_1")).factie().equals("z3"), "a zone-3 guh");
        helper.assertTrue(RijkDialoog.kenmerk(false, "", "guh", true, Npcs.van("oma_truus")).factie().equals("z1"), "a zone-1 guh");
        helper.assertTrue(RijkDialoog.kenmerk(false, "", "guh", true, Npcs.van("trainer_ridder")).factie().equals("orde"), "a trainer: Knuffelorde");
        helper.assertTrue(RijkDialoog.kenmerk(false, "", "zombie", true, null).factie().equals("wezen"), "any other creature");
        float a = RijkDialoog.toon("guh", "oma_truus"), b = RijkDialoog.toon("guh", "oma_truus");
        helper.assertTrue(a == b && a >= 1.0f && a <= 1.7f && RijkDialoog.toon("mika", "x") < RijkDialoog.toon("guh", "x"), "voices: stable per NPC, Mika lower");
        helper.assertTrue(RijkDialoog.optieSoort("vadsrijk.npc.optie.ja").equals("opdracht") && RijkDialoog.optieSoort("vadsrijk.npc.optie.later").equals("weg")
                && RijkDialoog.optieSoort("vadsrijk.npc.optie.rol.winkel").equals("winkel") && RijkDialoog.optieSoort("vadsrijk.npc.optie.rol.bank").equals("bank")
                && RijkDialoog.optieSoort("vadsrijk.npc.optie.vlieg.kandijkom.slot").equals("reis_slot")
                && RijkDialoog.optieSoort("vadsrijk.npc.optie.reiskaart").equals("kaart") && RijkDialoog.optieSoort("iets.anders").isEmpty(), "option icons");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void rijkVlag(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        RijkDialoog.emotie("test.t_dialoog.x", "boos");
        CompoundTag data = scene("test.t_dialoog.x");
        RijkDialoog.versier(p, "vr_test", null, data);
        CompoundTag pg = data.getListOrEmpty("Paginas").getCompoundOrEmpty(0);
        helper.assertTrue(data.getBooleanOr("Rijk", false), "a vr_ conversation is drawn in the Vadsrijk style");
        helper.assertTrue(pg.getStringOr("Factie", "").equals("z1") && pg.getStringOr("Stem", "").equals("guh") && pg.getFloatOr("Toon", 0) > 0
                && pg.getStringOr("Emotie", "").equals("boos"), "page: faction, voice, pitch, explicit mood (" + pg + ")");
        CompoundTag ander = scene("x");
        RijkDialoog.versier(p, "erfenis_iets", null, ander);
        helper.assertTrue(!ander.getBooleanOr("Rijk", false) && !ander.getListOrEmpty("Paginas").getCompoundOrEmpty(0).contains("Factie"),
                "outside the Vadsrijk (Erfenis, Knuffeldal...): untouched, the old screen");
        RijkDialoog.OUD.add(p.getUUID());
        CompoundTag oud = scene("x");
        RijkDialoog.versier(p, "vr_test", null, oud);
        RijkDialoog.OUD.remove(p.getUUID());
        helper.assertTrue(!oud.getBooleanOr("Rijk", false), "/guhs vadsrijk dialoog oud: the old screen on request");
        // quest data "npc:blij": the line gets the mood, the speaker stays "npc"
        List<Praat.Regel> r = QuestMotor.regels(p, null, List.of("npc:blij", "jij"), "test.t_dialoog.q.");
        helper.assertTrue(r.size() == 2 && r.get(0).naamKey().isEmpty() && RijkDialoog.emotieVan("test.t_dialoog.q.0") == Emotie.BLIJ
                && r.get(1).spreker() == p, "\"npc:blij\" = this NPC, glad");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 100)
    public static void watMoetIkDoen(GameTestHelper helper) {
        luister();
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        RijkGuh r = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(2, 1, 2));
        r.stelIn(Npcs.van("rekruteerder"));
        helper.assertTrue(RijkDialoog.doel(p, "niemand_hier") == null, "an NPC without quest business: no button");
        CompoundTag aanbod = RijkDialoog.doel(p, "rekruteerder");
        helper.assertTrue(aanbod != null && aanbod.getStringOr("Soort", "").equals("aanbod") && aanbod.getListOrEmpty("Args").size() == 3,
                "the sergeant offers hq01: \"if you say yes\" + title/what/where");
        // through the real click: the conversation carries the objective
        GEZIEN.remove(p.getUUID());
        QuestMotor.klik(p, r);
        CompoundTag gezien = GEZIEN.get(p.getUUID());
        helper.assertTrue(gezien != null && gezien.getBooleanOr("Rijk", false) && gezien.getCompoundOrEmpty("Doel").getStringOr("Soort", "").equals("aanbod"),
                "QuestMotor's offer arrives with Rijk + Doel: " + gezien);
        ListTag pgs = gezien.getListOrEmpty("Paginas");
        helper.assertTrue(!pgs.isEmpty() && pgs.getCompoundOrEmpty(0).contains("Factie"), "every page decorated");
        QuestMotor.vergeet(p);
        Questlog.neemAan(p, Quests.van("hq01_welkom"));
        CompoundTag actief = RijkDialoog.doel(p, "rekruteerder");
        helper.assertTrue(actief != null && actief.getStringOr("Soort", "").equals("actief"), "after accepting: the current step");
        Questlog.wisAlles(p);
        helper.succeed();
    }

    /** A one-page scene data as Praat.scene builds it (a far zone-1 NPC by name). */
    private static CompoundTag scene(String tekst) {
        CompoundTag data = new CompoundTag();
        ListTag l = new ListTag();
        CompoundTag pg = new CompoundTag();
        pg.putInt("Spreker", -1);
        pg.putString("Naam", "vadsrijk.npc.naam.oma_truus");
        pg.putString("Tekst", tekst);
        CompoundTag portret = new CompoundTag();
        portret.putString("Type", "guhs:vr_npc_guh");
        pg.put("Portret", portret);
        l.add(pg);
        data.put("Paginas", l);
        return data;
    }
}
