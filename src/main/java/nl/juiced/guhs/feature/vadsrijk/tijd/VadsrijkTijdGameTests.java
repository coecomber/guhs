package nl.juiced.guhs.feature.vadsrijk.tijd;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.clock.PackedClockStates;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.timeline.Timeline;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.ritme.Rooster;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.world.GuhTime;

/**
 * 1.10.1 (p1101-tijd): the Vadsrijk's own day/night. Every test does its clock work inside one call (no waiting), so the
 * tests of the batch never see each other's clock, and puts both clocks + the config back. Run:
 * {@code runGameTestServer -Pgt=VadsrijkTijd}.
 */
public class VadsrijkTijdGameTests {
    private static final String B = "vadsrijk_tijd";

    /** The mapping: day share and cycle length in real ticks, the vanilla default, the clamps, the clock text. */
    @GuhTest(template = "empty", batch = B)
    public static void tijdMapping(GameTestHelper helper) {
        double[][] gevallen = {{20, 0.667}, {20, 0.5}, {30, 0.75}, {12.5, 0.3}, {1, 0.9}, {240, 0.6}};
        for (double[] g : gevallen) {
            double c = g[0], d = g[1];
            double dag = VadsrijkTijd.dagTicks(c, d), nacht = VadsrijkTijd.nachtTicks(c, d);
            helper.assertTrue(Math.abs(dag + nacht - c * 1200) < 1e-6, "cycle " + c + " min = " + c * 1200 + " ticks: " + (dag + nacht));
            helper.assertTrue(Math.abs(dag / (dag + nacht) - d) < 1e-9, "day share " + d + ": " + dag / (dag + nacht));
            // the controller as it runs: one game tick at a time at the rate of the half the clock is in
            double klok = 0;
            int ticks = 0, dagEinde = -1;
            while (klok < VadsrijkTijd.DAG && ticks < 400_000) {
                klok += (float) VadsrijkTijd.tempo((long) klok, c, d);
                ticks++;
                if (dagEinde < 0 && klok >= VadsrijkTijd.DAG_EINDE) {
                    dagEinde = ticks;
                }
            }
            helper.assertTrue(Math.abs(dagEinde - dag) <= 2, c + "/" + d + ": day half took " + dagEinde + " ticks, expected " + dag);
            helper.assertTrue(Math.abs(ticks - c * 1200) <= 3, c + "/" + d + ": whole cycle took " + ticks + " ticks, expected " + c * 1200);
        }
        helper.assertTrue(VadsrijkTijd.tempo(0, 20, 0.5) == 1.0 && VadsrijkTijd.tempo(18000, 20, 0.5) == 1.0, "20 min, half/half = vanilla speed");
        helper.assertTrue(Math.abs(VadsrijkTijd.tempo(6000, 20, 0.667) - 0.7496) < 1e-3 && Math.abs(VadsrijkTijd.tempo(18000, 20, 0.667) - 1.5015) < 1e-3,
                "default: day slower, night faster");
        helper.assertTrue(VadsrijkTijd.dagHelft(0) && VadsrijkTijd.dagHelft(11999) && !VadsrijkTijd.dagHelft(12000) && !VadsrijkTijd.dagHelft(23999)
                && VadsrijkTijd.dagHelft(24000 * 7 + 5), "the day half is 0..11999 of every day");
        helper.assertTrue(TijdConfig.klemDag(0.1) == TijdConfig.MIN_DAG && TijdConfig.klemDag(1.5) == TijdConfig.MAX_DAG
                && TijdConfig.klemCyclus(0) == TijdConfig.MIN_CYCLUS && TijdConfig.klemCyclus(9999) == TijdConfig.MAX_CYCLUS, "clamped");
        helper.assertTrue("06:00".equals(VadsrijkTijd.uur(0)) && "12:00".equals(VadsrijkTijd.uur(6000)) && "00:00".equals(VadsrijkTijd.uur(18000))
                && "19:00".equals(VadsrijkTijd.uur(13000)), "clock text");
        helper.assertTrue("20".equals(TijdConfig.tekst(20)) && "0.667".equals(TijdConfig.tekst(0.667)) && "12.5".equals(TijdConfig.tekst(12.5)), "number text");
        helper.succeed();
    }

