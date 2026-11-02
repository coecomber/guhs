package nl.juiced.guhs.feature.erfenis.kaart.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.client.GuhKeys;
import nl.juiced.guhs.feature.erfenis.kaart.KaartPayloads;
import org.lwjgl.glfw.GLFW;

/**
 * The world map on the client: the key "Wereldkaart" (M by default; Options &gt; Controls &gt; Guhs; G, H, O, R, Z, V, B, N
 * and Alt are taken by other Guhs keys, J by JourneyMap in the dev client) and the receiver of the map data.
 */
public final class KaartClient {
    public static final KeyMapping TOETS = new KeyMapping("key.guhs.wereldkaart", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, GuhKeys.CATEGORY);

    private KaartClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(TOETS));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            while (TOETS.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null) {
                    open();
                }
            }
        });
        // no HUD under or over the map (also the JourneyMap minimap in the dev client, see ErfenisClient)
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderGuiEvent.Pre event) -> {
            if (Minecraft.getInstance().screen instanceof KaartScherm) {
                event.setCanceled(true);
            }
        });
        KaartPayloads.KLANT = kaart -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof KaartScherm s) {
                s.update(kaart);
            } else if (kaart.open()) {
                KaartScherm s = new KaartScherm(null);
                mc.setScreen(s);
                s.update(kaart);
            }
        };
    }

    /** Opens the map at once (it says "laden..." until the server's data is there). */
    public static void open() {
        open(null);
    }

    /** The same, with a "Terug" button that runs {@code terug} (the overview opens the map this way, and gets back). */
    public static void open(@Nullable Runnable terug) {
        Minecraft.getInstance().setScreen(new KaartScherm(terug));
        ClientPacketDistributor.sendToServer(new KaartPayloads.Vraag(true));
    }
}
