package nl.juiced.guhs.feature.spiesburcht.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.spiesburcht.AangebrandeMikaEntity;
import nl.juiced.guhs.feature.spiesburcht.KnabbelbakenBlockEntity;
import nl.juiced.guhs.feature.spiesburcht.KnekelMikaEntity;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.spiesburcht.VonkMikaEntity;
import nl.juiced.guhs.registry.ModEntities;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/**
 * The Spiesburcht on the client: the GeckoLib models of the Rookguh (rounder and rosier with every knabbel), the
 * Vonk-Mika, the Knekel-Mika and the Aangebrande Mika (glowing embers), the kooltjes, the Knabbelbaken's beam, the
 * Asguh's glowing cheeks and the Nether-Mika holding up your ingot. (2.10.1: the saved-Rookguh count moved from above the
 * Guhdex to the Rookguh's own page, see GuhDexScreen.)
 */
public final class SpiesburchtClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(SpiesburchtClient::renderers);
        modBus.addListener(SpiesburchtClient::layers);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SpiesburchtFeature.ROOKGUH.get(), RookguhRenderer::new);
        event.registerEntityRenderer(SpiesburchtFeature.VONK_MIKA.get(), ctx -> {
            var r = new GeoEntityRenderer<VonkMikaEntity>(ctx, new DefaultedEntityGeoModel<>(Guhs.id("vonk_mika"), true));
            r.addRenderLayer(new AutoGlowingGeoLayer<>(r));
            r.withScale(1.0f);
            return r;
        });
        event.registerEntityRenderer(SpiesburchtFeature.KNEKEL_MIKA.get(), ctx -> {
            var r = new GeoEntityRenderer<KnekelMikaEntity>(ctx, new DefaultedEntityGeoModel<>(Guhs.id("knekel_mika"), true));
            r.addRenderLayer(new AutoGlowingGeoLayer<>(r));
            return r;
        });
        event.registerEntityRenderer(SpiesburchtFeature.AANGEBRANDE_MIKA.get(), ctx -> {
            var r = new GeoEntityRenderer<AangebrandeMikaEntity>(ctx, new DefaultedEntityGeoModel<>(Guhs.id("aangebrande_mika"), "head_mid"));
            r.addRenderLayer(new AutoGlowingGeoLayer<>(r));
            return r;
        });
        event.registerEntityRenderer(SpiesburchtFeature.GLOEIEND_KOOLTJE.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.75f, true));
        event.registerEntityRenderer(SpiesburchtFeature.BRANDEND_KOOLTJE.get(), ctx -> new ThrownItemRenderer<>(ctx, 2.2f, true));
        event.registerBlockEntityRenderer(SpiesburchtFeature.KNABBELBAKEN_BE.get(), ctx -> new BakenRenderer());
    }

    /** Extra layers on renderers that are not ours: the Asguh's cheeks, and the ingot in a Nether-Mika's mouth. */
    @SuppressWarnings("unchecked")
    private static void layers(EntityRenderersEvent.AddLayers event) {
        EntityRenderer<?> guh = event.getRenderer(ModEntities.GUH.get());
        if (guh instanceof GeoEntityRenderer<?> geo) {
            GeoEntityRenderer<GuhEntity> r = (GeoEntityRenderer<GuhEntity>) geo;
            ResourceLocation cheeks = Guhs.id("textures/entity/guh_asguh_glowmask.png");
            r.addRenderLayer(new GeoRenderLayer<>(r) {
                @Override
                public void render(PoseStack poseStack, GuhEntity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource buffers,
                                   VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                    if (animatable.getVariant() == GuhVariant.ASGUH) {
                        RenderType glow = RenderType.eyes(cheeks);
                        getRenderer().reRender(model, poseStack, buffers, animatable, glow, buffers.getBuffer(glow), partialTick,
                                LightTexture.FULL_BRIGHT, packedOverlay, 0xFFFFFFFF);
                    }
                }
            });
        }
        EntityRenderer<?> nether = event.getRenderer(ModEntities.NETHER_MIKA.get());
        if (nether instanceof GeoEntityRenderer<?> geo) {
            GeoEntityRenderer<MikaEntity> r = (GeoEntityRenderer<MikaEntity>) geo;
            r.addRenderLayer(new BlockAndItemGeoLayer<>(r) {
                @Override
                protected ItemStack getStackForBone(GeoBone bone, MikaEntity mika) {
                    ItemStack held = mika.getItemBySlot(EquipmentSlot.MAINHAND);
                    return bone.getName().equals("head") && !held.isEmpty() ? held : null;
                }

                @Override
                protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, MikaEntity mika) {
                    return ItemDisplayContext.FIXED;
                }

                @Override
                protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, MikaEntity mika, MultiBufferSource buffers,
                                                  float partialTick, int packedLight, int packedOverlay) {
                    poseStack.translate(0, -0.05, -0.72);            // held up in front of its evil little face
                    poseStack.mulPose(Axis.YP.rotationDegrees((mika.tickCount + partialTick) * 6f));
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                    super.renderStackForBone(poseStack, bone, stack, mika, buffers, partialTick, packedLight, packedOverlay);
                }
            });
        }
    }

    /**
     * The Rookguh (2.8): a ghast, 3/4 of the size (the 16-unit model drawn {@link #SCALE} blocks big), with a flat guh
     * face. Its body swells a little and it turns from pale white to rosy pink with every knabbel.
     */
    static class RookguhRenderer extends GeoEntityRenderer<RookguhEntity> {
        /** How big the body is drawn (blocks; the vanilla ghast: 4.5). Same as ROOK_SCALE in tools/features/spiesburcht_modellen.py. */
        static final float SCALE = 3.375f;
        /** In a screen (the Guhdex page) it's drawn this much smaller and a bit higher, tentacles and all. */
        static final float GUI_SCALE = 0.42f, GUI_LIFT = 1.35f;

        RookguhRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<RookguhEntity>(Guhs.id("rookguh")) {
                @Override
                public void setCustomAnimations(RookguhEntity guh, long instanceId, AnimationState<RookguhEntity> state) {
                    super.setCustomAnimations(guh, instanceId, state);
                    float p = guh.plumpness();
                    // (absolute values on its own bone: GeckoLib doesn't reset a scale nothing animates, so never multiply)
                    getBone("rook").ifPresent(b -> {
                        b.setScaleX(0.92f + 0.16f * p);
                        b.setScaleZ(0.92f + 0.16f * p);
                        b.setScaleY(0.96f + 0.08f * p);
                    });
                    getBone("cheeks").ifPresent(b -> b.setHidden(p < 0.3f));
                }
            });
            this.shadowRadius = 1.5f;
        }

        @Override
        public void render(RookguhEntity guh, float yaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose,
                           net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
            boolean screen = !guh.isAddedToLevel();          // (the Guhdex's own Rookguh is never in the world)
            this.scaleWidth = this.scaleHeight = SCALE * (screen ? GUI_SCALE : 1f);
            if (screen) {
                pose.pushPose();
                pose.translate(0, GUI_LIFT, 0);
            }
            super.render(guh, yaw, partialTick, pose, buffers, light);
            if (screen) {
                pose.popPose();
            }
        }

        @Override
        public Color getRenderColor(RookguhEntity guh, float partialTick, int packedLight) {
            float p = guh.plumpness();
            return Color.ofRGBA(0.95f + 0.05f * p, 0.95f - 0.1f * p, 0.96f - 0.04f * p, 1.0f);
        }
    }

    /** The Knabbelbaken's beam, in the colour of the chosen effect. */
    static class BakenRenderer implements BlockEntityRenderer<KnabbelbakenBlockEntity> {
        @Override
        public void render(KnabbelbakenBlockEntity baken, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            if (baken.getLevel() == null) {
                return;
            }
            if (baken.levels() <= 0) {
                baken.refresh();
                if (baken.levels() <= 0) {
                    return;
                }
            }
            int height = baken.beamHeight();
            if (height <= 0) {
                return;
            }
            BeaconRenderer.renderBeaconBeam(poseStack, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0f, baken.getLevel().getGameTime(),
                    1, height, 0xFF000000 | baken.gunst().colour, 0.2f, 0.25f);
        }

        @Override
        public boolean shouldRenderOffScreen(KnabbelbakenBlockEntity baken) {
            return true;
        }

        @Override
        public int getViewDistance() {
            return 256;
        }

        @Override
        public AABB getRenderBoundingBox(KnabbelbakenBlockEntity baken) {
            return AABB.INFINITE;
        }
    }

    private SpiesburchtClient() {
    }
}
