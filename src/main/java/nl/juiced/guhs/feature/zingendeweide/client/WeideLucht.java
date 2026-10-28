package nl.juiced.guhs.feature.zingendeweide.client;

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
 * The sky of De Zingende Weide: the Guhmension's sky, and at night soft rainbow light pillars rising from the horizon all
 * around (like the Sift's), breathing slowly. They stand still against the horizon (they don't turn with the stars).
 */
public class WeideLucht extends GuhmensionSky {
    private static final int ZUILEN = 14, STUKKEN = 12;
    private static final float[][] KLEUREN = {{1f, 0.45f, 0.7f}, {1f, 0.7f, 0.35f}, {1f, 0.95f, 0.45f}, {0.45f, 1f, 0.7f}, {0.4f, 0.75f, 1f},
            {0.7f, 0.5f, 1f}};
    @Nullable
    private GpuBuffer zuilen;
    private int indices;

    @Override
    public boolean renderSky(LevelRenderState level, SkyRenderState sky, Matrix4fc modelView, Runnable setupFog) {
        boolean r = super.renderSky(level, sky, modelView, setupFog);
        float nacht = sky.starBrightness * sky.rainBrightness;
        if (nacht > 0.01f) {
            if (zuilen == null) {
                bouw();
            }
            float adem = 0.75f + 0.25f * Mth.sin(level.gameTime * 0.013f);
            float b = Math.min(1f, nacht * 1.8f) * adem;
            PoseStack pose = new PoseStack();
            pose.mulPose(Axis.YP.rotationDegrees(-90f));
            SkyDraw.draw("Zingende Weide light pillars", SkyDraw.COLOURED_STARS, zuilen, VertexFormat.Mode.QUADS, indices, null, new Vector4f(b, b, b, b),
                    new Matrix4f(modelView).mul(pose.last().pose()));
        }
        return r;
    }

    /** The pillars: thin bands on the sky dome from the horizon up, bright at the bottom, fading towards the top and the sides. */
    private void bouw() {
        RandomSource random = RandomSource.create(20181028L);
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(ZUILEN * STUKKEN * 3 * 4 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < ZUILEN; i++) {
                float hoek = (float) (i * Math.PI * 2 / ZUILEN + random.nextFloat() * 0.3f);
                float breed = 0.025f + random.nextFloat() * 0.03f;
                float hoog = (float) Math.toRadians(30 + random.nextInt(35));
                float[] k = KLEUREN[random.nextInt(KLEUREN.length)];
                float kracht = 0.35f + random.nextFloat() * 0.3f;
                // three side by side bands per pillar: a bright core and two soft edges
                for (int band = -1; band <= 1; band++) {
                    float a0 = hoek + band * breed * 0.9f - breed * 0.5f, a1 = a0 + breed;
                    float rand = band == 0 ? 1f : 0.35f;
                    for (int s = 0; s < STUKKEN; s++) {
                        float e0 = hoog * s / STUKKEN - 0.03f, e1 = hoog * (s + 1) / STUKKEN - 0.03f;
                        int c0 = kleur(k, kracht * rand * (1f - s / (float) STUKKEN)), c1 = kleur(k, kracht * rand * (1f - (s + 1) / (float) STUKKEN));
                        punt(b, a0, e0, c0);
                        punt(b, a1, e0, c0);
                        punt(b, a1, e1, c1);
                        punt(b, a0, e1, c1);
                    }
                }
            }
            MeshData mesh = b.buildOrThrow();
            indices = mesh.drawState().indexCount();
            zuilen = SkyDraw.upload("Zingende Weide light pillars", mesh);
        }
    }

    private static int kleur(float[] k, float sterkte) {
        float s = Mth.clamp(sterkte, 0f, 1f);
        return ARGB.colorFromFloat(1f, k[0] * s, k[1] * s, k[2] * s);
    }

    private static void punt(BufferBuilder b, float azimut, float elevatie, int kleur) {
        float r = 100f;
        float x = Mth.cos(elevatie) * Mth.cos(azimut) * r, y = Mth.sin(elevatie) * r, z = Mth.cos(elevatie) * Mth.sin(azimut) * r;
        b.addVertex(x, y, z).setColor(kleur);
    }
}
