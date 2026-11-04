package nl.juiced.guhs.feature.vadsrijk.kern.veilig.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;
import nl.juiced.guhs.feature.vadsrijk.kern.veilig.Veilig;
import org.lwjgl.glfw.GLFW;

/** The unstuck key "Losknuffelen" (U, rebindable, category Vadsrijk; only does something in the dimension). */
public final class VeiligClient {
    public static final KeyMapping VAST = new KeyMapping("key.guhs.vr_vast", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, KernClient.CATEGORIE);

    private VeiligClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(VAST));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean binnen = mc.player != null && Vadsrijk.in(mc.player);
            while (VAST.consumeClick()) {
                if (binnen && mc.screen == null) {
                    ClientPacketDistributor.sendToServer(new Veilig.Vast());
                }
            }
        });
    }
}
