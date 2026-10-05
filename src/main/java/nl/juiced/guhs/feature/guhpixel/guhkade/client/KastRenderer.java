package nl.juiced.guhs.feature.guhpixel.guhkade.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhpixel.guhkade.KastBlockEntity;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Doek;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Schermen;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Spel;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sprite;

/**
 * The little screen on a Guhkade cabinet: it really moves. While a guh plays you see its game (the same simulation with
 * the same seed on every client), otherwise the attract demo and the title with the cabinet's best score. The picture is
 * a pile of small quads from the sprite sheet (a white pixel of it for the plain rectangles), drawn full bright just in
 * front of the screen of the block model.
 */
public class KastRenderer implements BlockEntityRenderer<KastBlockEntity, KastRenderer.State> {
    /** The screen on the upper half of the model (facing north), in blocks: x from the viewer's left to right, y top to bottom. */
    private static final float LINKS = 14f / 16f, RECHTS = 2f / 16f, BOVEN = 1f + 10.5f / 16f, ONDER = 1f + 1.5f / 16f, Z = 3.8f / 16f;
    private static final float LAAG = 0.00012f;
    private static final double ZICHT = 28.0;

    /** The quads of one frame: 8 numbers each (x0, y0, x1, y1 in game pixels; u0, v0, u1, v1) and a colour. */
    public static class State extends BlockEntityRenderState implements Doek {
        boolean zichtbaar;
        Direction facing = Direction.NORTH;
        float[] quads = new float[8 * 256];
        int[] kleuren = new int[256];
        int aantal;

        private void quad(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int argb) {
            // clip to the screen (sprites may stick out), the texture with it
            if (x1 <= 0 || y1 <= 0 || x0 >= Sim.B || y0 >= Sim.H || x1 <= x0 || y1 <= y0) {
                return;
            }
            float b = x1 - x0, h = y1 - y0, du = u1 - u0, dv = v1 - v0;
            if (x0 < 0) {
                u0 -= x0 / b * du;
                x0 = 0;
            }
            if (y0 < 0) {
                v0 -= y0 / h * dv;
                y0 = 0;
            }
            if (x1 > Sim.B) {
                u1 -= (x1 - Sim.B) / b * du;
                x1 = Sim.B;
            }
            if (y1 > Sim.H) {
                v1 -= (y1 - Sim.H) / h * dv;
                y1 = Sim.H;
            }
            if (aantal >= kleuren.length) {
                if (aantal >= 2048) {
                    return;
                }
                quads = java.util.Arrays.copyOf(quads, quads.length * 2);
                kleuren = java.util.Arrays.copyOf(kleuren, kleuren.length * 2);
            }
            kleuren[aantal] = argb;
            int i = aantal++ * 8;
            quads[i] = x0;
            quads[i + 1] = y0;
            quads[i + 2] = x1;
            quads[i + 3] = y1;
            quads[i + 4] = u0;
            quads[i + 5] = v0;
            quads[i + 6] = u1;
            quads[i + 7] = v1;
        }

        @Override
        public void rect(int x, int y, int b, int h, int argb) {
            float u = (Sprite.WIT_U + 0.5f) / Sprite.BLAD, v = (Sprite.WIT_V + 0.5f) / Sprite.BLAD;
            quad(x, y, x + b, y + h, u, v, u, v, argb);
        }

        @Override
        public void sprite(Sprite s, int x, int y) {
            float f = 1f / Sprite.BLAD;
            quad(x, y, x + s.b, y + s.h, s.u * f, s.v * f, (s.u + s.b) * f, (s.v + s.h) * f, 0xFFFFFFFF);
        }
    }

    public KastRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KastBlockEntity kast, State state, float partialTick, Vec3 camera, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(kast, state, partialTick, camera, breakProgress);
        BlockState block = kast.getBlockState();
        state.aantal = 0;
        state.zichtbaar = kast.getLevel() != null && !kast.isBoven() && block.hasProperty(HorizontalDirectionalBlock.FACING)
                && camera.distanceToSqr(Vec3.atCenterOf(kast.getBlockPos())) < ZICHT * ZICHT;
        if (!state.zichtbaar) {
            return;
        }
        state.facing = block.getValue(HorizontalDirectionalBlock.FACING);
        long tijd = kast.getLevel().getGameTime();
        Spel spel = kast.spel();
        Sim sim = kast.scherm(tijd);
        var top = kast.top();
        int topScore = top.isEmpty() ? 0 : top.get(0).score();
        boolean knipper = tijd / 10 % 2 == 0;
        if (kast.modus() == KastBlockEntity.GUH) {
            sim.teken(state);
            if (sim.af()) {
                Schermen.af(state, KastScherm.woord(sim.stappen() >= Sim.MAX_STAPPEN ? "tijd" : "af"), sim.score(), false);
            }
        } else if (sim.af() || sim.stappen() == 0) {
            // between two demo rounds: the title
            Schermen.titel(state, spel.nieuw(sim.seed()), KastScherm.woord("naam." + spel.id), KastScherm.woord("top"), topScore, KastScherm.woord("start"), knipper);
        } else {
            sim.teken(state);
            if (knipper) {
                nl.juiced.guhs.feature.guhpixel.guhkade.spel.PixelFont.midden(state, KastScherm.woord("demo"), Sim.B / 2, Sim.H - 22, 2, 0xFFFF9AC8);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.zichtbaar || state.aantal == 0) {
            return;
        }
        final float[] q = state.quads;
        final int[] kleuren = state.kleuren;
        final int n = state.aantal;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(state.facing.toYRot() + 180f)));
        pose.translate(-0.5, 0, -0.5);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(KastScherm.BLAD), (last, vc) -> {
            float sx = (RECHTS - LINKS) / Sim.B, sy = (ONDER - BOVEN) / Sim.H;
            for (int k = 0; k < n; k++) {
                int i = k * 8;
                float x0 = LINKS + q[i] * sx, y0 = BOVEN + q[i + 1] * sy, x1 = LINKS + q[i + 2] * sx, y1 = BOVEN + q[i + 3] * sy;
                float z = Z - k * LAAG;
                int argb = kleuren[k];
                punt(vc, last, x0, y0, z, q[i + 4], q[i + 5], argb);
                punt(vc, last, x0, y1, z, q[i + 4], q[i + 7], argb);
                punt(vc, last, x1, y1, z, q[i + 6], q[i + 7], argb);
                punt(vc, last, x1, y0, z, q[i + 6], q[i + 5], argb);
            }
        });
        pose.popPose();
    }

    private static void punt(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int argb) {
        vc.addVertex(p, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, 0, 0, -1);
    }

    @Override
    public AABB getRenderBoundingBox(KastBlockEntity kast) {
        return new AABB(kast.getBlockPos()).expandTowards(0, 1, 0);
    }
}
