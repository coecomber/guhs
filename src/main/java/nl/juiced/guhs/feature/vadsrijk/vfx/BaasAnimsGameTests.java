package nl.juiced.guhs.feature.vadsrijk.vfx;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.17: every boss model has the animations the server may trigger on it. GeckoLib crashes the CLIENT ("List.getLast" in
 * AnimationTimeline.create) when a triggered animation is missing from the model's file: the Mika-imker (vr_z3_imker_mika, no
 * "baas" in its name, so tools/features/vadsrijk_t_vfx.py skipped it) crashed every player who saw a vfx moment of his.
 */
public final class BaasAnimsGameTests {
    private static final String B = "vadsrijk_baas_anims";

    private BaasAnimsGameTests() {
    }

    private static JsonObject animaties(String model) {
        String pad = "/assets/guhs/geckolib/animations/entity/" + model + ".animation.json";
        try (InputStream in = BaasAnimsGameTests.class.getResourceAsStream(pad)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
        } catch (Exception e) {
            return null;
        }
    }

    @GuhTest(template = "empty", batch = B)
    public static void mikaImkerHeeftAlleAnimaties(GameTestHelper helper) {
        JsonObject a = animaties("vr_z3_imker_mika");
        helper.assertTrue(a != null, "the Mika-imker's animation file");
        for (String n : BaasAnims.STANDAARD) {
            helper.assertTrue(a.has(n), "Mika-imker: vfx animation " + n);
        }
        for (String n : List.of("idle", "walk", "slag", "worp", "puf", "sprong", "giet")) {
            helper.assertTrue(a.has(n), "Mika-imker: own animation " + n);
        }
        // the other zone-3 bosses and Vahoegnir (its renderer reads vr_z3_baas_vahoegnir) keep theirs too
        for (String m : List.of("vr_z3_baas_vahoegnir", "vr_z3_baas_kruinroofvogel", "vr_z3_baas_nestwachter", "vr_z3_baas_bleu")) {
            JsonObject b = animaties(m);
            helper.assertTrue(b != null && BaasAnims.STANDAARD.stream().allMatch(b::has), m + ": vfx animations");
        }
        helper.succeed();
    }
}
