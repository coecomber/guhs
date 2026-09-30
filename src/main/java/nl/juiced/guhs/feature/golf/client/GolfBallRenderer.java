package nl.juiced.guhs.feature.golf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.golf.GolfBallEntity;

/** The guh golf ball: a little pink guh curled up into a ball (with ears and a face), rolling as it goes. */
public class GolfBallRenderer extends EntityRenderer<GolfBallEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Guhs.id("guh_golfbal"), "main");
    private static final ResourceLocation TEXTURE = Guhs.id("textures/entity/guh_golfbal.png");
    private final ModelPart ball;

    public GolfBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.ball = context.bakeLayer(LAYER);
        this.shadowRadius = 0.15f;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("ball", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5f, -2.5f, -2.5f, 5, 5, 5)
                .texOffs(0, 10).addBox(-2.5f, -3.5f, -0.5f, 1, 1, 1)
                .texOffs(4, 10).addBox(1.5f, -3.5f, -0.5f, 1, 1, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public void render(GolfBallEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, GolfBallEntity.SIZE / 2, 0);
        pose.mulPose(Axis.YP.rotation(entity.rollYaw));
        pose.mulPose(Axis.XP.rotation(Mth.lerp(partialTick, entity.oRoll, entity.roll)));
        pose.scale(-1, -1, 1);
        ball.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GolfBallEntity entity) {
        return TEXTURE;
    }
}
