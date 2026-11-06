package nl.juiced.guhs.feature.vadsrijk.zone5.verhaal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone 5 "Hoogvads", slice t-z5verhaal (few, focused): (1) every zone-5 quest step names WHO and WHERE (an explicit target with a
 * "where" text), every NPC has a spot/name/where text inside Hoogvads, and the detective data is complete; (2) the whole main line
 * "Verraad in de stad" walked through its real hooks (interrogations by clicking the suspect + asking the right question, soup for
 * the refugees, a WRONG then the right accusation on the board, the finale fallback, the lullaby, the cliffhanger) up to
 * {@code zone5_klaar}; (3) the finale hook interface (a registered fight wins over the fallback); (4) the airship gate to Hoogvads.
 */
public class Z5VerhaalGameTests {
    private static final String B = "vadsrijk_z5_verhaal";

    private static List<Quest> z5() {
        List<Quest> l = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (q.id().startsWith("z5_")) {
                l.add(q);
            }
        }
        return l;
    }

    @GuhTest(template = "empty", batch = B)
    public static void elkeStapWieEnWaar(GameTestHelper helper) {
        Language t = Language.getInstance();
        List<Quest> z5 = z5();
        long hoofd = z5.stream().filter(Quest::hoofd).count(), zij = z5.stream().filter(q -> q.soort() == Quest.Soort.ZIJ).count();
        helper.assertTrue(hoofd == 10 && zij >= 10, "zone 5: 10 main and ~10 side quests: " + hoofd + "/" + zij);
        helper.assertTrue(Quests.van("hq01_welkom") != null && Quests.van("z3_hq01_boom") != null, "zones 1-3 still load");
        List<Quest> alle = new ArrayList<>(Quests.alle().values());
        helper.assertTrue(alle.indexOf(Quests.van(Z5Haken.HQ01)) > alle.indexOf(Quests.van("z3_hq15_hoogvads")), "zone 5 comes after zone 3 in the questlog");
        for (String id : Z5VerhaalPlekken.NPC.keySet()) {
            NpcDef d = Npcs.van(id);
            helper.assertTrue(d != null && d.plek() != null, "NPC " + id + " is registered with a spot");
            helper.assertTrue(t.has(d.naamKey()) && t.has("vadsrijk.npc.plek.npc." + id) && t.has("vadsrijk.npc.praat." + id + ".1"),
                    "NPC " + id + ": name + where + lines");
            helper.assertTrue(Z5VerhaalPlekken.zone5(d.plek().getX(), d.plek().getY(), d.plek().getZ()), "NPC " + id + " stands in Hoogvads");
        }
        for (Quest q : z5) {
            for (String wie : List.of(q.gever(), q.inleveren())) {
                helper.assertTrue(Npcs.van(wie) != null && t.has("vadsrijk.npc.plek.npc." + wie), q.id() + ": who (" + wie + ") and where");
            }
            helper.assertTrue(t.has(q.titelKey()) && t.has(q.tekstKey()) && t.has(q.sleutel() + ".bezig"), q.id() + ": texts");
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                helper.assertTrue(t.has(q.doelKey(i)), q.id() + " step " + i + ": what");
                helper.assertTrue(!d.plek().isEmpty() && QuestPlek.pos(helper.getLevel(), d.plek()) != null, q.id() + " step " + i + ": a target (" + d.plek() + ")");
                helper.assertTrue(t.has(QuestPlek.tekstKey(d.plek())), q.id() + " step " + i + ": where text " + QuestPlek.tekstKey(d.plek()));
            }
        }
        // the detective data
        helper.assertTrue(Z5Speurwerk.BEWIJS.size() == 10 && Z5Speurwerk.VERHOREN.size() == 7 && Z5Speurwerk.VERDACHTEN.size() == 4,
                "10 clues, 7 interrogations, 4 suspects");
        helper.assertTrue(Z5Speurwerk.VERDACHTEN.values().stream().filter(Z5Speurwerk.Verdachte::verrader).count() == 1, "one traitor");
        for (String b : Z5Speurwerk.BEWIJS) {
            helper.assertTrue(t.has(Z5Verhaal.Z + "bewijs." + b + ".titel") && t.has(Z5Verhaal.Z + "bewijs." + b), "clue " + b + ": texts");
        }
        for (Z5Speurwerk.Verhoor v : Z5Speurwerk.VERHOREN.values()) {
            for (int i = 0; i < v.intro().size(); i++) {
                helper.assertTrue(t.has(Z5Verhaal.Z + "verhoor." + v.id() + ".intro." + i), v.id() + " intro " + i);
            }
            for (int k = 0; k < v.vragen().size(); k++) {
                helper.assertTrue(t.has(Z5Verhaal.Z + "verhoor." + v.id() + ".vraag." + k), v.id() + " question " + k);
                for (int i = 0; i < v.vragen().get(k).size(); i++) {
                    helper.assertTrue(t.has(Z5Verhaal.Z + "verhoor." + v.id() + ".antwoord." + k + "." + i), v.id() + " answer " + k + "." + i);
                }
            }
        }
        for (String id : Z5VerhaalPlekken.UITKIJK_IDS) {
            helper.assertTrue(t.has("vadsrijk.wereld.uitkijk." + id) && Z5VerhaalPlekken.poi(id) != null, "lookout " + id);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 600)
    public static void hoofdlijnViaApi(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        Z5VerhaalRegistry.klaarzetten(p);
        helper.assertTrue(Questlog.actief(p, Z5Haken.HQ01), "zone 4 done: z5_hq01 starts by itself");
        List<Quest> hoofd = z5().stream().filter(Quest::hoofd).toList();
        helper.assertTrue(hoofd.stream().map(Quest::id).toList().equals(Z5Haken.HOOFD), "the main quests in story order");
        String vorige = "z3_hq15_hoogvads";
        int x = 0;
        for (Quest q : hoofd) {
            helper.assertTrue(q.na().contains(vorige), q.id() + " follows " + vorige);
            vorige = q.id();
            if (Questlog.status(p, q.id()) == 0) {
                Questlog.neemAan(p, q);
            }
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (Questlog.doelKlaar(p, q, i) || d.type() == Quest.Doel.Type.PRAAT && d.doel().equals(q.inleveren())) {
                    continue;   // (already counted by an earlier hook, or the hand-in click does it)
                }
                doe(helper, p, q, d, x++);
                helper.assertTrue(Questlog.doelKlaar(p, q, i), q.id() + " step " + i + " (" + d.type() + " " + d.doel() + ") counted");
            }
            Praat.vergeet(p);
            RijkGuh inl = guh(helper, q.inleveren(), x++);
            QuestMotor.klik(p, inl);
            helper.assertTrue(Questlog.klaar(p, q.id()), q.id() + " handed in at " + q.inleveren());
            QuestMotor.afronden(p);   // (the dialogue closes: the quest action runs)
            inl.discard();
        }
        helper.assertTrue(Z5VerhaalApi.zone5Klaar(p), "the lullaby set zone5_klaar");
        helper.assertTrue(Z5Speurwerk.aantal(p) == 10, "all 10 clues in the Speurdersboekje: " + Z5Speurwerk.aantal(p));
        helper.assertTrue(Questlog.map(p, "z5_brieven").getBooleanOr("ringvinger", false), "the Grauwe brief to the Ringvinger");
        helper.assertTrue(Z5VerhaalApi.ontmaskerd(p), "the spy was unmasked");
        schoon(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void finaleHaak(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        Z5VerhaalRegistry.naarStap(p, 8);
        NpcApi.meld(p, "z5_spion", "x");   // (noise: nothing)
        Quest q = Quests.van(Z5Haken.HQ08);
        Questlog.tel(p, Quest.Doel.Type.GEBIED, d -> d == q.doelen().get(0), 1);
        helper.assertTrue(Z5Finale.volgende(p) == Z5Finale.Fase.SPION, "after the blowhole: the spy is next");
        List<String> gestart = new ArrayList<>();
        Z5Finale.registreer(Z5Finale.Fase.SPION, s -> gestart.add("spion"));
        Z5Finale.registreer(Z5Finale.Fase.DROOM, s -> gestart.add("droom"));
        try {
            Z5Finale.start(p);
            helper.assertTrue(gestart.equals(List.of("spion")) && !Z5Verhaal.bezig(p), "a registered fight wins over the fallback scene");
            Z5Finale.spionVerslagen(p);
            helper.assertTrue(gestart.equals(List.of("spion", "droom")) && Z5Finale.volgende(p) == Z5Finale.Fase.DROOM, "the dream starts after the spy");
            Z5Finale.droomOpgevrolijkt(p);
            helper.assertTrue(Z5Finale.volgende(p) == null && Questlog.allesKlaar(p, q), "both phases counted: back to the queen");
        } finally {
            Z5Finale.vergeetAlles();
        }
        schoon(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void luchtschipNaarHoogvads(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        Questlog.wisAlles(p);
        for (String vlag : List.of("zone1_klaar", "zone2_klaar", "zone3_klaar")) {
            Questlog.zetVlag(p, vlag, true);
        }
        Spelers.zetLevel(p, 56);
        helper.assertTrue(QuestMotor.reisSlot(p, "hoogvads") != null && !QuestMotor.reisDoelen(p, "luchtschipper").contains("hoogvads"),
                "before zone 4: Hoogvads is locked and not on the captains' lists");
        Questlog.zetVlag(p, Z5Verhaal.VLAG_ZONE4, true);
        helper.assertTrue(QuestMotor.reisSlot(p, "hoogvads") == null && QuestMotor.reisDoelen(p, "luchtschipper").contains("hoogvads"),
                "after zone 4 at lv 56: the line is open");
        helper.assertTrue(QuestMotor.reisDoelen(p, "z5_havenmeester").containsAll(Z5VerhaalRegistry.HAVEN_REIZEN), "the havenmeester flies everywhere");
        Spelers.zetLevel(p, Z5Verhaal.MIN_LEVEL - 5);
        helper.assertTrue(QuestMotor.reisSlot(p, "hoogvads") != null, "below the line's level: locked");
        schoon(p);
        Questlog.wisAlles(p);
        helper.succeed();
    }

    // ---- helpers ------------------------------------------------------------------------------------------------------------------

    private static void schoon(ServerPlayer p) {
        Z5VerhaalRegistry.vergeet(p);
        Praat.vergeet(p);
        QuestMotor.vergeet(p);
    }

    /** Completes one objective through the hook it really uses in game. */
    private static void doe(GameTestHelper helper, ServerPlayer p, Quest q, Quest.Doel d, int x) {
        switch (d.type()) {
            case PRAAT -> {
                RijkGuh g = guh(helper, d.doel(), x);
                QuestMotor.klik(p, g);
                QuestMotor.afronden(p);
                g.discard();
            }
            case GEBIED -> {
                op(helper, p, Z5VerhaalPlekken.poi(d.doel().substring(4)));
                Z5Verhaal.spoel(p);   // (a scene the spot started: the finale fallback, the lullaby, the cliffhanger)
            }
            case MELD -> meld(helper, p, q, d, x);
            default -> helper.fail(q.id() + ": no test path for " + d.type());
        }
    }

    private static void meld(GameTestHelper helper, ServerPlayer p, Quest q, Quest.Doel d, int x) {
        String[] s = d.doel().split(":", 2);
        switch (s[0]) {
            case "z5_verhoor" -> {
                Z5Speurwerk.Verhoor v = Z5Speurwerk.VERHOREN.get(s[1]);
                RijkGuh g = guh(helper, v.npc(), x);
                QuestMotor.klik(p, g);   // (KLIK_VOOR opens the interrogation)
                for (int k = 0; k < v.vragen().size(); k++) {
                    if (k != v.sleutel()) {
                        helper.assertTrue(Z5Speurwerk.vraagVoorTest(p, k), v.id() + ": the interrogation is open");
                        helper.assertTrue(!Z5Speurwerk.heeft(p, v.bewijs()), v.id() + ": a side question gives no clue");
                    }
                }
                helper.assertTrue(Z5Speurwerk.vraagVoorTest(p, v.sleutel()) && Z5Speurwerk.heeft(p, v.bewijs()), v.id() + ": the right question gives " + v.bewijs());
                Praat.vergeet(p);
                g.discard();
            }
            case "z5_soep" -> {
                for (int i = 1; i <= 4; i++) {
                    RijkGuh g = guh(helper, "z5_vluchteling_" + i, x + i);
                    QuestMotor.klik(p, g);
                    QuestMotor.klik(p, g);   // (twice: the second bowl does not count)
                    g.discard();
                }
                Praat.vergeet(p);
            }
            case "z5_beschuldigd" -> {
                helper.assertTrue(!Z5Speurwerk.beschuldig(p, null, "gouddraad") && !Questlog.doelKlaar(p, q, 0), "a wrong accusation counts nothing");
                helper.assertTrue(Z5Speurwerk.beschuldig(p, null, "zachtpels"), "the Chancellor is the traitor");
                Praat.vergeet(p);
            }
            case "z5_zilverstaart", "z5_droom" -> {
                Z5Verhaal.spoel(p);   // (the finale fallback started at the blowhole: both phases)
                if (!Questlog.doelKlaar(p, q, q.doelen().indexOf(d))) {
                    op(helper, p, Z5VerhaalPlekken.SPUITGAT);
                    Z5Verhaal.spoel(p);
                }
            }
            default -> {
                switch (d.doel()) {
                    case "z5:inspecteur" -> {
                        RijkGuh g = guh(helper, "z5_inspecteur", x);
                        QuestMotor.klik(p, g);
                        Praat.vergeet(p);
                        g.discard();
                    }
                    case "z5:aangekomen" -> {
                        Z5Verhaal.vlieg(p);
                        helper.assertTrue(Z5Verhaal.speelt(p, "aankomst"), "the first flight plays the arrival film");
                        Z5Verhaal.spoel(p);
                    }
                    case "z5:herinnering", "z5:ontmaskerd", "z5:wolkwal_wakker", "z5:cliffhanger" -> {
                        Praat.vergeet(p);
                        if (!Z5Verhaal.bezig(p)) {
                            BlockPos b = switch (d.doel()) {
                                case "z5:wolkwal_wakker" -> Z5VerhaalPlekken.BALKON_OOG;
                                case "z5:cliffhanger" -> Z5VerhaalPlekken.poi("z5_pier_oost");
                                default -> Z5VerhaalPlekken.LAAN;
                            };
                            op(helper, p, b);
                        }
                        helper.assertTrue(Questlog.doelKlaar(p, q, q.doelen().indexOf(d)) || Z5Verhaal.bezig(p), d.doel() + ": its scene plays");
                        Z5Verhaal.spoel(p);
                    }
                    default -> NpcApi.meld(p, s[0], s[1]);
                }
            }
        }
    }

    /** Stands the player at b for one check of the zone-5 hooks, then back (no far chunks for the test level). */
    private static void op(GameTestHelper helper, ServerPlayer p, BlockPos b) {
        Vec3 was = p.position();
        p.setPos(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        Z5Haken.stap(p, helper.getLevel());
        p.setPos(was.x, was.y, was.z);
    }

    private static RijkGuh guh(GameTestHelper helper, String id, int x) {
        RijkGuh g = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(1 + x % 3, 1, 1 + (x / 3) % 3));
        g.stelIn(Npcs.van(id));
        return g;
    }
}
