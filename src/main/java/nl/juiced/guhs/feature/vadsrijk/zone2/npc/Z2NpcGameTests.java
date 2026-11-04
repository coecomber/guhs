package nl.juiced.guhs.feature.vadsrijk.zone2.npc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
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
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone 2, slice npc (few, focused): (1) every zone-2 quest step names WHO and WHERE (an explicit target the tracker can point at,
 * with a "where" text) and every NPC has a spot, name and "where" text; (2) the whole zone-2 main chain walked through the shared
 * quest API (accept, each objective through its real hook, hand-in by clicking the NPC guh) up to {@code zone2_klaar}; (3) the
 * zone-2 secrets (knuffels under the ice, the boormeester clues) and the haven pilot's line to Kandijkom.
 */
public class Z2NpcGameTests {
    private static final String B = "vadsrijk_z2_npc";

    private static List<Quest> z2() {
        List<Quest> l = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (q.id().startsWith("z2_")) {
                l.add(q);
            }
        }
        return l;
    }

    @GuhTest(template = "empty", batch = B)
    public static void elkeStapWieEnWaar(GameTestHelper helper) {
        Language t = Language.getInstance();
        List<Quest> z2 = z2();
        long hoofd = z2.stream().filter(Quest::hoofd).count(), zij = z2.stream().filter(q -> q.soort() == Quest.Soort.ZIJ).count();
        helper.assertTrue(hoofd >= 12 && zij >= 5, "zone 2: ~12 main and ~5 side quests: " + hoofd + "/" + zij);
        helper.assertTrue(Quests.van("hq01_welkom") != null, "zone 1 still loads next to zone 2");
        for (String id : Z2NpcPlekken.NPC.keySet()) {
            NpcDef d = Npcs.van(id);
            helper.assertTrue(d != null && d.plek() != null, "NPC " + id + " is registered with a spot");
            helper.assertTrue(t.has(d.naamKey()) && t.has("vadsrijk.npc.plek.npc." + id), "NPC " + id + ": name + where text");
            helper.assertTrue(!d.kleren().isEmpty(), "NPC " + id + " wears something warm");
        }
        for (Quest q : z2) {
            for (String wie : List.of(q.gever(), q.inleveren())) {
                // (zone-1 NPCs such as the haven pilot get their "where" text from zone 1's generator: the fix-round tracker)
                helper.assertTrue(Npcs.van(wie) != null && (t.has("vadsrijk.npc.plek.npc." + wie) || !wie.startsWith("z2_")),
                        q.id() + ": who (" + wie + ") and where");
            }
            helper.assertTrue(t.has(q.titelKey()) && t.has(q.tekstKey()) && t.has(q.sleutel() + ".bezig"), q.id() + ": texts");
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                String wat = q.doelKey(i);
                helper.assertTrue(t.has(wat), q.id() + " step " + i + ": what");
                helper.assertTrue(!d.plek().isEmpty() && QuestPlek.pos(helper.getLevel(), d.plek()) != null, q.id() + " step " + i + ": a target (" + d.plek() + ")");
                // (a zone-1 NPC such as the haven pilot: its "where" text comes from zone 1's generator, the fix-round tracker)
                boolean zone1Npc = d.plek().startsWith("npc:") && !d.plek().startsWith("npc:z2_");
                helper.assertTrue(zone1Npc || t.has(QuestPlek.tekstKey(d.plek())), q.id() + " step " + i + ": where text " + QuestPlek.tekstKey(d.plek()));
                if (d.type() == Quest.Doel.Type.VERZAMEL) {
                    helper.assertTrue(Questlog.item(d.doel()) != net.minecraft.world.item.Items.AIR, q.id() + ": item " + d.doel());
                }
                if (d.doel().startsWith("poi:")) {
                    helper.assertTrue(Z2NpcPlekken.poi(d.doel().substring(4)) != null, q.id() + ": poi " + d.doel());
                }
            }
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 400)
    public static void hoofdlijnViaApi(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        Z2NpcRegistry.gebieden();
        // the gate: the haven pilot shows the Kandijkom line only after zone 1
        QuestMotor.ExtraOptie lijn = QuestMotor.EXTRA.get("luchtschipper").get(0);
        helper.assertTrue(!lijn.zichtbaar().test(p), "no Kandijkom line before zone 1");
        Questlog.zetVlag(p, NpcApi.ZONE1_VLAG, true);
        helper.assertTrue(lijn.zichtbaar().test(p), "the Kandijkom line after zone 1");
        Questlog.zetVlag(p, NpcApi.ZONE1_VLAG, false);
        // the chain, in story order
        List<Quest> hoofd = z2().stream().filter(Quest::hoofd).toList();
        String vorige = "hq12_schaduw";
        int x = 0;
        for (Quest q : hoofd) {
            helper.assertTrue(q.na().contains(vorige), q.id() + " follows " + vorige);
            vorige = q.id();
            if (Questlog.status(p, q.id()) == 0) {
                Questlog.neemAan(p, q);
            }
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (d.type() == Quest.Doel.Type.PRAAT && d.doel().equals(q.inleveren())) {
                    continue;   // (talking to the hand-in NPC happens with the hand-in click)
                }
                doe(helper, p, q, d, x++);
                helper.assertTrue(Questlog.doelKlaar(p, q, i), q.id() + " step " + i + " (" + d.type() + " " + d.doel() + ") counted");
            }
            RijkGuh inl = guh(helper, q.inleveren(), x++);
            QuestMotor.klik(p, inl);
            helper.assertTrue(Questlog.klaar(p, q.id()), q.id() + " handed in at " + q.inleveren());
            QuestMotor.afronden(p);   // (the dialogue closes: the quest action runs: flight, flashback, cliffhanger)
            inl.discard();
        }
        helper.assertTrue(Z2NpcApi.zone2Klaar(p), "the cliffhanger set zone2_klaar");
        Z2Verhaal.vergeet(p);
        Questlog.wisAlles(p);
        helper.succeed();
    }

    /** Completes one objective through the hook it really uses in game. */
    private static void doe(GameTestHelper helper, ServerPlayer p, Quest q, Quest.Doel d, int x) {
        String gebied = d.doel().startsWith("gebied:") ? d.doel().substring(7) : "";
        switch (d.type()) {
            case PRAAT -> {
                RijkGuh g = guh(helper, d.doel(), x);
                QuestMotor.klik(p, g);
                QuestMotor.afronden(p);
                g.discard();
            }
            case GEBIED -> {
                if (d.doel().startsWith("poi:")) {
                    BlockPos b = Z2NpcPlekken.poi(d.doel().substring(4));
                    net.minecraft.world.phys.Vec3 was = p.position();
                    p.setPos(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
                    Z2Haken.hogePlekken(p);
                    p.setPos(was.x, was.y, was.z);   // (back before the next tick: no far chunks for the test level)
                } else {
                    String g = d.doel().split("\\|")[0];   // ("z2_kandijkom|z2_kandijkom_dok": either area counts)
                    Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, g));
                }
            }
            case KAMPVUUR -> {
                BlockPos b = Z2NpcPlekken.KAMPVUUR.get(d.doel());
                Haken.KAMPVUUR.forEach(h -> h.accept(p, b));
            }
            case OPVROLIJK, ELITE, BAAS -> Questlog.tel(p, d.type(), y -> NpcHaken.pastMob(y.doel(), "guhs:vr_z2_proef", gebied), d.n());
            case VERZAMEL -> Questlog.geef(p, new ItemStack(Questlog.item(d.doel()), d.n()));
            case MELD -> {
                String[] s = d.doel().split(":", 2);
                NpcApi.meld(p, s[0], s[1]);
            }
            default -> helper.fail(q.id() + ": no test path for " + d.type());
        }
    }

    private static RijkGuh guh(GameTestHelper helper, String id, int x) {
        RijkGuh g = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(1 + x % 3, 1, 1 + (x / 3) % 3));
        g.stelIn(Npcs.van(id));
        return g;
    }

    @GuhTest(template = "empty", batch = B)
    public static void geheimenEnBeelden(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        // the knuffels under the ice count for Kleine Vlokje's quest (not for zone 1's Kleine Kiki)
        Quest kn = Quests.van("z2_s03_knuffels"), kiki = Quests.van("z03_knuffels");
        Questlog.neemAan(p, kn);
        if (kiki != null) {
            Questlog.neemAan(p, kiki);
        }
        for (var e : Z2NpcPlekken.KNUFFEL.entrySet()) {
            Z2Haken.vind(p, helper.getLevel(), Z2Haken.KNUFFELS, e.getKey(), e.getValue());
        }
        helper.assertTrue(Questlog.allesKlaar(p, kn), "10 knuffels under the ice: Vlokje's quest is done");
        helper.assertTrue(kiki == null || !Questlog.allesKlaar(p, kiki), "zone 1's knuffel quest does not count them");
        // the clues: the first starts the secret quest, five unlock the boormeester, his defeat completes it
        Quest bm = Quests.van("z2_geheim_boormeester");
        for (var e : Z2NpcPlekken.AANWIJZING.entrySet()) {
            Z2Haken.vind(p, helper.getLevel(), Z2Haken.AANWIJZINGEN, e.getKey(), e.getValue());
        }
        helper.assertTrue(Questlog.actief(p, bm.id()) && Questlog.doelKlaar(p, bm, 0) && Z2NpcApi.boormeesterMag(p), "5 clues: the secret quest runs");
        NpcApi.meld(p, "z2_boormeester", "verslagen");
        helper.assertTrue(Questlog.allesKlaar(p, bm), "the boormeester beaten: back to Uitvinder Tandwiel");
        // a petrified guh: not on the quest = it whispers; on the quest = freed with a click; afterwards it thanks you
        RijkGuh beeld = guh(helper, "z2_versteend_ijsje", 0);
        helper.assertTrue(!Z2Haken.bevrijd(p, "z2_versteend_ijsje"), "still crystal");
        Quest v = Quests.van("z2_hq06_versteend");
        Questlog.neemAan(p, v);
        QuestMotor.klik(p, beeld);
        QuestMotor.afronden(p);
        helper.assertTrue(Z2Haken.bevrijd(p, "z2_versteend_ijsje") && Z2NpcApi.nogVersteend(p, "z2_versteend_zwier"), "IJsje freed, Zwier not yet");
        Z2Haken.vergeet(p);
        Questlog.wisAlles(p);
        helper.succeed();
    }
}
