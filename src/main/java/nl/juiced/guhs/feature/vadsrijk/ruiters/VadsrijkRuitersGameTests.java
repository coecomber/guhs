package nl.juiced.guhs.feature.vadsrijk.ruiters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-RUITERS: the riding guhs. {@code routesKloppen}: every planned route is there, in its zone, without jumps (no piece longer
 * than the planner allows, no steeper than a stair), the spiral and the stair really climb, the timeline never teleports a rider and
 * the riders of a route are spread out. {@code geenDubbelen}: a manager on a test route keeps exactly one pair per rider through
 * ticks, a "reload" (the manager forgets its pairs), a foreign rider entity (refused) and hide/show; riders sit on their Pluimguh.
 * Run: {@code runGameTestServer -Pgt=VadsrijkRuiters}.
 */
public class VadsrijkRuitersGameTests {
    private static final String BATCH = "vadsrijk_ruiters";

    @GuhTest(template = "empty", batch = BATCH)
    public static void routesKloppen(GameTestHelper helper) {
        List<RuiterRoute> alle = RuiterRoutes.alle();
        for (String id : new String[] {"z3_rijpad", "z3_wenteltrap", "z3_honingtak", "z3_tak_westtak", "z3_tak_molenhoek", "z3_tak_lianenbrug",
                "z3_pluimstal", "z3_oefenbaan", "z3_wortelweg", "z3_stalweg", "z3_schimmelpad", "z1_koerier", "z1_hoofdweg"}) {
            helper.assertTrue(RuiterRoutes.van(id) != null, "route present: " + id);
        }
        int z1 = 0, z3 = 0;
        for (RuiterRoute r : alle) {
            List<Vec3> p = r.punten();
            for (int i = 1; i < p.size() && r.modus != RuiterRoute.Modus.STAL; i++) {   // (stable spots are separate places)
                Vec3 a = p.get(i - 1), b = p.get(i);
                double hor = Math.hypot(b.x - a.x, b.z - a.z);
                helper.assertTrue(hor <= 18.5, r.id + ": piece " + i + " too long (" + hor + ")");
                helper.assertTrue(Math.abs(b.y - a.y) <= 1.0 + 0.6 * hor, r.id + ": piece " + i + " too steep");
            }
            for (Vec3 v : p) {
                if (r.zone == 3) {
                    helper.assertTrue(v.x >= 1024 && v.x <= 1727 && v.z >= -1024 && v.z <= -129, r.id + " outside zone 3: " + v);
                } else {
                    helper.assertTrue(v.x >= -400 && v.x <= 400 && v.z >= -400 && v.z <= 1200, r.id + " outside zone 1: " + v);
                }
            }
            if (r.zone == 1) {
                z1 += r.ruiters;
            } else {
                z3 += r.ruiters;
            }
            // the timeline: a rider never jumps more than its speed per tick (+ a bit), riders of a route are spread out
            if (r.modus != RuiterRoute.Modus.STAL) {
                for (int k = 0; k < r.ruiters; k++) {
                    Vec3 vorig = null;
                    for (long t = 0; t < r.periode() + 5; t += 1) {
                        Vec3 nu = r.stand(t, k).pos();
                        if (vorig != null) {
                            helper.assertTrue(nu.distanceTo(vorig) <= r.snelheid * 1.05 + 0.01 || closedLoopWrap(r, vorig, nu),
                                    r.id + "#" + k + " jumps at t " + t + ": " + vorig + " -> " + nu);
                        }
                        vorig = nu;
                    }
                }
                for (int k = 1; k < r.ruiters; k++) {
                    boolean uiteen = false;   // (two riders may pass each other once in a while: look at a few moments)
                    for (long t : new long[] {1000, 2600, 4100, 7300}) {
                        uiteen |= r.stand(t, k).pos().distanceTo(r.stand(t, k - 1).pos()) > 8;
                    }
                    helper.assertTrue(uiteen, r.id + ": riders " + k + " and " + (k - 1) + " bunch up");
                }
            }
        }
        helper.assertTrue(z1 >= 2 && z3 >= 15, "riders per zone: z1 " + z1 + ", z3 " + z3);
        double laag = 999, hoog = -999;
        for (Vec3 v : RuiterRoutes.van("z3_rijpad").punten()) {
            laag = Math.min(laag, v.y);
            hoog = Math.max(hoog, v.y);
        }
        helper.assertTrue(laag < 90 && hoog > 225, "the riding spiral climbs from the ground to kruin 1: " + laag + " .. " + hoog);
        laag = 999;
        hoog = -999;
        for (Vec3 v : RuiterRoutes.van("z3_wenteltrap").punten()) {
            laag = Math.min(laag, v.y);
            hoog = Math.max(hoog, v.y);
        }
        helper.assertTrue(laag < 85 && hoog > 225, "the inner stair goes from the Schorsstad to kruin 1: " + laag + " .. " + hoog);
        helper.succeed();
    }

