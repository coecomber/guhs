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

    /** A land animal that ends up in deep water (2 deep) gets out of it by itself, or is put back home (test round 2: the
     *  elephant walked under water in the savanne pond). */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 260)
    public static void uitHetDiepe(GameTestHelper helper) {
        // a pool 4 x 5, 2 deep, inside the lion's home (centre 4,1,8, radius 7), with a stone rim
        for (int x = 8; x <= 13; x++) {
            for (int z = 5; z <= 11; z++) {
                boolean rand = x == 8 || x == 13 || z == 5 || z == 11;
                for (int y = 1; y <= 2; y++) {
                    helper.setBlock(new BlockPos(x, y, z), rand ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                            : net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
                }
            }
        }
        BlockPos thuis = new BlockPos(4, 1, 8);
        SafariDier d = dier(helper, thuis, 7);
        BlockPos in = helper.absolutePos(new BlockPos(10, 1, 8));
        d.teleportTo(in.getX() + 0.5, in.getY(), in.getZ() + 0.5);
        helper.runAfterDelay(5, () -> helper.assertTrue(d.isInWater(), "the lion starts in the pool"));
        helper.succeedWhen(() -> {
            BlockPos p = d.blockPosition();
            int diep = Math.max(SafariDier.waterDiepte(d.level(), p), SafariDier.waterDiepte(d.level(), p.above()) + (d.isInWater() ? 1 : 0));
            helper.assertTrue(!d.isRemoved() && (!d.isInWater() || diep < 2), "the lion is out of the deep water");
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
