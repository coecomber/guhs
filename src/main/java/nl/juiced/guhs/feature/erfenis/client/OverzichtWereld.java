package nl.juiced.guhs.feature.erfenis.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.feature.erfenis.Plot;

/**
 * De Erfenis (POC): what the overview draws INTO the world: the build grid while placing or moving, the hover and
 * selection outlines, and the ghosts (green = may, red = may not). Plain coloured quads (see {@link OverzichtScherm#teken}
 * for what is drawn where); everything else (labels, icons, panels) is the screen's 2D work.
 */
public final class OverzichtWereld {
    /** One box to draw this frame: block coords (min inclusive, max exclusive), fill and edge colours (ARGB, 0 = none). */
    record Doos(double x0, double y0, double z0, double x1, double y1, double z1, int vul, int rand, float dikte) {
    }

    private OverzichtWereld() {
    }

    static void teken(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        OverzichtScherm s = OverzichtScherm.huidig();
        if (!Overzicht.actief() || s == null) {
            return;
        }
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderTypes.debugQuads());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        BlockPos o = Overzicht.oorsprong();
        double gy = Overzicht.grondY() + 0.02;
        // the edge of the build area, always (soft pink)
        rand(vc, m, o.getX(), gy, o.getZ(), o.getX() + Plot.MAAT, gy, o.getZ() + Plot.MAAT, 0x90F07CB4, 0.12f);
        if (s.toontRaster()) {
            for (int i = 1; i < Plot.MAAT; i++) {
                int kleur = i % 8 == 0 ? 0x50FFFFFF : 0x26FFFFFF;
                vlak(vc, m, o.getX() + i - 0.02, gy, o.getZ(), o.getX() + i + 0.02, gy, o.getZ() + Plot.MAAT, kleur);
                vlak(vc, m, o.getX(), gy, o.getZ() + i - 0.02, o.getX() + Plot.MAAT, gy, o.getZ() + i + 0.02, kleur);
            }
        }
        for (Doos d : s.dozen()) {
            if ((d.vul() >>> 24) != 0) {
                doos(vc, m, d.x0(), d.y0(), d.z0(), d.x1(), d.y1(), d.z1(), d.vul());
            }
            if ((d.rand() >>> 24) != 0) {
                randen(vc, m, d.x0(), d.y0(), d.z0(), d.x1(), d.y1(), d.z1(), d.rand(), d.dikte());
            }
        }
        pose.popPose();
        buffers.endBatch(RenderTypes.debugQuads());
    }

    /** A flat horizontal rectangle. */
    private static void vlak(VertexConsumer vc, Matrix4f m, double x0, double y, double z0, double x1, double y1, double z1, int argb) {
        vc.addVertex(m, (float) x0, (float) y, (float) z0).setColor(argb);
        vc.addVertex(m, (float) x0, (float) y, (float) z1).setColor(argb);
        vc.addVertex(m, (float) x1, (float) y, (float) z1).setColor(argb);
        vc.addVertex(m, (float) x1, (float) y, (float) z0).setColor(argb);
    }

    /** A rectangle's outline on the ground. */
    private static void rand(VertexConsumer vc, Matrix4f m, double x0, double y, double z0, double x1, double y1, double z1, int argb, float d) {
        vlak(vc, m, x0 - d, y, z0 - d, x1 + d, y, z0 + d, argb);
        vlak(vc, m, x0 - d, y, z1 - d, x1 + d, y, z1 + d, argb);
        vlak(vc, m, x0 - d, y, z0 + d, x0 + d, y, z1 - d, argb);
        vlak(vc, m, x1 - d, y, z0 + d, x1 + d, y, z1 - d, argb);
    }

    /** A see-through box (all six faces). */
    static void doos(VertexConsumer vc, Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1, int argb) {
        float a = (float) x0, b = (float) y0, c = (float) z0, d = (float) x1, e = (float) y1, f = (float) z1;
        quad(vc, m, argb, a, e, c, a, e, f, d, e, f, d, e, c);   // top
        quad(vc, m, argb, a, b, c, d, b, c, d, b, f, a, b, f);   // bottom
        quad(vc, m, argb, a, b, c, a, e, c, d, e, c, d, b, c);   // north
        quad(vc, m, argb, a, b, f, d, b, f, d, e, f, a, e, f);   // south
        quad(vc, m, argb, a, b, c, a, b, f, a, e, f, a, e, c);   // west
        quad(vc, m, argb, d, b, c, d, e, c, d, e, f, d, b, f);   // east
    }

    /** The twelve edges of a box, each a thin box. */
    static void randen(VertexConsumer vc, Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1, int argb, float t) {
        double h = t / 2;
        for (double y : new double[] {y0, y1}) {
            doos(vc, m, x0 - h, y - h, z0 - h, x1 + h, y + h, z0 + h, argb);
            doos(vc, m, x0 - h, y - h, z1 - h, x1 + h, y + h, z1 + h, argb);
            doos(vc, m, x0 - h, y - h, z0 + h, x0 + h, y + h, z1 - h, argb);
            doos(vc, m, x1 - h, y - h, z0 + h, x1 + h, y + h, z1 - h, argb);
        }
        for (double[] p : new double[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
            doos(vc, m, p[0] - h, y0 + h, p[1] - h, p[0] + h, y1 - h, p[1] + h, argb);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int argb, float... p) {
        for (int i = 0; i < 12; i += 3) {
            vc.addVertex(m, p[i], p[i + 1], p[i + 2]).setColor(argb);
        }
    }
}
