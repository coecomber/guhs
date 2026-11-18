package nl.juiced.guhs.feature.guhwinter.tekenen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.feature.guhwinter.tekenen.TekeningEntity;

/**
 * The hung 2x2 drawing: the framed picture of {@link TekeningCache} on the front, plain wood on the back and the edges, one
 * sixteenth of a block thick (like a painting), lit like the spot it hangs in.
 */
public class TekeningRenderer extends EntityRenderer<TekeningEntity, TekeningRenderer.State> {
    private static final float D = 1f / 32f;
    /** A wood pixel of the frame (the back and the edges). */
    private static final float HU = 2.5f / TekeningCache.B;

    public static class State extends EntityRenderState {
        Direction richting = Direction.SOUTH;
        Identifier tex = TekeningCache.LEEG;
    }

    public TekeningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TekeningEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.richting = entity.getDirection();
        state.tex = TekeningCache.tex(entity.tekeningId());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.richting.get2DDataValue() * 90));
        int licht = state.lightCoords;
        float h = TekeningEntity.MAAT / 2f;
        collector.submitCustomGeometry(pose, RenderTypes.entitySolidZOffsetForward(state.tex), (p, vc) -> {
            // the front (towards the room): local +x = the left edge of the picture, +y = the top
            v(vc, p, h, -h, -D, 0, 1, 0, 0, -1, licht);
            v(vc, p, -h, -h, -D, 1, 1, 0, 0, -1, licht);
            v(vc, p, -h, h, -D, 1, 0, 0, 0, -1, licht);
            v(vc, p, h, h, -D, 0, 0, 0, 0, -1, licht);
            // the back
            v(vc, p, h, h, D, HU, HU, 0, 0, 1, licht);
            v(vc, p, -h, h, D, HU, HU, 0, 0, 1, licht);
            v(vc, p, -h, -h, D, HU, HU, 0, 0, 1, licht);
            v(vc, p, h, -h, D, HU, HU, 0, 0, 1, licht);
            // top, bottom, the two sides
            v(vc, p, h, h, -D, HU, HU, 0, 1, 0, licht);
            v(vc, p, -h, h, -D, HU, HU, 0, 1, 0, licht);
            v(vc, p, -h, h, D, HU, HU, 0, 1, 0, licht);
            v(vc, p, h, h, D, HU, HU, 0, 1, 0, licht);
            v(vc, p, h, -h, D, HU, HU, 0, -1, 0, licht);
            v(vc, p, -h, -h, D, HU, HU, 0, -1, 0, licht);
            v(vc, p, -h, -h, -D, HU, HU, 0, -1, 0, licht);
            v(vc, p, h, -h, -D, HU, HU, 0, -1, 0, licht);
            v(vc, p, h, h, D, HU, HU, -1, 0, 0, licht);
            v(vc, p, h, -h, D, HU, HU, -1, 0, 0, licht);
            v(vc, p, h, -h, -D, HU, HU, -1, 0, 0, licht);
            v(vc, p, h, h, -D, HU, HU, -1, 0, 0, licht);
            v(vc, p, -h, h, -D, HU, HU, 1, 0, 0, licht);
            v(vc, p, -h, -h, -D, HU, HU, 1, 0, 0, licht);
            v(vc, p, -h, -h, D, HU, HU, 1, 0, 0, licht);
            v(vc, p, -h, h, D, HU, HU, 1, 0, 0, licht);
        });
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float vv, int nx, int ny, int nz, int licht) {
        vc.addVertex(p, x, y, z).setColor(-1).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(licht).setNormal(p, nx, ny, nz);
    }
}
