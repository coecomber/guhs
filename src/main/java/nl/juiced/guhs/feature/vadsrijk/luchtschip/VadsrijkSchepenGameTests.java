package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * W-SCHEPEN: boarding on foot. Every real harbour sits on its pier tip POI (not a placeholder) with the ship flush beyond it, and
 * on a test pier: the docked ship lies within reach of the tip, a click on the tip boards, stepping off the tip onto the ship
 * boards, and stepping off an empty tip puts you back. Run: {@code runGameTestServer -Pgt=VadsrijkSchepen}.
 * (The map-side checks, every harbour against the real tiles: {@code python tools/vadsrijk/schepen_check.py}.)
 */
public class VadsrijkSchepenGameTests {
    private static final String BATCH = "vadsrijk_schepen";

    /** Every active line's harbours come from a generated POI (a pier tip), and the dock is UIT beyond it, at deck height. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void havensOpDeKaart(GameTestHelper helper) {
        int n = 0;
        for (Lijnen.Lijn l : Lijnen.ALLE) {
            if (l.test()) {
                continue;
            }
            helper.assertTrue(l.actief(), "line " + l.letter() + " flies (its two harbour POIs exist)");
            for (Lijnen.Haven h : new Lijnen.Haven[] {l.a(), l.b()}) {
                BlockPos b = Lijnen.zoek(h.id());
                helper.assertTrue(b != null, h.id() + ": a pier-tip POI on the map (not the placeholder)");
                helper.assertTrue(b.equals(h.plek()), h.id() + ": plek() is the POI");
                Vec3 d = h.dok().subtract(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
                helper.assertTrue(Math.abs(d.y) < 1e-6 && Math.abs(d.horizontalDistance() - Lijnen.Haven.UIT) < 1e-6
                        && d.normalize().dot(h.uit()) > 0.999, h.id() + ": the dock lies UIT straight out from the tip at deck height");
                n++;
            }
        }
        helper.assertTrue(n == 12, "12 harbours (lines 0, A-E): " + n);
        helper.succeed();
    }

    /** A test pier: the ship docks flush at the tip; click the tip / step on board / the safety net at an empty tip. */
    @GuhTest(template = "empty", batch = BATCH + "_pier", timeoutTicks = 100)
    public static void loopplank(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        // a 5 x 5 pier ending at x = o.x (the tip row), open air east of it
        for (int x = -4; x <= 0; x++) {
            for (int z = -2; z <= 2; z++) {
                sl.setBlock(o.offset(x, 2, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        for (int x = 30; x <= 32; x++) {
            sl.setBlock(o.offset(x, 2, 0), Blocks.STONE.defaultBlockState(), 3);
        }
        Lijnen.Haven a = new Lijnen.Haven("haven_t5_t6_vertrek", o.above(3), 270f, "t5");
        Lijnen.Haven b = new Lijnen.Haven("haven_t6_t5_vertrek", o.offset(31, 3, 0), 90f, "t6");
        int periode = 2000;
        int fase = (int) ((sl.getServer().getTickCount() + 600) % (2 * periode));   // ship 0 waits at a for the whole test
        Lijnen.Lijn l = new Lijnen.Lijn("test_plank_" + sl.getServer().getTickCount(), "P", a, b,
                List.of(new Vec3(o.getX() + 15.5, o.getY() + 12, o.getZ() + 0.5)), fase, 200, periode, true);
        Lijnen.ALLE.add(l);
        int k = Dienstregeling.schipAan(l, a, Tijd.nu(sl, l));
        LuchtschipEntity e = Luchtschepen.schipNu(sl, l, 0);
        helper.runAfterDelay(5, () -> {   // (the click boxes follow the ship from its first tick)
            try {
                pier(helper, sl, l, a, k, e);
            } finally {
                Luchtschepen.ruimOp(sl, l);
                Lijnen.ALLE.remove(l);
            }
            helper.succeed();
        });
    }

    private static void pier(GameTestHelper helper, ServerLevel sl, Lijnen.Lijn l, Lijnen.Haven a, int k, LuchtschipEntity e) {
        helper.assertTrue(k == 0, "ship 0 lies at a: " + k);
        helper.assertTrue(e != null && !e.vliegt(), "ship spawned, docked");
        BlockPos tip = a.plek();
        Vec3 voet = new Vec3(tip.getX() + 0.5, tip.getY(), tip.getZ() + 0.5);
        helper.assertTrue(e.position().distanceTo(a.dok()) < 0.01, "the ship lies at its dock: " + e.position() + " vs " + a.dok());
        // within survival reach from the tip: the nearest click box, from the eye of a player on the tip
        Vec3 oog = voet.add(0, 1.62, 0);
        double best = 1e9;
        for (SchipDeel d : e.delen()) {
            AABB bb = d.getBoundingBox();
            double dx = Math.max(Math.max(bb.minX - oog.x, 0), oog.x - bb.maxX);
            double dy = Math.max(Math.max(bb.minY - oog.y, 0), oog.y - bb.maxY);
            double dz = Math.max(Math.max(bb.minZ - oog.z, 0), oog.z - bb.maxZ);
            best = Math.min(best, Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        helper.assertTrue(best <= 1.5, "a click box within 1.5 of the eye on the tip (survival reach 3): " + best);
        // flush: the spinning props (9.5 behind the middle) end exactly on the tip's outer edge (the ship points east here)
        double achter = a.dok().x - 9.5;
        helper.assertTrue(Math.abs(achter - (tip.getX() + 1)) < 0.01, "the props end exactly on the tip's outer edge: " + achter);

        // 1) a right-click on the tip block boards
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.snapTo(voet.x - 1, voet.y, voet.z);
        helper.assertTrue(Loopplank.klik(p, tip.below()), "click on the tip boards");
        helper.assertTrue(p.getVehicle() == e, "on board after the tip click");
        e.uitstappen = true;
        p.stopRiding();
        e.uitstappen = false;
        // 2) stepping off the tip towards the ship boards
        ServerPlayer q = GuhMockPlayer.of(helper);
        q.setGameMode(GameType.SURVIVAL);
        q.snapTo(voet.x + 0.9, voet.y, voet.z + 1);
        helper.assertTrue(Loopplank.stap(sl, q, l, a), "the step was handled");
        helper.assertTrue(q.getVehicle() == e, "on board after stepping onto the ship");
        e.uitstappen = true;
        q.stopRiding();
        e.uitstappen = false;
        // 3) no ship: whoever drops off the tip is put back on it
        Luchtschepen.ruimOp(sl, l);
        helper.assertTrue(Loopplank.aangemeerd(sl, l, a) == null, "the ship is gone");
        ServerPlayer r = GuhMockPlayer.of(helper);
        r.setGameMode(GameType.SURVIVAL);
        r.snapTo(voet.x + 1.4, voet.y - 1.2, voet.z);
        r.fallDistance = 1.2f;
        helper.assertTrue(Loopplank.stap(sl, r, l, a), "the drop was caught");
        helper.assertTrue(r.position().distanceTo(voet) < 0.6 && r.fallDistance == 0, "back on the tip: " + r.position());
        // walking on the pier itself (floor under you) never triggers anything
        r.snapTo(voet.x - 2, voet.y, voet.z);
        helper.assertTrue(!Loopplank.stap(sl, r, l, a), "nothing happens on the pier");
    }
}
