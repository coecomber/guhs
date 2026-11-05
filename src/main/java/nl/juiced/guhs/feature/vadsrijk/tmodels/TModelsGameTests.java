package nl.juiced.guhs.feature.vadsrijk.tmodels;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-MODELS: the generated model assets fit the code. Every bone an animation moves exists in its model, every animation the
 * Java code asks for exists (Klodderreus: main + trigger anims; the zone-1 slimes: idle/hop/blij + the new "wiebel" bone),
 * and a Klodderreus can play its trigger animations on the server without errors.
 * Run: {@code runGameTestServer -Pgt=TModelsGameTests}.
 */
public class TModelsGameTests {
    private static final String BATCH = "vadsrijk_tmodels";

    private static JsonObject json(String pad) {
        try (InputStream in = TModelsGameTests.class.getResourceAsStream("/assets/guhs/" + pad)) {
            if (in == null) {
                throw new IllegalStateException("missing " + pad);
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Bones of geo + animation names; throws if an animation moves an unknown bone. */
    private static Set<String> controleer(String model, List<String> nodig) {
        JsonObject geo = json("geckolib/models/entity/" + model + ".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        Set<String> bones = new HashSet<>();
        geo.getAsJsonArray("bones").forEach(b -> bones.add(b.getAsJsonObject().get("name").getAsString()));
        JsonObject anims = json("geckolib/animations/entity/" + model + ".animation.json").getAsJsonObject("animations");
        for (String a : anims.keySet()) {
            for (String b : anims.getAsJsonObject(a).getAsJsonObject("bones").keySet()) {
                if (!bones.contains(b)) {
                    throw new IllegalStateException(model + ": animation " + a + " moves unknown bone " + b);
                }
            }
        }
        for (String n : nodig) {
            if (!anims.has(n)) {
                throw new IllegalStateException(model + ": missing animation " + n);
            }
        }
        return bones;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void modellenPassenBijDeCode(GameTestHelper helper) {
        controleer("vadsrijk/tmodels/grauwe_klodderreus", List.of("idle", "sprong", "land", "rol", "blij", "spuug", "brul", "aanloop"));
        for (String s : List.of("klodder", "bubbelklodder", "stuiterklodder", "moederklodder", "druipslijm", "kristalklodder", "kampklodder")) {
            Set<String> bones = controleer("vadsrijk/wezens/" + s, List.of("idle", "hop", "blij"));
            if (!bones.containsAll(Set.of("lijf", "oor_l", "oor_r", "wiebel"))) {
                throw new IllegalStateException(s + ": bones lijf/oor_l/oor_r/wiebel expected, got " + bones);
            }
        }
        for (String s : List.of("hoedjesbol", "sporenpuffer", "zwamwachter", "glimzwam", "tempelridder", "sporenpriester", "mosgeest", "knaag")) {
            controleer("vadsrijk/wezens/" + s, List.of("idle", "blij"));
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void reusSpeeltZijnActies(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Klodderreus k = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.COMMAND);
        BlockPos p = helper.absolutePos(new BlockPos(2, 1, 2));
        k.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        k.stelIn(3, Element.WATER, false, Set.of());
        k.setNoAi(true);
        sl.addFreshEntity(k);
        for (String a : KlodderGeo.ACTIES) {
            KlodderGeo.speel(k, a);
        }
        if (!k.isAlive()) {
            throw new IllegalStateException("the giant is gone");
        }
        helper.succeed();
    }
}
