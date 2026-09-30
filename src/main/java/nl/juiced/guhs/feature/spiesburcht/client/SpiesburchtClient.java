package nl.juiced.guhs.feature.spiesburcht.client;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.spiesburcht.AangebrandeMikaEntity;
import nl.juiced.guhs.feature.spiesburcht.KnabbelbakenBlockEntity;
import nl.juiced.guhs.feature.spiesburcht.KnekelMikaEntity;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.spiesburcht.VonkMikaEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The Spiesburcht on the client: the GeckoLib models of the Rookguh (rounder and rosier with every knabbel), the
 * Vonk-Mika, the Knekel-Mika and the Aangebrande Mika (glowing embers), the kooltjes, the Knabbelbaken's beam, the
 * Asguh's glowing cheeks and the Nether-Mika holding up your ingot. (2.10.1: the saved-Rookguh count moved from above the
 * Guhdex to the Rookguh's own page, see GuhDexScreen.)
 * <p>
 * 1.1.0 (GeckoLib 5): everything a renderer needs is copied at extract time into data tickets (plumpness, the held ingot);
 * the Asguh cheeks are a GuhRenderer hook.
 */
public final class SpiesburchtClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(SpiesburchtClient::renderers);
        modBus.addListener(SpiesburchtClient::layers);
        // the Asguh's glowing cheeks (1.1.0: a GuhRenderer hook instead of an extra layer on the guh renderer)
        Identifier cheeks = Guhs.id("textures/entity/guh_asguh_glowmask.png");
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (frame.variant == GuhVariant.ASGUH) {
                frame.glow(cheeks);
            }
        });
    }

    /** A Mika with glowing embers whose head bone follows its look (GeckoLib 4: DefaultedEntityGeoModel(id, true) / (id, "head_mid")). */
    static <T extends net.minecraft.world.entity.LivingEntity & com.geckolib.animatable.GeoAnimatable> GeoEntityRenderer<T, LivingEntityRenderState> gloeiend(
            EntityRendererProvider.Context ctx, String id, String headBone) {
        var r = new GeoEntityRenderer<T, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<T>(Guhs.id(id))) {
            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, headBone);
            }
        };
        r.withRenderLayer(new AutoGlowingGeoLayer<>(r));
        return r;
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SpiesburchtFeature.ROOKGUH.get(), RookguhRenderer::new);
        event.registerEntityRenderer(SpiesburchtFeature.VONK_MIKA.get(), ctx -> SpiesburchtClient.<VonkMikaEntity>gloeiend(ctx, "vonk_mika", "head"));
        event.registerEntityRenderer(SpiesburchtFeature.KNEKEL_MIKA.get(), ctx -> SpiesburchtClient.<KnekelMikaEntity>gloeiend(ctx, "knekel_mika", "head"));
        event.registerEntityRenderer(SpiesburchtFeature.AANGEBRANDE_MIKA.get(),
                ctx -> SpiesburchtClient.<AangebrandeMikaEntity>gloeiend(ctx, "aangebrande_mika", "head_mid"));
        event.registerEntityRenderer(SpiesburchtFeature.GLOEIEND_KOOLTJE.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.75f, true));
        event.registerEntityRenderer(SpiesburchtFeature.BRANDEND_KOOLTJE.get(), ctx -> new ThrownItemRenderer<>(ctx, 2.2f, true));
        event.registerBlockEntityRenderer(SpiesburchtFeature.KNABBELBAKEN_BE.get(), ctx -> new BakenRenderer());
    }

    /** The ingot a Nether-Mika holds up (an extra layer on a renderer that is not ours). */
    private static final DataTicket<ItemStackRenderState> BUIT = DataTicket.create("guhs_nether_mika_buit", ItemStackRenderState.class);
    private static final DataTicket<Float> BUIT_DRAAI = DataTicket.create("guhs_nether_mika_buit_draai", Float.class);

    @SuppressWarnings("unchecked")
    private static void layers(EntityRenderersEvent.AddLayers event) {
        EntityRenderer<?, ?> nether = event.getRenderer(ModEntities.NETHER_MIKA.get());
        if (nether instanceof GeoEntityRenderer<?, ?> geo) {
            GeoEntityRenderer<MikaEntity, LivingEntityRenderState> r = (GeoEntityRenderer<MikaEntity, LivingEntityRenderState>) geo;
            r.withRenderLayer(new GeoRenderLayer<MikaEntity, Void, LivingEntityRenderState>(r) {
                @Override
                public void addRenderData(MikaEntity mika, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                    ItemStack held = mika.getItemBySlot(EquipmentSlot.MAINHAND);
                    if (!held.isEmpty()) {
                        ItemStackRenderState item = new ItemStackRenderState();
                        Minecraft.getInstance().getItemModelResolver().updateForTopItem(item, held, ItemDisplayContext.FIXED, mika.level(), mika, mika.getId());
                        state.addGeckolibData(BUIT, item);
                        state.addGeckolibData(BUIT_DRAAI, (mika.tickCount + partialTick) * 6f);
                    }
                }

                @Override
                public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> info,
                                             BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
                    ItemStackRenderState item = info.getGeckolibData(BUIT);
                    if (item == null || item.isEmpty() || !info.willRender()) {
                        return;
                    }
                    float draai = info.getOrDefaultGeckolibData(BUIT_DRAAI, 0f);
                    info.model().getBone("head").ifPresent(head -> consumer.accept(head, (pass, bone, collector) -> {
                        PoseStack pose = pass.poseStack();
                        pose.translate(0, -0.05, -0.72);            // held up in front of its evil little face
                        pose.mulPose(Axis.YP.rotationDegrees(draai));
                        pose.scale(0.5f, 0.5f, 0.5f);
                        item.submit(pose, collector, pass.packedLight(), OverlayTexture.NO_OVERLAY, pass.renderState().outlineColor);
                    }));
                }
            });
        }
    }

    /**
     * The Rookguh (2.8): a ghast, 3/4 of the size (the 16-unit model drawn {@link #SCALE} blocks big), with a flat guh
     * face. Its body swells a little and it turns from pale white to rosy pink with every knabbel.
     */
    static class RookguhRenderer extends GeoEntityRenderer<RookguhEntity, LivingEntityRenderState> {
        /** How big the body is drawn (blocks; the vanilla ghast: 4.5). Same as ROOK_SCALE in tools/features/spiesburcht_modellen.py. */
        static final float SCALE = 3.375f;
        /** In a screen (the Guhdex page) it's drawn this much smaller and a bit higher, tentacles and all. */
        static final float GUI_SCALE = 0.42f, GUI_LIFT = 1.35f;
        private static final DataTicket<Float> PLUMP = DataTicket.create("guhs_rookguh_plump", Float.class);
        private static final DataTicket<Boolean> SCREEN = DataTicket.create("guhs_rookguh_screen", Boolean.class);

        RookguhRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<RookguhEntity>(Guhs.id("rookguh")));
            this.shadowRadius = 1.5f;
        }

        @Override
        public void addRenderData(RookguhEntity guh, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(PLUMP, guh.plumpness());
            state.addGeckolibData(SCREEN, !guh.isAddedToLevel());      // (the Guhdex's own Rookguh is never in the world)
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            float p = info.getOrDefaultGeckolibData(PLUMP, 0f);
            // (absolute values on its own bone, never multiplied)
            bones.ifPresent("rook", b -> {
                b.setScaleX(0.92f + 0.16f * p);
                b.setScaleZ(0.92f + 0.16f * p);
                b.setScaleY(0.96f + 0.08f * p);
            });
            if (p < 0.3f) {
                bones.ifPresent("cheeks", b -> b.skipRender(true).skipChildrenRender(true));
            }
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float s = SCALE * (info.getOrDefaultGeckolibData(SCREEN, false) ? GUI_SCALE : 1f);
            super.scaleModelForRender(info, widthScale * s, heightScale * s);
        }

        @Override
        public void submit(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            boolean screen = state.getOrDefaultGeckolibData(SCREEN, false);
            if (screen) {
                pose.pushPose();
                pose.translate(0, GUI_LIFT, 0);
            }
            super.submit(state, pose, collector, camera);
            if (screen) {
                pose.popPose();
            }
        }

        @Override
        public int getRenderColor(RookguhEntity guh, @Nullable Void related, float partialTick) {
            float p = guh.plumpness();
            return ARGB.colorFromFloat(1.0f, 0.95f + 0.05f * p, 0.95f - 0.1f * p, 0.96f - 0.04f * p);
        }
    }

    /** The Knabbelbaken's beam, in the colour of the chosen effect. */
    static class BakenRenderer implements BlockEntityRenderer<KnabbelbakenBlockEntity, BakenRenderer.State> {
        static class State extends BlockEntityRenderState {
            int height;
            int colour;
            float animationTime;
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(KnabbelbakenBlockEntity baken, State state, float partialTick, Vec3 cameraPosition,
                                       @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(baken, state, partialTick, cameraPosition, breakProgress);
            state.height = 0;
            if (baken.getLevel() == null) {
                return;
            }
            if (baken.levels() <= 0) {
                baken.refresh();
                if (baken.levels() <= 0) {
                    return;
                }
            }
            state.height = baken.beamHeight();
            state.colour = 0xFF000000 | baken.gunst().colour;
            state.animationTime = Math.floorMod(baken.getLevel().getGameTime(), 40) + partialTick;
        }

        @Override
        public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.height <= 0) {
                return;
            }
            BeaconRenderer.submitBeaconBeam(poseStack, collector, BeaconRenderer.BEAM_LOCATION, 1.0f, state.animationTime, 1, state.height,
                    state.colour, 0.2f, 0.25f);
        }

        @Override
        public boolean shouldRenderOffScreen() {
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
