package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.verhaal.E2Hervat;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.Richting;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 fix round 1 (fix-kern, playtest 1): story continuity. The headless test server has no Guheinde, so the places
 * themselves are not asked; what is tested is that nothing can put a player back.
 * <ul>
 *   <li>{@code einde2KernKaasbolHoudtStap}: a player in the middle of chapter 1 who clicks the kaasbol twice (and a late
 *       "ball opened") stays at their step; the ball is theirs to come back with; after chapter 1 the next chapter is the
 *       story; at step 0 it is still the opening;</li>
 *   <li>{@code einde2KernHints}: the hints of chapter 1's steps 2-9 and their look scenes (short, no click waits), the
 *       hint waits for its step's start, the airbending lesson after the luchtbal match comes once, and the direction
 *       phrases.</li>
 * </ul>
 */
public class E2KernHervatGameTests {
    private static final String BATCH = "einde2_kern";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        E2VerhaalDev.wis(p, E2Verhaal.LUCHT);
        E2VerhaalDev.wis(p, E2Verhaal.WATER);
        GuhQuests.saved(p).putInt(GuheindeGevecht.WINS, 1);
        return p;
    }

    private static void weg(ServerPlayer p) {
        AppaGuhEntity appa = Appa.geladen(p);
        if (appa != null) {
            appa.discard();
        }
        Kaasbol.vergeet(p.getUUID());
        StapHint.vergeet(p.getUUID());
        p.discard();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2KernKaasbolHoudtStap(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            helper.assertTrue(Kaasbol.mag(p) && !E2Hervat.kanVerder(p) && E2Hervat.huidig(p) == null, "step 0: the ball is the opening");
            E2VerhaalDev.zetTot(p, E2Verhaal.LUCHT, E2H1Lucht.ZWEVEN);
            helper.assertTrue(!Kaasbol.mag(p) && Kaasbol.zichtbaar(p) && E2Hervat.kanVerder(p) && E2Hervat.huidig(p) == E2H1Lucht.LIJN,
                    "in the middle of chapter 1: the ball stays as the way back, no opening");
            String een = Kaasbol.klik(p, p.blockPosition()), twee = Kaasbol.klik(p, p.blockPosition());
            helper.assertTrue(een.equals("verder") && twee.equals("verder"), "both clicks: back to the story, never the intro (" + een + ", " + twee + ")");
            Kaasbol.geopend(p);
            helper.assertTrue(E2H1Lucht.LIJN.stap(p) == E2H1Lucht.ZWEVEN, "the step is kept (" + E2H1Lucht.LIJN.stap(p) + ")");
            helper.assertTrue(!Appa.heeft(p), "a late 'ball opened' gives no Appa and no flight to the temple");
            // chapter 1 done: the next chapter is the story, the ball still brings the player back, nothing moves
            E2VerhaalDev.klaar(p, E2Verhaal.LUCHT);
            Verhaallijn water = E2Verhaal.lijn(E2Verhaal.WATER);
            helper.assertTrue(water != null && E2Hervat.huidig(p) == water && E2Hervat.kanVerder(p), "after chapter 1: chapter 2 is the story");
            helper.assertTrue(Kaasbol.klik(p, p.blockPosition()).equals("verder") && water.stap(p) == 0 && E2H1Lucht.LIJN.klaar(p),
                    "a click keeps both chapters as they are");
            helper.succeed();
        } finally {
            weg(p);
        }
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2KernHints(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            // merge_fix1: every step inside the temple (2..9) has a hint with its own look scene (fix-h1's steps shine themselves)
            for (int stap = E2H1Lucht.HAL; stap <= E2H1Lucht.ZUKA; stap++) {
                StapHint.Hint h = StapHint.hint(E2Verhaal.LUCHT, stap);
                Cutscene s = h == null ? null : h.kijk();
                helper.assertTrue(h != null && h.bijStartAan() && h.tekst() != null && s != null && s != StapHint.KIJK, "hint of step " + stap);
                helper.assertTrue(Cutscene.van(s.id()) == s && s.duur() == StapHint.KIJK_DUUR && s.pauzes().length == 0
                        && s.zinnen().size() == 1 && s.zinnen().get(0).lied(), "look scene " + s.id() + ": 3 s, one run-on caption, no click wait");
            }
            helper.assertTrue(Cutscene.van(StapHint.KIJK_ID) == StapHint.KIJK && StapHint.KIJK.zinnen().isEmpty(), "the caption-less look scene");
            // the lesson after the luchtbal match, and the scooter hint that waits for its step
            E2VerhaalDev.zetTot(p, E2Verhaal.LUCHT, E2H1Lucht.LUCHTBAL);
            E2H1Lucht.LIJN.verder(p, E2H1Lucht.LUCHTBAL);
            helper.assertTrue((E2Verhaal.LUCHT + "/" + E2H1Lucht.SCOOTER).equals(StapHint.wacht(p.getUUID())), "the scooter hint waits");
            helper.assertTrue(!E2H1Lucht.LIJN.eenmalig(p, "les_lucht"), "the airbending lesson started (once)");
            E2H1Lucht.LIJN.verder(p, E2H1Lucht.SCOOTER);
            helper.assertTrue((E2Verhaal.LUCHT + "/" + E2H1Lucht.ZWEVEN).equals(StapHint.wacht(p.getUUID())), "the next step's hint replaces it");
            // the direction phrases
            Vec3 o = new Vec3(0, 64, 0);
            check(helper, Richting.keys(o, new Vec3(100, 64, 0)), List.of("o", "ver"));
            check(helper, Richting.keys(o, new Vec3(0, 64, -12)), List.of("n", "dichtbij"));
            check(helper, Richting.keys(o, new Vec3(-50, 40, 50)), List.of("zw", "eindje", "onder"));
            check(helper, Richting.keys(o, new Vec3(30, 90, 30)), List.of("zo", "eindje", "boven"));
            check(helper, Richting.keys(o, new Vec3(2, 80, 1)), List.of("recht_boven"));
            check(helper, Richting.keys(o, new Vec3(1, 65, 2)), List.of("hier"));
            check(helper, Richting.keys(o, new Vec3(-400, 64, -390)), List.of("nw", "heel_ver"));
            helper.succeed();
        } finally {
            weg(p);
        }
    }

    private static void check(GameTestHelper helper, List<String> echt, List<String> moet) {
        helper.assertTrue(echt.equals(moet), "direction " + moet + " (got " + echt + ")");
    }
}
