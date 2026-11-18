package nl.juiced.guhs.feature.guhwinter.lootjes.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.guhwinter.lootjes.LootjesFeature;

/** The client side of the lootjes slice: the placed Surprise's renderer, the preview while you hold one, its turn key. */
public final class LootjesClient {
    private LootjesClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerBlockEntityRenderer(LootjesFeature.SURPRISE_BE.get(), SurpriseRenderer::new));
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(SurpriseVoorbeeld.DRAAI));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> SurpriseVoorbeeld.tick());
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterTranslucentBlocks e) -> SurpriseVoorbeeld.teken(e));
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered e) -> SurpriseVoorbeeld.klik(e));
        NeoForge.EVENT_BUS.addListener((InputEvent.MouseScrollingEvent e) -> SurpriseVoorbeeld.scroll(e));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> SurpriseVoorbeeld.vergeet());
    }
}