    /** The last point of a loop = its first: the wrap from the end to the start is not a jump. */
    private static boolean closedLoopWrap(RuiterRoute r, Vec3 a, Vec3 b) {
        return r.modus == RuiterRoute.Modus.RONDJE && a.distanceTo(b) < 1.5 * r.snelheid + 0.5;
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void geenDubbelen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        List<Vec3> pts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            pts.add(new Vec3(o.getX() + 0.5 + i, o.getY(), o.getZ() + 0.5));
        }
        RuiterRoute test = new RuiterRoute("test_ruiters", 3, RuiterRoute.Modus.HEENWEER, 2, 0.1, pts);
        RuiterRoute stal = new RuiterRoute("test_stal", 3, RuiterRoute.Modus.STAL, 2, 0, List.of(new Vec3(o.getX() + 0.5, o.getY(), o.getZ() + 3.5),
                new Vec3(o.getX() + 4.5, o.getY(), o.getZ() + 3.5)));
        Ruiters.Beheer b = new Ruiters.Beheer(() -> List.of(test, stal));
        Ruiters.BEHEERDERS.add(b);
        AABB doos = new AABB(o).inflate(16);
        List<Vec3> kijker = List.of(Vec3.atCenterOf(o));
        try {
            for (int i = 0; i < 5; i++) {
                b.tick(sl, kijker);
            }
            helper.assertTrue(b.aantal() == 4, "4 pairs shown: " + b.aantal());
            helper.assertTrue(tel(sl, doos, RuiterPluim.class) == 4 && tel(sl, doos, RuiterGuh.class) == 4,
                    "4 Pluimguhs + 4 guhs: " + tel(sl, doos, RuiterPluim.class) + " / " + tel(sl, doos, RuiterGuh.class));
            Ruiters.Paar p0 = b.paar("test_ruiters#0");
            helper.assertTrue(p0 != null && p0.guh != null && p0.guh.getVehicle() == p0.pluim, "the rider sits on its Pluimguh");
            Ruiters.Paar g = b.paar("test_stal#1");
            helper.assertTrue(g != null && g.verzorger != null && g.guh == null && g.verzorger.getVehicle() == null, "a groomer next to a Pluimguh");

            // a rider entity nobody spawned (as if loaded back from somewhere) is refused
            RuiterPluim vreemd = RuiterRegistry.PLUIM.get().create(sl, EntitySpawnReason.EVENT);
            vreemd.snapTo(o.getX() + 2, o.getY(), o.getZ() + 2, 0, 0);
            helper.assertTrue(!sl.addFreshEntity(vreemd) && tel(sl, doos, RuiterPluim.class) == 4, "a foreign rider entity is refused");

            // "reload": the manager forgets its pairs but the old entities are still there -> new pairs + the sweep removes the old ones
            Set<String> sleutels = Set.copyOf(b.paren.keySet());
            List<Ruiters.Paar> oud = new ArrayList<>(b.paren.values());
            b.paren.clear();
            b.tick(sl, kijker);
            int weg = Ruiters.veeg(sl);
            helper.assertTrue(weg == 8, "the sweep removes the 4 old pairs (8 entities): " + weg);
            helper.assertTrue(b.paren.keySet().equals(sleutels) && tel(sl, doos, RuiterPluim.class) == 4 && tel(sl, doos, RuiterGuh.class) == 4,
                    "still exactly one per rider after a reload: " + tel(sl, doos, RuiterPluim.class));
            for (Ruiters.Paar p : oud) {
                helper.assertTrue(p.pluim.isRemoved(), "old pair gone");
            }
            for (int i = 0; i < 40; i++) {   // ride a while: still 4, nobody doubled
                b.tick(sl, kijker);
            }
            helper.assertTrue(tel(sl, doos, RuiterPluim.class) == 4 && tel(sl, doos, RuiterGuh.class) == 4, "still 4 after riding");

            // nobody near: all gone; back again: exactly one per rider
            b.tick(sl, List.of());
            helper.assertTrue(b.aantal() == 0 && tel(sl, doos, RuiterPluim.class) == 0 && tel(sl, doos, RuiterGuh.class) == 0, "hidden: all gone");
            b.tick(sl, kijker);
            b.tick(sl, kijker);
            helper.assertTrue(tel(sl, doos, RuiterPluim.class) == 4 && tel(sl, doos, RuiterGuh.class) == 4, "shown again: 4 + 4");
            // the budget
            b.allesWeg();
            b.budget = 1;
            b.tick(sl, kijker);
            helper.assertTrue(tel(sl, doos, RuiterPluim.class) == 1, "budget 1 = one rider");
        } finally {
            b.allesWeg();
            Ruiters.BEHEERDERS.remove(b);
        }
        helper.succeed();
    }

    private static int tel(ServerLevel sl, AABB doos, Class<? extends net.minecraft.world.entity.Entity> c) {
        return sl.getEntitiesOfClass(c, doos, e -> !e.isRemoved()).size();
    }
}
