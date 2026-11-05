package nl.juiced.guhs.feature.vadsrijk.opslag.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;
import nl.juiced.guhs.feature.vadsrijk.opslag.MateriaalMenu;
import nl.juiced.guhs.feature.vadsrijk.opslag.OpslagPayloads;
import nl.juiced.guhs.feature.vadsrijk.opslag.OpslagRegistry;

/**
 * T-OPSLAG, the client: key I (knapzak, rebindable, category Vadsrijk), the two screens, two small buttons left of the vanilla
 * inventory in the Vadsrijk (Knapzak / Materiaaltas), and the HUD additions through {@link RijkHud#voegToe}.
 */
public final class OpslagClient {
    public static final KeyMapping KNAPZAK = new KeyMapping("key.guhs.vr_knapzak", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, KernClient.CATEGORIE);

    private OpslagClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KNAPZAK));
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            e.register(OpslagRegistry.OPSLAG_MENU.get(), OpslagScherm::new);
            e.register(OpslagRegistry.TAS_MENU.get(), MateriaalScherm::new);
        });
        OpslagPayloads.tasOntvanger = t -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.containerMenu instanceof MateriaalMenu m && m.containerId == t.containerId()) {
                m.zetInhoud(t.inhoud(), t.auto());
            }
        };
        OpslagPayloads.hudOntvanger = OpslagHud::ontvang;
        OpslagPayloads.gietOntvanger = OpslagHud::giet;
        RijkHud.voegToe("t_opslag_buffs", 100, OpslagHud::tekenBuffs);
        RijkHud.voegToe("t_opslag_doel", 110, OpslagHud::tekenDoel);
        RijkHud.voegToe("t_opslag_giet", 120, OpslagHud::tekenGiet);
        RijkHud.BOVEN_EXTRA = OpslagHud::bovenExtra;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            OpslagHud.tick();
            Minecraft mc = Minecraft.getInstance();
            boolean binnen = mc.player != null && Vadsrijk.in(mc.player);
            while (KNAPZAK.consumeClick()) {
                if (binnen && mc.screen == null) {
                    ClientPacketDistributor.sendToServer(new OpslagPayloads.Open(0));
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> OpslagHud.wis());
        // two small buttons left of the inventory screen, only in the Vadsrijk
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!(e.getScreen() instanceof InventoryScreen s) || mc.player == null || !Vadsrijk.in(mc.player) || mc.player.isCreative()) {
                return;
            }
            int x = s.getGuiLeft() - 24, y = s.getGuiTop() + 4;
            e.addListener(Button.builder(Component.literal("▤"), b -> ClientPacketDistributor.sendToServer(new OpslagPayloads.Open(0)))
                    .bounds(x, y, 20, 20).tooltip(Tooltip.create(Component.translatable("vadsrijk.opslag.knop.knapzak.uitleg"))).build());
            e.addListener(Button.builder(Component.literal("✿"), b -> ClientPacketDistributor.sendToServer(new OpslagPayloads.Open(1)))
                    .bounds(x, y + 22, 20, 20).tooltip(Tooltip.create(Component.translatable("vadsrijk.opslag.knop.tas.uitleg"))).build());
        });
    }
}
