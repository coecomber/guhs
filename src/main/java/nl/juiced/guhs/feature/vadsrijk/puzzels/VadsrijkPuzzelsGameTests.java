package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-PUZZELS: the data (22 puzzles, at least 5 per zone, every anchor POI exists, every block/item/page exists), every puzzle
 * solvable through the real input (stamped into the test area), failed tries -> tip 1 -> tip 2, the reset after solving,
 * the beam, the push rules, the riddle (private, counted), the star stones at night only, rewards once per player (also
 * two players at once), and the Verkenningsboek counting. Each test stamps on its own height (tests of one batch run side by
 * side). Run: {@code runGameTestServer -Pgt=VadsrijkPuzzels}.
 */
public class VadsrijkPuzzelsGameTests {
    private static final String BATCH = "vadsrijk_puzzels";

    private static void schoon(ServerPlayer p) {
        Spelers.data(p).extra.remove(PuzzelSpeler.SLICE);
    }

    /** Stamps d as a test instance at relative height {@code laag} * 12 + 4 (clearing a 14x14 box first). */
    private static Puzzels.Inst zet(GameTestHelper helper, PuzzelDef d, int laag, ServerPlayer p) {
        ServerLevel l = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 4 + laag * 12, 1));
        for (int x = -1; x < 15; x++) {
            for (int z = -1; z < 15; z++) {
                for (int y = -1; y < 9; y++) {
                    l.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        PuzzelStempel.stempel(l, d, o);
        Puzzels.Inst in = Puzzels.activeer(d, l, o, true);
        Puzzels.herstel(in);
        if (p != null) {
            p.snapTo(o.getX() + d.breed() / 2.0, o.getY() + d.hoogte() + 1, o.getZ() + d.diep() + 0.5);   // (above, not in the way of the beam)
        }
        return in;
    }

    private static void weg(Puzzels.Inst in) {
        Puzzels.verwijder(in);
    }

    private static PuzzelDef eerste(String soort) {
        for (PuzzelDef d : Puzzels.defs()) {
            if (d.soort().equals(soort)) {
                return d;
            }
        }
        throw new IllegalStateException("no puzzle of kind " + soort);
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void data(GameTestHelper helper) {
        if (Puzzels.defs().isEmpty()) {
            Puzzels.laad(helper.getLevel().getServer());
        }
        Map<Integer, Integer> per = new HashMap<>();
        java.util.Set<Integer> paginas = new java.util.HashSet<>();
        for (PuzzelDef d : Puzzels.defs()) {
            per.merge(d.zone(), 1, Integer::sum);
            helper.assertTrue(d.anker().punt() != null, d.id() + ": anchor POI " + d.anker().bron() + "/" + d.anker().plek() + " exists");
            for (PuzzelDef.Element e : d.elementen()) {
                helper.assertTrue(PuzzelRegistry.blok(e.blok()) != null, d.id() + ": block vr_pz_" + e.blok() + " exists");
                helper.assertTrue(e.x() >= 0 && e.z() >= 0 && e.x() < d.breed() && e.z() < d.diep(), d.id() + ": element inside the box");
            }
            for (PuzzelDef.ItemBeloning ib : d.beloning().items()) {
                helper.assertTrue(BuiltInRegistries.ITEM.getValue(Identifier.parse(ib.item())) != Items.AIR, d.id() + ": reward item " + ib.item());
            }
            for (PuzzelDef.Blok b : d.blokken()) {
                String naam = b.blok().contains("[") ? b.blok().substring(0, b.blok().indexOf('[')) : b.blok();
                helper.assertTrue(BuiltInRegistries.BLOCK.getValue(Identifier.parse(naam)) != Blocks.AIR, d.id() + ": block " + naam);
            }
            if (d.beloning().bladzijde() > 0) {
                helper.assertTrue(paginas.add(d.beloning().bladzijde()), d.id() + ": page " + d.beloning().bladzijde() + " given only once");
            }
            helper.assertTrue(d.beloning().munten() > 0, d.id() + ": every puzzle gives some coins");
        }
        helper.assertTrue(Puzzels.defs().size() >= 22, "22 puzzles, got " + Puzzels.defs().size());
        for (int z = 1; z <= 3; z++) {
            helper.assertTrue(per.getOrDefault(z, 0) >= 5, "at least 5 in zone " + z + ", got " + per.get(z));
        }
        helper.assertTrue(paginas.size() == PuzzelRegistry.BLADZIJDES, "all 9 torn pages are somewhere");
        for (int n = 1; n <= PuzzelRegistry.BLADZIJDES; n++) {
            helper.assertTrue(PuzzelRegistry.bladzijde(n) != null, "page item " + n);
        }
        helper.succeed();
    }

    /** Every puzzle of the data, stamped and solved through the same input players use; the reward comes once. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void alleOplosbaar(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        try {
            for (PuzzelDef d : List.copyOf(Puzzels.defs())) {
                Puzzels.Inst in = zet(helper, d, 0, p);
                helper.assertFalse(in.opgelost(), d.id() + ": not solved at the start");
                if (d.soort().equals("licht")) {
                    helper.assertFalse(Puzzels.straal(in).raak(), d.id() + ": the beam misses at the start");
                }
                boolean ok = PuzzelDev.los(in, p);
                helper.assertTrue(ok, d.id() + ": solved through the real input");
                helper.assertTrue(PuzzelSpeler.opgelost(p, d.id()), d.id() + ": the player got it");
                if (!d.deur().isEmpty()) {
                    helper.assertTrue(helper.getLevel().getBlockState(in.wereld(d.deur().get(0))).isAir(), d.id() + ": the door opened");
                }
                weg(in);
            }
            int pages = 0;
            for (int n = 1; n <= 9; n++) {
                pages += PuzzelSpeler.bladzijde(p, n) ? 1 : 0;
            }
            helper.assertTrue(pages == 9, "all pages found by solving everything, got " + pages);
            helper.assertTrue(PuzzelSpeler.ontgrendeld(p, "pz_vleugels_sterrenstof"), "a cosmetic unlock is remembered");
        } finally {
            schoon(p);
        }
        helper.succeed();
    }

    /** Wrong order: back to the start, counted; tip 1 after hintNa, tip 2 after twice; the right order still works. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void foutenEnTips(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        PuzzelDef d = Puzzels.def("z1_kleurplaten");
        helper.assertTrue(d != null, "z1_kleurplaten exists");
        Puzzels.Inst in = zet(helper, d, 1, p);
        int goed = d.oplossing().get(0), fout = d.oplossing().get(1);
        Puzzels.stap(in.level, in.element(goed), p);
        helper.assertTrue(in.voortgang == 1 && Puzzels.aan(in, goed), "the first plate lights up");
        Puzzels.stap(in.level, in.element(d.oplossing().get(2)), p);
        helper.assertTrue(in.voortgang == 0 && !Puzzels.aan(in, goed) && in.fouten(p) == 1, "a wrong plate: everything off, 1 failed try");
        helper.assertTrue(Puzzels.tipVoor(in, p) == null, "no tip after 1");
        for (int i = 1; i < d.hintNa(); i++) {
            Puzzels.stap(in.level, in.element(fout), p);
        }
        helper.assertTrue(d.key("hint1").equals(Puzzels.tipVoor(in, p)), "tip 1 after " + d.hintNa() + " tries");
        for (int i = 0; i < d.hintNa(); i++) {
            Puzzels.stap(in.level, in.element(fout), p);
        }
        helper.assertTrue(d.key("hint2").equals(Puzzels.tipVoor(in, p)), "tip 2 after " + d.hintNa() * 2);
        helper.assertTrue(PuzzelDev.los(in, p) && PuzzelSpeler.opgelost(p, d.id()), "the right order still solves it");
        weg(in);
        schoon(p);
        helper.succeed();
    }

    /** Solved stays solved (doors open) for {@code reset} ticks, then everything goes back; a reward is given only once. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void herstelEnEenmaal(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        PuzzelDef d = Puzzels.def("z1_vuurtorenschuur");
        Puzzels.Inst in = zet(helper, d, 2, p);
        long munten = Spelers.data(p).munten;
        helper.assertTrue(PuzzelDev.los(in, p), "the hidden lever opens it");
        long na = Spelers.data(p).munten;
        helper.assertTrue(na - munten == d.beloning().munten(), "the coins once: " + (na - munten));
        BlockPos deur = in.wereld(d.deur().get(0));
        helper.assertTrue(helper.getLevel().getBlockState(deur).isAir(), "door open");
        Puzzels.klik(in.level, in.element(d.van("hendel").get(0)), p);
        helper.assertTrue(Spelers.data(p).munten == na, "clicking again while solved gives nothing");
        in.opgelostTot = helper.getLevel().getGameTime();
        Puzzels.tick(in);
        helper.assertTrue(!in.opgelost() && helper.getLevel().getBlockState(deur).getBlock() == PuzzelRegistry.blok("geheimdeur"), "closed again after the reset time");
        helper.assertTrue(!Puzzels.aan(in, d.van("hendel").get(0)), "the lever back");
        helper.assertTrue(PuzzelDev.los(in, p) && Spelers.data(p).munten == na, "solving again: no second reward");
        weg(in);
        schoon(p);
        helper.succeed();
    }

    /** Two players near: both get it once. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void samenPuzzelen(GameTestHelper helper) {
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper);
        schoon(a);
        schoon(b);
        PuzzelDef d = Puzzels.def("z1_symboolslot");
        Puzzels.Inst in = zet(helper, d, 3, a);
        b.snapTo(a.getX() + 1, a.getY(), a.getZ());
        helper.assertTrue(PuzzelDev.los(in, a), "solved");
        helper.assertTrue(PuzzelSpeler.opgelost(a, d.id()) && PuzzelSpeler.opgelost(b, d.id()), "both players near got it");
        weg(in);
        schoon(a);
        schoon(b);
        helper.succeed();
    }

    /** Mirrors: a click turns one; the beam follows the stands; a wall stops it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lichtstraal(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        PuzzelDef d = Puzzels.def("z1_lichtstraal");
        Puzzels.Inst in = zet(helper, d, 4, p);
        int sp = d.van("spiegel").get(0);
        int voor = Puzzels.stand(in, sp);
        Puzzels.klik(in.level, in.element(sp), p);
        helper.assertTrue(Puzzels.stand(in, sp) == 1 - voor, "a click turns the mirror");
        helper.assertTrue(Puzzels.spiegel(1, 0) == 0 && Puzzels.spiegel(1, 1) == 2 && Puzzels.spiegel(0, 1) == 3, "the reflection rules");
        PuzzelDev.los(in, p);
        helper.assertTrue(in.opgelost() && Puzzels.aan(in, d.van("lichtdoel").get(0)), "the target lights up");
        weg(in);
        schoon(p);
        helper.succeed();
    }

    /** Push blocks: not into a wall, not out of the box; a target lights under a block; the reset button counts a try. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void schuiven(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        PuzzelDef d = Puzzels.def("z2_ijsschuif");
        Puzzels.Inst in = zet(helper, d, 5, p);
        int blok = d.van("schuifblok").get(0);
        BlockPos start = in.element(blok);
        // north of the first block (2,2) is (2,1) floor, then (2,0) the wall
        helper.assertTrue(Puzzels.duw(in, blok, 0, p), "one step north is free");
        helper.assertFalse(Puzzels.duw(in, blok, 0, p), "not into the wall");
        helper.assertTrue(helper.getLevel().getBlockState(in.element(blok)).getBlock() == PuzzelRegistry.blok("schuifblok"), "the block moved");
        Puzzels.klik(in.level, in.element(d.van("herstelknop").get(0)), p);
        helper.assertTrue(in.element(blok).equals(start) && helper.getLevel().getBlockState(start).getBlock() == PuzzelRegistry.blok("schuifblok"),
                "the reset button puts it back");
        helper.assertTrue(in.fouten(p) == 1, "and counts a try");
        helper.assertTrue(PuzzelDev.los(in, p), "the BFS solution from the generator works");
        for (int k : d.van("doelvloer")) {
            helper.assertTrue(Puzzels.aan(in, k), "every target tile lit");
        }
        weg(in);
        schoon(p);
        helper.succeed();
    }

    /** The riddle: only after clicking the stone, near it; a wrong answer counts and is swallowed; the right one solves. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void raadsel(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        PuzzelDef d = Puzzels.def("z1_fluistersteen");
        Puzzels.Inst in = zet(helper, d, 6, p);
        helper.assertFalse(Puzzels.zeg(p, "vads"), "without clicking the stone, chat is just chat");
        Puzzels.klik(in.level, in.element(d.van("fluistersteen").get(0)), p);
        helper.assertTrue(Puzzels.zeg(p, "kaas") && in.fouten(p) == 1, "a wrong answer: swallowed + counted");
        helper.assertFalse(Puzzels.zeg(p, "vads"), "the stone stops listening after an answer");
        Puzzels.klik(in.level, in.element(d.van("fluistersteen").get(0)), p);
        helper.assertTrue(Puzzels.zeg(p, "  De VADS! ") && PuzzelSpeler.opgelost(p, d.id()), "the answer, any case/punctuation");
        weg(in);
        schoon(p);
        helper.succeed();
    }

    /** Star stones: night only (forced here), toggled by clicks; the time trial: too late = a failed try. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void sterrenEnTijd(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        PuzzelDef d = Puzzels.def("z1_sterrenveld");
        Puzzels.Inst in = zet(helper, d, 7, p);
        int ster = d.van("sterrensteen").get(0);
        boolean was = Puzzels.nachtForceren;
        Puzzels.nachtForceren = false;
        if (!helper.getLevel().isDarkOutside()) {
            Puzzels.klik(in.level, in.element(ster), p);
            helper.assertFalse(Puzzels.aan(in, ster), "by day the star stones sleep");
        }
        Puzzels.nachtForceren = true;
        Puzzels.klik(in.level, in.element(ster), p);
        helper.assertTrue(Puzzels.aan(in, ster), "at night a click lights it");
        Puzzels.nachtForceren = was;
        weg(in);

        PuzzelDef t = Puzzels.def("z2_lantaarnsprint");
        Puzzels.Inst rit = zet(helper, t, 8, p);
        Puzzels.klik(rit.level, rit.element(t.van("startlantaarn").get(0)), p);
        rit.race.put(p.getUUID(), helper.getLevel().getGameTime() - 1);   // (time is up)
        Puzzels.tick(rit);
        helper.assertTrue(rit.fouten(p) == 1 && !PuzzelSpeler.opgelost(p, t.id()), "too late = a failed try, no reward");
        Puzzels.stap(rit.level, rit.element(t.van("eindplaat").get(0)), p);
        helper.assertFalse(PuzzelSpeler.opgelost(p, t.id()), "the end plate without a running race does nothing");
        helper.assertTrue(PuzzelDev.los(rit, p), "start + finish in time");
        weg(rit);
        schoon(p);
        helper.succeed();
    }

    /** The Verkenningsboek: zone 1-3 have the puzzle category, counted; solving one moves it by one. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void boek(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        for (int z = 1; z <= 3; z++) {
            Voltooiing.ZoneBoek boek = Voltooiing.zone("zone" + z);
            if (boek == null) {
                continue;
            }
            Voltooiing.herbouw("zone" + z);
            Voltooiing.Categorie cat = null;
            for (Voltooiing.Categorie c : Voltooiing.categorieen(boek)) {
                if (c.id().equals(PuzzelBoek.CAT)) {
                    cat = c;
                }
            }
            int zone = z;
            long aantal = Puzzels.defs().stream().filter(d -> d.zone() == zone && !d.id().startsWith("grot_")).count();   // V-INT: cave puzzles count in "Grotten"
            helper.assertTrue(cat != null && cat.dingen().size() == aantal && !cat.later(), "zone " + z + " counts its " + aantal + " puzzles");
            helper.assertTrue(Voltooiing.tel(p, cat).klaar() == 0, "nothing done yet");
            PuzzelDef d = Puzzels.defs().stream().filter(x -> x.zone() == zone && !x.id().startsWith("grot_")).findFirst().orElseThrow();
            PuzzelSpeler.map(p, "opgelost").putBoolean(d.id(), true);
            helper.assertTrue(Voltooiing.tel(p, cat).klaar() == 1, "one solved = one ticked");
        }
        schoon(p);
        helper.succeed();
    }
}
