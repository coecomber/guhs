package nl.juiced.guhs.feature.vadsrijk.puzzels;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.gametest.GuhTest;

/** 1.10.19: which cells the sand repair puts back (only damage done by falling blocks). */
public final class ZandHerstelGameTests {
    private ZandHerstelGameTests() {
    }

    @GuhTest(template = "empty", batch = "vadsrijk_zandherstel")
    public static void alleenGevallenZand(GameTestHelper helper) {
        var poeder = Blocks.PINK_CONCRETE_POWDER.defaultBlockState();
        var lucht = Blocks.AIR.defaultBlockState();
        helper.assertTrue(ZandHerstel.moetTerug(lucht, poeder), "a hole where the beach was");
        helper.assertTrue(ZandHerstel.moetTerug(poeder, lucht), "a pile where it landed");
        helper.assertTrue(ZandHerstel.moetTerug(Blocks.PINK_CONCRETE.defaultBlockState(), Blocks.WATER.defaultBlockState()), "powder that set in water");
        helper.assertTrue(!ZandHerstel.moetTerug(poeder, poeder) && !ZandHerstel.moetTerug(lucht, lucht), "unchanged cells stay");
        helper.assertTrue(!ZandHerstel.moetTerug(Blocks.CHEST.defaultBlockState(), lucht), "a player's / treasure chest stays");
        helper.assertTrue(!ZandHerstel.moetTerug(Blocks.CAMPFIRE.defaultBlockState(), Blocks.CAMPFIRE.defaultBlockState().setValue(
                net.minecraft.world.level.block.CampfireBlock.LIT, false)), "a lit campfire stays lit");
        helper.succeed();
    }
}
