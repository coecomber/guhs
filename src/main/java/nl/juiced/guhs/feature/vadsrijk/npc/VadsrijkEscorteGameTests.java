package nl.juiced.guhs.feature.vadsrijk.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.1: escorts for several players (bug: "Mijnwerker Mopje" followed one player and the next could not finish
 * hq09_druipgrotten). The NPC stays at its post, every player (or party) gets an own copy ({@link Escortes}).
 * <ul>
 * <li>two players at the same time: two copies, both finish (and hand in to the copy);</li>
 * <li>one after the other: the second starts right after the first finished;</li>
 * <li>the first player leaves (logs out / walks away / gives up): the copy vanishes at once and the step can be started again;</li>
 * <li>a party shares the copy and both get the arrival;</li>
 * <li>every escort quest of zones 1-6 (generic): click, a copy follows, the NPC stays, the arrival counts.</li>
 * </ul>
 */
public class VadsrijkEscorteGameTests {
    private static final String B = "vadsrijk_escorte";
    private static final String HQ09 = "hq09_druipgrotten";

    /** The NPC at its "post" next to the test (the real NPC spot is in the Vadsrijk). */
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

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        return p;
    }

    /** The player is at the escort step of hq09 (area reached, 12 cheered up). */
    private static Quest opEscorte(ServerPlayer p) {
        Quest q = Quests.van(HQ09);
        Questlog.neemAan(p, q);
        Questlog.tel(p, Quest.Doel.Type.GEBIED, d -> d == q.doelen().get(0), 1);
        Questlog.tel(p, Quest.Doel.Type.OPVROLIJK, d -> d == q.doelen().get(1), 12);
        return q;
    }

    /** Pretend the copy walked to the destination: the destination is where it stands (the walk itself is AI). */
    private static void aankomst(GameTestHelper helper, RijkGuh kopie) {
        Escortes.Escorte e = Escortes.vanKopie(kopie);
        helper.assertTrue(e != null, "the copy belongs to an escort");
        e.naar = kopie.blockPosition();
        Escortes.kijk(helper.getLevel().getServer());
    }

    private static void opruimen(GameTestHelper helper, ServerPlayer... ps) {
        Escortes.wisAlles(helper.getLevel().getServer());
        for (ServerPlayer p : ps) {
            QuestMotor.vergeet(p);
            nl.juiced.guhs.feature.verhaal.Praat.vergeet(p);
        }
        Partijen.wisAlles();
    }

    @GuhTest(template = "empty", batch = B)
    public static void tweeTegelijk(GameTestHelper helper) {
        Escortes.wisAlles(helper.getLevel().getServer());
        RijkGuh mopje = npc(helper, "mijnwerker_mopje", 1);
        ServerPlayer a = speler(helper), b = speler(helper);
        Quest q = opEscorte(a);
        opEscorte(b);
        QuestMotor.klik(a, mopje);
        QuestMotor.klik(b, mopje);
        RijkGuh ka = Escortes.kopieVan(a, "mijnwerker_mopje"), kb = Escortes.kopieVan(b, "mijnwerker_mopje");
        helper.assertTrue(ka != null && kb != null && ka != kb, "each player has an own copy of Mopje");
        helper.assertTrue(mopje.volgt() == null && mopje.isAlive(), "Mopje herself stays at her post");
        helper.assertTrue(a.getUUID().equals(ka.volgt()) && b.getUUID().equals(kb.volgt()), "each copy follows its own player");
        helper.assertTrue(QuestStap.van(a, q).doel() != null, "the tracker points somewhere");
        aankomst(helper, ka);
        aankomst(helper, kb);
        helper.assertTrue(Questlog.klaar(a, HQ09) && Questlog.klaar(b, HQ09), "both delivered Mopje and handed hq09 in");
        helper.assertTrue(Questlog.actief(a, "hq10_zwamtempel") && Questlog.actief(b, "hq10_zwamtempel"), "the next quest started for both");
        opruimen(helper, a, b);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void naElkaar(GameTestHelper helper) {
        Escortes.wisAlles(helper.getLevel().getServer());
        RijkGuh mopje = npc(helper, "mijnwerker_mopje", 1);
        ServerPlayer a = speler(helper), b = speler(helper);
        opEscorte(a);
        QuestMotor.klik(a, mopje);
        aankomst(helper, Escortes.kopieVan(a, "mijnwerker_mopje"));
        helper.assertTrue(Questlog.klaar(a, HQ09), "the first player finished");
        opEscorte(b);
        QuestMotor.klik(b, mopje);
        RijkGuh kb = Escortes.kopieVan(b, "mijnwerker_mopje");
        helper.assertTrue(kb != null && b.getUUID().equals(kb.volgt()), "right after, the second player's own copy follows");
        aankomst(helper, kb);
        helper.assertTrue(Questlog.klaar(b, HQ09), "the second player finished too");
        opruimen(helper, a, b);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void resetAlsSpelerWeggaat(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        Escortes.wisAlles(server);
        RijkGuh mopje = npc(helper, "mijnwerker_mopje", 1);
        ServerPlayer a = speler(helper), b = speler(helper);
        Quest q = opEscorte(a);
        opEscorte(b);
        // 1. walks away (or another dimension): gone at the next check
        QuestMotor.klik(a, mopje);
        RijkGuh ka = Escortes.kopieVan(a, "mijnwerker_mopje");
        helper.assertTrue(ka != null, "a copy follows A");
        a.snapTo(a.getX() + Escortes.KWIJT + 10, a.getY(), a.getZ());
        Escortes.kijk(server);
        helper.assertTrue(!ka.isAlive() && Escortes.alle().isEmpty(), "A walked off: the copy vanished at once");
        a.snapTo(mopje.getX() + 1, mopje.getY(), mopje.getZ());
        // 2. gives the quest up
        QuestMotor.klik(a, mopje);
        ka = Escortes.kopieVan(a, "mijnwerker_mopje");
        helper.assertTrue(ka != null, "A could start again right away");
        Questlog.vergeet(a, q.id());
        Escortes.kijk(server);
        helper.assertTrue(!ka.isAlive() && Escortes.alle().isEmpty(), "A gave the quest up: the copy vanished");
        opEscorte(a);
        // 3. logs out: gone, and B starts at once
        QuestMotor.klik(a, mopje);
        ka = Escortes.kopieVan(a, "mijnwerker_mopje");
        helper.assertTrue(ka != null, "A started a third time");
        a.connection.disconnect(net.minecraft.network.chat.Component.literal("test"));
        a.disconnect();
        Escortes.kijk(server);
        helper.assertTrue(!ka.isAlive() && Escortes.alle().isEmpty(), "A logged out: the copy vanished at once");
        QuestMotor.klik(b, mopje);
        RijkGuh kb = Escortes.kopieVan(b, "mijnwerker_mopje");
        helper.assertTrue(kb != null && b.getUUID().equals(kb.volgt()), "B starts immediately after A left");
        aankomst(helper, kb);
        helper.assertTrue(Questlog.klaar(b, HQ09), "B finished");
        helper.assertTrue(mopje.isAlive() && mopje.volgt() == null, "Mopje never left her post");
        opruimen(helper, a, b);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void partyDeeltEscorte(GameTestHelper helper) {
        Escortes.wisAlles(helper.getLevel().getServer());
        Partijen.wisAlles();
        RijkGuh mopje = npc(helper, "mijnwerker_mopje", 1);
        ServerPlayer a = speler(helper), b = speler(helper), c = speler(helper);
        opEscorte(a);
        opEscorte(b);
        opEscorte(c);
        Partijen.maakParty(a);
        Partijen.nodigUit(a, b);
        Partijen.accepteer(b);
        helper.assertTrue(Partijen.zelfdeParty(a, b), "A and B are a party");
        QuestMotor.klik(a, mopje);
        RijkGuh ka = Escortes.kopieVan(a, "mijnwerker_mopje");
        QuestMotor.klik(b, mopje);
        helper.assertTrue(Escortes.kopieVan(b, "mijnwerker_mopje") == ka && Escortes.alle().size() == 1, "B (same party, same step) shares A's copy");
        QuestMotor.klik(c, ka);
        helper.assertTrue(Escortes.kopieVan(c, "mijnwerker_mopje") == null, "a stranger cannot take the party's copy");
        aankomst(helper, ka);
        helper.assertTrue(Questlog.klaar(a, HQ09) && Questlog.klaar(b, HQ09), "both party members got the arrival (and handed in)");
        helper.assertTrue(Questlog.actief(c, HQ09) && !Questlog.allesKlaar(c, Quests.van(HQ09)), "the stranger still has the step");
        QuestMotor.klik(c, mopje);
        aankomst(helper, Escortes.kopieVan(c, "mijnwerker_mopje"));
        helper.assertTrue(Questlog.klaar(c, HQ09), "the stranger does it with an own copy");
        opruimen(helper, a, b, c);
        helper.succeed();
    }

    /** Generic: every ESCORTE objective of zones 1-6 (hq09, z10_rammel, z3_s02, z5_s07) for two players at the same time. */
    @GuhTest(template = "empty", batch = B)
    public static void alleEscortesTweeSpelers(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        int n = 0;
        for (Quest q : Quests.alle().values()) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (d.type() != Quest.Doel.Type.ESCORTE) {
                    continue;
                }
                n++;
                Escortes.wisAlles(server);
                helper.assertTrue(Escortes.bestemming(d) != null, q.id() + ": the escort destination exists");
                RijkGuh g = npc(helper, d.doel(), 1 + n);
                ServerPlayer a = speler(helper), b = speler(helper);
                for (ServerPlayer p : new ServerPlayer[] {a, b}) {
                    Questlog.neemAan(p, q);
                    QuestMotor.klik(p, g);
                    RijkGuh k = Escortes.kopieVan(p, d.doel());
                    helper.assertTrue(k != null && p.getUUID().equals(k.volgt()), q.id() + ": a copy of " + d.doel() + " follows each player");
                }
                helper.assertTrue(g.volgt() == null, q.id() + ": the NPC itself stays");
                for (ServerPlayer p : new ServerPlayer[] {a, b}) {
                    aankomst(helper, Escortes.kopieVan(p, d.doel()));
                    helper.assertTrue(Questlog.doelKlaar(p, q, i) || Questlog.klaar(p, q.id()), q.id() + ": the arrival counts for each player");
                }
                opruimen(helper, a, b);
                g.discard();
            }
        }
        helper.assertTrue(n >= 4, "the 4 escort quests were checked: " + n);
        helper.succeed();
    }
}
