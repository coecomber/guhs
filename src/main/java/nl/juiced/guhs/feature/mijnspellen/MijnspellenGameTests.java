package nl.juiced.guhs.feature.mijnspellen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesFeature;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesGame;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Baan;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Bak;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Erts;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Kant;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Punt;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Rit;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Wissel;
import nl.juiced.guhs.feature.mijnspellen.kegelgrot.Bowling;
import nl.juiced.guhs.feature.mijnspellen.kegelgrot.KegelBaan;
import nl.juiced.guhs.feature.mijnspellen.kegelgrot.KegelGame;
import nl.juiced.guhs.feature.mijnspellen.kegelgrot.KegelgrotFeature;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * The cave minigames of 1.7.0 (minigames2): the switch routing of the three rail networks (pure), a cart taking the
 * switch the way its lever stands when it gets there, the bowling score sheet (strikes, spares, the 10th frame), the lane
 * physics (gutter, bumpers, obstacles, strikes possible), the coins, the shops and clothing sources, the coin tag, the
 * protection, and a real shift in the Wisselgrot and a real game in the Kegelgrot (their hall templates).
 */
public class MijnspellenGameTests {
    private static final String BATCH = "mijnspellen";

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer p) {
        KarretjesGame.leave(p);
        KegelGame.leave(p);
        Minigames.forget(p);
        helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    // --- Karretjes-wissels: the networks ------------------------------------------------------------------------------------

    /** Every bin of every network can be reached (and only by its own settings); no two branches share a cell; the levers are distinct. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void karretjesWisselsSturen(GameTestHelper helper) {
        int[] wissels = {2, 3, 5}, bakken = {3, 4, 6};
        for (Niveau n : Niveau.values()) {
            Baan baan = KarretjesNet.baan(n);
            helper.assertTrue(baan.niveau() == n && baan.wissels().size() == wissels[n.ordinal()] && baan.bakken().size() == bakken[n.ordinal()],
                    n + ": " + baan.wissels().size() + " switches, " + baan.bakken().size() + " bins");
            helper.assertTrue(baan.ertsen().size() == baan.bakken().size(), n + ": one bin per ore");
            for (Bak b : baan.bakken()) {
                boolean[] s = KarretjesNet.standenNaar(baan, b);
                helper.assertTrue(KarretjesNet.route(baan, s).nr() == b.nr(), n + ": the settings for bin " + b.nr() + " lead there");
            }
            // every combination of levers ends in exactly one bin, and every bin is the end of some combination
            Set<Integer> bereikt = new java.util.HashSet<>();
            for (int mask = 0; mask < 1 << baan.wissels().size(); mask++) {
                boolean[] s = new boolean[baan.wissels().size()];
                for (int i = 0; i < s.length; i++) {
                    s[i] = (mask >> i & 1) == 1;
                }
                bereikt.add(KarretjesNet.route(baan, s).nr());
            }
            helper.assertTrue(bereikt.size() == baan.bakken().size(), n + ": every bin reachable");
            // the branches don't cross: every cell belongs to one stretch (switch cells to their incoming and outgoing ones)
            Map<Punt, Integer> gebruik = new HashMap<>();
            List<List<Punt>> stukken = new ArrayList<>();
            stukken.add(KarretjesNet.stam(baan));
            for (Wissel w : baan.wissels()) {
                stukken.add(KarretjesNet.tak(w.p(), w.links()));
                stukken.add(KarretjesNet.tak(w.p(), w.rechts()));
            }
            for (List<Punt> stuk : stukken) {
                for (int i = 1; i < stuk.size() - 1; i++) {
                    gebruik.merge(stuk.get(i), 1, Integer::sum);
                }
            }
            for (var e : gebruik.entrySet()) {
                helper.assertTrue(e.getValue() == 1, n + ": cell " + e.getKey() + " is used by " + e.getValue() + " stretches");
                for (Wissel w : baan.wissels()) {
                    helper.assertTrue(!w.p().equals(e.getKey()), n + ": a stretch runs through switch " + w.nr());
                }
            }
            // the levers stand above their switches, all on different spots; the rails of a switch show back + the open side
            Set<Integer> vs = new java.util.HashSet<>();
            for (Wissel w : baan.wissels()) {
                helper.assertTrue(vs.add(w.p().v()), n + ": two levers on one spot");
                boolean[] links = new boolean[baan.wissels().size()], rechts = new boolean[baan.wissels().size()];
                rechts[w.nr()] = true;
                Kant[] l = KarretjesNet.getoond(baan, w.p(), Set.of(), links), r = KarretjesNet.getoond(baan, w.p(), Set.of(), rechts);
                helper.assertTrue(l[0] == Kant.ACHTER && l[1] == Kant.LINKS && r[1] == Kant.RECHTS, n + ": switch " + w.nr() + " shows its way");
            }
            for (var e : KarretjesNet.rails(baan).entrySet()) {
                helper.assertTrue(e.getValue().size() == 2 || e.getValue().size() == 3, n + ": rail " + e.getKey() + " sides " + e.getValue());
            }
        }
        helper.assertTrue(KarretjesGame.class != null && RailShape.SOUTH_EAST != null, "");
        helper.succeed();
    }

    /** A cart takes the way its lever says when it reaches the switch: flipping before counts, flipping after doesn't. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void karretjesRitNeemtDeWissel(GameTestHelper helper) {
        Baan baan = KarretjesNet.baan(Niveau.MAKKELIJK);
        boolean[] s = new boolean[2];
        Rit rit = new Rit(baan, Erts.VADS);
        Bak uit = null;
        // roll until just before switch 0 (2, 0) with the lever left, then flip it right: the cart turns right
        int stappen = 0;
        while (rit.positie()[0] < 1.5) {
            rit.stap(0.1, s);
            stappen++;
        }
        int wortel = baan.wortel().nr();                                   // (levers are numbered left to right: the root is the middle one)
        s[wortel] = true;
        for (int i = 0; i < 1000 && uit == null; i++) {
            uit = rit.stap(0.1, s);
        }
        helper.assertTrue(uit != null && uit.erts() == Erts.VADS && uit.p().equals(new Punt(8, 3)), "flipped in time: the vads bin " + uit);
        helper.assertTrue(rit.aangekomen() == uit && rit.stap(0.1, s) == null, "it arrives once");
        // past the switch, flipping it back changes nothing
        Rit tweede = new Rit(baan, Erts.KOLEN);
        boolean[] t = new boolean[2];
        t[wortel] = true;
        while (tweede.doel() == baan.wortel()) {
            tweede.stap(0.1, t);
        }
        t[wortel] = false;
        Bak b2 = null;
        for (int i = 0; i < 1000 && b2 == null; i++) {
            b2 = tweede.stap(0.1, t);
        }
        helper.assertTrue(b2 != null && b2.erts() == Erts.VADS, "too late: it already went right " + b2);
        // left at both: coal
        Rit derde = new Rit(baan, Erts.KOLEN);
        Bak b3 = null;
        for (int i = 0; i < 1000 && b3 == null; i++) {
            b3 = derde.stap(0.12, new boolean[] {false, false});
        }
        helper.assertTrue(b3 != null && b3.erts() == Erts.KOLEN, "left, left: the coal bin");
        double[] p = new Rit(baan, Erts.IJZER).positie();
        helper.assertTrue(p[0] == KarretjesNet.START.u() && p[1] == 0, "carts start in the tunnel");
        helper.assertTrue(stappen > 0, "");
        helper.succeed();
    }

    /** Ertsbonnetjes: sorted carts x 0.6 / 1.0 / 1.4, +5 for a record, at most 90 (+5); kegelknopen: score / 4 x 0.7 / 1.0 / 1.45. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void mijnspellenMunten(GameTestHelper helper) {
        helper.assertTrue(KarretjesGame.munten(Niveau.MEDIUM, 35, false) == 35, "a good medium shift: 35");
        helper.assertTrue(KarretjesGame.munten(Niveau.LASTIG, 40, false) == 56, "a good lastig shift: 56");
        helper.assertTrue(KarretjesGame.munten(Niveau.MAKKELIJK, 35, false) == 21, "makkelijk: 0.6");
        helper.assertTrue(KarretjesGame.munten(Niveau.MEDIUM, 20, true) == 25, "+5 for a record");
        helper.assertTrue(KarretjesGame.munten(Niveau.LASTIG, 500, false) == KarretjesGame.MAX_MUNTEN, "at most 90");
        helper.assertTrue(KarretjesGame.munten(Niveau.MEDIUM, 0, false) == 0, "nothing for nothing");
        helper.assertTrue(KegelGame.munten(Niveau.MEDIUM, 140, false) == 35, "a good game on the bendy lane: 35");
        int lastig = KegelGame.munten(Niveau.LASTIG, 140, false);
        helper.assertTrue(lastig >= 50 && lastig <= 52, "the same on the obstacle lane: ~51: " + lastig);
        helper.assertTrue(KegelGame.munten(Niveau.MEDIUM, 140, true) == 40, "+5 for a record");
        helper.assertTrue(KegelGame.munten(Niveau.LASTIG, 300, false) == KegelGame.MAX_MUNTEN, "a perfect game: the cap");
        helper.succeed();
    }

    // --- the Kegelgrot ----------------------------------------------------------------------------------------------------

    private static Bowling blad(int... worpen) {
        Bowling b = new Bowling();
        for (int w : worpen) {
            b.gooi(w);
        }
        return b;
    }

    /** Real bowling scoring: strikes, spares, open frames, the 10th frame's bonus balls, the sheet's symbols, the pins standing. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void kegelenTelblad(GameTestHelper helper) {
        Bowling perfect = blad(10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10);
        helper.assertTrue(perfect.klaar() && perfect.score() == 300 && perfect.strikes() == 12, "a perfect game: 300, " + perfect.symbolen());
        helper.assertTrue(perfect.symbolen().endsWith("X X X"), "the 10th: X X X " + perfect.symbolen());
        Bowling negens = new Bowling();
        for (int i = 0; i < 10; i++) {
            negens.gooi(9);
            negens.gooi(0);
        }
        helper.assertTrue(negens.klaar() && negens.score() == 90 && negens.symbolen().startsWith("9- | 9-"), "all 9-: 90 " + negens.symbolen());
        Bowling spares = new Bowling();
        for (int i = 0; i < 21; i++) {
            spares.gooi(5);
        }
        helper.assertTrue(spares.klaar() && spares.score() == 150 && spares.symbolen().endsWith("5 / 5"), "all 5/ and a 5: 150 " + spares.symbolen());
        Bowling voorbeeld = blad(10, 7, 3, 9, 0, 10, 0, 8, 8, 2, 0, 6, 10, 10, 10, 8, 1);
        helper.assertTrue(voorbeeld.klaar() && voorbeeld.score() == 167, "the classic example sheet: 167, " + voorbeeld.score());
        Integer[] stand = voorbeeld.stand();
        helper.assertTrue(stand[0] == 20 && stand[1] == 39 && stand[2] == 48 && stand[3] == 66 && stand[9] == 167,
                "the running totals: " + java.util.Arrays.toString(stand));
        helper.assertTrue(voorbeeld.symbolen().equals("X | 7/ | 9- | X | -8 | 8/ | -6 | X | X | X 8 1"), "the symbols: " + voorbeeld.symbolen());
        // pins standing, the next rack, a strike waiting for its bonus
        Bowling b = blad(7);
        helper.assertTrue(b.staand() == 3 && !b.nieuweKegels() && b.positie()[1] == 1, "after 7: three stand for the second ball");
        b.gooi(3);
        helper.assertTrue(b.nieuweKegels() && b.positie()[0] == 1 && b.stand()[0] == null, "a spare: a fresh rack, its total waits for the next ball");
        b.gooi(4);
        helper.assertTrue(b.stand()[0] == 14 && b.score() == 14, "spare + 4 = 14");
        helper.assertTrue(blad(10).stand()[0] == null && blad(10).nieuweKegels(), "a strike waits for two balls, and gets a fresh rack");
        Bowling tiende = new Bowling();
        for (int i = 0; i < 18; i++) {
            tiende.gooi(0);
        }
        tiende.gooi(10);
        helper.assertTrue(!tiende.klaar() && tiende.staand() == 10, "a strike in the 10th: a fresh rack and a bonus ball");
        tiende.gooi(4);
        helper.assertTrue(!tiende.klaar() && tiende.staand() == 6, "then 4: six stand for the last ball");
        tiende.gooi(6);
        helper.assertTrue(tiende.klaar() && tiende.score() == 20 && tiende.symbolen().endsWith("X 4 /"), "X 4 / = 20 " + tiende.symbolen());
        Bowling open = new Bowling();
        for (int i = 0; i < 20; i++) {
            open.gooi(i == 18 ? 3 : i == 19 ? 4 : 0);
        }
        helper.assertTrue(open.klaar() && open.score() == 7, "an open 10th frame ends after two balls");
        helper.assertTrue(blad(3).staand() == 7, "");
        blad(3).gooi(9);   // (more than stand: limited, no crash)
        helper.succeed();
    }

    private static int rol(KegelBaan.Soort soort, double v0, float kracht, double hoekGraden, double spin, long seed) {
        KegelBaan baan = new KegelBaan(soort);
        boolean[] alle = new boolean[10];
        java.util.Arrays.fill(alle, true);
        baan.opzetten(alle);
        baan.gooi(v0, KegelBaan.snelheid(kracht), Math.toRadians(hoekGraden), spin, seed, seed * 7);
        int t = 0;
        while (baan.tik() && t < 2000) {
            t++;
        }
        return baan.omgevallen();
    }

    /** The lanes: a gutter ball hits nothing, a ball into the pocket can strike, the bumpers keep it in, the posts bounce it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void kegelenBaanNatuurkunde(GameTestHelper helper) {
        KegelBaan goot = new KegelBaan(KegelBaan.Soort.RECHT);
        boolean[] alle = new boolean[10];
        java.util.Arrays.fill(alle, true);
        goot.opzetten(alle);
        goot.gooi(1.2, KegelBaan.snelheid(0.7f), Math.toRadians(8), 0, 1, 0);
        boolean inGoot = false;
        while (goot.tik()) {
            inGoot |= goot.bal().goot;
        }
        helper.assertTrue(inGoot && goot.omgevallen() == 0, "a gutter ball knocks nothing over");
        int strikes = 0, totaal = 0;
        for (long seed = 0; seed < 12; seed++) {
            int n = rol(KegelBaan.Soort.RECHT, 0.5, 0.6f, -0.5, -0.5, seed);
            totaal += n;
            strikes += n == 10 ? 1 : 0;
        }
        helper.assertTrue(strikes >= 1 && totaal >= 12 * 6, "into the pocket with a little hook: strikes happen (" + strikes + "), average " + totaal / 12.0);
        int recht = 0;
        for (long seed = 0; seed < 8; seed++) {
            recht += rol(KegelBaan.Soort.RECHT, 0, 0.7f, 0, 0, seed);
        }
        helper.assertTrue(recht >= 8 * 5, "straight down the middle knocks most of them: " + recht / 8.0);
        // the bendy lane: the bumpers keep the ball between them, whatever you do
        KegelBaan bocht = new KegelBaan(KegelBaan.Soort.BOCHTIG);
        bocht.opzetten(alle);
        bocht.gooi(-1.2, KegelBaan.snelheid(1f), Math.toRadians(-9), -1, 3, 0);
        double ergst = 0;
        boolean kaatst = false;
        while (bocht.tik()) {
            KegelBaan.Bal b = bocht.bal();
            if (!b.weg) {
                ergst = Math.max(ergst, Math.abs(b.v - KegelBaan.midden(KegelBaan.Soort.BOCHTIG, b.u)));
            }
            kaatst |= (bocht.gebeurd() & 2) != 0;
        }
        helper.assertTrue(kaatst && ergst <= KegelBaan.HALF - KegelBaan.BAL_R + 0.05, "the bumpers bounce it back: " + ergst);
        helper.assertTrue(KegelBaan.midden(KegelBaan.Soort.BOCHTIG, 6.75) > 0.9 && KegelBaan.midden(KegelBaan.Soort.RECHT, 6.75) == 0, "the bend");
        // the obstacle lane: straight at a post, it bounces off
        KegelBaan paal = new KegelBaan(KegelBaan.Soort.HINDERNIS);
        paal.opzetten(alle);
        paal.gooi(KegelBaan.PAAL_V, KegelBaan.snelheid(0.5f), 0, 0, 5, 0);
        boolean geraakt = false;
        while (paal.tik()) {
            geraakt |= (paal.gebeurd() & 2) != 0 && paal.bal().u < KegelBaan.PAAL_U + 0.5;
        }
        helper.assertTrue(geraakt, "the post is in the way");
        helper.assertTrue(KegelBaan.obstakels(KegelBaan.Soort.HINDERNIS, 0).size() == 3 && KegelBaan.obstakels(KegelBaan.Soort.RECHT, 0).isEmpty(),
                "two posts and the Mika-blokje");
        helper.assertTrue(Math.abs(KegelBaan.schuifV(KegelBaan.SCHUIF_TIJD / 4) - KegelBaan.SCHUIF_V) < 0.01, "the Mika-blokje slides");
        // a second ball only meets the pins still standing
        KegelBaan tweede = new KegelBaan(KegelBaan.Soort.RECHT);
        boolean[] drie = new boolean[10];
        drie[6] = drie[9] = drie[5] = true;
        tweede.opzetten(drie);
        tweede.gooi(0, KegelBaan.snelheid(0.7f), 0, 0, 9, 0);
        while (tweede.tik()) {
            // roll
        }
        helper.assertTrue(tweede.omgevallen() <= 3, "only the three standing pins count");
        helper.succeed();
    }

    // --- shops, sources, tags, protection --------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void mijnspellenWinkelsEnBronnen(GameTestHelper helper) {
        GuhNpcEntity piepwiel = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(1, 2, 1));
        piepwiel.setKind(GuhNpcEntity.Kind.WISSELGUH);
        var offers = piepwiel.getOffers();
        Item petje = ModItems.clothingItem(GuhClothes.KARRETJES_CONDUCTEURSPETJE);
        helper.assertTrue(offers.stream().allMatch(o -> o.getCostA().is(KarretjesFeature.ERTSBONNETJE.get())), "Piepwiel wants ertsbonnetjes");
        helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(petje) && o.getCostA().getCount() >= 40 && o.getCostA().getCount() <= 80),
                "the conducteurspetje for 40-80");
        for (var blok : KarretjesFeature.BOUWBLOKKEN) {
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(blok.get()) && o.getResult().getCount() == 4 && o.getCostA().getCount() == 1),
                    "4 " + blok.getId() + " for 1");
        }
        for (Item deco : List.of(KarretjesFeature.ERTSKARRETJE_ITEM.get(), KarretjesFeature.STATIONSKLOKJE_ITEM.get(), KarretjesFeature.SEINLAMPJE_ITEM.get())) {
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(deco) && o.getCostA().getCount() >= 20 && o.getCostA().getCount() <= 64),
                    "deco " + deco + " for 20-64");
        }
        GuhNpcEntity guhde = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(3, 2, 3));
        guhde.setKind(GuhNpcEntity.Kind.KEGELGUH);
        var kegel = guhde.getOffers();
        helper.assertTrue(kegel.stream().allMatch(o -> o.getCostA().is(KegelgrotFeature.KEGELKNOOP.get())), "De Guhde wants kegelknopen");
        for (GuhClothes c : KegelgrotFeature.KLEDING) {
            helper.assertTrue(kegel.stream().anyMatch(o -> o.getResult().is(ModItems.clothingItem(c)) && o.getCostA().getCount() >= 40
                    && o.getCostA().getCount() <= 80), c + " for 40-80");
            helper.assertTrue("kegelen".equals(KledingBronnen.bron(c)) && KledingBronnen.prijs(c) != null, c + ": its source");
        }
        for (var blok : KegelgrotFeature.BOUWBLOKKEN) {
            helper.assertTrue(kegel.stream().anyMatch(o -> o.getResult().is(blok.get()) && o.getResult().getCount() == 4 && o.getCostA().getCount() == 1),
                    "4 " + blok.getId() + " for 1");
        }
        for (Item deco : List.of(KegelgrotFeature.KEGELTJE_ITEM.get(), KegelgrotFeature.WOLBALREK_ITEM.get(), KegelgrotFeature.NEON_KEGELBORD_ITEM.get())) {
            helper.assertTrue(kegel.stream().anyMatch(o -> o.getResult().is(deco) && o.getCostA().getCount() >= 20 && o.getCostA().getCount() <= 64),
                    "deco " + deco + " for 20-64");
        }
        helper.assertTrue("karretjes".equals(KledingBronnen.bron(GuhClothes.KARRETJES_CONDUCTEURSPETJE)), "the cap's source");
        helper.assertTrue(GuhClothes.KARRETJES_CONDUCTEURSPETJE.slot == GuhClothes.Slot.HEAD && GuhClothes.KEGELGROT_BOWLINGSHIRTJE.slot == GuhClothes.Slot.BODY
                && GuhClothes.KEGELGROT_ZWEETBANDJE.slot == GuhClothes.Slot.HEAD, "the slots");
        TagKey<Item> tickets = TagKey.create(Registries.ITEM, Guhs.id("knus/grijptickets"));
        helper.assertTrue(new ItemStack(KarretjesFeature.ERTSBONNETJE.get()).is(tickets) && new ItemStack(KegelgrotFeature.KEGELKNOOP.get()).is(tickets),
                "both coins work in the grijpmachine");
        ItemStack bal = new ItemStack(KegelgrotFeature.WOLBAL.get());
        helper.assertTrue(Features.isLoaned(bal) && !bal.getItem().canFitInsideContainerItems(), "the wool ball is loaned");
        helper.assertTrue(Highscores.game("karretjes_lastig") != null && Highscores.game("kegelen_medium") != null, "on the Highscores page");
        helper.assertTrue(Minigames.class != null, "");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void mijnspellenBescherming(GameTestHelper helper) {
        BlockPos vloer = new BlockPos(2, 1, 2);
        helper.setBlock(vloer, KarretjesFeature.BIELZENHOUT.get());
        BlockPos abs = helper.absolutePos(vloer);
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(4, 3, 4)));
        Mijnspel.TEST_GEBIEDEN.add(box);
        ServerPlayer p = player(helper);
        try {
            helper.assertTrue(Protected.at(helper.getLevel(), abs), "the hall is protected");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), p)).isCanceled(),
                    "survival players can't break it");
            p.setGameMode(GameType.CREATIVE);
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), p)).isCanceled(),
                    "creative may");
            helper.assertTrue(!Mijnspel.inGebouw(helper.getLevel(), abs.offset(30, 0, 0)), "outside it isn't");
        } finally {
            Mijnspel.TEST_GEBIEDEN.remove(box);
            leave(helper, p);
        }
        helper.succeed();
    }

    // --- the buildings: hall + shaft + hut by their jigsaws -------------------------------------------------------------------

    /**
     * Puts a whole building together the way worldgen does (the start pool, aligned on the hall's lift jigsaw): the hut
     * lands straight above the hall, so its Wolkenliftje really reaches the one in the hall; the NPC came along. Cleans up.
     */
    private static void gebouw(GameTestHelper helper, String sid, String spel, GuhNpcEntity.Kind kind) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos start = helper.absolutePos(new BlockPos(2, 16, 2));
        var pool = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL)
                .getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.TEMPLATE_POOL, Guhs.id(sid + "/start")));
        boolean ok = net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement.generateJigsaw(level, pool, Guhs.id(spel + "_lift"), 3, start, false);
        helper.assertTrue(ok, sid + ": the jigsaws fit together");
        BlockPos min = start.offset(-64, -16, -64), max = start.offset(64, 40, 64);
        List<BlockPos> liftjes = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(nl.juiced.guhs.feature.mijnhulp.MijnhulpFeature.WOLKENLIFTJE.get())) {
                liftjes.add(pos.immutable());
            }
        }
        try {
            helper.assertTrue(liftjes.size() == 2, sid + ": two Wolkenliftjes (hall and hut): " + liftjes);
            BlockPos onder = liftjes.get(0).getY() < liftjes.get(1).getY() ? liftjes.get(0) : liftjes.get(1);
            BlockPos boven = onder == liftjes.get(0) ? liftjes.get(1) : liftjes.get(0);
            helper.assertTrue(onder.getX() == boven.getX() && onder.getZ() == boven.getZ() && boven.getY() - onder.getY() > 20,
                    sid + ": the hut's lift is straight above the hall's: " + onder + " " + boven);
            helper.assertTrue(boven.equals(nl.juiced.guhs.feature.mijnhulp.Wolkenliftje.doel(level, onder, true))
                    && onder.equals(nl.juiced.guhs.feature.mijnhulp.Wolkenliftje.doel(level, boven, false)), sid + ": up and down");
            var npcs = level.getEntitiesOfClass(GuhNpcEntity.class, new net.minecraft.world.phys.AABB(min.getCenter(), max.getCenter()), n -> n.getKind() == kind);
            helper.assertTrue(npcs.size() == 1, sid + ": its guh came along");
        } finally {
            level.getEntitiesOfClass(GuhNpcEntity.class, new net.minecraft.world.phys.AABB(min.getCenter(), max.getCenter()), n -> n.getKind() == kind)
                    .forEach(n -> n.discard());
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
        }
    }

    @GuhTest(template = "empty", batch = BATCH + "_gebouw", timeoutTicks = 200)
    public static void mijnspellenGebouwen(GameTestHelper helper) {
        gebouw(helper, "karretjeswissels", "karretjes", GuhNpcEntity.Kind.WISSELGUH);
        gebouw(helper, "kegelgrot", "kegelgrot", GuhNpcEntity.Kind.KEGELGUH);
        helper.succeed();
    }

    // --- the real halls ------------------------------------------------------------------------------------------------------

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(2), n -> n.getKind() == kind);
        helper.assertTrue(npcs.size() == 1, kind + " is in the hall: " + npcs.size());
        return npcs.get(0);
    }

    /** A shift in the Wisselgrot: Piepwiel finds the networks, lays track 1, a lever turns the switch, carts end right and wrong. */
    @GuhTest(template = "karretjeswissels_zaal", timeoutTicks = 700, batch = BATCH)
    public static void karretjesDienstInDeWisselgrot(GameTestHelper helper) {
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.WISSELGUH);
        ServerPlayer p = player(helper);
        p.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        KarretjesGame game = KarretjesGame.of(npc);
        helper.assertTrue(game.start(npc, p, Niveau.MAKKELIJK), "the shift starts");
        helper.assertTrue(KarretjesGame.isPlaying(p) && game.begin(Niveau.MAKKELIJK) != null && game.begin(Niveau.LASTIG) != null,
                "all three networks found");
        Baan baan = KarretjesNet.baan(Niveau.MAKKELIJK);
        Wissel w0 = baan.wissels().stream().filter(w -> w.p().v() == 0).findFirst().orElseThrow();
        BlockPos wissel = game.cel(Niveau.MAKKELIJK, w0.p().u(), w0.p().v(), 1);
        BlockPos hendel = game.hendel(Niveau.MAKKELIJK, w0);
        helper.assertTrue(helper.getLevel().getBlockState(wissel).is(Blocks.RAIL), "the rails are laid");
        helper.assertTrue(helper.getLevel().getBlockState(hendel).is(Blocks.LEVER), "the levers are on the railing");
        for (Bak b : baan.bakken()) {
            BlockState s = helper.getLevel().getBlockState(game.cel(Niveau.MAKKELIJK, b.p().u(), b.p().v(), 1));
            helper.assertTrue(s.is(KarretjesFeature.ERTSBAK.get()) && s.getValue(MijnspelBlokken.SOORT) == b.erts().ordinal(), "the bin of " + b.erts());
        }
        RailShape links = helper.getLevel().getBlockState(wissel).getValue(RailBlock.SHAPE);
        game.testSpelen();
        helper.getLevel().setBlock(hendel, helper.getLevel().getBlockState(hendel).setValue(LeverBlock.POWERED, true), 3);
        helper.runAfterDelay(3, () -> {
            RailShape rechts = helper.getLevel().getBlockState(wissel).getValue(RailBlock.SHAPE);
            helper.assertTrue(rechts != links && game.standen()[w0.nr()], "the lever turned the switch: " + links + " -> " + rechts);
            game.stuur(helper.getLevel(), Erts.VADS);
            helper.assertTrue(game.karren().size() == 1, "a cart comes out of the tunnel");
        });
        helper.runAfterDelay(15, () -> helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                nl.juiced.guhs.feature.mijnspellen.karretjes.ErtsKarretjeEntity.class, helper.getBounds().inflate(2)).size() == 1, "a real cart rolls"));
        helper.runAfterDelay(260, () -> {
            helper.assertTrue(game.goed() == 1 && game.kwijt() == 0 && game.karren().isEmpty(), "the vads cart went right into the vads bin: "
                    + game.goed() + "/" + game.kwijt());
            game.stuur(helper.getLevel(), Erts.KOLEN);                     // (the switch still says right: wrong bin)
        });
        helper.runAfterDelay(520, () -> {
            helper.assertTrue(game.goed() == 1 && game.kwijt() == 1, "the coal cart got lost in the vads bin: " + game.goed() + "/" + game.kwijt());
            leave(helper, p);
            helper.assertTrue(!game.isRunning(), "leaving ends the shift");
            helper.succeed();
        });
    }

    private static final int[] REST = {10, 10, 10, 10, 10, 10, 10, 10, 10, 9, 0};

    /** A game in the Kegelgrot: De Guhde finds the lanes, ten pins stand, a real ball rolls and knocks some down, the sheet pays out. */
    @GuhTest(template = "kegelgrot_zaal", timeoutTicks = 500, batch = BATCH)
    public static void kegelenPotjeInDeKegelgrot(GameTestHelper helper) {
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.KEGELGUH);
        ServerPlayer p = player(helper);
        p.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        KegelGame game = KegelGame.of(npc);
        helper.assertTrue(game.start(npc, p, Niveau.MAKKELIJK), "the game starts");
        helper.assertTrue(KegelGame.isPlaying(p) && game.begin(Niveau.MEDIUM) != null && game.begin(Niveau.LASTIG) != null, "all three lanes found");
        helper.assertTrue(p.getInventory().countItem(KegelgrotFeature.WOLBAL.get()) == 1, "the loaned wool ball");
        helper.assertTrue(game.testKegels(helper.getLevel()).size() == 10, "ten pins stand");
        helper.assertTrue(game.gooiMet(p, 0.7f, 0.0, 0, 0), "bowled straight down the middle");
        helper.assertTrue(game.phase() == KegelGame.Phase.ROLLEN, "it rolls");
        helper.runAfterDelay(140, () -> {
            helper.assertTrue(game.phase() == KegelGame.Phase.VEGEN || game.phase() == KegelGame.Phase.AANLOOP, "the ball is done: " + game.phase());
            helper.assertTrue(game.blad().worpen().size() == 1 && game.blad().worpen().get(0) > 0, "pins fell: " + game.blad().worpen());
            game.testVegenKlaar();
        });
        helper.runAfterDelay(145, () -> {
            int staan = game.blad().staand();
            helper.assertTrue(game.phase() == KegelGame.Phase.AANLOOP, "ready for the next ball");
            helper.assertTrue(game.testKegels(helper.getLevel()).size() == staan, "the fallen pins are swept away: " + game.testKegels(helper.getLevel()).size()
                    + " vs " + staan);
            game.testBlad(REST);                                         // (the rest of the game at once: 9 strikes and 9-)
        });
        helper.runAfterDelay(150, () -> {
            int knopen = p.getInventory().countItem(KegelgrotFeature.KEGELKNOOP.get());
            int score = blad(REST).score();
            helper.assertTrue(score == 267 && knopen == KegelGame.munten(Niveau.MAKKELIJK, score, false), "kegelknopen for 267 on the straight lane: " + knopen);
            helper.assertTrue(KegelGame.best(p, Niveau.MAKKELIJK) == score, "the record");
            helper.assertTrue(p.getInventory().countItem(KegelgrotFeature.WOLBAL.get()) == 0, "the ball went back");
            leave(helper, p);
            helper.succeed();
        });
    }
}
