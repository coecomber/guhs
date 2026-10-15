package nl.juiced.guhs.feature.einde2.h2water.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.h2water.E2H2Water;
import nl.juiced.guhs.feature.einde2.h2water.E2H2WaterFeature;
import nl.juiced.guhs.feature.einde2.h2water.E2H2WaterPayloads;

/**
 * einde2 h2_water, client side: the boemerang in flight (drawn as its item), and what the chapter does to how the world
 * looks for the local player ({@link E2H2WaterPayloads.Toestand}):
 * <ul>
 *   <li>grey: the moon fish was stolen: a grey veil over the screen and colourless fog, until Yue-guh gives the moon its
 *       light back;</li>
 *   <li>stormy: the blizzard on the snow plain: a white, slowly breathing veil and fog that closes in to a few blocks;</li>
 *   <li>Koh's blank face: {@link BlancoLaag}, on every player whose face he took for a while.</li>
 * </ul>
 */
public final class E2H2WaterClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(E2H2WaterFeature.BOEMERANG_VLUCHT.get(), ThrownItemRenderer::new));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerBelowAll(Guhs.id("einde2_h2_water_wereld"), E2H2WaterClient::sluier));
        modBus.addListener(E2H2WaterClient::lagen);
        modBus.addListener(E2H2WaterClient::renderStates);
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFogColor event) -> mist(event));
        NeoForge.EVENT_BUS.addListener((ViewportEvent.RenderFog event) -> mistAfstand(event));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> E2H2WaterPayloads.Client.wis());
    }

    private static int toestand() {
        return E2H2WaterPayloads.Client.toestand;
    }

    /** The veil over the screen (under the hotbar and everything else). */
    static void sluier(GuiGraphicsExtractor g, DeltaTracker delta) {
        int t = toestand();
        if (t == E2H2Water.GEWOON || Minecraft.getInstance().player == null) {
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        if (t == E2H2Water.GRIJS) {
            g.fill(0, 0, w, h, 0x46606670);
        } else if (t == E2H2Water.STORMACHTIG) {
            float tijd = Minecraft.getInstance().player.tickCount + delta.getGameTimeDeltaPartialTick(false);
            int alfa = (int) (120 + 60 * Mth.sin(tijd * 0.07f) + 25 * Mth.sin(tijd * 0.23f));
            g.fill(0, 0, w, h, (Mth.clamp(alfa, 60, 210) << 24) | 0xE8EEF6);
        }
    }

    /** Grey: the fog loses its colour. The storm: white fog. */
    static void mist(ViewportEvent.ComputeFogColor event) {
        int t = toestand();
        if (t == E2H2Water.GRIJS) {
            float l = (event.getRed() * 0.3f + event.getGreen() * 0.59f + event.getBlue() * 0.11f) * 0.85f;
            event.setRed(l);
            event.setGreen(l);
            event.setBlue(l * 1.04f);
        } else if (t == E2H2Water.STORMACHTIG) {
            event.setRed(0.86f);
            event.setGreen(0.9f);
            event.setBlue(0.95f);
        }
    }

    /** The storm: you see a few blocks far. */
    static void mistAfstand(ViewportEvent.RenderFog event) {
        if (toestand() == E2H2Water.STORMACHTIG) {
            event.setNearPlaneDistance(0f);
            event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), 14f));
        }
    }

    private static void lagen(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new BlancoLaag(renderer));
            }
        }
    }

    private static void renderStates(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (avatar instanceof Player player) {
                    BlancoLaag.extract(player, state);
                }
            }
        });
    }

    private E2H2WaterClient() {
    }
}
