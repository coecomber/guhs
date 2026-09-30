package nl.juiced.guhs.feature.golf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.golf.GolfBallEntity;

/**
 * The guh golf ball: a little pink guh curled up into a ball (with ears and a face), rolling as it goes.
 * <p>
 * 1.1.0: render state (the roll of this frame) + submit.
 */
public class GolfBallRenderer extends EntityRenderer<GolfBallEntity, GolfBallRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Guhs.id("guh_golfbal"), "main");
    private static final Identifier TEXTURE = Guhs.id("textures/entity/guh_golfbal.png");
    private final ModelPart ball;

    public static class State extends EntityRenderState {
        float rollYaw;
        float roll;
    }

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
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GolfBallEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.rollYaw = entity.rollYaw;
        state.roll = Mth.lerp(partialTick, entity.oRoll, entity.roll);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0, GolfBallEntity.SIZE / 2, 0);
        pose.mulPose(Axis.YP.rotation(state.rollYaw));
        pose.mulPose(Axis.XP.rotation(state.roll));
        pose.scale(-1, -1, 1);
        collector.submitModelPart(ball, pose, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
