package nl.juiced.guhs.feature.guhpixel.guhkade;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxTest;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.guhpixel.Winkel;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Opname;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Spel;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the Guhkade (batch px_guhkade; run with {@code -Pgt=px_guhkade}): the cabinet block (two halves, its
 * block entity, breaking it), the two games (exact: the same seed and input give the same game, a recording played again
 * gives the same score), a player's game from right-click to the list with the server counting the score itself (and
 * refusing what it did not hand out), the top 5 with names for two players, how good guhs get and that their ceiling can
 * be beaten, a guh that walks up and really plays, the guh that is beaten (sad, practises more), the shop prices and the
 * Guhdex section. The test room guhkade_test_kasten has both cabinets against its north side, screens to the south.
 */
public class PxGuhkadeGameTests {
    private static final String BATCH = "px_guhkade", KAMER = "guhkade_test_kasten";
    /** The lower halves in the room (the floor is relative y 1). */
    private static final BlockPos FLAPPY = new BlockPos(5, 2, 3), PONG = new BlockPos(10, 2, 3);

    private static KastBlockEntity kast(GameTestHelper helper, BlockPos rel) {
        KastBlockEntity be = Guhkade.kast(helper.getLevel(), helper.absolutePos(rel));
        helper.assertTrue(be != null, "a cabinet at " + rel);
        return be;
    }

    private static ServerPlayer speler(GameTestHelper helper, BlockPos rel) {
        ServerPlayer p = PxTest.speler(helper);
        Vec3 v = Vec3.atBottomCenterOf(helper.absolutePos(rel));
        p.snapTo(v.x, v.y, v.z);
        return p;
    }

