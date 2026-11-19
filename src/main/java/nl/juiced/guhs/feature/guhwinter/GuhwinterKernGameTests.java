package nl.juiced.guhs.feature.guhwinter;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.feature.guhwinter.kern.Momenten;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.10 Guhwinter kern: the phase calculation, the dev date override and the moment catch-up. Everything happens inside
 * one call (no waiting), so the server's own moment tick never sees the override. Run:
 * {@code runGameTestServer -Pgt=GuhwinterKern}.
 */
public class GuhwinterKernGameTests {
    private static final String B = "guhwinter_kern";

    private static ZonedDateTime t(String s) {
        return Guhwinter.leesDatum(s).atZone(Guhwinter.ZONE);
    }

    /** The phases at their edges (Europe/Amsterdam, also from an instant in UTC), the windows, the plaza box. */
    @GuhTest(template = "empty", batch = B)
    public static void fasen(GameTestHelper helper) {
        Object[][] gevallen = {
                {"2026-11-10 12:00", GuhwinterFase.VOOR}, {"2026-11-24 00:00", GuhwinterFase.SINT}, {"2026-12-05 18:59", GuhwinterFase.SINT},
                {"2026-12-05 19:00", GuhwinterFase.PAKJESAVOND}, {"2026-12-05 23:59", GuhwinterFase.PAKJESAVOND},
                {"2026-12-06 00:00", GuhwinterFase.KERST}, {"2026-12-25 23:59", GuhwinterFase.KERST},
                {"2026-12-26 00:00", GuhwinterFase.OUDJAAR}, {"2027-01-02 23:59", GuhwinterFase.OUDJAAR},
                {"2027-01-03 00:00", GuhwinterFase.AFGELOPEN}, {"2028-06-01 12:00", GuhwinterFase.AFGELOPEN}};
        for (Object[] g : gevallen) {
            GuhwinterFase f = GuhwinterFase.op(t((String) g[0]));
            helper.assertTrue(f == g[1], g[0] + ": " + f + ", expected " + g[1]);
        }
        // 18:00 UTC on 5 Dec is 19:00 in Amsterdam (CET, +1)
        helper.assertTrue(GuhwinterFase.op(ZonedDateTime.of(2026, 12, 5, 18, 0, 0, 0, ZoneOffset.UTC)) == GuhwinterFase.PAKJESAVOND, "UTC 18:00 = pakjesavond");
        helper.assertTrue(GuhwinterFase.op(ZonedDateTime.of(2026, 12, 5, 17, 59, 0, 0, ZoneOffset.UTC)) == GuhwinterFase.SINT, "UTC 17:59 = still Sint");
        helper.assertTrue(GuhwinterFase.VOOR.sint() && GuhwinterFase.VOOR.bezig() && GuhwinterFase.PAKJESAVOND.sint() && !GuhwinterFase.KERST.sint()
                && GuhwinterFase.OUDJAAR.bezig() && !GuhwinterFase.AFGELOPEN.bezig(), "VOOR counts as Sint; AFGELOPEN is over");
        helper.assertTrue(Guhwinter.inschrijvenOpen(t("2026-12-01 23:59")) && !Guhwinter.inschrijvenOpen(t("2026-12-02 00:00")), "signing up until 1 Dec 23:59");
        helper.assertTrue(!Guhwinter.bouwenOpen(t("2026-12-01 23:59")) && Guhwinter.bouwenOpen(t("2026-12-02 00:00"))
                && Guhwinter.bouwenOpen(t("2026-12-05 17:59")) && !Guhwinter.bouwenOpen(t("2026-12-05 18:00")), "building 2 Dec - 5 Dec 18:00");
        helper.assertTrue(Guhwinter.kerstboomOpen(t("2026-12-25 23:59")) && !Guhwinter.kerstboomOpen(t("2026-12-26 00:00")), "kerstboom until 25 Dec");
        helper.assertTrue(!Guhwinter.albumOpen(t("2026-12-25 23:59")) && Guhwinter.albumOpen(t("2026-12-26 00:00"))
                && Guhwinter.albumOpen(t("2027-01-02 23:59")) && !Guhwinter.albumOpen(t("2027-01-03 00:00")), "album 26 Dec - 2 Jan");
        helper.assertTrue(GuhwinterZone.binnen(new BlockPos(-350, 30, -735)) && GuhwinterZone.binnen(new BlockPos(-276, 90, -661))
                && !GuhwinterZone.binnen(new BlockPos(-351, 30, -700)) && !GuhwinterZone.binnen(new BlockPos(-300, 91, -700))
                && !GuhwinterZone.binnen(new BlockPos(-300, 30, -660)) && !GuhwinterZone.binnen(new BlockPos(-300, 19, -700)), "the plaza box");
        helper.succeed();
    }

