package nl.juiced.guhs.feature.vadsrijk.udeco;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-DECO: the arrival clearance (gondola stations are no-spawn spots, a far spawner cannot spawn next to a campfire, a spawner
 * next to a campfire still can), the Honingtak deck test and the 6 stair boards. Run: {@code runGameTestServer -Pgt=UDeco}.
 */
public class UDecoGameTests {
    private static final String BATCH = "vadsrijk_udeco";

    @GuhTest(template = "empty", batch = BATCH)
    public static void aankomstvrij(GameTestHelper helper) {
        helper.assertTrue(Aankomstvrij.aankomst("gondel_dal") && Aankomstvrij.aankomst("gondel_berg") && Aankomstvrij.aankomst("boot_dok_schildpad")
                && Aankomstvrij.aankomst("haven_3_4_landing") && !Aankomstvrij.aankomst("boot_dok_wrak_water") && !Aankomstvrij.aankomst("kampvuur_kamp"),
                "arrival id patterns");
        BlockPos dal = Z2WereldPlekken.ALLE.get("gondel_dal");
        helper.assertTrue(dal != null && Aankomstvrij.punten().stream().anyMatch(p -> p.strikt() && p.pos().equals(dal)), "gondel_dal is an arrival spot");
        // a spawner centred right on the station: still nothing next to the station (strict)
        Spawners.Spawner opStation = new Spawners.Spawner("udeco:test", dal, 30, null, 1, 100, 1, 1, null, 0);
        helper.assertTrue(Aankomstvrij.verboden(opStation, dal.offset(10, 0, 5)), "no spawn 11 blocks from the gondola station");
        helper.assertTrue(!Aankomstvrij.verboden(opStation, dal.offset(30, 0, 0)) || Aankomstvrij.stadsrand(dal.offset(30, 0, 0)),
                "a spawn 30 blocks away is allowed");
        // campfires: a far spawner may not spawn 20 from it, a spawner placed next to it may
        Aankomstvrij.Punt kv = Aankomstvrij.punten().stream().filter(p -> !p.strikt()).findFirst().orElseThrow();
        BlockPos k = kv.pos();
        Spawners.Spawner ver = new Spawners.Spawner("udeco:ver", k.offset(60, 0, 0), 45, null, 1, 100, 1, 1, null, 0);
        Spawners.Spawner bij = new Spawners.Spawner("udeco:bij", k.offset(8, 0, 0), 10, null, 1, 100, 1, 1, null, 0);
        helper.assertTrue(Aankomstvrij.verboden(ver, k.offset(20, 0, 0)), "far spawner: not 20 from a campfire");
        boolean stad = Aankomstvrij.stadsrand(k.offset(20, 0, 0)) && !Aankomstvrij.stadsrand(bij.centrum());
        helper.assertTrue(stad || !Aankomstvrij.verboden(bij, k.offset(20, 0, 0)) || Aankomstvrij.punten().stream()
                .anyMatch(p -> p != kv && Aankomstvrij.binnen(k.offset(20, 0, 0), p.pos(), Aankomstvrij.STRAAL)), "a campfire's own spawner keeps working");
        helper.assertTrue(Spawners.VERBODEN_VOOR.size() >= 1, "hook registered");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void honingtakEnBorden(GameTestHelper helper) {
        BlockPos plein = Z3WereldPlekken.ALLE.get("plein");
        helper.assertTrue(plein != null && HoningtakBijen.opHoningtak(plein.getX(), plein.getY(), plein.getZ()), "the Honingplein is on the deck");
        helper.assertTrue(!HoningtakBijen.opHoningtak(Z3WereldPlekken.STAM_X, 167, Z3WereldPlekken.STAM_Z), "the trunk axis is not");
        for (int i = 0; i < Verdiepingborden.AANTAL; i++) {
            Vec3 p = Verdiepingborden.plek(i);
            BlockPos b = Z3WereldPlekken.ALLE.get("trap_bordes_" + (i + 1));
            helper.assertTrue(p != null && b != null && p.y > b.getY() && p.distanceTo(Vec3.atCenterOf(b)) < 8, "board " + (i + 1) + " at its landing");
            helper.assertTrue(Verdiepingborden.tekst(i).getString().length() > 3, "board text " + (i + 1));
        }
        helper.succeed();
    }
}
