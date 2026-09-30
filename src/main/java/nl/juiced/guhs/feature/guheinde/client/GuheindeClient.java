package nl.juiced.guhs.feature.guheinde.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.MikaRenderer;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * Client side of the Guheinde: Opper-Mika (a Mika with the Knabbelkroon), the starved Enderguh (the guh model, greyer
 * the hungrier), the knabbelkristallen (the end crystal model), the Mika-larfjes, the thrown eye and vetballen, the
 * Guhvleugels on your back and the pink-purple Guheinde sky.
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
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof LivingEntityRenderer<?, ?> renderer) {
                    @SuppressWarnings("unchecked")
                    LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> player =
                            (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) renderer;
                    player.addLayer(new GuhvleugelsLayer<>(player, event.getEntityModels()));
                }
            }
        });
        modBus.addListener((RegisterDimensionSpecialEffectsEvent event) -> event.register(Guhs.id("guheinde"), new GuheindeSky()));
    }

    private GuheindeClient() {
    }
}
