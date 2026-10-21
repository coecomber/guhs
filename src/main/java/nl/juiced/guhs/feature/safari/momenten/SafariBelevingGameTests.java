package nl.juiced.guhs.feature.safari.momenten;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.safari.SafariVeilig;
import nl.juiced.guhs.feature.safari.lobby.Kampvuur;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Test round 3 (J3): the park-wide message budget of the moments ({@link Meldingen}), no catch-up after loading, the
 * kampvuur's evening and stories, and the park's safety rule ({@link SafariVeilig#tegenhouden}; the gametest server has
 * no safari dimension, so the rule is tested as the pure decision the damage event uses).
 */
public class SafariBelevingGameTests {
    private static final String BATCH = "safari";

    /** Three whole days of moments: never two lines within the budget, a handful of lines a day, every line about the future. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void meldingBudget(GameTestHelper helper) {
        Meldingen m = new Meldingen();
        List<Long> verstuurd = new ArrayList<>();
        for (long g = 0; g < 3L * MomentSchema.DAG; g += 20) {
            long dag = g / MomentSchema.DAG;
            int t = (int) (g % MomentSchema.DAG);
            for (MomentSchema.Moment mo : MomentSchema.aankondigen(dag, t)) {
                helper.assertTrue(mo.van() > t, "only moments still to come are offered");
                m.bied((dag * MomentSchema.DAG + mo.van()) + ":" + mo.soort() + ":" + mo.zone(), Component.literal(mo.soort().name()), g + mo.van() - t);
            }
            if (m.volgende(g) != null) {
                verstuurd.add(g);
            }
        }
        for (int i = 1; i < verstuurd.size(); i++) {
            helper.assertTrue(verstuurd.get(i) - verstuurd.get(i - 1) >= Meldingen.BUDGET, "two lines within 2.5 minutes at " + verstuurd.get(i));
        }
        int perDag = verstuurd.size() / 3;
        helper.assertTrue(perDag >= 4 && perDag <= MomentSchema.DAG / Meldingen.BUDGET + 1, "a handful of lines a day, not " + perDag);
        helper.succeed();
    }

    /** Loading in the middle of a busy day: at most one line in the first minute, and nothing about what already began. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void meldingGeenInhaal(GameTestHelper helper) {
        Meldingen m = new Meldingen();
        long dag = 4;
        int start = 5200;          // a few feedings and a praatje already began today
        int lijnen = 0;
        Set<String> genoemd = new HashSet<>();
        for (int t = start; t < start + 1200; t += 20) {
            long g = dag * MomentSchema.DAG + t;
            for (MomentSchema.Moment mo : MomentSchema.aankondigen(dag, t)) {
                genoemd.add(mo.soort() + ":" + mo.zone() + ":" + mo.van());
                helper.assertTrue(mo.van() > start || mo.van() > t, "a moment that already began is never announced");
                m.bied(mo.soort() + ":" + mo.zone() + ":" + mo.van(), Component.literal("x"), g + mo.van() - t);
            }
            if (m.volgende(g) != null) {
                lijnen++;
            }
        }
        helper.assertTrue(lijnen == 1, "exactly one line in the first minute after loading, not " + lijnen);
        for (MomentSchema.Moment mo : MomentSchema.dag(dag)) {
            if (mo.van() <= start) {
                helper.assertTrue(!genoemd.contains(mo.soort() + ":" + mo.zone() + ":" + mo.van()), "old moment " + mo + " announced");
            }
        }
        // a notice that is out of date by the time the budget is free is dropped, not sent late
        Meldingen n = new Meldingen();
        n.bied("a", Component.literal("a"), 100);
        helper.assertTrue(n.volgende(0) != null, "the first line goes out at once");
        n.bied("b", Component.literal("b"), 500);
        helper.assertTrue(n.volgende(400) == null, "budget: not yet");
        helper.assertTrue(n.volgende(Meldingen.BUDGET) == null && n.wachtend() == 0, "moment b began meanwhile: dropped");
        helper.assertTrue(!n.bied("a", Component.literal("a"), 99999), "the same moment is offered once");
        helper.succeed();
    }

    /** Several waiting moments become one line. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void meldingSamen(GameTestHelper helper) {
        Meldingen m = new Meldingen();
        m.bied("eerste", Component.literal("eerste"), 50);
        helper.assertTrue(m.volgende(0) != null, "the first line goes out at once");
        for (int i = 0; i < 5; i++) {
            m.bied("m" + i, Component.literal("m" + i), 10_000 + i);
        }
        helper.assertTrue(m.volgende(100) == null, "within the budget: wait");
        Component regel = m.volgende(Meldingen.BUDGET);
        helper.assertTrue(regel != null && m.wachtend() == 0, "one line for all five");
        helper.assertTrue(m.volgende(Meldingen.BUDGET + 20) == null, "and then quiet");
        helper.succeed();
    }

    /** In the park a player takes no damage (falling, drowning, mobs); outside it does; /kill still works; animals are not players. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void veiligInPark(GameTestHelper helper) {
        var bronnen = helper.getLevel().damageSources();
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        var zombie = helper.spawn(EntityType.ZOMBIE, new net.minecraft.core.BlockPos(1, 1, 1));
        helper.assertTrue(SafariVeilig.tegenhouden(true, p, bronnen.fall()), "no fall damage in the park");
        helper.assertTrue(SafariVeilig.tegenhouden(true, p, bronnen.drown()), "no drowning in the park");
        helper.assertTrue(SafariVeilig.tegenhouden(true, p, bronnen.starve()), "no starving in the park");
        helper.assertTrue(SafariVeilig.tegenhouden(true, p, bronnen.mobAttack(zombie)), "no animal bites in the park");
        helper.assertTrue(SafariVeilig.tegenhouden(true, p, bronnen.lava()), "no lava in the park");
        helper.assertTrue(!SafariVeilig.tegenhouden(true, p, bronnen.genericKill()), "/kill still works");
        helper.assertTrue(!SafariVeilig.tegenhouden(false, p, bronnen.fall()), "outside the park: normal");
        helper.assertTrue(!SafariVeilig.tegenhouden(true, zombie, bronnen.fall()), "only players");
        helper.succeed();
    }

    /** The kampvuur burns in the evening; the three stories of one evening differ and are all real stories. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void kampvuurAvond(GameTestHelper helper) {
        helper.assertTrue(Kampvuur.avond(12000) && !Kampvuur.avond(6000) && !Kampvuur.avond(18000) && Kampvuur.avond(24000L * 5 + 13000), "evening hours");
        for (long dag = 0; dag < 6; dag++) {
            Set<Integer> s = new HashSet<>();
            for (int n = 0; n < Kampvuur.PER_DAG; n++) {
                int v = Kampvuur.verhaal(dag, n);
                helper.assertTrue(v >= 0 && v < Kampvuur.VERHALEN, "story " + v);
                s.add(v);
            }
            helper.assertTrue(s.size() == Kampvuur.PER_DAG, "three different stories on day " + dag);
        }
        helper.assertTrue(Kampvuur.verhaal(0, 0) != Kampvuur.verhaal(1, 0), "another story the next day");
        helper.succeed();
    }
}
