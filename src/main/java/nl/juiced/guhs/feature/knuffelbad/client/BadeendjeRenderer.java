package nl.juiced.guhs.feature.knuffelbad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knuffelbad.BadeendjeEntity;
import nl.juiced.guhs.feature.knuffelbad.Eendsoort;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * A rubber duck (geo/entity/badeendje.geo.json), in its kind's colours (textures/entity/badeendje_&lt;kind&gt;.png) with the
 * kind's little extras (a cap, a snorkel, guh ears...). It bobs and turns slowly; the glowing kinds shine in the dark
 * (and every duck is a bit easier to see in the star tunnel). The rider's own game hides a duck as soon as it's picked up.
 */
public class BadeendjeRenderer extends GeoEntityRenderer<BadeendjeEntity> {
    public BadeendjeRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<BadeendjeEntity>(Guhs.id("badeendje")) {
            @Override
            public ResourceLocation getTextureResource(BadeendjeEntity duck) {
                return Guhs.id("textures/entity/badeendje_" + duck.getSoort().id() + ".png");
            }
        });
        this.shadowRadius = 0.2f;
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, BadeendjeEntity duck, BakedGeoModel model, RenderType renderType, MultiBufferSource buffers, VertexConsumer buffer,
                               float partialTick, int packedLight, int packedOverlay) {
                if (duck.getSoort().glimt) {
                    RenderType glow = RenderType.eyes(Guhs.id("textures/entity/badeendje_" + duck.getSoort().id() + "_glow.png"));
                    getRenderer().reRender(model, pose, buffers, duck, glow, buffers.getBuffer(glow), partialTick, LightTexture.FULL_BRIGHT,
                            packedOverlay, 0xFFFFFFFF);
                }
            }
        });
    }

    @Override
    public void render(BadeendjeEntity duck, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (duck.lokaalGepakt) {
            return;
        }
        // never pitch dark: a duck in the star tunnel should still be found
        int block = Math.max(LightTexture.block(packedLight), 7);
        super.render(duck, entityYaw, partialTick, poseStack, bufferSource, LightTexture.pack(block, LightTexture.sky(packedLight)));
    }

    @Override
    protected void applyRotations(BadeendjeEntity duck, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        float t = ageInTicks + duck.getId() * 13;
        poseStack.translate(0, Math.sin(t * 0.12) * 0.05, 0);
        float spin = duck.vanRit() ? (float) Math.sin(t * 0.05) * 25f : 0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - duck.getYRot() + spin));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(t * 0.09) * 6f));
    }

    @Override
    public void preRender(PoseStack poseStack, BadeendjeEntity duck, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender) {
            Eendsoort soort = duck.getSoort();
            for (GeoBone bone : model.topLevelBones()) {
                toon(bone, soort);
            }
        }
        super.preRender(poseStack, duck, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    /** The extra bones (eend_*) only for the kinds that have them. */
    private static void toon(GeoBone bone, Eendsoort soort) {
        String name = bone.getName();
        if (name.startsWith("eend_")) {
            bone.setHidden(soort.bones.stream().noneMatch(name::startsWith));
        }
        for (GeoBone child : bone.getChildBones()) {
            toon(child, soort);
        }
    }

    @Override
    public float getMotionAnimThreshold(BadeendjeEntity animatable) {
        return Mth.EPSILON;
    }
}
