package nl.juiced.guhs.feature.bio.dieren.client;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.dieren.DierenSlice;
import nl.juiced.guhs.feature.bio.dieren.KoiEntity;
import nl.juiced.guhs.feature.bio.dieren.WolkenschaapjeEntity;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * Client side of the biomes3 slice "dieren": the GeckoLib renderers of the koi (a texture per colour, a kleintje at half
 * size) and the wolkenschaapje (its fluff bones go while it is shorn, a lammetje is smaller). The kikkerguh and the guh
 * variants need nothing here: they use the renderers they already have.
 */
public final class DierenClient {
    static final DataTicket<String> KOI_KLEUR = DataTicket.create("guhs_bio_dieren_koi_kleur", String.class);
    static final DataTicket<Boolean> KOI_KLEIN = DataTicket.create("guhs_bio_dieren_koi_klein", Boolean.class);
    static final DataTicket<Boolean> KAAL = DataTicket.create("guhs_bio_dieren_schaapje_kaal", Boolean.class);

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(DierenSlice.KOI.get(), KoiRenderer::new);
            event.registerEntityRenderer(DierenSlice.WOLKENSCHAAPJE.get(), SchaapjeRenderer::new);
        });
    }

    /** The koi: one model, a texture per colour. */
    public static class KoiRenderer extends GeoEntityRenderer<KoiEntity, LivingEntityRenderState> {
        public KoiRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<KoiEntity>(Guhs.id("koi")) {
                @Override
                public void addAdditionalStateData(KoiEntity koi, @Nullable Object related, GeoRenderState state) {
                    state.addGeckolibData(KOI_KLEUR, koi.kleur().id());
                    state.addGeckolibData(KOI_KLEIN, koi.isKlein());
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    String kleur = state.getGeckolibData(KOI_KLEUR);
                    return kleur == null ? Guhs.id("textures/entity/koi_roodwit.png") : Guhs.id("textures/entity/koi_" + kleur + ".png");
                }
            });
            this.shadowRadius = 0.25f;
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float scale = info.getOrDefaultGeckolibData(KOI_KLEIN, false) ? 0.5f : 1f;
            super.scaleModelForRender(info, widthScale * scale, heightScale * scale);
        }
    }

    /** The wolkenschaapje: the fluff (bone "pluis") goes while it is shorn; a lammetje at 60 %. */
    public static class SchaapjeRenderer extends GeoEntityRenderer<WolkenschaapjeEntity, LivingEntityRenderState> {
        public SchaapjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<WolkenschaapjeEntity>(Guhs.id("wolkenschaapje")));
            this.shadowRadius = 0.4f;
        }

        @Override
        public void addRenderData(WolkenschaapjeEntity schaapje, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(KAAL, schaapje.isGeschoren());
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            boolean kaal = info.getOrDefaultGeckolibData(KAAL, false);
            bones.ifPresent("pluis", b -> b.skipRender(kaal).skipChildrenRender(kaal));
            bones.ifPresent("pluis_kop", b -> b.skipRender(kaal).skipChildrenRender(kaal));
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float scale = info.renderState().isBaby ? 0.6f : 1f;
            super.scaleModelForRender(info, widthScale * scale, heightScale * scale);
        }

        @Override
        protected float getShadowRadius(LivingEntityRenderState state) {
            return state.isBaby ? this.shadowRadius * 0.5f : this.shadowRadius;
        }
    }

    private DierenClient() {
    }
}
