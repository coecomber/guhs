package nl.juiced.guhs.feature.droomsfeer.client;

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
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The sky of Guhdromenland: an endless twilight. The dimension has a fixed time, so this draws everything itself
 * (registered as {@code guhs:guhdromenland}; the dimension type's {@code "neoforge:custom_skybox"}):
 * <ol>
 *   <li>the colour: a box round the camera with a soft gradient (deep violet overhead, pink at the horizon, lavender below:
 *       textures/environment/droom_lucht.png);</li>
 *   <li>the stars: one static buffer of pastel stars over the upper sky that breathe very slowly;</li>
 *   <li>the moon: big and soft, a sleeping guh face with a pink nightcap (textures/environment/droom_maan.png), with a halo;</li>
 *   <li>now and then a falling star with a glittery tail (about every 8 seconds, at a random spot; the same for everyone).</li>
 * </ol>
 * Textures: tools/features/droomsfeer.py.
 */
public class DroomLucht implements CustomSkyboxRenderer {
    private static final Identifier LUCHT = Guhs.id("textures/environment/droom_lucht.png");
    private static final Identifier MAAN = Guhs.id("textures/environment/droom_maan.png");
    /** How far the stars and the moon hang (inside the box, whose faces are 100 away). */
    private static final float VER = 90f;
    /** Where the moon hangs: degrees around (0 = north) and above the horizon, and its half size in blocks at VER. */
    private static final float MAAN_AZ = 205f, MAAN_HOOG = 34f, MAAN_HALF = 15f;
    /** A falling star every this many ticks (when the slot's dice say so), lasting VAL_DUUR ticks. */
    private static final int VAL_ELKE = 160, VAL_DUUR = 26;

    @Nullable
    private GpuBuffer sterren;
    private int sterIndices;

    @Override
    public boolean renderSky(LevelRenderState level, SkyRenderState sky, Matrix4fc modelView, Runnable setupFog) {
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float t = level.gameTime + partialTick;
        kleur(modelView);
        if (sterren == null) {
            bouwSterren();
        }
        if (sterren != null) {
            float adem = 0.85f + 0.15f * Mth.sin(t * 0.012f);
            SkyDraw.draw("Droom stars", SkyDraw.COLOURED_STARS, sterren, VertexFormat.Mode.QUADS, sterIndices, null,
                    new Vector4f(adem, adem, adem, 1f), modelView);
        }
        maan(t, modelView);
        vallendeSter(t, modelView);
        return true;
    }

    // ---------------------------------------------------------------------------------------------------- the colour

    /** The gradient box: four walls with the texture from top (v 0) to bottom (v 1), a lid and a floor. */
    private static void kleur(Matrix4fc modelView) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float r = 100f;
        float[][] hoeken = {{-r, -r}, {r, -r}, {r, r}, {-r, r}};
        for (int i = 0; i < 4; i++) {
            float[] a = hoeken[i], c = hoeken[(i + 1) % 4];
            float u0 = i * 0.25f, u1 = u0 + 0.25f;
            dubbel(b, new float[][]{{a[0], r, a[1], u0, 0f}, {c[0], r, c[1], u1, 0f}, {c[0], -r, c[1], u1, 1f}, {a[0], -r, a[1], u0, 1f}});
        }
        dubbel(b, new float[][]{{-r, r, -r, 0f, 0.002f}, {r, r, -r, 1f, 0.002f}, {r, r, r, 1f, 0.004f}, {-r, r, r, 0f, 0.004f}});
        dubbel(b, new float[][]{{-r, -r, -r, 0f, 0.996f}, {r, -r, -r, 1f, 0.996f}, {r, -r, r, 1f, 0.998f}, {-r, -r, r, 0f, 0.998f}});
        SkyDraw.drawNow("Droom sky colour", RenderPipelines.END_SKY, b.buildOrThrow(), SkyDraw.texture(LUCHT), SkyDraw.white(1f), modelView);
    }

    /** A quad in both windings (seen from inside, whichever way the pipeline culls). */
    private static void dubbel(BufferBuilder b, float[][] v) {
        for (int[] volgorde : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : volgorde) {
                b.addVertex(v[i][0], v[i][1], v[i][2]).setUv(v[i][3], v[i][4]).setColor(0xFFFFFFFF);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------- spots on the sky

    /** A direction on the sky (az: degrees round, 0 = north; hoog: degrees above the horizon) and two axes across it. */
    private record Plek(Vector3f mid, Vector3f rechts, Vector3f op) {
        static Plek van(float az, float hoog) {
            double a = Math.toRadians(az), h = Math.toRadians(hoog);
            Vector3f dir = new Vector3f((float) (Math.sin(a) * Math.cos(h)), (float) Math.sin(h), (float) (-Math.cos(a) * Math.cos(h)));
            Vector3f rechts = new Vector3f(dir).cross(0f, 1f, 0f).normalize();
            Vector3f op = new Vector3f(rechts).cross(dir).normalize();
            return new Plek(dir.mul(VER), rechts, op);
        }

        Vector3f punt(float u, float v) {
            return new Vector3f(mid).add(new Vector3f(rechts).mul(u)).add(new Vector3f(op).mul(v));
        }
    }

    private static void quad(BufferBuilder b, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, int c0, int c1, int c2, int c3) {
        b.addVertex(p0.x, p0.y, p0.z).setColor(c0);
        b.addVertex(p1.x, p1.y, p1.z).setColor(c1);
        b.addVertex(p2.x, p2.y, p2.z).setColor(c2);
        b.addVertex(p3.x, p3.y, p3.z).setColor(c3);
        // (and the other way round, so no star is lost to back-face culling)
        b.addVertex(p3.x, p3.y, p3.z).setColor(c3);
        b.addVertex(p2.x, p2.y, p2.z).setColor(c2);
        b.addVertex(p1.x, p1.y, p1.z).setColor(c1);
        b.addVertex(p0.x, p0.y, p0.z).setColor(c0);
    }

    private static void ster(BufferBuilder b, Plek plek, float u, float v, float grootte, int kleur) {
        quad(b, plek.punt(u, v - grootte), plek.punt(u + grootte, v), plek.punt(u, v + grootte), plek.punt(u - grootte, v), kleur, kleur, kleur, kleur);
    }

    // ---------------------------------------------------------------------------------------------------- the stars

    private static final int[] STER_KLEUR = {0xFFFFFFFF, 0xFFFFE3F4, 0xFFE6DAFF, 0xFFFFF4C8, 0xFFD8F4FF};

    private void bouwSterren() {
        RandomSource random = RandomSource.create(20261028L);
        int aantal = 460;
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(aantal * 8 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < aantal; i++) {
                float az = random.nextFloat() * 360f;
                float hoog = 4f + (float) Math.toDegrees(Math.asin(random.nextFloat())) * 0.95f;     // denser near the top
                float helder = 0.35f + random.nextFloat() * 0.65f;
                int basis = STER_KLEUR[random.nextInt(STER_KLEUR.length)];
                int kleur = ARGB.color(Math.round(helder * 255), ARGB.red(basis), ARGB.green(basis), ARGB.blue(basis));
                float grootte = random.nextFloat() < 0.06f ? 0.42f : 0.14f + random.nextFloat() * 0.14f;   // a few big ones
                ster(b, Plek.van(az, hoog), 0f, 0f, grootte, kleur);
            }
            MeshData mesh = b.buildOrThrow();
            sterIndices = mesh.drawState().indexCount();
            sterren = SkyDraw.upload("Droom stars", mesh);
        }
    }

    // ---------------------------------------------------------------------------------------------------- the moon

    private static void maan(float t, Matrix4fc modelView) {
        Plek plek = Plek.van(MAAN_AZ, MAAN_HOOG);
        // the halo: a soft fan from a warm glow to nothing, breathing slowly
        float adem = 0.9f + 0.1f * Mth.sin(t * 0.008f);
        BufferBuilder h = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int binnen = ARGB.colorFromFloat(0.32f * adem, 1f, 0.86f, 0.9f), weg = ARGB.colorFromFloat(0f, 1f, 0.8f, 0.9f);
        int stukken = 48;
        for (int i = 0; i < stukken; i++) {
            double a0 = Math.PI * 2 * i / stukken, a1 = Math.PI * 2 * (i + 1) / stukken;
            float r0 = MAAN_HALF * 0.8f, r1 = MAAN_HALF * 2.1f;
            Vector3f p0 = plek.punt((float) Math.cos(a0) * r0, (float) Math.sin(a0) * r0), p1 = plek.punt((float) Math.cos(a1) * r0, (float) Math.sin(a1) * r0);
            Vector3f q0 = plek.punt((float) Math.cos(a0) * r1, (float) Math.sin(a0) * r1), q1 = plek.punt((float) Math.cos(a1) * r1, (float) Math.sin(a1) * r1);
            quad(h, p0, q0, q1, p1, binnen, weg, weg, binnen);
        }
        SkyDraw.drawNow("Droom moon halo", SkyDraw.COLOURED_STARS, h.buildOrThrow(), null, SkyDraw.white(1f), modelView);
        // the moon itself: one textured quad (a little bob, as if it is breathing in its sleep)
        float bob = Mth.sin(t * 0.01f) * 0.4f;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Vector3f[] p = {plek.punt(-MAAN_HALF, -MAAN_HALF + bob), plek.punt(MAAN_HALF, -MAAN_HALF + bob),
                plek.punt(MAAN_HALF, MAAN_HALF + bob), plek.punt(-MAAN_HALF, MAAN_HALF + bob)};
        float[][] uv = {{0f, 1f}, {1f, 1f}, {1f, 0f}, {0f, 0f}};
        for (int[] volgorde : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : volgorde) {
                b.addVertex(p[i].x, p[i].y, p[i].z).setUv(uv[i][0], uv[i][1]);
            }
        }
        SkyDraw.drawNow("Droom moon", SkyDraw.CELESTIAL_TRANSLUCENT, b.buildOrThrow(), SkyDraw.texture(MAAN), SkyDraw.white(1f), modelView);
    }

    // ---------------------------------------------------------------------------------------------------- falling stars

    private static void vallendeSter(float t, Matrix4fc modelView) {
        long slot = (long) Math.floor(t / VAL_ELKE);
        float in = t - slot * VAL_ELKE;
        if (in > VAL_DUUR) {
            return;
        }
        RandomSource r = RandomSource.create(slot * 2654435761L + 1808L);
        if (r.nextFloat() > 0.75f) {
            return;                                              // (not every slot: it stays a little surprise)
        }
        float az = r.nextFloat() * 360f, hoog = 30f + r.nextFloat() * 45f;
        float richting = (r.nextBoolean() ? 1f : -1f) * (20f + r.nextFloat() * 25f);   // degrees across the sky, sideways
        float f = in / VAL_DUUR;
        float kopAz = az + richting * f, kopHoog = hoog - 14f * f;
        float vervaag = f < 0.15f ? f / 0.15f : 1f - (f - 0.15f) / 0.85f;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int stukken = 14;
        for (int i = 0; i < stukken; i++) {
            float s0 = i / (float) stukken, s1 = (i + 1) / (float) stukken;
            float staart = 0.22f;                                  // how far back the tail reaches (in f)
            Vector3f a = Plek.van(kopAz - richting * staart * s0, kopHoog + 14f * staart * s0).mid();
            Vector3f c = Plek.van(kopAz - richting * staart * s1, kopHoog + 14f * staart * s1).mid();
            Vector3f dwars = new Vector3f(c).sub(a).cross(a).normalize().mul(0.35f * (1f - s0) + 0.05f);
            int k0 = ARGB.colorFromFloat(vervaag * (1f - s0), 1f, 0.92f, 0.97f), k1 = ARGB.colorFromFloat(vervaag * (1f - s1), 1f, 0.85f, 0.95f);
            quad(b, new Vector3f(a).add(dwars), new Vector3f(c).add(dwars), new Vector3f(c).sub(dwars), new Vector3f(a).sub(dwars), k0, k1, k1, k0);
        }
        // the bright little head
        Plek kop = Plek.van(kopAz, kopHoog);
        ster(b, kop, 0f, 0f, 0.6f, ARGB.colorFromFloat(vervaag, 1f, 1f, 0.92f));
        SkyDraw.drawNow("Droom falling star", SkyDraw.COLOURED_STARS, b.buildOrThrow(), null, SkyDraw.white(1f), modelView);
    }
}
