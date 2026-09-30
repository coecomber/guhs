package nl.juiced.guhs.feature.waterdiertjes.client;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.waterdiertjes.GlimguhtjeEntity;
import nl.juiced.guhs.feature.waterdiertjes.GuhEendjeEntity;
import nl.juiced.guhs.feature.waterdiertjes.GuhxolotlEmmertje;
import nl.juiced.guhs.feature.waterdiertjes.GuhxolotlEntity;
import nl.juiced.guhs.feature.waterdiertjes.KnabbelvlindertjeEntity;
import nl.juiced.guhs.feature.waterdiertjes.LieveheersbeestjeEntity;
import nl.juiced.guhs.feature.waterdiertjes.WaterdiertjesFeature;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

/**
 * Client side of the waterdiertjes: the GeckoLib renderers (a texture per colour for the guhxolotl and the knabbelvlindertje,
 * the fluffy yellow kuikentjes, the glowing lantern of the glimguhtje and the gold guhxolotl's sparkles at full bright),
 * and the emmertje icon in the colour of the guhxolotl inside it.
 * <p>
 * 1.1.0 (GeckoLib 5 / MC 26.1): colours and "baby" are copied into the render state (data tickets) at extract time; the
 * emmertje's icon is a client item definition that selects on {@code guhs:guhxolotl_kleur} (registered here).
 */
