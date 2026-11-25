package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensPlekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.22 small data fixes (run: {@code -Pgt=VadsrijkKleineData}): the secret IJsgrot's entrance spot is its roof hole (on top of the
 * dome, not the old tunnel mouth above the lake); the Schuilnest's inside spots are on the map and on the real floor of the carved
 * room; the mount quest with the old id "rijdier_hoedhupper" is the Wolkenschaapje quest (kept on purpose: saves, advancements).
 */
public class VadsrijkKleineDataGameTests {
    private static final String B = "vadsrijk_kleinedata";

    @GuhTest(template = "empty", batch = B)
    public static void ijsgrotIngangIsHetDakgat(GameTestHelper helper) {
        BlockPos grot = Z2WereldPlekken.plek("geheim_ijsgrot"), ingang = Z2WereldPlekken.plek("geheim_ijsgrot_ingang");
        helper.assertTrue(grot != null && ingang != null, "both IJsgrot spots are on the zone-2 map");
        helper.assertTrue(Math.abs(ingang.getX() - grot.getX()) <= 3 && Math.abs(ingang.getZ() - grot.getZ()) <= 3,
                "the entrance is on the dome (next to the roof hole), not the tunnel mouth west of it: " + ingang.toShortString());
        helper.assertTrue(ingang.getY() >= grot.getY() + 8, "the entrance stands on top of the dome: " + ingang.toShortString());
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void schuilnestOpDeVloer(GameTestHelper helper) {
        BlockPos min = Z5WereldPlekken.plek("z5w_schuilnest_min"), max = Z5WereldPlekken.plek("z5w_schuilnest_max");
        helper.assertTrue(min != null && max != null, "the Schuilnest room box is on the zone-5 map");
        for (String id : List.of("z5w_schuilnest", "z5w_schuilnest_aankomst", "z5w_schuilnest_uitluik", "z5w_leider_meestervermommer")) {
            BlockPos b = Z5WereldPlekken.plek(id);
            helper.assertTrue(b != null, id + " is on the zone-5 map (no fallback number)");
            helper.assertTrue(b.getX() >= min.getX() && b.getX() <= max.getX() && b.getZ() >= min.getZ() && b.getZ() <= max.getZ(),
                    id + " lies inside the room: " + b.toShortString());
            helper.assertTrue(b.getY() == min.getY(), id + " stands on the room's floor (y " + min.getY() + "): " + b.toShortString());
        }
        helper.assertTrue(Z5WezensPlekken.NEST_Y == min.getY(), "NEST_Y is the room's stand height: " + Z5WezensPlekken.NEST_Y);
        helper.assertTrue(Z5WezensPlekken.SCHUILNEST.equals(Z5WereldPlekken.plek("z5w_schuilnest"))
                && Z5WezensPlekken.NEST_AANKOMST.equals(Z5WereldPlekken.plek("z5w_schuilnest_aankomst")), "the combat slice uses the map spots");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void wolkenschaapjeQuestMetOudeId(GameTestHelper helper) {
        Quest q = Quests.van("rijdier_hoedhupper");
        helper.assertTrue(q != null && q.actieKlaar().equals("rijdier:wolkenschaapje"), "the old id is the Wolkenschaapje return quest");
        Language t = Language.getInstance();
        String titel = t.getOrDefault("vadsrijk.npc.q.rijdier_hoedhupper.titel", "");
        helper.assertTrue(!titel.isEmpty() && !titel.toLowerCase().contains("hoedhupper") && !titel.toLowerCase().contains("hat hopper"),
                "its title names the sheep: " + titel);
        String hint = t.getOrDefault("vadsrijk.puzzels.p.z1_sterrenveld.aanwijzing", "").toLowerCase();
        helper.assertTrue(!hint.isEmpty() && !hint.contains("hoedhupper") && !hint.contains("hat hopper"), "no Hoedhupper in the star field clue: " + hint);
        helper.succeed();
    }
}
