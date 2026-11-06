package nl.juiced.guhs.feature.vadsrijk.tapijt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden;
import nl.juiced.guhs.feature.vadsrijk.rijdier.RijdierExtraRegistry;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.As;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-TAPIJT: the flying carpet. The movement model as pure maths (the acceleration curve, the long glide, braking, the drift: the
 * momentum only slowly follows the carpet's yaw, the hover spring, ledges, the hop and the never-higher clamp, low ceilings, the
 * server's checks) with the default knobs, plus a real carpet hovering over a floor and over water, and the unlock + looks.
 * Run: {@code runGameTestServer -Pgt=VadsrijkTapijt}.
 */
public class VadsrijkTapijtGameTests {
    private static final String BATCH = "vadsrijk_tapijt";

    private static TapijtBalans.Knoppen k() {
        return TapijtBalans.standaardKnoppen();
    }

    private static double top() {
        return RijdierSoort.TAPIJT.snelheid(1) * k().topfactor();
    }

    private static double hoekGraden(double vx, double vz, float yaw) {
        double[] f = TapijtFysica.richting(yaw);
        return Math.toDegrees(Math.abs(Math.atan2(f[0] * vz - f[1] * vx, vx * f[0] + vz * f[1])));
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void versnellen(GameTestHelper helper) {
        double top = top();
        helper.assertTrue(Math.abs(top - 0.36 * 2.1) < 1e-6, "top speed at level 1: " + top);
        TapijtFysica.Horizontaal h = new TapijtFysica.Horizontaal(0, 0, 0, 0);
        double vorige = 0, vorigeWinst = Double.MAX_VALUE;
        double[] na = new double[81];
        for (int i = 1; i <= 80; i++) {
            h = TapijtFysica.stap(h.vx(), h.vz(), h.yaw(), 0, 1, 0, top, k());
            double v = h.snelheid(), winst = v - vorige;
            helper.assertTrue(winst > 0 && winst <= vorigeWinst + 1e-12, "smooth: every tick a bit less extra speed (tick " + i + ")");
            helper.assertTrue(v <= top + 1e-9, "never over the top speed");
            vorige = v;
            vorigeWinst = winst;
            na[i] = v / top;
        }
        helper.assertTrue(na[1] > 0.04 && na[1] < 0.15, "a strong start but not instant: " + na[1]);
        helper.assertTrue(na[10] > 0.45 && na[10] < 0.75, "half a second: about half speed " + na[10]);
        helper.assertTrue(na[40] > 0.93, "two seconds: (almost) the top " + na[40]);
        helper.assertTrue(h.vz() > 0 && Math.abs(h.vx()) < 1e-9, "yaw 0 = +z");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void uitglijdenEnRemmen(GameTestHelper helper) {
        double top = top();
        // let go of W at top speed: it glides on, slowly slower
        TapijtFysica.Horizontaal h = new TapijtFysica.Horizontaal(0, top, 0, 0);
        double afstand = 0;
        int tot = -1;
        for (int i = 1; i <= 400; i++) {
            h = TapijtFysica.stap(h.vx(), h.vz(), h.yaw(), 0, 0, 0, top, k());
            afstand += h.snelheid();
            if (i == 1) {
                helper.assertTrue(h.snelheid() > top * 0.97, "letting go is NOT stopping: " + h.snelheid() / top);
            }
            if (i == 20) {
                helper.assertTrue(h.snelheid() > top * 0.6 && h.snelheid() < top * 0.85, "after a second still gliding: " + h.snelheid() / top);
            }
            if (tot < 0 && h.snelheid() < 0.01) {
                tot = i;
            }
        }
        helper.assertTrue(afstand > 30, "glides a long way: " + afstand);
        helper.assertTrue(Math.abs(TapijtFysica.uitglijAfstand(top, k()) - afstand) < 2, "the screen's glide number fits: " + afstand);
        // S brakes much faster, then backs off slowly
        h = new TapijtFysica.Horizontaal(0, top, 0, 0);
        int stil = -1;
        for (int i = 1; i <= 200; i++) {
            h = TapijtFysica.stap(h.vx(), h.vz(), h.yaw(), 0, -1, 0, top, k());
            if (stil < 0 && h.vz() <= 0.01) {
                stil = i;
            }
        }
        helper.assertTrue(stil > 5 && stil < 40 && stil < tot / 4, "S brakes (not instantly) in " + stil + " ticks vs gliding " + tot);
        helper.assertTrue(h.vz() < 0 && -h.vz() <= top * k().achteruit() + 1e-6 && -h.vz() > top * k().achteruit() * 0.8, "then slowly backwards: " + h.vz());
        // A: a sideways push (left = +x at yaw 0), weaker than W
        h = new TapijtFysica.Horizontaal(0, 0, 0, 0);
        for (int i = 0; i < 100; i++) {
            h = TapijtFysica.stap(h.vx(), h.vz(), h.yaw(), 0, 0, 1, top, k());
        }
        helper.assertTrue(h.vx() > 0.05 && h.vx() < top * 0.6, "A slides it left, gently: " + h.vx());
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void drift(GameTestHelper helper) {
        double top = top();
        TapijtBalans.Knoppen k = k();
        // flying +z at top speed, then look 90 degrees to the side (yaw 90 = -x)
        TapijtFysica.Horizontaal h = new TapijtFysica.Horizontaal(0, top, 0, 0);
        h = TapijtFysica.stap(h.vx(), h.vz(), h.yaw(), 90, 0, 0, top, k);
        helper.assertTrue(h.draai() > 0 && h.draai() <= k.draaiMax() + 1e-6, "the carpet turns, at most draai_max a tick: " + h.draai());
        for (int i = 0; i < 9; i++) {
            h = TapijtFysica.stap(h.vx(), h.vz(), h.yaw(), 90, 0, 0, top, k);
        }
        double richting = Math.toDegrees(Math.atan2(-h.vx(), h.vz()));   // the heading of the momentum (yaw convention)
        helper.assertTrue(h.yaw() > 40, "after half a second the carpet points well round: " + h.yaw());
        helper.assertTrue(Math.abs(richting) < 12, "... but it still slides the old way (drift): heading " + richting);
        // with W held the momentum swings round to where it points; gliding it stays much longer in the old direction
        TapijtFysica.Horizontaal w = h, g = h;
        for (int i = 0; i < 20; i++) {
            w = TapijtFysica.stap(w.vx(), w.vz(), w.yaw(), 90, 1, 0, top, k);
            g = TapijtFysica.stap(g.vx(), g.vz(), g.yaw(), 90, 0, 0, top, k);
        }
        double hoekW = hoekGraden(w.vx(), w.vz(), w.yaw()), hoekG = hoekGraden(g.vx(), g.vz(), g.yaw());
        helper.assertTrue(hoekW < hoekG - 10, "with W it aligns faster: " + hoekW + " vs gliding " + hoekG);
        helper.assertTrue(hoekG > 40, "gliding: still drifting sideways after a second: " + hoekG);
        for (int i = 0; i < 60; i++) {
            w = TapijtFysica.stap(w.vx(), w.vz(), w.yaw(), 90, 1, 0, top, k);
        }
        helper.assertTrue(hoekGraden(w.vx(), w.vz(), w.yaw()) < 5 && Math.abs(w.yaw() - 90) < 1, "after 4 s of W it flies where it points");
        helper.assertTrue(w.snelheid() <= top + 1e-9, "turning never makes it faster than the top");
        // turning the yaw alone doesn't change the speed (the momentum is kept, only its direction drifts)
        TapijtFysica.Horizontaal r = TapijtFysica.stap(0, top, 0, 180, 1, 0, top, k);
        helper.assertTrue(Math.abs(r.snelheid() - top) < top * 0.05, "a big look-round keeps the momentum: " + r.snelheid() / top);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void zweven(GameTestHelper helper) {
        TapijtBalans.Knoppen k = k();
        double doel = k.zweefhoogte();
        // from the ground up: settles at the hover height without bouncing
        double y = 0, vy = 0, max = 0;
        for (int i = 0; i < 60; i++) {
            vy = TapijtFysica.verticaal(y, vy, 0, doel, k);
            y += vy;
            max = Math.max(max, y);
        }
        helper.assertTrue(Math.abs(y - doel) < 0.03 && max < doel + 0.15, "rises to the hover height, no overshoot: " + y + " max " + max);
        // off a ledge 10 blocks high: it sinks slowly (floaty) and lands softly at the hover height
        y = doel + 10;
        vy = 0;
        double laagste = y;
        for (int i = 0; i < 200; i++) {
            double oud = vy;
            vy = TapijtFysica.verticaal(y, vy, 0, doel, k);
            helper.assertTrue(vy >= -k.valMax() - 1e-9 && oud - vy <= k.zwaarte() + 1e-9, "a gentle, capped sink (tick " + i + ": " + vy + ")");
            y += vy;
            laagste = Math.min(laagste, y);
        }
        helper.assertTrue(Math.abs(y - doel) < 0.03 && laagste > doel - 0.1, "lands on its hover height: " + y + " lowest " + laagste);
        // nothing below in reach: glides down, never faster than val_max, never up
        vy = 0.3;
        for (int i = 0; i < 40; i++) {
            vy = TapijtFysica.verticaal(Double.POSITIVE_INFINITY, vy, 0, doel, k);
        }
        helper.assertTrue(Math.abs(vy + k.valMax()) < 1e-9, "into the deep: sinks at val_max " + vy);
        // the hop: only near the hover height, and never higher than hover + hop_max (no flying, not even holding space)
        helper.assertTrue(TapijtFysica.magHoppen(doel + 0.1, doel) && !TapijtFysica.magHoppen(doel + 0.8, doel)
                && !TapijtFysica.magHoppen(Double.POSITIVE_INFINITY, doel), "hop only from the hover height");
        double hopV = RijdierSoort.TAPIJT.sprong(RijdierSoort.MAX_LEVEL) * k.hopKracht() * 3;   // (even a silly strong hop)
        y = doel;
        vy = 0;
        max = 0;
        for (int i = 0; i < 400; i++) {
            boolean hop = TapijtFysica.magHoppen(y, doel);
            vy = TapijtFysica.verticaal(y, vy, hop ? hopV : 0, doel, k);
            y += vy;
            max = Math.max(max, y);
        }
        helper.assertTrue(max <= doel + k.hopMax() + 1e-6 && max > doel + 0.5, "hopping all the time stays under the cap: " + max);
        // a low ceiling: the rider must fit
        helper.assertTrue(TapijtFysica.doelHoogte(Double.POSITIVE_INFINITY, k) == doel, "open sky: the normal hover height");
        helper.assertTrue(Math.abs(TapijtFysica.doelHoogte(2.5, k) - 0.9) < 1e-9, "2.5 blocks of room: hovers at 0.9");
        helper.assertTrue(TapijtFysica.doelHoogte(1.0, k) == TapijtFysica.MIN_HOOGTE, "a tight tunnel: as low as it goes");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void server(GameTestHelper helper) {
        TapijtBalans.Knoppen k = k();
        double top = RijdierSoort.TAPIJT.snelheid(RijdierSoort.MAX_LEVEL) * k.topfactor();
        helper.assertTrue(!TapijtFysica.teSnel(top * 10, 10, top, k) && !TapijtFysica.teSnel(top * 10 * 1.2, 10, top, k), "top speed (+ lag) is fine");
        helper.assertTrue(TapijtFysica.teSnel(top * 10 * 2, 10, top, k), "twice the top speed is put back");
        double hoog = k.zweefhoogte() + k.hopMax();
        helper.assertTrue(!TapijtFysica.teHoog(hoog, 0, k) && !TapijtFysica.teHoog(Double.POSITIVE_INFINITY, 0, k), "a hop / a ledge is fine");
        helper.assertTrue(!TapijtFysica.teHoog(hoog + 5, 1.4, k), "sinking from a ledge is fine");
        helper.assertTrue(TapijtFysica.teHoog(hoog + 5, 0, k), "standing in the air 5 blocks up is not");
        helper.succeed();
    }

    /** A 5x5 stone floor (y 0) inside the test area (the "empty" template is 5x4x5: neighbours clear what sticks out), air above;
     * with {@code water}: a 3x3 pool on it with a stone rim. */
    private static void vloer(GameTestHelper helper, boolean water) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                boolean rand = x == 0 || z == 0 || x == 4 || z == 4;
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), !water ? Blocks.AIR.defaultBlockState()
                        : rand ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
                for (int y = 2; y < 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static TapijtEntity tapijt(GameTestHelper helper, double x, double y, double z) {
        ServerLevel sl = helper.getLevel();
        Entity e = EcoSpul.type(RijdierSoort.TAPIJT).create(sl, EntitySpawnReason.MOB_SUMMONED);
        helper.assertTrue(e instanceof TapijtEntity, "the carpet entity");
        TapijtEntity t = (TapijtEntity) e;
        BlockPos b = helper.absolutePos(BlockPos.ZERO);
        t.snapTo(b.getX() + x, b.getY() + y, b.getZ() + z, 0, 0);
        helper.assertTrue(sl.addFreshEntity(t), "the carpet joins the level");
        return t;
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void zweeftOverDeGrond(GameTestHelper helper) {
        vloer(helper, false);
        TapijtEntity land = tapijt(helper, 2.5, 3.5, 2.5);   // (2.5 blocks above the floor's top, inside the test area: floats down)
        double vloer = helper.absolutePos(BlockPos.ZERO).getY() + 1;   // the stone's top
        helper.runAfterDelay(5, () -> helper.assertTrue(land.getY() > vloer + 1.9 && land.getY() < vloer + 2.5, "it floats down slowly, not a drop: "
                + (land.getY() - vloer)));
        helper.runAfterDelay(120, () -> {
            double doel = TapijtBalans.get("zweefhoogte");
            helper.assertTrue(land.isAlive() && Math.abs(land.getY() - vloer - doel) < 0.1, "hovers over the floor: " + (land.getY() - vloer));
            land.discard();
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void zweeftOverWater(GameTestHelper helper) {
        vloer(helper, true);
        ServerLevel sl = helper.getLevel();
        BlockPos w = helper.absolutePos(new BlockPos(2, 1, 2));
        double opp = w.getY() + sl.getFluidState(w).getHeight(sl, w);
        TapijtEntity water = tapijt(helper, 2.5, 1.2, 2.5);   // (called while in the water: it rises out)
        helper.runAfterDelay(100, () -> {
            double doel = TapijtBalans.get("zweefhoogte");
            helper.assertTrue(water.isAlive(), "the carpet is still there (" + water.getRemovalReason() + ")");
            helper.assertTrue(Math.abs(water.getY() - opp - doel) < 0.1, "rose out of the water and hovers over it: " + (water.getY() - opp));
            water.discard();
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void krijgenEnLooks(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Rijdieren.reset(p);
        Aankleden.reset(p);
        helper.assertTrue(!Tapijt.heeft(p), "no carpet yet");
        helper.assertTrue(Tapijt.geef(p) && Tapijt.heeft(p) && nl.juiced.guhs.feature.vadsrijk.npc.Questlog.vlag(p, Tapijt.VLAG), "the zone-6 unlock + flag");
        helper.assertTrue(!Tapijt.geef(p), "only once");
        helper.assertTrue(Uiterlijk.delen(Aankleden.uiterlijk(p, RijdierSoort.TAPIJT))[2].equals("kussen_roze")
                && Uiterlijk.delen(Aankleden.uiterlijk(p, RijdierSoort.TAPIJT))[1].equals("kwast_goud"), "starts with a pink cushion and gold tassels");
        // its own looks only (no spots/hats on a carpet), all files there
        helper.assertTrue(Uiterlijk.van(RijdierSoort.TAPIJT, As.VACHT).size() == 6 && Uiterlijk.van(RijdierSoort.TAPIJT, As.PATROON).size() == 5
                && Uiterlijk.van(RijdierSoort.TAPIJT, As.TUIG).size() == 4, "weave 6, tassels 5, cushions 4");
        helper.assertTrue(Uiterlijk.van(RijdierSoort.TAPIJT, As.TUIG, "hoedje") == null && Uiterlijk.van(RijdierSoort.KNUFFELHERT, As.TUIG, "kussen_roze") == null,
                "no shared tack on the carpet, no cushions on the deer");
        for (Uiterlijk.Variant v : Uiterlijk.alle()) {
            if (v.soort() != RijdierSoort.TAPIJT) {
                continue;
            }
            String pad = switch (v.as()) {
                case VACHT -> v.id().equals(Uiterlijk.NATUREL) ? "textures/entity/vr_eco_tapijt.png" : "textures/entity/vr_eco_tapijt_" + v.id() + ".png";
                case PATROON -> "textures/entity/vr_rijpatroon_tapijt_" + v.id() + ".png";
                case TUIG -> "models/item/vr_rijtuig_" + v.id() + ".json";
            };
            helper.assertTrue(bestaat(pad), "file " + pad);
            if (v.as() == As.TUIG) {
                helper.assertTrue(RijdierExtraRegistry.TUIG.containsKey(v.id()), "cushion item " + v.id());
            }
        }
        helper.assertTrue(bestaat("geckolib/models/entity/vr_eco_tapijt.geo.json") && bestaat("geckolib/animations/entity/vr_eco_tapijt.animation.json"),
                "model + animations");
        helper.assertTrue(BuiltInRegistries.ITEM.getOptional(Guhs.id("vr_item_zadel_tapijt")).isPresent() && EcoSpul.ZADELS.get(RijdierSoort.TAPIJT) != null,
                "its icon item (rolled-up carpet)");
        // wearing: cushion off and on, a bought weave needs owning first
        helper.assertTrue(Aankleden.kies(p, RijdierSoort.TAPIJT, As.TUIG, ""), "cushion off");
        helper.assertTrue(!Aankleden.kies(p, RijdierSoort.TAPIJT, As.VACHT, "turkoois"), "not owned yet");
        helper.assertTrue(Aankleden.geef(p, "vacht:tapijt:turkoois") && Aankleden.kies(p, RijdierSoort.TAPIJT, As.VACHT, "turkoois"), "given + worn");
        helper.assertTrue(Aankleden.geef(p, "tuig:tapijt:kussen_paars"), "a cushion by its key");
        helper.assertTrue(RijdierSoort.TAPIJT.glijdt() && !RijdierSoort.PLUIMGUH.glijdt(), "only the carpet glides");
        Rijdieren.reset(p);
        Aankleden.reset(p);
        helper.succeed();
    }

    private static boolean bestaat(String pad) {
        return VadsrijkTapijtGameTests.class.getResource("/assets/guhs/" + pad) != null;
    }
}
