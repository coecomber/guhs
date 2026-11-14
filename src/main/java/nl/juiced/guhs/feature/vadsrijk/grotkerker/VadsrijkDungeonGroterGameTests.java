package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData.Deur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotKerker.Run;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotKerker.Staat;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kamer;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.3 "Grotere dungeons" (+ the live dungeon bugs and the leave button).
 * <ul>
 *   <li>{@code data}: every cave from zone 2 on is big (4 pack rooms + the elite's den, a sluis of 3+ columns with 2+ levers, the
 *       chest halfway in its box); the Zwamgrot keeps 2 rooms; every room has spawn spots, all inside the cave's space and in front
 *       of the gate, the boss room's spots behind it; the per-column space of every cave is loaded and holds every room centre;</li>
 *   <li>{@code spawnPlekkenInTegels}: every spawn spot of every room (and the levers, the chest spot) checked against the shipped
 *       map tiles: free cell + head room on a solid floor (reachability on the tiles: tools/vadsrijk/grotten/grotcheck.py);</li>
 *   <li>{@code grootRun}: a big test cave: 3+4+4+4 pack creatures + the elite alone in the last room, the sluis shuts at the start
 *       and only opens when every lever is pulled, the chest halfway appears for the participant when room 2 is clear, all beaten
 *       -> the boss; a reset puts the levers back up and opens the sluis;</li>
 *   <li>{@code verlatenEnPopup}: closing the window after the boss keeps the run (the member stays inside, the instance stays);
 *       "Dungeon verlaten" (the inventory button's action): the member is put outside the door and out of the group, the run closes
 *       once the last member left; a non-member's leave does nothing;</li>
 *   <li>{@code partyErbij}: a party mate who walks into a running dungeon joins the group instead of being put out;</li>
 *   <li>{@code herbouwKlein}: the rebuild command on a small part of De Kristalgrot's box: the dry run counts the differences with the
 *       tile, the real run writes them (with a backup), a second dry run finds nothing, the restore brings the old blocks back.</li>
 * </ul>
 * Run: {@code runGameTestServer -Pgt=VadsrijkDungeonGroter}.
 */
public class VadsrijkDungeonGroterGameTests {
    private static final String BATCH = "vadsrijk_dungeon_groter";

    @GuhTest(template = "empty", batch = BATCH)
    public static void data(GameTestHelper helper) {
        for (Kerker k : Kerkers.alle()) {
            if (k.zone() >= 2) {
                helper.assertTrue(k.groot() && k.kamers().size() == 5, k.id() + ": 4 pack rooms + the elite's den (" + k.kamers().size() + ")");
                helper.assertTrue(k.sluis().size() >= 3 && k.sluisBord() != null, k.id() + ": a sluis");
                helper.assertTrue(k.hendels().size() >= 2, k.id() + ": levers in the sluis room");
                helper.assertTrue(k.kist() != null && k.bevat(k.kist().getX() + 0.5, k.kist().getY() + 0.1, k.kist().getZ() + 0.5), k.id() + ": the chest halfway in the cave");
                for (int[] c : k.sluis()) {
                    helper.assertFalse(k.achter(c[0] + 0.5, c[1] + 0.5, c[2] + 0.5), k.id() + ": the sluis is in front of the gate");
                }
            } else {
                helper.assertTrue(!k.groot() && k.kamers().size() == 2 && k.sluis().isEmpty(), "the Zwamgrot stays as it was");
            }
            helper.assertTrue(Kerkers.ruimte(k) != null, k.id() + ": its per-column space is loaded");
            List<Kamer> alle = new ArrayList<>(k.kamers());
            alle.add(k.baas());
            for (Kamer a : alle) {
                helper.assertTrue(a.aantalPlekken() >= 8, k.id() + " " + a.id() + ": spawn spots (" + a.aantalPlekken() + ")");
                for (int i = 0; i < a.aantalPlekken(); i++) {
                    BlockPos p = a.plek(i);
                    helper.assertTrue(k.bevat(p.getX() + 0.5, p.getY() + 0.1, p.getZ() + 0.5), k.id() + " " + a.id() + ": spot " + p.toShortString() + " in the cave");
                    boolean achter = k.achter(p.getX() + 0.5, p.getY() + 0.1, p.getZ() + 0.5);
                    if (a == k.baas()) {
                        helper.assertTrue(achter, k.id() + ": the boss room's spot " + p.toShortString() + " is behind the gate");
                    } else {
                        helper.assertFalse(achter, k.id() + " " + a.id() + ": spot " + p.toShortString() + " is in front of the gate");
                    }
                }
                helper.assertTrue(k.bevat(a.x() + 0.5, a.y() + 0.1, a.z() + 0.5), k.id() + " " + a.id() + ": its centre is in the cave's space");
            }
        }
        // the space is the cave, not its box: a spot high above the cave's ceiling inside the box is not "in" it
        Kerker kr = Kerkers.van("kristalgrot");
        helper.assertTrue(kr != null && kr.bevat(894.5, 114.1, -1110.5), "De Kristalgrot: its first room counts");
        helper.assertTrue(kr.minX() <= 860 && kr.maxZ() >= -1030 && kr.minY() <= 100 && !kr.bevat(860.5, 100.0, -1030.5),
                "De Kristalgrot: a spot inside its box but in the rock far from every room does not");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH + "_tegels", timeoutTicks = 600)
    public static void spawnPlekkenInTegels(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Map<Long, Map<Long, BlockState>> tegels = new java.util.HashMap<>();
        int n = 0;
        for (Kerker k : Kerkers.alle()) {
            List<BlockPos> plekken = new ArrayList<>();
            for (Kamer a : k.kamers()) {
                for (int i = 0; i < a.aantalPlekken(); i++) {
                    plekken.add(a.plek(i));
                }
            }
            for (int i = 0; i < k.baas().aantalPlekken(); i++) {
                plekken.add(k.baas().plek(i));
            }
            if (k.kist() != null) {
                plekken.add(k.kist());
            }
            for (BlockPos p : plekken) {
                Map<Long, BlockState> t = tegels.computeIfAbsent(((long) (p.getX() >> 6) << 32) | ((p.getZ() >> 6) & 0xFFFFFFFFL),
                        key -> GrotHerbouw.tegelBlokken(sl, p.getX() >> 6, p.getZ() >> 6, new int[1]));
                BlockState onder = t.getOrDefault(p.below().asLong(), Blocks.AIR.defaultBlockState());
                BlockState voet = t.getOrDefault(p.asLong(), Blocks.AIR.defaultBlockState());
                BlockState hoofd = t.getOrDefault(p.above().asLong(), Blocks.AIR.defaultBlockState());
                helper.assertTrue(!onder.isAir() && onder.getFluidState().isEmpty(), k.id() + ": spot " + p.toShortString() + " has a floor in the tiles (" + onder + ")");
                helper.assertTrue(voet.getCollisionShape(sl, p).isEmpty() && voet.getFluidState().isEmpty() && hoofd.getCollisionShape(sl, p).isEmpty(),
                        k.id() + ": spot " + p.toShortString() + " is free cave space in the tiles (" + voet + " / " + hoofd + ")");
                n++;
            }
            for (BlockPos h : k.hendels()) {
                Map<Long, BlockState> t = tegels.computeIfAbsent(((long) (h.getX() >> 6) << 32) | ((h.getZ() >> 6) & 0xFFFFFFFFL),
                        key -> GrotHerbouw.tegelBlokken(sl, h.getX() >> 6, h.getZ() >> 6, new int[1]));
                helper.assertTrue(t.getOrDefault(h.asLong(), Blocks.AIR.defaultBlockState()).getBlock() instanceof LeverBlock, k.id() + ": a lever at " + h.toShortString());
            }
        }
        helper.assertTrue(n > 600, "checked every spawn spot: " + n);
        helper.succeed();
    }

    /** A big test cave on the test spot: 5 rooms in a row (x 3, 9, 15, 21, 27), a 3-column sluis at x 12, 3 levers, a boss room. */
    static Kerker grootKerker(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        for (int x = -1; x <= 33; x++) {
            for (int z = -1; z <= 13; z++) {
                sl.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 0; y < 5; y++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        int X = o.getX(), Y = o.getY(), Z = o.getZ();
        List<BlockPos> hendels = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            BlockPos h = new BlockPos(X + 8 + i, Y, Z + 1);
            sl.setBlock(h, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.NORTH), 3);
            hendels.add(h);
        }
        List<Kamer> kamers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            kamers.add(new Kamer("kamer_" + (i + 1), X + 3 + i * 6, Y, Z + 4, 2));
        }
        return new Kerker("kristalgrot", 2, X - 1, Y - 2, Z - 1, X + 33, Y + 8, Z + 13, 22, 24, 24, 25, List.copyOf(kamers),
                new Kamer("baaskamer", X + 16, Y, Z + 11, 2),
                List.of(new int[] {X + 15, Y, Z + 8, Y + 4}, new int[] {X + 16, Y, Z + 8, Y + 4}, new int[] {X + 17, Y, Z + 8, Y + 4}),
                new BlockPos(X + 16, Y, Z + 8), List.of(new int[] {X + 16, Y, Z + 11, 2}),
                List.of(new int[] {X + 12, Y, Z + 3, Y + 4}, new int[] {X + 12, Y, Z + 4, Y + 4}, new int[] {X + 12, Y, Z + 5, Y + 4}),
                new BlockPos(X + 12, Y, Z + 4), List.copyOf(hendels), new BlockPos(X + 9, Y, Z + 6));
    }

    static List<VadsrijkMob> wezens(ServerLevel sl, Kerker k) {
        return VadsrijkGrotKerkerGameTests.runWezens(sl, k);
    }

    @GuhTest(template = "empty", batch = BATCH + "_run", timeoutTicks = 200)
    public static void grootRun(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Kerker k = grootKerker(helper);
        Kerkers.TEST.put(k.id(), k);
        Run r = GrotKerker.run(k.id());
        GrotKerker.reset(sl, k, r, null);
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.data(p).extra.remove(GrotKerker.DATA);
        p.teleportTo(k.kamers().get(0).x() + 0.5, k.kamers().get(0).y(), k.kamers().get(0).z() + 0.5);
        GrotKerker.TEST_KLAAR = true;
        DungeonRij.wisAlles();
        helper.runAfterDelay(5, () -> {
            try {
                DungeonRij.devToegang(p, k.id());
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(r.staat == Staat.BEZIG, "the run started: " + r.staat);
                helper.assertTrue(r.plan.size() == 3 + 4 + 4 + 4 + 1, "solo: 3 + 4 + 4 + 4 + the elite, got " + r.plan.size());
                helper.assertTrue(r.plan.get("elite").kamer() == 4 && r.plan.values().stream().filter(pl -> pl.kamer() == 4).count() == 1,
                        "the elite alone in the last room (its den)");
                helper.assertTrue(r.sluisDicht && sl.getBlockState(new BlockPos(k.sluis().get(1)[0], k.sluis().get(1)[1] + 1, k.sluis().get(1)[2])).is(Blocks.BARRIER),
                        "the sluis shut at the start");
                // two of three levers: still shut
                for (int i = 0; i < 2; i++) {
                    BlockPos h = k.hendels().get(i);
                    sl.setBlock(h, sl.getBlockState(h).setValue(LeverBlock.POWERED, true), 3);
                }
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(r.sluisDicht && !r.sluisOpen && r.hendelsGemeld == 2, "2 of 3 levers: the sluis stays shut");
                BlockPos h3 = k.hendels().get(2);
                sl.setBlock(h3, sl.getBlockState(h3).setValue(LeverBlock.POWERED, true), 3);
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(r.sluisOpen && !r.sluisDicht && sl.getBlockState(new BlockPos(k.sluis().get(1)[0], k.sluis().get(1)[1] + 1, k.sluis().get(1)[2])).isAir(),
                        "every lever pulled: the sluis opened");
                // rooms 1 + 2 beaten: the chest halfway for the participant
                for (VadsrijkMob m : wezens(sl, k)) {
                    String s = nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.sleutel(m);
                    if (s != null && (s.endsWith(":k1_0") || s.contains(":k1_") || s.contains(":k2_"))) {
                        m.verslagen(p);
                    }
                }
                long munten = Spelers.data(p).munten;
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(r.tussenGegeven, "room 2 clear: the chest halfway");
                // (the chest itself may first fall from the sky with the chest ceremony; its coins go straight to the player)
                helper.assertTrue(Spelers.data(p).munten - munten == KerkerLoot.TUSSEN_MUNTEN * 4 + 100,
                        "the chest halfway for the participant: zone 2 = 700 brons, got " + (Spelers.data(p).munten - munten));
                helper.assertTrue(r.staat == Staat.BEZIG, "rooms 3, 4 and the elite still to go");
                for (VadsrijkMob m : wezens(sl, k)) {
                    m.verslagen(p);
                }
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(r.staat == Staat.BAAS, "everything beaten: the boss: " + r.staat);
                GrotKerker.reset(sl, k, r, null);
                helper.assertTrue(!r.sluisDicht && !sl.getBlockState(h3).getValue(LeverBlock.POWERED), "a reset: the sluis open, the levers back up");
                helper.succeed();
            } finally {
                GrotKerker.reset(sl, k, r, null);
                GrotKerker.TEST_KLAAR = false;
                DungeonRij.wisAlles();
                Kerkers.TEST.remove(k.id());
                Spelers.data(p).extra.remove(GrotKerker.DATA);
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_uit", timeoutTicks = 200)
    public static void verlatenEnPopup(GameTestHelper helper) {
        Kerker k = VadsrijkDungeonGameTests.opzet(helper);
        Deur d = DungeonDeur.hoofd(k.id());
        ServerPlayer p = VadsrijkDungeonGameTests.speler(helper, d);
        ServerPlayer q = VadsrijkDungeonGameTests.speler(helper, d);
        helper.runAfterDelay(3, () -> {
            try {
                var s = helper.getLevel().getServer();
                DungeonRij.Instantie i = DungeonRij.binnen(k.id(), List.of(p, q));
                helper.assertTrue(i != null && i.leden.size() == 2 && k.bevat(p.getX(), p.getY(), p.getZ()), "two members inside");
                // the boss falls: cleared; the window after the boss is closed: nothing forces a choice
                DungeonRij.klaar(helper.getLevel(), k.id(), Set.of(p, q));
                DungeonRij.actie(p, "dicht", k.id());
                for (int t = 1; t <= 8; t++) {
                    DungeonRij.tickRij(s, VadsrijkDungeonGameTests.nu(helper) + t * 10L);
                }
                helper.assertTrue(DungeonRij.instantie(k.id()) != null && k.bevat(p.getX(), p.getY(), p.getZ()), "closing the window keeps the run (time to loot)");
                // a non-member's leave does nothing
                ServerPlayer x = VadsrijkDungeonGameTests.speler(helper, d);
                DungeonRij.verlaat(x, k.id());
                helper.assertTrue(DungeonRij.instantie(k.id()).leden.size() == 2, "a non-member's leave changes nothing");
                // "Dungeon verlaten": out by the door, out of the group; the run stays for the other one
                DungeonRij.verlaat(p, k.id());
                helper.assertTrue(p.blockPosition().equals(d.voor()) && !DungeonRij.instantie(k.id()).leden.contains(p.getUUID()), "p is outside and out of the group");
                helper.assertTrue(DungeonRij.instantie(k.id()) != null, "q is still in: the run stays");
                DungeonRij.verlaat(q, k.id());
                helper.assertTrue(DungeonRij.instantie(k.id()) == null, "the last member left: the dungeon closed");
                helper.assertTrue(DungeonRij.dungeonVan(p.getUUID()) == null && DungeonRij.dungeonVan(q.getUUID()) == null, "nobody is a member any more");
                helper.succeed();
            } finally {
                VadsrijkDungeonGameTests.opruimen(helper, k, p, q);
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_party", timeoutTicks = 200)
    public static void partyErbij(GameTestHelper helper) {
        Kerker k = VadsrijkDungeonGameTests.opzet(helper);
        Deur d = DungeonDeur.hoofd(k.id());
        ServerPlayer p = VadsrijkDungeonGameTests.speler(helper, d);
        ServerPlayer q = VadsrijkDungeonGameTests.speler(helper, d);
        helper.runAfterDelay(3, () -> {
            try {
                helper.assertTrue(DungeonRij.solo(p, k.id(), false), "p solo in");
                Partijen.nodigUit(p, q);
                Partijen.accepteer(q);
                helper.assertTrue(Partijen.zelfdeParty(p, q), "p and q are a party");
                q.teleportTo(k.kamers().get(0).x() + 0.5, k.kamers().get(0).y(), k.kamers().get(0).z() + 0.5);
                DungeonRij.uitzetten(helper.getLevel(), Dungeons.van(k.id()));
                helper.assertTrue(DungeonRij.lid(q, k.id()) && k.bevat(q.getX(), q.getY(), q.getZ()), "the party mate joined the group and stays inside");
                // someone outside the party is still put out
                ServerPlayer x = VadsrijkDungeonGameTests.speler(helper, d);
                x.teleportTo(k.kamers().get(0).x() + 0.5, k.kamers().get(0).y(), k.kamers().get(0).z() + 0.5);
                DungeonRij.uitzetten(helper.getLevel(), Dungeons.van(k.id()));
                helper.assertFalse(k.bevat(x.getX(), x.getY(), x.getZ()), "a stranger is still put outside");
                helper.succeed();
            } finally {
                Partijen.verlaat(q.getUUID());
                Partijen.verlaat(p.getUUID());
                VadsrijkDungeonGameTests.opruimen(helper, k, p, q);
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_herbouw", timeoutTicks = 600)
    public static void herbouwKlein(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Kerker k = Kerkers.van("kristalgrot");
        GrotHerbouw.Data d = GrotHerbouw.data("kristalgrot");
        helper.assertTrue(k != null && d != null, "rebuild data for De Kristalgrot");
        helper.assertTrue(GrotHerbouw.data("zwamgrot") == null, "no rebuild for the Zwamgrot");
        // a small piece round the middle of room 1 (floor 113, air above): in the test world it is all different
        BoundingBox b = new BoundingBox(892, 110, -1113, 896, 118, -1109);
        try {
            GrotHerbouw.Uitslag eerst = GrotHerbouw.plan(sl, d, b);
            helper.assertTrue(eerst.veranderd > 0, "the dry run finds differences with the tile: " + eerst.veranderd);
            helper.assertTrue(eerst.bovenAnders == 0, "nothing at/above the surface in the cave's middle");
            BlockState voor = sl.getBlockState(new BlockPos(894, 113, -1111));
            GrotHerbouw.Uitslag echt = GrotHerbouw.echt(sl, k, d, b);
            helper.assertTrue(echt.backup != null && echt.geschreven == echt.veranderd && echt.veranderd > 0, "written with a backup: " + echt.tekst(true));
            helper.assertTrue(GrotHerbouw.plan(sl, d, b).veranderd == 0, "a second dry run finds nothing left");
            helper.assertTrue(!sl.getBlockState(new BlockPos(894, 113, -1111)).isAir() || voor.isAir(), "the floor of room 1 is there");
            String f = GrotHerbouw.herstel(sl, "kristalgrot", echt.backup);
            helper.assertTrue(f == null, "the restore worked: " + f);
            helper.assertTrue(sl.getBlockState(new BlockPos(894, 113, -1111)) == voor, "the restore brought the old block back");
            helper.assertTrue(GrotHerbouw.plan(sl, d, b).veranderd == eerst.veranderd, "after the restore the dry run counts the same again");
            helper.assertTrue(GrotHerbouw.herstel(sl, "kristalgrot", "../level.dat") != null, "no files outside the backups");
            helper.succeed();
        } catch (java.io.IOException e) {
            helper.fail("backup: " + e.getMessage());
        }
    }
}
