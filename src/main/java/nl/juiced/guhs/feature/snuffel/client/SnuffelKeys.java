package nl.juiced.guhs.feature.snuffel.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import nl.juiced.guhs.client.GuhKeys;
import org.lwjgl.glfw.GLFW;

/**
 * The keys of a dog on Het Snuffeleiland (Options &gt; Controls &gt; Guhs): sniff (hold, R), sit (Z), wag (V), bark (B) and
 * the snuffelboekje (N). Digging is the attack button. They only do something while you are a dog.
 */
public final class SnuffelKeys {
    public static final KeyMapping SNUFFEL = new KeyMapping("key.guhs.snuffel.snuffel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, GuhKeys.CATEGORY);
    public static final KeyMapping ZIT = new KeyMapping("key.guhs.snuffel.zit", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, GuhKeys.CATEGORY);
    public static final KeyMapping KWISPEL = new KeyMapping("key.guhs.snuffel.kwispel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, GuhKeys.CATEGORY);
    public static final KeyMapping BLAF = new KeyMapping("key.guhs.snuffel.blaf", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, GuhKeys.CATEGORY);
    public static final KeyMapping BOEKJE = new KeyMapping("key.guhs.snuffel.boekje", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, GuhKeys.CATEGORY);
    private static final KeyMapping[] ALLE = {SNUFFEL, ZIT, KWISPEL, BLAF, BOEKJE};

    private SnuffelKeys() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        for (KeyMapping k : ALLE) {
            event.register(k);
        }
    }

    /** Presses that came while they mean nothing are thrown away (so they don't fire later). */
    static void leeg() {
        for (KeyMapping k : ALLE) {
            while (k.consumeClick()) {
                // (drained)
            }
        }
    }
}
