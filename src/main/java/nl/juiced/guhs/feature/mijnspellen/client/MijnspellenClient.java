package nl.juiced.guhs.feature.mijnspellen.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesFeature;
import nl.juiced.guhs.feature.mijnspellen.kegelgrot.KegelgrotFeature;
import nl.juiced.guhs.feature.mijnspellen.kegelgrot.WolbalItem;

/**
 * Client side of the cave minigames: the ore carts (the vanilla minecart renderer, with their load), the pins, the wool
 * ball and the Mika-blokje ({@link KegelRenderer}), the models of Conducteur Piepwiel and De Guhde, and the power bar
 * while you hold the wool ball.
 */
public final class MijnspellenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(KarretjesFeature.KAR.get(), context -> new MinecartRenderer(context, ModelLayers.MINECART));
            event.registerEntityRenderer(KegelgrotFeature.KEGEL.get(), KegelRenderer::new);
            event.registerEntityRenderer(KegelgrotFeature.BAL.get(), KegelRenderer::new);
            event.registerEntityRenderer(KegelgrotFeature.SCHUIF.get(), KegelRenderer::new);
        });
        modBus.addListener(MijnspellenClient::registerGui);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.WISSELGUH, Guhs.id("entity/guh_npc_wisselguh"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.KEGELGUH, Guhs.id("entity/guh_npc_kegelguh"));
    }

    private static void registerGui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("kegelgrot_kracht"), MijnspellenClient::renderKracht);
    }

    private static int holdTicks(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof WolbalItem ? player.getTicksUsingItem() : -1;
    }

    private static void renderKracht(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        int ticks = holdTicks(mc.player);
        if (ticks < 0) {
            return;
        }
        float kracht = WolbalItem.kracht(ticks);
        int w = 120, h = 8, x = (g.guiWidth() - w) / 2, y = g.guiHeight() - 92;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFFF7B6CB);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2A1A30);
        int filled = Math.round(w * kracht);
        for (int i = 0; i < filled; i++) {                       // pink -> magenta
            float t = i / (float) w;
            int r = (int) (255 - t * 30), gr = (int) (190 - t * 140), b = (int) (220 - t * 20);
            g.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | r << 16 | gr << 8 | b);
        }
        boolean recht = mc.player.isShiftKeyDown();
        Component label = Component.translatable(recht ? "gui.guhs.kegelen.kracht_recht" : "gui.guhs.kegelen.kracht", Math.round(kracht * 100));
        g.centeredText(mc.font, label, g.guiWidth() / 2, y - 11, kracht >= 1f ? 0xFFFFD27A : 0xFFFFE6EE);
    }

    private MijnspellenClient() {
    }
}
