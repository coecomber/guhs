package nl.juiced.guhs.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhSpawnRuimte;
import nl.juiced.guhs.registry.ModEntities;

/** 1.8.0: big guhs no longer spawn half in a wall (and suffocate): the real size gets room, with a safety net. */
public final class GuhSpawnGameTests {
    private static final String EMPTY = "empty";

    private GuhSpawnGameTests() {
    }

    /** A stone floor, a wall at x 0 and a low roof (y 2) over x 0-1: a nook a big guh doesn't fit in; open room from x 2. */
    static void bouwSleuf(GameTestHelper helper) {
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        for (int z = 0; z < 7; z++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(0, y, z), Blocks.STONE);
            }
            helper.setBlock(new BlockPos(1, 2, z), Blocks.STONE);
        }
    }

    static GuhEntity groteGuhInDeSleuf(GameTestHelper helper, boolean tam) {
        GuhEntity guh = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 3)));
        guh.snapTo(at.x, at.y, at.z, 0, 0);
        guh.setGuhScale(GuhEntity.MAX_SCALE);
        if (tam) {
            guh.setTame(true, false);
        }
        helper.getLevel().addFreshEntity(guh);
        return guh;
    }

    @GuhTest(template = EMPTY, batch = "guhspawn")
    public static void groteGuhKrijgtRuimteBijHetSpawnen(GameTestHelper helper) {
        bouwSleuf(helper);
        GuhEntity guh = groteGuhInDeSleuf(helper, false);
        helper.assertFalse(GuhSpawnRuimte.vrij(helper.getLevel(), guh), "the nook is too small for a 2.1x guh");
        helper.assertTrue(GuhSpawnRuimte.pasIn(helper.getLevel(), guh, true), "it fits somewhere after the check");
        helper.assertTrue(GuhSpawnRuimte.vrij(helper.getLevel(), guh), "its hitbox is free of blocks");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = "guhspawn", timeoutTicks = 120)
    public static void vastgezetteGuhStiktNiet(GameTestHelper helper) {
        bouwSleuf(helper);
        GuhEntity guh = groteGuhInDeSleuf(helper, true);   // tame: may not shrink, must be moved
        float scale = guh.getGuhScale();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(guh.isAlive(), "the stuck guh is still alive");
            helper.assertTrue(guh.getHealth() >= guh.getMaxHealth(), "and it took no suffocation damage");
            helper.assertTrue(GuhSpawnRuimte.vrij(helper.getLevel(), guh), "it stands free now");
            helper.assertTrue(guh.getGuhScale() == scale, "a tame guh keeps its size");
            helper.succeed();
        });
    }
}
