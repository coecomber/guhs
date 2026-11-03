package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.ProefKlodder;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * The npc slice's few focused tests (SLICE_BRIEF: few): the quest data hangs together, quest progress through the kern hooks
 * (GEBIED_BETREDEN, KAMPVUUR, VERSLAGEN), talking/handing in through an NPC guh, the secret clues, and the end of the story
 * setting {@code zone1_klaar} in {@code SpelerData.extra("npc")}.
 */
public class VadsrijkNpcGameTests {
    private static final String B = "vadsrijk_npc";

    @GuhTest(template = "empty", batch = B)
    public static void questDataKlopt(GameTestHelper helper) {
        helper.assertTrue(Quests.alle().size() >= 40, "the quests were read: " + Quests.alle().size());
        long hoofd = Quests.alle().values().stream().filter(Quest::hoofd).count();
        long zij = Quests.alle().values().stream().filter(q -> q.soort() == Quest.Soort.ZIJ).count();
        helper.assertTrue(hoofd >= 10 && zij >= 20, "~10 main and ~20 side quests: " + hoofd + "/" + zij);
        for (Quest q : Quests.alle().values()) {
            helper.assertTrue(Npcs.van(q.gever()) != null && Npcs.van(q.inleveren()) != null, q.id() + ": its NPCs exist");
            for (Quest.Doel d : q.doelen()) {
                if (d.doel().startsWith("poi:")) {
                    helper.assertTrue(NpcPlekken.poi(d.doel().substring(4)) != null, q.id() + ": poi " + d.doel());
                }
                if (d.type() == Quest.Doel.Type.VERZAMEL) {
                    helper.assertTrue(Questlog.item(d.doel()) != net.minecraft.world.item.Items.AIR, q.id() + ": item " + d.doel());
                }
            }
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void voortgangViaHaken(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        // GEBIED_BETREDEN: "zwamzoet|haven" counts either
        Quest q6 = Quests.van("hq06_zwamzoet");
        Questlog.neemAan(p, q6);
        Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, "haven"));
        helper.assertTrue(Questlog.doelKlaar(p, q6, 0) && !Questlog.allesKlaar(p, q6), "arriving at the haven counts, the talk not yet");
        // KAMPVUUR: a campfire inside Knuffelkamp
        Quest q4 = Quests.van("hq04_kampvuur");
        Questlog.neemAan(p, q4);
        Haken.KAMPVUUR.forEach(h -> h.accept(p, new BlockPos(5, 65, 1020)));
        helper.assertTrue(Questlog.doelKlaar(p, q4, 0), "a campfire in Knuffelkamp counts");
        // VERSLAGEN: the bestiary counts every kind, area objectives count by area
        ProefKlodder k = KernRegistry.PROEFKLODDER.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(k != null, "a test klodder");
        k.snapTo(p.getX(), p.getY(), p.getZ());
        Haken.VERSLAGEN.forEach(h -> h.accept(new Haken.Verslagen(k, Set.of(p))));
        helper.assertTrue(Questlog.map(p, "wezens").getIntOr("guhs:vr_proefklodder", 0) == 1, "the bestiary page of the test klodder");
        Quest q12 = Quests.van("z12_kleuren");
        Questlog.neemAan(p, q12);
        for (int i = 0; i < 15; i++) {
            Questlog.tel(p, Quest.Doel.Type.OPVROLIJK, d -> NpcHaken.pastMob(d.doel(), "guhs:vr_mob_klodder", "klodderweide"), 1);
        }
        Questlog.tel(p, Quest.Doel.Type.OPVROLIJK, d -> NpcHaken.pastMob(d.doel(), "guhs:vr_mob_klodder", "sporenwoud"), 1);
        helper.assertTrue(Questlog.waarde(p, q12, 0) == 15 && Questlog.allesKlaar(p, q12), "15 in the Klodderweide, capped");
        // the clues: the first one starts the secret quest, five complete its first objective
        Quest spion = Quests.van("geheim_spion");
        for (var e : NpcPlekken.AANWIJZING.entrySet()) {
            NpcHaken.vind(p, helper.getLevel(), "aanwijzingen", e.getKey(), e.getValue());
        }
        helper.assertTrue(Questlog.actief(p, spion.id()) && Questlog.doelKlaar(p, spion, 0), "5 clues: the spy quest is on and its clues done");
        helper.assertTrue(QuestMotor.klaarVoorOntmaskeren(p) && Verhaal.magOntmaskeren(p), "Akim may be unmasked now");
        helper.assertTrue(Questlog.doelKlaar(p, spion, 1) && !Questlog.doelKlaar(p, spion, 2), "unmasking counted (bazen's right-click test)");
        NpcApi.meld(p, "spion", "gevangen");
        helper.assertTrue(Questlog.allesKlaar(p, spion) && NpcMarkers.marker(p, "grootmeester") == NpcMarkers.INLEVEREN,
                "caught: back to the Grootmeester (?)");
        Questlog.wisAlles(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void inleverenEnZone1Klaar(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        RijkGuh kok = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(2, 1, 2));
        kok.stelIn(Npcs.van("kok"));
        // hq01: the place, then talking to the cook = talk objective + hand-in in one click
        Quest q1 = Quests.van("hq01_welkom");
        helper.assertTrue(Questlog.beschikbaar(p, q1) && NpcMarkers.marker(p, "rekruteerder") == NpcMarkers.HOOFD, "the sergeant has a gold !");
        Questlog.neemAan(p, q1);
        Questlog.tel(p, Quest.Doel.Type.GEBIED, d -> d.doel().equals("poi:kampplein"), 1);
        helper.assertTrue(NpcMarkers.marker(p, "kok") == NpcMarkers.PRATEN, "the cook shows a ? (talk)");
        QuestMotor.klik(p, kok);
        helper.assertTrue(Questlog.klaar(p, q1.id()), "talking to the cook finished hq01");
        helper.assertTrue(Questlog.beschikbaar(p, Quests.van("hq02_pad")), "and opened hq02");
        QuestMotor.vergeet(p);
        // hq12: the Mika pin to the Grootmeester -> the cliffhanger -> zone1_klaar
        RijkGuh gm = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(4, 1, 2));
        gm.stelIn(Npcs.van("grootmeester"));
        Quest q12 = Quests.van("hq12_schaduw");
        Questlog.neemAan(p, q12);
        helper.assertTrue(!NpcApi.zone1Klaar(p), "not done yet");
        Questlog.geef(p, new ItemStack(Questlog.item("guhs:vr_npc_mika_speldje")));
        QuestMotor.klik(p, gm);
        helper.assertTrue(Questlog.klaar(p, q12.id()), "hq12 handed in");
        QuestMotor.afronden(p);   // (the hand-in dialogue closes: its action runs)
        helper.assertTrue(NpcApi.zone1Klaar(p) && nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).extra("npc").getBooleanOr("zone1_klaar", false),
                "the cliffhanger set zone1_klaar in extra(\"npc\")");
        Questlog.wisAlles(p);
        helper.succeed();
    }
}
