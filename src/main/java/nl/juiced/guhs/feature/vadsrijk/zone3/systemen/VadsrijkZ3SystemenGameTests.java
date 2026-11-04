package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone 3 systemen: layered areas (kern Gebieden with a y-range / cylinder), ...
 * Run: {@code runGameTestServer -Pgt=VadsrijkZ3Systemen}.
 */
public class VadsrijkZ3SystemenGameTests {
    private static final String BATCH = "vadsrijk_z3_systemen";

    @GuhTest(template = "empty", batch = BATCH)
    public static void gelaagdeGebieden(GameTestHelper helper) {
        Z3Lagen.registreer();
        // same x/z, different heights
        verwacht(helper, 2300, 250, 0, "z3_kruinzee");
        verwacht(helper, 2300, 150, 0, "z3_schorsstad");
        verwacht(helper, 2300, 40, 0, "z3_hart");
        verwacht(helper, 2500, 30, 0, "z3_wortelduister");
        verwacht(helper, 2500, 80, 0, "z3_wortelvelden");
        verwacht(helper, 1800, 240, 0, "z3_honingtak");
        verwacht(helper, 1800, 80, 0, "z3_wortelvelden");
        verwacht(helper, 1850, 170, 300, "z3_takkenbanen");
        verwacht(helper, 1620, 150, 690, "z3_ring");
        verwacht(helper, 2400, 150, 0, "z3_stamspiraal");
        // a flat lookup cannot know the layer: it skips the layered areas
        Gebieden.Gebied plat = Gebieden.op(2300, 0);
        helper.assertTrue(plat != null && plat.id().equals("z3_ring"), "flat lookup in zone 3: " + (plat == null ? null : plat.id()));
        // zone 1/2 are unchanged (no layer, every height)
        helper.assertTrue(Gebieden.op(0, 0).id().equals("zwamzoet") && Gebieden.op(0, 300, 0).id().equals("zwamzoet")
                && Gebieden.op(0, -40, 0).id().equals("zwamzoet"), "zone 1 ignores y");
        helper.assertTrue(!Gebieden.van("zwamzoet").gelaagd(), "old areas have no layer");
        // a cylinder: a box corner is outside the circle
        Gebieden.Gebied c = Gebieden.Gebied.cilinder("t", 0, 0, 0, 10, 0, 10, 1, 1, false);
        helper.assertTrue(c.bevat(5, 5, 5) && !c.bevat(9.5, 5, 9.5) && !c.bevat(5, 11, 5) && c.bevat(new BlockPos(0, 10, 9)), "cylinder");
        Gebieden.Gebied ring = c.metRing(0, 0, 4, 10);
        helper.assertTrue(!ring.bevat(1, 5, 1) && ring.bevat(6, 5, 0), "ring");
        // Z3Lagen agrees with the runtime list
        helper.assertTrue(Z3Lagen.op(2300, 250, 0).id().equals("z3_kruinzee") && Z3Lagen.op(0, 64, 0) == null, "Z3Lagen.op");
        helper.succeed();
    }

    private static void verwacht(GameTestHelper helper, double x, double y, double z, String id) {
        Gebieden.Gebied g = Gebieden.op(x, y, z);
        helper.assertTrue(g != null && g.id().equals(id), "at " + x + ", " + y + ", " + z + ": " + (g == null ? null : g.id()) + " instead of " + id);
    }
}