    private static GuhEntity guh(GameTestHelper helper, ServerPlayer baas, BlockPos rel, String naam) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), rel);
        guh.tame(baas);
        guh.setCustomName(Component.literal(naam));
        return guh;
    }

    /** A whole game played by the games' own bot, as a client would record it. */
    private static Opname opname(Spel spel, long seed, int doel) {
        Sim sim = spel.nieuw(seed);
        Opname o = new Opname(sim.bits());
        while (!sim.af()) {
            int invoer = sim.bot(doel);
            o.schrijf(invoer);
            sim.stap(invoer);
        }
        return o;
    }

    private static int tel(ServerPlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    // =====================================================================================================================
    // the block
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void kastStaatEnValtSamen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos onder = helper.absolutePos(FLAPPY);
        helper.assertTrue(level.getBlockState(onder).is(GuhkadeSlice.KAST_FLAPPY.get())
                && level.getBlockState(onder).getValue(KastBlock.HALF) == DoubleBlockHalf.LOWER
                && level.getBlockState(onder.above()).getValue(KastBlock.HALF) == DoubleBlockHalf.UPPER, "two halves");
        KastBlockEntity flappy = kast(helper, FLAPPY), pong = kast(helper, PONG);
        helper.assertTrue(flappy.spel() == Spel.FLAPPY && pong.spel() == Spel.PONG, "each cabinet its own game");
        helper.assertTrue(Guhkade.kast(level, onder.above()) == null, "the upper half is no cabinet of its own");
        helper.assertTrue(flappy.top().size() == 3 && flappy.top().get(0).huis() && flappy.top().get(0).score() > flappy.top().get(2).score(),
                "a cabinet comes with three house names, the best first: " + flappy.top());
        helper.assertTrue(Guhkade.Kasten.rond(level, onder, 12).size() == 2 && Guhkade.Kasten.rond(level, onder, 12).get(0) == flappy,
                "both cabinets are known, the nearest first");
        // breaking the upper half takes the lower half with it and drops exactly one cabinet
        level.destroyBlock(onder.above(), true);
        helper.assertTrue(level.getBlockState(onder).isAir() && level.getBlockState(onder.above()).isAir(), "both halves are gone");
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(1), e -> e.getItem().is(GuhkadeSlice.KAST_FLAPPY_ITEM.get()));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().getCount() == 1, "one cabinet dropped, got " + drops.size());
        helper.assertTrue(Guhkade.Kasten.rond(level, onder, 12).size() == 1, "the broken one is forgotten");
        drops.forEach(ItemEntity::discard);
        // and putting one down again makes both halves
        KastBlock.bouw(level, onder, GuhkadeSlice.KAST_PONG.get(), net.minecraft.core.Direction.SOUTH);
        helper.assertTrue(Guhkade.kast(level, onder) != null && Guhkade.kast(level, onder).spel() == Spel.PONG, "built again");
        helper.succeed();
    }

    // =====================================================================================================================
    // the games
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void spellenZijnExact(GameTestHelper helper) {
        for (Spel spel : Spel.values()) {
            for (long seed : new long[] {1L, 42L, -77123456789L}) {
                Sim.Uitkomst a = Sim.voorspel(spel, seed, 4), b = Sim.voorspel(spel, seed, 4);
                helper.assertTrue(a.equals(b) && a.score() >= 4 && a.score() <= 4 + 5, spel + ": the same seed gives the same game, about 4 points: " + a + " " + b);
                Opname o = opname(spel, seed, 4);
                Sim.Uitkomst terug = Sim.speelAf(spel, seed, o.bytes(), o.stappen());
                helper.assertTrue(terug.equals(a), spel + ": a recording played again gives the same score: " + terug + " vs " + a);
                // half a recording is a game that was left early: it counts as far as it got, never more
                Sim.Uitkomst half = Sim.speelAf(spel, seed, o.bytes(), o.stappen() / 2);
                helper.assertTrue(half.score() <= a.score() && half.stappen() == o.stappen() / 2, spel + ": half a game: " + half);
                // another seed with the same input is another game
                Sim.Uitkomst anders = Sim.speelAf(spel, seed + 1, o.bytes(), o.stappen());
                helper.assertTrue(anders.stappen() <= o.stappen(), spel + ": never more steps than were sent");
            }
            // nothing pressed at all: the game simply ends (Flappy: plop, no points; Pong: three times past you, a lucky tap at most)
            Sim.Uitkomst niks = Sim.speelAf(spel, 5L, new byte[Sim.MAX_STAPPEN / 4], Sim.MAX_STAPPEN);
            helper.assertTrue(niks.score() <= (spel == Spel.FLAPPY ? 0 : 15) && niks.stappen() < Sim.MAX_STAPPEN, spel + ": doing nothing gets you nowhere: " + niks);
            // nonsense input does not break anything
            byte[] rommel = new byte[4000];
            new java.util.Random(9L).nextBytes(rommel);
            Sim.Uitkomst r = Sim.speelAf(spel, 5L, rommel, Integer.MAX_VALUE);
            helper.assertTrue(r.stappen() <= Sim.MAX_STAPPEN && r.score() >= 0, spel + ": random input: " + r);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // a player's game
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void spelerSpeeltEenEchtPotje(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5)), q = speler(helper, new BlockPos(6, 2, 5));
        BlockPos pos = helper.absolutePos(FLAPPY);
        KastBlockEntity be = kast(helper, FLAPPY);
        Guhkade.open(p, pos);
        long seed = Guhkade.seedVan(p);
        helper.assertTrue(be.modus() == KastBlockEntity.SPELER && p.getUUID().equals(be.bezet()), "p stands at the buttons");
        Guhkade.open(q, pos);
        helper.assertTrue(Guhkade.seedVan(q) == 0L && p.getUUID().equals(be.bezet()), "q has to wait");
        Opname spel = opname(Spel.FLAPPY, seed, 3);
        helper.assertTrue(Guhkade.klaar(p, pos, seed, spel.stappen(), spel.bytes()) == null, "a game that was never started does not count");
        helper.assertTrue(!Guhkade.start(p, pos, seed + 1), "another seed than the server gave is refused");
        helper.assertTrue(Guhkade.start(p, pos, seed), "the real one starts");
        Guhkade.Uitslag u = Guhkade.klaar(p, pos, seed, spel.stappen(), spel.bytes());
        helper.assertTrue(u != null && u.score() == 3 && u.record(), "the server counted 3 pillars itself: " + u);
        helper.assertTrue(be.scoreVan(p.getUUID()) == 3 && be.plaatsVan(p.getUUID()) == 3 && be.top().get(2).naam().getString().equals(p.getName().getString()),
                "third on the cabinet, under two house names: " + be.top());
        helper.assertTrue(Guhkade.best(p, Spel.FLAPPY) == 3 && Guhkade.potjes(p, Spel.FLAPPY) == 1 && Guhkade.best(q, Spel.FLAPPY) == 0, "p's own numbers, not q's");
        // the same game cannot be handed in twice, and a seed is good for one game
        helper.assertTrue(Guhkade.klaar(p, pos, seed, spel.stappen(), spel.bytes()) == null && Guhkade.seedVan(p) != seed, "once only");
        // more steps than there was time for (no client can play ten minutes in no time)
        long seed2 = Guhkade.seedVan(p);
        helper.assertTrue(Guhkade.start(p, pos, seed2), "the next game");
        Opname lang = opname(Spel.FLAPPY, seed2, 60);
        helper.assertTrue(lang.stappen() > 400 && Guhkade.klaar(p, pos, seed2, lang.stappen(), lang.bytes()) == null && be.scoreVan(p.getUUID()) == 3,
                "a game of " + lang.stappen() + " steps in zero ticks is refused");
        // from far away
        long seed3 = Guhkade.seedVan(p);
        helper.assertTrue(Guhkade.start(p, pos, seed3), "and the next");
        Opname ver = opname(Spel.FLAPPY, seed3, 2);
        p.snapTo(p.getX() + 40, p.getY(), p.getZ());
        helper.assertTrue(Guhkade.klaar(p, pos, seed3, ver.stappen(), ver.bytes()) == null, "not from 40 blocks away");
        p.snapTo(p.getX() - 40, p.getY(), p.getZ());
        // p walks away: q may play
        Guhkade.stop(p, pos);
        helper.assertTrue(be.bezet() == null && be.modus() == KastBlockEntity.DEMO, "the cabinet is free again");
        Guhkade.open(q, pos);
        helper.assertTrue(q.getUUID().equals(be.bezet()) && Guhkade.seedVan(q) != 0L, "now q stands at the buttons");
        Guhkade.stop(q, pos);
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void topVijfMetNamenVoorIedereen(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5)), q = speler(helper, new BlockPos(6, 2, 5));
        KastBlockEntity flappy = kast(helper, FLAPPY), pong = kast(helper, PONG);
        Guhkade.Uitslag u = Guhkade.verwerk(p, flappy, 20);
        helper.assertTrue(u.plaats() == 1 && u.record(), "p is the new number one: " + u);
        helper.assertTrue(Guhkade.verwerk(q, flappy, 10).plaats() == 2, "q second");
        u = Guhkade.verwerk(p, flappy, 15);
        helper.assertTrue(u.plaats() == 0 && !u.record() && flappy.scoreVan(p.getUUID()) == 20, "a worse game changes nothing");
        helper.assertTrue(flappy.top().size() == 5 && flappy.top().get(0).id().equals(p.getUUID()) && flappy.top().get(1).id().equals(q.getUUID())
                && flappy.top().get(2).huis(), "one line each, the best first, then the house names: " + flappy.top());
        helper.assertTrue(Guhkade.verwerk(q, flappy, 0).plaats() == 0 && flappy.scoreVan(q.getUUID()) == 10, "zero points is no line");
        // an equal score: who was there first stays above
        helper.assertTrue(Guhkade.verwerk(q, flappy, 20).plaats() == 2 && flappy.top().get(0).id().equals(p.getUUID()), "a tie: p stays first");
        // more than five: the top shows five, the cabinet remembers more (but not without end)
        for (int i = 0; i < 40; i++) {
            flappy.voegToe(new UUID(77L, i), Component.literal("Guh " + i), 30 + i, true);
        }
        helper.assertTrue(flappy.top().size() == KastBlockEntity.TOP && flappy.top().get(0).score() == 69 && flappy.regels().size() == KastBlockEntity.MAX_REGELS,
                "five on the screen, " + KastBlockEntity.MAX_REGELS + " remembered");
        helper.assertTrue(flappy.schermTag().getListOrEmpty("Regels").size() == KastBlockEntity.TOP
                && KastBlockEntity.leesRegels(flappy.schermTag()).get(0).naam().getString().equals("Guh 39"), "what a client gets: the top 5 with names");
        // the other cabinet has its own list; the players' own bests are per game and per player
        helper.assertTrue(pong.scoreVan(p.getUUID()) == 0 && pong.top().size() == 3, "the Mika-Pong cabinet knows nothing of this");
        Guhkade.verwerk(q, pong, 33);
        helper.assertTrue(Guhkade.best(p, Spel.FLAPPY) == 20 && Guhkade.best(q, Spel.FLAPPY) == 20 && Guhkade.best(p, Spel.PONG) == 0 && Guhkade.best(q, Spel.PONG) == 33
                && Guhkade.potjes(p, Spel.FLAPPY) == 2 && Guhkade.potjes(q, Spel.FLAPPY) == 3, "per player, per game");
        flappy.wis();
        helper.assertTrue(flappy.top().size() == 3 && flappy.top().get(0).huis(), "wiped: the house names are back");
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    // =====================================================================================================================
    // the guhs
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhsWordenBeterTotHunPlafond(GameTestHelper helper) {
        RandomSource r = RandomSource.create(20261005L);
        int hoogste = 0;
        for (Spel spel : Spel.values()) {
            for (GuhVariant v : GuhVariant.values()) {
                for (GuhPersonality a : GuhPersonality.values()) {
                    int plafond = GuhKunde.plafond(spel, v, a);
                    helper.assertTrue(plafond >= 3 && plafond <= 100, spel + " " + v + " " + a + ": a sane ceiling: " + plafond);
                    if (spel == Spel.FLAPPY) {
                        hoogste = Math.max(hoogste, plafond);
                    }
                    for (int keren : new int[] {0, 1, 5, 20, 80, 500, 10_000}) {
                        int doel = GuhKunde.doel(spel, v, a, keren, r);
                        helper.assertTrue(doel >= 1 && doel <= plafond, spel + " " + v + " " + a + " after " + keren + " games: " + doel + " of " + plafond);
                    }
                }
            }
        }
        helper.assertTrue(GuhKunde.plafond(Spel.FLAPPY, GuhVariant.WOLK, GuhPersonality.CUDDLY) > GuhKunde.plafond(Spel.FLAPPY, GuhVariant.PINGUH, GuhPersonality.CUDDLY)
                && GuhKunde.plafond(Spel.PONG, GuhVariant.TECKEL, GuhPersonality.CUDDLY) > GuhKunde.plafond(Spel.PONG, GuhVariant.WOLK, GuhPersonality.CUDDLY)
                && GuhKunde.plafond(Spel.FLAPPY, GuhVariant.NORMAL, GuhPersonality.PLAYFUL) > GuhKunde.plafond(Spel.FLAPPY, GuhVariant.NORMAL, GuhPersonality.LAZY),
                "the ceiling depends on the kind of guh and a little on its character");
        // slowly better: the average of many games grows with the games played, and gets close to the ceiling
        double vorig = 0;
        int plafond = GuhKunde.plafond(Spel.PONG, GuhVariant.NORMAL, GuhPersonality.CUDDLY);
        for (int keren : new int[] {0, 5, 15, 40, 120}) {
            double som = 0;
            for (int i = 0; i < 400; i++) {
                som += GuhKunde.doel(Spel.PONG, GuhVariant.NORMAL, GuhPersonality.CUDDLY, keren, r);
            }
            double gemiddeld = som / 400;
            helper.assertTrue(gemiddeld > vorig, "after " + keren + " games the average is " + gemiddeld + " (was " + vorig + ")");
            vorig = gemiddeld;
        }
        helper.assertTrue(GuhKunde.kunde(0) < 0.25 && GuhKunde.kunde(18) > 0.6 && GuhKunde.kunde(18) < 0.8 && GuhKunde.kunde(200) > 0.99 && vorig > plafond * 0.8,
                "a beginner at a fifth, the ceiling only after a long time");
        // tough but reachable: the same games can be played to more than the best guh's ceiling
        boolean haalbaar = false;
        for (long seed = 1; seed <= 6 && !haalbaar; seed++) {
            haalbaar = Sim.voorspel(Spel.FLAPPY, seed, hoogste + 1).score() > hoogste;
        }
        helper.assertTrue(haalbaar, "Flappy Guh can be played past the highest ceiling (" + hoogste + ")");
        helper.assertTrue(Sim.voorspel(Spel.PONG, 3L, 90).score() >= 90, "Mika-Pong too");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 1200)
    public static void guhLooptNaarDeKastEnSpeelt(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(13, 2, 13));
        GuhEntity guh = guh(helper, p, new BlockPos(7, 2, 9), "Vadsje");
        UUID id = guh.getUUID();
        KastGoal.TEST_ALTIJD.add(id);
        KastBlockEntity flappy = kast(helper, FLAPPY), pong = kast(helper, PONG);
        boolean[] gezien = new boolean[1];
        helper.onEachTick(() -> {
            for (KastBlockEntity be : new KastBlockEntity[] {flappy, pong}) {
                if (be.modus() == KastBlockEntity.GUH && id.equals(be.bezet())) {
                    // while it plays: the flag (the bobbing on the client), the claim, the screen in guh mode, and it stands in front of the screen
                    helper.assertTrue(GuhHooks.heeft(guh, PxVlaggen.SPEELT_KAST) && GuhkadeSlice.NS.equals(GuhKiezer.geclaimd(guh)), "flag and claim while playing");
                    helper.assertTrue(guh.position().distanceTo(KastGoal.staanplek(be)) < 1.2, "it stands in front of the screen");
                    helper.assertTrue(be.aanZet().getString().equals("Vadsje") && be.scherm(helper.getLevel().getGameTime()) != null, "its name and its game on the screen");
                    gezien[0] = true;
                }
            }
        });
        helper.succeedWhen(() -> {
            KastBlockEntity be = flappy.scoreVan(id) > 0 ? flappy : pong.scoreVan(id) > 0 ? pong : null;
            helper.assertTrue(be != null, "no score of the guh yet");
            helper.assertTrue(gezien[0], "it was seen playing");
            KastBlockEntity.Regel regel = be.regels().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
            helper.assertTrue(regel.guh() && regel.naam().getString().equals("Vadsje") && regel.score() >= 1, "its line: " + regel);
            helper.assertTrue(!GuhHooks.heeft(guh, PxVlaggen.SPEELT_KAST) && GuhKiezer.geclaimd(guh).isEmpty(), "flag and claim are gone afterwards");
            helper.assertTrue(GuhKunde.keren(guh, be.spel()) == 1, "one game played");
            KastGoal.TEST_ALTIJD.remove(id);
            guh.discard();
            PxTest.klaar(helper, p);
        });
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void verslagenGuhKijktSipEnOefentMeer(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 6)), q = speler(helper, new BlockPos(6, 2, 6));
        GuhEntity guh = guh(helper, p, new BlockPos(8, 2, 8), "Njegje");
        guh.getPersistentData().putLong(GuhKunde.RUST, Long.MAX_VALUE / 2);      // (it does not walk off to play by itself in this test)
        KastBlockEntity be = kast(helper, FLAPPY);
        UUID weg = new UUID(5L, 5L);
        be.voegToe(guh.getUUID(), guh.getName(), 12, true);
        be.voegToe(weg, Component.literal("Verweg"), 5, true);
        helper.runAfterDelay(10, () -> {     // (the guh stands on the floor by now)
            Guhkade.Uitslag u = Guhkade.verwerk(p, be, 3);
            helper.assertTrue(u.verslagen().isEmpty() && Guhkade.verslagen(p) == 0 && GuhKunde.oefen(guh) == 0, "3 points beats no guh");
            u = Guhkade.verwerk(p, be, 13);
            helper.assertTrue(u.verslagen().size() == 2 && Guhkade.verslagen(p) == 2, "13 points beats both guhs (12 and 5): " + u);
            helper.assertTrue(guh.emotes.current() == Emote.VERDRIETJE, "the guh nearby looks sad: " + guh.emotes.current());
            helper.assertTrue(GuhKunde.oefen(guh) == GuhKunde.OEFEN_NA_VERLIES && !be.isGeklopt(guh.getUUID()), "and wants to practise");
            helper.assertTrue(be.isGeklopt(weg), "the guh that is not around finds out later");
            // not again for the same guh while it has not come back
            u = Guhkade.verwerk(p, be, 14);
            helper.assertTrue(u.verslagen().isEmpty() && Guhkade.verslagen(p) == 2, "beating your own score beats no guh again");
            // another player has not beaten them yet: it counts for them, on their own counter
            u = Guhkade.verwerk(q, be, 6);
            helper.assertTrue(u.verslagen().size() == 1 && Guhkade.verslagen(q) == 1 && Guhkade.verslagen(p) == 2, "q beats the far guh only: " + u);
            // practice games count double and use up the wish to practise
            GuhKunde.gespeeld(guh, Spel.FLAPPY);
            helper.assertTrue(GuhKunde.keren(guh, Spel.FLAPPY) == 2 && GuhKunde.oefen(guh) == GuhKunde.OEFEN_NA_VERLIES - 1, "a practice game counts double");
            guh.getPersistentData().remove(GuhKunde.OEFEN);
            GuhKunde.gespeeld(guh, Spel.FLAPPY);
            helper.assertTrue(GuhKunde.keren(guh, Spel.FLAPPY) == 3, "a normal game counts once");
            // the guh practised and is back above the player: beating it again counts again
            be.voegToe(guh.getUUID(), guh.getName(), 30, true);
            helper.assertTrue(be.plaatsVan(guh.getUUID()) == 1 && !be.isGeklopt(guh.getUUID()), "the guh is number one");
            u = Guhkade.verwerk(p, be, 31);
            helper.assertTrue(u.verslagen().size() == 1 && Guhkade.verslagen(p) == 3 && u.plaats() == 1, "and beaten again: " + u);
            // the sad look never hurts or costs the guh anything
            helper.assertTrue(guh.isAlive() && guh.getHealth() == guh.getMaxHealth() && guh.isTame(), "only a game, njeg");
            guh.discard();
            PxTest.klaar(helper, p, q);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the shop and the Guhdex
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void winkelEersteKastDuurderDanDeVolgende(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        Winkel.Aanbod flappy = Winkel.van(GuhkadeSlice.aanbod(Spel.FLAPPY)), pong = Winkel.van(GuhkadeSlice.aanbod(Spel.PONG));
        helper.assertTrue(flappy != null && pong != null && flappy.groep().equals("guhkade") && pong.groep().equals("guhkade"), "both cabinets are in the shop");
        Muntjes.zet(p, 1000);
        helper.assertTrue(Winkel.prijs(p, flappy) == 250 && Winkel.prijs(p, pong) == 250, "the first cabinet, whichever, costs 250");
        helper.assertTrue(Winkel.koop(p, flappy.id()) == Winkel.Uitkomst.OK && Muntjes.saldo(p) == 750 && tel(p, GuhkadeSlice.KAST_FLAPPY_ITEM.get()) == 1,
                "bought: a real cabinet");
        helper.assertTrue(Winkel.prijs(p, flappy) == 150 && Winkel.prijs(p, pong) == 150, "every next one costs 150, also the other game");
        helper.assertTrue(Winkel.koop(p, pong.id()) == Winkel.Uitkomst.OK && Muntjes.saldo(p) == 600 && tel(p, GuhkadeSlice.KAST_PONG_ITEM.get()) == 1, "the second");
        helper.assertTrue(Winkel.koop(p, flappy.id()) == Winkel.Uitkomst.OK && Muntjes.saldo(p) == 450 && tel(p, GuhkadeSlice.KAST_FLAPPY_ITEM.get()) == 2,
                "and as many as you like");
        Muntjes.zet(q, 200);
        helper.assertTrue(Winkel.prijs(q, pong) == 250 && Winkel.koop(q, pong.id()) == Winkel.Uitkomst.TE_DUUR && Muntjes.saldo(q) == 200,
                "another player still pays the first price (and cannot yet)");
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void gidsLaatRecordsZien(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 6));
        helper.assertTrue(GidsBlad.secties().stream().anyMatch(s -> s.id().equals(GuhkadeSlice.NS) && s.volgorde() == 50 && !s.zonderToegang()),
                "the Guhkade section is registered at place 50");
        String leeg = GidsBlad.stand(p).toString();
        helper.assertTrue(leeg.contains("gui.guhs.guhkade.gids.kop") && leeg.contains("gui.guhs.guhkade.gids.nog_niet"), "before the first game: " + leeg);
        Guhkade.verwerk(p, kast(helper, PONG), 27);
        String vol = GidsBlad.stand(p).toString();
        helper.assertTrue(vol.contains("gui.guhs.guhkade.gids.best.waarde") && vol.contains("27") && vol.contains("gui.guhs.guhkade.gids.verslagen"),
                "after a game: the record and the guhs beaten: " + vol);
        PxTest.klaar(helper, p);
        helper.succeed();
    }
}
