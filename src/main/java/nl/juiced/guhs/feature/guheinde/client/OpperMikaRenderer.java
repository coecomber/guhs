package nl.juiced.guhs.feature.guheinde.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.OpperMikaEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/** Opper-Mika: the Mika model in his own dark colours, with the Knabbelkroon (its 3D item model) on his head. */
public class OpperMikaRenderer extends GeoEntityRenderer<OpperMikaEntity> {
    private static final ResourceLocation TEXTURE = Guhs.id("textures/entity/opper_mika.png");

    public OpperMikaRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<OpperMikaEntity>(Guhs.id("mika"), true) {
            @Override
            public ResourceLocation getTextureResource(OpperMikaEntity mika) {
                return TEXTURE;
            }
        }.withAltAnimations(Guhs.id("guh")));
        this.shadowRadius = 0.8f;
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            private ItemStack crown;

            @Override
            protected ItemStack getStackForBone(GeoBone bone, OpperMikaEntity mika) {
                if (!bone.getName().equals("head")) {
                    return null;
                }
                if (crown == null) {
                    crown = new ItemStack(GuheindeFeature.KNABBELKROON.get());
                }
                return crown;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, OpperMikaEntity mika) {
                return ItemDisplayContext.HEAD;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, OpperMikaEntity mika,
                                              MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                poseStack.translate(0, 0.62, -0.1);            // on top of the head, between the ears
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.scale(0.7f, 0.7f, 0.7f);
                super.renderStackForBone(poseStack, bone, stack, mika, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
    }
}
