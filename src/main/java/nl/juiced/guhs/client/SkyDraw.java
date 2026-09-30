package nl.juiced.guhs.client;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import javax.annotation.Nullable;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import nl.juiced.guhs.Guhs;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * 1.1.0 (MC 26.1): drawing helpers for our custom skies ({@link GuhmensionSky}, Guheinde). There is no immediate mode
 * (Tesselator + RenderSystem.setShader/setShaderColor) any more: geometry goes into a GPU buffer and is drawn in a
 * {@link RenderPass} with a {@link RenderPipeline} (shader + blending + vertex format) and a colour modulator.
 * Same pattern as vanilla's {@code SkyRenderer}.
 */
public final class SkyDraw {
    /** Textured quad (position_tex) blended normally: the pink moon (vanilla's CELESTIAL pipeline blends additively). */
    public static final RenderPipeline CELESTIAL_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Guhs.id("pipeline/celestial_translucent"))
            .withVertexShader("core/position_tex")
            .withFragmentShader("core/position_tex")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .build();
    /** Coloured quads (position_color) added to the sky like vanilla's stars: the pink stars. */
    public static final RenderPipeline COLOURED_STARS = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Guhs.id("pipeline/coloured_stars"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .build();

    private SkyDraw() {
    }

    /** Mod bus (client): our pipelines must be known before the shaders are compiled. */
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CELESTIAL_TRANSLUCENT);
        event.registerPipeline(COLOURED_STARS);
        event.registerPipeline(GuhRenderTypes.EYES_ADDITIVE);
    }

    public static AbstractTexture texture(Identifier id) {
        return Minecraft.getInstance().getTextureManager().getTexture(id);
    }

    /** Upload a GPU buffer once (static geometry: stars, discs). The mesh is closed. */
    public static GpuBuffer upload(String label, MeshData mesh) {
        try (mesh) {
            return RenderSystem.getDevice().createBuffer(() -> label, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
        }
    }

    /** Draw a mesh built this frame (uploaded as an immediate buffer, closed afterwards). */
    public static void drawNow(String label, RenderPipeline pipeline, MeshData mesh, @Nullable AbstractTexture texture,
                               Vector4fc colour, Matrix4fc modelView) {
        try (mesh) {
            GpuBuffer vertices = pipeline.getVertexFormat().uploadImmediateVertexBuffer(mesh.vertexBuffer());
            draw(label, pipeline, vertices, mesh.drawState().mode(), mesh.drawState().indexCount(), texture, colour, modelView);
        }
    }

    /** Draw an uploaded buffer ({@code indexCount} as in {@code MeshData.DrawState#indexCount}). */
    public static void draw(String label, RenderPipeline pipeline, GpuBuffer vertices, VertexFormat.Mode mode, int indexCount,
                            @Nullable AbstractTexture texture, Vector4fc colour, Matrix4fc modelView) {
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(modelView, colour, new Vector3f(), new Matrix4f());
        GpuTextureView colourTarget = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
        GpuTextureView depthTarget = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> label, colourTarget, OptionalInt.empty(), depthTarget, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            if (texture != null) {
                pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
            }
            pass.setVertexBuffer(0, vertices);
            if (mode == VertexFormat.Mode.QUADS) {
                RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(mode);
                pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
                pass.drawIndexed(0, 0, indexCount, 1);
            } else {
                pass.draw(0, indexCount);
            }
        }
    }

    public static Vector4f white(float alpha) {
        return new Vector4f(1f, 1f, 1f, alpha);
    }
}
