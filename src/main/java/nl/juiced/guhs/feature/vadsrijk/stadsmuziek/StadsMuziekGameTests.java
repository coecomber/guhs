package nl.juiced.guhs.feature.vadsrijk.stadsmuziek;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * I-AUDIO: the area to town-music mapping only (the sound itself is a listen check for the user).
 * Run: {@code runGameTestServer -Pgt=StadsMuziek}.
 */
public class StadsMuziekGameTests {
    private static final String BATCH = "vadsrijk_stadsmuziek";

    @GuhTest(template = "empty", batch = BATCH)
    public static void gebiedNaarDeuntje(GameTestHelper helper) {
        BlockPos plein = WereldPlekken.ALLE.get("plein"), haven = WereldPlekken.ALLE.get("haven_plein"), kamp = WereldPlekken.AANKOMST;
        verwacht(helper, plein.getX(), plein.getZ(), "zwamzoet");
        verwacht(helper, haven.getX(), haven.getZ(), "haven");
        verwacht(helper, kamp.getX(), kamp.getZ(), "knuffelkamp");
        BlockPos kplein = Z2WereldPlekken.ALLE.get("plein");
        verwacht(helper, kplein.getX(), kplein.getZ(), "kandijkom");
        verwacht(helper, Z2WereldPlekken.DOK_LANDING.getX(), Z2WereldPlekken.DOK_LANDING.getZ(), "kandijkom");
        // the wilderness stays ambient only
        verwacht(helper, 380, 0, null);       // Klodderweide
        verwacht(helper, -380, 100, null);    // Sporenwoud
        verwacht(helper, -380, -380, null);   // Druipgrotten
        verwacht(helper, -400, -2500, null);  // Knuffelmeer
        helper.assertTrue(StadsMuziek.stadVan("z2_kandijkom_dok").equals("kandijkom") && StadsMuziek.stadVan("zwamtempel") == null
                && StadsMuziek.stadVan(null) == null, "stadVan");
        // two tunes per town, each a registered sound event, and every town has a bed and day sounds
        for (String stad : StadsMuziek.STEDEN) {
            var stukken = StadsMuziek.muziekVan(stad);
            helper.assertTrue(stukken.size() >= 2, stad + ": fewer than two tunes");
            for (var h : stukken) {
                helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(h.getId()), "not registered: " + h.getId());
            }
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(StadsMuziek.BED.get(stad).getId()), stad + ": no bed");
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(StadsMuziek.SFEER.get(stad).getId()), stad + ": no sfeer");
        }
        helper.assertTrue(StadsMuziek.muziekVan(null).isEmpty(), "music outside a town");
        helper.succeed();
    }

    private static void verwacht(GameTestHelper helper, double x, double z, String stad) {
        String echt = StadsMuziek.stadOp(x, z);
        helper.assertTrue(stad == null ? echt == null : stad.equals(echt), "at " + (int) x + "," + (int) z + ": expected " + stad + ", got " + echt);
    }
}
