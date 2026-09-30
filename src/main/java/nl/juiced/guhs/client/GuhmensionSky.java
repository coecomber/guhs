package nl.juiced.guhs.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guheinde.client.GuheindeSky;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * The Guhmension sky: like the overworld, but with a big pink moon and pink, twinkly stars.
 * <p>
 * 1.1.0 (MC 26.1): {@code DimensionSpecialEffects} is gone. The dimension type (or biome) sets the environment attribute
 * {@code "neoforge:custom_skybox": "guhs:guhmension"} and keeps {@code "skybox": "overworld"}; NeoForge then calls
 * {@link #renderSky} instead of the vanilla sky. Sky colour, sun/moon/star angles, star brightness and the sunrise colour
 * come from the {@link SkyRenderState} vanilla already extracted from the environment attributes. Fog colour and cloud
 * height are plain attributes in the JSON now (they were the overworld defaults anyway).
 */
public class GuhmensionSky implements CustomSkyboxRenderer {
    private static final Identifier SUN = Identifier.withDefaultNamespace("textures/environment/celestial/sun.png");
    private static final Identifier MOON = Guhs.id("textures/environment/pink_moon.png");
    private static final float SUN_SIZE = 30f;
    private static final float MOON_SIZE = 26f;   // a bit bigger than the overworld's (20)

    /** Vanilla's helper for the sky disc and the sunrise fan (its own buffers; nothing of it depends on resource packs). */
    @Nullable
    private SkyRenderer vanilla;
    @Nullable
    private GpuBuffer stars;
    private int starIndices;

    /** Mod bus (client): the Guhmension and Guheinde skies. */
    public static void register(RegisterCustomEnvironmentEffectRendererEvent event) {
        event.registerSkyboxRenderer(Guhs.id("guhmension"), new GuhmensionSky());
        event.registerSkyboxRenderer(Guhs.id("guheinde"), new GuheindeSky());
    }

    @Override
    public boolean renderSky(LevelRenderState level, SkyRenderState sky, Matrix4fc modelView, Runnable setupFog) {
        setupFog.run();
        Minecraft mc = Minecraft.getInstance();
        if (vanilla == null) {
            vanilla = new SkyRenderer(mc.getTextureManager(), mc.getAtlasManager());
            buildStars();
        }
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        // the sky colour and the sunrise/sunset glow: as in the overworld
        vanilla.renderSkyDisc(sky.skyColor);
        PoseStack pose = new PoseStack();
        vanilla.renderSunriseAndSunset(pose, sky.sunAngle, sky.sunriseAndSunsetColor);

        // sun, pink moon, pink stars
        float clear = sky.rainBrightness;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90f));

        pose.pushPose();
        pose.mulPose(Axis.XP.rotation(sky.sunAngle));
        SkyDraw.drawNow("Guhmension sun", RenderPipelines.CELESTIAL, quad(SUN_SIZE, false), SkyDraw.texture(SUN), SkyDraw.white(clear),
                matrix(modelView, pose));
        pose.popPose();

        // the moon: always full, drawn with normal blending so its pink shows
        pose.pushPose();
        pose.mulPose(Axis.XP.rotation(sky.moonAngle));
        SkyDraw.drawNow("Guhmension pink moon", SkyDraw.CELESTIAL_TRANSLUCENT, quad(MOON_SIZE, true), SkyDraw.texture(MOON),
                SkyDraw.white(clear), matrix(modelView, pose));
        pose.popPose();

        float brightness = sky.starBrightness * clear;
        if (brightness > 0f && stars != null) {
            // twinkle: the whole field breathes a little
            float twinkle = 0.85f + 0.15f * Mth.sin((level.gameTime + partialTick) * 0.05f);
            float b = brightness * twinkle;
            pose.pushPose();
            pose.mulPose(Axis.XP.rotation(sky.starAngle));
            SkyDraw.draw("Guhmension pink stars", SkyDraw.COLOURED_STARS, stars, VertexFormat.Mode.QUADS, starIndices, null,
                    new Vector4f(b, b, b, brightness), matrix(modelView, pose));
            pose.popPose();
        }
        pose.popPose();
        return true;
    }

    private static Matrix4f matrix(Matrix4fc modelView, PoseStack pose) {
        return new Matrix4f(modelView).mul(pose.last().pose());
    }

    /** A celestial quad at y = 100 facing down to the player (the moon's texture turned like vanilla's moon). */
    private static MeshData quad(float size, boolean moon) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float y = 100f;
        if (moon) {
            b.addVertex(-size, y, -size).setUv(1, 1);
            b.addVertex(size, y, -size).setUv(0, 1);
            b.addVertex(size, y, size).setUv(0, 0);
            b.addVertex(-size, y, size).setUv(1, 0);
        } else {
            b.addVertex(-size, y, -size).setUv(0, 0);
            b.addVertex(size, y, -size).setUv(1, 0);
            b.addVertex(size, y, size).setUv(1, 1);
            b.addVertex(-size, y, size).setUv(0, 1);
        }
        return b.buildOrThrow();
    }

    /** Pink stars: 1800 little quads in pinks, lilacs and a few white ones (same seed and colours as 1.0.0). */
    private void buildStars() {
        RandomSource random = RandomSource.create(10842L);
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(1800 * 4 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < 1800; i++) {
                float x = random.nextFloat() * 2f - 1f, y = random.nextFloat() * 2f - 1f, z = random.nextFloat() * 2f - 1f;
                float size = 0.15f + random.nextFloat() * 0.12f;
                float len = x * x + y * y + z * z;
                if (len <= 0.010f || len >= 1f) {
                    continue;
                }
                org.joml.Vector3f dir = new org.joml.Vector3f(x, y, z).normalize(100f);
                float spin = (float) (random.nextDouble() * Math.PI * 2);
                org.joml.Quaternionf rot = new org.joml.Quaternionf().rotateTo(new org.joml.Vector3f(0, 0, -1), dir).rotateZ(spin);
                int kind = random.nextInt(10);
                float r = 1f, g = kind < 6 ? 0.55f + random.nextFloat() * 0.2f : kind < 9 ? 0.7f : 1f, bl = kind < 6 ? 0.8f : 1f;
                int colour = ARGB.colorFromFloat(1f, r, g, bl);
                float[][] corners = {{size, -size}, {size, size}, {-size, size}, {-size, -size}};
                for (float[] c : corners) {
                    org.joml.Vector3f v = new org.joml.Vector3f(c[0], c[1], 0).rotate(rot).add(dir);
                    b.addVertex(v.x, v.y, v.z).setColor(colour);
                }
            }
            MeshData mesh = b.buildOrThrow();
            starIndices = mesh.drawState().indexCount();
            stars = SkyDraw.upload("Guhmension pink stars", mesh);
        }
    }
}
