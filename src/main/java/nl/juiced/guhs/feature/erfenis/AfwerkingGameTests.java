package nl.juiced.guhs.feature.erfenis;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import nl.juiced.guhs.feature.erfenis.kaart.BestelData;
import nl.juiced.guhs.feature.erfenis.kaart.BestelGoed;
import nl.juiced.guhs.feature.erfenis.kaart.Bestelling;
import nl.juiced.guhs.feature.erfenis.kaart.Bestellingen;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.9 afwerking: the makelaarstour (tutorial to Hoeve 2) step by step, the makelaar's postcards and the Kaartjesbord, the
 * secret theetaartje recipe, Oma Breiguh's orders, and Opa's kelder under a stamped Hoeve.
 */
public class AfwerkingGameTests {
    private static final String BATCH = "erfenis_afwerking";
    private static final long T0 = 1_000_000_000L;

    private static ServerPlayer speler(GameTestHelper helper, String naam) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), naam));
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
        return p;
    }

    /** The tour: every step only on its own moment, in order; a later Hoeve level skips ahead; the goodbye gives the deco. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void makelaarstour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ErfenisData data = ErfenisData.get(level.getServer());
        ServerPlayer p = speler(helper, "Tourguh");
        Base b = data.vanSpeler(p.getUUID(), "Tourguh", T0);
        try {
            helper.assertTrue(b.tutorial == 0, "the tour has not started");
            Makelaarstour.opMoment(p, "hoeve_level:2");
            helper.assertTrue(b.tutorial == 0, "Hoeve 2 before arriving does not end a tour that never began");
            Makelaarstour.opMoment(p, "plukje_opgeruimd:1");
            helper.assertTrue(b.tutorial == 0, "nothing before arriving");
            Makelaarstour.opMoment(p, "aangekomen");
            helper.assertTrue(b.tutorial == 1, "arrived: step 1 (clear a plukje)");
            Makelaarstour.opMoment(p, "aangekomen");
            Makelaarstour.opMoment(p, "gebouw_gebouwd:kippenhok");
            Makelaarstour.opMoment(p, "bewoners:1");
            helper.assertTrue(b.tutorial == 1, "later moments out of order don't skip steps");
            Makelaarstour.opMoment(p, "plukje_opgeruimd:5");
            helper.assertTrue(b.tutorial == 2, "plukje cleared: step 2 (Kippenhok)");
            Makelaarstour.opMoment(p, "gebouw_gebouwd:bakkerij");
            helper.assertTrue(b.tutorial == 2, "another building is not the Kippenhok");
            Makelaarstour.opMoment(p, "gebouw_gebouwd:kippenhok");
            helper.assertTrue(b.tutorial == 3, "Kippenhok: step 3 (first resident)");
            Makelaarstour.opMoment(p, "bewoners:1");
            helper.assertTrue(b.tutorial == 4, "resident: step 4 (first order)");
            Makelaarstour.opMoment(p, "bestelling_geleverd:1");
            helper.assertTrue(b.tutorial == 5, "order: step 5 (Hoeve 2)");
            helper.assertTrue(!b.decoOpslag.containsKey("borstbeeld_makelaar"), "no goodbye present yet");
            Makelaarstour.opMoment(p, "hoeve_level:2");
            helper.assertTrue(b.tutorial == Makelaarstour.KLAAR, "Hoeve 2: the tour is done");
            helper.assertTrue(b.decoOpslag.getOrDefault("borstbeeld_makelaar", 0) == 1 && b.decoOpslag.getOrDefault("brievenbus", 0) == 1,
                    "his bust and the letterbox are in the decoschuurtje");
            Makelaarstour.opMoment(p, "hoeve_level:3");
            helper.assertTrue(b.decoOpslag.getOrDefault("borstbeeld_makelaar", 0) == 1, "the goodbye present comes once");

            // skipping ahead: a second village goes from step 1 straight to the end with Hoeve 2
            ServerPlayer q = speler(helper, "Snelguh");
            Base c = data.vanSpeler(q.getUUID(), "Snelguh", T0);
            try {
                Makelaarstour.opMoment(q, "aangekomen");
                Afwerking.opMoment(q, "hoeve_level:3");
                helper.assertTrue(c.ansichtkaarten() == 0, "no postcards while he is still here");
                Makelaarstour.opMoment(q, "hoeve_level:4");
                helper.assertTrue(c.tutorial == Makelaarstour.KLAAR, "a later Hoeve level ends the tour at once");
            } finally {
                data.verwijder(q.getUUID());
            }

            // his postcards from the holiday: one per Hoeve level 3..7, each once; the board grows with them
            Afwerking.opMoment(p, "hoeve_level:3");
            helper.assertTrue(b.vondsten.contains("ansichtkaart_4") && b.ansichtkaarten() == 1, "the first postcard at Hoeve 3");
            helper.assertTrue(b.decoOpslag.getOrDefault("kaartjesbord", 0) == 1, "the Kaartjesbord in the decoschuurtje");
            helper.assertTrue(p.getInventory().contains(s -> s.is(Items.PAPER)), "the card itself in your pockets");
            Afwerking.opMoment(p, "hoeve_level:3");
            helper.assertTrue(b.ansichtkaarten() == 1, "the same card never twice");
            int knusZonder = b.knusheid();
            b.decoOpslag.remove("kaartjesbord");
            b.deco.add(new Base.Deco("kaartjesbord", 60, 60, 0));
            Afwerking.opMoment(p, "hoeve_level:7");
            helper.assertTrue(b.ansichtkaarten() == 5, "five cards by Hoeve 7 (" + b.ansichtkaarten() + ")");
            helper.assertTrue(!b.decoOpslag.containsKey("kaartjesbord"), "no second board while one stands in the village");
            helper.assertTrue(b.knusheid() - knusZonder == 2 * 4, "the placed board: 2 Knusheid per extra card (" + (b.knusheid() - knusZonder) + ")");
            Afwerking.opMoment(p, "hoeve_level:10");
            helper.assertTrue(b.ansichtkaarten() == 5, "only five come by post");
        } finally {
            data.verwijder(p.getUUID());
        }
        helper.succeed();
    }

    /** Sherlock's rewards: the theetaartje recipe stays secret until zaak 2; the testament is a book; flags are mirrored. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void sherlockBeloningen(GameTestHelper helper) {
        Base b = Base.nieuw(UUID.randomUUID(), "Bakguh", 0, 0, T0);
        b.allesOpen();
        b.hoeve().level = 10;
        b.guhlden = 1_000_000;
        Gebouw bak = b.nieuwGebouw(GebouwType.BAKKERIJ, 1, 53, 41, 0, T0);
        List<Recept> recepten = Productie.recepten(GebouwType.BAKKERIJ);
        int thee = -1;
        for (int i = 0; i < recepten.size(); i++) {
            thee = recepten.get(i).id().equals("theetaartje") ? i : thee;
        }
        helper.assertTrue(thee >= 0, "the Bakkerij knows Opa's theetaartjes");
        helper.assertTrue(recepten.get(thee).uit() == Goed.KOEK && recepten.get(thee).uitN() == 2, "two koek per batch");
        Base.Fout f = b.kies(bak.id, thee);
        helper.assertTrue(f != null && "erfenis.fout.recept_geheim".equals(f.key()), "secret before the reward");
        b.beloningen.add("recept_theetaartje");
        helper.assertTrue(b.kies(bak.id, thee) == null && Productie.recept(bak).id().equals("theetaartje"), "chosen after the reward");
        Base terug = Base.load(b.save());
        helper.assertTrue(terug.beloningen.contains("recept_theetaartje"), "the reward flags are saved");

        ServerLevel level = helper.getLevel();
        ErfenisData data = ErfenisData.get(level.getServer());
        ServerPlayer p = speler(helper, "Speurguh");
        Base eigen = data.vanSpeler(p.getUUID(), "Speurguh", T0);
        try {
            Afwerking.beloning(p, "testament");
            Afwerking.beloning(p, "opas_kelder");
            helper.assertTrue(eigen.beloningen.containsAll(List.of("testament", "opas_kelder")), "mirrored into the village");
            helper.assertTrue(eigen.moetStempelen, "nobody there: the Hoeve (with its kelder) is stamped on the next visit");
            int boeken = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                boeken += p.getInventory().getItem(i).is(Items.WRITTEN_BOOK) ? 1 : 0;
            }
            helper.assertTrue(boeken == 2, "the testament and Opa's diary as books (" + boeken + ")");
            Afwerking.beloning(p, "testament");
            helper.assertTrue(eigen.beloningen.stream().filter("testament"::equals).count() == 1, "a flag once");
        } finally {
            data.verwijder(p.getUUID());
        }
        helper.succeed();
    }

    /** Oma Breiguh (zaak 4) rides along with ordinary carts, never with the star order, and pays half as much again. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void omaBestelt(GameTestHelper helper) {
        int oma = 0, gewoon = 0;
        for (long seed = 1; seed <= 40; seed++) {
            for (boolean metOma : new boolean[] {false, true}) {
                BestelData.Speler s = new BestelData.Speler();
                Bestellingen.bijwerken(s, omgeving(metOma), seed, new UUID(9, seed), T0, false);
                for (Bestelling k : s.karren) {
                    helper.assertTrue(metOma || !k.oma, "no Oma without the reward");
                    if (k.oma) {
                        oma++;
                    } else if (metOma) {
                        gewoon++;
                    }
                }
                helper.assertTrue(s.ster == null || !s.ster.oma, "never the star order");
                Bestelling terug = s.karren.isEmpty() ? null : Bestelling.load(s.karren.get(0).save());
                helper.assertTrue(terug == null || terug.oma == s.karren.get(0).oma, "saved");
            }
        }
        helper.assertTrue(oma > 0 && gewoon > oma, "Oma comes often, not always (" + oma + " of " + (oma + gewoon) + ")");
        helper.succeed();
    }

    private static Bestellingen.Omgeving omgeving(boolean oma) {
        List<Bestellingen.Buur> buren = List.of(new Bestellingen.Buur(new UUID(7, 1), "Knabbeldorp", 0xFFF29BB8, 3),
                new Bestellingen.Buur(new UUID(7, 2), "Kaasdijk", 0xFF9BD8F2, 4), new Bestellingen.Buur(new UUID(7, 3), "Vadsveen", 0xFFB8F29B, 5));
        return new Bestellingen.Omgeving() {
            @Override
            public List<Bestellingen.Buur> buren() {
                return buren;
            }

            @Override
            public int hoeve() {
                return 4;
            }

            @Override
            public int marktplein() {
                return 4;
            }

            @Override
            public boolean kanMaken(BestelGoed g) {
                return true;
            }

            @Override
            public boolean oma() {
                return oma;
            }
        };
    }

    /** The court case's reward: the Detectivebureau is stamped in its end look (the L10 shape, much taller) at level 2. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void bureauEindlook(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Base b = Base.nieuw(UUID.randomUUID(), "Bureauguh", 0, 0, T0);
        b.allesOpen();
        b.hoeve().level = 10;
        Gebouw g = b.nieuwGebouw(GebouwType.DETECTIVEBUREAU, 2, 1, 1, 0, T0);
        BlockPos o = helper.absolutePos(new BlockPos(0, 0, 0)).offset(-1, 0, -1);
        int gewoon = hoogste(level, o, g, b);
        b.beloningen.add("bureau_eindlook");
        int eind = hoogste(level, o, g, b);
        helper.assertTrue(eind > gewoon, "the end look is taller (" + gewoon + " -> " + eind + ")");
        helper.succeed();
    }

    private static int hoogste(ServerLevel level, BlockPos o, Gebouw g, Base b) {
        Stempel.gebouw(level, o, b, g);
        nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen.klaarNu();
        int top = 0;
        for (int x = 0; x < g.b(); x++) {
            for (int z = 0; z < g.d(); z++) {
                for (int y = 1; y < 20; y++) {
                    if (!level.getBlockState(o.offset(g.x + x, y, g.z + z)).isAir()) {
                        top = Math.max(top, y);
                    }
                }
            }
        }
        return top;
    }

    /** Opa's kelder under an 11x11 Hoeve: a trapdoor in the floor, a ladder down, the diary on the lectern. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opasKelder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos hoek = helper.absolutePos(new BlockPos(0, 7, 0));
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                for (int y = -6; y <= 3; y++) {
                    level.setBlock(hoek.offset(x, y, z), y < 0 ? Blocks.DIRT.defaultBlockState()
                            : y == 0 ? (x > 1 && x < 9 && z > 1 && z < 7 ? Blocks.OAK_PLANKS : Blocks.GRASS_BLOCK).defaultBlockState()
                            : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        OpasKelder.stempel(level, hoek, 11, 11);
        BlockPos luik = null;
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                if (level.getBlockState(hoek.offset(x, 0, z)).getBlock() instanceof TrapDoorBlock) {
                    luik = hoek.offset(x, 0, z);
                }
            }
        }
        helper.assertTrue(luik != null, "a trapdoor in the floor");
        int lz = luik.getZ() - hoek.getZ();
        helper.assertTrue(lz <= 6, "the trapdoor sits in the Hoeve's floor (not the yard) when there is room");
        for (int y = 1; y <= 4; y++) {
            BlockPos l = luik.below(y);
            helper.assertTrue(level.getBlockState(l).is(Blocks.LADDER) && level.getBlockState(l).canSurvive(level, l), "a ladder down at -" + y);
            helper.assertTrue(level.getBlockState(l).getValue(LadderBlock.FACING) != null, "ladder facing");
        }
        helper.assertTrue(level.getBlockState(hoek.offset(3, -4, 4)).isAir() && level.getBlockState(hoek.offset(3, -3, 4)).isAir()
                && level.getBlockState(hoek.offset(5, -2, 5)).is(Blocks.LANTERN), "room to stand in the cellar, the lantern above the rug");
        helper.assertTrue(level.getBlockEntity(hoek.offset(5, -4, 2)) instanceof LecternBlockEntity le && !le.getBook().isEmpty(), "Opa's diary on the lectern");
        OpasKelder.stempel(level, hoek, 11, 11);
        helper.assertTrue(level.getBlockState(luik).getBlock() instanceof TrapDoorBlock, "stamping twice keeps it the same");
        boolean[] gedaan = {false};
        nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen.daarna(level, () -> gedaan[0] = true);
        nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen.klaarNu();
        helper.assertTrue(gedaan[0], "a job behind the stamp queue runs");
        helper.succeed();
    }

    /** 1.9.3: opa's 50 planks spent on other buildings first was a softlock (only the Houtzagerij makes planks). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void houtzagerijZonderPlanken(GameTestHelper helper) {
        Prijzen.Prijs eerste = Prijzen.gebouw(GebouwType.HOUTZAGERIJ, 1);
        helper.assertTrue(!eerste.goederen().containsKey(Goed.PLANK), "a new Houtzagerij asks no planks");
        helper.assertTrue(eerste.guhlden() > 0 && eerste.guhlden() <= Base.START_GUHLDEN, "it still costs a few Guhlden");
        helper.assertTrue(Prijzen.gebouw(GebouwType.HOUTZAGERIJ, 2).goederen().getOrDefault(Goed.PLANK, 0) > 0,
                "upgrades still ask planks (by then it makes them)");
        ErfenisData data = ErfenisData.get(helper.getLevel().getServer());
        ServerPlayer p = speler(helper, "Plankloosguh");
        Base b = data.vanSpeler(p.getUUID(), "Plankloosguh", T0);
        try {
            b.verander(Goed.PLANK, -b.goed(Goed.PLANK));
            helper.assertTrue(b.heeft(Goed.PLANK) == 0, "all planks spent");
            helper.assertTrue(b.kanBetalen(eerste) == null, "without planks the Houtzagerij can still be paid");
        } finally {
            data.verwijder(p.getUUID());
        }
        helper.succeed();
    }

    /** 1.9.3: the tour's Guhrrit was keyed by his spot (in front of you), so every step put another Guhrrit in the village. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void eenTourGuhrrit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, "Tourguh2");
        String sleutel = Makelaarstour.sleutel(p);
        var a = nl.juiced.guhs.feature.erfenis.sherlock.Guhrrit.verschijn(level, helper.absolutePos(new BlockPos(1, 1, 1)), 0f, false, sleutel);
        var c = nl.juiced.guhs.feature.erfenis.sherlock.Guhrrit.verschijn(level, helper.absolutePos(new BlockPos(5, 1, 6)), 90f, false, sleutel);
        var d = nl.juiced.guhs.feature.erfenis.sherlock.Guhrrit.verschijn(level, helper.absolutePos(new BlockPos(2, 1, 3)), 0f, true, sleutel);
        try {
            helper.assertTrue(a != null && a == c && c == d, "the next tour steps reuse the same Guhrrit");
            helper.assertTrue(d.getKind() == nl.juiced.guhs.entity.GuhNpcEntity.Kind.GUHRRIT_VAKANTIE, "the goodbye still puts on his holiday clothes");
            int n = level.getEntitiesOfClass(nl.juiced.guhs.entity.GuhNpcEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12)).size();
            helper.assertTrue(n == 1, "one Guhrrit in the village, not " + n);
        } finally {
            nl.juiced.guhs.feature.erfenis.sherlock.Guhrrit.weg(a);
        }
        helper.succeed();
    }
}
