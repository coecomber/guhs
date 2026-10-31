package nl.juiced.guhs.feature.kermisritten;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the turning kermis rides (1.8.1, batch "kermisritten"): every ride ends in its rest pose, turning the
 * controller turns the ride, a rider goes round and comes out at the gate with a kermisbon, nobody gets off halfway, and a
 * controller on its own (no stamp) or broken away causes no trouble.
 */
public class KermisrittenGameTests {
    private static final String BATCH = "kermisritten", KAMER = "vadskracht_test_kamer";
    private static final BlockPos PLEK = new BlockPos(4, 2, 4);

    private static KermisritBlockEntity zet(GameTestHelper helper, KermisritSoort soort, Direction facing) {
        helper.setBlock(PLEK, KermisrittenFeature.blok(soort).defaultBlockState().setValue(KermisritBlock.FACING, facing));
        // the seats hang outside the test room: keep the chunks around it running
        BlockPos abs = helper.absolutePos(PLEK);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.getLevel().setChunkForced((abs.getX() >> 4) + dx, (abs.getZ() >> 4) + dz, true);
            }
        }
        return (KermisritBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(PLEK));
    }

    /** The geometry alone: after a whole ride every seat is back where it started, and halfway it is somewhere else. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ritEindigtInRustStand(GameTestHelper helper) {
        for (KermisritSoort s : KermisritSoort.values()) {
            for (int i = 0; i < s.stoelen; i++) {
                Vec3 begin = s.plek(i, 0), eind = s.plek(i, s.profiel.duur()), midden = s.plek(i, s.profiel.duur() * 0.37);
                helper.assertTrue(begin.distanceTo(eind) < 1e-6, s.id + " seat " + i + " ends at rest");
                helper.assertTrue(begin.distanceTo(midden) > 0.01, s.id + " seat " + i + " moves");
            }
            helper.assertTrue(s.profiel.hoek(1) < s.profiel.hoek(2) && s.profiel.vaartNu(s.profiel.duur() / 2.0) == 1, s.id + " speeds up");
        }
        helper.succeed();
    }

    /** No seat goes through the floor, and the Booster reaches its height. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void stoelenBlijvenBovenDeVloer(GameTestHelper helper) {
        double top = 0;
        for (KermisritSoort s : KermisritSoort.values()) {
            for (int i = 0; i < s.stoelen; i++) {
                for (int t = 0; t < s.profiel.duur(); t += 7) {
                    double y = s.plek(i, t).y;
                    helper.assertTrue(y >= 1, s.id + " seat " + i + " stays above the floor: " + y);
                    if (s == KermisritSoort.BOOSTER) {
                        top = Math.max(top, y);
                    }
                }
            }
        }
        helper.assertTrue(top > 55, "the Booster goes high: " + top);
        helper.succeed();
    }

    /** Facing east: the gate side is east, the ride's frame turned with it. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void draaitMetFacing(GameTestHelper helper) {
        Vec3 v = new Vec3(1, 2, 5);
        helper.assertTrue(KermisritSoort.draai(v, Direction.SOUTH).equals(v), "south is the design");
        helper.assertTrue(KermisritSoort.draai(v, Direction.EAST).equals(new Vec3(5, 2, -1)), "east");
        helper.assertTrue(KermisritSoort.draai(v, Direction.WEST).equals(new Vec3(-5, 2, 1)), "west");
        helper.assertTrue(KermisritSoort.draai(v, Direction.NORTH).equals(new Vec3(-1, 2, -5)), "north");
        KermisritBlockEntity rit = zet(helper, KermisritSoort.TWISTER, Direction.EAST);
        helper.assertTrue(rit.uitstapPlek().x > rit.getBlockPos().getX() + 5, "the gate is on the east side");
        helper.succeed();
    }

    /** A whole ride: on at the post, off is refused halfway, out at the gate with a kermisbon. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 1200)
    public static void ritMetSpeler(GameTestHelper helper) {
        KermisritBlockEntity rit = zet(helper, KermisritSoort.TWISTER, Direction.SOUTH);
        ServerLevel level = helper.getLevel();
        var speler = helper.makeMockServerPlayerInLevel();
        speler.setGameMode(GameType.SURVIVAL);
        speler.teleportTo(level, rit.getBlockPos().getX() + 0.5, rit.getBlockPos().getY() + 1, rit.getBlockPos().getZ() + 0.5,
                java.util.Set.of(), 0, 0, true);
        helper.startSequence()
                .thenExecuteAfter(10, () -> rit.zorgVoorStoelen(level))
                .thenExecuteAfter(1, () -> {
                    helper.assertTrue(rit.stoel(-1) != null && rit.stoel(0) != null && rit.stoel(8) != null, "seats and post were made");
                    rit.klik(speler, -1);
                    helper.assertTrue(speler.getVehicle() == rit.stoel(0), "the post puts you in the first free seat");
                    rit.begin(level, level.getGameTime());
                })
                .thenExecuteAfter(100, () -> {
                    helper.assertTrue(rit.rijdt(), "it rides");
                    helper.assertTrue(speler.getVehicle() == rit.stoel(0), "still sitting");
                    helper.assertTrue(speler.position().distanceTo(rit.wereldPlek(0, 0)) > 0.5, "the rider moved along");
                    speler.setShiftKeyDown(true);
                    speler.stopRiding();
                    speler.setShiftKeyDown(false);
                    helper.assertTrue(speler.isPassenger(), "no sneaking off halfway");
                    rit.klik(helper.makeMockServerPlayerInLevel(), 1);
                })
                .thenWaitUntil(() -> helper.assertTrue(!rit.rijdt(), "the ride ends"))
                .thenExecute(() -> {
                    helper.assertTrue(!speler.isPassenger(), "everybody out");
                    helper.assertTrue(speler.position().distanceTo(rit.uitstapPlek()) < 1.5, "at the gate");
                    helper.assertTrue(speler.getInventory().countItem(ModItems.KERMISBON.get()) == 1, "a kermisbon");
                })
                .thenSucceed();
    }

    /** Controller gone: its seats go, without a trace. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void blokWegRuimtStoelenOp(GameTestHelper helper) {
        KermisritBlockEntity rit = zet(helper, KermisritSoort.ZWEEFMOLEN, Direction.SOUTH);
        ServerLevel level = helper.getLevel();
        KermisritStoel[] stoel = new KermisritStoel[1];
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    rit.zorgVoorStoelen(level);
                    stoel[0] = rit.stoel(3);
                    helper.assertTrue(stoel[0] != null, "a seat");
                    helper.setBlock(PLEK, Blocks.AIR);
                })
                .thenWaitUntil(() -> helper.assertTrue(stoel[0].isRemoved(), "the seat is gone"))
                .thenSucceed();
    }
}
