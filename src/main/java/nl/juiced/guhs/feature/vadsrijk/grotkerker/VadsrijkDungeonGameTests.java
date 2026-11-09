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
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData.Deur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonNet;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
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
 * Run: {@code runGameTestServer -Pgt=VadsrijkDungeon}.
 */
public class VadsrijkDungeonGameTests {
    private static final String BATCH = "vadsrijk_dungeon";

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
        DungeonRij.wisAlles();
        DungeonRij.TEST = true;
        DungeonRij.TEST_LEVEL = helper.getLevel();
        GrotKerker.TEST_KLAAR = true;
        Kerker k = VadsrijkGrotKerkerGameTests.testKerker(helper);
        Kerkers.TEST.put(k.id(), k);
        Dungeons.TEST.put(k.id(), new GrotDungeon(k.id()));
        DungeonDeur.TEST.put(k.id(), List.of(testDeur(helper, k)));
        GrotKerker.reset(helper.getLevel(), k, GrotKerker.run(k.id()), null);
        return k;
    }

    static void opruimen(GameTestHelper helper, Kerker k, ServerPlayer... spelers) {
        GrotKerker.reset(helper.getLevel(), k, GrotKerker.run(k.id()), null);
        DungeonRij.wisAlles();
        DungeonRij.TEST = false;
        DungeonRij.TEST_LEVEL = null;
        GrotKerker.TEST_KLAAR = false;
        Kerkers.TEST.remove(k.id());
        Dungeons.TEST.remove(k.id());
        DungeonDeur.TEST.remove(k.id());
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
        helper.assertTrue(!DungeonDeurData.ALLE.isEmpty(), "door data");
        for (Kerker k : Kerkers.alle()) {
            Deur d = DungeonDeur.hoofd(k.id());
            helper.assertTrue(d != null, k.id() + ": a main door");
            helper.assertFalse(Dungeons.van(k.id()).bevat(d.voor().getX() + 0.5, d.voor().getY() + 0.5, d.voor().getZ() + 0.5),
                    k.id() + ": the stand spot is OUTSIDE the dungeon");
            helper.assertTrue(Dungeons.van(k.id()).bevat(d.binnen().getX() + 0.5, d.binnen().getY() + 0.5, d.binnen().getZ() + 0.5)
                    || !k.bevat(d.binnen().getX() + 0.5, d.binnen().getY() + 0.5, d.binnen().getZ() + 0.5), k.id() + ": the inside spot is not cut off");
            helper.assertTrue(d.cellen().size() >= 6, k.id() + ": a real door (" + d.cellen().size() + " blocks)");
            BlockPos m = DungeonDeur.midden(d);
            helper.assertTrue(m.distSqr(d.voor()) <= 12 * 12, k.id() + ": the stand spot is in front of the door");
            helper.assertTrue(k.bevat(d.binnen().getX() + 0.5, d.binnen().getY() + 0.5, d.binnen().getZ() + 0.5)
                    || d.binnen().distSqr(m) <= DungeonRij.BIJ_DEUR * DungeonRij.BIJ_DEUR, k.id() + ": the inside spot is in the cave or on the way");
            helper.assertTrue(GrotBazen.def(k.id()) != null, k.id() + ": a boss for the window");
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH + "_rij", timeoutTicks = 200)
    public static void rijVultEnStart(GameTestHelper helper) {
        Kerker k = opzet(helper);
        Deur d = DungeonDeur.hoofd(k.id());
        ServerPlayer p = speler(helper, d);
        helper.runAfterDelay(3, () -> {
            try {
                List<UUID> nep = DungeonRij.voegNepToe(k.id(), 3, "ja");
                helper.assertTrue(DungeonRij.inRij(p, k.id()), "into the queue");
                DungeonRij.Check c = DungeonRij.CHECKS.get(k.id());
                helper.assertTrue(c != null && c.leden.size() == 4 && c.uitRij, "4 in the queue: a ready check for all 4");
                helper.assertTrue(DungeonNet.LAATSTE.get(p.getUUID()) != null && DungeonNet.LAATSTE.get(p.getUUID()).meta().getStringOr("soort", "").equals("klaar"),
                        "the ready check window was sent");
                DungeonRij.antwoord(p, true);
                helper.assertTrue(DungeonRij.instantie(k.id()) == null, "not in before the others said yes");
                DungeonRij.tickRij(helper.getLevel().getServer(), c.start + 20);   // the made-up ones answer "yes"
                DungeonRij.Instantie i = DungeonRij.instantie(k.id());
                helper.assertTrue(i != null && i.leden.size() == 1 && i.leden.contains(p.getUUID()), "in: the cave is your group's (made-up ones stay out)");
                helper.assertTrue(DungeonRij.CHECKS.isEmpty() && DungeonRij.rijGrootte(k.id()) == 0, "check done, queue empty");
                for (UUID id : nep) {
                    helper.assertFalse(DungeonRij.NEP.containsKey(id), "made-up member gone");
                }
                helper.assertTrue(p.blockPosition().equals(d.binnen()), "teleported to just inside the door: " + p.blockPosition() + " vs " + d.binnen());
                helper.assertTrue(k.bevat(p.getX(), p.getY(), p.getZ()), "in the cave box");
                GrotKerker.Run r = GrotKerker.run(k.id());
                GrotKerker.tick(helper.getLevel(), k, r);
                helper.assertTrue(r.staat == GrotKerker.Staat.BEZIG && r.spelers == 1, "the run started for the group of 1: " + r.staat + " " + r.spelers);
                helper.assertFalse(DungeonRij.magStarten(k.id()), "once per group");
                helper.succeed();
            } finally {
                opruimen(helper, k, p);
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_weiger", timeoutTicks = 200)
    public static void weigerEnVerlaten(GameTestHelper helper) {
        Kerker k = opzet(helper);
        Deur d = DungeonDeur.hoofd(k.id());
        ServerPlayer p = speler(helper, d);
        helper.runAfterDelay(3, () -> {
            try {
                var s = helper.getLevel().getServer();
                // 1. somebody says no: no start, the "no" ones leave the queue, you stay queued
                DungeonRij.voegNepToe(k.id(), 3, "nee");
                DungeonRij.inRij(p, k.id());
                DungeonRij.Check c = DungeonRij.CHECKS.get(k.id());
                helper.assertTrue(c != null, "a check");
                DungeonRij.antwoord(p, true);
                DungeonRij.tickRij(s, c.start + 20);
                helper.assertTrue(DungeonRij.CHECKS.isEmpty() && DungeonRij.instantie(k.id()) == null, "a 'no': nobody goes in");
                helper.assertTrue(k.id().equals(DungeonRij.rijVan(p.getUUID())) && DungeonRij.rijGrootte(k.id()) == 1, "you stay queued, the decliners left: "
                        + DungeonRij.rijGrootte(k.id()));
                // 2. leaving the queue
                DungeonRij.uitRij(p);
                helper.assertTrue(DungeonRij.rijVan(p.getUUID()) == null, "out of the queue");
                // 3. the logout path (the event handler's call)
                DungeonRij.inRij(p, k.id());
                DungeonRij.uitRijWeg(p.getUUID(), null);
                helper.assertTrue(DungeonRij.rijVan(p.getUUID()) == null, "logout: out of the queue");
                // 4. time-out: the silent ones leave, you (who said yes) stay
                DungeonRij.voegNepToe(k.id(), 3, "niets");
                DungeonRij.inRij(p, k.id());
                c = DungeonRij.CHECKS.get(k.id());
                helper.assertTrue(c != null, "a check (time-out case)");
                DungeonRij.antwoord(p, true);
                DungeonRij.tickRij(s, c.tot + 1);
                helper.assertTrue(DungeonRij.CHECKS.isEmpty() && DungeonRij.instantie(k.id()) == null && DungeonRij.rijGrootte(k.id()) == 1,
                        "time-out: no start, silent ones out: " + DungeonRij.rijGrootte(k.id()));
                // 5. "Nu starten" with 2 of 4
                DungeonRij.voegNepToe(k.id(), 1, "ja");
                DungeonRij.nuStarten(p);
                c = DungeonRij.CHECKS.get(k.id());
                helper.assertTrue(c != null && c.leden.size() == 2, "start early: a check for the 2 in the queue");
                DungeonRij.antwoord(p, true);
                DungeonRij.tickRij(s, c.start + 20);
                helper.assertTrue(DungeonRij.lid(p, k.id()), "started with 2 of 4 (you in)");
                // 6. no queueing while in a dungeon
                helper.assertFalse(DungeonRij.inRij(p, k.id()), "no queue while inside a dungeon");
                helper.succeed();
            } finally {
                opruimen(helper, k, p);
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_uit", timeoutTicks = 200)
    public static void uitgangGeenLus(GameTestHelper helper) {
        Kerker k = opzet(helper);
        Deur d = DungeonDeur.hoofd(k.id());
        ServerPlayer p = speler(helper, d);
        helper.runAfterDelay(3, () -> {
            try {
                var s = helper.getLevel().getServer();
                helper.assertTrue(DungeonRij.solo(p, k.id(), false), "solo in");
                helper.assertTrue(k.bevat(p.getX(), p.getY(), p.getZ()), "inside");
                GrotKerker.tick(helper.getLevel(), k, GrotKerker.run(k.id()));
                DungeonRij.verlaat(p, k.id());
                helper.assertTrue(p.blockPosition().equals(d.voor()), "out: on the stand spot in front of the door: " + p.blockPosition());
                helper.assertFalse(k.bevat(p.getX(), p.getY(), p.getZ()), "the stand spot is outside the cave box");
                helper.assertTrue(DungeonRij.instantie(k.id()) == null, "the group left: the cave closed");
                helper.assertTrue(GrotKerker.staat(k.id()) == GrotKerker.Staat.SLAAPT, "the run reset");
                // ticking on: nothing bounces you back in
                for (int t = 1; t <= 10; t++) {
                    DungeonRij.uitzetten(helper.getLevel(), Dungeons.van(k.id()));
                    DungeonRij.tickRij(s, nu(helper) + t * 10L);
                    GrotKerker.tick(helper.getLevel(), k, GrotKerker.run(k.id()));
                }
                helper.assertTrue(p.blockPosition().equals(d.voor()) && DungeonRij.dungeonVan(p.getUUID()) == null, "still outside after ticking: " + p.blockPosition());
                helper.assertTrue(GrotKerker.staat(k.id()) == GrotKerker.Staat.SLAAPT, "no run without the group");
                // a door click right after coming out does nothing (grace), later it opens the window (never a teleport)
                BlockPos cel = new BlockPos(d.cellen().get(0)[0], d.cellen().get(0)[1], d.cellen().get(0)[2]);
                DungeonNet.LAATSTE.remove(p.getUUID());
                DungeonRij.klikDeur(helper.getLevel(), cel, p);
                helper.assertTrue(DungeonNet.LAATSTE.get(p.getUUID()) == null, "grace: the click right after coming out is ignored");
                DungeonRij.NET_UIT.clear();
                DungeonRij.klikDeur(helper.getLevel(), cel, p);
                helper.assertTrue(DungeonNet.LAATSTE.get(p.getUUID()) != null && DungeonNet.LAATSTE.get(p.getUUID()).meta().getStringOr("soort", "").equals("deur")
                        && p.blockPosition().equals(d.voor()), "a click opens the window, it does not move you");
                // the cooldown blocks a new start; the dev reset lifts it
                DungeonRij.zetWachttijd(p, k.id(), 60_000);
                helper.assertTrue(DungeonRij.weiger(p, k.id(), true) != null && !DungeonRij.solo(p, k.id(), false), "cooldown: no start");
                DungeonRij.zetWachttijd(p, k.id(), 0);
                helper.assertTrue(DungeonRij.weiger(p, k.id(), true) == null, "cooldown reset: may start");
                // no start while already in another dungeon
                DungeonRij.devToegang(p, "kristalgrot");
                helper.assertTrue(DungeonRij.weiger(p, k.id(), true) != null, "no start while in another dungeon");
                helper.succeed();
            } finally {
                opruimen(helper, k, p);
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_deur", timeoutTicks = 200)
    public static void deurBlokkeert(GameTestHelper helper) {
        Kerker k = opzet(helper);
        Deur d = DungeonDeur.hoofd(k.id());
        ServerPlayer lid = speler(helper, d);
        ServerPlayer ander = speler(helper, d);
        helper.runAfterDelay(3, () -> {
            try {
                ServerLevel sl = helper.getLevel();
                // a solid block in one cell before: never replaced
                int[] c0 = d.cellen().get(0);
                BlockPos vast = new BlockPos(c0[0], c0[1], c0[2]);
                sl.setBlock(vast, Blocks.STONE.defaultBlockState(), 2);
                int n = DungeonDeur.plaats(sl, d);
                helper.assertTrue(n == d.cellen().size() - 1 && sl.getBlockState(vast).is(Blocks.STONE), "only open cells become door: " + n);
                sl.setBlock(vast, Blocks.AIR.defaultBlockState(), 2);
                helper.assertTrue(DungeonDeur.plaats(sl, d) == d.cellen().size(), "the whole door");
                for (int[] c : d.cellen()) {
                    BlockPos b = new BlockPos(c[0], c[1], c[2]);
                    BlockState s = sl.getBlockState(b);
                    helper.assertTrue(s.is(DungeonDeur.BLOK.get()), "door block at " + b);
                    helper.assertTrue(s.getCollisionShape(sl, b).equals(Shapes.block()) && s.blocksMotion(), "a full solid block: you cannot walk through");
                    helper.assertTrue(s.getDestroySpeed(sl, b) < 0, "unbreakable");
                }
                helper.assertTrue(sl.getBlockState(new BlockPos(d.cellen().get(4)[0], d.cellen().get(4)[1], d.cellen().get(4)[2])).getValue(DungeonDeur.DEEL) == 1,
                        "the knocker in the middle");
                // the member is in; the other one sneaks into the box: put outside the door; the member stays
                helper.assertTrue(DungeonRij.solo(lid, k.id(), false), "the member in");
                ander.teleportTo(k.kamers().get(0).x() + 0.5, k.kamers().get(0).y(), k.kamers().get(0).z() + 0.5);
                helper.assertTrue(k.bevat(ander.getX(), ander.getY(), ander.getZ()), "the other one is in the box");
                DungeonRij.uitzetten(sl, Dungeons.van(k.id()));
                helper.assertTrue(ander.blockPosition().equals(d.voor()), "a non-member is put outside the door: " + ander.blockPosition());
                helper.assertTrue(k.bevat(lid.getX(), lid.getY(), lid.getZ()), "the member stays inside");
                // the other one cannot start (the cave is taken) and gets the window with "bezet"
                helper.assertFalse(DungeonRij.solo(ander, k.id(), false), "the door does not open for a non-member while the cave is taken");
                helper.assertFalse(DungeonRij.lid(ander, k.id()), "not a member");
                helper.succeed();
            } finally {
                for (int[] c : d.cellen()) {
                    helper.getLevel().setBlock(new BlockPos(c[0], c[1], c[2]), Blocks.AIR.defaultBlockState(), 2);
                }
                opruimen(helper, k, lid, ander);
            }
        });
    }

    /**
     * X-DUNGEON: the name board over a door: exactly 2 text displays (name + small line), right over the door's top and 1 block out
     * towards the stand spot, the name text; running it again (a chunk reload, every 2 s) makes no doubles; a killed line comes
     * back; a moved door takes its board along (the old one goes). A seal ("zegel") click gives a message, not the window. And the
     * door item exists (creative / JEI only).
     */
    @GuhTest(template = "empty", batch = BATCH + "_bord", timeoutTicks = 200)
    public static void naambord(GameTestHelper helper) {
        Kerker k = opzet(helper);
        Deur d = DungeonDeur.hoofd(k.id());
        ServerPlayer p = speler(helper, d);
        helper.runAfterDelay(3, () -> {
            ServerLevel sl = helper.getLevel();
            try {
                DungeonDeur.plaats(sl, d);
                nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.zorg(sl, d, 99, false);
                List<net.minecraft.world.entity.Display.TextDisplay> b = nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.borden(sl, d);
                helper.assertTrue(b.size() == 2, "two board lines: " + b.size() + " (plek " + nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.plek(sl, d)
                        + ", klaar " + nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, DungeonDeur.midden(d)) + ")");
                int top = Integer.MIN_VALUE;
                for (int[] c : d.cellen()) {
                    top = Math.max(top, c[1]);
                }
                for (var e : b) {
                    helper.assertTrue(e.getY() >= top + 1 && e.getY() < top + 2, "over the door's top: " + e.getY());
                    helper.assertTrue(e.getZ() > d.cellen().get(0)[2] + 1 && e.getZ() < d.voor().getZ() + 1, "outside, towards the stand spot: " + e.getZ());
                }
                String tekst = nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.regels(d).get(0).getString();
                helper.assertTrue(tekst.equals(Dungeons.van(k.id()).naam().getString()), "the name line: " + tekst);
                for (int i = 0; i < 3; i++) {
                    nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.zorg(sl, d, 99, false);
                }
                helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.borden(sl, d).size() == 2, "no doubles");
                b.get(0).discard();
                nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.zorg(sl, d, 99, false);
                helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.borden(sl, d).size() == 2, "a killed line comes back");
                // the door moves 1 block west: the board goes along (still 2, all over the moved door)
                Deur d2 = DungeonDeur.verschoven(d, -1, 0, 0);
                for (int[] c : d.cellen()) {
                    sl.setBlock(new BlockPos(c[0], c[1], c[2]), Blocks.AIR.defaultBlockState(), 2);
                }
                DungeonDeur.plaats(sl, d2);
                nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.zorg(sl, d2, 99, false);
                List<net.minecraft.world.entity.Display.TextDisplay> b2 = nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.borden(sl, d2);
                helper.assertTrue(b2.size() == 2, "after the move: two lines: " + b2.size());
                double mx = 0;
                for (int[] c : d2.cellen()) {
                    mx += c[0] + 0.5;
                }
                mx /= d2.cellen().size();
                for (var e : b2) {
                    helper.assertTrue(Math.abs(e.getX() - mx) < 0.01, "over the moved door: " + e.getX() + " vs " + mx);
                }
                for (int[] c : d2.cellen()) {
                    sl.setBlock(new BlockPos(c[0], c[1], c[2]), Blocks.AIR.defaultBlockState(), 2);
                }
                // a seal: a click says where the door is, no window
                Deur zegel = new Deur(d.dungeon(), false, d.thema(), false, d.cellen(), d.voor(), d.yawVoor(), d.binnen(), d.yawBinnen(), true);
                DungeonDeur.TEST.put(k.id(), List.of(d, zegel));
                DungeonDeur.plaats(sl, d);
                DungeonRij.KIJKERS.remove(p.getUUID());
                int[] c0 = d.cellen().get(0);
                DungeonDeur.TEST.put(k.id(), List.of(zegel));
                DungeonRij.klikDeur(sl, new BlockPos(c0[0], c0[1], c0[2]), p);
                helper.assertFalse(DungeonRij.KIJKERS.containsKey(p.getUUID()), "a seal opens no window");
                DungeonDeur.TEST.put(k.id(), List.of(d));
                DungeonRij.klikDeur(sl, new BlockPos(c0[0], c0[1], c0[2]), p);
                helper.assertTrue(DungeonRij.KIJKERS.containsKey(p.getUUID()), "the door opens the window");
                helper.assertTrue(DungeonDeur.ITEM.get().getDefaultInstance().getItem() instanceof net.minecraft.world.item.BlockItem bi
                        && bi.getBlock() == DungeonDeur.BLOK.get(), "the door has an item");
                helper.succeed();
            } finally {
                for (var e : sl.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,
                        new net.minecraft.world.phys.AABB(DungeonDeur.midden(d)).inflate(10),
                        e -> e.entityTags().contains(nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonBord.TAG))) {
                    e.discard();
                }
                for (int[] c : d.cellen()) {
                    sl.setBlock(new BlockPos(c[0], c[1], c[2]), Blocks.AIR.defaultBlockState(), 2);
                }
                opruimen(helper, k, p);
            }
        });
    }
}
