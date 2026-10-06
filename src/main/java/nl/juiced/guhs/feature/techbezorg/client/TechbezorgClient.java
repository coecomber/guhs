package nl.juiced.guhs.feature.techbezorg.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.techbezorg.BezorgguhtjeEntity;
import nl.juiced.guhs.feature.techbezorg.TechbezorgFeature;

/**
 * Client side of bbq2 (tech-bezorg): the GeckoLib renderer of the Bezorgguhtje (model, animations and textures from
 * tools/features/tech_bezorg_modellen.py; asleep it has its own texture with closed eyes) and the screens of the Stepstation
 * and the Haltepaaltje.
 */
public final class TechbezorgClient {
    /** The model is drawn this much smaller than it was built (= tech_bezorg_modellen.SCHAAL): about 0.8 block tall. */
    public static final float SCHAAL = 0.8f;
    private static final DataTicket<Boolean> SLAAPT = DataTicket.create("guhs_bezorgguhtje_slaapt", Boolean.class);
    private static final Identifier TEXTUUR_SLAAPT = Guhs.id("textures/entity/bezorgguhtje_slaapt.png");

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(TechbezorgFeature.BEZORGGUHTJE.get(), GuhtjeRenderer::new));
        modBus.addListener((RegisterMenuScreensEvent event) -> {
            event.register(TechbezorgFeature.STEPSTATION_MENU.get(), StepstationScreen::new);
            event.register(TechbezorgFeature.HALTE_MENU.get(), HalteScreen::new);
        });
    }

    /** The Bezorgguhtje: its head follows where it looks (not while it sleeps), closed eyes while asleep. */
    public static class GuhtjeRenderer extends GeoEntityRenderer<BezorgguhtjeEntity, LivingEntityRenderState> {
        public GuhtjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<BezorgguhtjeEntity>(Guhs.id("bezorgguhtje")) {
                @Override
                public void addAdditionalStateData(BezorgguhtjeEntity guhtje, @Nullable Object related, GeoRenderState state) {
                    state.addGeckolibData(SLAAPT, guhtje.slaapt());
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Boolean.TRUE.equals(state.getGeckolibData(SLAAPT)) ? TEXTUUR_SLAAPT : super.getTextureResource(state);
                }
            });
            this.shadowRadius = 0.3f;
            withScale(SCHAAL, SCHAAL);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            if (!Boolean.TRUE.equals(info.getGeckolibData(SLAAPT))) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            }
        }
    }

    private TechbezorgClient() {
    }
}
