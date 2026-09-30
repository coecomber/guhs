package nl.juiced.guhs.feature.guhwaiispellen.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhwaiispellen.GuhwaiiSpellenBlocks;
import nl.juiced.guhs.feature.guhwaiispellen.GuhwaiiSpellenPayloads;

/**
 * Client side of the surf beach of Guhwai'i (3.0): the surf board (and Lilo-guh on hers), the waves ({@link GolfRenderer}),
 * your own ride ({@link SurfClient}: it runs the ride itself, the panel, the camera), the hula dance ({@link HulaClient}:
 * the steps sliding in, your keys, Lilo-guh swaying on the beat), Lilo-guh's screen ({@link SpelScherm}) and Tikiguh's own
 * model (a guh behind a big carved tiki mask).
 */
public final class GuhwaiiSpellenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(GuhwaiiSpellenBlocks.SURFPLANK.get(), SurfPlankRenderer::new));
        modBus.addListener((RegisterGuiLayersEvent event) -> {
            event.registerAboveAll(Guhs.id("guhwaiispellen_surf"), SurfClient::hud);
            event.registerAboveAll(Guhs.id("guhwaiispellen_hula"), HulaClient::hud);
        });
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.TIKIGUH, Guhs.id("geo/entity/guh_npc_tikiguh.geo.json"));
        HulaClient.liloDanst();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            SurfClient.tick();
            HulaClient.tick();
        });
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                GolfRenderer.render(event);
            }
        });
        NeoForge.EVENT_BUS.addListener((InputEvent.Key event) -> HulaClient.toets(event.getKey(), event.getScanCode(), event.getAction()));
        NeoForge.EVENT_BUS.addListener((MovementInputUpdateEvent event) -> HulaClient.stilStaan(event));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            SurfClient.uit();
            HulaClient.uit();
        });
    }

    /** guhwaiispellen_open: Lilo-guh's screen (surf or hula). */
    public static void open(GuhwaiiSpellenPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new SpelScherm(payload.npcId(), payload.data()));
    }

    private GuhwaiiSpellenClient() {
    }
}
