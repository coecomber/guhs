package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SkyDraw;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * Drawing helpers of the atmosphere: coloured quads (position_color) in one go per layer, additive ({@link #GLOED}: lights,
 * halos, aurora, beams, rainbow) or normally blended ({@link RenderPipelines#DEBUG_QUADS}: shadow, rain, sky tint). Depth tested,
 * no depth write, no culling, no fog.
 */
final class Teken {
    static final RenderPipeline GLOED = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Guhs.id("pipeline/vr_sfeer_gloed"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .build();

    private Teken() {
    }

    static void pipelines(RegisterRenderPipelinesEvent e) {
        e.registerPipeline(GLOED);
    }

    static BufferBuilder begin() {
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    /** Draw and close (nothing when the builder stayed empty). */
    static void klaar(String label, BufferBuilder b, boolean gloed, Matrix4fc mv) {
        MeshData mesh = b.build();
        if (mesh == null) {
            return;
        }
        SkyDraw.drawNow(label, gloed ? GLOED : RenderPipelines.DEBUG_QUADS, mesh, null, SkyDraw.white(1f), mv);
    }

    static void quad(BufferBuilder b, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, int c0, int c1, int c2, int c3) {
        b.addVertex(p0.x, p0.y, p0.z).setColor(c0);
        b.addVertex(p1.x, p1.y, p1.z).setColor(c1);
        b.addVertex(p2.x, p2.y, p2.z).setColor(c2);
        b.addVertex(p3.x, p3.y, p3.z).setColor(c3);
    }

    /** A soft round glow facing the camera: bright centre {@code kleur}, fading to nothing at radius r (8 wedges). */
    static void halo(BufferBuilder b, float x, float y, float z, float r, int kleur, Vector3f rechts, Vector3f op) {
        int weg = ARGB.color(0, ARGB.red(kleur), ARGB.green(kleur), ARGB.blue(kleur));
        int n = 8;
        float px = x + rechts.x * r, py = y + rechts.y * r, pz = z + rechts.z * r;
        for (int i = 1; i <= n; i++) {
            double a = Math.PI * 2 * i / n;
            float cs = (float) Math.cos(a) * r, sn = (float) Math.sin(a) * r;
            float qx = x + rechts.x * cs + op.x * sn, qy = y + rechts.y * cs + op.y * sn, qz = z + rechts.z * cs + op.z * sn;
            b.addVertex(x, y, z).setColor(kleur);
            b.addVertex(x, y, z).setColor(kleur);
            b.addVertex(px, py, pz).setColor(weg);
            b.addVertex(qx, qy, qz).setColor(weg);
            px = qx;
            py = qy;
            pz = qz;
        }
    }

    static int kleur(float a, float r, float g, float bl) {
        return ARGB.colorFromFloat(Math.max(0f, Math.min(1f, a)), r, g, bl);
    }
}
