package nl.juiced.guhs.feature.safari;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.safari.gedrag.Kijkplekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Op Safari! round 3 (J1): the activity scheduler (lively, with rest: about 60% active, many kinds of activity), the
 * favourite-spot visit near a viewpoint, and the sleeping pose every species needs.
 */
public class SafariGedragGameTests {
    private static final String BATCH = "safari_gedrag";
    private static final String VLOER = "px_test_16";
    // (the world tests switch SafariDier.ALTIJD_ACTIEF on and off: each in a batch of its own, batches run one after another)

    /** The draw: at least 5 kinds, between half and four fifths active (with and without favourites). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void planner(GameTestHelper helper) {
        for (boolean fav : new boolean[]{true, false}) {
            RandomSource r = RandomSource.create(42);
            Map<BezigheidGoal.Soort, Integer> tel = new EnumMap<>(BezigheidGoal.Soort.class);
            int actief = 0, n = 4000;
            for (int i = 0; i < n; i++) {
                BezigheidGoal.Soort s = BezigheidGoal.kies(r, fav, true, false);
                tel.merge(s, 1, Integer::sum);
                if (!BezigheidGoal.rustig(s)) {
                    actief++;
                }
            }
            double deel = actief / (double) n;
            helper.assertTrue(tel.size() >= (fav ? 7 : 6), "many kinds of activity: " + tel);
            helper.assertTrue(deel >= 0.5 && deel <= 0.8, "active share " + String.format("%.2f", deel) + " (favourites " + fav + ")");
        }
        helper.succeed();
    }

    /** In the world: an animal with a player near (here: always) gets busy with something by itself. */
    @GuhTest(template = VLOER, batch = BATCH + "_bezig", timeoutTicks = 200)
    public static void wordtBezig(GameTestHelper helper) {
        SafariDier.ALTIJD_ACTIEF = true;
        SafariDier d = helper.spawn(SafariFeature.DIER.get(), new BlockPos(4, 1, 8));
        d.zet("leeuw", "vrouw", helper.absolutePos(new BlockPos(4, 1, 8)), 4, -1, -1, 0);
        helper.succeedWhen(() -> {
            boolean bezig = d.bezigheid() != null;
            if (bezig) {
                SafariDier.ALTIJD_ACTIEF = false;
            }
            helper.assertTrue(bezig, "the lion is busy with something");
        });
    }

    /** A favourite spot 7 blocks outside its home (in view of a viewpoint): it goes there and stays a while. */
    @GuhTest(template = VLOER, batch = BATCH + "_favoriet", timeoutTicks = 400)
    public static void bezoektFavoriet(GameTestHelper helper) {
        SafariDier.ALTIJD_ACTIEF = true;
        BlockPos thuis = new BlockPos(2, 1, 8);
        BlockPos fav = helper.absolutePos(new BlockPos(11, 1, 8));
        Kijkplekken.zet(List.of(new Kijkplekken.Kijkplek("test", "", Vec3.atCenterOf(fav.offset(0, 2, 6)), new Vec3(0, 0, -1), null,
                List.of(new Kijkplekken.Favoriet("drinkplek", fav, Set.of("leeuw"), 1)))), Map.of());
        SafariDier d = helper.spawn(SafariFeature.DIER.get(), thuis);
        d.zet("leeuw", "vrouw", helper.absolutePos(thuis), 2, -1, -1, 0);
        d.bezigheid.forceer(BezigheidGoal.Soort.FAVORIET);
        helper.succeedWhen(() -> {
            double afstand = Math.hypot(d.getX() - (fav.getX() + 0.5), d.getZ() - (fav.getZ() + 0.5));
            boolean daar = afstand <= 3 && d.bezigheid() == BezigheidGoal.Soort.FAVORIET;
            if (daar) {
                SafariDier.ALTIJD_ACTIEF = false;
                Kijkplekken.vergeet();
            }
            helper.assertTrue(daar, "the lion visits its favourite spot (" + String.format("%.1f", afstand) + " blocks away, " + d.bezigheid() + ", bezoek " + d.bezoek() + ", fav " + fav + ", pos " + d.blockPosition() + ", slaapt " + d.slaapt() + ")");
        });
    }

    /** Every species can lie down (slaap, rust) and has the rest activity; the sleeping texture exists where generated. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void elkeSoortSlaapt(GameTestHelper helper) {
        for (ParkData.Soort s : ParkData.get().soorten.values()) {
            helper.assertTrue(s.animaties().contains("slaap") && s.animaties().contains("rust"), s.naam() + " has slaap + rust: " + s.animaties());
            helper.assertTrue(s.acties().contains("rust"), s.naam() + " can rest");
        }
        helper.succeed();
    }
}
