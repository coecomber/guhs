package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Groep;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Zoek;
import nl.juiced.guhs.feature.vadsrijk.ambacht.PlekWereld.Spot;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * "Korf verplaatsen": a click on a free hive stand while the zone is full offers the swap window; confirming takes exactly the
 * chosen hive away (its combs are gone, nothing is given), puts the new hive on the clicked stand and keeps the count at the
 * maximum; closing changes nothing; forged requests are refused. Run: {@code runGameTestServer -Pgt=VadsrijkKorfWissel}.
 */
public class VadsrijkKorfWisselGameTests {
    private static final String BATCH = "vadsrijk_korfwissel";

    /** Hive stand {@code naam} of a test meadow in {@code zone} (tier 3: Verzamelen level 1 is enough). */
    private static Spot stand(GameTestHelper helper, String naam, int zone, int i) {
        Groep g = new Groep("t_wissel_" + naam, zone, "x", NodeSoort.KORF, 3, 1, 5, Zoek.BLOEM, "bloesemhoning");
        return new Spot(g.id() + "_0", g, List.of(helper.absolutePos(new BlockPos(1 + i, 2, 1))), List.of(helper.absolutePos(new BlockPos(1 + i, 2, 3))));
    }

    /** A fresh player with a full zone 3: four hives (returned), the second one filled with combs. */
    private static Spot[] volleZone(GameTestHelper helper, ServerPlayer p, String wie) {
        ServerLevel sl = helper.getLevel();
        AmbachtData.reset(p);
        Spot[] k = new Spot[Imkeren.PER_ZONE];
        for (int i = 0; i < k.length; i++) {
            k[i] = stand(helper, wie + i, 3, i);
            helper.assertTrue(Verzamelen.doeOogst(p, sl, k[i], 0).isEmpty() && Imkeren.eigen(p, k[i]), "hive " + i + " placed");
        }
        Imkeren.vul(p, k[1]);
        helper.assertTrue(Imkeren.inZone(p, 3) == Imkeren.PER_ZONE && Imkeren.raten(p, k[1]) == Imkeren.MAX_RATEN, "zone 3 is full, hive 1 has combs");
        return k;
    }

