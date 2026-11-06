package nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;
import nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-WOLKWAL (batch vadsrijk_wolkwal): the knight model fits the guh animations (all core bones) and the Sparringridder's bones;
 * which guhs are the Wolkwacht and in which fur; a knight prop spawns; the moving tips sit on the whale and the spout timer;
 * the two panoramas have camera keyframes and trigger at their spots; the enterable upper floors are on the map, above their
 * ground floors.
 */
public class WolkwalGameTests {
    private static final String B = "vadsrijk_wolkwal";
    static final String GEO = "assets/guhs/geckolib/models/entity/vadsrijk/zone5/wolkwal/";

    private static JsonObject json(String pad) {
        try (InputStream in = WolkwalGameTests.class.getClassLoader().getResourceAsStream(pad)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static Set<String> botten(String model) {
        Set<String> s = new HashSet<>();
        JsonObject j = json(GEO + model + ".geo.json");
        if (j != null) {
            for (JsonElement b : j.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
                s.add(b.getAsJsonObject().get("name").getAsString());
            }
        }
        return s;
    }

    @GuhTest(template = "empty", batch = B)
    public static void riddermodel(GameTestHelper helper) {
        Set<String> r = botten("wolkwacht_ridder");
        for (String b : List.of("root", "body", "head", "tail", "ear_left", "ear_right", "leg_front_left", "leg_front_right", "leg_back_left",
                "leg_back_right", "helm", "pluim", "kuras", "schild", "zwaard", "kraag", "cape")) {
            helper.assertTrue(r.contains(b), "knight model has bone " + b);
        }
        // every bone the guh's idle/walk/sit animations move exists (else GeckoLib skips it: the knight would stand still)
        JsonObject g = json("assets/guhs/geckolib/animations/entity/guh.animation.json");
        helper.assertTrue(g != null, "guh animations readable");
        for (String a : List.of("animation.guh.idle", "animation.guh.walk")) {
            for (String b : g.getAsJsonObject("animations").getAsJsonObject(a).getAsJsonObject("bones").keySet()) {
                helper.assertTrue(r.contains(b), a + " moves " + b + ": in the knight model");
            }
        }
        JsonObject eigen = json("assets/guhs/geckolib/animations/entity/" + Wolkwacht.ANIM + ".animation.json");
        helper.assertTrue(eigen != null && eigen.getAsJsonObject("animations").has("animation.guh.idle")
                && eigen.getAsJsonObject("animations").has("animation.guh.sit"), "knight animation file (idle + sit)");
        for (String v : Wolkwacht.VACHTEN) {
            helper.assertTrue(WolkwalGameTests.class.getClassLoader().getResource("assets/guhs/" + Wolkwacht.textuur(v)) != null, "texture " + v);
        }
        // the Sparringridder version keeps t-z5wezens' bones (their animations: idle/loop/aanval/mik/blok/buig/blij)
        Set<String> s = botten("sparringridder");
        for (String b : List.of("root", "lijf", "helm", "arm_l", "schild", "arm_r", "wapen", "head", "body")) {
            helper.assertTrue(s.contains(b), "sparringridder has bone " + b);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void wieIsWolkwacht(GameTestHelper helper) {
        helper.assertTrue(Wolkwacht.isWolkwacht(GuhClothes.KNIGHT_HELMET, GuhClothes.WOLKENKRAAG), "helmet + cloud collar = Wolkwacht");
        helper.assertFalse(Wolkwacht.isWolkwacht(GuhClothes.KNIGHT_HELMET, null), "a helmet alone is not");
        helper.assertFalse(Wolkwacht.isWolkwacht(null, GuhClothes.WOLKENKRAAG), "a collar alone is not");
        helper.assertTrue(Wolkwacht.vacht(GuhVariant.SNOW).equals("guh_snow") && Wolkwacht.vacht(GuhVariant.NORMAL).equals("guh"), "furs");
        helper.assertTrue(Wolkwacht.vacht(GuhVariant.GHOST).equals("guh"), "an unknown fur: the plain guh");
        helper.assertTrue(Wolkwacht.textuur("x").endsWith("wolkwacht_ridder_guh.png"), "unknown fur texture");
        Set<String> n = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            n.add(Wolkwacht.vacht(i));
        }
        helper.assertTrue(n.size() >= 4, "a formation has varied furs: " + n);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void ridderProp(GameTestHelper helper) {
        Vec3 v = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        Z3Figurant f = Wolkwacht.prop(helper.getLevel(), v, 90, "guh_golden", false, 1f);
        helper.assertTrue(f != null && !f.isRemoved(), "the knight prop spawns");
        helper.assertTrue(f.model().equals(Wolkwacht.MODEL) && f.textuur().contains("guh_golden") && f.anim().equals("animation.guh.idle"),
                "knight model, golden fur, standing guard");
        helper.assertFalse(f.isPickable(), "no hitbox");
        f.discard();
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void bewegendeWalvis(GameTestHelper helper) {
        helper.assertTrue(WolkwalBeweging.STUKKEN.size() == 4, "2 fin tips + 2 fluke tips");
        for (WolkwalBeweging.Stuk s : WolkwalBeweging.STUKKEN) {
            Vec3 p = s.pos();
            helper.assertTrue(p.x >= Z5WereldPlekken.MIN_X - 20 && p.x <= Z5WereldPlekken.MAX_X + 20 && p.z >= Z5WereldPlekken.MIN_Z - 20
                    && p.z <= Z5WereldPlekken.MAX_Z + 20, s.id() + " lies at the whale");
            helper.assertTrue(p.y > 120 && p.y < 240, s.id() + " height " + p.y);
            Vec3 e = s.eind();
            helper.assertTrue(e.distanceTo(p) > s.lengte() - 0.5, s.id() + " points outward");
            // the walkable cloud platforms of the fins/fluke stay clear of the moving parts (at least 25 blocks)
            for (String pl : List.of("z5w_zuidvin", "z5w_noordvin", "z5w_staartvlakte_noord", "z5w_donderbuik_arena")) {
                BlockPos b = Z5WereldPlekken.plek(pl);
                if (b != null) {
                    helper.assertTrue(Vec3.atCenterOf(b).distanceTo(p) > 25, s.id() + " is not on the platform " + pl);
                }
            }
        }
        long t = helper.getLevel().getGameTime();
        WolkwalBeweging.spuit(helper.getLevel());
        helper.assertTrue(WolkwalBeweging.spuitNu(t + 5) && !WolkwalBeweging.spuitNu(t + WolkwalBeweging.SPUIT_DUUR + 5), "spout timer");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void panoramas(GameTestHelper helper) {
        Language t = Language.getInstance();
        for (WolkwalUitzicht.Uitzicht u : WolkwalUitzicht.UITZICHTEN) {
            List<RijkScene.Punt> pt = WolkwalUitzicht.punten(u);
            helper.assertTrue(pt.size() >= 4, u.id() + " has camera keyframes");
            for (int i = 1; i < pt.size(); i++) {
                helper.assertTrue(pt.get(i).t() > pt.get(i - 1).t(), u.id() + " keyframes in order");
            }
            RijkScene s = WolkwalUitzicht.scene(u);
            helper.assertTrue(s.duur() >= 150 && s.duur() <= 240, u.id() + " is short: " + s.duur());
            helper.assertTrue(t.has(s.titelKey()) && t.has(s.ondertitelKey()), u.id() + " caption texts");
            BlockPos b = Z5WereldPlekken.plek(u.plekken().get(0));
            helper.assertTrue(b != null && WolkwalUitzicht.bij(Vec3.atBottomCenterOf(b)) == u, u.id() + " triggers at " + u.plekken().get(0));
        }
        helper.assertTrue(WolkwalUitzicht.bij(new Vec3(2700, 225, -520)) == null, "not in the middle of the city");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void bovenverdiepingen(GameTestHelper helper) {
        for (String h : WolkwalRegistry.BOVEN) {
            BlockPos boven = Z5WereldPlekken.plek(h + "_boven");
            helper.assertTrue(boven != null, h + " has an upper floor on the map");
            Z5WereldPlekken.Huis huis = Z5WereldPlekken.HUIZEN.stream().filter(x -> x.id().equals(h)).findFirst().orElse(null);
            helper.assertTrue(huis != null, h + " is a house");
            int dy = boven.getY() - huis.binnen().getY();
            helper.assertTrue(dy >= 4 && dy <= 6, h + ": the upper floor is one storey up (" + dy + ")");
            helper.assertTrue(Math.abs(boven.getX() - huis.binnen().getX()) <= 12 && Math.abs(boven.getZ() - huis.binnen().getZ()) <= 12,
                    h + ": in the same house");
        }
        helper.succeed();
    }
}
