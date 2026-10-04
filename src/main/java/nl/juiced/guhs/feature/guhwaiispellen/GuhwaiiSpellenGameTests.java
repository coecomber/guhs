package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.MerchantOffer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * GameTests of the surf beach of Guhwai'i (3.0, guhwaii-spellen): the surf ride (catching, pumping, a knabbeldraai off
 * the lip, the whitewater, a crooked landing), a whole surf game with Lilo-guh (schelpjesmunten, the level's own board),
 * the hula steps on the song's beat, the judging and a dance with Lilo-guh on the flower mat, Tikiguh's shop, the levels'
 * own boards and Highscores rows, and finding the open water from the surf shack.
 */
public class GuhwaiiSpellenGameTests {
    private static final String STRAND = "guhwaiispellen_test_strand";
    private static final String WATER = "guhwaiispellen_test_water";

    // --- a surf bot: paddles to the peak, catches the wave facing away from the break, pumps, spins off the lip ------------

    static int bot(SurfSim s, int spin) {
        int in = 0;
        switch (s.fase()) {
            case PEDDELEN -> {
                int k = s.volgende();
                if (k >= 0) {
                    SurfGolven g = s.golven();
                    double doel = g.golf(k).piek() + g.golf(k).kant() * 5;
                    boolean dicht = Math.abs(g.crest(k, s.step() + 1) - s.u()) < g.stand().vangen() * 1.5;
                    if (!dicht && s.v() < doel - 0.5) {
                        in |= SurfSim.LINKS;
                    } else if (!dicht && s.v() > doel + 0.5) {
                        in |= SurfSim.RECHTS;
                    }
                    if (Math.abs(g.crest(k, s.step() + 1) - s.u()) < g.stand().vangen() * 0.6) {
                        in |= SurfSim.VOORUIT;
                    }
                }
            }
            case RIJDEN -> {
                in |= s.step() % 28 < 14 ? SurfSim.VOORUIT : SurfSim.ACHTERUIT;
                if (s.face() > -0.45 && s.vaart() >= SurfSim.SPRING_VAART + 0.02) {
                    in |= SurfSim.SPRING;
                }
            }
            case LUCHT -> {
                if (Math.abs(s.spin()) < spin - 10) {
                    in |= SurfSim.LINKS;
                }
            }
            default -> {
            }
        }
        return in;
    }

    /** The ride: you catch waves, ride them, land a knabbeldraai for its points (x the multiplier), and it all ends. */
    @GuhTest(template = STRAND)
    public static void guhwaiispellenSurfScoreEnTrucs(GameTestHelper helper) {
        for (Niveau n : Niveau.values()) {
            boolean draai = false;
            for (int seed = 1; seed <= 6; seed++) {
                SurfSim s = new SurfSim(n, seed);
                List<SurfSim.Gebeurtenis> alles = new ArrayList<>();
                while (!s.klaar() && s.step() < 5000) {
                    alles.addAll(s.stap(bot(s, 360)));
                }
                helper.assertTrue(s.klaar(), n + ": the game ends (" + s.step() + " steps, " + s.golven().duur() + " at most)");
                helper.assertTrue(s.step() <= s.golven().duur(), n + ": within its time");
                helper.assertTrue(alles.stream().anyMatch(e -> e.soort() == SurfSim.Soort.VANG), n + ": a wave is caught");
                helper.assertTrue(alles.stream().anyMatch(e -> e.soort() == SurfSim.Soort.GOLF_KLAAR), n + ": a wave is ridden out");
                helper.assertTrue(s.score() > 0 && s.gereden() > 0, n + ": points for riding: " + s.score());
                int som = alles.stream().mapToInt(SurfSim.Gebeurtenis::punten).sum();
                helper.assertTrue(som < s.score(), n + ": the tricks are part of the score (riding points on top)");
                draai |= alles.stream().anyMatch(e -> e.soort() == SurfSim.Soort.TRUC && e.naam().startsWith("draai") && e.punten() >= 350);
            }
            helper.assertTrue(draai, n + ": a knabbeldraai off the lip (350 x the multiplier)");
        }
        // the same keys give the same ride (the surfer's game and the server agree), a different seed other waves
        SurfSim a = new SurfSim(Niveau.MEDIUM, 42), b = new SurfSim(Niveau.MEDIUM, 42);
        while (!a.klaar()) {
            int in = bot(a, 360);
            a.stap(in);
            b.stap(in);
            helper.assertTrue(a.u() == b.u() && a.v() == b.v() && a.score() == b.score(), "deterministic at step " + a.step());
        }
        helper.assertTrue(new SurfGolven(Niveau.MEDIUM, 42).golf(0).piek() != new SurfGolven(Niveau.MEDIUM, 43).golf(0).piek()
                || new SurfGolven(Niveau.MEDIUM, 42).golf(1).piek() != new SurfGolven(Niveau.MEDIUM, 43).golf(1).piek(), "other seed, other waves");
        // a crooked landing (a quarter turn) is a PLONS: the multiplier back to 1, the wave gone, never points off
        SurfSim c = new SurfSim(Niveau.LASTIG, 7);
        boolean plons = false;
        int voor = 0;
        while (!c.klaar() && !plons) {
            voor = c.score();
            for (SurfSim.Gebeurtenis e : c.stap(bot(c, 90))) {
                if (e.soort() == SurfSim.Soort.PLONS && e.naam().equals("scheef")) {
                    plons = true;
                }
            }
        }
        helper.assertTrue(plons, "a quarter turn lands crooked: PLONS");
        helper.assertTrue(c.mult() == 1 && c.score() >= voor, "after a PLONS: multiplier 1, no points lost");
        // makkelijk catches by itself, lastig only when you paddle (W)
        SurfSim zelf = new SurfSim(Niveau.MAKKELIJK, 3), stil = new SurfSim(Niveau.LASTIG, 3);
        boolean gevangen = false, stilGevangen = false;
        for (int i = 0; i < 900; i++) {
            gevangen |= zelf.stap(0).stream().anyMatch(e -> e.soort() == SurfSim.Soort.VANG);
            stilGevangen |= stil.stap(0).stream().anyMatch(e -> e.soort() == SurfSim.Soort.VANG);
        }
        helper.assertTrue(gevangen && !stilGevangen, "makkelijk catches by itself (" + gevangen + "), lastig needs W (" + stilGevangen + ")");
        // Lilo-guh surfs too
        SurfSim lilo = new SurfSim(Niveau.MEDIUM, 5);
        while (!lilo.klaar()) {
            lilo.stap(SurfSim.liloInvoer(lilo));
        }
        helper.assertTrue(lilo.gereden() > 0, "Lilo-guh rides waves: " + lilo.gereden());
        helper.assertTrue(SurfSim.munten(0) == 0 && SurfSim.munten(299) == 0 && SurfSim.munten(300) == 1 && SurfSim.munten(1400) == 3,
                "schelpjesmunten for a surf score");
        helper.succeed();
    }

    private static GuhNpcEntity lilo(GameTestHelper helper, String plek, BlockPos rel) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.LILO_GUH);
        npc.roleData.putString(NpcRollen.PLEK, plek);
        BlockPos p = helper.absolutePos(rel);
        npc.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, GuhNpcEntity npc) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.snapTo(npc.getX(), npc.getY(), npc.getZ() + 1.5);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    /** 1.2.7: a dancer whose Lilo-guh stopped ticking (teleported away) doesn't stay "dancing": the watchdog frees them. */
    @GuhTest(template = STRAND, timeoutTicks = 100, batch = "guhwaiispellen_hula_waak")
    public static void guhwaiispellenHulaDanserBlijftNietHangen(GameTestHelper helper) {
        GuhNpcEntity npc = lilo(helper, "hula", new BlockPos(6, 2, 2));
        ServerPlayer p = speler(helper, npc);
        HulaSpel spel = HulaSpel.of(npc);
        helper.assertTrue(spel.start(npc, p, HulaLiedje.GUHLA_HULA_ROCK) && HulaSpel.danst(p), "the dance starts");
        helper.assertTrue(!HulaSpel.waak(p) && HulaSpel.danst(p), "Lilo-guh is there: dancing on");
        spel.testStil(helper.getLevel(), HulaSpel.STIL_TICKS + 5);
        helper.assertTrue(HulaSpel.waak(p) && !HulaSpel.danst(p) && nl.juiced.guhs.feature.Minigames.playing(p) == null,
                "Lilo-guh hasn't ticked for 2 seconds: an ordinary player again");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!spel.bezig(), "and Lilo-guh tidies up her side when she ticks again");
            npc.discard();
            weg(helper, p);
            helper.succeed();
        });
    }

    /** A whole surf game with Lilo-guh: the roles, the loaned board, the boards on the water, schelpjesmunten, the level's own board. */
    @GuhTest(template = STRAND, timeoutTicks = 200)
    public static void guhwaiispellenSurfSpelMunten(GameTestHelper helper) {
        GuhNpcEntity npc = lilo(helper, "surf", new BlockPos(2, 2, 2));
        helper.assertTrue(NpcRollen.van(npc) == GuhwaiiSpellenFeature.LILO_SURF && Features.role(GuhNpcEntity.Kind.LILO_GUH) != null,
                "Lilo-guh at the surf shack has the surf role");
        ServerPlayer p = speler(helper, npc);
        // (the spot's line-up lands on the test floor: the origin lies 38 blocks back, the waves roll in towards it)
        Surfplek.Spot spot = new Surfplek.Spot(helper.absolutePos(new BlockPos(6, 1, 6)).offset(-(int) SurfGolven.U_LINE, 0, 0), 0);
        helper.assertTrue(SurfSpel.start(npc, p, Niveau.MEDIUM, spot, 11), "the game starts");
        SurfSpel s = SurfSpel.van(p);
        helper.assertTrue(s != null && SurfSpel.surft(p) && nl.juiced.guhs.feature.Minigames.playing(p) != null, "surfing (a minigame)");
        helper.assertTrue(p.getVehicle() instanceof SurfPlankEntity bord && !bord.isLilo() && s.liloBord != null && s.liloBord.isLilo(),
                "you stand on your board, Lilo-guh has her own");
        helper.assertTrue(GuhQuests.count(p, GuhwaiiSpellenBlocks.SURFPLANKJE_LEEN.get()) == 1, "the loaned surfplankje");
        while (!s.sim().klaar()) {
            s.stapNu(p, bot(s.sim(), 360));
        }
        int score = s.sim().score();
        helper.assertTrue(score > 0, "points: " + score);
        s.einde(helper.getLevel(), p, true);
        int verwacht = Niveau.MEDIUM.munten(SurfSim.munten(score)) + SurfSpel.EERSTE_MUNTEN;
        helper.assertTrue(GuhQuests.count(p, GuhwaiiSpellenBlocks.SCHELPJESMUNT.get()) == verwacht,
                "schelpjesmunten for " + score + " (+ the welcome present): " + GuhQuests.count(p, GuhwaiiSpellenBlocks.SCHELPJESMUNT.get()) + " != " + verwacht);
        helper.assertTrue(GuhQuests.count(p, GuhwaiiSpellenBlocks.SURFPLANKJE_LEEN.get()) == 0 && !SurfSpel.surft(p) && p.getVehicle() == null,
                "the board goes back, the game is over");
        helper.assertTrue(!Scorebord.top(p.level().getServer(), "surfen_medium").isEmpty() && Scorebord.top(p.level().getServer(), "surfen_lastig").stream()
                .noneMatch(e -> e.player().equals(p.getUUID())), "on the medium board only");
        helper.assertTrue(GuhwaiiSpellenFeature.data(p).getIntOr("Surf_medium", 0) == score, "the record of medium");
        helper.assertTrue(p.getAdvancements().getOrStartProgress(p.level().getServer().getAdvancements().get(Guhs.id("verhalen/guhwaii_spellen_surf"))).isDone(),
                "the advancement of a ridden wave");
        weg(helper, p);
        helper.succeed();
    }

    /** The hula steps sit on the song's beat grid (whole beats, eighths on lastig), makkelijk only the hips, lastig the VAHOEG!s. */
    @GuhTest(template = STRAND)
    public static void guhwaiispellenHulaOpDeBeat(GameTestHelper helper) {
        for (HulaLiedje l : HulaLiedje.values()) {
            List<HulaKaart.Noot> kaart = HulaKaart.van(l);
            helper.assertTrue(kaart.size() >= 40, l + ": enough steps: " + kaart.size());
            double vorige = -1;
            Set<HulaKaart.Pas> passen = new HashSet<>();
            for (HulaKaart.Noot n : kaart) {
                helper.assertTrue(n.beat() > vorige, l + ": in time order");
                helper.assertTrue(Math.abs(n.beat() * 2 - Math.round(n.beat() * 2)) < 1e-9, l + ": on the grid (eighths): " + n.beat());
                if (l.niveau != Niveau.LASTIG) {
                    helper.assertTrue(l == HulaLiedje.GUHLA_HULA_ROCK || n.beat() == Math.floor(n.beat()), l + ": makkelijk on whole beats");
                }
                helper.assertTrue(Math.abs(l.tick(n.beat()) - n.beat() * 1200.0 / l.bpm) < 1e-9 && Math.abs(l.ms(n.beat()) - n.beat() * 60000.0 / l.bpm) < 1e-9,
                        l + ": the time of a beat is beat x 60 / BPM");
                passen.add(n.pas());
                vorige = n.beat();
            }
            helper.assertTrue(kaart.get(kaart.size() - 1).beat() == l.maten * 4, l + ": the last step on the final chord (beat " + l.maten * 4 + ")");
            helper.assertTrue(l.ms(l.maten * 4) / 1000.0 < l.seconds, l + ": the song is long enough");
            if (l.niveau == Niveau.MAKKELIJK) {
                helper.assertTrue(passen.equals(Set.of(HulaKaart.Pas.LINKS, HulaKaart.Pas.RECHTS)), "makkelijk: only the hips " + passen);
            } else if (l.niveau == Niveau.MEDIUM) {
                helper.assertTrue(passen.size() == 4 && !passen.contains(HulaKaart.Pas.VAHOEG), "medium: the four moves " + passen);
            } else {
                helper.assertTrue(passen.size() == 5, "lastig: all five, with VAHOEG! " + passen);
            }
            helper.assertTrue(HulaLiedje.of(l.niveau) == l && l.board().equals("hula_" + l.niveau.id()), l + ": the song is the level");
        }
        helper.assertTrue(HulaLiedje.ALOHA_NJEG.bpm == 88 && HulaLiedje.GUHLA_HULA_ROCK.bpm == 132 && HulaLiedje.VAHOEG_HULA_HOP.bpm == 160, "the BPMs");
        // the judging windows
        HulaLiedje m = HulaLiedje.GUHLA_HULA_ROCK;
        helper.assertTrue(HulaKaart.oordeel(m, 0) == HulaKaart.Oordeel.VAHOEG && HulaKaart.oordeel(m, -50) == HulaKaart.Oordeel.VAHOEG
                && HulaKaart.oordeel(m, 90) == HulaKaart.Oordeel.NJEG && HulaKaart.oordeel(m, -150) == HulaKaart.Oordeel.GUH
                && HulaKaart.oordeel(m, 200) == HulaKaart.Oordeel.MIS, "VAHOEG! / Njeg! / Guh. / Mis");
        helper.assertTrue(HulaKaart.oordeel(HulaLiedje.ALOHA_NJEG, 65) == HulaKaart.Oordeel.VAHOEG && HulaKaart.oordeel(HulaLiedje.VAHOEG_HULA_HOP, 65)
                == HulaKaart.Oordeel.NJEG, "makkelijk is a bit more forgiving");
        helper.assertTrue(HulaKaart.punten(HulaKaart.Oordeel.VAHOEG, 0) == 100 && HulaKaart.punten(HulaKaart.Oordeel.VAHOEG, 40) == 300
                && HulaKaart.punten(HulaKaart.Oordeel.MIS, 10) == 0, "the combo multiplies up to x3");
        helper.succeed();
    }

    /** A dance with Lilo-guh on the flower mat: steps judged on the beat, a wrong move doesn't count, misses, coins, the board. */
    @GuhTest(template = STRAND, timeoutTicks = 200)
    public static void guhwaiispellenHulaDans(GameTestHelper helper) {
        GuhNpcEntity npc = lilo(helper, "hula", new BlockPos(6, 2, 2));
        helper.assertTrue(NpcRollen.van(npc) == GuhwaiiSpellenFeature.LILO_HULA, "Lilo-guh on the podium has the hula role");
        ServerPlayer p = speler(helper, npc);
        HulaSpel spel = HulaSpel.of(npc);
        helper.assertTrue(spel.start(npc, p, HulaLiedje.GUHLA_HULA_ROCK) && HulaSpel.danst(p), "the dance starts");
        helper.assertTrue(spel.mat() != null && spel.mat().equals(helper.absolutePos(new BlockPos(6, 2, 6))), "on the flower mat: " + spel.mat());
        helper.assertTrue(p.blockPosition().equals(spel.mat()), "the dancer stands on the mat");
        List<HulaKaart.Noot> kaart = HulaKaart.van(HulaLiedje.GUHLA_HULA_ROCK);
        HulaKaart.Noot eerste = kaart.get(0), tweede = kaart.get(1), derde = kaart.get(2);
        HulaLiedje l = HulaLiedje.GUHLA_HULA_ROCK;
        spel.verschuif((long) Math.ceil(l.tick(eerste.beat())));             // (the song is at the first step)
        int wrong = (eerste.pas().ordinal() + 1) % HulaKaart.Pas.values().length;
        spel.tik(npc, p, eerste.index(), (int) Math.round(l.ms(eerste.beat())), wrong);
        helper.assertTrue(spel.tel(HulaKaart.Oordeel.VAHOEG) == 0 && spel.score() == 0, "the wrong move doesn't count");
        spel.tik(npc, p, eerste.index(), (int) Math.round(l.ms(eerste.beat()) + 10), eerste.pas().ordinal());
        spel.tik(npc, p, tweede.index(), (int) Math.round(l.ms(tweede.beat()) - 95), tweede.pas().ordinal());
        spel.tik(npc, p, derde.index(), (int) Math.round(l.ms(derde.beat()) + 160), derde.pas().ordinal());
        helper.assertTrue(spel.tel(HulaKaart.Oordeel.VAHOEG) == 1 && spel.tel(HulaKaart.Oordeel.NJEG) == 1 && spel.tel(HulaKaart.Oordeel.GUH) == 1,
                "on the beat: VAHOEG!, a bit early: Njeg!, late: Guh.");
        int na3 = spel.score();
        helper.assertTrue(na3 == HulaKaart.punten(HulaKaart.Oordeel.VAHOEG, 1) + HulaKaart.punten(HulaKaart.Oordeel.NJEG, 2)
                + HulaKaart.punten(HulaKaart.Oordeel.GUH, 3), "points with the combo: " + na3);
        spel.tik(npc, p, eerste.index(), (int) Math.round(l.ms(eerste.beat())), eerste.pas().ordinal());
        helper.assertTrue(spel.score() == na3, "a step counts once");
        // time goes on: the steps nobody danced are misses, the combo is gone
        spel.verschuif(200);
        spel.tick(npc);
        helper.assertTrue(spel.tel(HulaKaart.Oordeel.MIS) > 0 && spel.combo() == 0, "missed steps: " + spel.tel(HulaKaart.Oordeel.MIS));
        helper.assertTrue(!spel.foutloos(), "not flawless any more");
        // the end: schelpjesmunten for the score (none below 800: only the welcome present), the medium board
        spel.klaar(npc, p);
        helper.assertTrue(!HulaSpel.danst(p) && !spel.bezig(), "the dance is over");
        int munten = Niveau.MEDIUM.munten(HulaKaart.munten(na3)) + HulaSpel.EERSTE_MUNTEN;
        helper.assertTrue(GuhQuests.count(p, GuhwaiiSpellenBlocks.SCHELPJESMUNT.get()) == munten, "coins: " + munten);
        helper.assertTrue(Scorebord.top(p.level().getServer(), "hula_medium").stream().anyMatch(e -> e.player().equals(p.getUUID()) && e.score() == na3),
                "on the hula_medium board");
        helper.assertTrue(GuhwaiiSpellenFeature.data(p).getIntOr("Hula_medium", 0) == na3, "the record of the medium song");
        weg(helper, p);
        helper.succeed();
    }

    /** Tikiguh's stall: every Tiki decoration for schelpjesmunten, never sold out, no clothes. */
    @GuhTest(template = STRAND)
    public static void guhwaiispellenTikiWinkel(GameTestHelper helper) {
        var offers = TikiWinkel.offers();
        Set<Item> verkocht = new HashSet<>();
        for (MerchantOffer o : offers) {
            helper.assertTrue(o.getBaseCostA().is(GuhwaiiSpellenBlocks.SCHELPJESMUNT.get()) && o.getBaseCostA().getCount() > 0, "paid in schelpjesmunten");
            helper.assertTrue(o.getMaxUses() == Integer.MAX_VALUE, "never sold out");
            helper.assertTrue(!(o.getResult().getItem() instanceof nl.juiced.guhs.item.GuhClothingItem), "no clothes");
            verkocht.add(o.getResult().getItem());
        }
        for (var b : GuhwaiiSpellenBlocks.TIKI) {
            helper.assertTrue(verkocht.contains(b.get().asItem()), "Tikiguh sells " + b.getId());
        }
        helper.assertTrue(NpcRollen.rol(GuhNpcEntity.Kind.TIKIGUH) != null, "Tikiguh has his role");
        GuhNpcEntity tiki = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        tiki.setKind(GuhNpcEntity.Kind.TIKIGUH);
        helper.assertTrue(NpcRollen.van(tiki) == GuhwaiiSpellenFeature.TIKIGUH && NpcRollen.van(tiki).offers(tiki).size() == offers.size(),
                "his shop is the Tiki stall");
        helper.succeed();
    }

    /** Each level has its own board and Highscores row; the Minigames group lists all six. */
    @GuhTest(template = STRAND)
    public static void guhwaiispellenNiveausEigenBorden(GameTestHelper helper) {
        Set<String> borden = new HashSet<>();
        for (Niveau n : Niveau.values()) {
            borden.add("surfen_" + n.id());
            borden.add(HulaLiedje.of(n).board());
        }
        helper.assertTrue(borden.size() == 6, "six boards: " + borden);
        for (String b : borden) {
            helper.assertTrue(Highscores.GAMES.stream().anyMatch(g -> g.id().equals(b) && g.board().equals(b) && !g.lowerIsBetter()),
                    "a Highscores row (points) for " + b);
        }
        SpelGroepen.Groep groep = SpelGroepen.van("guhwaii_spellen");
        helper.assertTrue(groep != null && new HashSet<>(groep.spellen()).equals(borden) && groep.tijdperk() == SpelGroepen.Tijdperk.VERHALEN,
                "the Minigames group has the six rows");
        helper.assertTrue(groep.icoon().get().is(GuhwaiiSpellenBlocks.SCHELPJESMUNT.get()), "its coin is the schelpjesmunt");
        helper.succeed();
    }

    /** From the surf shack, the way to the open water is found (the direction along the water, its first block at the beach). */
    @GuhTest(template = WATER)
    public static void guhwaiispellenSurfplekZoekt(GameTestHelper helper) {
        BlockPos lilo = helper.absolutePos(new BlockPos(2, 2, 5));
        Surfplek.Spot spot = Surfplek.zoek(helper.getLevel(), lilo, 8, 16);
        if (spot == null) {
            StringBuilder b = new StringBuilder();
            for (int x = 0; x < 12; x++) {
                BlockPos p = lilo.offset(x, 0, 0);
                for (int y = 3; y >= -3; y--) {
                    b.append(x).append(',').append(y).append('=').append(helper.getLevel().getBlockState(p.above(y)).getBlock().getDescriptionId()
                            .replace("block.minecraft.", "")).append(' ');
                }
                b.append("| ");
            }
            helper.fail("no surf spot: " + b);
        }
        helper.assertTrue(Math.cos(spot.hoek()) > 0.9, "out along the water (+x): " + Math.toDegrees(spot.hoek()));
        helper.assertTrue(helper.getLevel().getFluidState(spot.origin()).isSource() && spot.origin().getX() - lilo.getX() <= 5,
                "its origin is the first water at the beach: " + spot.origin().subtract(lilo));
        helper.assertTrue(Surfplek.zoek(helper.getLevel(), lilo, 8, 60) == null, "not when the water is too short");
        helper.succeed();
    }
}
