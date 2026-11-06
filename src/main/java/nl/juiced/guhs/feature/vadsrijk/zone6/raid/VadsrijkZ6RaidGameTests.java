package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-Z6RAID: the raid's rules. Run: {@code runGameTestServer -Pgt=VadsrijkZ6Raid}. Each test its own batch.
 * <ul>
 *   <li>schaal: the scaling math (HP x (0.55 + 0.45 n), mechanics ceil(n/2) min 2) and that a real boss uses it;</li>
 *   <li>verdiepingEnWipe: a cheered-up boss opens its floor's gate and moves the group up; a wipe counts and keeps the floor;
 *       a raid start closes every gate again (reset);</li>
 *   <li>lootPerClear: every clear gives a chest (twice in a row, no week limit), the first one with the guarantees;</li>
 *   <li>zandloper: two players on the two plates at once turn the Zandwachter over (kwetsbaar, back x1.8);</li>
 *   <li>spiegels: two mirrors on the real image unmask the Spiegelvizier; a fake one makes a clone;</li>
 *   <li>sterrenbeelden: all three out inside the window = cheered up together; only one = they light each other again.</li>
 * </ul>
 */
public class VadsrijkZ6RaidGameTests {
    private static final String BATCH = "vadsrijk_z6raid";

    private static ServerPlayer speler(GameTestHelper helper, Vec3 waar) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.CREATIVE);
        p.snapTo(waar.x, waar.y, waar.z);
        Spelers.data(p).vads = Spelers.maxVads(p);
        return p;
    }

    private static void vloer(ServerLevel sl, BlockPos c, int r) {
        for (int cx = (c.getX() - r - 8) >> 4; cx <= (c.getX() + r + 8) >> 4; cx++) {
            for (int cz = (c.getZ() - r - 8) >> 4; cz <= (c.getZ() + r + 8) >> 4; cz++) {
                sl.setChunkForced(cx, cz, true);
            }
        }
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                sl.setBlock(c.offset(x, -1, z), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 2);
                for (int y = 0; y < 4; y++) {
                    sl.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static <T extends Z6RaidBaas> T zet(ServerLevel sl, EntityType<T> type, Vec3 c, int n) {
        T b = type.create(sl, EntitySpawnReason.COMMAND);
        b.testN = n;
        b.snapTo(c.x, c.y, c.z, 0, 0);
        b.stelIn(Z6RaidBaas.LEVEL, Element.ROZE, false, Set.of());
        b.zetThuis(c);
        b.schaal(0);
        sl.addFreshEntity(b);
        return b;
    }

    @GuhTest(template = "empty", batch = BATCH + "_schaal")
    public static void schaal(GameTestHelper helper) {
        helper.assertTrue(Math.abs(Z6Schaal.factor(2) - 1.45) < 1e-9 && Math.abs(Z6Schaal.factor(10) - 5.05) < 1e-9, "HP factor 2 -> 1.45, 10 -> 5.05");
        helper.assertTrue(Z6Schaal.factor(1) == Z6Schaal.factor(2) && Z6Schaal.factor(14) == Z6Schaal.factor(10), "clamped to 2..10");
        int[] verwacht = {2, 2, 2, 2, 3, 3, 4, 4, 5};
        for (int n = 2; n <= 10; n++) {
            helper.assertTrue(Z6Schaal.mech(n) == verwacht[n - 2], "mechanics for " + n + " = " + verwacht[n - 2] + ", got " + Z6Schaal.mech(n));
        }
        helper.assertTrue(Z6Schaal.schadeFactor(10) < 1.2 && Z6Schaal.schadeFactor(2) < 1.0, "a hit barely scales");
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 2, 1));
        vloer(sl, c, 4);
        Zandwachter twee = zet(sl, Z6RaidRegistry.ZANDWACHTER.get(), Vec3.atBottomCenterOf(c), 2);
        Zandwachter tien = zet(sl, Z6RaidRegistry.ZANDWACHTER.get(), Vec3.atBottomCenterOf(c).add(3, 0, 0), 10);
        double ratio = tien.maxVads() / twee.maxVads();
        helper.assertTrue(Math.abs(ratio - 5.05 / 1.45) < 0.01, "10 players vs 2: x" + (5.05 / 1.45) + ", got x" + ratio);
        helper.assertTrue(Math.abs(twee.maxVads() - 30000 * 1.45) < 1, "2 players: 30000 x 1.45, got " + twee.maxVads());
        helper.assertTrue(twee.plaatNodig() == 1 && tien.plaatNodig() == 2, "plate count 1 (2p) / 2 (10p)");
        twee.discard();
        tien.discard();
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH + "_reset")
    public static void verdiepingEnWipe(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos oud = Z6RaidPlekken.testAnker;
        Z6RaidPlekken.testAnker = a;
        try {
            for (int k = 0; k <= 3; k++) {
                for (BlockPos b : Z6RaidPlekken.poortBlokken(k)) {
                    sl.getChunk(b);
                    sl.setBlock(b, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            Z6Raid.wisAlles();
            Z6Raid.Groep g = Z6Raid.testGroep(3);
            helper.assertTrue(g.n == 3 && Z6Raid.n() == 3 && Z6Raid.magSpawnen(1) && !Z6Raid.magSpawnen(2), "group of 3 on floor 1: only boss 1 may spawn");
            Z6Raid.resetVerdiepingen(sl);
            for (int k = 0; k <= 3; k++) {
                helper.assertTrue(!Z6RaidBouw.poortOpen(sl, k), "reset: gate " + k + " closed");
            }
            Zandwachter z = zet(sl, Z6RaidRegistry.ZANDWACHTER.get(), Vec3.atBottomCenterOf(a), 3);
            Z6Raid.baasVerslagen(sl, z);
            helper.assertTrue(g.verdieping == 2 && Z6RaidBouw.poortOpen(sl, 1) && !Z6RaidBouw.poortOpen(sl, 2) && !Z6RaidBouw.poortOpen(sl, 0),
                    "boss 1 cheered up: gate 1 open, group on floor 2");
            helper.assertTrue(Z6Raid.magSpawnen(2) && !Z6Raid.magSpawnen(1), "now boss 2 may spawn, boss 1 not again");
            g.gevechtStart = 100;
            Z6Raid.wipe(sl, g);
            Z6Raid.wipe(sl, g);
            helper.assertTrue(g.wipes == 2 && g.verdieping == 2 && g.gevechtStart == -1, "two wipes counted, still floor 2, the fight timer reset");
            helper.assertTrue(Arenas.van(Z6Raid.arenaId(2)) != null && Arenas.van(Z6Raid.arenaId(4)).straal() == Z6RaidPlekken.STRAAL, "the 4 arenas exist");
            Z6Raid.resetVerdiepingen(sl);
            helper.assertTrue(!Z6RaidBouw.poortOpen(sl, 1), "a new start closes gate 1 again");
            z.discard();
        } finally {
            Z6Raid.wisAlles();
            Z6RaidPlekken.testAnker = oud;
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH + "_loot")
    public static void lootPerClear(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Z6Raid.Groep g = Z6Raid.testGroep(2);
        try {
            LootTabel.Resultaat eerste = Z6RaidLoot.clearKist(p, g, p.getRandom());
            LootTabel.Resultaat tweede = Z6RaidLoot.clearKist(p, g, p.getRandom());
            LootTabel.Resultaat derde = Z6RaidLoot.clearKist(p, g, p.getRandom());
            for (LootTabel.Resultaat r : List.of(eerste, tweede, derde)) {
                helper.assertTrue(r.munten() >= 2500 && r.items().stream().anyMatch(s -> {
                    Uitrusting u = Rollen.van(s);
                    return u != null && u.type().startsWith("z6raid_sterrenzijde") && u.gebonden();
                }), "every clear: coins + a bound Sterrenzijde piece");
            }
            helper.assertTrue(eerste.items().stream().anyMatch(s -> s.is(Z6RaidLoot.REPLICA.get())), "first clear: the ring replica");
            helper.assertTrue(eerste.items().stream().anyMatch(s -> {
                Uitrusting u = Rollen.van(s);
                return u != null && u.type().equals("z6raid_ringbreker");
            }), "first clear: a raid weapon for your class (Ridder: Ringbreker)");
            helper.assertTrue(Spelers.data(p).extra("z6raid").getIntOr("clears", 0) == 3, "3 clears counted, no week limit");
            Z6RaidLoot.titels(p, g);
            helper.assertTrue(Z6RaidLoot.heeftTitel(p, "ringbreker") && Z6RaidLoot.heeftTitel(p, "sterrenaansteker"), "titles after a clear without fainting");
            ServerPlayer q = GuhMockPlayer.of(helper);
            g.flauw.add(q.getUUID());
            Z6RaidLoot.titels(q, g);
            helper.assertTrue(Z6RaidLoot.heeftTitel(q, "ringbreker") && !Z6RaidLoot.heeftTitel(q, "sterrenaansteker"), "fainted once: no Sterrenaansteker");
            int voor = Z6Raid.clearsBeloond, haak = Z6RaidHaken.geroepen;
            Z6Raid.klaar(helper.getLevel(), g);
            helper.assertTrue(Z6Raid.clearsBeloond == voor + 1 && g.klaar(), "the clear is counted");
            helper.assertTrue(Z6RaidHaken.geroepen == haak, "nobody of this test group is in the tower: no hook");
            LootTabel.Resultaat baas = Z6RaidLoot.baasKist("zandwachter", p, p.getRandom());
            helper.assertTrue(baas.munten() >= 700, "bosses 1-3 give a small chest");
        } finally {
            Z6Raid.wisAlles();
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH + "_zandloper", timeoutTicks = 200)
    public static void zandloper(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 2, 1));
        vloer(sl, c, 16);
        Zandwachter z = zet(sl, Z6RaidRegistry.ZANDWACHTER.get(), Vec3.atBottomCenterOf(c), 2);
        ServerPlayer a = speler(helper, z.plaat(0)), b = speler(helper, z.plaat(1));
        z.testFilter = q -> q == a || q == b;
        double v0 = z.vads();
        z.krijgSchade(1000, a);
        double zonder = v0 - z.vads();
        helper.assertTrue(Math.abs(zonder - 500) < 1, "zandhuid: x0.5 before the turn, got " + zonder);
        z.startZandloper(sl);
        helper.runAfterDelay(60, () -> {
            a.snapTo(z.plaat(0).x, z.plaat(0).y, z.plaat(0).z);
            b.snapTo(z.plaat(1).x, z.plaat(1).y, z.plaat(1).z);
            helper.assertTrue(z.omgedraaid == 1 && z.kwetsbaar() && !z.loopt(), "both plates held: turned over (" + z.opPlaat(0) + "/" + z.opPlaat(1) + ")");
            double v = z.vads();
            z.krijgSchade(1000, null);
            helper.assertTrue(Math.abs(v - z.vads() - 1000) < 1, "kwetsbaar: full damage, got " + (v - z.vads()));
            z.discard();
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_spiegel", timeoutTicks = 200)
    public static void spiegels(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 2, 1));
        vloer(sl, c, 18);
        Spiegelvizier s = zet(sl, Z6RaidRegistry.SPIEGELVIZIER.get(), Vec3.atBottomCenterOf(c), 4);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(3, 0, 3));
        s.testFilter = q -> q == p;
        s.verstop(sl);
        helper.assertTrue(s.verstopt() && s.plekken().size() == 3 && s.spiegels().size() == 2, "4 players: 3 images, 2 mirrors");
        double v = s.vads();
        s.krijgSchade(1000, p);
        helper.assertTrue(s.vads() == v, "hidden = immune");
        int echt = s.echtePlek();
        helper.assertTrue(echt >= 0, "the real one stands on an image spot");
        int nep = (echt + 1) % s.plekken().size();
        for (Z3Doel d : s.spiegels()) {
            while (Math.floorMod(d.stand(), s.plekken().size()) != nep) {
                s.doelGebruikt(d, p);
            }
        }
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(s.fouten >= 1 && s.klonen() >= 1, "two beams on a fake image: it breaks into a clone");
            for (Z3Doel d : s.spiegels()) {
                while (Math.floorMod(d.stand(), s.plekken().size()) != s.echtePlek()) {
                    s.doelGebruikt(d, p);
                }
            }
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(s.ontmaskerd == 1 && !s.verstopt() && s.kwetsbaar(), "two beams on the real one: unmasked");
                s.discard();
                helper.succeed();
            });
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_sterren", timeoutTicks = 400)
    public static void sterrenbeelden(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 2, 1));
        vloer(sl, c, 16);
        Sterrenbeeld k = zet(sl, Z6RaidRegistry.STER_KNUFFEL.get(), Vec3.atBottomCenterOf(c), 2);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(4, 0, 0));
        k.testFilter = q -> q == p;
        helper.runAfterDelay(20, () -> {
            List<Sterrenbeeld> alle = k.alle();
            helper.assertTrue(alle.size() == 3, "the Knuffel brought the Bultguh and the Pluis, got " + alle.size());
            Sterrenbeeld pluis = alle.stream().filter(s -> s.soort() == Sterrenbeeld.PLUIS).findFirst().orElseThrow();
            pluis.krijgSchade(pluis.vads() + 10, p);
            helper.assertTrue(pluis.uit() && !pluis.geplofd() && k.venster() >= 0, "the Pluis is out (not beaten), the window runs");
            helper.runAfterDelay(Sterrenbeeld.VENSTER + 10, () -> {
                helper.assertTrue(k.aangestoken == 1 && !pluis.uit() && pluis.vads() > pluis.maxVads() * 0.3, "too late: they lit each other again (40 %)");
                for (Sterrenbeeld s : k.alle()) {
                    s.krijgSchade(s.vads() + 10, p);
                }
                helper.assertTrue(k.geplofd(), "all three out in the window: cheered up together");
                helper.succeed();
            });
        });
    }
}
