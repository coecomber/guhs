package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.npc.Beloningen;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Klodders;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.4 xp-rem: the xp brake against much lower targets (compared with the CLASS that gets the xp, not the generic level) and the
 * xp tally log line.
 * <ul>
 *   <li>{@code formule}: 1 up to 3 levels below, then -15 % per level, at least 10 %; the zone tops; quests count as their zone's top,
 *       a job advancement never brakes;</li>
 *   <li>{@code klasse39TegenMob28}: a real kill: class 39 vs a lv-28 Klodder gets 10 % of what a class-25 player gets of the same kill;
 *       the popup shows once, then pauses; the tally counts what the brake held back;</li>
 *   <li>{@code lageKlasseVolleXp}: generic 39 with an active class of 10 vs a zone-1 mob (lv 8): full xp (generic AND class);</li>
 *   <li>{@code grensPlus3}: class 31 vs lv 28 full, 32 -> 85 %;</li>
 *   <li>{@code partyLagereKlasse}: one kill shared in a party: the class-39 member 10 %, the class-20 member full;</li>
 *   <li>{@code xpTelling}: the tally line: built from several sources (mob kill, quest, test), written on demand, after 30 min on the
 *       next gain, and on a real logout (PlayerList.remove -> PlayerLoggedOutEvent), and it reaches log4j (captured appender).</li>
 * </ul>
 * Run: {@code runGameTestServer -Pgt=VadsrijkXpRem}.
 */
public class VadsrijkXpRemGameTests {
    private static final String BATCH = "vadsrijk_xprem";

    @GuhTest(template = "empty", batch = BATCH)
    public static void formule(GameTestHelper helper) {
        helper.assertTrue(Math.abs(Spelers.xpRem(39, 28) - 0.1) < 1e-9, "39 vs 28: max(0.1, 1 - 0.15 x 8) = 0.1, got " + Spelers.xpRem(39, 28));
        helper.assertTrue(Spelers.xpRem(31, 28) == 1.0 && Spelers.xpRem(20, 28) == 1.0, "3 below or higher targets: full");
        helper.assertTrue(Math.abs(Spelers.xpRem(32, 28) - 0.85) < 1e-9 && Math.abs(Spelers.xpRem(34, 28) - 0.55) < 1e-9, "4 below 85 %, 6 below 55 %");
        helper.assertTrue(Math.abs(Spelers.xpRem(70, 1) - 0.1) < 1e-9, "never below 10 %");
        helper.assertTrue(ZonePoort.zoneTop(1) == 20 && ZonePoort.zoneTop(2) == 30 && ZonePoort.zoneTop(3) == 50 && ZonePoort.zoneTop(0) == 20,
                "zone tops 20 / 30 / 50 (0 = zone 1)");
        Quest z2 = null, klasse = null;
        for (Quest q : Quests.alle().values()) {
            if (z2 == null && q.soort() == Quest.Soort.ZIJ && ZoneBoeken.zoneVan(q.id()) == 2) {
                z2 = q;
            }
            if (klasse == null && q.soort() == Quest.Soort.KLASSE) {
                klasse = q;
            }
        }
        helper.assertTrue(z2 != null && Beloningen.remLevel(z2) == 30, "a zone-2 side quest counts as level 30");
        helper.assertTrue(Math.abs(Spelers.xpRem(39, Beloningen.remLevel(z2)) - 0.1) < 1e-9, "a zone-2 quest for a class-39 player: 10 %");
        helper.assertTrue(klasse != null && Beloningen.remLevel(klasse) == 0, "a job advancement never brakes");
        helper.succeed();
    }

