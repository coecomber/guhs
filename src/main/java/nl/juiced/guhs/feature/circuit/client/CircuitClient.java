package nl.juiced.guhs.feature.circuit.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.circuit.CircuitFeature;
import nl.juiced.guhs.feature.circuit.CircuitPayloads;

/**
 * Client side of the Guh-Circuit: the Mika-pikkers (the Mika model in a racing bandana), the rolling kaasknabbels, Coach
 * Vahoegvroem's own model (helmet, headset, stopwatch) and her screen. The race panel and the race guh are the race's.
 */
public final class CircuitClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(CircuitFeature.MIKAPIKKER.get(), MikaPikkerRenderer::new);
            event.registerEntityRenderer(CircuitFeature.ROLKNABBEL.get(), RolknabbelRenderer::new);
        });
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.CIRCUITGUH, Guhs.id("geo/entity/guh_npc_circuitguh.geo.json"));
    }

    public static void open(CircuitPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new CircuitScreen(payload));
    }

    private CircuitClient() {
    }
}
