package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotDeurData.Deur;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * W-GROT: the closed cave doors, the group per cave, the queue and the ready check, in the small test cave of
 * {@link VadsrijkGrotKerkerGameTests#testKerker} with a test door in front of it.
 * <ul>
 *   <li>{@code data}: every cave in the build has a main door; its stand spot ("voor") lies OUTSIDE the cave box, its inside spot in
 *       the cave or on the way to it; the door cells are near the cave.</li>
 *   <li>{@code rijVultEnStart}: you + 3 made-up members fill the queue to 4 -> a ready check; all yes -> you are teleported in
 *       (the made-up ones stay out), the cave is your group's, the run starts for 1 player; the queue is empty.</li>
 *   <li>{@code weigerEnVerlaten}: a "no" in the check -> no start, the decliners leave the queue, you stay queued; leaving the queue;
 *       the logout path; the time-out path (silent ones out); "Nu starten" with 2 of 4.</li>
 *   <li>{@code uitgangGeenLus}: out through the door = standing on the stand spot outside the box, the cave closes; ticking on keeps
 *       you outside (no bounce back in); the cooldown blocks a new start and the dev reset lifts it; no start while in another
 *       dungeon.</li>
 *   <li>{@code deurBlokkeert}: the door is a solid, unbreakable full block placed only in open cells; a non-member in the cave box
 *       is put outside the door, the member stays.</li>
 * </ul>
 * Run: {@code runGameTestServer -Pgt=VadsrijkGrotDeur}.
 */
public class VadsrijkGrotDeurGameTests {
    private static final String BATCH = "vadsrijk_grotdeur";

    /** The test door: 3 wide x 3 high, 2 blocks south of the test cave's box; "voor" 2 further south, "binnen" in the cave. */
    static Deur testDeur(GameTestHelper helper, Kerker k) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        for (int x = -1; x <= 13; x++) {
            for (int z = 14; z <= 19; z++) {
                sl.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 0; y < 5; y++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        int X = o.getX(), Y = o.getY(), Z = o.getZ();
        List<int[]> cellen = new java.util.ArrayList<>();
        for (int x = 5; x <= 7; x++) {
            for (int y = 0; y <= 2; y++) {
                cellen.add(new int[] {X + x, Y + y, Z + 15, x == 6 && y == 1 ? 1 : 0});
            }
        }
        return new Deur(k.id(), true, "zwam", false, List.copyOf(cellen), new BlockPos(X + 6, Y, Z + 18), 180f, new BlockPos(X + 6, Y, Z + 12), 0f);
    }

    /** Sets everything up; returns the cave. */
    static Kerker opzet(GameTestHelper helper) {
        GrotRij.wisAlles();
        GrotRij.TEST = true;
        GrotRij.TEST_LEVEL = helper.getLevel();
        GrotKerker.TEST_KLAAR = true;
        Kerker k = VadsrijkGrotKerkerGameTests.testKerker(helper);
        Kerkers.TEST.put(k.id(), k);
        GrotDeur.TEST.put(k.id(), List.of(testDeur(helper, k)));
        GrotKerker.reset(helper.getLevel(), k, GrotKerker.run(k.id()), null);
        return k;
    }

    static void opruimen(GameTestHelper helper, Kerker k, ServerPlayer... spelers) {
        GrotKerker.reset(helper.getLevel(), k, GrotKerker.run(k.id()), null);
        GrotRij.wisAlles();
        GrotRij.TEST = false;
        GrotRij.TEST_LEVEL = null;
        GrotKerker.TEST_KLAAR = false;
        Kerkers.TEST.remove(k.id());
        GrotDeur.TEST.remove(k.id());
        for (ServerPlayer p : spelers) {
            Spelers.data(p).extra.remove(GrotKerker.DATA);
        }
    }

    static ServerPlayer speler(GameTestHelper helper, Deur d) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Spelers.data(p).extra.remove(GrotKerker.DATA);
        p.teleportTo(d.voor().getX() + 0.5, d.voor().getY(), d.voor().getZ() + 0.5);
        return p;
    }

    static long nu(GameTestHelper helper) {
        return helper.getLevel().getServer().getTickCount();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void data(GameTestHelper helper) {
        helper.assertTrue(!GrotDeurData.ALLE.isEmpty(), "door data");
        for (Kerker k : Kerkers.alle()) {
            Deur d = GrotDeur.hoofd(k.id());
            helper.assertTrue(d != null, k.id() + ": a main door");
            helper.assertFalse(k.bevat(d.voor().getX() + 0.5, d.voor().getY() + 0.5, d.voor().getZ() + 0.5), k.id() + ": the stand spot is OUTSIDE the cave box");
            helper.assertTrue(d.cellen().size() >= 6, k.id() + ": a real door (" + d.cellen().size() + " blocks)");
            BlockPos m = GrotDeur.midden(d);
            helper.assertTrue(m.distSqr(d.voor()) <= 12 * 12, k.id() + ": the stand spot is in front of the door");
            helper.assertTrue(k.bevat(d.binnen().getX() + 0.5, d.binnen().getY() + 0.5, d.binnen().getZ() + 0.5)
                    || d.binnen().distSqr(m) <= GrotRij.BIJ_DEUR * GrotRij.BIJ_DEUR, k.id() + ": the inside spot is in the cave or on the way");
            helper.assertTrue(GrotBazen.def(k.id()) != null, k.id() + ": a boss for the window");
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH + "_rij", timeoutTicks = 200)
    public static void rijVultEnStart(GameTestHelper helper) {
        Kerker k = opzet(helper);
        Deur d = GrotDeur.hoofd(k.id());
        ServerPlayer p = speler(helper, d);
        helper.runAfterDelay(3, () -> {
            try {
                List<UUID> nep = GrotRij.voegNepToe(k.id(), 3, "ja");
                helper.assertTrue(GrotRij.inRij(p, k.id()), "into the queue");
                GrotRij.Check c = GrotRij.CHECKS.get(k.id());
                helper.assertTrue(c != null && c.leden.size() == 4 && c.uitRij, "4 in the queue: a ready check for all 4");
                helper.assertTrue(GrotNet.LAATSTE.get(p.getUUID()) != null && GrotNet.LAATSTE.get(p.getUUID()).meta().getStringOr("soort", "").equals("klaar"),
                        "the ready check window was sent");
                GrotRij.antwoord(p, true);
                helper.assertTrue(GrotRij.instantie(k.id()) == null, "not in before the others said yes");
                GrotRij.tickRij(helper.getLevel().getServer(), c.start + 20);   // the made-up ones answer "yes"
                GrotRij.Instantie i = GrotRij.instantie(k.id());
                helper.assertTrue(i != null && i.leden.size() == 1 && i.leden.contains(p.getUUID()), "in: the cave is your group's (made-up ones stay out)");
                helper.assertTrue(GrotRij.CHECKS.isEmpty() && GrotRij.rijGrootte(k.id()) == 0, "check done, queue empty");
                for (UUID id : nep) {
                    helper.assertFalse(GrotRij.NEP.containsKey(id), "made-up member gone");
                }
                helper.assertTrue(p.blockPosition().equals(d.binnen()), "teleported to just inside the door: " + p.blockPosition() + " vs " + d.binnen());
                helper.assertTrue(k.bevat(p.getX(), p.getY(), p.getZ()), "in the cave box");
                GrotKerker.Run r = GrotKerker.run(k.id());
                GrotKerker.tick(helper.getLevel(), k, r);
                helper.assertTrue(r.staat == GrotKerker.Staat.BEZIG && r.spelers == 1, "the run started for the group of 1: " + r.staat + " " + r.spelers);
                helper.assertFalse(GrotRij.magStarten(k.id()), "once per group");
                helper.succeed();
            } finally {
                opruimen(helper, k, p);
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_weiger", timeoutTicks = 200)
    public static void weigerEnVerlaten(GameTestHelper helper) {
        Kerker k = opzet(helper);
        Deur d = GrotDeur.hoofd(k.id());
        ServerPlayer p = speler(helper, d);
        helper.runAfterDelay(3, () -> {
            try {
                var s = helper.getLevel().getServer();
                // 1. somebody says no: no start, the "no" ones leave the queue, you stay queued
                GrotRij.voegNepToe(k.id(), 3, "nee");
                GrotRij.inRij(p, k.id());
                GrotRij.Check c = GrotRij.CHECKS.get(k.id());
                helper.assertTrue(c != null, "a check");
                GrotRij.antwoord(p, true);
                GrotRij.tickRij(s, c.start + 20);
                helper.assertTrue(GrotRij.CHECKS.isEmpty() && GrotRij.instantie(k.id()) == null, "a 'no': nobody goes in");
                helper.assertTrue(k.id().equals(GrotRij.rijVan(p.getUUID())) && GrotRij.rijGrootte(k.id()) == 1, "you stay queued, the decliners left: "
                        + GrotRij.rijGrootte(k.id()));
                // 2. leaving the queue
                GrotRij.uitRij(p);
                helper.assertTrue(GrotRij.rijVan(p.getUUID()) == null, "out of the queue");
                // 3. the logout path (the event handler's call)
                GrotRij.inRij(p, k.id());
                GrotRij.uitRijWeg(p.getUUID(), null);
                helper.assertTrue(GrotRij.rijVan(p.getUUID()) == null, "logout: out of the queue");
                // 4. time-out: the silent ones leave, you (who said yes) stay
                GrotRij.voegNepToe(k.id(), 3, "niets");
                GrotRij.inRij(p, k.id());
                c = GrotRij.CHECKS.get(k.id());
                helper.assertTrue(c != null, "a check (time-out case)");
                GrotRij.antwoord(p, true);
                GrotRij.tickRij(s, c.tot + 1);
                helper.assertTrue(GrotRij.CHECKS.isEmpty() && GrotRij.instantie(k.id()) == null && GrotRij.rijGrootte(k.id()) == 1,
                        "time-out: no start, silent ones out: " + GrotRij.rijGrootte(k.id()));
                // 5. "Nu starten" with 2 of 4
                GrotRij.voegNepToe(k.id(), 1, "ja");
                GrotRij.nuStarten(p);
                c = GrotRij.CHECKS.get(k.id());
                helper.assertTrue(c != null && c.leden.size() == 2, "start early: a check for the 2 in the queue");
                GrotRij.antwoord(p, true);
                GrotRij.tickRij(s, c.start + 20);
                helper.assertTrue(GrotRij.lid(p, k.id()), "started with 2 of 4 (you in)");
                // 6. no queueing while in a dungeon
                helper.assertFalse(GrotRij.inRij(p, k.id()), "no queue while inside a dungeon");
                helper.succeed();
            } finally {
                opruimen(helper, k, p);
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_uit", timeoutTicks = 200)
    public static void uitgangGeenLus(GameTestHelper helper) {
        Kerker k = opzet(helper);
        Deur d = GrotDeur.hoofd(k.id());
        ServerPlayer p = speler(helper, d);
        helper.runAfterDelay(3, () -> {
            try {
                var s = helper.getLevel().getServer();
                helper.assertTrue(GrotRij.solo(p, k.id(), false), "solo in");
                helper.assertTrue(k.bevat(p.getX(), p.getY(), p.getZ()), "inside");
                GrotKerker.tick(helper.getLevel(), k, GrotKerker.run(k.id()));
                GrotRij.verlaat(p, k.id());
                helper.assertTrue(p.blockPosition().equals(d.voor()), "out: on the stand spot in front of the door: " + p.blockPosition());
                helper.assertFalse(k.bevat(p.getX(), p.getY(), p.getZ()), "the stand spot is outside the cave box");
                helper.assertTrue(GrotRij.instantie(k.id()) == null, "the group left: the cave closed");
                helper.assertTrue(GrotKerker.staat(k.id()) == GrotKerker.Staat.SLAAPT, "the run reset");
                // ticking on: nothing bounces you back in
                for (int t = 1; t <= 10; t++) {
                    GrotRij.uitzetten(helper.getLevel(), k);
                    GrotRij.tickRij(s, nu(helper) + t * 10L);
                    GrotKerker.tick(helper.getLevel(), k, GrotKerker.run(k.id()));
                }
                helper.assertTrue(p.blockPosition().equals(d.voor()) && GrotRij.grotVan(p.getUUID()) == null, "still outside after ticking: " + p.blockPosition());
                helper.assertTrue(GrotKerker.staat(k.id()) == GrotKerker.Staat.SLAAPT, "no run without the group");
                // a door click right after coming out does nothing (grace), later it opens the window (never a teleport)
                BlockPos cel = new BlockPos(d.cellen().get(0)[0], d.cellen().get(0)[1], d.cellen().get(0)[2]);
                GrotNet.LAATSTE.remove(p.getUUID());
                GrotRij.klikDeur(helper.getLevel(), cel, p);
                helper.assertTrue(GrotNet.LAATSTE.get(p.getUUID()) == null, "grace: the click right after coming out is ignored");
                GrotRij.NET_UIT.clear();
                GrotRij.klikDeur(helper.getLevel(), cel, p);
                helper.assertTrue(GrotNet.LAATSTE.get(p.getUUID()) != null && GrotNet.LAATSTE.get(p.getUUID()).meta().getStringOr("soort", "").equals("deur")
                        && p.blockPosition().equals(d.voor()), "a click opens the window, it does not move you");
                // the cooldown blocks a new start; the dev reset lifts it
                GrotRij.zetWachttijd(p, k.id(), 60_000);
                helper.assertTrue(GrotRij.weiger(p, k.id(), true) != null && !GrotRij.solo(p, k.id(), false), "cooldown: no start");
                GrotRij.zetWachttijd(p, k.id(), 0);
                helper.assertTrue(GrotRij.weiger(p, k.id(), true) == null, "cooldown reset: may start");
                // no start while already in another dungeon
                GrotRij.devToegang(p, "kristalgrot");
                helper.assertTrue(GrotRij.weiger(p, k.id(), true) != null, "no start while in another dungeon");
                helper.succeed();
            } finally {
                opruimen(helper, k, p);
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_deur", timeoutTicks = 200)
    public static void deurBlokkeert(GameTestHelper helper) {
        Kerker k = opzet(helper);
        Deur d = GrotDeur.hoofd(k.id());
        ServerPlayer lid = speler(helper, d);
        ServerPlayer ander = speler(helper, d);
        helper.runAfterDelay(3, () -> {
            try {
                ServerLevel sl = helper.getLevel();
                // a solid block in one cell before: never replaced
                int[] c0 = d.cellen().get(0);
                BlockPos vast = new BlockPos(c0[0], c0[1], c0[2]);
                sl.setBlock(vast, Blocks.STONE.defaultBlockState(), 2);
                int n = GrotDeur.plaats(sl, d);
                helper.assertTrue(n == d.cellen().size() - 1 && sl.getBlockState(vast).is(Blocks.STONE), "only open cells become door: " + n);
                sl.setBlock(vast, Blocks.AIR.defaultBlockState(), 2);
                helper.assertTrue(GrotDeur.plaats(sl, d) == d.cellen().size(), "the whole door");
                for (int[] c : d.cellen()) {
                    BlockPos b = new BlockPos(c[0], c[1], c[2]);
                    BlockState s = sl.getBlockState(b);
                    helper.assertTrue(s.is(GrotDeur.BLOK.get()), "door block at " + b);
                    helper.assertTrue(s.getCollisionShape(sl, b).equals(Shapes.block()) && s.blocksMotion(), "a full solid block: you cannot walk through");
                    helper.assertTrue(s.getDestroySpeed(sl, b) < 0, "unbreakable");
                }
                helper.assertTrue(sl.getBlockState(new BlockPos(d.cellen().get(4)[0], d.cellen().get(4)[1], d.cellen().get(4)[2])).getValue(GrotDeur.DEEL) == 1,
                        "the knocker in the middle");
                // the member is in; the other one sneaks into the box: put outside the door; the member stays
                helper.assertTrue(GrotRij.solo(lid, k.id(), false), "the member in");
                ander.teleportTo(k.kamers().get(0).x() + 0.5, k.kamers().get(0).y(), k.kamers().get(0).z() + 0.5);
                helper.assertTrue(k.bevat(ander.getX(), ander.getY(), ander.getZ()), "the other one is in the box");
                GrotRij.uitzetten(sl, k);
                helper.assertTrue(ander.blockPosition().equals(d.voor()), "a non-member is put outside the door: " + ander.blockPosition());
                helper.assertTrue(k.bevat(lid.getX(), lid.getY(), lid.getZ()), "the member stays inside");
                // the other one cannot start (the cave is taken) and gets the window with "bezet"
                helper.assertFalse(GrotRij.solo(ander, k.id(), false), "the door does not open for a non-member while the cave is taken");
                helper.assertFalse(GrotRij.lid(ander, k.id()), "not a member");
                helper.succeed();
            } finally {
                for (int[] c : d.cellen()) {
                    helper.getLevel().setBlock(new BlockPos(c[0], c[1], c[2]), Blocks.AIR.defaultBlockState(), 2);
                }
                opruimen(helper, k, lid, ander);
            }
        });
    }
}
