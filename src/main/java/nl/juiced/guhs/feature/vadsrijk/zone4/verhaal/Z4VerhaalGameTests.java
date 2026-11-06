package nl.juiced.guhs.feature.vadsrijk.zone4.verhaal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcHaken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.Reiskaart;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone 4, slice t-z4verhaal (few, focused): (1) every zone-4 quest step names WHO and WHERE (a target + a where text), every NPC
 * has a spot in zone 4, a name and a where text; (2) the whole main line "De Grote Knuffeltij" walked through the real hooks (the
 * sailboat click, the 12 knuffels, the mist flash, the wreck, the party + the whale, the hand-in clicks) up to the cliffhanger with
 * {@code zone4_klaar} + the zone-5 airship flag; (3) the small games (volleyball, kite contest) and the travel-map place.
 */
public class Z4VerhaalGameTests {
    private static final String B = "vadsrijk_z4_verhaal";

    private static List<Quest> z4() {
        List<Quest> l = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (q.id().startsWith("z4_")) {
                l.add(q);
            }
        }
        return l;
    }

    @GuhTest(template = "empty", batch = B)
    public static void elkeStapWieEnWaar(GameTestHelper helper) {
        Language t = Language.getInstance();
        List<Quest> z4 = z4();
        long hoofd = z4.stream().filter(Quest::hoofd).count(), zij = z4.stream().filter(q -> q.soort() == Quest.Soort.ZIJ).count();
        helper.assertTrue(hoofd == 7 && zij >= 15, "zone 4: 7 main and ~15 side quests: " + hoofd + "/" + zij);
        helper.assertTrue(Quests.van("z3_hq15_hoogvads") != null && Quests.van("hq01_welkom") != null, "zones 1-3 still load");
        for (String id : Z4VerhaalPlekken.NPC.keySet()) {
            NpcDef d = Npcs.van(id);
            helper.assertTrue(d != null && d.plek() != null, "NPC " + id + " is registered with a spot");
            helper.assertTrue(t.has(d.naamKey()) && t.has("vadsrijk.npc.plek.npc." + id) && t.has("vadsrijk.npc.praat." + id + ".3"),
                    "NPC " + id + ": name + where text + lines");
            helper.assertTrue(Z4VerhaalPlekken.zone4(d.plek().getX(), d.plek().getZ()), "NPC " + id + " stands in zone 4");
        }
        for (Quest q : z4) {
            for (String wie : List.of(q.gever(), q.inleveren())) {
                helper.assertTrue(Npcs.van(wie) != null && t.has("vadsrijk.npc.plek.npc." + wie), q.id() + ": who (" + wie + ") and where");
            }
            helper.assertTrue(t.has(q.titelKey()) && t.has(q.tekstKey()) && t.has(q.sleutel() + ".bezig"), q.id() + ": texts");
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                helper.assertTrue(t.has(q.doelKey(i)), q.id() + " step " + i + ": what");
                helper.assertTrue(!d.plek().isEmpty() && QuestPlek.pos(helper.getLevel(), d.plek()) != null, q.id() + " step " + i + ": a target (" + d.plek() + ")");
                helper.assertTrue(t.has(QuestPlek.tekstKey(d.plek())), q.id() + " step " + i + ": where text " + QuestPlek.tekstKey(d.plek()));
                if (d.type() == Quest.Doel.Type.VERZAMEL) {
                    helper.assertTrue(Questlog.item(d.doel()) != net.minecraft.world.item.Items.AIR, q.id() + ": item " + d.doel());
                }
            }
        }
        for (String id : List.of("strand_1", "strand_6", "groot_1", "groot_4", "wrak_2")) {
            helper.assertTrue(t.has(Z4Verhaal.Z + "tijknuffel." + id + ".naam"), "knuffel " + id + " has a name + story");
        }
        helper.assertTrue(Z4VerhaalPlekken.TIJKNUFFEL.size() + Z4VerhaalPlekken.GROTE_KNUFFEL.size() + Z4VerhaalPlekken.WRAKKNUFFEL.size()
                == Z4VerhaalPlekken.KNUFFELTIJ_TOTAAL, "the Knuffeltij is a fixed 12");
        helper.assertTrue(Reiskaart.PLEKKEN.contains("lampionkust") && t.has("vadsrijk.npc.reiskaart.plek.lampionkust"), "travel map: lampionkust");
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.zone("zone4") != null, "Verkenningsboek page zone 4");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 600)
    public static void hoofdlijnViaApi(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        Z4Haken.vergeet(p);
        Z4VerhaalRegistry.gebieden();
        Z4VerhaalRegistry.klaarzetten(p);
        Spelers.zetLevel(p, 56);   // (capped by Levels.MAX; the quests ask at most that)
        Spelers.data(p).gezien.removeIf(s -> s.startsWith("npc:z4_"));
        List<Quest> hoofd = z4().stream().filter(Quest::hoofd).toList();
        String vorige = "z3_hq15_hoogvads";
        int x = 0;
        for (Quest q : hoofd) {
            helper.assertTrue(q.na().contains(vorige), q.id() + " follows " + vorige);
            vorige = q.id();
            if (Questlog.status(p, q.id()) == 0) {
                helper.assertTrue(Questlog.beschikbaar(p, q), q.id() + " is available");
                Questlog.neemAan(p, q);
            }
            Z4Verhaal.vergeet(p);   // (the first flight's opening scene: not needed here)
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (Questlog.doelKlaar(p, q, i) || d.type() == Quest.Doel.Type.PRAAT && d.doel().equals(q.inleveren())) {
                    continue;
                }
                doe(helper, p, q, d, x++);
                helper.assertTrue(Questlog.doelKlaar(p, q, i), q.id() + " step " + i + " (" + d.type() + " " + d.doel() + ") counted");
            }
            RijkGuh inl = guh(helper, q.inleveren(), x++);
            QuestMotor.klik(p, inl);
            helper.assertTrue(Questlog.klaar(p, q.id()), q.id() + " handed in at " + q.inleveren());
            QuestMotor.afronden(p);   // (the dialogue closes: the quest action runs: the next quest, the cliffhanger)
            inl.discard();
        }
        helper.assertTrue(Z4VerhaalApi.zone4Klaar(p), "the cliffhanger set zone4_klaar");
        helper.assertTrue(Z4VerhaalApi.luchtschipNaarHoogvads(p), "the zone-5 airship line is open");
        helper.assertTrue(Z4VerhaalApi.knuffeltij(p) == 12, "all 12 Knuffeltij knuffels cheered up: " + Z4VerhaalApi.knuffeltij(p));
        helper.assertTrue(Z4VerhaalApi.heeftZeilboot(p), "the sailboat was handed out");
        Z4Verhaal.vergeet(p);
        Z4Haken.vergeet(p);
        Questlog.wisAlles(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void spelletjesEnReiskaart(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        Z4Haken.vergeet(p);
        // the travel map: a teaser before zone 3 is done, a real place after
        helper.assertTrue(Reiskaart.plek(p, "lampionkust").staat() == Reiskaart.TEASER, "lampionkust is a teaser before zone 3");
        Z4VerhaalRegistry.klaarzetten(p);
        helper.assertTrue(Reiskaart.plek(p, "lampionkust").staat() == Reiskaart.OPEN, "lampionkust opens after zone 3 at lv 50");
        helper.assertTrue(QuestMotor.reisDoelen(p, "z3_kapitein").contains("lampionkust"), "Kapitein Zoemzeil flies to the coast");
        // beach volleyball: three right guesses win (a wrong one first: no harm)
        Questlog.neemAan(p, Quests.van(Z4Haken.HQ01));
        Questlog.voltooi(p, Quests.van(Z4Haken.HQ01));
        Questlog.voltooi(p, Quests.van(Z4Haken.HQ02));
        Quest volley = Quests.van(Z4Haken.S06);
        Questlog.neemAan(p, volley);
        RijkGuh smash = guh(helper, "z4_volleybal", 0);
        QuestMotor.klik(p, smash);
        int kant = Z4Haken.volleyKant(p);
        helper.assertTrue(kant >= 0, "Smash serves");
        Z4Haken.volleyAntwoord(p, smash, (kant + 1) % 3);
        for (int i = 0; i < 3; i++) {
            Z4Haken.volleyAntwoord(p, smash, Z4Haken.volleyKant(p));
        }
        helper.assertTrue(Questlog.allesKlaar(p, volley), "three points win the volleyball game");
        smash.discard();
        // the kite contest: 30 s at the pole with your kite
        Quest wed = Quests.van(Z4Haken.S02);
        Questlog.voltooi(p, Quests.van("z4_s01_vlieger"));
        Questlog.neemAan(p, wed);
        Questlog.geef(p, new ItemStack(Z4VerhaalRegistry.item("vlieger")));
        BlockPos paal = Z4VerhaalPlekken.poi("z4_wedstrijdpaal");
        Vec3 was = p.position();
        p.setPos(paal.getX() + 0.5, paal.getY(), paal.getZ() + 0.5);
        for (int i = 0; i < 61 && !Questlog.allesKlaar(p, wed); i++) {
            Z4Haken.spelletjes(p, helper.getLevel());
        }
        p.setPos(was.x, was.y, was.z);
        helper.assertTrue(Questlog.allesKlaar(p, wed), "30 s in the wind wins the kite contest");
        // fishing counts in zone 4 only (here: by the event API)
        Quest vis = Quests.van(Z4Haken.S03);
        Questlog.neemAan(p, vis);
        for (int i = 0; i < 5; i++) {
            NpcApi.meld(p, "z4_vis", "gevangen");
        }
        helper.assertTrue(Questlog.allesKlaar(p, vis), "5 fish for Visser Dobber");
        Z4Haken.vergeet(p);
        Questlog.wisAlles(p);
        helper.succeed();
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
            case GEBIED -> op(helper, p, d.doel().substring(4));
            case GEHEIM -> {
                java.util.Map<String, BlockPos> m = d.doel().equals("teller:" + Z4Haken.TIJ) ? Z4VerhaalPlekken.TIJKNUFFEL
                        : d.doel().equals("teller:" + Z4Haken.WRAK) ? Z4VerhaalPlekken.WRAKKNUFFEL : null;
                helper.assertTrue(m != null, q.id() + ": no test path for " + d.doel());
                String soort = d.doel().substring(7);
                m.forEach((id, b) -> Z4Haken.vind(p, helper.getLevel(), soort, id, b));
            }
            case OPVROLIJK, ELITE, BAAS -> Questlog.tel(p, d.type(), y -> NpcHaken.pastMob(y.doel(), "guhs:vr_z4_proef", ""), d.n());
            case VERZAMEL -> Questlog.geef(p, new ItemStack(Questlog.item(d.doel()), d.n()));
            case MELD -> meld(helper, p, q, d, x);
            default -> helper.fail(q.id() + ": no test path for " + d.type());
        }
    }

    private static void meld(GameTestHelper helper, ServerPlayer p, Quest q, Quest.Doel d, int x) {
        switch (d.doel()) {
            case "z4:zeilboot" -> {   // the real path: a click on Botenguh Zeiltje
                RijkGuh g = guh(helper, "z4_zeilverhuur", x);
                QuestMotor.klik(p, g);
                g.discard();
            }
            case "z4:mistflash" -> {
                op(helper, p, "z4_mysterie_poort");
                helper.assertTrue(Z4Verhaal.speelt(p, "mist"), "the mist flash plays at the grey gate");
                Z4Verhaal.spoel(p);
            }
            case "z4_vliegervanger:verslagen" -> op(helper, p, "z4_wrak_ruim");   // (no boss slice here: Kootje's scene)
            case "z4:feest", "z4:wolkwal" -> {
                Z4Verhaal.spoel(p);
                if (!Questlog.doelKlaar(p, q, q.doelen().indexOf(d))) {
                    op(helper, p, "z4_feestplek");
                    helper.assertTrue(Z4Verhaal.bezig(p), d.doel() + ": its scene plays");
                    Z4Verhaal.spoel(p);   // (the party's end starts the whale; spoel runs both)
                }
            }
            default -> {
                String[] s = d.doel().split(":", 2);
                for (int i = 0; i < d.n(); i++) {
                    NpcApi.meld(p, s[0], s[1]);
                }
            }
        }
    }

    /** Stand at a zone-4 POI for one tick of the zone-4 hooks, then back (the test level has no far chunks). */
    private static void op(GameTestHelper helper, ServerPlayer p, String poi) {
        BlockPos b = Z4VerhaalPlekken.poi(poi);
        Vec3 was = p.position();
        p.setPos(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        Z4Haken.stap(p, helper.getLevel(), 100000L + helper.getTick() * 1000L);
        p.setPos(was.x, was.y, was.z);
    }

    private static RijkGuh guh(GameTestHelper helper, String id, int x) {
        RijkGuh g = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(1 + x % 3, 1, 1 + (x / 3) % 3));
        g.stelIn(Npcs.van(id));
        return g;
    }
}
