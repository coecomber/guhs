package nl.juiced.guhs.feature.vadsrijk.toetsen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-TOETSEN: the layout of the key overview in the pause menu (the part that can be checked without a client). The real numbers:
 * 5 groups, 23 keys (with u-hud's emote key), widest line ~150 px. Free room right of the 204-wide pause buttons at a 1920x1080
 * window: GUI scale 2 = 960x540 -> ~350x528, scale 3 = 640x360 -> ~190x348, scale 4 = 480x270 -> ~110x258.
 * Run: {@code runGameTestServer -Pgt=VadsrijkUToetsen}.
 */
public class VadsrijkUToetsenGameTests {
    private static final String BATCH = "vadsrijk_u_toetsen";

    private static List<ToetsenIndeling.Regel> echt() {
        List<ToetsenIndeling.Regel> r = new ArrayList<>();
        int[] perGroep = {6, 8, 1, 1, 7};
        for (int g : perGroep) {
            r.add(new ToetsenIndeling.Regel(true, 50));
            for (int i = 0; i < g; i++) {
                r.add(new ToetsenIndeling.Regel(false, 40 + 4 + 60 + (i * 13) % 50));
            }
        }
        return r;
    }

    private static void binnen(GameTestHelper helper, ToetsenIndeling.Uitkomst u, int maxB, int maxH, String wat) {
        helper.assertTrue(u != null, wat + ": a layout exists");
        helper.assertTrue(u.schermBreedte() <= maxB && u.schermHoogte() <= maxH,
                wat + ": fits in " + maxB + "x" + maxH + " (got " + u.schermBreedte() + "x" + u.schermHoogte() + " at " + u.schaal() + ")");
        // no two lines on top of each other, every line inside its column
        List<ToetsenIndeling.Plek> p = u.plekken();
        for (int i = 1; i < p.size(); i++) {
            ToetsenIndeling.Plek a = p.get(i - 1), b = p.get(i);
            helper.assertTrue(b.kolom() > a.kolom() || (b.kolom() == a.kolom() && b.y() > a.y()), wat + ": lines go down, then to the next column");
        }
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void indelingPastOpElkeSchaal(GameTestHelper helper) {
        List<ToetsenIndeling.Regel> r = echt();
        // GUI scale 2: everything at full size in one column
        ToetsenIndeling.Uitkomst u2 = ToetsenIndeling.kies(r, 350, 528, 0.5f, 110);
        binnen(helper, u2, 350, 528, "scale 2");
        helper.assertTrue(u2.schaal() == 1f && u2.kolommen() == 1 && !u2.gesneden(), "scale 2: full size, one column");
        // GUI scale 3: still readable, smaller or in columns
        ToetsenIndeling.Uitkomst u3 = ToetsenIndeling.kies(r, 190, 348, 0.5f, 110);
        binnen(helper, u3, 190, 348, "scale 3");
        // GUI scale 4: tight, but it must still fit (shrunk)
        ToetsenIndeling.Uitkomst u4 = ToetsenIndeling.kies(r, 110, 258, 0.5f, 110);
        binnen(helper, u4, 110, 258, "scale 4");
        helper.assertTrue(u4.schaal() < 1f, "scale 4: shrunk");
        // a header never ends a column: every header is followed by a key in the same column
        for (ToetsenIndeling.Uitkomst u : new ToetsenIndeling.Uitkomst[] {u2, u3, u4}) {
            for (int i = 0; i < r.size(); i++) {
                if (r.get(i).kop()) {
                    helper.assertTrue(i + 1 < r.size() && u.plekken().get(i + 1).kolom() == u.plekken().get(i).kolom(), "header stays with its first key");
                }
            }
        }
        // far too small (or GUI scale 1, no shrinking allowed) -> null = the "Toetsen" button instead
        helper.assertTrue(ToetsenIndeling.kies(r, 40, 258, 0.5f, 110) == null, "40 px wide: no panel");
        helper.assertTrue(ToetsenIndeling.kies(r, 110, 100, 1f, 110) == null, "no shrinking and too low: no panel");
        helper.assertTrue(ToetsenIndeling.kies(List.of(), 300, 300, 0.5f, 110) == null, "no keys: no panel");
        helper.succeed();
    }
}
