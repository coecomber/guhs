package nl.juiced.guhs.feature.guhwinter.hoentjes;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;

/**
 * The one game test of the hoentjes slice (run it alone: {@code -Pgt=hoentjes}): the kerstwandeling's scene API. A
 * waterhoentje walks and an alpine hoentje hops across the waterdiertjes test meadow (14 x 14 grass), both get there and
 * vanish; scene hoentjes are never saved and can't be hurt; an unknown soort gives null; the two Guhdex pages are bonus pages.
 */
public class HoentjesGameTests {
    private static final String WEI = "waterdiertjes_test_wei";
    private static final String BATCH = "hoentjes";

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 800)
    public static void hoentjesSceneOversteken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 a1 = helper.absoluteVec(new Vec3(1.5, 2, 4.5)), b1 = helper.absoluteVec(new Vec3(12.5, 2, 4.5));
        Vec3 a2 = helper.absoluteVec(new Vec3(1.5, 2, 9.5)), b2 = helper.absoluteVec(new Vec3(12.5, 2, 9.5));
        Hoentje water = GuhwinterHoentjes.spawnScene(level, "waterhoen", a1, b1);
        Hoentje alpine = GuhwinterHoentjes.spawnScene(level, "alpine hoentje", a2, b2);
        helper.assertTrue(water instanceof WaterhoenEntity && alpine instanceof AlpinehoenEntity, "both soorten spawn");
        helper.assertTrue(GuhwinterHoentjes.spawnScene(level, "kip", a1, b1) == null, "an unknown soort gives null");
        helper.assertTrue(water.isScene() && !water.shouldBeSaved() && water.isInvulnerable() && !water.removeWhenFarAway(1e6),
                "a scene hoentje is never saved, can't be hurt and doesn't despawn mid-scene");
        helper.assertTrue(GuhDex.EXTRA.containsAll(GuhwinterHoentjes.PAGINAS) && GuhwinterHoentjes.PAGINAS.stream().allMatch(GuhDex::isCreaturePage)
                && GuhwinterHoentjes.PAGINAS.stream().noneMatch(GuhDex.TELLEND::contains), "two bonus creature pages");
        double[] verst = {0, 0};
        helper.onEachTick(() -> {
            if (water.isAlive()) {
                verst[0] = Math.max(verst[0], water.getX() - a1.x);
            }
            if (alpine.isAlive()) {
                verst[1] = Math.max(verst[1], alpine.getX() - a2.x);
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(water.isRemoved() && alpine.isRemoved() && verst[0] > 9 && verst[1] > 9,
                "both crossed (" + verst[0] + ", " + verst[1] + ") and vanished"));
    }
}
