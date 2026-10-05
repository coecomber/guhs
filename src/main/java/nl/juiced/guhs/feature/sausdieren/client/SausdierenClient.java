package nl.juiced.guhs.feature.sausdieren.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.sausdieren.SausblubjeEntity;
import nl.juiced.guhs.feature.sausdieren.SausdierenFeature;
import nl.juiced.guhs.feature.sausdieren.SausloperEntity;

/**
 * Client side of bbq2 (sausdieren): the GeckoLib renderers of the Sausloper (a cold one gets its pale blue coat, the saddle
 * shows when it wears one) and the Sausblubje (drawn as big as it is; its sauce glows). Models, animations and textures:
 * tools/features/sausdieren_modellen.py.
 */
public final class SausdierenClient {
    private static final DataTicket<Boolean> KOUD = DataTicket.create("guhs_sausloper_koud", Boolean.class);
    private static final DataTicket<Boolean> ZADEL = DataTicket.create("guhs_sausloper_zadel", Boolean.class);
    private static final DataTicket<Float> SCHAAL = DataTicket.create("guhs_sausblubje_schaal", Float.class);
    private static final Identifier KOUD_TEXTUUR = Guhs.id("textures/entity/sausloper_koud.png");
    /** In a screen (its Guhdex page) a Sausblubje is drawn as a middle one. */
    static final float SCHERM_SCHAAL = 1.9f;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(SausdierenFeature.SAUSLOPER.get(), SausloperRenderer::new);
            event.registerEntityRenderer(SausdierenFeature.SAUSBLUBJE.get(), SausblubjeRenderer::new);
        });
    }

    static class SausloperRenderer extends GeoEntityRenderer<SausloperEntity, LivingEntityRenderState> {
        SausloperRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<SausloperEntity>(Guhs.id("sausloper")) {
                @Override
                public void addAdditionalStateData(SausloperEntity loper, @Nullable Object related, GeoRenderState state) {
                    state.addGeckolibData(KOUD, loper.isKoud());
                    state.addGeckolibData(ZADEL, loper.isSaddled());
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Boolean.TRUE.equals(state.getGeckolibData(KOUD)) ? KOUD_TEXTUUR : super.getTextureResource(state);
                }
            });
            this.shadowRadius = 0.5f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            if (!info.getOrDefaultGeckolibData(ZADEL, false)) {
                bones.ifPresent("zadel", b -> b.skipRender(true).skipChildrenRender(true));
            }
        }
    }

    static class SausblubjeRenderer extends GeoEntityRenderer<SausblubjeEntity, LivingEntityRenderState> {
        SausblubjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<SausblubjeEntity>(Guhs.id("sausblubje")));
            this.shadowRadius = 0.25f;
            withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        public void addRenderData(SausblubjeEntity blubje, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(SCHAAL, blubje.isAddedToLevel() ? blubje.schaal() : SCHERM_SCHAAL);
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float s = info.getOrDefaultGeckolibData(SCHAAL, 1f);
            super.scaleModelForRender(info, widthScale * s, heightScale * s);
        }
    }

    private SausdierenClient() {
    }
}
