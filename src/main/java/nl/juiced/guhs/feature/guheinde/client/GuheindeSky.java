package nl.juiced.guhs.feature.guheinde.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SkyDraw;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * The Guheinde sky: like the End's, but a slow purple-pink swirl of knabbel crumbs (textures/environment/guheinde_sky.png).
 * <p>
 * 1.1.0 (MC 26.1, owner R): a NeoForge {@link CustomSkyboxRenderer}, registered as {@code guhs:guheinde} by
 * {@code client.GuhmensionSky#register}; the dimension type sets {@code "skybox": "end"} and the attribute
 * {@code "neoforge:custom_skybox": "guhs:guheinde"}. The fog colour (0.24, 0.12, 0.2) is the attribute {@code visual/fog_color}.
 */
public class GuheindeSky implements CustomSkyboxRenderer {
    private static final Identifier SKY = Guhs.id("textures/environment/guheinde_sky.png");
    private static final int COLOUR = 0xFF6A3A64;

    @Override
    public boolean renderSky(LevelRenderState level, SkyRenderState sky, Matrix4fc modelView, Runnable setupFog) {
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float drift = (level.gameTime + partialTick) * 0.0004f;
        // the six faces of the End box, uv 0..8 (+ the drift) instead of vanilla's 0..16
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < 6; i++) {
            Matrix4f m = new Matrix4f();
            switch (i) {
                case 1 -> m.rotationX((float) (Math.PI / 2));
                case 2 -> m.rotationX((float) (-Math.PI / 2));
                case 3 -> m.rotationX((float) Math.PI);
                case 4 -> m.rotationZ((float) (Math.PI / 2));
                case 5 -> m.rotationZ((float) (-Math.PI / 2));
                default -> {
                }
            }
            b.addVertex(m, -100f, -100f, -100f).setUv(drift, 0f).setColor(COLOUR);
            b.addVertex(m, -100f, -100f, 100f).setUv(drift, 8f).setColor(COLOUR);
            b.addVertex(m, 100f, -100f, 100f).setUv(8f + drift, 8f).setColor(COLOUR);
            b.addVertex(m, 100f, -100f, -100f).setUv(8f + drift, 0f).setColor(COLOUR);
        }
        SkyDraw.drawNow("Guheinde sky", RenderPipelines.END_SKY, b.buildOrThrow(), SkyDraw.texture(SKY), SkyDraw.white(1f), modelView);
        return true;
    }
}
