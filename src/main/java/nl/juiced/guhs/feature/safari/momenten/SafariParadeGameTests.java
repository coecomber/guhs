package nl.juiced.guhs.feature.safari.momenten;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.safari.rit.Pad;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Test round 4 (design ronde 47): the pinguïnparade walks as a neat stoet, two blocks apart, never into each other.
 */
public class SafariParadeGameTests {
    private static final String BATCH = "safari";

    /** Tamboer and flag in front, the penguins in the middle, the dancers at the back. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void paradeOpstelling(GameTestHelper helper) {
        List<Parade.Rol> o = Parade.opstelling();
        helper.assertTrue(o.get(0) == Parade.Rol.TAMBOER && o.get(1) == Parade.Rol.VLAG, "tamboer and flag in front");
        helper.assertTrue(o.get(o.size() - 1) == Parade.Rol.DANSER, "dancers at the back");
        long pinguins = o.stream().filter(r -> r == Parade.Rol.PINGUIN).count();
        helper.assertTrue(pinguins >= 5, "a row of penguins");
        int laatstePinguin = o.lastIndexOf(Parade.Rol.PINGUIN), eersteDanser = o.indexOf(Parade.Rol.DANSER);
        helper.assertTrue(o.indexOf(Parade.Rol.PINGUIN) == 2 && laatstePinguin < eersteDanser, "the penguins in one row");
        helper.succeed();
    }

    /** Two blocks between members along a straight walk; round a sharp bend still never closer than 1.4 blocks. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void paradeAfstand(GameTestHelper helper) {
        int n = Parade.opstelling().size();
        Pad recht = new Pad(List.of(new Vec3(0, 64, 0), new Vec3(60, 64, 0)), false);
        for (double s = (n - 1) * Parade.AFSTAND; s < 60; s += 3.7) {
            List<Vec3> p = Parade.posities(recht, s, n);
            for (int k = 1; k < n; k++) {
                double d = p.get(k).distanceTo(p.get(k - 1));
                helper.assertTrue(Math.abs(d - 2.0) < 1e-6, "straight: 2 blocks apart, got " + d + " at " + s);
            }
        }
        Pad bocht = new Pad(List.of(new Vec3(0, 64, 0), new Vec3(20, 64, 0), new Vec3(20, 64, 20), new Vec3(0, 64, 20)), false);
        double min = Double.MAX_VALUE;
        for (double s = (n - 1) * Parade.AFSTAND; s <= bocht.lengte(); s += 0.25) {
            List<Vec3> p = Parade.posities(bocht, s, n);
            for (int a = 0; a < n; a++) {
                for (int b = a + 1; b < n; b++) {
                    min = Math.min(min, p.get(a).distanceTo(p.get(b)));
                }
            }
        }
        helper.assertTrue(min >= 1.4, "round the bends nobody clips: closest " + min);
        helper.succeed();
    }

    /** Without parade.json of the new lobby the village walk falls back to the loop of beleving.json. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void paradeDorpRoute(GameTestHelper helper) {
        List<Vec3> dorp = ParadeRoute.dorp();
        helper.assertTrue(dorp.size() >= 2, "a village walk (parade.json or beleving.json), got " + dorp.size());
        Vec3 p = ParadeRoute.punt(com.google.gson.JsonParser.parseString("[3, 70, -4]"));
        Vec3 q = ParadeRoute.punt(com.google.gson.JsonParser.parseString("{\"x\": 3, \"y\": 70, \"z\": -4}"));
        helper.assertTrue(p.equals(new Vec3(3.5, 70, -3.5)) && p.equals(q), "both point formats");
        helper.succeed();
    }
}
