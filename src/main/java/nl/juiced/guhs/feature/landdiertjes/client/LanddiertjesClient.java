package nl.juiced.guhs.feature.landdiertjes.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.landdiertjes.GuhKonijntjeEntity;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.landdiertjes.Landdiertje;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 3.0 (Guhverhalen), slice landdiertjes, client side: the GeckoLib renderers of the four critters (models, animations and
 * textures from tools/features/landdiertjes_modellen.py). The guh-konijntje has a texture per fur colour
 * ({@code guh_konijntje_<kleur>.png}). Their shoulder copies (the pluiseekhoorntje) are drawn by PiepClient with these.
 */
public final class LanddiertjesClient {
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
    public static class DierRenderer<T extends Landdiertje> extends GeoEntityRenderer<T> {
        public DierRenderer(EntityRendererProvider.Context context, String naam, float schaduw) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id(naam), true));
            this.shadowRadius = schaduw;
        }

    }

    /** The guh-konijntje: its fur colour picks the texture. */
    public static class KonijntjeRenderer extends GeoEntityRenderer<GuhKonijntjeEntity> {
        public KonijntjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<GuhKonijntjeEntity>(Guhs.id("guh_konijntje"), true) {
                @Override
                public ResourceLocation getTextureResource(GuhKonijntjeEntity konijn) {
                    return Guhs.id("textures/entity/guh_konijntje_" + konijn.kleur().id() + ".png");
                }
            });
            this.shadowRadius = 0.25f;
        }
    }

    private LanddiertjesClient() {
    }
}
