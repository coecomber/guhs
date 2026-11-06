package nl.juiced.guhs.feature.vadsrijk.zone4.dieren;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-Z4DIEREN: Ouwe Dobber's loop (closed, smooth, slow, never near the static Schildpadeiland, a real pause at the jetty), his
 * seats (8, all high above the water, getting off at the halte = the jetty, at sea = in the water beside him), and the guh kites
 * (positions from the map, the entity carries its data, is never saved). Run: {@code runGameTestServer -Pgt=Z4Dieren}.
 */
public class Z4DierenGameTests {
    private static final String BATCH = "vadsrijk_z4dieren";

    @GuhTest(template = "empty", batch = BATCH)
    public static void dobberRoute(GameTestHelper helper) {
        DobberRoute r = DobberRoute.get();
        helper.assertTrue(Z4DierenPlekken.CHECK_FOUTEN == 0, "the generator found collisions: " + Z4DierenPlekken.CHECK_FOUTEN);
        helper.assertTrue(Math.abs(r.lengte() - Z4DierenPlekken.LENGTE) < Z4DierenPlekken.LENGTE * 0.03, "length " + r.lengte() + " vs " + Z4DierenPlekken.LENGTE);
        helper.assertTrue(r.lengte() > 150 && r.lengte() < 600, "length " + r.lengte());
        // waiting at the halte, then a slow lap that ends where it began
        helper.assertTrue(r.wacht(0) && r.wacht(DobberRoute.PAUZE - 1) && !r.wacht(DobberRoute.PAUZE + 1), "pause");
        double[] h = Z4DierenPlekken.DOBBER_ROUTE[Z4DierenPlekken.DOBBER_HALTE];
        helper.assertTrue(r.halte().distanceTo(new Vec3(h[0], 0, h[1])) < 0.5, "halte point " + r.halte());
        BlockPos eiland = Z4WereldPlekken.SCHILDPAD;
        Vec3 vorige = OuweDobber.plek(0);
        double maxStap = 0, minEiland = 1e9;
        for (int t = 1; t <= r.cyclus() + 5; t++) {
            Vec3 p = OuweDobber.plek(t);
            maxStap = Math.max(maxStap, Math.hypot(p.x - vorige.x, p.z - vorige.z));
            minEiland = Math.min(minEiland, Math.hypot(p.x - eiland.getX(), p.z - eiland.getZ()));
            helper.assertTrue(Z4WereldPlekken.inZone4(p.x, p.z), "outside zone 4 at t=" + t);
            helper.assertTrue(p.y > Z4WereldPlekken.ZEE && p.y < Z4WereldPlekken.ZEE + 1.5, "not at the water at t=" + t + ": " + p.y);
            vorige = p;
        }
        helper.assertTrue(maxStap <= DobberRoute.SNELHEID * 1.15 + 1e-6, "too fast: " + maxStap);
        helper.assertTrue(minEiland > 28, "too close to the static island: " + minEiland);
        helper.assertTrue(r.afstand(r.cyclus() - 1) > r.lengte() - 0.5, "the lap does not end at the halte");
        // the jetty is next to the halte (reachable: within 9 blocks of his middle, his box is 10 wide)
        Vec3 dok = new Vec3(Z4DierenPlekken.DOK_X + 0.5, 0, Z4DierenPlekken.DOK_Z + 0.5);
        helper.assertTrue(r.halte().distanceTo(dok) < 9, "jetty too far from the halte: " + r.halte().distanceTo(dok));
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void dobberZitjes(GameTestHelper helper) {
        helper.assertTrue(OuweDobber.ZITPLAATSEN == 8 && OuweDobber.magErbij(7) && !OuweDobber.magErbij(8), "8 seats");
        for (int i = 0; i < OuweDobber.ZITPLAATSEN; i++) {
            for (float yaw : new float[]{0, 90, 217, -45}) {
                Vec3 z = OuweDobber.zitje(i, yaw);
                helper.assertTrue(z.y >= 2.9, "seat " + i + " too low");
                helper.assertTrue(Math.hypot(z.x, z.z) < 5.5, "seat " + i + " off the shell");
            }
        }
        OuweDobber d = Z4DierenRegistry.OUWE_DOBBER.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(d != null, "no turtle");
        d.setVast(true);
        Vec3 hier = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        d.setPos(hier.x, hier.y, hier.z);
        helper.getLevel().addFreshEntity(d);
        helper.assertTrue(!d.shouldBeSaved(), "never saved in a chunk");
        helper.assertTrue(d.getType().canSerialize(), "must be serializable (vanilla only lets you ride those)");
        // at sea (far from the halte): off into the water beside him, not onto the jetty
        Vec3 af = d.afstapPlek(null);
        helper.assertTrue(af.y < Z4DierenPlekken.DOK_Y && Math.hypot(af.x - d.getX(), af.z - d.getZ()) > 6, "off at sea: " + af);
        helper.assertTrue(af.y > Z4WereldPlekken.ZEE, "off at sea: at the surface, " + af.y);
        // near the halte: onto the jetty
        Vec3 h = DobberRoute.get().halte();
        d.setPos(h.x, OuweDobber.OPPERVLAK - OuweDobber.DIEPTE, h.z);
        Vec3 af2 = d.afstapPlek(null);
        helper.assertTrue(af2.y == Z4DierenPlekken.DOK_Y && Math.abs(af2.x - (Z4DierenPlekken.DOK_X + 0.5)) < 0.01, "off at the halte: " + af2);
        d.discard();
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void vliegers(GameTestHelper helper) {
        helper.assertTrue(Z4DierenPlekken.VLIEGERS.size() >= 15, "kites: " + Z4DierenPlekken.VLIEGERS.size());
        for (Z4DierenPlekken.Vlieger v : Z4DierenPlekken.VLIEGERS) {
            helper.assertTrue(v.y() > v.ay() + 4, "kite not above its anchor: " + v);
            helper.assertTrue(Z4WereldPlekken.inZone4(v.x(), v.z()) && Z4WereldPlekken.inZone4(v.ax(), v.az()), "kite outside zone 4: " + v);
            helper.assertTrue(v.grootte() > 1 && v.grootte() < 8, "kite size " + v.grootte());
        }
        GuhVlieger k = Z4DierenRegistry.VLIEGER.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(k != null, "no kite");
        Z4DierenPlekken.Vlieger v0 = Z4DierenPlekken.VLIEGERS.getFirst();
        Vec3 o = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        k.zet(new Z4DierenPlekken.Vlieger(o.x, o.y, o.z, o.x + 3, o.y + 10, o.z + 2, v0.grootte(), v0.kleur(), v0.kleur2()));
        helper.getLevel().addFreshEntity(k);
        helper.assertTrue(k.kleur() == v0.kleur() && k.kleur2() == v0.kleur2() && k.grootte() == v0.grootte(), "data");
        helper.assertTrue(k.anker().distanceTo(o) < 0.01, "anchor " + k.anker());
        helper.assertTrue(!k.isPickable() && !k.shouldBeSaved() && !k.getType().canSerialize(), "a kite is decoration only");
        helper.assertTrue(k.getBoundingBoxForCulling().contains(o) && k.getBoundingBoxForCulling().contains(k.position()), "culling box");
        k.discard();
        helper.succeed();
    }
}
