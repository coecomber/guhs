package nl.juiced.guhs.feature.vadsrijk.zone5.wereld;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-Z5 (batch vadsrijk_z5bewoners): the townsfolk of Hoogvads render as proper guhs. Every skin of {@link Z5WereldPlekken#BEWONERS}
 * goes on the model it was made for (guh fur on the guh model, npc_* on its guh_npc_* model with the sitting guh's animations, the
 * konijntje on its own model) and that model, texture and animation file exist. (The client renderer hides the saddle/armour/outfit
 * bones of the guh model: Z3BazenClient.FigurantRenderer.)
 */
public class Z5BewonersGameTests {
    private static final String B = "vadsrijk_z5bewoners";

    private static boolean bestaat(String pad) {
        return Z5BewonersGameTests.class.getClassLoader().getResource("assets/guhs/" + pad) != null;
    }

    @GuhTest(template = "empty", batch = B)
    public static void vormen(GameTestHelper helper) {
        Set<String> gezien = new HashSet<>();
        int n = 0;
        for (Z5WereldPlekken.Bewoner b : Z5WereldPlekken.BEWONERS) {
            if (b.soort() == 2 || !gezien.add(b.skin() + "/" + b.soort())) {
                continue;
            }
            String[] m = Z5Bewoners.vorm(b);
            helper.assertTrue(bestaat("geckolib/models/entity/" + m[0] + ".geo.json"), "model " + m[0] + " for " + b.skin());
            helper.assertTrue(bestaat(m[1]), "texture " + m[1]);
            helper.assertTrue(bestaat("geckolib/animations/entity/" + m[2] + ".animation.json"), "animations " + m[2] + " for " + b.skin());
            helper.assertTrue(!m[1].contains("uiltje"), "the uiltje is a bird, not guh fur: " + b.skin());
            helper.assertTrue(!m[0].equals("guh") || (m[1].startsWith("textures/entity/guh") && !m[1].contains("konijntje")),
                    "only guh fur on the guh model: " + b.skin());
            n++;
        }
        helper.assertTrue(n > 5, "several kinds of townsfolk: " + n);
        helper.succeed();
    }
}
