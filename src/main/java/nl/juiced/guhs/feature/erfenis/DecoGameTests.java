package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.erfenis.koppeling.Dorpsbeloningen;
import nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * De Erfenis, deco slice: the decowinkel (prices, Hoeve locks, Decobon), storage (nothing is ever lost or sold: deco back
 * into the decoschuurtje, path tiles into the stock and drawn again for free), the path layer's neighbour bitmask with real
 * corners and crossings in the world, hekjes connecting, and the Knusheid (paths per 10, hekjes per 5/4, milestones, record).
 */
public class DecoGameTests {
    private static final String BATCH = "erfenis_deco";
    private static final long T0 = 1_000_000_000L;

    /** A base with all land open, Hoeve 10 and plenty of Guhlden. */
    private static Base ruim() {
        Base b = Base.nieuw(UUID.randomUUID(), "Decoguh", 0, 0, T0);
        b.allesOpen();
        b.hoeve().level = 10;
        b.guhlden = 100_000;
        return b;
    }

    private static List<int[]> rij(int x0, int z0, int x1, int z1) {
        List<int[]> l = new ArrayList<>();
        l.add(new int[] {x0, z0});
        l.addAll(Paden.stap(x0, z0, x1, z1));
        return l;
    }

    /** Every shop piece has a vormen shape (or is a path kind), a name, and the catalogue numbers of deco.md. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void catalogus(GameTestHelper helper) {
        int paden = 0;
        for (DecoWinkel.Stuk s : DecoWinkel.alle()) {
            if (s.soort() == DecoWinkel.Soort.PAD) {
                helper.assertTrue(GebouwVormen.padSoorten().contains(s.id()), "path kind " + s.id() + " exists in vormen");
                paden++;
            } else {
                helper.assertTrue(GebouwVormen.decoIds().contains(s.id()), "deco " + s.id() + " has a vormen shape");
                int[] m = GebouwVormen.decoMaat(s.id());
                helper.assertTrue(s.soort() != DecoWinkel.Soort.HEK || m[0] == 1 && m[1] == 1, s.id() + ": a hekje is 1x1");
                helper.assertTrue(s.soort() == DecoWinkel.Soort.HEK || ErfenisDeco.knusheid(s.id()) > 0, s.id() + " gives Knusheid");
            }
            helper.assertTrue(s.prijs() > 0 && s.vanaf() >= 1 && s.vanaf() <= 10, s.id() + ": price and Hoeve level");
        }
        helper.assertTrue(paden == 9, "9 path kinds, got " + paden);
        helper.assertTrue(DecoWinkel.stuk("zandpaadje").prijs() == 1 && DecoWinkel.stuk("sterrenpad").vanaf() == 9
                && DecoWinkel.stuk("kristalfontein").prijs() == 6000 && DecoWinkel.stuk("wolkenwilg").vanaf() == 8, "deco.md prices");
        helper.assertTrue(DecoWinkel.stuk("trekguh") == null && DecoWinkel.stuk("kaassausfonteintje") == null, "special pieces are not sold");
        helper.succeed();
    }

    /** Buying: Guhlden, the Hoeve lock, not enough money, the Decobon (only for real pieces). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void kopen(GameTestHelper helper) {
        Base b = ruim();
        b.hoeve().level = 3;
        b.guhlden = 100;
        helper.assertTrue(DecoWinkel.koop(b, "bloembakje", false, 0) == null && b.guhlden == 85 && b.decoOpslag.get("bloembakje") == 1,
                "bloembakje: 15 Guhlden, into the schuurtje");
        Base.Fout f = DecoWinkel.koop(b, "picknicktafel", false, 0);
        helper.assertTrue(f != null && "erfenis.fout.slot".equals(f.key()) && b.guhlden == 85, "picknicktafel waits for Hoeve 4");
        f = DecoWinkel.koop(b, "zonnebloemrij", false, 0);
        helper.assertTrue(f != null && "erfenis.fout.guhlden".equals(f.key()) && b.guhlden == 85, "90 Guhlden is too much");
        helper.assertTrue(DecoWinkel.koop(b, "trekguh", false, 0) != null, "a find is not for sale");
        // the Decobon (koppeling's Dorpsbeloningen)
        var server = helper.getLevel().getServer();
        UUID eigenaar = b.eigenaar;
        Dorpsbeloningen.geefDecobon(server, eigenaar, 1);
        f = DecoActies.koop(server, b, "knarspad", true);
        helper.assertTrue(f != null && Dorpsbeloningen.decobonnen(server, eigenaar) == 1, "no Decobon for a path tile");
        helper.assertTrue(DecoActies.koop(server, b, "zonnebloemrij", true) == null && b.guhlden == 85 && b.decoOpslag.get("zonnebloemrij") == 1
                && Dorpsbeloningen.decobonnen(server, eigenaar) == 0, "the Decobon buys the zonnebloemrij for free");
        helper.assertTrue(DecoActies.koop(server, b, "bloembakje", true) != null, "no bon left");
        helper.succeed();
    }

    /** Nothing is lost or sold: put away = schuurtje, gum = stock (drawn again for free), buildings and deco push paths aside. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opslag(GameTestHelper helper) {
        Base b = ruim();
        int geld = b.guhlden;
        Paden.Uitkomst u = Paden.trek(b, "knarspad", rij(70, 80, 79, 80));
        helper.assertTrue(u.gezet() == 10 && u.betaald() == 20 && b.guhlden == geld - 20 && Paden.tegels(b) == 10, "10 tiles of knarspad, 2 each");
        u = Paden.trek(b, "knarspad", rij(70, 80, 74, 80));
        helper.assertTrue(u.gezet() == 0 && u.overgeslagen() == 0 && b.guhlden == geld - 20, "drawing over the same path: nothing, free");
        u = Paden.gum(b, rij(70, 80, 73, 80));
        helper.assertTrue(u.gezet() == 4 && Paden.tegels(b) == 6 && b.padVoorraad.get("knarspad") == 4 && b.guhlden == geld - 20,
                "the gum: 4 tiles into the stock, no money back");
        u = Paden.trek(b, "knarspad", rij(70, 82, 75, 82));
        helper.assertTrue(u.gezet() == 6 && u.uitVoorraad() == 4 && u.betaald() == 4 && !b.padVoorraad.containsKey("knarspad"),
                "drawing again: 4 from the stock for free, 2 paid");
        // another kind over it: swaps, the old tile into the stock
        u = Paden.trek(b, "zandpaadje", rij(75, 82, 75, 82));
        helper.assertTrue(u.gezet() == 1 && "zandpaadje".equals(Paden.padOp(b, 75, 82)) && b.padVoorraad.get("knarspad") == 1, "swap kinds");
        // hekjes: the stock is the decoschuurtje
        u = Paden.trek(b, "wilgenhekje", rij(70, 90, 74, 90));
        helper.assertTrue(u.gezet() == 5 && b.deco.size() == 5 && u.betaald() == 15, "5 wilgenhekjes, 3 each");
        helper.assertTrue(Paden.trek(b, "wilgenhekje", rij(76, 82, 76, 82)).gezet() == 1
                && Paden.trek(b, "wilgenhekje", rij(75, 82, 75, 82)).gezet() == 0, "a hekje never on a path");
        u = Paden.gum(b, rij(70, 90, 72, 90));
        helper.assertTrue(u.gezet() == 3 && b.decoOpslag.get("wilgenhekje") == 3 && b.deco.size() == 3, "gum: hekjes into the schuurtje");
        // a deco piece over a path takes the tiles up; a building over paths and deco too
        b.decoOpslag.put("picknicktafel", 1);
        helper.assertTrue(b.plaatsDeco("picknicktafel", 76, 79, 0) == null && Paden.padOp(b, 76, 80) == null
                && b.padVoorraad.getOrDefault("knarspad", 0) >= 3, "deco over a path: the tiles go into the stock");
        int voor = Paden.tegels(b);
        helper.assertTrue(b.plaats(GebouwType.KIPPENHOK, 70, 76, 0, T0, 0) == null, "a kippenhok over it all");
        helper.assertTrue(Paden.tegels(b) < voor && b.decoOp(76, 79) == null && b.decoOpslag.get("picknicktafel") == 1,
                "the building: paths into the stock, the picknicktafel back into the schuurtje");
        for (int x = 70; x < 78; x++) {
            for (int z = 76; z < 86; z++) {
                helper.assertTrue(Paden.padOp(b, x, z) == null, "no path left under the kippenhok");
            }
        }
        // moving and turning a placed piece
        b.decoOpslag.put("guhbankje_hout", 1);
        helper.assertTrue(b.plaatsDeco("guhbankje_hout", 90, 90, 0) == null, "bankje placed");
        Base.Deco d = b.decoOp(90, 90);
        helper.assertTrue(b.verplaatsDeco(d, 92, 94, 1) == null && b.decoOp(92, 94) != null && b.decoOp(92, 95) != null
                && b.decoOp(93, 94) == null && b.decoOp(90, 90) == null, "moved and turned: 1x2 now");
        helper.succeed();
    }

    /** The neighbour bitmask and the real blocks: straight, corner, crossing and end of a zandpaadje (pad.json). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void padHoekjes(GameTestHelper helper) {
        Base b = ruim();
        // a plus: (2,1)..(2,5) and (0,3)..(4,3), plus an L at (6,1)-(6,3)-(8,3)
        Paden.trek(b, "zandpaadje", rij(2, 1, 2, 5));
        Paden.trek(b, "zandpaadje", rij(0, 3, 4, 3));
        Paden.trek(b, "zandpaadje", rij(6, 1, 6, 3));
        Paden.trek(b, "zandpaadje", rij(6, 3, 8, 3));
        helper.assertTrue(Paden.verbindingen(b, 2, 3) == 15, "crossing: N E S W");
        helper.assertTrue(Paden.verbindingen(b, 2, 2) == 5, "straight N-S");
        helper.assertTrue(Paden.verbindingen(b, 2, 1) == 4, "end: only S");
        helper.assertTrue(Paden.verbindingen(b, 6, 3) == (1 | 2), "corner N-E");
        helper.assertTrue(Paden.verbindingen(b, 1, 3) == (2 | 8), "straight E-W");
        // into the world (the plot's corner = the test's origin), then the blocks
        ServerLevel level = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(0, 0, 0));
        for (int x = 0; x < 10; x++) {
            for (int z = 0; z < 7; z++) {
                level.setBlockAndUpdate(o.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        List<int[]> alles = new ArrayList<>();
        for (int x = 0; x < 10; x++) {
            for (int z = 0; z < 7; z++) {
                alles.add(new int[] {x, z});
            }
        }
        DecoActies.stempelTegels(level, o, b, alles);
        GebouwVormen.klaarNu();
        helper.assertTrue(level.getBlockState(o.offset(2, 0, 3)).is(Blocks.COARSE_DIRT), "crossing = coarse dirt, got " + level.getBlockState(o.offset(2, 0, 3)));
        helper.assertTrue(level.getBlockState(o.offset(2, 0, 2)).is(Blocks.DIRT_PATH), "straight = dirt path");
        helper.assertTrue(level.getBlockState(o.offset(2, 0, 1)).is(Blocks.ROOTED_DIRT), "end = rooted dirt");
        helper.assertTrue(level.getBlockState(o.offset(5, 0, 5)).is(Blocks.GRASS_BLOCK), "no path = grass");
        // erase one arm: the crossing becomes a T (coarse dirt in zandpaadje too), the arm's end becomes an end
        Paden.Uitkomst u = Paden.gum(b, rij(2, 4, 2, 5));
        DecoActies.stempelTegels(level, o, b, u.veranderd());
        GebouwVormen.klaarNu();
        helper.assertTrue(Paden.verbindingen(b, 2, 3) == (1 | 2 | 8), "a T now");
        helper.assertTrue(level.getBlockState(o.offset(2, 0, 4)).is(Blocks.GRASS_BLOCK), "the erased tile is grass again");
        helper.succeed();
    }

    /** Hekjes drawn in a row connect to each other in the world, and let go when one is erased. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void hekjesVerbinden(GameTestHelper helper) {
        Base b = ruim();
        Paden.trek(b, "wilgenhekje", rij(1, 2, 4, 2));
        ServerLevel level = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(0, 0, 0));
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 5; z++) {
                level.setBlockAndUpdate(o.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        DecoActies.stempelTegels(level, o, b, rij(1, 2, 4, 2));
        GebouwVormen.klaarNu();
        BlockState s = level.getBlockState(o.offset(2, 1, 2));
        helper.assertTrue(s.is(Blocks.BAMBOO_FENCE) && s.getValue(FenceBlock.EAST) && s.getValue(FenceBlock.WEST), "middle hekje connects both ways: " + s);
        Paden.Uitkomst u = Paden.gum(b, rij(3, 2, 3, 2));
        DecoActies.stempelTegels(level, o, b, u.veranderd());
        GebouwVormen.klaarNu();
        s = level.getBlockState(o.offset(2, 1, 2));
        helper.assertTrue(s.is(Blocks.BAMBOO_FENCE) && !s.getValue(FenceBlock.EAST) && s.getValue(FenceBlock.WEST), "its neighbour is gone: " + s);
        helper.assertTrue(level.getBlockState(o.offset(3, 1, 2)).isAir(), "the erased hekje is gone");
        helper.succeed();
    }

    /** Knusheid: paths 1 per 10 tiles, hekjes 1 per 5 (Knuffelhaag 1 per 4), pieces their own value; the record keeps titles. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void knusheid(GameTestHelper helper) {
        Base b = ruim();
        int basis = b.knusheid();
        Paden.trek(b, "zandpaadje", rij(70, 80, 78, 80));
        helper.assertTrue(b.knusheid() == basis, "9 tiles: nothing yet");
        Paden.trek(b, "zandpaadje", rij(79, 80, 79, 80));
        helper.assertTrue(b.knusheid() == basis + 1 && Knusheid.paden(b) == 1, "10 tiles: +1");
        Paden.trek(b, "wilgenhekje", rij(70, 90, 73, 90));
        helper.assertTrue(b.knusheid() == basis + 1, "4 hekjes: nothing yet");
        Paden.trek(b, "wilgenhekje", rij(74, 90, 74, 90));
        helper.assertTrue(b.knusheid() == basis + 2, "5 hekjes: +1");
        Paden.trek(b, "knuffelhaag", rij(70, 92, 73, 92));
        helper.assertTrue(b.knusheid() == basis + 3, "4 knuffelhagen: +1");
        b.decoOpslag.put("kristalfontein", 1);
        helper.assertTrue(b.plaatsDeco("kristalfontein", 90, 90, 0) == null && b.knusheid() == basis + 43, "kristalfontein: +40");
        helper.assertTrue(b.knusheid() == Knusheid.gebouwen(b) + Knusheid.paden(b) + b.knusDeco, "the parts add up");
        // milestones and the record
        b.knusRecord = 0;
        b.knusGemeld = 0;
        Erfenis.meldKnus(b);
        helper.assertTrue(b.knusRecord == b.knusheid() && b.momenten.contains("knusheid:" + b.knusheid()), "the moment and the record");
        int mijlpalen = 0;
        for (int m : Knusheid.MIJLPALEN) {
            mijlpalen += m <= b.knusheid() ? 1 : 0;
        }
        helper.assertTrue(b.meldingen.size() == mijlpalen, "one message per milestone reached: " + b.meldingen.size() + " vs " + mijlpalen);
        b.meldingen.clear();
        int record = b.knusRecord;
        b.bergDecoOp(b.decoOp(90, 90));
        Erfenis.meldKnus(b);
        helper.assertTrue(b.knusRecord == record && b.meldingen.isEmpty() && b.knusheid() == record - 40, "put away: Knusheid down, record stays");
        helper.assertTrue(Knusheid.volgende(0) == 50 && Knusheid.volgende(250) == 500 && Knusheid.volgende(99999) == -1, "next milestone");
        // saving keeps the stock and the record
        b.padVoorraad.put("knarspad", 7);
        Base terug = Base.load(b.save(false));
        helper.assertTrue(terug.padVoorraad.get("knarspad") == 7 && terug.knusRecord == record && Paden.tegels(terug) == 10, "saved and loaded");
        helper.succeed();
    }

    /** The stroke helper: every step goes to a side neighbour, so a drawn diagonal still connects. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void streek(GameTestHelper helper) {
        List<int[]> l = Paden.stap(0, 0, 5, 3);
        int[] vorig = {0, 0};
        for (int[] t : l) {
            helper.assertTrue(Math.abs(t[0] - vorig[0]) + Math.abs(t[1] - vorig[1]) == 1, "side by side");
            vorig = t;
        }
        helper.assertTrue(vorig[0] == 5 && vorig[1] == 3 && l.size() == 8, "ends there in 8 steps");
        List<int[]> terug = Paden.decodeer(Paden.codeer(l));
        helper.assertTrue(terug.size() == 8 && terug.get(7)[0] == 5, "code and decode");
        helper.succeed();
    }
}
