package nl.juiced.guhs.feature.vadsrijk.momenten;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Het Vadsrijk "momenten": the caravan's road and state machine, the attack rewards, the wave timing and the cart entities.
 * Run: {@code runGameTestServer -Pgt=VadsrijkMomenten}.
 */
public class VadsrijkMomentenGameTests {
    private static final String BATCH = "vadsrijk_momenten";

    @GuhTest(template = "empty", batch = BATCH)
    public static void routeVanHavenNaarSporenwoud(GameTestHelper helper) {
        KaravaanRoute r = KaravaanRoute.ZONE1;
        BlockPos haven = WereldPlekken.plek("haven_plein"), vuur = WereldPlekken.plek("kampvuur_sporenwoud_1"), plein = WereldPlekken.plek("plein");
        helper.assertTrue(r.op(0).distanceTo(Vec3.atBottomCenterOf(haven)) < 8, "the road starts at the haven plein: " + r.op(0));
        helper.assertTrue(r.op(r.lengte()).distanceTo(Vec3.atBottomCenterOf(vuur)) < 8, "the road ends at the Sporenwoud campfire: " + r.op(r.lengte()));
        helper.assertTrue(r.lengte() > 600 && r.lengte() < 1000, "road length " + r.lengte());
        // stops in order along the road, Zwamzoet's stop near the plein
        List<String> ids = new ArrayList<>();
        double vorige = -1;
        for (KaravaanRoute.Stop s : r.stops()) {
            helper.assertTrue(r.afstandVan(s.index()) > vorige, "stops out of order at " + s.id());
            vorige = r.afstandVan(s.index());
            ids.add(s.id());
        }
        helper.assertTrue(ids.equals(List.of("haven", "kustpad", "zwamzoet", "sporenwoud")), "stops " + ids);
        Vec3 zz = r.punten().get(r.stops().get(2).index());
        helper.assertTrue(zz.distanceTo(Vec3.atBottomCenterOf(plein)) < 20, "the Zwamzoet stop is by the plein: " + zz);
        // no jumps: corners close together, the road stays near ground level, walking it never moves more than one tick's worth
        for (int i = 1; i < r.punten().size(); i++) {
            Vec3 a = r.punten().get(i - 1), b = r.punten().get(i);
            helper.assertTrue(a.distanceTo(b) < 90 && Math.abs(a.y - b.y) <= 3, "corner " + i + " jumps: " + a + " -> " + b);
        }
        for (double s = 0; s < r.lengte(); s += 7.3) {
            Vec3 a = r.op(s), b = r.op(s + KaravaanStaat.SNELHEID);
            helper.assertTrue(Math.hypot(b.x - a.x, b.z - a.z) <= KaravaanStaat.SNELHEID + 1e-6 && Math.abs(b.y - a.y) < 0.2, "jump at s=" + s);
            helper.assertTrue(Math.abs(r.dichtstbij(r.op(s).x, r.op(s).z) - s) < 0.5, "nearest point at s=" + s);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void karavaanRijdtStoptEnKeert(GameTestHelper helper) {
        KaravaanStaat st = new KaravaanStaat(KaravaanRoute.ZONE1, RandomSource.create(4));
        st.aanvalPauze = Integer.MAX_VALUE;   // no attacks in this test
        List<String> stops = new ArrayList<>();
        stops.add(st.stop());
        double vorige = st.s();
        int ticks = 0;
        while (stops.size() < 7 && ticks < 200_000) {
            KaravaanStaat.Gebeurtenis g = st.tick(true, true, RandomSource.create(ticks));
            ticks++;
            if (st.fase() == KaravaanStaat.Fase.RIJDEN && g != KaravaanStaat.Gebeurtenis.VERTROKKEN) {
                helper.assertTrue((st.s() - vorige) * st.richting() > 0, "the cart drives the wrong way at tick " + ticks);
            } else {
                helper.assertTrue(st.s() == vorige || g == KaravaanStaat.Gebeurtenis.AANGEKOMEN || g == KaravaanStaat.Gebeurtenis.VERTROKKEN,
                        "the cart moves while stopped");
            }
            vorige = st.s();
            if (g == KaravaanStaat.Gebeurtenis.AANGEKOMEN) {
                stops.add(st.stop());
            }
        }
        helper.assertTrue(stops.equals(List.of("haven", "kustpad", "zwamzoet", "sporenwoud", "zwamzoet", "kustpad", "haven")), "stops " + stops);
        // there and back: 2 x the road at SNELHEID + the stop times (haven 1x at the start, 2x kustpad, 2x zwamzoet, 1x sporenwoud)
        int verwacht = (int) Math.round(2 * KaravaanRoute.ZONE1.lengte() / KaravaanStaat.SNELHEID) + 20 * (60 + 2 * 30 + 2 * 75 + 60);
        helper.assertTrue(Math.abs(ticks - verwacht) < 40, "a round trip took " + ticks + " ticks, expected ~" + verwacht);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void aanvalAlleenDichtbijEnGeeftOp(GameTestHelper helper) {
        RandomSource r = RandomSource.create(9);
        KaravaanStaat st = new KaravaanStaat(KaravaanRoute.ZONE1, r);
        helper.assertTrue(st.aanvalPauze() >= KaravaanStaat.PAUZE_MIN / 2 && st.aanvalPauze() < KaravaanStaat.PAUZE_MIN * 3 / 2,
                "first pause " + st.aanvalPauze());
        st.zet(150, 1);
        st.aanvalPauze = 0;
        helper.assertTrue(st.tick(false, true, r) != KaravaanStaat.Gebeurtenis.AANVAL_BEGINT, "attack without players near");
        helper.assertTrue(st.tick(true, false, r) != KaravaanStaat.Gebeurtenis.AANVAL_BEGINT, "attack where it may not (town/stop)");
        helper.assertTrue(st.tick(true, true, r) == KaravaanStaat.Gebeurtenis.AANVAL_BEGINT, "no attack with players near out of town");
        double s = st.s();
        for (int i = 0; i < KaravaanStaat.AANVAL_MAX - 1; i++) {
            helper.assertTrue(st.tick(true, true, r) == KaravaanStaat.Gebeurtenis.GEEN && st.fase() == KaravaanStaat.Fase.AANVAL, "attack ended early at " + i);
        }
        helper.assertTrue(st.s() == s, "the cart moved during the attack");
        helper.assertTrue(st.tick(true, true, r) == KaravaanStaat.Gebeurtenis.AANVAL_OPGEGEVEN, "the attackers don't give up after 2 min");
        helper.assertTrue(st.fase() == KaravaanStaat.Fase.RIJDEN && st.aanvalPauze() >= KaravaanStaat.PAUZE_MIN - 1
                && st.aanvalPauze() <= KaravaanStaat.PAUZE_MAX, "after giving up: drive on, pause " + st.aanvalPauze());
        // won: drive on with a new pause
        st.aanvalPauze = 0;
        helper.assertTrue(st.tick(true, true, r) == KaravaanStaat.Gebeurtenis.AANVAL_BEGINT, "second attack");
        st.aanvalGewonnen(r);
        helper.assertTrue(st.fase() == KaravaanStaat.Fase.RIJDEN && st.aanvalPauze() >= KaravaanStaat.PAUZE_MIN, "after winning");
        // where: never in Zwamzoet or at a stop, yes on the open road
        KaravaanRoute route = KaravaanRoute.ZONE1;
        helper.assertTrue(!Karavaan.magAanval(st, route.punten().get(route.stops().get(2).index())), "attack allowed in Zwamzoet");
        helper.assertTrue(!Karavaan.magAanval(st, route.punten().get(route.stops().get(1).index())), "attack allowed at a stop");
        helper.assertTrue(Karavaan.magAanval(st, route.punten().get(12)), "no attack on the shore road " + route.punten().get(12));
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void beloningVerdeling(GameTestHelper helper) {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        Map<UUID, Integer> geholpen = new LinkedHashMap<>();
        geholpen.put(a, 3);
        geholpen.put(b, 1);
        geholpen.put(c, 0);
        var alles = KaravaanBeloning.verdeel(geholpen, 5, 5, RandomSource.create(1));
        long basis = KaravaanBeloning.BASIS + KaravaanBeloning.PER_WEZEN * 5L;
        helper.assertTrue(alles.size() == 2 && !alles.containsKey(c), "only real helpers: " + alles.keySet());
        helper.assertTrue(alles.get(a).munten() == basis + 3L * KaravaanBeloning.EIGEN, "a got " + alles.get(a));
        helper.assertTrue(alles.get(b).munten() == basis + KaravaanBeloning.EIGEN, "b got " + alles.get(b));
        var deels = KaravaanBeloning.verdeel(geholpen, 5, 2, RandomSource.create(1));
        helper.assertTrue(deels.get(a).munten() == Math.round(basis * 0.4) + 3L * KaravaanBeloning.EIGEN && deels.get(a).zeldzaam() == null
                && deels.get(b).zeldzaam() == null, "half done: less and no rare items " + deels);
        helper.assertTrue(KaravaanBeloning.verdeel(Map.of(), 5, 5, RandomSource.create(1)).isEmpty(), "nobody helped: nothing");
        helper.assertTrue(KaravaanBeloning.verdeel(geholpen, 5, 0, RandomSource.create(1)).isEmpty(), "none cheered up: nothing");
        int zeldzaam = 0;
        RandomSource r = RandomSource.create(77);
        for (int i = 0; i < 2000; i++) {
            zeldzaam += KaravaanBeloning.verdeel(Map.of(a, 1), 4, 4, r).get(a).zeldzaam() != null ? 1 : 0;
        }
        helper.assertTrue(zeldzaam > 400 && zeldzaam < 600, "rare chance ~25 %: " + zeldzaam + "/2000");
        for (String id : KaravaanBeloning.ZELDZAAM) {
            helper.assertTrue(!Karavaan.zeldzaam(id).isEmpty(), "rare item " + id + " has no stack");
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void beloningKomtAanEnTitel(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        long voor = Spelers.data(p).munten;
        Karavaan.geef(p, new KaravaanBeloning.Beloning(120, "regenboogdrankje"));
        helper.assertTrue(Spelers.data(p).munten == voor + 120, "coins " + Spelers.data(p).munten);
        helper.assertTrue(p.getInventory().contains(new net.minecraft.world.item.ItemStack(MomentenRegistry.REGENBOOGDRANKJE.get())),
                "the rare potion did not arrive");
        helper.assertTrue(!Cosmetica.heeftTitel(p, Karavaan.TITEL), "title too early");
        Karavaan.geef(p, new KaravaanBeloning.Beloning(10, null));
        Karavaan.geef(p, new KaravaanBeloning.Beloning(10, null));
        helper.assertTrue(Cosmetica.heeftTitel(p, Karavaan.TITEL), "no title Karavaanvriend after " + Karavaan.TITEL_NA + " helped attacks");
        // the shop exists, sells the potion, and its stock rotates but stays the same within a rotation
        Winkels.Winkel w = Karavaan.maakWinkel(3);
        helper.assertTrue(w.waar("regenboogdrankje") != null && w.waar("herrolknabbel") != null && w.waren().size() == 5, "stock " + w.waren());
        helper.assertTrue(Karavaan.maakWinkel(3).waren().stream().map(Winkels.Waar::id).toList().equals(w.waren().stream().map(Winkels.Waar::id).toList()),
                "the same rotation gives another stock");
        boolean anders = false;
        for (int i = 4; i < 12; i++) {
            anders |= !Karavaan.maakWinkel(i).waren().stream().map(Winkels.Waar::id).toList().equals(w.waren().stream().map(Winkels.Waar::id).toList());
        }
        helper.assertTrue(anders, "the stock never rotates");
        helper.assertTrue(Winkels.winkel(Karavaan.WINKEL) != null, "shop karavaan not registered");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void golfTiming(GameTestHelper helper) {
        GolfBaan b = new GolfBaan(330, -398, 500, Golf.R_MAX);
        helper.assertTrue(b.straal(0) == 0, "starts at the temple");
        double vorige = 0;
        for (int t = 1; t <= GolfBaan.DUUR + 10; t++) {
            double r = b.straal(t);
            helper.assertTrue(r >= vorige - 1e-9, "the wave shrinks at " + t);
            helper.assertTrue(r - vorige < 25, "the wave jumps at " + t + ": " + (r - vorige));
            vorige = r;
        }
        helper.assertTrue(Math.abs(b.straal(GolfBaan.A) - 380) < 1e-6 && Math.abs(b.straal(GolfBaan.B) - 620) < 1e-6, "slow phase around the player");
        helper.assertTrue(b.straal(GolfBaan.DUUR) >= Golf.R_MAX - 1e-6, "does not reach the zone edge");
        helper.assertTrue(!b.heel(0, -44, GolfBaan.A) && b.heel(0, -44, GolfBaan.B), "the player's spot heals during the slow phase");
        GolfBaan dichtbij = new GolfBaan(330, -398, 20, Golf.R_MAX);   // standing at the temple (dev)
        helper.assertTrue(dichtbij.straal(GolfBaan.A) == 0 && dichtbij.straal(GolfBaan.B) > 100, "near the temple " + dichtbij.straal(GolfBaan.B));
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void karKanNietKapot(GameTestHelper helper) {
        var sl = helper.getLevel();
        KaravaanSlak slak = MomentenRegistry.KARAVAAN.get().create(sl, EntitySpawnReason.EVENT);
        HandelGuh guh = MomentenRegistry.HANDELAAR.get().create(sl, EntitySpawnReason.EVENT);
        helper.assertTrue(slak != null && guh != null, "entities not created");
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
        slak.snapTo(pos.x, pos.y, pos.z, 0, 0);
        guh.kleed();
        guh.snapTo(pos.x, pos.y + 1, pos.z, 0, 0);
        sl.addFreshEntity(slak);
        sl.addFreshEntity(guh);
        helper.assertTrue(guh.startRiding(slak, true, false) && guh.getVehicle() == slak, "the trader does not sit on the cart");
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(!slak.hurtServer(sl, sl.damageSources().playerAttack(p), 50f), "the cart can be hurt");
        guh.hurtServer(sl, sl.damageSources().playerAttack(p), 50f);
        helper.assertTrue(guh.isAlive() && guh.getHealth() == guh.getMaxHealth(), "the trader guh got hurt");
        helper.assertTrue(!slak.shouldBeSaved() && !guh.shouldBeSaved(), "caravan entities are saved");
        slak.discard();
        guh.discard();
        helper.succeed();
    }
}
