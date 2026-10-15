package nl.juiced.guhs.feature.bio.wereld.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import nl.juiced.guhs.client.GuhmensionSky;
import nl.juiced.guhs.client.SkyDraw;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * The sky of the Wolkenweide: the sky of the Guhmensie ({@link GuhmensionSky}: the pink moon, the pink stars), with a
 * warmer sunset and noticeably more stars at night. The biome sets the environment attribute
 * {@code "neoforge:custom_skybox": "guhs:wolkenweide"}; everything extra is scaled by {@link WolkClient#diepte}, which is
 * 0 at the edge of the biome, so walking in or out shows no jump.
 */
public class WolkLucht extends GuhmensionSky {
    /** The glow a sunset is pulled towards: a warm peach. */
    private static final int WARM = 0xFFB070;
    @Nullable
    private GpuBuffer sterren;
    private int indices;

    @Override
    public boolean renderSky(LevelRenderState level, SkyRenderState sky, Matrix4fc modelView, Runnable setupFog) {
        float d = WolkClient.diepte();
        float helder = sky.starBrightness * sky.rainBrightness;
        if (d > 0f) {
            int kleur = sky.sunriseAndSunsetColor;
            int a = Math.min(255, Math.round(ARGB.alpha(kleur) * (1f + 0.45f * d)));
            sky.sunriseAndSunsetColor = ARGB.color(a, Mth.lerpInt(0.55f * d, ARGB.red(kleur), ARGB.red(WARM)), Mth.lerpInt(0.55f * d, ARGB.green(kleur), ARGB.green(WARM)),
                    Mth.lerpInt(0.55f * d, ARGB.blue(kleur), ARGB.blue(WARM)));
            sky.starBrightness = Math.min(1f, sky.starBrightness * (1f + 0.5f * d));
        }
        boolean r = super.renderSky(level, sky, modelView, setupFog);
        if (d > 0f && helder > 0f) {
            if (sterren == null) {
                bouw();
            }
            // a second field of small pale stars, twinkling a little out of step with the pink ones
            float twinkel = 0.8f + 0.2f * Mth.sin(level.gameTime * 0.031f + 1.7f);
            float b = Math.min(1f, helder * 1.6f) * d * twinkel;
            PoseStack pose = new PoseStack();
            pose.mulPose(Axis.YP.rotationDegrees(-90f));
            pose.mulPose(Axis.XP.rotation(sky.starAngle));
            pose.mulPose(Axis.ZP.rotationDegrees(37f));
            SkyDraw.draw("Wolkenweide extra stars", SkyDraw.COLOURED_STARS, sterren, VertexFormat.Mode.QUADS, indices, null, new Vector4f(b, b, b, b),
                    new Matrix4f(modelView).mul(pose.last().pose()));
        }
        return r;
    }

    /** 2600 small stars: white, pale gold, pale blue. */
    private void bouw() {
        RandomSource random = RandomSource.create(26100813L);
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(2600 * 4 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < 2600; i++) {
                float x = random.nextFloat() * 2f - 1f, y = random.nextFloat() * 2f - 1f, z = random.nextFloat() * 2f - 1f;
                float size = 0.08f + random.nextFloat() * 0.09f + (random.nextInt(40) == 0 ? 0.14f : 0f);
                float len = x * x + y * y + z * z;
                if (len <= 0.010f || len >= 1f) {
                    continue;
                }
                org.joml.Vector3f dir = new org.joml.Vector3f(x, y, z).normalize(100f);
                org.joml.Quaternionf rot = new org.joml.Quaternionf().rotateTo(new org.joml.Vector3f(0, 0, -1), dir).rotateZ((float) (random.nextDouble() * Math.PI * 2));
                int soort = random.nextInt(10);
                int kleur = soort < 5 ? ARGB.colorFromFloat(1f, 1f, 1f, 1f) : soort < 8 ? ARGB.colorFromFloat(1f, 1f, 0.93f, 0.74f) : ARGB.colorFromFloat(1f, 0.80f, 0.90f, 1f);
                float[][] hoeken = {{size, -size}, {size, size}, {-size, size}, {-size, -size}};
                for (float[] c : hoeken) {
                    org.joml.Vector3f v = new org.joml.Vector3f(c[0], c[1], 0).rotate(rot).add(dir);
                    b.addVertex(v.x, v.y, v.z).setColor(kleur);
                }
            }
            MeshData mesh = b.buildOrThrow();
            indices = mesh.drawState().indexCount();
            sterren = SkyDraw.upload("Wolkenweide extra stars", mesh);
        }
    }
}
