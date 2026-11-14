package nl.juiced.guhs.feature.vadsrijk.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeon;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.Ondergrond;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoortConfig;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.2: back where you left ({@link Terugkeer}). Re-entry next to the kept portal (out of it, facing away), the first entry
 * (nothing kept: the normal start), a kept spot in a zone the gate closed (the arrival of the highest open zone), a spot without a
 * floor, and dungeons (leaving from inside keeps the spot before the door). The world checks run in the real Vadsrijk
 * dimension on a small platform built high in the air (zone 1 coordinates), or in the test level when the server has no Vadsrijk.
 * Run: {@code -Pgt=VadsrijkTerugkeer}.
 */
public class VadsrijkTerugkeerGameTests {
    private static final String B = "vadsrijk_terugkeer";
    /** A spot in zone 1 (inside the barrier's free area), high in the air so nothing of the map is in the way. */
    static final BlockPos PLAT = new BlockPos(200, 230, 200), PLAT2 = new BlockPos(260, 230, 200);

    /** A 9x9 stone platform with air above at {@code m} (feet at m.y), and a 1x2 portal of ours standing on it at m. */
    private static void platform(ServerLevel l, BlockPos m, boolean portaal) {
        for (int cx = (m.getX() - 8) >> 4; cx <= (m.getX() + 8) >> 4; cx++) {
            for (int cz = (m.getZ() - 8) >> 4; cz <= (m.getZ() + 8) >> 4; cz++) {
                l.getChunk(cx, cz);
            }
        }
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                l.setBlock(m.offset(x, -1, z), Math.abs(x) <= 4 && Math.abs(z) <= 4 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
                for (int y = 0; y <= 4; y++) {
                    l.setBlock(m.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
        }
        if (portaal) {
            var st = WereldRegistry.PORTAAL.get().defaultBlockState().setValue(PortaalBlok.AXIS, Direction.Axis.X).setValue(PortaalBlok.WAKKER, true);
            l.setBlock(m, st, 2 | 16);
            l.setBlock(m.above(), st, 2 | 16);
        }
    }

    /**
     * The Vadsrijk when this test server has it, else the test level itself: the choice only reads blocks of the level it is
     * given plus pure coordinate rules (zone gate, barriers, dungeons), so the same platform works in both.
     */
    private static ServerLevel wereld(GameTestHelper helper) {
        ServerLevel vr = Vadsrijk.level(helper.getLevel().getServer());
        return vr != null ? vr : helper.getLevel();
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void terugBijHetPortaal(GameTestHelper helper) {
        ServerLevel vr = wereld(helper);
        ServerPlayer p = speler(helper);
        // the first entry: nothing kept, so the normal start
        Terugkeer.zet(p, null);
        helper.assertTrue(Terugkeer.ingang(p, vr, BlockPos.ZERO) == null, "the first entry: the normal start (the jetty)");
        platform(vr, PLAT, true);
        Terugkeer.zet(p, PLAT);
        Terugkeer.Doel d = Terugkeer.ingang(p, vr, BlockPos.ZERO);
        helper.assertTrue(d != null && "terug".equals(d.waarom()), "the kept portal spot is used: " + d);
        BlockPos staat = BlockPos.containing(d.pos());
        helper.assertTrue(d.pos().distanceTo(Vec3.atBottomCenterOf(PLAT)) <= 5, "next to the portal: " + d.pos());
        helper.assertTrue(!Terugkeer.isPortaal(vr.getBlockState(staat)) && !Terugkeer.isPortaal(vr.getBlockState(staat.above())), "not in the portal");
        double dx = staat.getX() - PLAT.getX(), dz = staat.getZ() - PLAT.getZ();
        helper.assertTrue(dx * dx + dz * dz >= 4, "at least 2 blocks from the portal (no instant re-trigger): " + staat);
        double kijkX = -Math.sin(Math.toRadians(d.yaw())), kijkZ = Math.cos(Math.toRadians(d.yaw()));
        helper.assertTrue(kijkX * dx + kijkZ * dz > 0, "facing away from the portal (yaw " + d.yaw() + ", offset " + dx + "," + dz + ")");
        helper.assertTrue(vr.getBlockState(staat.below()).is(Blocks.STONE), "on the floor");
        // the survival side does the same with the entry spot inside a portal
        Terugkeer.Doel uit = Terugkeer.uitPortaal(vr, Vec3.atBottomCenterOf(PLAT));
        helper.assertTrue(uit != null && !Terugkeer.isPortaal(vr.getBlockState(BlockPos.containing(uit.pos()))), "out of the portal on the way back too: " + uit);
        helper.assertTrue(Terugkeer.uitPortaal(vr, Vec3.atBottomCenterOf(PLAT.offset(3, 0, 3))) == null, "no portal there: the spot stays as it is");
        Terugkeer.zet(p, null);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 300)
    public static void geslotenZoneEnGeenVloer(GameTestHelper helper) {
        ServerLevel vr = wereld(helper);
        ServerPlayer p = speler(helper);
        Integer oud = ZonePoortConfig.TEST;
        boolean streng = ZonePoort.TEST_STRENG;
        try {
            ZonePoortConfig.TEST = 1;
            ZonePoort.TEST_STRENG = true;
            // kept in zone 2 (Kandijkom) while the gate only has zone 1 open: De Haven (zone 1's arrival)
            BlockPos kandijkom = new BlockPos(704, 91, -1060);
            helper.assertTrue(ZonePoort.zoneOp(kandijkom.getX(), kandijkom.getY(), kandijkom.getZ()) == 2, "the spot is in zone 2");
            Terugkeer.zet(p, kandijkom);
            Terugkeer.Doel d = Terugkeer.ingang(p, vr, BlockPos.ZERO);
            helper.assertTrue(d != null && "zone_dicht".equals(d.waarom()), "a closed zone: fallback (" + d + ")");
            Vec3 haven = Vec3.atBottomCenterOf(WereldPlekken.HAVEN_LANDING);
            helper.assertTrue(Math.hypot(d.pos().x - haven.x, d.pos().z - haven.z) <= 6, "to De Haven, zone 1's arrival: " + d.pos());
            // kept on a spot without any floor (air high above zone 1): the same fallback
            platform(vr, PLAT2, false);
            BlockPos lucht = PLAT2.offset(0, 40, 0);
            Terugkeer.zet(p, lucht);
            d = Terugkeer.ingang(p, vr, BlockPos.ZERO);
            helper.assertTrue(d != null && "ongeldig".equals(d.waarom()) && Math.hypot(d.pos().x - haven.x, d.pos().z - haven.z) <= 6,
                    "no floor: De Haven (" + d + ")");
            // the platform without a portal: just the spot itself
            Terugkeer.zet(p, PLAT2);
            d = Terugkeer.ingang(p, vr, BlockPos.ZERO);
            helper.assertTrue(d != null && "terug".equals(d.waarom()) && BlockPos.containing(d.pos()).equals(PLAT2), "a good spot without a portal: right there (" + d + ")");
        } finally {
            ZonePoortConfig.TEST = oud;
            ZonePoort.TEST_STRENG = streng;
            Terugkeer.zet(p, null);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void dungeonsWordenNietOnthouden(GameTestHelper helper) {
        // leaving from inside a dungeon keeps the spot in front of its main door
        int getest = 0;
        for (Dungeon dg : Dungeons.alle()) {
            DungeonDeurData.Deur deur = DungeonDeur.hoofd(dg.id());
            if (deur == null || !dg.bevat(deur.binnen().getX() + 0.5, deur.binnen().getY(), deur.binnen().getZ() + 0.5)) {
                continue;
            }
            BlockPos b = deur.binnen();
            BlockPos k = Terugkeer.uitgangVoor(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, false);
            helper.assertTrue(deur.voor().equals(k), dg.id() + ": leaving from inside keeps the door's outside spot, not the instance (" + k + ")");
            helper.assertTrue(Terugkeer.dungeonOp(deur.voor().getX() + 0.5, deur.voor().getY(), deur.voor().getZ() + 0.5) == null,
                    dg.id() + ": the door's outside spot is not in the dungeon");
            getest++;
        }
        helper.assertTrue(getest > 0, "at least one dungeon checked");
        // other spots that are never kept
        helper.assertTrue(Terugkeer.uitgangVoor(10, 70, 10, true) == null, "during a scene nothing is kept");
        helper.assertTrue(Terugkeer.uitgangVoor(Ondergrond.KAMP.getX(), Ondergrond.KAMP.getY(), Ondergrond.KAMP.getZ(), false) == null, "the proeftuin is never kept");
        helper.assertTrue(Terugkeer.uitgangVoor(90000, 70, 90000, false) == null, "outside the island sea nothing is kept");
        helper.assertTrue(new BlockPos(10, 70, 10).equals(Terugkeer.uitgangVoor(10.4, 70, 10.6, false)), "a normal spot is kept as it is");
        helper.succeed();
    }
}
