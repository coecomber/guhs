package nl.juiced.guhs.feature.knabbelspelen.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.knabbelspelen.KnabbelspelenFeature;
import nl.juiced.guhs.feature.knabbelspelen.KnabbelspelenPayloads;

/**
 * Client side of De Knabbelspelen: the moving things ({@link DingRenderer}), Juf Vahoegsakee's own model (a sweatband,
 * a whistle on a cord and a clipboard; the whistle bobs when she talks), her screen ({@link SpelleiderScherm}) and the
 * blindfold of Guhguhtje prik (a dark pink cloth over the whole screen).
 */
public final class KnabbelspelenClient {
    private static boolean blinddoek;
    private static long blinddoekSinds;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(KnabbelspelenFeature.DING.get(), DingRenderer::new));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Guhs.id("knabbelspelen_blinddoek"),
                KnabbelspelenClient::renderBlinddoek));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> blinddoek = false);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.SPELLEIDERGUH, Guhs.id("geo/entity/guh_npc_spelleiderguh.geo.json"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.SPELLEIDERGUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.12f;
            bot.apply("vahoegsakee_fluitje").ifPresent(b -> b.setRotX((float) Math.sin(t) * 0.12f));
            bot.apply("vahoegsakee_klembord").ifPresent(b -> b.setRotZ((float) Math.sin(t * 0.5f) * 0.04f));
        });
    }

    /** guhs:knabbelspelen_open: Juf Vahoegsakee's screen. */
    public static void open(KnabbelspelenPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new SpelleiderScherm(payload.npcId(), payload.data()));
    }

    /** guhs:knabbelspelen_blinddoek. */
    public static void blinddoek(boolean aan) {
        blinddoek = aan;
        blinddoekSinds = System.currentTimeMillis();
    }

    private static void renderBlinddoek(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!blinddoek || mc.player == null) {
            return;
        }
        if (System.currentTimeMillis() - blinddoekSinds > 120_000) {
            blinddoek = false;                                      // (never stuck blind: the game lasts far shorter)
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        g.fill(0, 0, w, h, 0xF4180A16);
        // the cloth: soft pink folds, a knot at the side
        for (int i = 0; i < 6; i++) {
            int y = h / 8 + i * h / 8;
            g.fill(0, y, w, y + 2, 0x40F6A6CC);
        }
        g.fill(0, 0, w, 6, 0xFFE889B4);
        g.fill(0, h - 6, w, h, 0xFFE889B4);
        g.fill(w - 34, h / 2 - 14, w - 10, h / 2 + 14, 0xFFD06A9C);
        g.fill(w - 28, h / 2 - 8, w - 16, h / 2 + 8, 0xFFE889B4);
        g.drawCenteredString(mc.font, Component.translatable("gui.guhs.knabbelspelen.blinddoek"), w / 2, h - 40, 0xFFF6C4DC);
    }

    private KnabbelspelenClient() {
    }
}
