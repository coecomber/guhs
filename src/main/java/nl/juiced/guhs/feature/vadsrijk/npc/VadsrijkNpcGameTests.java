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
        Haken.KAMPVUUR.forEach(h -> h.accept(p, nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.kamp(5, 65, 1020)));
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

    /**
     * Fix round 1: walk the whole main story through the API; at every step (before taking a quest, each open objective, the
     * hand-in) the tracker's step must say WHAT and WHERE in real text (every key translated), point at a spot, and name the
     * NPC for talk/hand-in steps. Also: the level gate says "level first, then talk to ...", the auto-started quests (hq05, hq10,
     * hq12) start, and the Mika pin can never be lost (it is hq12's start item, not a collect objective).
     */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void hoofdlijnHeeftAltijdWieEnWaar(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        nl.juiced.guhs.feature.vadsrijk.kern.Spelers.zetLevel(p, 1);
        java.util.List<Quest> hoofd = Quests.alle().values().stream().filter(Quest::hoofd).filter(q -> nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken.zoneVan(q.id()) == 1).toList();   // (zone 1; zones 2 and 3 have their own tests; K-INT)
        helper.assertTrue(hoofd.size() == 13, "13 main quests (12 + the element quiz): " + hoofd.size());
        int stappen = 0;
        for (Quest q : hoofd) {
            QuestStap s = QuestStap.hoofdlijn(p);
            helper.assertTrue(s != null && s.quest().equals(q.id()), "the tracker is at " + q.id() + ": " + (s == null ? null : s.quest()));
            if (!Questlog.actief(p, q.id())) {
                if (nl.juiced.guhs.feature.vadsrijk.kern.Spelers.level(p) < q.lv()) {
                    check(helper, s, q.id() + " level gate", true);
                    helper.assertTrue(sleutels(s.wat()).contains(QuestStap.P + "level") && sleutels(s.waar()).contains(Npcs.van(q.gever()).naamKey()),
                            q.id() + ": the level gate names the level and then the giver");
                    nl.juiced.guhs.feature.vadsrijk.kern.Spelers.zetLevel(p, q.lv());
                    s = QuestStap.hoofdlijn(p);
                }
                check(helper, s, q.id() + " offer", true);
                helper.assertTrue(sleutels(s.wat()).contains(Npcs.van(q.gever()).naamKey()), q.id() + ": go to the giver, by name");
                helper.assertTrue(q.auto() || NpcMarkers.marker(p, q.gever()) == NpcMarkers.HOOFD, q.id() + ": the giver shows the gold !");
                Questlog.neemAan(p, q);
                stappen++;
            }
            for (int i = 0; i < q.doelen().size(); i++) {
                if (Questlog.doelKlaar(p, q, i)) {
                    continue;
                }
                QuestStap d = QuestStap.hoofdlijn(p);
                Quest.Doel doel = q.doelen().get(i);
                check(helper, d, q.id() + " objective " + i + " (" + doel.type() + ")", doel.type() != Quest.Doel.Type.LEVEL);
                if (doel.type() == Quest.Doel.Type.PRAAT) {
                    helper.assertTrue(sleutels(d.waar()).contains(QuestStap.PLEK + "npc." + doel.doel()), q.id() + ": where " + doel.doel() + " is");
                }
                if (doel.type() == Quest.Doel.Type.VERZAMEL) {
                    Questlog.geef(p, new ItemStack(Questlog.item(doel.doel()), doel.n()));
                } else {
                    int[] v = Questlog.v(p, q);
                    v[i] = doel.n();
                    Questlog.zetV(p, q, v);
                }
                stappen++;
            }
            QuestStap in = QuestStap.hoofdlijn(p);
            check(helper, in, q.id() + " hand-in", true);
            helper.assertTrue(sleutels(in.wat()).contains(Npcs.van(q.inleveren()).naamKey()), q.id() + ": hand in to " + q.inleveren() + ", by name");
            helper.assertTrue(NpcMarkers.marker(p, q.inleveren()) == NpcMarkers.INLEVEREN, q.id() + ": a ? over " + q.inleveren());
            Questlog.voltooi(p, q);
            if (q.actieKlaar().startsWith("start:")) {
                QuestMotor.doeActie(p, q.actieKlaar(), null);
            } else if (q.id().equals("hq04_kampvuur")) {
                Questlog.neemAan(p, Quests.van("hq05_klodderreus"));   // (what the BAM does)
            }
            stappen++;
        }
        QuestStap na = QuestStap.hoofdlijn(p);   // (integration: after zone 1 the main line goes on in zone 2)
        helper.assertTrue((na == null || na.quest().startsWith("z2_")) && Questlog.klaar(p, "hq12_schaduw"), "the zone-1 main story is done");
        helper.assertTrue(QuestStap.hoofdGedaan(p).size() == 13, "13 steps of history");
        helper.assertTrue(stappen > 30, "walked " + stappen + " steps");
        Questlog.wisAlles(p);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, QuestStap s, String waar, boolean plek) {
        helper.assertTrue(s != null, waar + ": a step");
        for (net.minecraft.network.chat.Component c : new net.minecraft.network.chat.Component[] {s.wat(), s.waar()}) {
            helper.assertTrue(!c.getString().isBlank(), waar + ": empty text");
            for (String k : sleutels(c)) {
                helper.assertTrue(net.minecraft.locale.Language.getInstance().has(k), waar + ": missing text " + k);
            }
        }
        helper.assertTrue(!plek || s.doel() != null, waar + ": the tracker has a spot to point at");
    }

    /** Every translation key in a component (also in its arguments). */
    private static java.util.Set<String> sleutels(net.minecraft.network.chat.Component c) {
        java.util.Set<String> l = new java.util.HashSet<>();
        if (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
            l.add(t.getKey());
            for (Object a : t.getArgs()) {
                if (a instanceof net.minecraft.network.chat.Component x) {
                    l.addAll(sleutels(x));
                }
            }
        }
        for (net.minecraft.network.chat.Component x : c.getSiblings()) {
            l.addAll(sleutels(x));
        }
        return l;
    }

    // ---- fix round 2 (g-ux) ----------------------------------------------------------------------------------------------------

    /** The airship NPCs: the travel map (open/here/unknown/teaser), real dialogue choices, flying from both. */
    @GuhTest(template = "empty", batch = B)
    public static void reisKeuzes(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        nl.juiced.guhs.feature.vadsrijk.kern.Spelers.zetLevel(p, 5);
        java.util.List<String> gevlogen = new java.util.ArrayList<>();
        QuestMotor.VLIEG_TEST = (x, w) -> gevlogen.add(w);
        try {
            RijkGuh kap = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(1, 1, 1));
            kap.stelIn(Npcs.van("kapitein"));
            // before the BAM: a click opens the travel map; Zwamzoet and Kandijkom are not discovered, zone 3/4 are teasers
            QuestMotor.klik(p, kap);
            helper.assertTrue(Reiskaart.openSinds(p.getUUID()) != null, "the captain opens the travel map");
            helper.assertTrue(Reiskaart.plek(p, "knuffelkamp").staat() != Reiskaart.ONBEKEND, "the camp is always open");
            helper.assertTrue(Reiskaart.plek(p, "zwamzoet").staat() == Reiskaart.ONBEKEND && Reiskaart.plek(p, "kandijkom").staat() == Reiskaart.ONBEKEND,
                    "Zwamzoet and Kandijkom not discovered yet");
            helper.assertTrue(Reiskaart.plek(p, "schemerwortel").staat() == Reiskaart.TEASER && Reiskaart.plek(p, "lampionkust").staat() == Reiskaart.TEASER,
                    "zone 3 and 4 are teasers");
            Reiskaart.kiesVoorTest(p, "zwamzoet");
            helper.assertTrue(gevlogen.isEmpty(), "an undiscovered place does not fly");
            // the playtest case: hq06 runs ("say when you want to leave") -> the dialogue has the map + the real choice now
            Questlog.neemAan(p, Quests.van("hq06_zwamzoet"));
            QuestMotor.klik(p, kap);
            helper.assertTrue(QuestMotor.reisOpen(p).equals(java.util.List.of("zwamzoet")), "hq06 running: the choice is there: " + QuestMotor.reisOpen(p));
            QuestMotor.kiesVoorTest(p, QuestMotor.REIS_OPTIE);
            helper.assertTrue(gevlogen.equals(java.util.List.of("kapitein>zwamzoet")), "choosing it flies: " + gevlogen);
            // ... and through the map
            QuestMotor.klik(p, kap);
            QuestMotor.kiesVoorTest(p, QuestMotor.REIS_KAART);
            helper.assertTrue(Reiskaart.openSinds(p.getUUID()) != null, "the dialogue's map choice opens the map");
            Reiskaart.kiesVoorTest(p, "zwamzoet");
            helper.assertTrue(gevlogen.get(gevlogen.size() - 1).equals("kaart>zwamzoet"), "the map flies too: " + gevlogen);
            Reiskaart.kiesVoorTest(p, "knuffelkamp");
            helper.assertTrue(gevlogen.size() == 2, "a map choice only counts right after opening it at an airship NPC");
            // the haven pilot: Kandijkom after zone 1 and lv 20 (or once the zone-2 story started)
            RijkGuh pil = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(2, 1, 1));
            pil.stelIn(Npcs.van("luchtschipper"));
            Questlog.zetVlag(p, NpcApi.ZONE1_VLAG, true);
            QuestMotor.klik(p, pil);
            helper.assertTrue(Reiskaart.plek(p, "kandijkom").staat() == Reiskaart.ONBEKEND
                    && "vadsrijk.npc.reis.slot.kandijkom_level".equals(QuestMotor.reisSlot(p, "kandijkom")), "Kandijkom locked below lv 20");
            Reiskaart.kiesVoorTest(p, "kandijkom");
            helper.assertTrue(gevlogen.size() == 2, "locked Kandijkom does not fly");
            nl.juiced.guhs.feature.vadsrijk.kern.Spelers.zetLevel(p, 20);
            QuestMotor.klik(p, pil);
            Reiskaart.kiesVoorTest(p, "kandijkom");
            helper.assertTrue(gevlogen.get(gevlogen.size() - 1).equals("kaart>kandijkom"), "lv 20: Kandijkom flies: " + gevlogen + " (lv " + nl.juiced.guhs.feature.vadsrijk.kern.Spelers.level(p) + ", slot "
                    + QuestMotor.reisSlot(p, "kandijkom") + ", hier " + Reiskaart.hier(p) + ", kaart open " + Reiskaart.openSinds(p.getUUID()) + ")");
            // the Kandijkom captain: back to De Haven or straight to the camp (dialogue choices exist for him too)
            helper.assertTrue(QuestMotor.reisDoelen(p, "z2_kapitein").equals(java.util.List.of("zwamzoet", "knuffelkamp")), "Kandijkom captain's lines");
            RijkGuh pool = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(3, 1, 1));
            pool.stelIn(Npcs.van("z2_kapitein"));
            QuestMotor.klik(p, pool);
            helper.assertTrue(Reiskaart.openSinds(p.getUUID()) != null, "the Kandijkom captain opens the map too");
        } finally {
            QuestMotor.VLIEG_TEST = null;
            QuestMotor.vergeet(p);
            Reiskaart.vergeet(p.getUUID());
        }
        helper.succeed();
    }

    /** The job advancement shows in the tracker (and questlog) once it may be started, pointing at the Grootmeester. */
    @GuhTest(template = "empty", batch = B)
    public static void klasseInTracker(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        var d = nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p);
        String oud = d.klasse;
        d.klasse = "knuffelridder";
        nl.juiced.guhs.feature.vadsrijk.kern.Spelers.zetLevel(p, 5);
        helper.assertTrue(NpcVolg.klasseQuest(p) == null, "lv 5, story not there: no advancement line");
        Quest q = Quests.van("adv10_knuffelridder");
        for (String na : q.na()) {
            Questlog.neemAan(p, Quests.van(na));
            Questlog.voltooi(p, Quests.van(na));
        }
        nl.juiced.guhs.feature.vadsrijk.kern.Spelers.zetLevel(p, q.lv());
        helper.assertTrue(NpcVolg.klasseQuest(p) == q, "requirements met: the advancement quest is offered: " + NpcVolg.klasseQuest(p));
        NpcPayloads.Volg v = NpcVolg.bouw(p);
        helper.assertTrue(v.regels().stream().anyMatch(r -> r.quest().equals(q.id()) && r.doel().isPresent()), "it is in the tracker with a beam target");
        helper.assertTrue(NpcLog.bouw(p).get("Klasse") instanceof net.minecraft.nbt.CompoundTag, "and in the questlog");
        Questlog.neemAan(p, q);
        helper.assertTrue(NpcVolg.bouw(p).regels().stream().anyMatch(r -> r.quest().equals(q.id())), "still tracked while active");
        Questlog.voltooi(p, q);
        helper.assertTrue(NpcVolg.klasseQuest(p) == null || !NpcVolg.klasseQuest(p).id().equals(q.id()), "gone once handed in");
        d.klasse = "pluisjager";
        helper.assertTrue(NpcVolg.klasseQuest(p) == null || !"knuffelridder".equals(NpcVolg.klasseQuest(p).klasse()), "only your own class");
        d.klasse = oud;
        helper.succeed();
    }
}
