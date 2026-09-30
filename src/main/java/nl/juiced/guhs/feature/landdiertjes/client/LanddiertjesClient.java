package nl.juiced.guhs.feature.landdiertjes.client;

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
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.landdiertjes.GuhKonijntjeEntity;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.landdiertjes.Landdiertje;

/**
 * 3.0 (Guhverhalen), slice landdiertjes, client side: the GeckoLib renderers of the four critters (models, animations and
 * textures from tools/features/landdiertjes_modellen.py). The guh-konijntje has a texture per fur colour
 * ({@code guh_konijntje_<kleur>.png}). Their shoulder copies (the pluiseekhoorntje) are drawn by PiepClient with these.
 */
public final class LanddiertjesClient {
    /** The konijntje's fur colour id (render state ticket; the model picks the texture from it). */
    static final DataTicket<String> KLEUR = DataTicket.create("guhs_konijntje_kleur", String.class);

    public static void init(IEventBus modBus) {
        modBus.addListener(LanddiertjesClient::renderers);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(LanddiertjesFeature.PLUISEGELTJE.get(), c -> new DierRenderer<>(c, "pluisegeltje", 0.25f));
        event.registerEntityRenderer(LanddiertjesFeature.GUH_KONIJNTJE.get(), KonijntjeRenderer::new);
        event.registerEntityRenderer(LanddiertjesFeature.PLUISEEKHOORNTJE.get(), c -> new DierRenderer<>(c, "pluiseekhoorntje", 0.22f));
        event.registerEntityRenderer(LanddiertjesFeature.SHUCKLE.get(), c -> new DierRenderer<>(c, "shuckle", 0.32f));
    }

    /** A critter: its own model (the "head" bone follows where it looks). */
    public static class DierRenderer<T extends Landdiertje> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        public DierRenderer(EntityRendererProvider.Context context, String naam, float schaduw) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id(naam)));
            this.shadowRadius = schaduw;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    /** The guh-konijntje: its fur colour picks the texture. */
    public static class KonijntjeRenderer extends GeoEntityRenderer<GuhKonijntjeEntity, LivingEntityRenderState> {
        public KonijntjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<GuhKonijntjeEntity>(Guhs.id("guh_konijntje")) {
                @Override
                public void addAdditionalStateData(GuhKonijntjeEntity konijn, @Nullable Object related, GeoRenderState state) {
                    state.addGeckolibData(KLEUR, konijn.kleur().id());
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    String kleur = state.getGeckolibData(KLEUR);
                    return kleur == null ? super.getTextureResource(state) : Guhs.id("textures/entity/guh_konijntje_" + kleur + ".png");
                }
            });
            this.shadowRadius = 0.25f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    private LanddiertjesClient() {
    }
}
