package nl.juiced.guhs.feature.smul.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.smul.SmulFeature;
import nl.juiced.guhs.feature.smul.SmulPayloads;

/** Client side of the Vadsig eetfestijn: the falling food and the Smulguh's screen (only loaded on the client). */
public final class SmulClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(SmulFeature.HAPJE.get(), SmulHapjeRenderer::new));
    }

    public static void open(SmulPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new SmulScreen(payload.npcId(), payload.data()));
    }

    private SmulClient() {
    }
}
