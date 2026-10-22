package nl.juiced.guhs.feature.safari;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.MapColor;
import nl.juiced.guhs.gametest.GuhTest;

/** Test round 5 (FX-A): the kaartwand's data is in the jar, every map decodes and is (almost) all colour. */
public class SafariKaartwandGameTests {
    @GuhTest(template = "empty", batch = "safari_kaartwand")
    public static void kaartwandKaartenZijnGevuld(GameTestHelper helper) {
        JsonObject data = Kaartwand.lees();
        helper.assertTrue(data != null, "kaartwand.json is in the jar");
        JsonArray kaarten = data.getAsJsonArray("kaarten");
        helper.assertTrue(kaarten.size() == 36, "6 x 6 maps: " + kaarten.size());
        JsonArray palet = data.getAsJsonArray("palet");
        int geen = 0;
        for (int i = 0; i < palet.size(); i++) {
            if (Kaartwand.kleur(palet.get(i).getAsString()) == MapColor.NONE) {
                geen++;
            }
        }
        helper.assertTrue(geen <= 1, "every palette block has a map colour (only air may not): " + geen);
        for (int k = 0; k < kaarten.size(); k++) {
            byte[] pix = Kaartwand.pixels(kaarten.get(k).getAsJsonObject().get("pixels").getAsString());
            int leeg = 0;
            for (int i = 0; i < 128 * 128; i++) {
                int w = (pix[2 * i] & 0xFF) | (pix[2 * i + 1] & 0xFF) << 8;
                if (Kaartwand.kleur(palet.get(w >> 2).getAsString()) == MapColor.NONE) {
                    leeg++;
                }
            }
            helper.assertTrue(leeg < 128 * 128 / 50, "map " + k + " is drawn (" + leeg + " empty pixels)");
        }
        helper.succeed();
    }
}
