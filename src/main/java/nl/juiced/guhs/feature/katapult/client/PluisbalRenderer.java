package nl.juiced.guhs.feature.katapult.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.katapult.PluisbalEntity;

import net.minecraft.client.renderer.rendertype.RenderTypes;
/** The pluisbal: a big fluffy pink ball (a cube with fluff tufts on every side and two little guh ears), tumbling as it flies. */
public class PluisbalRenderer extends EntityRenderer<PluisbalEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Guhs.id("pluisbal"), "main");
    private static final Identifier TEXTURE = Guhs.id("textures/entity/pluisbal.png");
    private final ModelPart ball;

    public PluisbalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.ball = context.bakeLayer(LAYER);
        this.shadowRadius = 0.25f;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("bal", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4f, -4f, -4f, 8, 8, 8)
                .texOffs(0, 16).addBox(-3f, -3f, -5f, 6, 6, 10)
                .texOffs(0, 16).addBox(-5f, -3f, -3f, 10, 6, 6)
                .texOffs(0, 16).addBox(-3f, -5f, -3f, 6, 10, 6)
                .texOffs(32, 0).addBox(-3.5f, -6f, -1f, 2, 2, 2)
                .texOffs(32, 0).addBox(1.5f, -6f, -1f, 2, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(PluisbalEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, PluisbalEntity.SIZE / 2, 0);
        float spin = Mth.lerp(partialTick, entity.oSpin, entity.spin);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getId() * 37 % 360));
        pose.mulPose(Axis.XP.rotation(spin));
        pose.scale(-1, -1, 1);
        ball.render(pose, buffers.getBuffer(RenderTypes.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public Identifier getTextureLocation(PluisbalEntity entity) {
        return TEXTURE;
    }
}
