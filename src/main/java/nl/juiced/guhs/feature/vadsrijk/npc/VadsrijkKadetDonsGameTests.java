package nl.juiced.guhs.feature.vadsrijk.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.27 (Luc, live): "Proef van de Wolkwacht: de verdwaalde kadet" (z5_s07_kadet). He picked Kadet Dons up and the quest did
 * not seem to notice: the objective's fixed target "npc:z5_kadet_dons" won in the tracker, so the line and the beam stayed on
 * Dons' pier while his copy already walked along, and the arrival only counted in the north-east corner of the oefenplein, 27
 * blocks from Kapitein Veerkracht, to whom you bring him.
 * <p>Through the real interaction path ({@code Player.interactOn}, the 1.10.23 lesson) and the real arrival check
 * ({@link Escortes#kijk}); only the walk itself (AI) is not played.
 */
public class VadsrijkKadetDonsGameTests {
    private static final String B = "vadsrijk_kadet_dons";
    private static final String Q = "z5_s07_kadet", DONS = "z5_kadet_dons", KAPITEIN = "z5_veerkracht";

    private static RijkGuh npc(GameTestHelper helper, String id, int dx) {
        ServerLevel sl = helper.getLevel();
        RijkGuh g = NpcRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
        g.stelIn(Npcs.van(id));
        BlockPos b = helper.absolutePos(new BlockPos(dx, 1, 2));
        g.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        g.setNoAi(true);
        sl.addFreshEntity(g);
        return g;
    }

    private static void klik(ServerPlayer p, RijkGuh g) {
        p.interactOn(g, InteractionHand.MAIN_HAND, g.getBoundingBox().getCenter().subtract(g.position()));
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void ophalenTeltEnBrengenNaarDeKapitein(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        Escortes.wisAlles(server);
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        Quest q = Quests.van(Q);
        helper.assertTrue(q != null && q.doelen().size() == 1 && q.doelen().get(0).type() == Quest.Doel.Type.ESCORTE && q.inleveren().equals(KAPITEIN),
                "self-check: z5_s07_kadet is one escort, handed in to Kapitein Veerkracht");
        Quest.Doel d = q.doelen().get(0);
        BlockPos naar = Escortes.bestemming(d);
        helper.assertTrue(naar != null, "the destination z5_academie exists");
        Questlog.neemAan(p, q);
        RijkGuh dons = npc(helper, DONS, 1), kapitein = npc(helper, KAPITEIN, 3);

        // 1. before the pick-up: the tracker sends you to Kadet Dons
        QuestStap voor = QuestStap.van(p, q);
        helper.assertTrue(voor.waar().toString().contains("plek.npc." + DONS) && voor.doel() != null && !naar.equals(voor.doel()),
                "before the pick-up the tracker points at Kadet Dons: " + voor.waar() + " @ " + voor.doel());

        // 2. the real right-click: a copy follows, Dons himself stays, and the tracker NOW points at the Academie
        klik(p, dons);
        RijkGuh kopie = Escortes.kopieVan(p, DONS);
        helper.assertTrue(kopie != null && p.getUUID().equals(kopie.volgt()), "a real right-click on Kadet Dons makes his copy follow");
        helper.assertTrue(dons.isAlive() && dons.volgt() == null, "Dons himself stays on his pier");
        QuestStap na = QuestStap.van(p, q);
        helper.assertTrue(na.waar().toString().contains("plek.poi.z5_academie") && !na.waar().toString().contains("plek.npc." + DONS),
                "after the pick-up the tracker names the oefenplein, not Dons' pier: " + na.waar());
        helper.assertTrue(naar.equals(na.doel()), "after the pick-up the beam points at the destination " + naar + ": " + na.doel());

        // 3. neither at the spot nor at the captain (his post is far from this test): nothing yet
        Escortes.kijk(server);
        helper.assertTrue(kopie.isAlive() && !Questlog.doelKlaar(p, q, 0), "not there yet: the step stays open and the copy keeps following");

        // 4. the copy next to Kapitein Veerkracht (27 blocks from the spot z5_academie): that IS the arrival
        NpcDef kap = Npcs.van(KAPITEIN);
        helper.assertTrue(kap.plek().closerThan(naar, Escortes.BIJ_BESTEMMING) && !kap.plek().closerThan(naar, NpcPlekken.POI_STRAAL + 2),
                "self-check: the captain waits at the Academie, but outside the arrival radius of the spot");
        Npcs.testLevend(KAPITEIN, kapitein);
        Escortes.kijk(server);
        helper.assertTrue(Questlog.doelKlaar(p, q, 0) && Questlog.allesKlaar(p, q), "Kadet Dons brought to Kapitein Veerkracht: the escort counts");
        helper.assertTrue(QuestStap.van(p, q).waar().toString().contains("plek.npc." + KAPITEIN), "the tracker now says: hand in to the captain");

        // 5. the real right-click on the captain hands the quest in
        klik(p, kapitein);
        helper.assertTrue(Questlog.klaar(p, Q), "a real right-click on Kapitein Veerkracht hands the quest in");

        Npcs.testLevend(KAPITEIN, null);
        Escortes.wisAlles(server);
        QuestMotor.vergeet(p);
        nl.juiced.guhs.feature.verhaal.Praat.vergeet(p);
        dons.discard();
        kapitein.discard();
        Questlog.wisAlles(p);
        helper.succeed();
    }

    /** The arrival at the spot itself still counts (the copy is there, the captain is not near). */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void aankomstOpHetOefenpleinTelt(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        Escortes.wisAlles(server);
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        Quest q = Quests.van(Q);
        Questlog.neemAan(p, q);
        RijkGuh dons = npc(helper, DONS, 1);
        klik(p, dons);
        RijkGuh kopie = Escortes.kopieVan(p, DONS);
        helper.assertTrue(kopie != null, "a copy follows");
        Escortes.Escorte e = Escortes.vanKopie(kopie);
        e.naar = kopie.blockPosition();   // (the walk is AI: the destination is where the copy stands)
        Escortes.kijk(server);
        helper.assertTrue(Questlog.doelKlaar(p, q, 0), "the arrival at the destination counts");
        Escortes.wisAlles(server);
        QuestMotor.vergeet(p);
        nl.juiced.guhs.feature.verhaal.Praat.vergeet(p);
        dons.discard();
        Questlog.wisAlles(p);
        helper.succeed();
    }
}
