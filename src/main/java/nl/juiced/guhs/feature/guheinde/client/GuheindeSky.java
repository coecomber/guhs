package nl.juiced.guhs.feature.guheinde.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SkyDraw;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The Guheinde sky: like the End's, but a slow purple-pink swirl of knabbel crumbs (textures/environment/guheinde_sky.png).
 * <p>
 * 1.1.0 (MC 26.1, owner R): a NeoForge {@link CustomSkyboxRenderer}, registered as {@code guhs:guheinde} by
 * {@code client.GuhmensionSky#register}; the dimension type sets {@code "skybox": "end"} and the attribute
 * {@code "neoforge:custom_skybox": "guhs:guheinde"}. The fog colour (0.24, 0.12, 0.2) is the attribute {@code visual/fog_color}.
 * <p>
 * einde2 fix round 1 (playtest 1, item 3: "a bit friendlier, a bit lighter pink, a tiny bit guh-themed, without losing the
 * empty raw feel"): the box is tinted a little lighter and pinker, and three faint things hang in it, all in ONE static
 * buffer built once and drawn with one call (additive, like vanilla's stars): a sparse field of dim pink stars, a guh-face
 * constellation (round head, two ears, two eyes, the little cavia mouth) joined by hair-thin lines, and a soft guh-shaped
 * moon (a pale disc with two ear bumps and a halo that fades out). The whole set breathes very slowly (90-100 %).
 */
public class GuheindeSky implements CustomSkyboxRenderer {
    private static final Identifier SKY = Guhs.id("textures/environment/guheinde_sky.png");
    /** The tint of the swirl (was 0xFF6A3A64: a little lighter and pinker now). */
    private static final int COLOUR = 0xFF94507A;
    /** How far the faint things hang (inside the End box, whose faces are 100 away). */
    private static final float VER = 90f;

    @Nullable
    private GpuBuffer guh;
    private int guhIndices;

    @Override
    public boolean renderSky(LevelRenderState level, SkyRenderState sky, Matrix4fc modelView, Runnable setupFog) {
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float time = level.gameTime + partialTick;
        float drift = time * 0.0004f;
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

        // the faint guh things: one static buffer, one draw call
        if (guh == null) {
            bouwGuh();
        }
        if (guh != null) {
            float adem = 0.95f + 0.05f * Mth.sin(time * 0.01f);
            SkyDraw.draw("Guheinde guh sky", SkyDraw.COLOURED_STARS, guh, VertexFormat.Mode.QUADS, guhIndices, null,
                    new Vector4f(adem, adem, adem, 1f), modelView);
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the static buffer

    /** A spot on the sky: its direction and two axes across it (a small shape there is drawn flat, it is far away). */
    private record Plek(Vector3f mid, Vector3f rechts, Vector3f op) {
        /** {@code az}: degrees around (0 = north, 90 = east), {@code hoog}: degrees above the horizon. */
        static Plek van(float az, float hoog) {
            double a = Math.toRadians(az), h = Math.toRadians(hoog);
            Vector3f dir = new Vector3f((float) (Math.sin(a) * Math.cos(h)), (float) Math.sin(h), (float) (-Math.cos(a) * Math.cos(h)));
            Vector3f rechts = new Vector3f(dir).cross(0f, 1f, 0f).normalize();
            Vector3f op = new Vector3f(rechts).cross(dir).normalize();
            return new Plek(dir.mul(VER), rechts, op);
        }

        /** The point (u, v) on this spot, in blocks across the sky at {@link #VER}. */
        Vector3f punt(float u, float v) {
            return new Vector3f(mid).add(new Vector3f(rechts).mul(u)).add(new Vector3f(op).mul(v));
        }
    }

    private static void quad(BufferBuilder b, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, int c0, int c1, int c2, int c3) {
        b.addVertex(p0.x, p0.y, p0.z).setColor(c0);
        b.addVertex(p1.x, p1.y, p1.z).setColor(c1);
        b.addVertex(p2.x, p2.y, p2.z).setColor(c2);
        b.addVertex(p3.x, p3.y, p3.z).setColor(c3);
    }

    /** One star: a small diamond at (u, v) on the spot. */
    private static void ster(BufferBuilder b, Plek plek, float u, float v, float grootte, int kleur) {
        quad(b, plek.punt(u, v - grootte), plek.punt(u + grootte, v), plek.punt(u, v + grootte), plek.punt(u - grootte, v), kleur, kleur, kleur, kleur);
    }

    /** A hair-thin line from (u0, v0) to (u1, v1) on the spot. */
    private static void lijn(BufferBuilder b, Plek plek, float u0, float v0, float u1, float v1, float dik, int kleur) {
        float du = u1 - u0, dv = v1 - v0, len = (float) Math.hypot(du, dv);
        float nu = -dv / len * dik, nv = du / len * dik;
        // (counter-clockwise as the player sees it: the pipeline culls back faces)
        quad(b, plek.punt(u0 - nu, v0 - nv), plek.punt(u1 - nu, v1 - nv), plek.punt(u1 + nu, v1 + nv), plek.punt(u0 + nu, v0 + nv), kleur, kleur, kleur, kleur);
    }

    /** The guh outline from its middle: the head (radius 1) and two ears; the far edge of whichever circle the ray leaves last. */
    private static float guhRand(double hoek) {
        double cx = Math.cos(hoek), cy = Math.sin(hoek), r = 1.0;
        for (double kant : new double[]{-1, 1}) {
            double ox = kant * 0.62, oy = 0.78, er = 0.36;
            double t = ox * cx + oy * cy, d2 = ox * ox + oy * oy - t * t;
            if (t > 0 && d2 < er * er) {
                r = Math.max(r, t + Math.sqrt(er * er - d2));
            }
        }
        return (float) r;
    }

    private void bouwGuh() {
        RandomSource random = RandomSource.create(20261016L);
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(1200 * 4 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            // 1. a sparse field of dim pink stars over the upper sky (the void below stays empty)
            for (int i = 0; i < 140; i++) {
                float az = random.nextFloat() * 360f, hoog = 8f + (float) Math.toDegrees(Math.asin(random.nextFloat())) * 0.9f;
                float a = 0.18f + random.nextFloat() * 0.22f;
                int kleur = ARGB.colorFromFloat(a, 1f, 0.62f + random.nextFloat() * 0.2f, 0.85f);
                ster(b, Plek.van(az, hoog), 0f, 0f, 0.18f + random.nextFloat() * 0.1f, kleur);
            }

            // 2. the guh-face constellation, low in the north-west (about 12 blocks across at VER)
            Plek face = Plek.van(305f, 32f);
            float s = 6f;
            // the outline as hair-thin lines (48 bits), stars on twelve of its points: the ear tips and around the head
            int lijnKleur = ARGB.colorFromFloat(0.09f, 1f, 0.75f, 0.9f);
            for (int i = 0; i < 48; i++) {
                double h0 = Math.PI * 2 * i / 48, h1 = Math.PI * 2 * (i + 1) / 48;
                float r0 = guhRand(h0) * s, r1 = guhRand(h1) * s;
                lijn(b, face, (float) Math.cos(h0) * r0, (float) Math.sin(h0) * r0, (float) Math.cos(h1) * r1, (float) Math.sin(h1) * r1, 0.05f, lijnKleur);
            }
            int sterKleur = ARGB.colorFromFloat(0.55f, 1f, 0.8f, 0.92f);
            double oor = Math.atan2(0.78, 0.62);
            double[] hoeken = {oor, Math.PI - oor, oor - 0.42, Math.PI - oor + 0.42, 0.0, Math.PI, -0.6, -Math.PI + 0.6, -1.2, -Math.PI + 1.2,
                    -Math.PI / 2, Math.PI / 2};
            for (double hoek : hoeken) {
                float r = guhRand(hoek) * s;
                ster(b, face, (float) Math.cos(hoek) * r, (float) Math.sin(hoek) * r, 0.32f, sterKleur);
            }
            int oogKleur = ARGB.colorFromFloat(0.7f, 1f, 0.88f, 0.95f);
            ster(b, face, -0.38f * s, 0.18f * s, 0.42f, oogKleur);
            ster(b, face, 0.38f * s, 0.18f * s, 0.42f, oogKleur);
            // the little cavia mouth: a soft "w"
            float[][] mond = {{-0.3f, -0.3f}, {-0.14f, -0.42f}, {0f, -0.3f}, {0.14f, -0.42f}, {0.3f, -0.3f}};
            for (int i = 0; i + 1 < mond.length; i++) {
                lijn(b, face, mond[i][0] * s, mond[i][1] * s, mond[i + 1][0] * s, mond[i + 1][1] * s, 0.05f, lijnKleur);
            }
            for (float[] p : mond) {
                ster(b, face, p[0] * s, p[1] * s, 0.22f, ARGB.colorFromFloat(0.4f, 1f, 0.8f, 0.92f));
            }

            // 3. the soft guh moon, high in the south-east: a pale disc with two ears and a halo that fades out
            Plek maan = Plek.van(135f, 48f);
            float m = 7f;
            int stukken = 72;
            int binnen = ARGB.colorFromFloat(0.24f, 1f, 0.78f, 0.88f), rand0 = ARGB.colorFromFloat(0.16f, 1f, 0.72f, 0.86f);
            int halo = rand0, weg = ARGB.colorFromFloat(0f, 1f, 0.7f, 0.86f);
            Vector3f mid = maan.punt(0f, 0f);
            for (int i = 0; i < stukken; i++) {
                double h0 = Math.PI * 2 * i / stukken, h1 = Math.PI * 2 * (i + 1) / stukken;
                float r0 = guhRand(h0) * m, r1 = guhRand(h1) * m;
                float c0 = (float) Math.cos(h0), s0 = (float) Math.sin(h0), c1 = (float) Math.cos(h1), s1 = (float) Math.sin(h1);
                Vector3f a0 = maan.punt(c0 * r0, s0 * r0), a1 = maan.punt(c1 * r1, s1 * r1);
                // the body (a fan from the middle; the fourth corner doubles the third), brighter in the middle
                quad(b, mid, a0, a1, a1, binnen, rand0, rand0, rand0);
                // the halo: from the edge out to 1.4x, fading to nothing
                Vector3f b0 = maan.punt(c0 * r0 * 1.4f, s0 * r0 * 1.4f), b1 = maan.punt(c1 * r1 * 1.4f, s1 * r1 * 1.4f);
                quad(b, a0, b0, b1, a1, halo, weg, weg, halo);
            }

            MeshData mesh = b.buildOrThrow();
            guhIndices = mesh.drawState().indexCount();
            guh = SkyDraw.upload("Guheinde guh sky", mesh);
        }
    }
}
