package nl.juiced.guhs.feature.safari;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Op Safari! step 1: an animal never leaves its home (escape-proof, design 4.5c), and the park data's zone lookups work
 * without the generators' files (fallback raster) as well as with them.
 */
public class SafariGameTests {
    private static final String BATCH = "safari";
    private static final String VLOER = "px_test_16";

    private static SafariDier dier(GameTestHelper helper, BlockPos thuisRel, int straal) {
        SafariDier d = helper.spawn(SafariFeature.DIER.get(), thuisRel);
        d.zet("leeuw", "man", helper.absolutePos(thuisRel), straal, -1, -1, 0);
        return d;
    }

    /** Thrown far out of its home (a knockback, a jump off a ledge over a fence): put straight back. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 60)
    public static void ontsnaptNooit(GameTestHelper helper) {
        BlockPos thuis = new BlockPos(1, 1, 8);
        SafariDier d = dier(helper, thuis, 2);
        BlockPos ver = helper.absolutePos(thuis.offset(13, 6, 0));     // (beyond straal + TELEPORT_MARGE, still in the ticking test area)
        d.teleportTo(ver.getX() + 0.5, ver.getY(), ver.getZ() + 0.5);
        d.setDeltaMovement(new Vec3(0.8, 0.6, 0));
        helper.succeedWhen(() -> {
            Vec3 h = Vec3.atBottomCenterOf(helper.absolutePos(thuis));
            double afstand = Math.hypot(d.getX() - h.x, d.getZ() - h.z);
            helper.assertTrue(!d.isRemoved() && afstand <= 2 + 1, "the lion is back home (" + String.format("%.1f", afstand) + " blocks away)");
        });
    }

    /** A little outside its home: it walks back by itself. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void looptTerug(GameTestHelper helper) {
        BlockPos thuis = new BlockPos(3, 1, 8);
        SafariDier d = dier(helper, thuis, 3);
        BlockPos weg = helper.absolutePos(new BlockPos(11, 1, 8));
        d.teleportTo(weg.getX() + 0.5, weg.getY(), weg.getZ() + 0.5);
        helper.succeedWhen(() -> {
            Vec3 h = Vec3.atBottomCenterOf(helper.absolutePos(thuis));
            double afstand = Math.hypot(d.getX() - h.x, d.getZ() - h.z);
            helper.assertTrue(afstand <= 3 + SafariDier.LOOP_MARGE, "the lion walked back (" + String.format("%.1f", afstand) + ")");
        });
    }

    /** The zone raster: the entrance lies in the park, the corner of the stamped area outside it; every column has a biome zone. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zones(GameTestHelper helper) {
        ParkData d = ParkData.get();
        BlockPos in = d.ingang;
        helper.assertTrue(d.zone(in.getX(), in.getZ()) >= 0, "the entrance lies inside the park");
        helper.assertTrue(d.zone(2, 2) < 0, "the corner of the stamped area is decor");
        helper.assertTrue(d.zone(-50, 900) < 0, "outside the stamped area is no park");
        helper.assertTrue(d.biomeZone(2, 2) >= 0 && d.biomeZone(-50, 900) >= 0, "the decor takes the biome of the nearest zone");
        helper.assertTrue(!d.plekken.isEmpty() && !d.soorten.isEmpty(), "there are homes and species");
        helper.succeed();
    }
}
