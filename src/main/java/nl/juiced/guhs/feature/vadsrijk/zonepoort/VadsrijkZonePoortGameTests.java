package nl.juiced.guhs.feature.vadsrijk.zonepoort;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeon;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Lijnen;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Poorten;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.Reiskaart;
import nl.juiced.guhs.feature.vadsrijk.questrobot.QuestRobot;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * X-ZONE1: the zone gate. Boarding/travel/quests/dungeons of a closed zone say "binnenkort", the zone-1 story ends on "Wordt
 * vervolgd", the safety net puts you back, the soft cap banks xp, raising the gate opens everything, ops/creative bypass; and the
 * quest robot plays zone 1 completely with only zone 1 open. Run: {@code runGameTestServer -Pgt=VadsrijkZonePoort}.
 */
public class VadsrijkZonePoortGameTests {
    private static final String BATCH = "vadsrijk_zonepoort";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Questlog.wisAlles(p);
        return p;
    }

    /** The server gate at {@code open}, strict (the mock player is creative); the per-player book % off unless a test sets it. */
    private static void poort(int open) {
        ZonePoortConfig.TEST = open;
        ZonePoortConfig.TEST_BOEK = 0;
        ZonePoort.TEST_STRENG = true;
    }

    private static void herstel() {
        ZonePoortConfig.TEST = null;
        ZonePoortConfig.TEST_BOEK = null;
        ZonePoort.TEST_STRENG = false;
    }

    /** Does this component (or any part/argument of it) use this lang key? */
    static boolean heeft(Component c, String key) {
        if (c.getContents() instanceof TranslatableContents t) {
            if (t.getKey().equals(key)) {
                return true;
            }
            for (Object a : t.getArgs()) {
                if (a instanceof Component ac && heeft(ac, key)) {
                    return true;
                }
            }
        }
        for (Component s : c.getSiblings()) {
            if (heeft(s, key)) {
                return true;
            }
        }
        return false;
    }

    /** Ship, travel NPC, travel map: zone 2 says "binnenkort"; zone 1 and the way back stay open; raising the gate opens it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void schipEnReisDicht(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            poort(1);
            Spelers.zetLevel(p, 20);
            Questlog.zetVlag(p, NpcApi.ZONE1_VLAG, true);
            Lijnen.Lijn a = Lijnen.vanId("z1_z2");
            String slot = Poorten.dicht(p, a, true);
            helper.assertTrue("vadsrijk.zonepoort.lijn.2".equals(slot), "line A to zone 2: closed (soon): " + slot);
            helper.assertTrue(heeft(Poorten.uitleg(p, a, slot), "vadsrijk.zonepoort.lijn.2"), "the pier/board explanation says the ship sails soon");
            helper.assertTrue(!ZonePoort.isPoortSlot(Poorten.dicht(p, a, false)), "the way back (zone 2 -> zone 1) is never gated");
            helper.assertTrue(!ZonePoort.isPoortSlot(Poorten.dicht(p, Lijnen.vanId("kamp_z1"), true)), "line 0 (camp -> zone 1) stays open");
            helper.assertTrue("vadsrijk.zonepoort.reis.kandijkom".equals(QuestMotor.reisSlot(p, "kandijkom")), "travel NPC / bell: Kandijkom soon");
            helper.assertTrue(!ZonePoort.isPoortSlot(QuestMotor.reisSlot(p, "zwamzoet")), "De Haven stays a normal destination");
            for (String plek : new String[] {"kandijkom", "schemerwortel", "lampionkust", "hoogvads", "zijdeland"}) {
                var rp = Reiskaart.plek(p, plek);
                helper.assertTrue(rp.staat() == Reiskaart.TEASER && "vadsrijk.zonepoort.kaart".equals(rp.reden()), plek + ": grey 'binnenkort' on the travel map");
            }
            // raise the gate: everything opens at once, nothing to migrate
            ZonePoortConfig.TEST = 6;
            helper.assertTrue(!ZonePoort.isPoortSlot(Poorten.dicht(p, a, true)), "gate 6: line A is a normal line again");
            helper.assertTrue(QuestMotor.reisSlot(p, "kandijkom") == null, "gate 6, zone 1 done, lv 20: Kandijkom open");
            helper.assertTrue(Reiskaart.plek(p, "kandijkom").staat() != Reiskaart.TEASER, "gate 6: Kandijkom on the map");
            // gate 2: zone 2 open, zone 3 soon
            ZonePoortConfig.TEST = 2;
            helper.assertTrue(QuestMotor.reisSlot(p, "kandijkom") == null && "vadsrijk.zonepoort.reis.schemerwortel".equals(QuestMotor.reisSlot(p, "schemerwortel")),
                    "gate 2: Kandijkom open, De Schemerwortel soon");
            // ops / creative: through the gate
            ZonePoortConfig.TEST = 1;
            ZonePoort.TEST_STRENG = false;
            p.setGameMode(GameType.CREATIVE);
            helper.assertTrue(Poorten.dicht(p, a, true) == null && !ZonePoort.isPoortSlot(QuestMotor.reisSlot(p, "kandijkom")), "creative: through the gate");
        } finally {
            herstel();
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }

    /** The end of zone 1: the story completes, zone 2's first main quest is not offered, the tracker says "Wordt vervolgd". */
    @GuhTest(template = "empty", batch = BATCH)
    public static void eindeZone1(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            poort(1);
            for (Quest q : Quests.alle().values()) {
                if (q.hoofd() && ZoneBoeken.zoneVan(q.id()) == 1) {
                    Questlog.voltooi(p, q);
                }
            }
            Questlog.zetVlag(p, NpcApi.ZONE1_VLAG, true);
            Spelers.zetLevel(p, 20);
            Quest z2 = Quests.van("z2_hq01_splinterpieken");
            helper.assertTrue(z2 != null, "zone 2's first main quest exists");
            helper.assertTrue(!Questlog.beschikbaar(p, z2), "gate 1: zone 2's first quest is never offered");
            QuestStap s = QuestStap.hoofdlijn(p);
            helper.assertTrue(s != null && heeft(s.titel(), "vadsrijk.zonepoort.volg.titel") && heeft(s.wat(), "vadsrijk.zonepoort.volg.wat") && s.doel() == null,
                    "tracker: 'Wordt vervolgd: het schip naar zone 2 vaart binnenkort'");
            var regel = ZonePoort.binnenkortRegel(p, null, z2.gever());
            helper.assertTrue(regel != null && regel.tekstKey().equals("vadsrijk.zonepoort.schip"), "its giver (the captain) says the ship sails soon");
            helper.assertTrue(ZonePoort.binnenkortRegel(p, null, "grootmeester") == null || z2.gever().equals("grootmeester"), "other NPCs keep their own talk");
            helper.assertTrue(Questlog.status(p, z2.id()) == 0, "no quest state was touched");
            // raise the gate: the captain offers it at once
            ZonePoortConfig.TEST = 6;
            helper.assertTrue(Questlog.beschikbaar(p, z2), "gate 6: zone 2's first quest is offered");
            QuestStap t = QuestStap.hoofdlijn(p);
            helper.assertTrue(t != null && t.quest().equals(z2.id()) && !heeft(t.titel(), "vadsrijk.zonepoort.volg.titel"), "gate 6: the tracker shows the real next quest");
            helper.assertTrue(ZonePoort.binnenkortRegel(p, null, z2.gever()) == null, "gate 6: the captain has no 'soon' line");
        } finally {
            herstel();
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }

    /**
     * 1.10.2 (gate 3 on the server): zones 2 and 3 open, the end of zone 3 is the new cliffhanger. Lines A/B and the travel NPCs to
     * zones 2/3 work, line C / Lampionkust / zone 4 say "binnenkort", the soft cap is 50, zone 4's first main quest is not offered,
     * the tracker says "Wordt vervolgd" and both its giver (Wasja) and the captain of the ship to zone 4 (z3_kapitein) say the ship
     * sails soon; dungeons of zones 1-3 open, 4-6 closed.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void eindeZone3Poort3(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            poort(3);
            for (Quest q : Quests.alle().values()) {
                if (q.hoofd() && ZoneBoeken.zoneVan(q.id()) >= 1 && ZoneBoeken.zoneVan(q.id()) <= 3) {
                    Questlog.voltooi(p, q);
                }
            }
            for (String v : new String[] {NpcApi.ZONE1_VLAG, "zone2_klaar", "zone3_klaar"}) {
                Questlog.zetVlag(p, v, true);
            }
            Spelers.zetLevel(p, 50);
            helper.assertTrue(ZonePoort.hoogste(p) == 3 && ZonePoort.plafond(p) == 55,
                    "zones 1-3 played: the per-player cap is zone 3's top 50 + 5 = 55: " + ZonePoort.hoogste(p) + " / " + ZonePoort.plafond(p));
            // the ways to zones 2 and 3 work; zone 4 says soon
            helper.assertTrue(!ZonePoort.isPoortSlot(Poorten.dicht(p, Lijnen.vanId("z1_z2"), true)), "gate 3: line A to zone 2 open");
            helper.assertTrue(!ZonePoort.isPoortSlot(Poorten.dicht(p, Lijnen.vanId("z2_z3"), true)), "gate 3: line B to zone 3 open");
            String c = Poorten.dicht(p, Lijnen.vanId("z3_z4"), true);
            helper.assertTrue("vadsrijk.zonepoort.lijn.4".equals(c), "gate 3: line C to zone 4 sails soon: " + c);
            helper.assertTrue(!ZonePoort.isPoortSlot(Poorten.dicht(p, Lijnen.vanId("z3_z4"), false)), "gate 3: the way back from zone 4 is never gated");
            helper.assertTrue(QuestMotor.reisSlot(p, "kandijkom") == null && QuestMotor.reisSlot(p, "schemerwortel") == null,
                    "gate 3: Kandijkom and De Schemerwortel are normal destinations: " + QuestMotor.reisSlot(p, "kandijkom") + " / "
                            + QuestMotor.reisSlot(p, "schemerwortel"));
            helper.assertTrue("vadsrijk.zonepoort.reis.lampionkust".equals(QuestMotor.reisSlot(p, "lampionkust")), "gate 3: Lampionkust soon");
            helper.assertTrue(QuestMotor.reisDoelen(p, "z3_kapitein").contains("lampionkust"), "the zone-3 captain shows Lampionkust (locked, soon)");
            helper.assertTrue(Reiskaart.plek(p, "schemerwortel").staat() != Reiskaart.TEASER, "gate 3: De Schemerwortel on the travel map");
            for (String plek : new String[] {"lampionkust", "hoogvads", "zijdeland"}) {
                var rp = Reiskaart.plek(p, plek);
                helper.assertTrue(rp.staat() == Reiskaart.TEASER && "vadsrijk.zonepoort.kaart".equals(rp.reden()), plek + ": grey 'binnenkort' on the travel map");
            }
            helper.assertTrue(ZonePoort.dicht(p, 4) && !ZonePoort.dicht(p, 3) && !ZonePoort.dicht(p, 2), "gate 3: zone 4 closed, 2 and 3 open");
            // the end of zone 3: Wordt vervolgd + the ship sails soon
            Quest z4 = Quests.van("z4_hq01_lampionkust");
            helper.assertTrue(z4 != null && ZoneBoeken.zoneVan(z4.id()) == 4, "zone 4's first main quest exists");
            helper.assertTrue(!Questlog.beschikbaar(p, z4), "gate 3: zone 4's first quest is never offered");
            QuestStap s = QuestStap.hoofdlijn(p);
            helper.assertTrue(s != null && heeft(s.titel(), "vadsrijk.zonepoort.volg.titel") && heeft(s.wat(), "vadsrijk.zonepoort.zone.4") && s.doel() == null,
                    "tracker: 'Wordt vervolgd: het schip naar De Lampionkust vaart binnenkort'");
            var wasja = ZonePoort.binnenkortRegel(p, null, z4.gever());
            helper.assertTrue(wasja != null && wasja.tekstKey().equals("vadsrijk.zonepoort.schip"), "its giver (" + z4.gever() + ") says the ship sails soon");
            var kapitein = ZonePoort.binnenkortRegel(p, null, "z3_kapitein");
            helper.assertTrue(kapitein != null && kapitein.tekstKey().equals("vadsrijk.zonepoort.schip"), "the captain of the ship to zone 4 says it sails soon");
            helper.assertTrue(ZonePoort.binnenkortRegel(p, null, "z2_kapitein") == null && ZonePoort.binnenkortRegel(p, null, "luchtschipper") == null,
                    "the other captains keep their own talk");
            helper.assertTrue(Questlog.status(p, z4.id()) == 0, "no quest state was touched");
            // dungeons: zones 1-3 open, 4-6 closed
            for (Dungeon d : Dungeons.alle()) {
                boolean dicht = ZonePoort.dungeonDicht(p, d.id()) != null;
                helper.assertTrue(dicht == (d.zone() > 3), d.id() + " (zone " + d.zone() + "): " + (dicht ? "closed" : "open"));
            }
            // just after zone 1 (gate 3): zone 2's first quest is offered, the zone-1 captain has no 'soon' line
            Questlog.wisAlles(p);
            for (Quest q : Quests.alle().values()) {
                if (q.hoofd() && ZoneBoeken.zoneVan(q.id()) == 1) {
                    Questlog.voltooi(p, q);
                }
            }
            Questlog.zetVlag(p, NpcApi.ZONE1_VLAG, true);
            Spelers.zetLevel(p, 20);
            Quest z2 = Quests.van("z2_hq01_splinterpieken");
            helper.assertTrue(Questlog.beschikbaar(p, z2), "gate 3: zone 2's first quest is offered");
            helper.assertTrue(ZonePoort.binnenkortRegel(p, null, z2.gever()) == null, "gate 3: the zone-1 captain has no 'soon' line");
            QuestStap t = QuestStap.hoofdlijn(p);
            helper.assertTrue(t != null && t.quest().equals(z2.id()) && !heeft(t.titel(), "vadsrijk.zonepoort.volg.titel"), "gate 3: the tracker shows the real zone-2 quest");
        } finally {
            herstel();
            Questlog.wisAlles(p);
            Spelers.zetLevel(p, 1);
        }
        helper.succeed();
    }

    /**
     * 1.10.2: the level cap is per player: the top of the highest zone he ENTERED + 5 (zone 1 -> 25, 2 -> 35, 3 -> 55, 4 -> 61,
     * 5 -> 65, 6 -> none), whatever the server gate. Standing in a zone (the safety-net tick) raises it; finishing a zone's quest too.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void plafondPerSpeler(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            poort(3);
            int[] verwacht = {25, 25, 35, 55, 61, 65, 0};
            for (int z = 1; z <= 6; z++) {
                ZonePoort.zetHoogste(p, z);
                helper.assertTrue(ZonePoort.plafond(p) == verwacht[z], "highest zone " + z + ": cap " + verwacht[z] + ", got " + ZonePoort.plafond(p));
            }
            ZonePoort.zetHoogste(p, 1);
            poort(1);
            helper.assertTrue(ZonePoort.plafond(p) == 25, "never in zone 2: 25, also with the server gate at 1");
            poort(6);
            helper.assertTrue(ZonePoort.plafond(p) == 25, "never in zone 2: 25, also with every zone open on the server");
            // standing in zone 2 (gate 6, book off, zone-1 story done) raises it
            Questlog.zetVlag(p, NpcApi.ZONE1_VLAG, true);
            ZonePoort.vergeetCache(p.getUUID());
            p.snapTo(704.5, 100, -1100.5);
            ZonePoort.controleer(helper.getLevel(), p, 4);
            helper.assertTrue(ZonePoort.hoogste(p) == 2 && ZonePoort.plafond(p) == 35, "entered zone 2: cap 35: " + ZonePoort.hoogste(p));
            // finishing a zone-3 quest counts as having been there
            Quest z3 = Quests.van("z3_hq01_boom");
            Questlog.voltooi(p, z3);
            helper.assertTrue(ZonePoort.hoogste(p) == 3 && ZonePoort.plafond(p) == 55, "a finished zone-3 quest: cap 55");
            // the level stops at the cap (generic + class), the rest is banked
            ZonePoort.zetHoogste(p, 1);
            Spelers.zetLevel(p, 24);
            SpelerData d = Spelers.data(p);
            Spelers.geefXp(p, 50_000_000, "test", d.klasse);
            helper.assertTrue(d.level() == 25 && d.level(d.klasse) == 25 && ZonePoort.opPlafond(p, d), "the level stops at 25: " + d.level() + " / " + d.level(d.klasse));
        } finally {
            herstel();
            Questlog.wisAlles(p);
            Spelers.data(p).extra.remove("zonepoort");
            Spelers.zetLevel(p, 1);
            ZonePoort.vergeet(p.getUUID());
        }
        helper.succeed();
    }

    /**
     * 1.10.2: the per-player gate. With the server at 3, zone 2 opens for a player only after zone 1's main story AND the book %;
     * the captain, the travel map and the tracker say what is missing; the book % (or the robot's bypass) opens it with a "the way is
     * open" message; grandfathering (been there / a started quest); the way back is never gated; ops/creative walk through.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void toegangPerSpeler(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            poort(3);
            ZonePoortConfig.TEST_BOEK = 100;   // (a fresh mock player is far below 100 % of the zone-1 book)
            ZonePoort.zetHoogste(p, 1);
            // 1. no story, no book: closed, and the line says both
            helper.assertTrue(ZonePoort.dicht(p, 2) && !ZonePoort.serverDicht(p, 2), "zone 2: open on the server, closed for this player");
            String r = QuestMotor.reisSlot(p, "kandijkom");
            helper.assertTrue("vadsrijk.zonepoort.eis.2".equals(r), "travel NPC: the per-player gate key: " + r);
            helper.assertTrue(ZonePoort.isPoortSlot(r) && ZonePoort.isEis(r), "an 'eis' gate key");
            Object[] a = ZonePoort.args(p, r);
            helper.assertTrue(a.length == 6 && a[1].toString().contains("✘") && a[3].toString().contains("✘"), "both conditions marked ✘");
            // 2. story done, book short: still closed; the tracker says explore, the captain says what is missing
            for (Quest q : Quests.alle().values()) {
                if (q.hoofd() && ZoneBoeken.zoneVan(q.id()) == 1) {
                    Questlog.voltooi(p, q);
                }
            }
            Questlog.zetVlag(p, NpcApi.ZONE1_VLAG, true);
            Spelers.zetLevel(p, 20);
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(ZonePoort.verhaalKlaar(p, 1) && !ZonePoort.boekOk(p, 1), "story done, book " + ZonePoort.boekProcent(p, 1) + " % < 100 %");
            helper.assertTrue("vadsrijk.zonepoort.eis.2".equals(QuestMotor.reisSlot(p, "kandijkom")), "still closed for the book");
            helper.assertTrue("vadsrijk.zonepoort.eis.2".equals(Poorten.dicht(p, Lijnen.vanId("z1_z2"), true)), "airship line A: the per-player key");
            helper.assertTrue(!ZonePoort.isPoortSlot(Poorten.dicht(p, Lijnen.vanId("z1_z2"), false)), "the way back is never gated");
            Object[] b = ZonePoort.args(p, "vadsrijk.zonepoort.eis.2");
            helper.assertTrue(b[1].toString().contains("✔") && b[3].toString().contains("✘"), "story ✔, book ✘");
            Quest z2 = Quests.van("z2_hq01_splinterpieken");
            helper.assertTrue(!Questlog.beschikbaar(p, z2), "zone 2's first quest is not offered yet");
            QuestStap s = QuestStap.hoofdlijn(p);
            helper.assertTrue(s != null && heeft(s.titel(), "vadsrijk.zonepoort.verken.titel") && heeft(s.wat(), "vadsrijk.zonepoort.verken.wat"),
                    "tracker: 'Volgende: verken De Knuffelzwammen tot 100 %'");
            var regel = ZonePoort.binnenkortRegel(p, null, z2.gever());
            helper.assertTrue(regel != null && regel.tekstKey().equals("vadsrijk.zonepoort.eis"), "the captain says both conditions with progress");
            var rp = Reiskaart.plek(p, "kandijkom");
            helper.assertTrue(rp.staat() == Reiskaart.TEASER && "vadsrijk.zonepoort.eis.kaart".equals(rp.reden()), "travel map: the book % line: " + rp.reden());
            helper.assertTrue(Spelers.data(p).extra("zonepoort").getIntOr("toegang", 0) < 2, "no access stored yet");
            // 3. the book % reached (here: the requirement lowered): open, stored, the message once
            ZonePoortConfig.TEST_BOEK = 0;
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(!ZonePoort.dicht(p, 2) && QuestMotor.reisSlot(p, "kandijkom") == null, "both conditions met: zone 2 open");
            helper.assertTrue(Questlog.beschikbaar(p, z2), "zone 2's first quest is offered");
            ZonePoort.meldToegang(p);
            helper.assertTrue(Spelers.data(p).extra("zonepoort").getIntOr("toegang", 0) >= 2
                    && Spelers.data(p).extra("zonepoort").getIntOr("toegang_gemeld", 0) >= 2, "access stored + 'De weg naar ... is open!' sent");
            ZonePoortConfig.TEST_BOEK = 100;
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(!ZonePoort.dicht(p, 2), "once granted it stays granted (even when the book requirement goes up)");
            // 4. grandfathering + the robot's bypass, on a fresh player state
            Questlog.wisAlles(p);
            Spelers.data(p).extra.remove("zonepoort");
            ZonePoort.zetHoogste(p, 1);
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(ZonePoort.dicht(p, 2), "fresh: closed");
            ZonePoort.zetHoogste(p, 3);
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(!ZonePoort.dicht(p, 2) && !ZonePoort.dicht(p, 3), "been in zone 3 already: zones 2 and 3 stay open (grandfathered)");
            Spelers.data(p).extra.remove("zonepoort");
            ZonePoort.zetHoogste(p, 1);
            Questlog.zetVlag(p, NpcApi.ZONE1_VLAG, true);
            ZonePoort.BOEK_VRIJ.add(p.getUUID());
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(!ZonePoort.dicht(p, 2), "the quest robot's bypass counts as the book %");
            ZonePoort.BOEK_VRIJ.remove(p.getUUID());
            // 5. ops / creative walk through
            Spelers.data(p).extra.remove("zonepoort");
            ZonePoort.zetHoogste(p, 1);
            ZonePoort.vergeetCache(p.getUUID());
            ZonePoort.TEST_STRENG = false;
            p.setGameMode(GameType.CREATIVE);
            helper.assertTrue(!ZonePoort.dicht(p, 2) && QuestMotor.reisSlot(p, "kandijkom") == null, "creative: through the gate");
        } finally {
            herstel();
            ZonePoort.BOEK_VRIJ.remove(p.getUUID());
            Questlog.wisAlles(p);
            Spelers.data(p).extra.remove("zonepoort");
            Spelers.zetLevel(p, 1);
            ZonePoort.vergeet(p.getUUID());
        }
        helper.succeed();
    }

    /**
     * 1.10.2: fainting wakes you at the NEAREST campfire you lit, not the last one you clicked. Two campfires 64 blocks apart, the far
     * one clicked last (the checkpoint): faint next to the near one and you wake there; faint next to the far one and you wake
     * there. A campfire in a closed zone is skipped; nothing lit at all = the old rule.
     */
    @GuhTest(template = "empty", batch = BATCH + "_vuur")
    public static void flauwDichtstbijKampvuur(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ServerLevel sl = helper.getLevel();
        SpelerData d = Spelers.data(p);
        BlockPos oudCp = d.checkpoint;
        try {
            poort(6);
            ZonePoort.zetHoogste(p, 6);   // (wherever the test structure lies: every zone open for him)
            BlockPos a = helper.absolutePos(new BlockPos(1, 2, 1));
            BlockPos b = a.east(64);
            for (BlockPos v : new BlockPos[] {a, b}) {
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        sl.setBlockAndUpdate(v.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                        sl.setBlockAndUpdate(v.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        sl.setBlockAndUpdate(v.offset(dx, 1, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
                sl.setBlockAndUpdate(v, net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
            }
            Spelers.data(p).extra.remove("voltooiing");
            d.checkpoint = null;
            p.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 2.5);
            nl.juiced.guhs.feature.vadsrijk.kern.Kampvuren.steekAan(p, a);
            p.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 2.5);
            nl.juiced.guhs.feature.vadsrijk.kern.Kampvuren.steekAan(p, b);   // the far one is clicked last: the checkpoint
            BlockPos cp = d.checkpoint;
            helper.assertTrue(cp != null && cp.distSqr(b) <= 9, "the checkpoint is at the last clicked campfire: " + cp);
            helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.vuren(p).size() >= 2, "both campfires are remembered");
            // faint 5 blocks from campfire A
            p.snapTo(a.getX() + 5.5, a.getY(), a.getZ() + 0.5);
            BlockPos w = nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen.waarWakker(p);
            helper.assertTrue(w.distSqr(a) <= 9, "fainting next to A wakes you at A (the nearest), not at B (the last clicked): " + w + " A=" + a);
            // faint next to campfire B: B (the checkpoint spot)
            p.snapTo(b.getX() - 4.5, b.getY(), b.getZ() + 0.5);
            helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen.waarWakker(p).equals(cp), "fainting next to B wakes you at B");
            // a campfire that is gone does not count: B wins
            sl.setBlockAndUpdate(a, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            p.snapTo(a.getX() + 5.5, a.getY(), a.getZ() + 0.5);
            helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen.waarWakker(p).equals(cp), "a removed campfire is skipped: back to B");
            // nothing lit, no checkpoint: the old rule (the area's respawn)
            Spelers.data(p).extra.remove("voltooiing");
            d.checkpoint = null;
            helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.kern.Kampvuren.dichtstbij(p, p.position()) == null
                    && nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen.waarWakker(p).equals(nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen.standaard(p)),
                    "no campfire lit: the old rule");
        } finally {
            herstel();
            d.checkpoint = oudCp;
            Spelers.data(p).extra.remove("voltooiing");
        }
        helper.succeed();
    }

    /** 1.10.2: a lit campfire in a closed zone is skipped (the zone gate); with the gate open it is the nearest again. */
    @GuhTest(template = "empty", batch = BATCH + "_vuur")
    public static void flauwKampvuurDichteZone(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        SpelerData d = Spelers.data(p);
        BlockPos oudCp = d.checkpoint;
        try {
            Spelers.data(p).extra.remove("voltooiing");
            BlockPos z1 = new BlockPos(0, 70, 0), z3 = new BlockPos(1404, 150, -576);
            helper.assertTrue(ZonePoort.zoneOp(0.5, 70, 0.5) == 1 && ZonePoort.zoneOp(1404.5, 150, -575.5) == 3, "zone lookup");
            d.checkpoint = z1;   // (zone 1: the checkpoint spot itself)
            nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.kampvuur(p, z3);
            poort(1);
            BlockPos w = nl.juiced.guhs.feature.vadsrijk.kern.Kampvuren.dichtstbij(p, new Vec3(1404, 150, -570));
            helper.assertTrue(z1.equals(w), "gate 1: the zone-3 campfire is skipped, the zone-1 checkpoint wins: " + w);
        } finally {
            herstel();
            d.checkpoint = oudCp;
            Spelers.data(p).extra.remove("voltooiing");
        }
        helper.succeed();
    }

    /** Dungeons of a closed zone refuse (solo, group and queue all ask DungeonRij.weiger); the Zwamgrot (zone 1) stays open. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dungeonsDicht(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            poort(1);
            int dicht = 0, open = 0;
            for (Dungeon d : Dungeons.alle()) {
                Component w = ZonePoort.dungeonDicht(p, d.id());
                if (d.zone() <= 1) {
                    helper.assertTrue(w == null, d.id() + " (zone 1) open");
                    open++;
                } else {
                    helper.assertTrue(w != null && heeft(w, "vadsrijk.zonepoort.dungeon"), d.id() + " (zone " + d.zone() + ") soon");
                    dicht++;
                }
            }
            helper.assertTrue(open >= 1 && dicht >= 5, "zone 1 has its dungeon, zones 2-6 are closed: " + open + "/" + dicht);
            ZonePoortConfig.TEST = 6;
            ZonePoort.zetHoogste(p, 6);   // (1.10.2: the per-player gate: he has been everywhere)
            for (Dungeon d : Dungeons.alle()) {
                helper.assertTrue(ZonePoort.dungeonDicht(p, d.id()) == null, d.id() + ": gate 6 open");
            }
        } finally {
            herstel();
        }
        helper.succeed();
    }

    /** The safety net: in a closed zone you are put back on your last open spot (a pearl / elytra / walk); open zones: nothing. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void vangnet(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ServerLevel sl = helper.getLevel();
        try {
            poort(1);
            helper.assertTrue(ZonePoort.zoneOp(704, 100, -1100) == 2 && ZonePoort.zoneOp(0, 70, 0) == 1 && ZonePoort.zoneOp(-373, 70, 996) == 0
                    && ZonePoort.zoneOp(1404, 150, -576) == 3 && ZonePoort.zoneOp(3070, 272, -520) == 5 && ZonePoort.zoneOp(3730, 70, -760) == 6,
                    "zone lookup (zone 2 Kandijkom, zone 1, the camp, zone 3, the zone-5 dream cloud, zone 6)");
            BlockPos b = helper.absolutePos(new BlockPos(1, 2, 1));
            Vec3 thuis = Vec3.atBottomCenterOf(b);
            ZonePoort.zetLaatstOpen(p, thuis);
            p.snapTo(704.5, 100, -1100.5);   // (as if an ender pearl / glider dropped him in zone 2)
            helper.assertTrue(ZonePoort.controleer(sl, p, 2), "in zone 2 with the gate at 1: caught");
            helper.assertTrue(p.position().distanceTo(thuis) < 0.01, "put back on the last open spot: " + p.position());
            // close to the border: pushed back (not teleported)
            ZonePoort.zetLaatstOpen(p, new Vec3(704.5, 100, -1095.5));
            p.snapTo(704.5, 100, -1100.5);
            helper.assertTrue(ZonePoort.controleer(sl, p, 4) && p.getDeltaMovement().z > 0.3, "near the border: a push back south");
            // raise the gate / bypass: nothing happens
            ZonePoortConfig.TEST = 6;
            ZonePoort.zetHoogste(p, 2);   // (1.10.2: and the per-player gate lets him in: he has been there)
            p.snapTo(704.5, 100, -1100.5);
            helper.assertTrue(!ZonePoort.controleer(sl, p, 6), "gate 6: free to be in zone 2");
            ZonePoortConfig.TEST = 1;
            ZonePoort.TEST_STRENG = false;
            p.setGameMode(GameType.CREATIVE);
            helper.assertTrue(!ZonePoort.controleer(sl, p, 8), "creative: free");
        } finally {
            herstel();
            ZonePoort.vergeet(p.getUUID());
        }
        helper.succeed();
    }

    /** The soft cap: with gate 1 the level stops at 20 (generic and class); the rest is banked and paid out as a bonus later. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zachtPlafond(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            poort(1);
            ZonePoort.zetHoogste(p, 1);   // (1.10.2: never in zone 2 -> cap 25)
            Spelers.zetLevel(p, 24);
            SpelerData d = Spelers.data(p);
            String k = d.klasse;
            Spelers.geefXp(p, 200_000, "test", k);
            helper.assertTrue(d.level() == 25 && d.generiekXp == Levels.totaalVoorGeneriek(25), "generic level stops at 25: " + d.level());
            helper.assertTrue(d.level(k) == 25, "class level stops at 25: " + d.level(k));
            long pot = ZonePoort.gespaard(d, null);
            long max = Levels.totaalVoorGeneriek(25 + ZonePoort.BANK_LEVELS) - Levels.totaalVoorGeneriek(25);
            helper.assertTrue(pot > 0 && pot <= max, "the rest is banked (max " + ZonePoort.BANK_LEVELS + " levels): " + pot);
            helper.assertTrue(ZonePoort.opPlafond(p, d), "at the soft cap (HUD full, menu 'hoogste level voor nu')");
            Spelers.geefXp(p, 10_000_000, "test", k);
            helper.assertTrue(ZonePoort.gespaard(d, null) == max, "the bank is full at " + max);
            // the cap rises (1.10.2: he entered zone 6): the next gains get a +100 % bonus from the bank
            ZonePoort.zetHoogste(p, 6);
            long voor = d.generiekXp, pot0 = ZonePoort.gespaard(d, null);
            Spelers.geefXp(p, 100, "test", k);
            long erbij = d.generiekXp - voor, uitPot = pot0 - ZonePoort.gespaard(d, null);
            helper.assertTrue(uitPot > 0 && erbij - uitPot >= 100 && uitPot == Math.min(pot0, erbij - uitPot),
                    "cap up: the gain doubles from the bank (gain " + erbij + ", from the bank " + uitPot + ")");
            helper.assertTrue(!ZonePoort.opPlafond(p, d), "zone 6 entered: no cap");
        } finally {
            herstel();
            Questlog.wisAlles(p);
            Spelers.data(p).extra.remove("zonepoort");
            Spelers.zetLevel(p, 1);
        }
        helper.succeed();
    }

    /** The quest robot with only zone 1 open: every zone-1 main quest done, side quests too, and the story ends on "binnenkort". */
    @GuhTest(template = "empty", batch = BATCH + "_robot1", timeoutTicks = 1200)
    public static void robotZone1Poort1(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        try {
            poort(1);
            QuestRobot.Rapport r = QuestRobot.speel(p, 1);
            int hoofd = 0, hoofdGehaald = 0, zij = 0, zijGehaald = 0;
            StringBuilder vast = new StringBuilder();
            for (QuestRobot.Uitslag u : r.quests.values()) {
                if (u.zone != 1 || u.soort.equals("rijdier")) {
                    continue;   // (the mount return quests wait for your first mount: the zone-3 starter, by design)
                }
                boolean ok = u.status.equals("gehaald");
                if (u.soort.equals("hoofd")) {
                    hoofd++;
                    hoofdGehaald += ok ? 1 : 0;
                } else {
                    zij++;
                    zijGehaald += ok ? 1 : 0;
                }
                if (!ok) {
                    vast.append(' ').append(u.quest).append('=').append(u.status).append('(').append(u.waarom).append(')');
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("X-ZONE1 robot gate 1: main {}/{}, other {}/{}{}", hoofdGehaald, hoofd, zijGehaald, zij, vast);
            helper.assertTrue(hoofd >= 13 && hoofdGehaald == hoofd, "every zone-1 main quest done with gate 1: " + hoofdGehaald + "/" + hoofd + vast);
            helper.assertTrue(zijGehaald >= zij * 0.99 - 0.001 || zij - zijGehaald <= 0, "zone-1 side quests done: " + zijGehaald + "/" + zij + vast);
            helper.assertTrue(NpcApi.zone1Klaar(p), "the zone-1 story is finished (cliffhanger)");
            Quest z2 = Quests.van("z2_hq01_splinterpieken");
            helper.assertTrue(z2 != null && Questlog.status(p, z2.id()) == 0 && !Questlog.beschikbaar(p, z2), "zone 2 not started, not offered");
            QuestStap s = QuestStap.hoofdlijn(p);
            helper.assertTrue(s != null && heeft(s.titel(), "vadsrijk.zonepoort.volg.titel"), "tracker: Wordt vervolgd");
        } finally {
            herstel();
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }

    /** The quest robot with every zone open (gate 6, strict: the gate code runs but lets everything through): no main quest stuck. */
    @GuhTest(template = "empty", batch = BATCH + "_robot6", timeoutTicks = 1200)
    public static void robotAlleZonesPoort6(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        try {
            poort(6);
            QuestRobot.Rapport r = QuestRobot.speel(p, 6);
            int[] hoofd = new int[7], gehaald = new int[7];
            StringBuilder vast = new StringBuilder();
            for (QuestRobot.Uitslag u : r.quests.values()) {
                if (!u.soort.equals("hoofd") || u.zone < 1 || u.zone > 6) {
                    continue;
                }
                hoofd[u.zone]++;
                if (u.status.equals("gehaald")) {
                    gehaald[u.zone]++;
                } else {
                    vast.append(' ').append(u.quest).append('=').append(u.status).append('(').append(u.waarom).append(')');
                }
            }
            StringBuilder s = new StringBuilder();
            for (int z = 1; z <= 6; z++) {
                s.append(" z").append(z).append(' ').append(gehaald[z]).append('/').append(hoofd[z]);
            }
            com.mojang.logging.LogUtils.getLogger().info("X-ZONE1 robot gate 6: main{}{}", s, vast);
            for (int z = 1; z <= 6; z++) {
                helper.assertTrue(hoofd[z] > 0 && gehaald[z] == hoofd[z], "zone " + z + " main quests all done with gate 6:" + s + vast);
            }
        } finally {
            herstel();
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }
}
