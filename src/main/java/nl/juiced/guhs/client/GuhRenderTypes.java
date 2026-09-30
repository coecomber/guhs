package nl.juiced.guhs.client;

import java.util.function.Function;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import nl.juiced.guhs.Guhs;

/**
 * 1.1.0 (MC 26.1): our own render types.
 * <p>
 * {@link #eyes}: 1.21.1's {@code RenderType.eyes} added the texture to the picture (blend ONE, ONE), so the black parts
 * of our glow textures (guh glows, the Guhtwo's floating glow, badeendjes, vogels) were invisible. 26.1's
 * {@code RenderTypes.eyes} blends by alpha (vanilla's eye textures got an alpha channel), which draws those black
 * pixels as black. This is vanilla's eyes pipeline with the old additive blending.
 */
public final class GuhRenderTypes {
    public static final RenderPipeline EYES_ADDITIVE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Guhs.id("pipeline/eyes_additive"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

    private static final Function<Identifier, RenderType> EYES = Util.memoize(texture -> RenderType.create("guhs_eyes_additive",
            RenderSetup.builder(EYES_ADDITIVE).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));

    private GuhRenderTypes() {
    }

    /** A glow texture added on top (1.21.1 {@code RenderType.eyes}): black = nothing. */
    public static RenderType eyes(Identifier texture) {
        return EYES.apply(texture);
    }
}
