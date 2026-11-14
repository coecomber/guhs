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
        int[] verwacht = {2, 2, 2, 3, 3, 4, 4, 5, 5};
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
        double comp = nl.juiced.guhs.feature.vadsrijk.kern.mob.MobBalans.compensatie(twee.mobLevel());   // 1101-balans: boss HP x the main-stat compensation
        helper.assertTrue(Math.abs(twee.maxVads() - 30000 * 1.45 * comp) < 1, "2 players: 30000 x 1.45 x " + comp + ", got " + twee.maxVads());
        helper.assertTrue(twee.plaatNodig() == 1 && tien.plaatNodig() == 2, "plate count 1 (2p) / 2 (10p)");
        twee.discard();
        tien.discard();
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH + "_reset")
    public static void verdiepingEnWipe(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(1, 30, 1));   // (floor 0 sits 12 below the anchor)
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

    // ---- X-RAID: the door, the Wachtzaal, the start circle ----------------------------------------------------------------------

    /**
     * X-RAID: the WHOLE door niche (9 wide) sends you into De Wachtzaal (the old box was 4 wide: the user walked past it at the
     * side into the hollow tower); the exit puts you on the stair top OUTSIDE the box, and standing there never bounces you back
     * in (no teleport loop); walking back into the niche works again.
     */
    @GuhTest(template = "empty", batch = BATCH + "_deur")
    public static void deurNaarWachtzaal(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        // (far from the test grid: the Wachtzaal floor and the door floor must not spill into a neighbouring test)
        BlockPos a = helper.absolutePos(new BlockPos(1, 30, 1)).offset(0, 0, 4000);
        BlockPos d = helper.absolutePos(new BlockPos(40, 2, 1)).offset(0, 0, 4000);
        BlockPos oudA = Z6RaidPlekken.testAnker, oudP = Z6RaidPlekken.testPoort;
        Z6RaidPlekken.testAnker = a;
        Z6RaidPlekken.testPoort = d;
        try {
            vloer(sl, Z6RaidPlekken.midden(0), 22);
            vloer(sl, d, 8);
            for (int dx = -1; dx <= 3; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    helper.assertTrue(Z6RaidPlekken.bijPoort(d.getX() + dx + 0.5, d.getY(), d.getZ() + dz + 0.5), "niche cell " + dx + "," + dz + " is the door");
                }
            }
            BlockPos uit = Z6RaidPlekken.buiten();
            helper.assertTrue(!Z6RaidPlekken.bijPoort(uit.getX() + 0.5, uit.getY(), uit.getZ() + 0.5), "the outside spot is not in the door box");
            // in at the SIDE of the niche (where the old 4-wide box missed you)
            ServerPlayer p = speler(helper, new Vec3(d.getX() + 2.5, d.getY(), d.getZ() + 4.5));
            helper.assertTrue(Z6Raid.deur(sl, p) == 1, "the side of the niche takes you in");
            helper.assertTrue(Z6RaidPlekken.verdieping(p.getX(), p.getY(), p.getZ()) == 0, "you are in De Wachtzaal, got " + p.position());
            helper.assertTrue(Z6Raid.deur(sl, p) == 0, "standing in the Wachtzaal: nothing happens");
            // out through the exit arch
            BlockPos u = Z6RaidPlekken.uitgang();
            p.snapTo(u.getX() + 0.5, u.getY(), u.getZ() + 0.5);
            helper.assertTrue(Z6Raid.deur(sl, p) == -1, "the exit arch takes you out");
            helper.assertTrue(Z6RaidPlekken.verdieping(p.getX(), p.getY(), p.getZ()) == -1 && !Z6RaidPlekken.bijPoort(p.getX(), p.getY(), p.getZ()),
                    "outside, on the stair top in front of the door, got " + p.position());
            for (int i = 0; i < 5; i++) {
                helper.assertTrue(Z6Raid.deur(sl, p) == 0, "no teleport loop: standing outside does nothing");
            }
            // and back in
            p.snapTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5);
            helper.assertTrue(Z6Raid.deur(sl, p) == 1 && Z6RaidPlekken.verdieping(p.getX(), p.getY(), p.getZ()) == 0, "the door works again");
            helper.succeed();
        } finally {
            Z6RaidPlekken.testAnker = oudA;
            Z6RaidPlekken.testPoort = oudP;
        }
    }

    /**
     * X-RAID: the start flow. Alone on the circle nothing starts (never solo); a second real player on it for 3 s starts the raid
     * for both; the tracker line points at the circle in the Wachtzaal and at the door outside.
     */
    @GuhTest(template = "empty", batch = BATCH + "_cirkel")
    public static void startCirkel(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(1, 30, 1)).offset(0, 0, 4300);   // (far from the test grid)
        BlockPos oudA = Z6RaidPlekken.testAnker;
        Z6RaidPlekken.testAnker = a;
        Z6Raid.Groep oudG = Z6Raid.groep;   // (shared static state: restored, never wiped, so parallel raid tests keep theirs)
        java.util.List<java.util.List<java.util.UUID>> oudRij = new java.util.ArrayList<>(Z6Raid.RIJ);
        Z6Raid.groep = null;
        Z6Raid.RIJ.clear();
        try {
            vloer(sl, Z6RaidPlekken.midden(0), 22);
            for (int k = 0; k <= 3; k++) {
                for (BlockPos b : Z6RaidPlekken.poortBlokken(k)) {
                    sl.getChunk(b);
                }
            }
            Vec3 c = Z6RaidPlekken.start();
            ServerPlayer een = speler(helper, c);
            long t = 100000;
            for (int i = 0; i <= 8; i++) {
                Z6Raid.cirkel(sl, t + i * 10);
            }
            helper.assertTrue(Z6Raid.groep() == null, "one player on the circle never starts the raid");
            ServerPlayer twee = speler(helper, c.add(1, 0, 0));
            long t2 = t + 200;
            for (int i = 0; i <= Z6Raid.START_TICKS / 10 + 1; i++) {
                Z6Raid.cirkel(sl, t2 + i * 10);
            }
            Z6Raid.Groep g = Z6Raid.groep();
            helper.assertTrue(g != null && g.leden.contains(een.getUUID()) && g.leden.contains(twee.getUUID()) && g.n == 2 && g.nep.isEmpty(),
                    "two real players for 3 s: the raid starts for both (no made-up members)");
            helper.assertTrue(Z6RaidPlekken.verdieping(een.getX(), een.getY(), een.getZ()) == 1, "the group went up to floor 1, got " + een.position());
            helper.assertTrue(Z6RaidHeraut.volgRegel(true).doel().orElseThrow().equals(BlockPos.containing(Z6RaidPlekken.start()))
                    && Z6RaidHeraut.volgRegel(false).doel().orElseThrow().equals(Z6RaidPlekken.poort()), "tracker: circle inside, door outside");
            helper.succeed();
        } finally {
            Z6RaidPlekken.testAnker = oudA;
            Z6Raid.groep = oudG;
            Z6Raid.RIJ.clear();
            Z6Raid.RIJ.addAll(oudRij);
        }
    }

    /**
     * X-RAID: no softlock in De Wachtzaal of the REAL template: from where the door drops you, you can walk to the start circle,
     * to the hall keeper and to the exit arch (2 air above a floor, 4 directions, a step down of at most 1).
     */
    @GuhTest(template = "empty", batch = BATCH + "_zaal", timeoutTicks = 400)
    public static void wachtzaalBereikbaar(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(1, 30, 1)).offset(0, 0, 4600);   // (the 60x67x51 template: far from the test grid)
        BlockPos oudA = Z6RaidPlekken.testAnker;
        Z6RaidPlekken.testAnker = a;
        try {
            helper.assertTrue(Z6RaidBouw.zetTemplate(sl), "the binnentoren template exists");
            BlockPos m = Z6RaidPlekken.midden(0);
            BlockPos van = m.offset(8, 0, 0);
            java.util.Set<BlockPos> gezien = new java.util.HashSet<>();
            java.util.ArrayDeque<BlockPos> rij = new java.util.ArrayDeque<>();
            helper.assertTrue(staan(sl, van), "the arrival spot is standable");
            gezien.add(van);
            rij.add(van);
            while (!rij.isEmpty()) {
                BlockPos b = rij.poll();
                for (var r : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    for (int dy = 0; dy >= -1; dy--) {
                        BlockPos n = b.relative(r).offset(0, dy, 0);
                        if (Math.abs(n.getX() - m.getX()) > 26 || Math.abs(n.getZ() - m.getZ()) > 26 || gezien.contains(n) || !staan(sl, n)) {
                            continue;
                        }
                        gezien.add(n);
                        rij.add(n);
                        break;
                    }
                }
            }
            BlockPos cirkel = BlockPos.containing(Z6RaidPlekken.start());
            BlockPos uit = Z6RaidPlekken.uitgang();
            helper.assertTrue(gezien.contains(cirkel), "the start circle is reachable from the door's arrival spot (" + gezien.size() + " cells)");
            helper.assertTrue(gezien.stream().anyMatch(b -> b.closerThan(uit, 1.6)), "the exit arch is reachable (no softlock)");
            helper.assertTrue(gezien.stream().anyMatch(b -> b.closerThan(Z6RaidHeraut.zaalwachter(), 1.5)), "the hall keeper stands on a walkable spot");
            helper.succeed();
        } finally {
            Z6RaidPlekken.testAnker = oudA;
        }
    }

    /** A cell a player can stand in: something to stand on below, nothing to bump into in the 2 cells of the body. */
    private static boolean staan(ServerLevel sl, BlockPos b) {
        return !sl.getBlockState(b.below()).getCollisionShape(sl, b.below()).isEmpty()
                && sl.getBlockState(b).getCollisionShape(sl, b).isEmpty() && sl.getBlockState(b.above()).getCollisionShape(sl, b.above()).isEmpty();
    }
}
