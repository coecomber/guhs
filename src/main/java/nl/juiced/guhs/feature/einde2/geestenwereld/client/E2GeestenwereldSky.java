package nl.juiced.guhs.feature.einde2.geestenwereld.client;

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
 * The sky of the Geestenwereld: no sun and no moon, a slow twilight of lila and teal with a few stars
 * (textures/environment/geestenwereld_lucht.png), and over it pastel curtains of northern light that drift the other way
 * (geestenwereld_noorderlicht.png, see-through). Both are drawn on the six faces of a box round the camera, like the End's
 * sky ({@code GuheindeSky} does the same with one layer). Registered as {@code guhs:geestenwereld} by
 * {@link E2GeestenwereldClient}; the textures come from tools/features/einde2_geestenwereld_tex.py.
 */
public class E2GeestenwereldSky implements CustomSkyboxRenderer {
    private static final Identifier LUCHT = Guhs.id("textures/environment/geestenwereld_lucht.png");
    private static final Identifier NOORDERLICHT = Guhs.id("textures/environment/geestenwereld_noorderlicht.png");

    @Override
    public boolean renderSky(LevelRenderState level, SkyRenderState sky, Matrix4fc modelView, Runnable setupFog) {
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float t = level.gameTime + partialTick;
        laag(LUCHT, 2f, t * 0.00012f, 0xFFFFFFFF, modelView, "Geestenwereld sky");
        laag(NOORDERLICHT, 1.5f, -t * 0.00035f, 0xE0FFFFFF, modelView, "Geestenwereld northern lights");
        return true;
    }

    /** One layer: the texture on the six faces of the box (uv 0..schaal, shifted by drift), tinted by kleur (ARGB). */
    private static void laag(Identifier texture, float schaal, float drift, int kleur, Matrix4fc modelView, String label) {
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
            float u0 = drift + i * 0.37f;
            b.addVertex(m, -100f, -100f, -100f).setUv(u0, 0f).setColor(kleur);
            b.addVertex(m, -100f, -100f, 100f).setUv(u0, schaal).setColor(kleur);
            b.addVertex(m, 100f, -100f, 100f).setUv(u0 + schaal, schaal).setColor(kleur);
            b.addVertex(m, 100f, -100f, -100f).setUv(u0 + schaal, 0f).setColor(kleur);
        }
        SkyDraw.drawNow(label, RenderPipelines.END_SKY, b.buildOrThrow(), SkyDraw.texture(texture), SkyDraw.white(1f), modelView);
    }
}
