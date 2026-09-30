package nl.juiced.guhs.feature.knabbelspelen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.knabbelspelen.SpelDing;
import nl.juiced.guhs.feature.knabbelspelen.Spijkerpoepen;

/**
 * Draws the Knabbelspelen's moving things: the item (a kaasknabbel - golden or not -, the knabbelspijker, the tail)
 * and a thin string: from a swinging knabbel up to the beam, from the spijker up to its player's guh belt.
 * <p>
 * 1.1.0: render state + submit; the item is an {@link ItemStackRenderState} made at extract time, the string is custom
 * geometry (same quads as before).
 */
public class DingRenderer extends EntityRenderer<SpelDing, DingRenderer.State> {
    private static final Identifier TOUW = Identifier.withDefaultNamespace("textures/block/white_wool.png");

    public static class State extends EntityRenderState {
        int soort;
        final ItemStackRenderState item = new ItemStackRenderState();
        /** The string's other end (relative to the thing), and for the spijker its hanging angles. */
        Vec3 touw = Vec3.ZERO;
        float yRot;
    }

    public DingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpelDing ding, State state, float partial) {
        super.extractRenderState(ding, state, partial);
        Vec3 hier = ding.getPosition(partial);
        state.soort = ding.soort();
        state.yRot = ding.getYRot();
        ItemDisplayContext context = switch (state.soort) {
            case SpelDing.HANGKNABBEL -> {
                state.touw = ding.touw().subtract(hier);
                yield ItemDisplayContext.GROUND;
            }
            case SpelDing.SPIJKER -> {
                Entity eigenaar = ding.level().getEntity(ding.eigenaar());
                state.touw = eigenaar instanceof Player p ? riem(p, partial).subtract(hier) : new Vec3(0, Spijkerpoepen.LENGTE, 0);
                yield ItemDisplayContext.FIXED;
            }
            default -> ItemDisplayContext.FIXED;
        };
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.item, ding.stack(), context, ding.level(), null, ding.getId());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        switch (state.soort) {
            case SpelDing.HANGKNABBEL -> {
                touw(pose, collector, Vec3.ZERO.add(0, 0.2, 0), state.touw, 0xFFF4EEDC, light);
                pose.pushPose();
                pose.translate(0, 0.05, 0);
                pose.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 4f));
                pose.scale(1.4f, 1.4f, 1.4f);
                state.item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                pose.popPose();
            }
            case SpelDing.SPIJKER -> {
                Vec3 riem = state.touw;
                touw(pose, collector, new Vec3(0, 0.45, 0), riem, 0xFFB08050, light);
                pose.pushPose();
                // the spijker hangs along its string, tip down
                double hoekX = Math.atan2(riem.z, riem.y), hoekZ = Math.atan2(riem.x, riem.y);
                pose.translate(0, 0.22, 0);
                pose.mulPose(Axis.XP.rotation((float) hoekX));
                pose.mulPose(Axis.ZP.rotation((float) -hoekZ));
                pose.mulPose(Axis.ZP.rotationDegrees(-45));
                pose.scale(0.7f, 0.7f, 0.7f);
                state.item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                pose.popPose();
            }
            default -> {
                pose.pushPose();
                pose.translate(0, 0.25, 0);
                pose.mulPose(Axis.YP.rotationDegrees(180 - state.yRot));
                pose.scale(0.8f, 0.8f, 0.8f);
                state.item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                pose.popPose();
            }
        }
        super.submit(state, pose, collector, camera);
    }

    /** The player's guh belt, smoothly (like Spijkerpoepen.riem, between two ticks). */
    static Vec3 riem(Player p, float partial) {
        double yaw = Math.toRadians(Mth.lerp(partial, p.yRotO, p.getYRot()));
        Vec3 achter = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(Spijkerpoepen.ACHTER);
        return p.getPosition(partial).add(achter).add(0, p.isCrouching() ? Spijkerpoepen.RIEM_GEBUKT : Spijkerpoepen.RIEM, 0);
    }

    /** A thin string (a little square tube) from a to b (relative to the entity). */
    static void touw(PoseStack pose, SubmitNodeCollector collector, Vec3 a, Vec3 b, int argb, int light) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1e-6) {
            return;
        }
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TOUW), (last, vc) -> {
            Vec3 n = dir.normalize();
            Vec3 side = Math.abs(n.y) > 0.9 ? new Vec3(1, 0, 0) : n.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 up = side.cross(n).normalize();
            double r = 0.018;
            Vec3[] hoek = {side.scale(r).add(up.scale(r)), side.scale(-r).add(up.scale(r)), side.scale(-r).add(up.scale(-r)), side.scale(r).add(up.scale(-r))};
            for (int i = 0; i < 4; i++) {
                Vec3 h0 = hoek[i], h1 = hoek[(i + 1) % 4];
                Vec3 normaal = h0.add(h1).normalize();
                vertex(vc, last, a.add(h0), 0, 0, normaal, argb, light);
                vertex(vc, last, b.add(h0), 0, 1, normaal, argb, light);
                vertex(vc, last, b.add(h1), 1, 1, normaal, argb, light);
                vertex(vc, last, a.add(h1), 1, 0, normaal, argb, light);
            }
        });
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, Vec3 n, int argb, int light) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb).setUv(u * 0.1f, v * 0.1f).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }

    @Override
    public boolean shouldRender(SpelDing ding, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;                                            // (the string can be long: never cull it away)
    }
}
