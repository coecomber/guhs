package nl.juiced.guhs.feature.erfenis.client;

import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.erfenis.kaart.client.KaartClient;
import org.lwjgl.glfw.GLFW;

/**
 * 1.9.2: the Erfenis keys moved from O / M to F9 / F10 (they took over players' own O and M). Minecraft keeps every
 * key in options.txt, so a new default alone changes nothing for someone who already played 1.9.0/1.9.1. Once per
 * client (marker file in config/), a key still sitting on the old plain O / M moves to its new default; a key the
 * player chose themselves is left alone, and so is anything they set after this ran.
 */
public final class ToetsVerhuizing {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String MARKER = "guhs-toetsen-1.9.2.done";
    private static boolean klaar;

    public static void init() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            if (!klaar) {
                klaar = true;
                verhuis();
            }
        });
    }

    private static void verhuis() {
        try {
            Path marker = FMLPaths.CONFIGDIR.get().resolve(MARKER);
            if (Files.exists(marker)) {
                return;
            }
            boolean veranderd = zet(ErfenisClient.TOETS, GLFW.GLFW_KEY_O) | zet(KaartClient.TOETS, GLFW.GLFW_KEY_M);
            if (veranderd) {
                KeyMapping.resetMapping();
                Minecraft.getInstance().options.save();
            }
            Files.writeString(marker, "Guhs 1.9.2: Erfenis keys moved from O/M to F9/F10 once. Delete this file to run it again.\n");
        } catch (Exception e) {
            LOGGER.warn("Guhs: could not move the Erfenis keys to F9/F10", e);
        }
    }

    private static boolean zet(KeyMapping toets, int oud) {
        if (toets.getKeyModifier() != KeyModifier.NONE || !toets.getKey().equals(InputConstants.Type.KEYSYM.getOrCreate(oud))) {
            return false;
        }
        toets.setKeyModifierAndCode(KeyModifier.NONE, toets.getDefaultKey());
        return true;
    }

    private ToetsVerhuizing() {
    }
}
