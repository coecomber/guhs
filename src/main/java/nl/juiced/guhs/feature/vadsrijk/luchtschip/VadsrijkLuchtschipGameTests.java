package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-LUCHTSCHIP: the timetable, the paths, the data of all lines, and a real (short) test line with a ship, a passenger, the
 * locked-in flight and the landing. Run: {@code runGameTestServer -Pgt=VadsrijkLuchtschip}.
 */
public class VadsrijkLuchtschipGameTests {
    private static final String BATCH = "vadsrijk_luchtschip";

    /** Every line: 60-90 s, both directions start and end at the docks, smooth (no jumps), never below the sea; departures every 3 min. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dienstregeling(GameTestHelper helper) {
        helper.assertTrue(Lijnen.ALLE.stream().filter(l -> !l.test()).count() == 5, "five lines: 0, A, B, C, D");
        for (Lijnen.Lijn l : Lijnen.ALLE) {
            if (l.test()) {
                continue;
            }
            int d = Dienstregeling.duur(l);
            helper.assertTrue(d >= Lijnen.MIN_DUUR && d <= Lijnen.MAX_DUUR, l.letter() + ": flight " + d + " ticks");
            helper.assertTrue(l.a().id().startsWith("haven_" + l.a().zone() + "_") && l.a().id().endsWith("_vertrek")
                    && l.b().id().equals("haven_" + l.b().zone() + "_" + l.a().zone() + "_vertrek"), l.letter() + ": POI ids " + l.a().id() + " / " + l.b().id());
            for (boolean heen : new boolean[] {true, false}) {
                Dienstregeling.Pad pad = Dienstregeling.pad(l, heen);
                Vec3 van = heen ? l.a().dok() : l.b().dok(), naar = heen ? l.b().dok() : l.a().dok();
                helper.assertTrue(pad.op(0).distanceTo(van) < 0.5 && pad.op(1).distanceTo(naar) < 0.5, l.letter() + ": path ends at the docks");
                helper.assertTrue(pad.lengte() > 300, l.letter() + ": a real journey (" + pad.lengte() + ")");
            }
            // sweep two periods: speed, height, and exactly one departure per period from each harbour
            Vec3 vorig = null;
            int vertrekA = 0;
            for (int t = 0; t < 2 * l.periode(); t++) {
                Dienstregeling.Stand s = Dienstregeling.stand(l, 0, l.fase() + t);
                if (vorig != null) {
                    helper.assertTrue(s.pos().distanceTo(vorig) < 0.75, l.letter() + ": jump of " + s.pos().distanceTo(vorig) + " at " + t);
                }
                helper.assertTrue(s.pos().y > 60, l.letter() + ": too low at " + t + ": " + s.pos());
                vorig = s.pos();
                if (Dienstregeling.totVertrek(l, l.fase() + t) == 0) {
                    vertrekA++;
                }
                Dienstregeling.Stand s1 = Dienstregeling.stand(l, 1, l.fase() + t);
                helper.assertTrue(s.fase() != s1.fase(), l.letter() + ": both ships in the same phase at " + t);
            }
            helper.assertTrue(vertrekA == 2, l.letter() + ": departures in two periods: " + vertrekA);
            helper.assertTrue(Dienstregeling.stand(l, 0, l.fase()).fase() == Dienstregeling.Fase.VLIEGT_AB
                    && Dienstregeling.stand(l, 1, l.fase()).fase() == Dienstregeling.Fase.VLIEGT_BA, l.letter() + ": both leave at the same moment");
            helper.assertTrue(Dienstregeling.schipAan(l, l.a(), l.fase() - 10) == 0 && Dienstregeling.schipAan(l, l.b(), l.fase() - 10) == 1,
                    l.letter() + ": a ship waits at each pier before the departure");
        }
        helper.assertTrue(Luchtschepen.klok(20 * 83).equals("1:23") && Luchtschepen.klok(0).equals("0:00"), "m:ss");
        helper.succeed();
    }

    /** A test line in the test area: board, depart (locked in, no damage, no sneaking off), land on the other pier. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void vlucht(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                sl.setBlock(o.offset(x, 2, z), Blocks.STONE.defaultBlockState(), 3);
                sl.setBlock(o.offset(x + 30, 2, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        Lijnen.Haven a = new Lijnen.Haven("haven_t1_t2_vertrek", o.above(3), 270f, "t1");
        Lijnen.Haven b = new Lijnen.Haven("haven_t2_t1_vertrek", o.offset(30, 3, 0), 90f, "t2");
        int periode = 200, duur = 60;
        int fase = (int) ((sl.getGameTime() + 30) % periode);
        Lijnen.Lijn l = new Lijnen.Lijn("test_" + sl.getGameTime(), "T", a, b, List.of(new Vec3(o.getX() + 15.5, o.getY() + 8, o.getZ() + 0.5)),
                fase, duur, periode, true);
        Lijnen.ALLE.add(l);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.snapTo(a.plek().getX() + 0.5, a.plek().getY(), a.plek().getZ() + 0.5);
        int k = Dienstregeling.schipAan(l, a, Tijd.nu(sl, l));
        helper.assertTrue(k == 0, "ship 0 waits at a: " + k);
        LuchtschipEntity e = Luchtschepen.schipNu(sl, l, 0);
        helper.assertTrue(e != null, "ship spawned");
        helper.assertTrue(Luchtschepen.instappen(p, e), "boarded");
        helper.assertTrue(p.getVehicle() == e, "on board");
        long start = sl.getGameTime();
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(e.vliegt() && p.getVehicle() == e, "flying with the passenger");
            helper.assertTrue(p.getPersistentData().getStringOr(Luchtschepen.TAG_NAAR, "").equals(b.id()), "destination remembered");
            p.setShiftKeyDown(true);
            p.stopRiding();
            p.setShiftKeyDown(false);
            helper.assertTrue(p.getVehicle() == e, "no sneaking off in flight");
            float hp = p.getHealth();
            p.hurtServer(sl, sl.damageSources().generic(), 4f);
            helper.assertTrue(p.getHealth() == hp, "no damage on board");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(sl.getGameTime() - start > 60, "not yet");
            helper.assertTrue(p.getVehicle() == null, "landed: off the ship");
            helper.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(b.plek())) < 4, "on the pier of b: " + p.position());
            helper.assertTrue(p.getPersistentData().getStringOr(Luchtschepen.TAG_NAAR, "").isEmpty(), "destination cleared");
            Luchtschepen.ruimOp(sl, l);
            Lijnen.ALLE.remove(l);
        });
    }

    /** Logout in flight: you are put on the destination pier (and the ship goes on without you). */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void uitloggenInVlucht(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                sl.setBlock(o.offset(x + 30, 2, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        Lijnen.Haven a = new Lijnen.Haven("haven_t3_t4_vertrek", o.above(3), 270f, "t3");
        Lijnen.Haven b = new Lijnen.Haven("haven_t4_t3_vertrek", o.offset(30, 3, 0), 90f, "t4");
        int fase = (int) ((sl.getGameTime() + 10) % 400);
        Lijnen.Lijn l = new Lijnen.Lijn("test_uit_" + sl.getGameTime(), "U", a, b, List.of(new Vec3(o.getX() + 15.5, o.getY() + 8, o.getZ() + 0.5)),
                fase, 200, 400, true);
        Lijnen.ALLE.add(l);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        LuchtschipEntity e = Luchtschepen.schipNu(sl, l, 0);
        helper.assertTrue(e != null && Luchtschepen.instappen(p, e), "boarded");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(e.vliegt(), "in flight");
            Luchtschepen.opUitloggen(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            helper.assertTrue(p.getVehicle() == null, "off the ship");
            helper.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(b.plek())) < 4, "put on the destination pier: " + p.position());
            helper.assertTrue(e.isAlive(), "the ship flies on");
            Luchtschepen.ruimOp(sl, l);
            Lijnen.ALLE.remove(l);
            helper.succeed();
        });
    }

    /** The luxury travel map: the first flight to a place is free, then it costs the price (or is refused). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void luxeKaartje(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        String plek = "testplek_" + helper.getLevel().getGameTime();
        helper.assertTrue(Luchtschepen.luxe(p, plek, false), "first time free");
        helper.assertTrue(!Luchtschepen.luxe(p, plek, false), "second time: no money, no flight");
        helper.assertTrue(Luchtschepen.luxe(p, plek, true), "tests fly free");
        helper.succeed();
    }

    /** The view distance of the dimension: configured 20 by default, never below the server's own. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zichtafstand(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        int basis = server.getPlayerList().getViewDistance();
        helper.assertTrue(Zicht.STANDAARD == 20, "default 20");
        int z = Zicht.effectief(server);
        helper.assertTrue(z >= basis && z <= 32, "effective " + z + " (server " + basis + ")");
        helper.assertTrue(Zicht.ingesteld() <= 0 || z == Math.max(basis, Zicht.ingesteld()), "uses the configured value");
        helper.succeed();
    }
}
