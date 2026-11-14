package nl.juiced.guhs.feature.vadsrijk.klassen.magier;

import java.util.Set;
import java.util.function.Function;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.ProefKlodder;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.feature.vadsrijk.wezens.Weerkaatsen;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.10.2 (SCHADE_LUC_ANALYSE): Roze Vuurbal and Meteoorguh crit by CHANCE (they were always a crit: a bug), and Tijdsprongetje halves
 * the Vuurbal cooldown instead of resetting it. Run: {@code runGameTestServer -Pgt=VadsrijkMagierKritiek}.
 */
public class VadsrijkMagierKritiekGameTests {
    private static final String BATCH = "vadsrijk_magierkritiek";
    private static final int N = 300;

    /** Kritiek exactly {@code doel} for this player (the class/gear base cancelled out through Spelers.EXTRA_STATS). */
    private static Function<ServerPlayer, Stats> kritiek(ServerPlayer p, double doel) {
        double basis = Spelers.stats(p).get(Stat.KRITIEK);
        Function<ServerPlayer, Stats> f = x -> x == p ? Stats.van(Stat.KRITIEK, doel - basis) : new Stats();
        Spelers.EXTRA_STATS.add(f);
        return f;
    }

    private static ProefKlodder klodder(GameTestHelper helper) {
        ProefKlodder m = helper.spawn(KernRegistry.PROEFKLODDER.get(), 2, 1, 2);
        m.stelIn(70, Element.GEEN, false, Set.of());
        return m;
    }

    /** {@code n} hits of this kind; returns how many were crits (a fresh klodder whenever one falls). */
    private static int crits(GameTestHelper helper, ServerPlayer p, boolean meteoor) {
        ProefKlodder m = klodder(helper);
        int k = 0;
        for (int i = 0; i < N; i++) {
            if (m.geplofd() || m.isRemoved() || m.vads() < 50) {
                m.discard();
                m = klodder(helper);
            }
            Schade.Uitkomst u = meteoor ? Vadsmagier.meteoorTreffer(p, m, 1) : Vadsmagier.vuurbalTreffer(p, m, 1, Element.ROZE);
            k += u.kritiek() ? 1 : 0;
        }
        m.discard();
        return k;
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void vuurbalKritiekIsKans(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        String oud = d.klasse;
        d.klasse = Klassen.MAGIER;
        Function<ServerPlayer, Stats> f = kritiek(p, 0);
        try {
            helper.assertTrue(Math.abs(Spelers.stats(p).get(Stat.KRITIEK)) < 1e-6, "Kritiek is 0 for the test: " + Spelers.stats(p).get(Stat.KRITIEK));
            int v0 = crits(helper, p, false), m0 = crits(helper, p, true);
            helper.assertTrue(v0 == 0, "Kritiek 0: a Vuurbal never crits (" + v0 + "/" + N + ")");
            helper.assertTrue(m0 == 0, "Kritiek 0: a Meteoorguh never crits (" + m0 + "/" + N + ")");
            Spelers.EXTRA_STATS.remove(f);
            f = kritiek(p, 30);
            int v30 = crits(helper, p, false), m30 = crits(helper, p, true);
            double fv = v30 / (double) N, fm = m30 / (double) N;
            // 300 rolls at 30 %: sd ~2.6 %; 0.18..0.42 is > 4 sd either side
            helper.assertTrue(fv > 0.18 && fv < 0.42, "Kritiek 30 %: the Vuurbal crits at about that rate: " + v30 + "/" + N);
            helper.assertTrue(fm > 0.18 && fm < 0.42, "Kritiek 30 %: the Meteoorguh crits at about that rate: " + m30 + "/" + N);
        } finally {
            Spelers.EXTRA_STATS.remove(f);
            d.klasse = oud;
        }
        helper.succeed();
    }

    /** Tijdsprongetje: a Blink halves the remaining Vuurbal cooldown (8 s left -> 4 s), and a ready Vuurbal stays ready. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void tijdsprongHalveert(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        long nu = p.level().getGameTime();
        d.klaarOp.put("roze_vuurbal", nu + 160);
        Vadsmagier.Blink.tijdsprong(p, d);
        long na = d.klaarOp.get("roze_vuurbal");
        helper.assertTrue(na == nu + 80, "8 s left -> 4 s left: " + (na - nu) + " ticks");
        d.klaarOp.put("roze_vuurbal", nu - 5);
        Vadsmagier.Blink.tijdsprong(p, d);
        helper.assertTrue(d.klaarOp.get("roze_vuurbal") == nu - 5, "a ready Vuurbal is left alone");
        d.klaarOp.remove("roze_vuurbal");
        helper.succeed();
    }

    /**
     * 1.10.2 (LUC_DOOD_ANALYSE): a lv-20 mage's Meteoorguh on 5 Kristalklodders, every allowed roll reflecting: one roll per cast, the
     * reflect capped at 15 % of max Vads and no mob multiplier, so after 3 casts he still has well over half his Vads. The Liedpuls
     * never rolls.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void kristalSpiegelNietDodelijk(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        SpelerData d = Spelers.data(p);
        String oud = d.klasse;
        d.klasse = Klassen.MAGIER;
        Spelers.zetLevel(p, 20);
        List<LivingEntity> klodders = new ArrayList<>();
        try {
            for (int i = 0; i < 5; i++) {
                var kk = helper.spawn(WezensRegistry.KRISTALKLODDER.get(), 1 + i % 3, 1, 1 + i / 3);
                kk.stelIn(15, null, false, Set.of());
                klodders.add(kk);
            }
            double max = Spelers.maxVads(p);
            d.vads = max;
            Weerkaatsen.TEST_ALTIJD = true;
            Weerkaatsen.vergeet(p.getUUID());
            double cap = Weerkaatsen.terug(p, 1e9);
            helper.assertTrue(Math.abs(cap - max * Weerkaatsen.MAX_DEEL) < 1e-6, "one reflect is capped at 15 % of max Vads: " + cap + " / " + max);
            helper.assertTrue(Weerkaatsen.magRollen(p, "meteoorguh", 7) && !Weerkaatsen.magRollen(p, "meteoorguh", 7) && Weerkaatsen.magRollen(p, "meteoorguh", 8),
                    "one roll per cast (skill + tick)");
            helper.assertTrue(!Weerkaatsen.magRollen(p, "liedpuls", 9) && !Weerkaatsen.magRollen(p, "sterrenwever", 9), "automatic procs never roll");
            Weerkaatsen.vergeet(p.getUUID());
            // one real cast on the 5 (a huge hit, so an uncapped reflect would one-shot him)
            double voor = Spelers.vads(p);
            for (LivingEntity e : klodders) {
                Vadsmagier.meteoorTreffer(p, e, 5000);
            }
            double verlies = voor - Spelers.vads(p);
            helper.assertTrue(verlies > 0, "the cast was reflected once (TEST_ALTIJD)");
            helper.assertTrue(verlies <= max * Weerkaatsen.MAX_DEEL + 1e-6, "at most one capped reflect per cast: lost " + verlies + " of " + max);
            helper.assertTrue(Spelers.vads(p) >= max * 0.5, "he survives with >= 50 % Vads: " + Spelers.vads(p) + " / " + max);
            helper.succeed();
        } finally {
            Weerkaatsen.TEST_ALTIJD = false;
            Weerkaatsen.vergeet(p.getUUID());
            klodders.forEach(LivingEntity::discard);
            d.klasse = oud;
        }
    }
}
