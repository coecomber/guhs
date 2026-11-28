package nl.juiced.guhs.feature.vadsrijk.zone5.wezens;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Dienstregeling;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Lijnen;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Luchtschepen;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.LuchtschipEntity;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * The fin fall net against the airships (1.10.26 report: "the boat back to zone 4 drops me at the Zuidvin"). Line D's way down
 * flies under the south fin, through the catch box of the fall net; a passenger must stay on board, a real faller is still caught.
 * Run: {@code runGameTestServer -Pgt=Z5VangnetSchip}.
 */
public class Z5VangnetSchipGameTests {
    private static final String BATCH = "vadsrijk_z5vangnet_schip";

    /** The net's box of the tick ({@link Z5Bouw#tick}: zone 5 counts from 60 below y 150) plus the catch rule. */
    private static boolean zouVangen(Vec3 v) {
        return Z5WezensPlekken.inZone(v.x, v.y + 60, v.z) && Z5Bouw.vangplek(v.x, v.y, v.z) != null;
    }

    /** The rule itself: under the south fin you are caught to its lift; on the fin, far below it and elsewhere you are not. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void vangplekRegel(GameTestHelper helper) {
        helper.assertTrue(Z5WezensPlekken.LIFT_ZUID_ONDER.equals(Z5Bouw.vangplek(2565, 99, -622)), "under the south fin: back to its lift");
        helper.assertTrue(Z5Bouw.vangplek(2565, 150, -622) == null, "on the fin (inside its y range): not caught");
        helper.assertTrue(Z5Bouw.vangplek(2565, 30, -622) == null, "more than 90 below the fin: not caught");
        helper.assertTrue(Z5Bouw.vangplek(2500, 99, -622) == null, "beside the fin: not caught");
        // the reason for the airship exemption, reported (not asserted: a re-routed line D may stay clear of the box)
        Lijnen.Lijn d = Lijnen.lijn("D");
        helper.assertTrue(d != null, "line D exists");
        for (boolean heen : new boolean[] {true, false}) {
            Dienstregeling.Pad pad = Dienstregeling.pad(d, heen);
            int n = 0;
            Vec3 eerste = null;
            for (int i = 0; i <= 2000; i++) {
                Vec3 v = pad.op(i / 2000.0);
                if (zouVangen(v)) {
                    n++;
                    eerste = eerste == null ? v : eerste;
                }
            }
            org.slf4j.LoggerFactory.getLogger("guhs").info("Z5VangnetSchip: line D {} crosses the fin net on {}/2001 path samples{}",
                    heen ? "up (z4 -> z5)" : "down (z5 -> z4)", n, eerste == null ? "" : ", first at " + eerste);
        }
        helper.succeed();
    }

    /**
     * A passenger of a flying airship is never caught, even at a spot under the south fin (where line D's way down passes); once
     * off the ship the same player at the same spot is caught and put at the lift.
     */
    // (own batch: the ship flies outside the tiny test area, and neighbouring tests clear the entities in their area)
    @GuhTest(template = "empty", batch = BATCH + "_vlucht", timeoutTicks = 400)
    public static void passagierBlijftAanBoord(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                sl.setBlock(o.offset(x, 2, z), Blocks.STONE.defaultBlockState(), 3);
                sl.setBlock(o.offset(x + 30, 2, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        Lijnen.Haven a = new Lijnen.Haven("haven_t7_t8_vertrek", o.above(3), 270f, "t7");
        Lijnen.Haven b = new Lijnen.Haven("haven_t8_t7_vertrek", o.offset(30, 3, 0), 90f, "t8");
        int periode = 200, duur = 60;
        int fase = (int) ((sl.getServer().getTickCount() + 30) % (2 * periode));   // (mod 2P: ship 0 is the one at a)
        Lijnen.Lijn l = new Lijnen.Lijn("test_vangnet_" + sl.getServer().getTickCount(), "V", a, b,
                List.of(new Vec3(o.getX() + 15.5, o.getY() + 8, o.getZ() + 0.5)), fase, duur, periode, true);
        Lijnen.ALLE.add(l);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.snapTo(a.plek().getX() + 0.5, a.plek().getY(), a.plek().getZ() + 0.5);
        // where line D's way down is under the fin (ship centre ~2565, 99, -622)
        double vx = 2565, vy = 99, vz = -622;
        helper.assertTrue(Z5Bouw.vangbaar(p), "on foot in survival: the net may catch you");
        LuchtschipEntity e = Luchtschepen.schipNu(sl, l, 0);
        helper.assertTrue(e != null, "ship spawned");
        helper.assertTrue(Luchtschepen.instappen(p, e), "boarded");
        helper.assertTrue(!Z5Bouw.vangbaar(p), "on board at the pier: never caught");
        long start = sl.getServer().getTickCount();
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(e.vliegt(), "flying");
            helper.assertTrue(p.getVehicle() == e, "on board");
            helper.assertTrue(!Z5Bouw.vangnet(p, sl, vx, vy, vz), "a passenger under the south fin is NOT caught");
            helper.assertTrue(p.getVehicle() == e, "still on board after the net's check");
            helper.assertTrue(p.getPersistentData().getStringOr(Luchtschepen.TAG_NAAR, "").equals(b.id()), "still flying to the destination");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(sl.getServer().getTickCount() - start > 60, "not yet");
            helper.assertTrue(p.getVehicle() == null, "landed: off the ship");
            helper.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(b.plek())) < 4, "on the pier of b: " + p.position());
            // off the ship the net works again
            helper.assertTrue(Z5Bouw.vangbaar(p), "on foot again: the net may catch you");
            helper.assertTrue(Z5Bouw.vangnet(p, sl, vx, vy, vz), "a faller under the south fin is caught");
            helper.assertTrue(p.blockPosition().distManhattan(Z5WezensPlekken.LIFT_ZUID_ONDER) <= 2, "put at the south fin lift: " + p.position());
            Luchtschepen.ruimOp(sl, l);
            Lijnen.ALLE.remove(l);
        });
    }
}