    /** The Vadsrijk dimension type (the gametest server has no Vadsrijk level, so tests read its type from the registry). */
    static DimensionType type(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE)
                .getOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath("guhs", "vadsrijk"))).value();
    }

    /** Time of day of the Vadsrijk, the way GuhTime reads it in a Vadsrijk level. */
    static long tijdVanDeDag(MinecraftServer server) {
        return Math.floorMod(server.clockManager().getTotalTicks(GuhTime.klok(type(server), server.registryAccess())), GuhTime.DAY);
    }

    private static int cache = 1;

    /** The sky light of the Vadsrijk's timelines now (Level#updateSkyBrightness: skyDarken = 15 - this; dark from skyDarken 4). */
    static float hemellicht(MinecraftServer server, DimensionType type) {
        float v = EnvironmentAttributes.SKY_LIGHT_LEVEL.defaultValue();
        for (Holder<Timeline> t : type.timelines()) {
            if (t.value().attributes().contains(EnvironmentAttributes.SKY_LIGHT_LEVEL)) {
                v = t.value().createTrackSampler(EnvironmentAttributes.SKY_LIGHT_LEVEL, server.clockManager()).applyTimeBased(v, cache++);
            }
        }
        return v;
    }

    static boolean donker(MinecraftServer server, DimensionType type) {
        return (int) (15.0F - hemellicht(server, type)) >= 4;   // (Level#isDarkOutside: !isBrightOutside = skyDarken >= 4)
    }

    /** /time set in the overworld leaves the Vadsrijk alone, and /time set of the Vadsrijk leaves the overworld alone. */
    @GuhTest(template = "empty", batch = B)
    public static void overworldTijdRaaktVadsrijkNiet(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel vr = Vadsrijk.level(server), ow = server.overworld();
        DimensionType type = type(server);
        Holder<WorldClock> klok = VadsrijkTijd.klok(server).orElse(null);
        helper.assertTrue(klok != null, "the clock guhs:vadsrijk is registered");
        helper.assertTrue(type.defaultClock().map(h -> h.is(GuhTime.VADSRIJK_KLOK)).orElse(false), "the Vadsrijk's default clock is guhs:vadsrijk");
        helper.assertTrue(GuhTime.klok(type, server.registryAccess()).is(GuhTime.VADSRIJK_KLOK), "GuhTime picks the Vadsrijk clock for the Vadsrijk type");
        helper.assertTrue(GuhTime.klok(ow).is(WorldClocks.OVERWORLD), "the overworld keeps its own clock");
        boolean zon = false;
        for (Holder<Timeline> t : type.timelines()) {
            zon |= t.is(Identifier.fromNamespaceAndPath("guhs", "vadsrijk_dag")) && t.value().clock().is(GuhTime.VADSRIJK_KLOK);
            helper.assertTrue(!t.value().clock().is(WorldClocks.OVERWORLD) || t.is(Identifier.withDefaultNamespace("villager_schedule")),
                    "no overworld day timeline in the Vadsrijk: " + t.getRegisteredName());
        }
        helper.assertTrue(zon, "the Vadsrijk's sun/moon/sky timeline runs on the Vadsrijk clock");

        ServerClockManager m = server.clockManager();
        Holder<WorldClock> owKlok = GuhTime.overworldClock(ow);
        long owWas = m.getTotalTicks(owKlok), vrWas = m.getTotalTicks(klok);
        try {
            m.setTotalTicks(klok, 24000L * 4 + 6000);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set 18000");
            helper.assertTrue(GuhTime.timeOfDay(ow) == 18000, "overworld /time set worked: " + GuhTime.timeOfDay(ow));
            helper.assertTrue(m.getTotalTicks(klok) == 24000L * 4 + 6000, "overworld /time set did not touch the Vadsrijk: " + m.getTotalTicks(klok));
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set night");
            helper.assertTrue(m.getTotalTicks(klok) == 24000L * 4 + 6000, "overworld /time set night did not touch the Vadsrijk either");
            long owNu = m.getTotalTicks(owKlok);
            String cmd = vr != null ? "execute in guhs:vadsrijk run time set 13000" : "time of guhs:vadsrijk set 13000";
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), cmd);
            helper.assertTrue(tijdVanDeDag(server) == 13000, "/time set of the Vadsrijk moves the Vadsrijk clock: " + tijdVanDeDag(server));
            helper.assertTrue(m.getTotalTicks(owKlok) == owNu, "... and not the overworld clock");
            if (vr != null) {
                helper.assertTrue(GuhTime.timeOfDay(vr) == 13000 && GuhTime.timeOfDay(ow) != 13000, "GuhTime reads the Vadsrijk clock in the Vadsrijk level");
            }
        } finally {
            m.setTotalTicks(owKlok, owWas);
            m.setTotalTicks(klok, vrWas);
        }
        helper.succeed();
    }

    /** "Is it night" (Rooster, the sky light behind isDarkOutside, GuhTime) follows the Vadsrijk clock; the speed of each half. */
    @GuhTest(template = "empty", batch = B)
    public static void nachtVolgtVadsrijkKlok(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel ow = server.overworld();
        DimensionType type = type(server);
        ServerClockManager m = server.clockManager();
        Holder<WorldClock> klok = VadsrijkTijd.klok(server).orElseThrow(), owKlok = GuhTime.overworldClock(ow);
        long owWas = m.getTotalTicks(owKlok), vrWas = m.getTotalTicks(klok);
        double cWas = TijdConfig.cyclus(), dWas = TijdConfig.dagDeel();
        try {
            TijdConfig.zet(20, 0.667, false);
            // Vadsrijk midnight, overworld noon
            m.setTotalTicks(klok, 24000L * 2 + 18000);
            m.setTotalTicks(owKlok, 24000L * 2 + 6000);
            ow.updateSkyBrightness();
            helper.assertTrue(Rooster.nachtOp(tijdVanDeDag(server)), "the town schedule says night in the Vadsrijk");
            helper.assertTrue(donker(server, type), "the Vadsrijk sky is dark (timeline on the Vadsrijk clock): sky light " + hemellicht(server, type));
            helper.assertTrue(ow.isBrightOutside() && !Rooster.nachtOp(GuhTime.timeOfDay(ow)), "while the overworld has its noon");
            VadsrijkTijd.pas(server);
            helper.assertTrue(Math.abs(m.getRate(klok) - VadsrijkTijd.tempo(18000, 20, 0.667)) < 1e-5, "night half: night speed " + m.getRate(klok));
            // Vadsrijk noon, overworld midnight
            m.setTotalTicks(klok, 24000L * 2 + 6000);
            m.setTotalTicks(owKlok, 24000L * 2 + 18000);
            ow.updateSkyBrightness();
            helper.assertTrue(!Rooster.nachtOp(tijdVanDeDag(server)) && !donker(server, type), "Vadsrijk noon: day, bright: " + hemellicht(server, type));
            helper.assertTrue(ow.isDarkOutside(), "overworld midnight: dark");
            VadsrijkTijd.pas(server);
            helper.assertTrue(Math.abs(m.getRate(klok) - VadsrijkTijd.tempo(6000, 20, 0.667)) < 1e-5, "day half: day speed " + m.getRate(klok));
            // the edges of the day half: sunset starts at 12000, dusk + night + dawn run at night speed
            m.setTotalTicks(klok, 24000L * 2 + 11999);
            VadsrijkTijd.pas(server);
            helper.assertTrue(Math.abs(m.getRate(klok) - VadsrijkTijd.tempo(0, 20, 0.667)) < 1e-5, "11999: still day speed");
            m.setTotalTicks(klok, 24000L * 2 + 12000);
            VadsrijkTijd.pas(server);
            helper.assertTrue(Math.abs(m.getRate(klok) - VadsrijkTijd.tempo(12000, 20, 0.667)) < 1e-5, "12000: dusk runs at night speed");
            // a new config applies at the next pas
            TijdConfig.zet(30, 0.5, false);
            VadsrijkTijd.pas(server);
            helper.assertTrue(Math.abs(m.getRate(klok) - 12000.0 / (0.5 * 30 * 1200)) < 1e-5, "30 min half/half: " + m.getRate(klok));
            // zet nacht / zet dag: the next evening / morning of the Vadsrijk only
            long owNu = m.getTotalTicks(owKlok);
            helper.assertTrue(VadsrijkTijd.zet(server, true) && tijdVanDeDag(server) == 13000, "zet nacht -> 13000: " + tijdVanDeDag(server));
            helper.assertTrue(Rooster.nachtOp(tijdVanDeDag(server)), "after zet nacht the towns sleep");
            helper.assertTrue(VadsrijkTijd.zet(server, false) && tijdVanDeDag(server) == 1000, "zet dag -> 1000: " + tijdVanDeDag(server));
            helper.assertTrue(m.getTotalTicks(owKlok) == owNu, "zet dag/nacht never moves the overworld");
        } finally {
            TijdConfig.zet(cWas, dWas, false);
            m.setTotalTicks(owKlok, owWas);
            m.setTotalTicks(klok, vrWas);
            VadsrijkTijd.pas(server);
        }
        helper.succeed();
    }

    /** The Vadsrijk clock is in vanilla's SavedData world_clocks (time + speed survive a save/load round trip). */
    @GuhTest(template = "empty", batch = B)
    public static void tijdOpslaanEnLaden(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerClockManager m = server.clockManager();
        Holder<WorldClock> klok = VadsrijkTijd.klok(server).orElseThrow();
        long was = m.getTotalTicks(klok);
        try {
            m.setTotalTicks(klok, 24000L * 9 + 7777);
            VadsrijkTijd.pas(server);
            float tempo = m.getRate(klok);
            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
            Tag tag = PackedClockStates.CODEC.encodeStart(ops, m.packState()).getOrThrow();
            helper.assertTrue(tag.toString().contains("guhs:vadsrijk"), "saved under guhs:vadsrijk: " + tag);
            PackedClockStates terug = PackedClockStates.CODEC.parse(ops, tag).getOrThrow();
            ClockState s = terug.clocks().get(klok);
            helper.assertTrue(s != null, "the Vadsrijk clock comes back after loading");
            helper.assertTrue(s.totalTicks() == 24000L * 9 + 7777, "its time: " + s.totalTicks());
            helper.assertTrue(Math.abs(s.rate() - tempo) < 1e-6, "its speed: " + s.rate() + " vs " + tempo);
            helper.assertTrue(server.getDataStorage().computeIfAbsent(ServerClockManager.TYPE) == m, "the clock manager is the world's SavedData (world_clocks)");
        } finally {
            m.setTotalTicks(klok, was);
            VadsrijkTijd.pas(server);
        }
        helper.succeed();
    }
}
