package nl.juiced.guhs.feature.huisje.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import nl.juiced.guhs.client.GuhRenderer;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.huisje.HuisjeFeature;
import nl.juiced.guhs.feature.huisje.HuisjePayloads;

/**
 * Client side of the Guhhuisje (2.10): the big guh-head renderer, its three models, the huisje screen, the blue dome
 * ("laat klus-area zien") and hiding guhs that sleep inside (no model, no name, no shadow).
 */
public final class HuisjeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(HuisjeFeature.HUISJE_BE.get(), HuisjeRenderer::new));
        // 1.3.2: the sleeping stand-ins inside a huisje look like guhs; the door fade
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(HuisjeFeature.SLAPER.get(), GuhRenderer::new));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) ->
                event.registerAboveAll(nl.juiced.guhs.Guhs.id("huisje_binnen_fade"), BinnenFade::extractRenderState));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event) -> BinnenFade.tick());
        HuisjePayloads.fade = p -> Minecraft.getInstance().execute(BinnenFade::start);
        modBus.addListener(HuisjeRenderer::registerModels);
        HuisjePayloads.opener = p -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof HuisjeScreen s && s.pos().asLong() == p.data().getLongOr("Pos", 0L)) {
                s.update(p.data());
            } else {
                mc.setScreen(new HuisjeScreen(p.data()));
            }
        });
        HuisjePayloads.overzichtOntvanger = p -> Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof HuisjeScreen s && s.pos().asLong() == p.data().getLongOr("Pos", 0L)) {
                s.overzicht(p.data());
            }
        });
        NeoForge.EVENT_BUS.addListener(HuisjeKoepel::teken);
        // (asleep inside its huisje: no model, no name, no shadow; 1.1.0: the guh renderer is a GeckoLib renderer, so this is a
        // render state modifier instead of cancelling RenderLivingEvent.Pre)
        modBus.addListener((RegisterRenderStateModifiersEvent event) -> event.<GuhEntity, LivingEntityRenderState>registerEntityModifier(GuhRenderer.class,
                (guh, state) -> {
                    if (BandVlaggen.heeft(guh, BandVlaggen.HUISJE_BINNEN)) {
                        state.isInvisible = true;
                        state.isInvisibleToPlayer = true;
                        state.nameTag = null;
                        state.scoreText = null;
                        state.shadowPieces.clear();
                        state.displayFireAnimation = false;
                        state.outlineColor = 0;
                    }
                }));
    }

    private HuisjeClient() {
    }
}