    static Klodders.Klodder klodder(GameTestHelper helper, int lv) {
        ServerLevel sl = helper.getLevel();
        BlockPos b = helper.absolutePos(new BlockPos(1, 1, 1));
        Klodders.Klodder k = WezensRegistry.KLODDER.get().create(sl, EntitySpawnReason.COMMAND);
        k.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0, 0);
        k.stelIn(lv, Element.WATER, false, Set.of());
        sl.addFreshEntity(k);
        return k;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void klasse39TegenMob28(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper), q = GuhMockPlayer.of(helper);
        try {
            Spelers.zetLevel(p, 39);
            Spelers.zetLevel(q, 25);
            Spelers.vergeetRemMelding(p);
            Spelers.schrijfXpTelling(p);
            SpelerData dp = Spelers.data(p), dq = Spelers.data(q);
            long gp = dp.generiekXp, kp = dp.xp(dp.klasse), gq = dq.generiekXp;
            klodder(helper, 28).verslagen(p);
            klodder(helper, 28).verslagen(q);
            long dP = dp.generiekXp - gp, dK = dp.xp(dp.klasse) - kp, dQ = dq.generiekXp - gq;
            helper.assertTrue(dQ > 20, "class 25 vs lv 28: full xp " + dQ);
            helper.assertTrue(Math.abs(dP - Math.round(dQ * 0.1)) <= 1, "class 39 vs lv 28: 10 % of " + dQ + ", got " + dP);
            helper.assertTrue(Math.abs(dK - dP) <= 1, "the class gets the same braked amount: " + dK);
            // the popup: once, then a pause of 30 s
            helper.assertFalse(Spelers.meldRem(p, 5), "the kill already showed the popup: paused now");
            Spelers.vergeetRemMelding(p);
            helper.assertTrue(Spelers.meldRem(p, 5), "after the pause it shows again");
            String regel = Spelers.schrijfXpTelling(p);
            helper.assertTrue(regel != null && regel.contains(" mob " + dP) && regel.contains("brake -"), "the tally counts the mob xp and the brake: " + regel);
            helper.succeed();
        } finally {
            Spelers.zetLevel(p, 1);
            Spelers.zetLevel(q, 1);
        }
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void lageKlasseVolleXp(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        try {
            Spelers.zetLevel(p, 39);
            d.xp.put(d.klasse, Levels.totaalVoor(10));   // generic 39, the active (and only) class 10
            helper.assertTrue(d.level() == 39 && d.klasseLevel() == 10, "generic 39, class 10: " + d.level() + " / " + d.klasseLevel());
            helper.assertTrue(Spelers.xpRem(p, 8) == 1.0, "class 10 vs a zone-1 lv-8 mob: no brake");
            helper.assertTrue(Spelers.xpRem(39, 8) == 0.1, "(the generic level would have braked to 10 %)");
            Klodders.Klodder k = klodder(helper, 8);
            long verwacht = k.xp();
            long g0 = d.generiekXp, k0 = d.xp(d.klasse);
            k.verslagen(p);
            long dg = d.generiekXp - g0, dk = d.xp(d.klasse) - k0;
            helper.assertTrue(Math.abs(dg - verwacht) <= 1, "full generic xp " + verwacht + ", got " + dg);
            helper.assertTrue(dk >= dg, "full class xp (catch-up never lowers it): " + dk);
            helper.succeed();
        } finally {
            Spelers.zetLevel(p, 1);
        }
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void grensPlus3(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        try {
            Spelers.zetLevel(p, 31);
            long g0 = d.generiekXp;
            Spelers.geefXpDoel(p, 1000, "test", 28);
            helper.assertTrue(d.generiekXp - g0 == 1000, "class 31 vs lv 28 (3 below): full, got " + (d.generiekXp - g0));
            Spelers.zetLevel(p, 32);
            g0 = d.generiekXp;
            Spelers.geefXpDoel(p, 1000, "test", 28);
            helper.assertTrue(d.generiekXp - g0 == 850, "class 32 vs lv 28 (4 below): 85 %, got " + (d.generiekXp - g0));
            helper.succeed();
        } finally {
            Spelers.zetLevel(p, 1);
        }
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void partyLagereKlasse(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper), q = GuhMockPlayer.of(helper);
        try {
            Spelers.zetLevel(p, 39);
            Spelers.zetLevel(q, 20);
            Partijen.nodigUit(p, q);
            Partijen.accepteer(q);
            helper.assertTrue(Partijen.zelfdeParty(p, q), "p and q are a party");
            long gp = Spelers.data(p).generiekXp, gq = Spelers.data(q).generiekXp;
            // (party sharing needs both in Het Vadsrijk; the gametest level is not, so both are helpers here: the brake is decided per
            // receiving player in geefXpGroep either way)
            var ontvangers = Spelers.geefXpGroep(List.of(p, q), 1000, "mob", 18);
            helper.assertTrue(ontvangers.size() == 2, "both got the kill once: " + ontvangers.size());
            long dp = Spelers.data(p).generiekXp - gp, dq = Spelers.data(q).generiekXp - gq;
            helper.assertTrue(dp == 100, "class 39 vs lv 18: 10 %, got " + dp);
            helper.assertTrue(dq == 1000, "class 20 vs lv 18 (party mate at a lower class): full, got " + dq);
            helper.succeed();
        } finally {
            Partijen.verlaat(q.getUUID());
            Partijen.verlaat(p.getUUID());
            Spelers.zetLevel(p, 1);
            Spelers.zetLevel(q, 1);
        }
    }

    /** Captures what log4j receives from Spelers' logger. */
    static final class Vanger extends org.apache.logging.log4j.core.appender.AbstractAppender {
        final List<String> regels = new ArrayList<>();

        Vanger() {
            super("GuhsXpTellingTest", null, null, true, org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY);
        }

        @Override
        public void append(org.apache.logging.log4j.core.LogEvent event) {
            String m = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
            if (m.contains("xp tally")) {
                synchronized (regels) {
                    regels.add(event.getLoggerName() + " " + event.getLevel() + " " + m);
                }
            }
        }
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void xpTelling(GameTestHelper helper) {
        var ctx = (org.apache.logging.log4j.core.LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        Vanger v = new Vanger();
        v.start();
        ctx.getConfiguration().getRootLogger().addAppender(v, org.apache.logging.log4j.Level.ALL, null);
        ctx.updateLoggers();
        ServerPlayer p = GuhMockPlayer.of(helper);
        try {
            Spelers.zetLevel(p, 39);
            Spelers.schrijfXpTelling(p);
            // several sources: a real mob kill (braked), a braked quest gain, a plain gain
            klodder(helper, 28).verslagen(p);
            Spelers.geefXpDoel(p, 500, "quest", ZonePoort.zoneTop(2));
            Spelers.geefXp(p, 40, "test");
            String regel = Spelers.schrijfXpTelling(p);
            helper.assertTrue(regel != null && regel.startsWith("[Vadsrijk] xp tally test-mock-player lv 39") && regel.contains(" mob ")
                    && regel.contains(" quest 50 ") && regel.contains(" test 40 ") && regel.contains("brake -"), "the line on demand: " + regel);
            helper.assertTrue(Spelers.schrijfXpTelling(p) == null, "written: the tally starts over");
            // 30 minutes later: the next gain writes it
            Spelers.geefXp(p, 10, "test");
            Spelers.zetXpTellingSinds(p, Spelers.XP_TEL_MS + 1000);
            Spelers.laatsteXpTelling = null;
            Spelers.geefXp(p, 10, "test");
            helper.assertTrue(Spelers.laatsteXpTelling != null && Spelers.laatsteXpTelling.contains(" test 20 (100%, 2x)") && Spelers.laatsteXpTelling.contains(" over 30 min"),
                    "the 30-minute line: " + Spelers.laatsteXpTelling);
            // the minute check writes a 30-minute window even without a new gain
            Spelers.geefXp(p, 5, "test");
            Spelers.zetXpTellingSinds(p, Spelers.XP_TEL_MS + 1000);
            Spelers.laatsteXpTelling = null;
            Spelers.schrijfVerlopenTellingen(helper.getLevel().getServer());
            helper.assertTrue(Spelers.laatsteXpTelling != null && Spelers.laatsteXpTelling.contains(" test 5 "), "the minute check: " + Spelers.laatsteXpTelling);
            // in Het Vadsrijk itself (the HUD sync after a gain runs there): still counted and written
            ServerLevel vr = Vadsrijk.level(helper.getLevel().getServer());
            if (vr != null) {   // (the gametest server has no Vadsrijk level loaded: then skipped)
                p.teleportTo(vr, 0.5, 120, 0.5, Set.of(), 0, 0, false);
                Spelers.geefXp(p, 9, "mob");
                String inRijk = Spelers.schrijfXpTelling(p);
                helper.assertTrue(Vadsrijk.in(p) && inRijk != null && inRijk.contains(" mob 9 "), "in Het Vadsrijk: " + inRijk);
            }
            // a real logout (PlayerList.remove fires PlayerLoggedOutEvent -> KernEvents)
            Spelers.geefXp(p, 7, "test");
            Spelers.laatsteXpTelling = null;
            helper.getLevel().getServer().getPlayerList().remove(p);
            helper.assertTrue(Spelers.laatsteXpTelling != null && Spelers.laatsteXpTelling.contains(" test 7 "), "the logout line: " + Spelers.laatsteXpTelling);
            synchronized (v.regels) {
                helper.assertTrue(v.regels.size() >= (vr != null ? 5 : 4) && v.regels.stream().allMatch(r -> r.contains(" INFO ")),
                        "every line reached log4j at INFO (no filter): " + v.regels);
            }
            helper.succeed();
        } finally {
            ctx.getConfiguration().getRootLogger().removeAppender(v.getName());
            ctx.updateLoggers();
            v.stop();
            Spelers.zetLevel(p, 1);
        }
    }

    /**
     * Zone-1 quests whose json level (mostly 1) is far below the level you are when you hand them in: the estimate from their place
     * in the chain (tutorial hq01-hq07, the Zwamzoet side quests after hq06/hq07, the woods chapter hq08, the epilogue hq12) and
     * the secret quest (its clues are spread over the whole zone).
     */
    static final java.util.Map<String, Integer> VERWACHT = java.util.Map.ofEntries(
            java.util.Map.entry("hq01_welkom", 1), java.util.Map.entry("hq02_pad", 2), java.util.Map.entry("hq03_poppen", 2),
            java.util.Map.entry("hq03b_elementen", 3), java.util.Map.entry("hq03c_opruimen", 3), java.util.Map.entry("hq04_kampvuur", 4),
            java.util.Map.entry("hq05_klodderreus", 4), java.util.Map.entry("hq06_zwamzoet", 5), java.util.Map.entry("hq07_littekens", 6),
            java.util.Map.entry("hq08_sporenwoud", 8), java.util.Map.entry("hq12_schaduw", 19), java.util.Map.entry("k01_dobber", 2),
            java.util.Map.entry("k02_uitzicht", 2), java.util.Map.entry("geheim_spion", 12));

    /** The level you are when you hand a quest in: the estimate above, else its own level or that of a quest before it. */
    static int verwachtLevel(Quest q, java.util.Map<String, Quest> alle, int diepte) {
        Integer v = VERWACHT.get(q.id());
        if (v != null) {
            return v;
        }
        int lv = Math.max(1, q.lv());
        if (diepte < 40) {
            for (String na : q.na()) {
                Quest n = alle.get(na);
                if (n != null) {
                    lv = Math.max(lv, verwachtLevel(n, alle, diepte + 1));
                }
            }
        }
        return lv;
    }

    /** 1.10.4: no quest gives more than 1.5 level at the level you hand it in (after the main-story zone factor); secret quests ~0.75. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void questXpPlafond(GameTestHelper helper) {
        var alle = Quests.alle();
        helper.assertTrue(alle.size() > 150, "all quest files loaded: " + alle.size());
        List<String> te = new ArrayList<>();
        int geheim = 0;
        for (Quest q : alle.values()) {
            long xp = Math.max(0, Math.round(q.beloning().xp() * Beloningen.xpFactor(q)));
            int lv = verwachtLevel(q, alle, 0);
            double deel = xp / (double) Levels.xpVoor(lv);
            if (deel > 1.5) {
                te.add(String.format(java.util.Locale.ROOT, "%s %d xp at lv %d = %.2f level", q.id(), xp, lv, deel));
            }
            if (q.soort() == Quest.Soort.GEHEIM) {
                geheim++;
                helper.assertTrue(deel > 0.6 && deel < 0.9, "secret quest " + q.id() + ": ~0.75 level, got " + deel);
            }
        }
        helper.assertTrue(te.isEmpty(), "quests above 1.5 level: " + te);
        helper.assertTrue(geheim >= 3, "the secret quests were checked: " + geheim);
        helper.assertTrue(Quests.van("z2_geheim_boormeester").beloning().xp() < 4000, "live: the Boormeester secret quest gave 15,000 xp (2.9 levels)");
        helper.succeed();
    }
}
