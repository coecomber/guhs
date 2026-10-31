package nl.juiced.guhs.feature.kermisritten2;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModItems;

/** 1.8.1: the Botsguhs' bumps and prizes, the anchors' turning with the stamp, and the Spookguhhuis' track. */
public class Ritten2GameTests {
    private static final String BATCH = "kermisritten2", KAMER = "vadskracht_test_kamer";

    private static BotsguhEntity auto(GameTestHelper helper, AABB baan, double x, double z) {
        ServerLevel level = helper.getLevel();
        BotsguhEntity a = Ritten2Feature.BOTSGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        a.setPos(x, baan.minY, z);
        a.zetBaan(BlockPos.containing(baan.getCenter()), baan, baan.getCenter(), 0);
        level.addFreshEntity(a);
        return a;
    }

    /** A hard hit counts for the driver and passes the speed on; a soft nudge doesn't count. */
    @SuppressWarnings("removal")
    @GuhTest(template = KAMER, batch = BATCH)
    public static void botsTelt(GameTestHelper helper) {
        BlockPos o = helper.absolutePos(new BlockPos(5, 1, 5));
        AABB baan = new AABB(o.getX() - 20, o.getY(), o.getZ() - 20, o.getX() + 20, o.getY() + 5, o.getZ() + 20);
        BotsguhEntity a = auto(helper, baan, o.getX(), o.getZ()), b = auto(helper, baan, o.getX() + 1.2, o.getZ());
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.startRiding(a, true, true);
        a.zetVaart(new Vec3(0.02, 0, 0));
        BotsguhEntity.bots(a, b);
        helper.assertTrue(a.botsingen() == 0, "a nudge is no bump");
        a.setPos(o.getX(), a.getY(), o.getZ());
        b.setPos(o.getX() + 1.2, b.getY(), o.getZ());
        a.zetVaart(new Vec3(0.3, 0, 0));
        b.zetVaart(Vec3.ZERO);
        helper.assertTrue(BotsguhEntity.bots(a, b), "a hard hit is a bump");
        helper.assertTrue(a.botsingen() == 1, "the driver counts it");
        helper.assertTrue(b.vaart().x > 0.25 && a.vaart().x < 0.05, "the speed went to the other car");
        helper.assertTrue(b.getX() - a.getX() >= BotsguhEntity.STRAAL * 2 - 1e-6, "pushed apart");
        p.stopRiding();
        helper.succeed();
    }

