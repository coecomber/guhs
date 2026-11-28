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

    /** Type id -> animation file, where the two differ (the rest: the file is named after the type id). */
    private static final java.util.Map<String, String> AFWIJKEND = java.util.Map.of(
            "vr_z5_baas_donderbuik", "vadsrijk/zone5/wezens/donderbuik",
            "vr_z5_baas_gorgonzola", "vadsrijk/zone5/wezens/gorgonzola",
            "vr_z5_baas_zilverstaart", "vadsrijk/zone5/wezens/zilverstaart",
            "vr_z5_baas_grauwe_droom", "vadsrijk/zone5/droom/grauwe_droom");

    /**
     * 1.10.26: EVERY registered guhs entity with the "vfx" controller (not a fixed list) has the six vfx animations in its
     * animation file. The zone-5 bosses (files under vadsrijk/zone5/, skipped by the tool's vr_*baas_* glob) crashed every
     * client that hit one. A new boss whose file is not named after its type id: add it to AFWIJKEND and to EXTRA in
     * tools/features/vadsrijk_t_vfx.py.
     */
    @GuhTest(template = "empty", batch = B)
    public static void elkeBaasMetVfxControllerHeeftDeAnimaties(GameTestHelper helper) {
        java.util.List<String> fout = new java.util.ArrayList<>();
        int n = 0;
        for (var type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!"guhs".equals(id.getNamespace()) || !id.getPath().startsWith("vr_")) {
                continue;
            }
            net.minecraft.world.entity.Entity e;
            try {
                e = type.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            } catch (RuntimeException ex) {
                continue;
            }
            if (!(e instanceof com.geckolib.animatable.GeoEntity g) || e instanceof nl.juiced.guhs.feature.vadsrijk.bazen.mika.MikaSpion) {
                continue;   // (the Mika spy uses the shared guh animations: BaasAnims.speel skips it)
            }
            boolean vfx;
            try {
                vfx = g.getAnimatableInstanceCache().getManagerForId(e.getId()).getAnimationControllers().containsKey(BaasAnims.CONTROLLER);
            } catch (RuntimeException ex) {
                continue;
            }
            if (!vfx) {
                continue;
            }
            n++;
            String model = AFWIJKEND.getOrDefault(id.getPath(), id.getPath());
            JsonObject a = animaties(model);
            if (a == null) {
                fout.add(id.getPath() + ": no animation file " + model);
            } else {
                for (String naam : BaasAnims.STANDAARD) {
                    if (!a.has(naam)) {
                        fout.add(id.getPath() + ": " + naam + " missing in " + model);
                    }
                }
            }
        }
        helper.assertTrue(n >= 20, "bosses with a vfx controller found: " + n);
        helper.assertTrue(fout.isEmpty(), "boss vfx animations: " + fout);
        helper.succeed();
    }
}