    private static int honing(ServerPlayer p) {
        return MateriaalTas.tel(p, s -> Materialen.van(s) != null);
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void volBiedtWisselAan(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spot[] k = volleZone(helper, p, "a");
        Spot nieuw = stand(helper, "a_nieuw", 3, 5);
        helper.assertTrue(Imkeren.wisselOpen(p) == null, "no window before the click");
        helper.assertTrue(Verzamelen.doeOogst(p, sl, nieuw, 0).equals("vadsrijk.ambacht.korf.vol"), "the click itself is still refused");
        helper.assertTrue(nieuw.id().equals(Imkeren.wisselOpen(p)), "...but the swap window is offered for this stand: " + Imkeren.wisselOpen(p));
        helper.assertTrue(!Imkeren.eigen(p, nieuw) && Imkeren.inZone(p, 3) == Imkeren.PER_ZONE && Imkeren.raten(p, k[1]) == Imkeren.MAX_RATEN,
                "nothing changed yet");
        // a full OTHER zone does not matter: a stand in zone 4 with room is simply placed (no window)
        AmbachtData.zetLevel(p, Ambacht.VERZAMELEN, 60);
        Imkeren.vergeet(p.getUUID());
        Spot z4 = stand(helper, "a_z4", 4, 6);
        helper.assertTrue(Verzamelen.doeOogst(p, sl, z4, 0).isEmpty() && Imkeren.eigen(p, z4) && Imkeren.wisselOpen(p) == null, "zone 4 has room");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bevestigenWisselt(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spot[] k = volleZone(helper, p, "b");
        Imkeren.vul(p, k[2]);
        Spot nieuw = stand(helper, "b_nieuw", 3, 5);
        Verzamelen.doeOogst(p, sl, nieuw, 0);
        helper.assertTrue(Imkeren.ratenVan(p, k[1].id()) == Imkeren.MAX_RATEN, "the window shows the combs of the hive");
        String m = Imkeren.doeWissel(p, k[1].id(), nieuw);
        helper.assertTrue(m.isEmpty(), "swap: " + m);
        helper.assertTrue(!Imkeren.eigen(p, k[1]) && Imkeren.raten(p, k[1]) == 0 && !Imkeren.korven(p).contains(k[1].id()), "the chosen hive is gone");
        helper.assertTrue(honing(p) == 0, "its combs are lost: nothing in the bag");
        helper.assertTrue(Imkeren.eigen(p, nieuw) && Imkeren.raten(p, nieuw) == 0, "the new hive stands, empty");
        helper.assertTrue(Imkeren.eigen(p, k[0]) && Imkeren.eigen(p, k[2]) && Imkeren.eigen(p, k[3]) && Imkeren.raten(p, k[2]) == Imkeren.MAX_RATEN,
                "the other three stay, with their combs");
        helper.assertTrue(Imkeren.inZone(p, 3) == Imkeren.PER_ZONE && Imkeren.ids(p).size() == Imkeren.PER_ZONE, "still 4/4");
        helper.assertTrue(Imkeren.wisselOpen(p) == null, "the window is done");
        // the freed stand is a normal free stand again: full zone -> the window, and a hive put back there starts empty
        helper.assertTrue(Verzamelen.doeOogst(p, sl, k[1], 0).equals("vadsrijk.ambacht.korf.vol") && k[1].id().equals(Imkeren.wisselOpen(p)), "freed stand offers again");
        helper.assertTrue(Imkeren.doeWissel(p, nieuw.id(), k[1]).isEmpty() && Imkeren.eigen(p, k[1]) && Imkeren.raten(p, k[1]) == 0, "back, without the old combs");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void sluitenVerandertNiets(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spot[] k = volleZone(helper, p, "c");
        Spot nieuw = stand(helper, "c_nieuw", 3, 5);
        List<String> voor = Imkeren.ids(p);
        Verzamelen.doeOogst(p, sl, nieuw, 0);
        Imkeren.vergeet(p.getUUID());   // (what the "dicht" of the closed window does)
        helper.assertTrue(Imkeren.ids(p).equals(voor) && Imkeren.raten(p, k[1]) == Imkeren.MAX_RATEN && !Imkeren.eigen(p, nieuw), "cancel: nothing changed");
        // a confirm that comes after the window closed is refused
        helper.assertTrue(Imkeren.doeWissel(p, k[1].id(), nieuw).equals(Imkeren.K_WISSEL + "kan_niet"), "no swap without an open window");
        helper.assertTrue(Imkeren.ids(p).equals(voor) && Imkeren.raten(p, k[1]) == Imkeren.MAX_RATEN && !Imkeren.eigen(p, nieuw), "still nothing changed");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void vervalstGeweigerd(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper), ander = GuhMockPlayer.of(helper);
        Spot[] k = volleZone(helper, p, "d");
        Spot[] vanAnder = volleZone(helper, ander, "e");
        AmbachtData.zetLevel(p, Ambacht.VERZAMELEN, 60);
        Spot z4 = stand(helper, "d_z4", 4, 6);
        helper.assertTrue(Verzamelen.doeOogst(p, sl, z4, 0).isEmpty(), "a hive in zone 4");
        Imkeren.vul(p, z4);
        Spot nieuw = stand(helper, "d_nieuw", 3, 5);
        List<String> voor = Imkeren.ids(p), voorAnder = Imkeren.ids(ander);
        String nee = Imkeren.K_WISSEL + "kan_niet";
        // someone else's hive
        Verzamelen.doeOogst(p, sl, nieuw, 0);
        helper.assertTrue(Imkeren.doeWissel(p, vanAnder[1].id(), nieuw).equals(nee), "another player's hive is refused");
        helper.assertTrue(Imkeren.ids(ander).equals(voorAnder) && Imkeren.raten(ander, vanAnder[1]) == Imkeren.MAX_RATEN, "the other player keeps everything");
        // a hive of mine in another zone
        Verzamelen.doeOogst(p, sl, nieuw, 0);
        helper.assertTrue(Imkeren.doeWissel(p, z4.id(), nieuw).equals(nee) && Imkeren.eigen(p, z4) && Imkeren.raten(p, z4) == Imkeren.MAX_RATEN,
                "a hive in another zone is refused");
        // a hive that does not exist
        Verzamelen.doeOogst(p, sl, nieuw, 0);
        helper.assertTrue(Imkeren.doeWissel(p, "t_wissel_bestaat_niet_0", nieuw).equals(nee), "an unknown hive is refused");
        // another stand than the one the window was offered for
        Verzamelen.doeOogst(p, sl, nieuw, 0);
        Spot stiekem = stand(helper, "d_stiekem", 3, 7);
        helper.assertTrue(Imkeren.doeWissel(p, k[0].id(), stiekem).equals(nee), "only the offered stand");
        // a stand that is not in the world (the packet entry looks it up), and one refusal closes the offer
        Verzamelen.doeOogst(p, sl, nieuw, 0);
        helper.assertTrue(Imkeren.wissel(p, k[0].id(), "t_wissel_bestaat_niet_0").equals(nee) && Imkeren.wisselOpen(p) == null, "an unknown stand is refused");
        helper.assertTrue(Imkeren.doeWissel(p, k[0].id(), nieuw).equals(nee), "...and the offer is closed after a refusal");
        // not at the maximum: no swap (a plain click places)
        Verzamelen.doeOogst(p, sl, nieuw, 0);
        Imkeren.eenWeg(p, k[3].id());
        helper.assertTrue(Imkeren.doeWissel(p, k[0].id(), nieuw).equals(nee) && Imkeren.eigen(p, k[0]) && !Imkeren.eigen(p, nieuw), "no swap below the maximum");
        List<String> over = new java.util.ArrayList<>(voor);
        over.remove(k[3].id());
        helper.assertTrue(Imkeren.ids(p).equals(over) && Imkeren.raten(p, k[1]) == Imkeren.MAX_RATEN && Imkeren.raten(p, z4) == Imkeren.MAX_RATEN,
                "after all refusals: my other hives and combs are as before: " + Imkeren.ids(p));
        helper.succeed();
    }
}