    /** /guhs winter datum: the override moves the clock (and keeps running), is saved, and "echt" takes it away. */
    @GuhTest(template = "empty", batch = B)
    public static void datum(GameTestHelper helper) {
        MinecraftServer s = helper.getLevel().getServer();
        GuhwinterData d = GuhwinterData.get(s);
        Long oud = d.verschuiving;
        try {
            Guhwinter.zetDatum(s, Guhwinter.leesDatum("2026-12-24 18:30"));
            long verschil = Guhwinter.nu(s).toInstant().toEpochMilli() - t("2026-12-24 18:30").toInstant().toEpochMilli();
            helper.assertTrue(Guhwinter.nepDatum(s) && Math.abs(verschil) < 5000, "the override reads 24 Dec 18:30 (" + verschil + " ms off)");
            helper.assertTrue(Guhwinter.fase(s) == GuhwinterFase.KERST && Guhwinter.kerstboomOpen(s) && !Guhwinter.albumOpen(s), "24 Dec = kerst");
            helper.assertTrue(java.util.Objects.equals(GuhwinterData.load(GuhwinterData.save(d)).verschuiving, d.verschuiving), "the override is saved");
            Guhwinter.zetDatum(s, Guhwinter.leesDatum("2027-01-03"));
            helper.assertTrue(Guhwinter.fase(s) == GuhwinterFase.AFGELOPEN && !Guhwinter.bezig(s), "3 Jan (no time = 00:00) = afgelopen");
            Guhwinter.zetEcht(s);
            helper.assertTrue(!Guhwinter.nepDatum(s) && Math.abs(Guhwinter.nuMs(s) - System.currentTimeMillis()) < 5000, "echt: the real clock again");
            helper.assertTrue(Guhwinter.leesDatum("vahoeg") == null && Guhwinter.leesDatum("2026-13-01 10:00") == null, "not a date");
        } finally {
            d.verschuiving = oud;
            d.setDirty();
        }
        helper.succeed();
    }

    /** opMoment: in time order, once, a failing one isn't retried, and after a "restart" only what's new fires (catch-up). */
    @GuhTest(template = "empty", batch = B)
    public static void momenten(GameTestHelper helper) {
        MinecraftServer s = helper.getLevel().getServer();
        ZonedDateTime t1 = t("2026-12-02 00:00"), t2 = t("2026-12-05 19:00"), t3 = t("2027-01-03 00:00");
        List<String> log = new ArrayList<>();
        Momenten m = nieuw(log, t1, t2, t3);
        boolean dubbel = false;
        try {
            m.voegToe("a", t1, x -> { });
        } catch (IllegalArgumentException e) {
            dubbel = true;
        }
        helper.assertTrue(dubbel, "an id can be used once");
        GuhwinterData d = new GuhwinterData();
        int[] opgeslagen = {0};
        helper.assertTrue(m.vuur(s, t1.toInstant().toEpochMilli() - 1, d.gevuurd, () -> opgeslagen[0]++).isEmpty() && opgeslagen[0] == 0, "nothing before its time");
        List<String> uit = m.vuur(s, t2.toInstant().toEpochMilli(), d.gevuurd, () -> opgeslagen[0]++);
        helper.assertTrue(uit.equals(List.of("a", "a2", "b")) && log.equals(uit), "a, a2 (same time: registration order), b: " + uit + " / " + log);
        helper.assertTrue(d.gevuurd("b") && opgeslagen[0] > 0, "b counts as fired although it threw");
        helper.assertTrue(m.vuur(s, t2.toInstant().toEpochMilli() + 60_000, d.gevuurd, () -> { }).isEmpty(), "once only");
        // the server goes down before t3 and comes back after it: saved, loaded, the listeners registered again
        GuhwinterData na = GuhwinterData.load(GuhwinterData.save(d));
        List<String> log2 = new ArrayList<>();
        Momenten m2 = nieuw(log2, t1, t2, t3);
        List<String> inhalen = m2.vuur(s, t3.toInstant().toEpochMilli() + 86_400_000L, na.gevuurd, () -> { });
        helper.assertTrue(inhalen.equals(List.of("c")) && log2.equals(List.of("c")), "catch-up after downtime: only c: " + inhalen);
        boolean onbekend = false;
        try {
            Guhwinter.opMoment("bestaatniet:test", x -> { });
        } catch (IllegalArgumentException e) {
            onbekend = true;
        }
        helper.assertTrue(onbekend, "an unknown named moment is refused");
        helper.assertTrue(Guhwinter.momenten().stream().anyMatch(l -> l.id().equals("afgelopen:kern")), "kern's end moment is registered");
        helper.succeed();
    }

    /** Listeners registered out of time order: c (t3), a (t1), b (t2, throws), a2 (t1). */
    private static Momenten nieuw(List<String> log, ZonedDateTime t1, ZonedDateTime t2, ZonedDateTime t3) {
        Momenten m = new Momenten();
        m.voegToe("c", t3, x -> log.add("c"));
        m.voegToe("a", t1, x -> log.add("a"));
        m.voegToe("b", t2, x -> {
            log.add("b");
            throw new IllegalStateException("boem (expected in this test)");
        });
        m.voegToe("a2", t1, x -> log.add("a2"));
        return m;
    }
}
