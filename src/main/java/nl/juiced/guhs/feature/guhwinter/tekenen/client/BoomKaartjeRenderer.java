package nl.juiced.guhs.feature.guhwinter.tekenen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.feature.guhwinter.tekenen.BoomKaartje;

/** A little card around the kerstboom: the framed drawing on both sides (readable from both), swinging softly. */
public class BoomKaartjeRenderer extends EntityRenderer<BoomKaartje, BoomKaartjeRenderer.State> {
    private static final float H = 0.32f, D = 0.01f;

    public static class State extends EntityRenderState {
        Identifier tex = TekeningCache.LEEG;
        float draai;
        float fase;
    }

    public BoomKaartjeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BoomKaartje entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tex = TekeningCache.tex(entity.tekeningId());
        state.draai = entity.getYRot();
        state.fase = (entity.getId() % 17) * 0.7f;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0, H, 0);
        float t = state.ageInTicks * 0.05f + state.fase;
        pose.mulPose(Axis.YP.rotationDegrees(-state.draai + Mth.sin(t) * 14f));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 1.3f) * 4f));
        int licht = state.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entitySolid(state.tex), (p, vc) -> {
            TekeningRenderer.v(vc, p, H, -H, -D, 0, 1, 0, 0, -1, licht);
            TekeningRenderer.v(vc, p, -H, -H, -D, 1, 1, 0, 0, -1, licht);
            TekeningRenderer.v(vc, p, -H, H, -D, 1, 0, 0, 0, -1, licht);
            TekeningRenderer.v(vc, p, H, H, -D, 0, 0, 0, 0, -1, licht);
            // the back: the same picture, mirrored so it reads the right way round from behind
            TekeningRenderer.v(vc, p, H, H, D, 1, 0, 0, 0, 1, licht);
            TekeningRenderer.v(vc, p, -H, H, D, 0, 0, 0, 0, 1, licht);
            TekeningRenderer.v(vc, p, -H, -H, D, 0, 1, 0, 0, 1, licht);
            TekeningRenderer.v(vc, p, H, -H, D, 1, 1, 0, 0, 1, licht);
        });
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
