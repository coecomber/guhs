package nl.juiced.guhs.feature.safari;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Op Safari! test round 5 (fix slice FX-B): fish in the middle of their water, the Kriebelhuis tanks never nearly empty,
 * the Beverbeek ranger's directions and the busstation in the Superkompas.
 */
public class SafariFxbGameTests {
    private static final String BATCH = "safari";

    /** A swimmer goes to the middle of a 6-deep water column (never the top two blocks), a bottom dweller to the bottom. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void safariVisZwemtInHetMidden(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        for (int y = 2; y <= 7; y++) {
            helper.setBlock(new BlockPos(2, y, 2), Blocks.WATER);
        }
        BlockPos top = helper.absolutePos(new BlockPos(2, 7, 2));
        BlockPos vis = SafariDier.waterPlek(helper.getLevel(), top.getX(), top.getY(), top.getZ(), top.getY() - 20, false);
        BlockPos bodem = SafariDier.waterPlek(helper.getLevel(), top.getX(), top.getY(), top.getZ(), top.getY() - 20, true);
        int y = vis.getY() - helper.absolutePos(BlockPos.ZERO).getY();
        helper.assertTrue(y >= 3 && y <= 5, "a fish swims in the middle (rel. y 3..5), not at the top: " + y);
        helper.assertTrue(bodem.getY() - helper.absolutePos(BlockPos.ZERO).getY() == 2, "a starfish lies on the bottom");
        helper.succeed();
    }

    /** Every Kriebelhuis water species at least 3, the small fish a school of 8; land animals and decor nothing extra. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void safariAquariumNooitBijnaLeeg(GameTestHelper helper) {
        ParkData d = ParkData.get();
        helper.assertTrue(Populatie.minimum(d.soort("regenboogvis")) == Populatie.SCHOOL, "rainbow fish: a school");
        helper.assertTrue(Populatie.minimum(d.soort("kwal")) == Populatie.MIN_AQUARIUM, "jellyfish: at least 3");
        helper.assertTrue(Populatie.minimum(d.soort("anemoon")) == 0, "the anemone is decoration");
        helper.assertTrue(Populatie.minimum(d.soort("leeuw")) == 0, "a lion: just its family");
        helper.succeed();
    }

    /** The ranger names the direction in eight steps (north is -z), and the Superkompas knows the busstation. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void safariRangerWijstHetBusstation(GameTestHelper helper) {
        helper.assertTrue("noord".equals(nl.juiced.guhs.feature.beverbeek.BeverbeekRanger.richting(0, -300)), "north");
        helper.assertTrue("oost".equals(nl.juiced.guhs.feature.beverbeek.BeverbeekRanger.richting(300, 0)), "east");
        helper.assertTrue("zuid".equals(nl.juiced.guhs.feature.beverbeek.BeverbeekRanger.richting(0, 300)), "south");
        helper.assertTrue("west".equals(nl.juiced.guhs.feature.beverbeek.BeverbeekRanger.richting(-300, 0)), "west");
        helper.assertTrue("noordoost".equals(nl.juiced.guhs.feature.beverbeek.BeverbeekRanger.richting(200, -200)), "northeast");
        helper.assertTrue(nl.juiced.guhs.item.SuperkompasItem.categoryOf("safari_busstation") >= 0, "the Superkompas lists the busstation");
        helper.assertTrue(nl.juiced.guhs.feature.safari.momenten.Parade.BABYS.size() == 2, "two little penguins walk in the parade");
        helper.succeed();
    }
}
