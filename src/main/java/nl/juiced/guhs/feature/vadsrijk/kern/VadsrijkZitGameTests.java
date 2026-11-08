package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhSeatEntity;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;

/**
 * W-SPEL: "Bij Kloofmoeder Roosje ging ik op een magenta kussen zitten maar dan val ik helemaal door de grond naar beneden tot y
 * level 1." The guh seat (every kussen, zitzak and stoel of guh furniture) was refused by {@link Buitenstaanders} in the Vadsrijk:
 * the player still "rode" it, was put 0.4 into the floor and fell through. Checks: the seat belongs in the Vadsrijk, sitting really
 * sits on a seat in the level, and standing up always ends on a free spot at or above the cushion (also under a low ceiling).
 * Run: {@code runGameTestServer -Pgt=VadsrijkZit}.
 */
public class VadsrijkZitGameTests {
    private static final String BATCH = "vadsrijk_zit";

    @GuhTest(template = "empty", batch = BATCH)
    public static void stoelHoortErbij(GameTestHelper helper) {
        helper.assertTrue(Buitenstaanders.hoortErbij(ModEntities.GUH_SEAT.get()), "the guh seat may be in the Vadsrijk");
        helper.succeed();
    }

    /** Sit on a magenta cushion on a one-block floor above air (like Roosje's dais) and stand up: never below the cushion. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void kussenZitEnOpstaan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos vloer = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(vloer, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        BlockPos kussen = vloer.above();
        BlockState s = ModBlocks.KUSSENS.get(DyeColor.MAGENTA).get().defaultBlockState();
        level.setBlockAndUpdate(kussen, s);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.teleport(new TeleportTransition(level, Vec3.atBottomCenterOf(kussen.east()), Vec3.ZERO, 0f, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        s.useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(kussen), Direction.UP, kussen, false));
        helper.assertTrue(p.getVehicle() instanceof GuhSeatEntity seat && !seat.isRemoved() && seat.level() == level, "sitting rides a seat that is in the level");
        p.stopRiding();
        helper.assertTrue(!p.isPassenger(), "stood up");
        helper.assertTrue(p.getY() >= kussen.getY(), "stood up at or above the cushion (y " + p.getY() + " vs " + kussen.getY() + ")");
        helper.assertTrue(vrij(level, p), "standing in free space at " + p.position() + " (cushion " + kussen + ")");

        // a low ceiling right above the cushion: stand up next to it, still not inside or under the floor
        level.setBlockAndUpdate(kussen.above(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(kussen.east(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(vloer.east(), Blocks.SMOOTH_QUARTZ.defaultBlockState());
        s.useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(kussen), Direction.UP, kussen, false));
        helper.assertTrue(p.isPassenger(), "sat down again");
        p.stopRiding();
        helper.assertTrue(p.getY() >= vloer.getY() + 1 - 1e-3, "under a ceiling: still on the floor (y " + p.getY() + ")");
        helper.assertTrue(vrij(level, p), "under a ceiling: in free space at " + p.position());
        helper.succeed();
    }

    /** The same in the real Vadsrijk dimension, when this server has it: the seat joins the level there. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void kussenInHetVadsrijk(GameTestHelper helper) {
        ServerLevel vr = Vadsrijk.level(helper.getLevel().getServer());
        if (vr == null) {
            helper.succeed();
            return;
        }
        BlockPos vloer = new BlockPos(24, 200, 24);
        vr.getChunk(vloer);
        BlockState oudVloer = vr.getBlockState(vloer), oudKussen = vr.getBlockState(vloer.above());
        vr.setBlockAndUpdate(vloer, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        BlockState s = ModBlocks.KUSSENS.get(DyeColor.MAGENTA).get().defaultBlockState();
        vr.setBlockAndUpdate(vloer.above(), s);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.teleport(new TeleportTransition(vr, Vec3.atBottomCenterOf(vloer.above()).add(1, 0, 0), Vec3.ZERO, 0f, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        try {
            s.useWithoutItem(vr, p, new BlockHitResult(Vec3.atCenterOf(vloer.above()), Direction.UP, vloer.above(), false));
            helper.assertTrue(p.getVehicle() instanceof GuhSeatEntity seat && !seat.isRemoved() && vr.getEntity(seat.getId()) == seat,
                    "in the Vadsrijk the seat joins the level (was refused: the player fell through the floor)");
            p.stopRiding();
            helper.assertTrue(p.getY() >= vloer.getY() + 1, "stood up on the cushion, not below the floor (y " + p.getY() + ")");
        } finally {
            p.stopRiding();
            vr.setBlockAndUpdate(vloer.above(), oudKussen);
            vr.setBlockAndUpdate(vloer, oudVloer);
        }
        helper.succeed();
    }

    /** No block collides with the standing player (entities do not count). */
    private static boolean vrij(ServerLevel level, ServerPlayer p) {
        net.minecraft.world.phys.AABB box = p.getType().getDimensions().makeBoundingBox(p.position()).deflate(0.01);
        for (net.minecraft.world.phys.shapes.VoxelShape s : level.getBlockCollisions(null, box)) {
            if (!s.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
