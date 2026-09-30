package nl.juiced.guhs.feature.katapult.client;

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
import nl.juiced.guhs.feature.katapult.PluisbalEntity;

/**
 * The pluisbal: a big fluffy pink ball (a cube with fluff tufts on every side and two little guh ears), tumbling as it flies.
 * <p>
 * 1.1.0: render state + submit ({@code submitModelPart}); {@code RenderTypes.entityCutout} is 1.0.0's no-cull cutout.
 */
public class PluisbalRenderer extends EntityRenderer<PluisbalEntity, PluisbalRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Guhs.id("pluisbal"), "main");
    private static final Identifier TEXTURE = Guhs.id("textures/entity/pluisbal.png");
    private final ModelPart ball;

    public static class State extends EntityRenderState {
        float spin;
        int id;
    }

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
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PluisbalEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.spin = Mth.lerp(partialTick, entity.oSpin, entity.spin);
        state.id = entity.getId();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0, PluisbalEntity.SIZE / 2, 0);
        pose.mulPose(Axis.YP.rotationDegrees(state.id * 37 % 360));
        pose.mulPose(Axis.XP.rotation(state.spin));
        pose.scale(-1, -1, 1);
        collector.submitModelPart(ball, pose, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
