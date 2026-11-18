package nl.juiced.guhs.feature.guhwinter.spellen;

import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Opname;
import nl.juiced.guhs.feature.guhwinter.spellen.race.RaceSim;
import nl.juiced.guhs.gametest.GuhTest;

/** Guhwinter spellen: the Pepernotenrace's score check (replay, time sanity, the pepernoten a run gives). */
public class SpellenGameTests {
    private static final String EMPTY = "empty";

    /**
     * A recorded run gives the same score when the server plays it again; a run counts only when its steps fit in the
     * time that went by; a run gives at most {@value Pepernotenrace#MAX_PER_RUN} pepernoten and nothing under ten seconds.
     */
    @GuhTest(template = EMPTY)
    public static void raceScoreSanity(GameTestHelper helper) {
        long seed = 1234567L;
        RaceSim sim = new RaceSim(seed);
        Opname opname = new Opname(sim.bits());
        int[] patroon = {0, 0, RaceSim.LINKS, 0, 0, RaceSim.SPRING, 0, RaceSim.RECHTS, 0, RaceSim.RECHTS, 0, RaceSim.BUK, 0, RaceSim.LINKS};
        int i = 0;
        while (!sim.af() && sim.stappen() < 30 * 60 * 3) {
            int invoer = (sim.stappen() % 9 == 0) ? patroon[i++ % patroon.length] : 0;
            opname.schrijf(invoer);
            sim.stap(invoer);
        }
        RaceSim.Uitkomst terug = RaceSim.speelAf(seed, opname.bytes(), opname.stappen());
        helper.assertTrue(terug.score() == sim.score() && terug.stappen() == sim.stappen() && terug.pepernoten() == sim.pepernoten(),
                "replay gives the same run: " + terug + " vs score " + sim.score() + ", steps " + sim.stappen());
        RaceSim.Uitkomst andersSeed = RaceSim.speelAf(seed + 1, opname.bytes(), opname.stappen());
        helper.assertTrue(andersSeed.stappen() > 0, "another seed plays too (it just ends elsewhere)");
        // the time check: honest, a slow client, and one that claims a 3 minute run after 30 seconds
        int n = 30 * 60;
        helper.assertTrue(RaceSim.geloofwaardig(n, 60_000L), "one minute of steps in one minute");
        helper.assertTrue(RaceSim.geloofwaardig(n, 90_000L), "slower is fine");
        helper.assertTrue(RaceSim.geloofwaardig(n, 56_000L), "a little faster (connection) is fine");
        helper.assertFalse(RaceSim.geloofwaardig(30 * 180, 30_000L), "3 minutes of steps in 30 seconds is not");
        helper.assertFalse(RaceSim.geloofwaardig(n, -5L), "no negative time");
        // the reward: capped, nothing for a very short run
        helper.assertTrue(Pepernotenrace.beloning(new RaceSim.Uitkomst(5000, 30 * 120, 400, 900)) == Pepernotenrace.MAX_PER_RUN, "capped at the max");
        helper.assertTrue(Pepernotenrace.beloning(new RaceSim.Uitkomst(300, 30 * 40, 34, 120)) == 3, "one per ten picked up");
        helper.assertTrue(Pepernotenrace.beloning(new RaceSim.Uitkomst(80, 30 * 5, 12, 20)) == 0, "under ten seconds: nothing");
        helper.assertTrue(Pepernotenrace.beloning(new RaceSim.Uitkomst(80, 30 * 15, 2, 50)) == 1, "a real run gives at least one");
        // oliebollen: a full score needs (almost) the whole game
        helper.assertTrue(Oliebollen.maxVoorTijd(45_000L) == 100 && Oliebollen.maxVoorTijd(10_000L) < 40, "oliebollen time cap");
        helper.succeed();
    }
}