    /** The walls hold the car, whatever you do. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void muurHoudt(GameTestHelper helper) {
        BlockPos o = helper.absolutePos(new BlockPos(5, 1, 5));
        AABB baan = new AABB(o.getX() - 3, o.getY(), o.getZ() - 3, o.getX() + 3, o.getY() + 5, o.getZ() + 3);
        BotsguhEntity a = auto(helper, baan, o.getX(), o.getZ());
        for (int i = 0; i < 400; i++) {
            a.rijd(1f, i % 50 == 0 ? 1f : 0.2f, BotsguhEntity.TOP);
            helper.assertTrue(a.getX() >= baan.minX && a.getX() <= baan.maxX && a.getZ() >= baan.minZ && a.getZ() <= baan.maxZ, "stays on the floor");
        }
        helper.assertTrue(BotsguhEntity.bonnenVoor(4) == 0 && BotsguhEntity.bonnenVoor(5) == 1 && BotsguhEntity.bonnenVoor(15) == 2, "bump prizes");
        helper.succeed();
    }

    /** A block offset and a point offset agree in all four turns of the stamp. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void draaitMee(GameTestHelper helper) {
        BlockPos a = new BlockPos(100, 64, -40);
        for (Rotation r : Rotation.values()) {
            helper.assertTrue(BlockPos.containing(Anker.punt(a, r, 7, 1, -3)).equals(Anker.blok(a, r, 7, 1, -3)), "block and point agree at " + r);
        }
        helper.assertTrue(Anker.draai(Direction.EAST, Direction.NORTH) == Rotation.COUNTERCLOCKWISE_90, "east to north is a quarter turn back");
        helper.succeed();
    }

    /** The anchor blocks are real block entities, and the floor turns with the block. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void baanDraait(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Ritten2Feature.BOTSBAAN.get().defaultBlockState().setValue(AnkerBlock.FACING, Direction.NORTH));
        AABB noord = helper.getBlockEntity(pos, BotsbaanBlockEntity.class).binnen();
        helper.assertTrue(noord.getXsize() == 23 && noord.getZsize() == 16, "the floor is 23 by 16: " + noord);
        helper.setBlock(pos, Ritten2Feature.BOTSBAAN.get().defaultBlockState().setValue(AnkerBlock.FACING, Direction.EAST));
        AABB oost = helper.getBlockEntity(pos, BotsbaanBlockEntity.class).binnen();
        helper.assertTrue(oost.getXsize() == 16 && oost.getZsize() == 23, "and turns with its anchor: " + oost);
        helper.succeed();
    }

    /** A whole ride: the cart leaves, the ghosts pop up, at the way out you get off with a kermisbon; under way you can't. */
    @SuppressWarnings("removal")
    @GuhTest(template = KAMER, batch = BATCH)
    public static void heleRit(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Ritten2Feature.SPOOKHUIS.get().defaultBlockState().setValue(AnkerBlock.FACING, Direction.EAST));
        SpookhuisBlockEntity huis = helper.getBlockEntity(pos, SpookhuisBlockEntity.class);
        SpookkarEntity kar = Ritten2Feature.SPOOKKAR.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        kar.zetSpoor(huis, true);
        helper.getLevel().addFreshEntity(kar);
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.startRiding(kar, true, true);
        boolean onderweg = false;
        for (int i = 0; i < 5000 && kar.staat() != SpookkarEntity.TERUG; i++) {
            kar.stap(huis);
            onderweg |= kar.opWeg();
        }
        helper.assertTrue(onderweg && kar.staat() == SpookkarEntity.TERUG, "the ride ends: state " + kar.staat());
        helper.assertTrue(p.getVehicle() == null, "out at the end");
        helper.assertTrue(p.getInventory().countItem(ModItems.KERMISBON.get()) == 1, "with a kermisbon");
        for (int i = 0; i < 400 && kar.staat() != SpookkarEntity.WACHT; i++) {
            kar.stap(huis);
        }
        helper.assertTrue(kar.staat() == SpookkarEntity.WACHT && kar.afstand() == 0, "back at the station");
        helper.succeed();
    }

    /** The track never crosses itself and stays out of the partitions, the tower and the walls of the design. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void spoorVrij(GameTestHelper helper) {
        int[][] s = SpookhuisBlockEntity.SPOOR;
        Set<Long> gezien = new HashSet<>();
        for (int i = 0; i < s.length; i++) {
            int[] a = s[i], b = s[(i + 1) % s.length];
            helper.assertTrue(a[0] == b[0] || a[1] == b[1], "straight stretches");
            int n = Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
            for (int k = 0; k < n; k++) {
                int x = a[0] + Integer.signum(b[0] - a[0]) * k, z = a[1] + Integer.signum(b[1] - a[1]) * k;
                helper.assertTrue(gezien.add(x * 1000L + z), "the track crosses itself at " + x + "," + z);
                // the house is x 0..17, z 0..22; partitions as in kermiskit_modules.spookhuis
                helper.assertFalse(x == 5 && z < 19, "through the first partition at z " + z);
                helper.assertFalse(x == 10 && z > 3, "through the second partition at z " + z);
                helper.assertTrue(Math.hypot(x - 3, z - 3) > 3.3, "through the tower at " + x + "," + z);
                if (x == 0 || x == 17) {
                    helper.assertTrue(x == 17 && (Math.abs(z - 8) <= 1 || Math.abs(z - 14) <= 1), "through the wall at " + x + "," + z);
                }
            }
        }
        for (double[] sc : SpookhuisBlockEntity.SCHRIK) {
            helper.assertTrue(sc[3] < SpookhuisBlockEntity.REGELS, "a line for every ghost");
        }
        helper.succeed();
    }
}
