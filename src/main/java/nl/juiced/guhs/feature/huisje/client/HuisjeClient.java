package nl.juiced.guhs.feature.huisje.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.huisje.HuisjeFeature;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;
import nl.juiced.guhs.feature.huisje.HuisjePayloads;

/**
 * Client side of the Guhhuisje (2.10): the big guh-head renderer, its three models, the huisje screen, the blue dome
 * ("laat klus-area zien") and hiding guhs that sleep inside (no model, no name, no shadow).
 */
public final class HuisjeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(HuisjeFeature.HUISJE_BE.get(), HuisjeRenderer::new));
        modBus.addListener((ModelEvent.RegisterAdditional event) -> {
            for (HuisjeMaat maat : HuisjeMaat.values()) {
                event.register(HuisjeRenderer.model(maat));
            }
        });
        HuisjePayloads.opener = p -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof HuisjeScreen s && s.pos().asLong() == p.data().getLong("Pos")) {
                s.update(p.data());
            } else {
                mc.setScreen(new HuisjeScreen(p.data()));
            }
        });
        NeoForge.EVENT_BUS.addListener(HuisjeKoepel::teken);
        NeoForge.EVENT_BUS.addListener((RenderLivingEvent.Pre<?, ?> event) -> {
            if (event.getEntity() instanceof GuhEntity guh && BandVlaggen.heeft(guh, BandVlaggen.HUISJE_BINNEN)) {
                event.setCanceled(true);   // (asleep inside its huisje)
            }
        });
    }

    private HuisjeClient() {
    }
}
