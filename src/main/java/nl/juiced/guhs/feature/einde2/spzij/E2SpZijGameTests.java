package nl.juiced.guhs.feature.einde2.spzij;

import java.util.List;
import java.util.UUID;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * einde2 "Sponsguh", slice sp_zij (CONTRACT_E2_C 8.3): the core state of the side lines.
 * <ul>
 *   <li>{@code spZijSpelletjes}: the karate game (standing still is a soft shove that counts nothing and takes nothing away,
 *       a step aside or a jump counts, ten win) and the wait at the mooring (visible seconds, stepping off starts again);</li>
 *   <li>{@code spZijLijnen}: the gate (shut before the job, open after it: the three lines begin by themselves), the rocket
 *       parts and the lanterns (only the third moves the line, each counts once), the belt and the lunch (the variant 3_b),
 *       every step has a hint, and the resident Sandy-guh is there only for players past the karate.</li>
 * </ul>
 */
public class E2SpZijGameTests {
    private static final String BATCH = "einde2_sp_zij";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void spZijSpelletjes(GameTestHelper helper) {
        // karate: rest, wind-up, shove
        Boomkoepel.Karate k = new Boomkoepel.Karate(0, 0);
        Vec3 hier = new Vec3(0.5, 0, 0.5);
        helper.assertTrue(k.tik(10, hier, true) == Boomkoepel.Karate.Uit.NIETS, "she rests first");
        helper.assertTrue(k.tik(Boomkoepel.Karate.RUST, hier, true) == Boomkoepel.Karate.Uit.AANLOOP, "then she winds up");
        long t = Boomkoepel.Karate.RUST;
        helper.assertTrue(k.tik(t + 5, hier, true) == Boomkoepel.Karate.Uit.NIETS, "the wind-up takes a moment (no hidden clock: it is shown)");
        t += Boomkoepel.Karate.AANLOOP;
        helper.assertTrue(k.tik(t, hier.add(0.3, 0, 0), true) == Boomkoepel.Karate.Uit.GERAAKT && k.ontweken == 0,
                "standing (almost) still: a soft shove, nothing counted");
        // a step aside and a jump both count; nothing is ever taken away
        int ontweken = 0;
        for (int i = 0; i < Boomkoepel.Karate.NODIG; i++) {
            t += Boomkoepel.Karate.RUST;
            helper.assertTrue(k.tik(t, hier, true) == Boomkoepel.Karate.Uit.AANLOOP, "wind-up " + i);
            t += Boomkoepel.Karate.AANLOOP;
            Boomkoepel.Karate.Uit uit = i % 2 == 0 ? k.tik(t, hier.add(1.2, 0, 0), true) : k.tik(t, hier, false);
            ontweken++;
            helper.assertTrue(k.ontweken == ontweken && uit == (ontweken >= Boomkoepel.Karate.NODIG ? Boomkoepel.Karate.Uit.KLAAR
                    : Boomkoepel.Karate.Uit.ONTWEKEN), "dodge " + ontweken + " counts (" + (i % 2 == 0 ? "aside" : "jump") + ")");
        }
        helper.assertTrue(k.tik(t + 999, hier, true) == Boomkoepel.Karate.Uit.KLAAR, "ten dodges: won, and it stays won");
        helper.assertTrue(new Boomkoepel.Karate(7, 0).ontweken == 7, "a game picks up the saved count");
        // the wait at the mooring: visible seconds, stepping off starts again
        UUID id = UUID.randomUUID();
        helper.assertTrue(Guhlander.wacht(id, false, 0, 10) == -1, "not on the mooring: no wait");
        helper.assertTrue(Guhlander.wacht(id, true, 100, 10) == 10, "on the mooring: 10 seconds to go");
        helper.assertTrue(Guhlander.wacht(id, true, 100 + 61, 10) == 7, "three seconds later: 7 to go");
        helper.assertTrue(Guhlander.wacht(id, false, 100 + 80, 10) == -1 && Guhlander.wacht(id, true, 200, 10) == 10, "stepped off: it starts again");
        helper.assertTrue(Guhlander.wacht(id, true, 200 + 199, 10) == 1 && Guhlander.wacht(id, true, 200 + 200, 10) == 0, "ten seconds on it: the ship comes");
        helper.assertTrue(Guhlander.wacht(id, true, 401, 10) == 10, "(a new wait after that starts fresh)");
        Guhlander.wacht(id, false, 0, 10);
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void spZijLijnen(GameTestHelper helper) {
        ServerPlayer p = speler(helper), q = speler(helper);
        Verhaallijn sandy = SpZijLijnen.SANDY_LIJN, guhlander = SpZijLijnen.GUHLANDER_LIJN, held = SpZijLijnen.ZEEMEERMAN_LIJN;
        SponsguhWezen bewoner = null;
        try {
            // the gate: the Guhvatar story done is not enough, the job is
            helper.assertTrue(!SpZij.open(p) && !SpZijLijnen.beginWatOpenIs(p), "before the Guhvatar story: shut");
            SpVerhaalDev.guhvatarKlaar(p);
            SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, SpVerhaal.R_SOLLICITATIE);
            helper.assertTrue(!SpZij.open(p), "at the job interview: still shut");
            SpVerhaal.RECEPT_LIJN.verder(p, SpVerhaal.R_SOLLICITATIE);
            helper.assertTrue(SpZij.open(p) && SpZijLijnen.beginWatOpenIs(p) && !SpZijLijnen.beginWatOpenIs(p)
                    && sandy.begonnen(p) && guhlander.begonnen(p) && held.begonnen(p), "the job done: open, the three lines begin (once)");
            for (Verhaallijn l : SpZijLijnen.lijnen()) {
                for (int s = 0; s < l.stappen(); s++) {
                    helper.assertTrue(StapHint.hint(l.id(), s) != null, l.id() + " step " + s + " has a hint");
                }
                helper.assertTrue(SpVerhaal.zijlijn(l.id()) == l && SpVerhaal.GROEP.equals(l.groep()), l.id() + " is registered in the group sponsguh");
            }
            // Sandy: the parts (only the third moves the line, each counts once)
            SpZijCommando.zetTot(p, SpZijLijnen.SANDY, SpZijLijnen.S_ONDERDELEN);
            helper.assertTrue(Boomkoepel.pak(p, null, 2) && !Boomkoepel.pak(p, null, 2) && sandy.stap(p) == SpZijLijnen.S_ONDERDELEN,
                    "a part counts once, the line waits");
            helper.assertTrue(Boomkoepel.pak(p, null, 1) && Boomkoepel.pak(p, null, 3) && sandy.stap(p) == SpZijLijnen.S_RAKET
                    && Boomkoepel.gevonden(p) == 3, "the third part: to the rocket");
            Boomkoepel.klaar(p);
            helper.assertTrue(sandy.klaar(p) && !sandy.eenmalig(p, "luchthelm"), "the rocket: the line done, the luchthelm given once");
            // the lanterns
            SpZijCommando.zetTot(p, SpZijLijnen.GUHLANDER, SpZijLijnen.G_LANTAARNS);
            helper.assertTrue(Guhlander.pak(p, null, 3) && Guhlander.pak(p, null, 1) && guhlander.stap(p) == SpZijLijnen.G_LANTAARNS
                    && Guhlander.pak(p, null, 2) && guhlander.stap(p) == SpZijLijnen.G_TERUG, "three lanterns: back to the captain");
            helper.assertTrue(!Guhlander.pak(p, null, 2), "(a lantern after the step counts nothing)");
            // the heroes: the belt, the lunch (variant 3_b)
            SpZijCommando.zetTot(p, SpZijLijnen.ZEEMEERMAN, SpZijLijnen.Z_RIEM);
            helper.assertTrue(Helden.pakRiem(p, null) && !Helden.pakRiem(p, null) && held.stap(p) == SpZijLijnen.Z_RIEM_TERUG, "the belt: once");
            held.verder(p, SpZijLijnen.Z_RIEM_TERUG);
            helper.assertTrue("3".equals(held.sleutel(p)), "the errand: to the front door");
            Helden.lunchOpgehaald(p);
            helper.assertTrue("3_b".equals(held.sleutel(p)) && held.stap(p) == SpZijLijnen.Z_LUNCH, "the lunch fetched: the variant 3_b");
            // the resident Sandy-guh only for players past the karate; each player before it has their own
            bewoner = E2SponsguhFeature.SANDY_GUH.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
            helper.assertTrue(bewoner != null, "a Sandy-guh");
            Boomkoepel.markeer(bewoner, SpZij.BB + "/sandy");
            SpZijCommando.zetTot(q, SpZijLijnen.SANDY, SpZijLijnen.S_KARATE);
            helper.assertTrue(Zicht.magZien(p, bewoner) && !Zicht.magZien(q, bewoner), "the resident: seen past the karate, not at it");
            Boomkoepel.markeer(bewoner, SpZij.BB + "/zeemeerman");
            helper.assertTrue(SpZijLijnen.SANDY_LIJN.stappen() == SpZijLijnen.S_KLAAR && SpZijLijnen.GUHLANDER_LIJN.stappen() == SpZijLijnen.G_KLAAR
                    && SpZijLijnen.ZEEMEERMAN_LIJN.stappen() == SpZijLijnen.Z_KLAAR && SpZijLijnen.ZEEMEERMAN_LIJN.extraSleutels().equals(List.of("3_b")),
                    "the step counts and the one variant");
        } finally {
            if (bewoner != null) {
                bewoner.discard();
            }
            for (ServerPlayer x : List.of(p, q)) {
                SpZijCommando.wis(x);
                SpVerhaalDev.wis(x);
                for (String l : E2Verhaal.HOOFDSTUKKEN) {
                    E2VerhaalDev.wis(x, l);
                }
                helper.getLevel().removePlayerImmediately(x, Entity.RemovalReason.DISCARDED);
            }
        }
        helper.succeed();
    }
}
