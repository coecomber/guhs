package nl.juiced.guhs.feature.waterdiertjes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.waterdiertjes.GlimguhtjeEntity;
import nl.juiced.guhs.feature.waterdiertjes.GuhEendjeEntity;
import nl.juiced.guhs.feature.waterdiertjes.GuhxolotlEmmertje;
import nl.juiced.guhs.feature.waterdiertjes.GuhxolotlEntity;
import nl.juiced.guhs.feature.waterdiertjes.KnabbelvlindertjeEntity;
import nl.juiced.guhs.feature.waterdiertjes.LieveheersbeestjeEntity;
import nl.juiced.guhs.feature.waterdiertjes.WaterdiertjesFeature;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

/**
 * Client side of the waterdiertjes: the GeckoLib renderers (a texture per colour for the guhxolotl and the knabbelvlindertje,
 * the fluffy yellow kuikentjes, the glowing lantern of the glimguhtje and the gold guhxolotl's sparkles at full bright),
 * and the emmertje icon in the colour of the guhxolotl inside it.
 */
public final class WaterdiertjesClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(WaterdiertjesFeature.GUHXOLOTL.get(), GuhxolotlRenderer::new);
            event.registerEntityRenderer(WaterdiertjesFeature.GUH_EENDJE.get(), EendjeRenderer::new);
            event.registerEntityRenderer(WaterdiertjesFeature.KNABBELVLINDERTJE.get(), context -> new GeoEntityRenderer<>(context,
                    new DefaultedEntityGeoModel<KnabbelvlindertjeEntity>(Guhs.id("knabbelvlindertje")) {
                        @Override
                        public Identifier getTextureResource(KnabbelvlindertjeEntity v) {
                            return Guhs.id("textures/entity/knabbelvlindertje_" + v.kleur().id() + ".png");
                        }
                    }) {
                {
                    this.shadowRadius = 0.1f;
                }
            });
            event.registerEntityRenderer(WaterdiertjesFeature.GLIMGUHTJE.get(), context -> {
                GeoEntityRenderer<GlimguhtjeEntity> r = new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<GlimguhtjeEntity>(Guhs.id("glimguhtje"))) {
                    {
                        this.shadowRadius = 0.0f;
                    }
                };
                r.addRenderLayer(new AutoGlowingGeoLayer<>(r));
                return r;
            });
            event.registerEntityRenderer(WaterdiertjesFeature.LIEVEHEERSBEESTJE.get(), context -> new GeoEntityRenderer<>(context,
                    new DefaultedEntityGeoModel<LieveheersbeestjeEntity>(Guhs.id("lieveheersbeestje"))) {
                {
                    this.shadowRadius = 0.08f;
                }
            });
        });
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> ItemProperties.register(WaterdiertjesFeature.GUHXOLOTL_EMMERTJE.get(),
                Guhs.id("kleur"), (stack, level, entity, seed) -> GuhxolotlEmmertje.kleur(stack).ordinal() / 4f)));
    }

    /** The guhxolotl: one model, a texture per colour, little ones smaller, the golden one's sparkles glow. */
    public static class GuhxolotlRenderer extends GeoEntityRenderer<GuhxolotlEntity> {
        public GuhxolotlRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<GuhxolotlEntity>(Guhs.id("guhxolotl"), true) {
                @Override
                public Identifier getTextureResource(GuhxolotlEntity x) {
                    return Guhs.id("textures/entity/guhxolotl_" + x.kleur().getSerializedName() + ".png");
                }
            });
            this.shadowRadius = 0.3f;
            addRenderLayer(new AutoGlowingGeoLayer<>(this) {
                @Override
                public void render(PoseStack pose, GuhxolotlEntity x, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                                   VertexConsumer buffer, float partialTick, int light, int overlay) {
                    if (x.kleur() == GuhxolotlEntity.Kleur.GOUD) {
                        super.render(pose, x, model, type, buffers, buffer, partialTick, light, overlay);
                    }
                }
            });
        }

        @Override
        public void render(GuhxolotlEntity x, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            float scale = x.isBaby() ? 0.55f : 1f;
            this.scaleWidth = scale;
            this.scaleHeight = scale;
            super.render(x, yaw, partialTick, pose, buffers, light);
        }
    }

    /** The guh-eendje: mama cream-white, the kuikentjes small, fluffy and yellow. */
    public static class EendjeRenderer extends GeoEntityRenderer<GuhEendjeEntity> {
        public EendjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<GuhEendjeEntity>(Guhs.id("guh_eendje"), true) {
                @Override
                public Identifier getTextureResource(GuhEendjeEntity e) {
                    return Guhs.id(e.isBaby() ? "textures/entity/guh_eendje_kuiken.png" : "textures/entity/guh_eendje.png");
                }
            });
            this.shadowRadius = 0.25f;
        }

        @Override
        public void render(GuhEendjeEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            float scale = e.isBaby() ? 0.5f : 1f;
            this.scaleWidth = scale;
            this.scaleHeight = scale;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }
    }

    private WaterdiertjesClient() {
    }
}