public final class WaterdiertjesClient {
    static final DataTicket<String> VLINDER_KLEUR = DataTicket.create("guhs_knabbelvlindertje_kleur", String.class);
    static final DataTicket<String> XOLOTL_KLEUR = DataTicket.create("guhs_guhxolotl_kleur", String.class);
    static final DataTicket<Boolean> KUIKEN = DataTicket.create("guhs_guh_eendje_kuiken", Boolean.class);

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(WaterdiertjesFeature.GUHXOLOTL.get(), GuhxolotlRenderer::new);
            event.registerEntityRenderer(WaterdiertjesFeature.GUH_EENDJE.get(), EendjeRenderer::new);
            event.registerEntityRenderer(WaterdiertjesFeature.KNABBELVLINDERTJE.get(), context -> new GeoEntityRenderer<KnabbelvlindertjeEntity, LivingEntityRenderState>(context,
                    new DefaultedEntityGeoModel<KnabbelvlindertjeEntity>(Guhs.id("knabbelvlindertje")) {
                        @Override
                        public void addAdditionalStateData(KnabbelvlindertjeEntity v, @Nullable Object related, GeoRenderState state) {
                            state.addGeckolibData(VLINDER_KLEUR, v.kleur().id());
                        }

                        @Override
                        public Identifier getTextureResource(GeoRenderState state) {
                            String kleur = state.getGeckolibData(VLINDER_KLEUR);
                            return kleur == null ? super.getTextureResource(state) : Guhs.id("textures/entity/knabbelvlindertje_" + kleur + ".png");
                        }
                    }) {
                {
                    this.shadowRadius = 0.1f;
                }
            });
            event.registerEntityRenderer(WaterdiertjesFeature.GLIMGUHTJE.get(), context -> {
                GeoEntityRenderer<GlimguhtjeEntity, LivingEntityRenderState> r = new GeoEntityRenderer<GlimguhtjeEntity, LivingEntityRenderState>(context,
                        new DefaultedEntityGeoModel<GlimguhtjeEntity>(Guhs.id("glimguhtje"))) {
                    {
                        this.shadowRadius = 0.0f;
                    }
                };
                r.withRenderLayer(new AutoGlowingGeoLayer<>(r));
                return r;
            });
            event.registerEntityRenderer(WaterdiertjesFeature.LIEVEHEERSBEESTJE.get(), context -> new GeoEntityRenderer<LieveheersbeestjeEntity, LivingEntityRenderState>(context,
                    new DefaultedEntityGeoModel<LieveheersbeestjeEntity>(Guhs.id("lieveheersbeestje"))) {
                {
                    this.shadowRadius = 0.08f;
                }
            });
        });
        // (1.1.0: was ItemProperties "guhs:kleur" = ordinal / 4 with model overrides; D's client item definition selects on the name)
        modBus.addListener((RegisterSelectItemModelPropertyEvent event) -> event.register(Guhs.id("guhxolotl_kleur"), EmmertjeKleur.TYPE));
    }

    /** The colour of the guhxolotl in an emmertje (roze/mint/choco/wit/goud) for the emmertje's client item definition. */
    public record EmmertjeKleur() implements SelectItemModelProperty<String> {
        public static final SelectItemModelProperty.Type<EmmertjeKleur, String> TYPE =
                SelectItemModelProperty.Type.create(MapCodec.unit(new EmmertjeKleur()), Codec.STRING);

        @Override
        public String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext context) {
            return GuhxolotlEmmertje.kleur(stack).getSerializedName();
        }

        @Override
        public Codec<String> valueCodec() {
            return Codec.STRING;
        }

        @Override
        public SelectItemModelProperty.Type<EmmertjeKleur, String> type() {
            return TYPE;
        }
    }

    /** The guhxolotl: one model, a texture per colour, little ones smaller, the golden one's sparkles glow. */
    public static class GuhxolotlRenderer extends GeoEntityRenderer<GuhxolotlEntity, LivingEntityRenderState> {
        public GuhxolotlRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<GuhxolotlEntity>(Guhs.id("guhxolotl")) {
                @Override
                public void addAdditionalStateData(GuhxolotlEntity x, @Nullable Object related, GeoRenderState state) {
                    state.addGeckolibData(XOLOTL_KLEUR, x.kleur().getSerializedName());
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    String kleur = state.getGeckolibData(XOLOTL_KLEUR);
                    return kleur == null ? super.getTextureResource(state) : Guhs.id("textures/entity/guhxolotl_" + kleur + ".png");
                }
            });
            this.shadowRadius = 0.3f;
            withRenderLayer(new AutoGlowingGeoLayer<>(this) {
                @Override
                public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
                    if (GuhxolotlEntity.Kleur.GOUD.getSerializedName().equals(info.getGeckolibData(XOLOTL_KLEUR))) {
                        super.submitRenderTask(info, collector);
                    }
                }
            });
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float scale = info.renderState().isBaby ? 0.55f : 1f;
            super.scaleModelForRender(info, widthScale * scale, heightScale * scale);
        }

        /** 1.21.1 halved the shadow of baby mobs in the entity render dispatcher. */
        @Override
        protected float getShadowRadius(LivingEntityRenderState state) {
            return state.isBaby ? this.shadowRadius * 0.5f : this.shadowRadius;
        }
    }

    /** The guh-eendje: mama cream-white, the kuikentjes small, fluffy and yellow. */
    public static class EendjeRenderer extends GeoEntityRenderer<GuhEendjeEntity, LivingEntityRenderState> {
        public EendjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<GuhEendjeEntity>(Guhs.id("guh_eendje")) {
                @Override
                public void addAdditionalStateData(GuhEendjeEntity e, @Nullable Object related, GeoRenderState state) {
                    state.addGeckolibData(KUIKEN, e.isBaby());
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Guhs.id(state.getOrDefaultGeckolibData(KUIKEN, false) ? "textures/entity/guh_eendje_kuiken.png" : "textures/entity/guh_eendje.png");
                }
            });
            this.shadowRadius = 0.25f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float scale = info.renderState().isBaby ? 0.5f : 1f;
            super.scaleModelForRender(info, widthScale * scale, heightScale * scale);
        }

        /** 1.21.1 halved the shadow of baby mobs in the entity render dispatcher. */
        @Override
        protected float getShadowRadius(LivingEntityRenderState state) {
            return state.isBaby ? this.shadowRadius * 0.5f : this.shadowRadius;
        }
    }

    private WaterdiertjesClient() {
    }
}
