package nl.juiced.guhs.feature.guheinde.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.client.MikaRenderer;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * Client side of the Guheinde: Opper-Mika (a Mika with the Knabbelkroon), the starved Enderguh (the guh model, greyer
 * the hungrier), the knabbelkristallen (the end crystal model), the Mika-larfjes, the thrown eye and vetballen.
 * <p>
 * 1.1.0: the Guhvleugels on your back are drawn by vanilla's wings layer from their equipment asset (guhs:guhvleugels),
 * and the pink-purple Guheinde sky ({@link GuheindeSky}) is registered by {@code client.GuhmensionSky#register} (R).
 */
public final class GuheindeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(GuheindeFeature.OPPER_MIKA.get(), OpperMikaRenderer::new);
            event.registerEntityRenderer(GuheindeFeature.HONGERIGE_ENDERGUH.get(), HongerigeEnderguhRenderer::new);
            event.registerEntityRenderer(GuheindeFeature.KNABBELKRISTAL_ENTITY.get(), KnabbelkristalRenderer::new);
            event.registerEntityRenderer(GuheindeFeature.MIKA_LARFJE.get(), MikaRenderer::new);
            event.registerEntityRenderer(GuheindeFeature.OOG_ENTITY.get(), context -> new ThrownItemRenderer<>(context, 1f, true));
            event.registerEntityRenderer(GuheindeFeature.MIKA_VETBAL.get(), context -> new ThrownItemRenderer<>(context, 1.4f, false));
        });
    }

    private GuheindeClient() {
    }
}
