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

    /** Round 4: inside a viewpoint's view the own behaviour is drawn more often than outside it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void meerGedragInZicht(GameTestHelper helper) {
        int[] ged = new int[2];
        for (int z = 0; z < 2; z++) {
            RandomSource r = RandomSource.create(7);
            for (int i = 0; i < 4000; i++) {
                if (BezigheidGoal.kies(r, true, true, false, z == 1) == BezigheidGoal.Soort.GEDRAG) {
                    ged[z]++;
                }
            }
        }
        helper.assertTrue(ged[1] > ged[0] * 1.2, "more own behaviour in view: " + ged[1] + " vs " + ged[0]);
        helper.succeed();
    }

    /** Round 4 (test round 4: "de dieren zijn nog passief"): left alone, an animal keeps changing what it does. */
    @GuhTest(template = VLOER, batch = BATCH + "_wissel", timeoutTicks = 1600)
    public static void wisseltActiviteit(GameTestHelper helper) {
        SafariDier.ALTIJD_ACTIEF = true;
        SafariDier d = helper.spawn(SafariFeature.DIER.get(), new BlockPos(8, 1, 8));
        d.zet("zebra", "vrouw", helper.absolutePos(new BlockPos(8, 1, 8)), 5, -1, -1, 0);
        Set<BezigheidGoal.Soort> gezien = new java.util.HashSet<>();
        int[] wissels = {0};
        BezigheidGoal.Soort[] vorige = {null};
        helper.succeedWhen(() -> {
            BezigheidGoal.Soort nu = d.bezigheid();
            if (nu != null && nu != vorige[0]) {
                gezien.add(nu);
                wissels[0]++;
            }
            vorige[0] = nu;
            boolean genoeg = gezien.size() >= 3 && wissels[0] >= 4;
            if (genoeg) {
                SafariDier.ALTIJD_ACTIEF = false;
            }
            helper.assertTrue(genoeg, "the zebra changes activity: " + gezien + ", " + wissels[0] + " changes");
        });
    }

    /** Round 4: a penguin on the slide loop climbs the points (bottom to top) and slides down on its belly. */
    @GuhTest(template = VLOER, batch = BATCH + "_glijbaan", timeoutTicks = 800)
    public static void pinguinGlijbaan(GameTestHelper helper) {
        SafariDier.ALTIJD_ACTIEF = true;
        BlockPos onder = helper.absolutePos(new BlockPos(3, 1, 3)), midden = helper.absolutePos(new BlockPos(3, 1, 8));
        BlockPos boven = helper.absolutePos(new BlockPos(9, 1, 11));
        Kijkplekken.zet(List.of(), Map.of("glijbaan", List.of(onder, midden, boven)));
        SafariDier d = helper.spawn(SafariFeature.DIER.get(), new BlockPos(5, 1, 5));
        d.zet("pinguin", "vrouw", helper.absolutePos(new BlockPos(5, 1, 5)), 4, -1, -1, 0);
        d.bezigheid.forceerGedrag("glijbaan");
        boolean[] bovenGeweest = {false};
        helper.succeedWhen(() -> {
            if (d.distanceToSqr(boven.getX() + 0.5, boven.getY(), boven.getZ() + 0.5) < 2.5 * 2.5) {
                bovenGeweest[0] = true;
            }
            boolean glijdt = bovenGeweest[0] && "buikglij".equals(d.actie());
            if (glijdt) {
                SafariDier.ALTIJD_ACTIEF = false;
                Kijkplekken.vergeet();
            }
            helper.assertTrue(glijdt, "the penguin climbs to the top and slides (top reached " + bovenGeweest[0] + ", actie '" + d.actie()
                    + "', " + d.bezigheid() + ", pos " + d.blockPosition() + ")");
        });
    }

    /** Round 4: a beaver carries a branch (actie "draag", the prop_draag bone) to the nearest dam point and gets there. */
    @GuhTest(template = VLOER, batch = BATCH + "_bever", timeoutTicks = 800)
    public static void beverDraagtNaarDam(GameTestHelper helper) {
        SafariDier.ALTIJD_ACTIEF = true;
        BlockPos dam = helper.absolutePos(new BlockPos(12, 1, 12));
        Kijkplekken.zet(List.of(), Map.of("beverdam", List.of(dam)));
        SafariDier d = helper.spawn(SafariFeature.DIER.get(), new BlockPos(3, 1, 3));
        d.zet("bever", "vrouw", helper.absolutePos(new BlockPos(3, 1, 3)), 4, -1, -1, 0);
        d.bezigheid.forceerGedrag("draag");
        boolean[] droeg = {false};
        helper.succeedWhen(() -> {
            droeg[0] |= "draag".equals(d.actie());
            double afstand = Math.sqrt(d.distanceToSqr(dam.getX() + 0.5, dam.getY(), dam.getZ() + 0.5));
            boolean daar = droeg[0] && afstand < 3;
            if (daar) {
                SafariDier.ALTIJD_ACTIEF = false;
                Kijkplekken.vergeet();
            }
            helper.assertTrue(daar, "the beaver carries its branch to the dam (carried " + droeg[0] + ", " + String.format("%.1f", afstand)
                    + " blocks, actie '" + d.actie() + "', " + d.bezigheid() + ")");
        });
    }

    /** Round 4: the show animations and props are in the files the behaviour uses (giraffe pluk + leaf, otter schelp +
     *  shell, the lion's roar), and giraffe and elephant are 1.5x. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void showsEnSchaal(GameTestHelper helper) {
        ParkData d = ParkData.get();
        helper.assertTrue(d.soort("giraffe").acties().contains("anim:pluk"), "giraffe plucks: " + d.soort("giraffe").acties());
        helper.assertTrue(d.soort("otter").acties().contains("anim:schelp"), "otter taps its shell: " + d.soort("otter").acties());
        helper.assertTrue(d.soort("leeuw").acties().contains("anim:brul"), "lion roars: " + d.soort("leeuw").acties());
        helper.assertTrue(d.soort("giraffe").schaal() == 1.5f && d.soort("olifant").schaal() == 1.5f && d.soort("zebra").schaal() == 1f,
                "giraffe and olifant 1.5x");
        for (String a : BezigheidGoal.SHOW.keySet()) {
            boolean ergens = d.soorten.values().stream().anyMatch(s -> s.acties().contains("anim:" + a));
            helper.assertTrue(ergens, "some species shows " + a);
        }
        helper.succeed();
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
