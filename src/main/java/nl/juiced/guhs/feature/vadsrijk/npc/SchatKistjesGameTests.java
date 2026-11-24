package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import nl.juiced.guhs.gametest.GuhTest;

/** 1.10.15: the treasure chests (SchatKistjes) and the find radius of 4. */
public final class SchatKistjesGameTests {
    private static final String B = "vadsrijk_schatkistjes";

    private SchatKistjesGameTests() {
    }

    /** An empty spot gets a chest, a spot with water or another block keeps what it has; every zone has treasures. */
    @GuhTest(template = "empty", batch = B)
    public static void kistjeOpLeegPlekje(GameTestHelper helper) {
        var sl = helper.getLevel();
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.AIR.defaultBlockState());
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.WATER.defaultBlockState());
        helper.setBlock(new BlockPos(1, 1, 3), Blocks.OAK_PLANKS.defaultBlockState());
        BlockPos leeg = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.assertTrue(SchatKistjes.zet(sl, leeg) && sl.getBlockState(leeg).getBlock() instanceof ChestBlock, "a chest on the empty spot");
        helper.assertTrue(SchatKistjes.zet(sl, leeg), "a chest that stands there counts");
        helper.assertTrue(!SchatKistjes.zet(sl, helper.absolutePos(new BlockPos(3, 1, 1))), "not in water");
        helper.assertTrue(!SchatKistjes.zet(sl, helper.absolutePos(new BlockPos(1, 1, 3))) && helper.getBlockState(new BlockPos(1, 1, 3)).is(Blocks.OAK_PLANKS),
                "another block stays");
        int n = 0;
        for (Map<String, BlockPos> m : SchatKistjes.bronnen()) {
            helper.assertTrue(!m.isEmpty(), "every zone has treasures");
            for (BlockPos p : m.values()) {
                helper.assertTrue(p != null && SchatKistjes.isSchat(p), "a known treasure spot");
                n++;
            }
        }
        helper.assertTrue(n >= 20 && !SchatKistjes.isSchat(leeg), "treasures counted (" + n + "), a random spot is none");
        helper.assertTrue(NpcPlekken.VIND_STRAAL == 4 && nl.juiced.guhs.feature.vadsrijk.zone3.npc.Z3NpcPlekken.VIND_STRAAL == 4, "find radius 4");
        helper.succeed();
    }
}
