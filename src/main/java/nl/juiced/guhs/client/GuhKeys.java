package nl.juiced.guhs.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.MaagPayloads;
import org.lwjgl.glfw.GLFW;

/** The key to your own guh stomach (G by default; change it in Options > Controls > Guhs). */
@EventBusSubscriber(modid = Guhs.MODID, value = Dist.CLIENT)
public final class GuhKeys {
    public static final KeyMapping MAAG = new KeyMapping("key.guhs.maag", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.guhs");

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(MAAG);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (MAAG.consumeClick()) {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) {
                PacketDistributor.sendToServer(new MaagPayloads.MaagKey());
            }
        }
    }

    private GuhKeys() {
    }
}
