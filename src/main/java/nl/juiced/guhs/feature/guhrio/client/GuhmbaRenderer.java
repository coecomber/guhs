package nl.juiced.guhs.feature.guhrio.client;

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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhrio.GuhmbaEntity;

/**
 * The Guhmba: a blocky little Mika of a few boxes (a round brown body with a grumpy face, two cat ears, two big feet that
 * shuffle), drawn from its own texture (textures/entity/guhrio_guhmba.png, made by tools/features/guhrio.py; the boxes'
 * places on the texture are the constants below). Landed on, it is squashed flat and wide.
 */
public class GuhmbaRenderer extends EntityRenderer<GuhmbaEntity, GuhmbaRenderer.State> {
    private static final Identifier TEX = Guhs.id("textures/entity/guhrio_guhmba.png");
    /** The texture is 64 x 64; each rectangle is {u, v, breedte, hoogte} in pixels. */
    private static final float[] GEZICHT = {0, 0, 12, 9}, VACHT = {16, 0, 12, 9}, BOVEN = {32, 0, 12, 10}, VOET = {0, 16, 6, 6}, OOR = {16, 16, 4, 4};

    public static class State extends EntityRenderState {
        float yaw, plat, loop;
    }

    public GuhmbaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GuhmbaEntity guhmba, State state, float partialTick) {
        super.extractRenderState(guhmba, state, partialTick);
        state.yaw = Mth.rotLerp(partialTick, guhmba.yRotO, guhmba.getYRot());
        state.plat = Mth.lerp(partialTick, guhmba.platheidO, guhmba.platheid);
        state.loop = Mth.lerp(partialTick, guhmba.loopO, guhmba.loop);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        float plat = state.plat;
        pose.scale(Mth.lerp(plat, 1f, 1.3f) / 16f, Mth.lerp(plat, 1f, 0.22f) / 16f, Mth.lerp(plat, 1f, 1.3f) / 16f);
        float stap = (float) Math.sin(state.loop) * (1f - plat);
        int licht = state.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEX), (last, vc) -> {
            // feet (they shuffle), body (it rocks a little), ears; all in pixels, the front is +z
            doos(vc, last, licht, -6, 0, -3 + stap * 1.5f, -1, 3, 4 + stap * 1.5f, VOET, VOET, VOET);
            doos(vc, last, licht, 1, 0, -3 - stap * 1.5f, 6, 3, 4 - stap * 1.5f, VOET, VOET, VOET);
            float wieg = Math.abs(stap) * 0.6f;
            doos(vc, last, licht, -6, 3 + wieg, -5, 6, 12 + wieg, 5, GEZICHT, VACHT, BOVEN);
            doos(vc, last, licht, -5.5f, 12 + wieg, -1, -2.5f, 15 + wieg, 1, OOR, OOR, OOR);
            doos(vc, last, licht, 2.5f, 12 + wieg, -1, 5.5f, 15 + wieg, 1, OOR, OOR, OOR);
        });
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    /** A box from (x0, y0, z0) to (x1, y1, z1): its front (+z) face, its other three sides, its top and bottom. */
    private static void doos(VertexConsumer vc, PoseStack.Pose pose, int licht, float x0, float y0, float z0, float x1, float y1, float z1,
                             float[] voor, float[] zij, float[] boven) {
        vlak(vc, pose, licht, voor, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1);      // front
        vlak(vc, pose, licht, zij, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1);      // back
        vlak(vc, pose, licht, zij, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0);       // its left
        vlak(vc, pose, licht, zij, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0);      // its right
        vlak(vc, pose, licht, boven, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0);     // top
        vlak(vc, pose, licht, zij, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0);      // bottom
    }

    /** One face: bottom left, bottom right, top right, top left (seen from outside). */
    private static void vlak(VertexConsumer vc, PoseStack.Pose pose, int licht, float[] uv, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz) {
        float u0 = uv[0] / 64f, v0 = uv[1] / 64f, u1 = (uv[0] + uv[2]) / 64f, v1 = (uv[1] + uv[3]) / 64f;
        punt(vc, pose, licht, ax, ay, az, u0, v1, nx, ny, nz);
        punt(vc, pose, licht, bx, by, bz, u1, v1, nx, ny, nz);
        punt(vc, pose, licht, cx, cy, cz, u1, v0, nx, ny, nz);
        punt(vc, pose, licht, dx, dy, dz, u0, v0, nx, ny, nz);
    }

    private static void punt(VertexConsumer vc, PoseStack.Pose pose, int licht, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(licht).setNormal(pose, nx, ny, nz);
    }
}
