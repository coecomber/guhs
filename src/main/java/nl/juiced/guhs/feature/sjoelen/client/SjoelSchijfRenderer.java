package nl.juiced.guhs.feature.sjoelen.client;

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
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.sjoelen.SjoelSchijfEntity;

/** A sjoelschijf: a round wooden puck (two crossed boxes) with a little guh face on top, turning as it slides. */
public class SjoelSchijfRenderer extends EntityRenderer<SjoelSchijfEntity, SjoelSchijfRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Guhs.id("sjoelschijf"), "main");
    private static final Identifier HOUT = Guhs.id("textures/entity/sjoelschijf.png"), ROZE = Guhs.id("textures/entity/sjoelschijf_roze.png");
    private final ModelPart puck;

    /** 26.1 render state: what the renderer needs from the puck. */
    public static class State extends EntityRenderState {
        float draai;
        boolean roze;
    }

    public SjoelSchijfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.puck = context.bakeLayer(LAYER);
        this.shadowRadius = 0.18f;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("puck", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5f, -2f, -2.5f, 7, 2, 5)
                .texOffs(0, 8).addBox(-2.5f, -1.8f, -3.5f, 5, 1.8f, 7), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SjoelSchijfEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.draai = entity.draai();
        state.roze = entity.kleur() == 1;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(state.draai));
        pose.scale(-1, -1, 1);
        collector.submitModelPart(puck, pose, RenderTypes.entityCutout(state.roze ? ROZE : HOUT), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
