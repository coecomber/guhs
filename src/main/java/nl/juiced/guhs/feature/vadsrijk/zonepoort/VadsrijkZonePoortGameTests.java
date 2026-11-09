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

    private static void poort(int open) {
        ZonePoortConfig.TEST = open;
        ZonePoort.TEST_STRENG = true;
    }

    private static void herstel() {
        ZonePoortConfig.TEST = null;
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
            Spelers.zetLevel(p, 19);
            SpelerData d = Spelers.data(p);
            String k = d.klasse;
            Spelers.geefXp(p, 20_000, "test", k);
            helper.assertTrue(d.level() == 20 && d.generiekXp == Levels.totaalVoorGeneriek(20), "generic level stops at 20: " + d.level());
            helper.assertTrue(d.level(k) == 20, "class level stops at 20: " + d.level(k));
            long pot = ZonePoort.gespaard(d, null);
            long max = Levels.totaalVoorGeneriek(20 + ZonePoort.BANK_LEVELS) - Levels.totaalVoorGeneriek(20);
            helper.assertTrue(pot > 0 && pot <= max, "the rest is banked (max " + ZonePoort.BANK_LEVELS + " levels): " + pot);
            helper.assertTrue(ZonePoort.opPlafond(p, d), "at the soft cap (HUD full, menu 'hoogste level voor nu')");
            Spelers.geefXp(p, 10_000_000, "test", k);
            helper.assertTrue(ZonePoort.gespaard(d, null) == max, "the bank is full at " + max);
            // gate up: the next gains get a +100 % bonus from the bank
            ZonePoortConfig.TEST = 6;
            long voor = d.generiekXp, pot0 = ZonePoort.gespaard(d, null);
            Spelers.geefXp(p, 100, "test", k);
            long erbij = d.generiekXp - voor, uitPot = pot0 - ZonePoort.gespaard(d, null);
            helper.assertTrue(uitPot > 0 && erbij - uitPot >= 100 && uitPot == Math.min(pot0, erbij - uitPot),
                    "gate 6: the gain doubles from the bank (gain " + erbij + ", from the bank " + uitPot + ")");
            helper.assertTrue(!ZonePoort.opPlafond(p, d), "gate 6: no cap");
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
                if (u.zone != 1) {
                    continue;
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
