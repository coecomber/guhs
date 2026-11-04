package nl.juiced.guhs.feature.vadsrijk.wereld.client;

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
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SkyDraw;
import nl.juiced.guhs.feature.vadsrijk.kern.client.SceneSpeler;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.world.GuhTime;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The sky of zone 1 (design ronde 5), drawn over the vanilla sky of the dimension (kern's dimension type keeps the normal
 * overworld-like sky with pink colours; this adds):
 * <ul>
 *   <li>a soft pink-gold glow round the sun;</li>
 *   <li>a second moon: a small pink guh moon trailing the vanilla moon (two moons);</li>
 *   <li>at night: constellations in guh shape (round head, two ears, eyes, a little "w" mouth) among faint stars;</li>
 *   <li>far landmarks of later zones on the horizon (silhouettes in the haze: crystal spires, a floating castle, a giant
 *       tree, a dark volcano with black fluff);</li>
 *   <li>now and then the grey dragon circling far away (a flapping silhouette; the real model is the wezens slice's);</li>
 *   <li>(i-lore, WERELDVERHAAL §10) two hints of the Keizer inside scenes: the second moon that blinks once at the end of the
 *       zone-1 cliffhanger (#15), and a thin grey line with fingers on the horizon for one second of the zone-2 flashback (#14).</li>
 * </ul>
 * Textures: tools/features/vadsrijk_wereld.py (textures/vadsrijk/wereld/lucht_*.png).
 */
final class WereldLucht {
    private static final Identifier MAAN = Guhs.id("textures/vadsrijk/wereld/lucht_maan.png");
    private static final Identifier DRAAK = Guhs.id("textures/vadsrijk/wereld/lucht_draak.png");
    private static final Identifier[] VERTE = {Guhs.id("textures/vadsrijk/wereld/lucht_verte_0.png"), Guhs.id("textures/vadsrijk/wereld/lucht_verte_1.png"),
            Guhs.id("textures/vadsrijk/wereld/lucht_verte_2.png"), Guhs.id("textures/vadsrijk/wereld/lucht_verte_3.png")};
    /** Landmarks: azimuth (0 = north, clockwise), half width and height (blocks at distance 90). */
    private static final float[][] VERTE_PLEK = {{350f, 26f, 26f}, {75f, 22f, 22f}, {255f, 24f, 24f}, {140f, 28f, 20f}};
    /** The dragon: one pass every this many ticks, lasting VLUCHT ticks. */
    private static final int DRAAK_ELKE = 20 * 150, VLUCHT = 20 * 28;
    /** (i-lore) The moon shot of the zone-1 cliffhanger (Verhaal.cliffhanger, 312 ticks): the moon from OOG_VAN, one blink at OOG_KNIPPER. */
    private static final String OOG_SCENE = "npc:cliffhanger", VINGER_SCENE = "npc:z2_flashback";
    private static final int OOG_VAN = 262, OOG_KNIPPER = 292, KNIPPER_DUUR = 6;
    /** (i-lore) The fingers on the horizon in the zone-2 flashback (Z2Verhaal.flashback, 260 ticks): one second from VINGERS_VAN. */
    private static final int VINGERS_VAN = 228, VINGERS_DUUR = 20;

    @Nullable
    private static GpuBuffer sterren;
    private static int sterIndices;

    private WereldLucht() {
    }

    static void teken(RenderLevelStageEvent.AfterSky e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float t = mc.level.getGameTime() + pt;
        float hoek = GuhTime.celestialAngle(mc.level) * Mth.TWO_PI;
        long tijd = GuhTime.timeOfDay(mc.level);
        float nacht = nachtFactor(tijd);
        Matrix4fc mv = e.getModelViewMatrix();
        PoseStack pose = new PoseStack();
        pose.mulPose(Axis.YP.rotationDegrees(-90f));
        // 1. the sun's pink-gold glow
        pose.pushPose();
        pose.mulPose(Axis.XP.rotation(hoek));
        float dag = 1f - nacht;
        if (dag > 0.02f) {
            SkyDraw.drawNow("Vadsrijk sun glow", SkyDraw.COLOURED_STARS, gloed(9f, 46f, ARGB.colorFromFloat(0.55f * dag, 1f, 0.78f, 0.62f)), null,
                    SkyDraw.white(1f), m(mv, pose));
        }
        pose.popPose();
        // 2. the second moon (pink guh moon), a little behind the vanilla moon (not while the cliffhanger stares at it, see 6.)
        RijkScene sc = SceneSpeler.huidig();
        float st = SceneSpeler.tijd() + pt;
        boolean oogShot = sc != null && OOG_SCENE.equals(sc.id()) && st >= OOG_VAN;
        pose.pushPose();
        pose.mulPose(Axis.XP.rotation(hoek + Mth.PI + 0.42f));
        pose.mulPose(Axis.ZP.rotationDegrees(14f));
        float maanA = 0.3f + 0.7f * nacht;
        if (!oogShot) {
            SkyDraw.drawNow("Vadsrijk second moon", SkyDraw.CELESTIAL_TRANSLUCENT, vlak(11f), SkyDraw.texture(MAAN), SkyDraw.white(maanA), m(mv, pose));
        }
        pose.popPose();
        // 3. guh constellations at night
        if (nacht > 0.02f) {
            if (sterren == null) {
                bouwSterren();
            }
            float adem = nacht * (0.85f + 0.15f * Mth.sin(t * 0.03f));
            pose.pushPose();
            pose.mulPose(Axis.XP.rotation(hoek));
            SkyDraw.draw("Vadsrijk guh stars", SkyDraw.COLOURED_STARS, sterren, VertexFormat.Mode.QUADS, sterIndices, null,
                    new Vector4f(adem, adem, adem, nacht), m(mv, pose));
            pose.popPose();
        }
        // 4. far landmarks on the horizon (fixed in the world, in the haze); [zone 2] zone 2 draws its own horizon
        boolean zone2 = mc.player != null && nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldRegistry.inZone2(mc.player.getX(), mc.player.getZ());
        for (int i = 0; i < VERTE.length && !zone2; i++) {
            float[] v = VERTE_PLEK[i];
            float a = 0.42f + 0.25f * dag;
            SkyDraw.drawNow("Vadsrijk landmark " + i, SkyDraw.CELESTIAL_TRANSLUCENT, horizon(v[0], v[1], v[2]), SkyDraw.texture(VERTE[i]),
                    new Vector4f(1f, 0.92f, 0.96f, a), mv);
        }
        // 5. the dragon circling far away now and then
        long slot = (long) Math.floor(t / DRAAK_ELKE);
        float in = t - slot * DRAAK_ELKE;
        if (in < VLUCHT && RandomSource.create(slot * 7919L).nextInt(3) != 0) {
            float f = in / VLUCHT;
            float az = 200f + 140f * f + (slot % 4) * 37f;
            float hoog = 22f + 7f * Mth.sin(f * Mth.PI);
            float a = Mth.sin(f * Mth.PI) * 0.85f;
            int frame = ((int) (t / 6f)) % 2;
            SkyDraw.drawNow("Vadsrijk dragon", SkyDraw.CELESTIAL_TRANSLUCENT, draak(az, hoog, 7f, frame), SkyDraw.texture(DRAAK),
                    new Vector4f(0.55f, 0.5f, 0.6f, a), mv);
        }
        // 6. (i-lore) the Keizer's hints inside two scenes, in the look direction of the scene's last keyframe
        if (oogShot) {
            float[] r = richting(sc);
            float a = Mth.clamp((st - OOG_VAN) / 10f, 0f, 1f);
            float k = st >= OOG_KNIPPER && st < OOG_KNIPPER + KNIPPER_DUUR ? Math.abs(Mth.cos(Mth.PI * (st - OOG_KNIPPER) / KNIPPER_DUUR)) : 1f;
            SkyDraw.drawNow("Vadsrijk staring moon", SkyDraw.CELESTIAL_TRANSLUCENT, bord(r[0], r[1], 9f, 9f * Math.max(k, 0.04f)), SkyDraw.texture(MAAN),
                    SkyDraw.white(a), mv);
        } else if (sc != null && VINGER_SCENE.equals(sc.id()) && st >= VINGERS_VAN && st < VINGERS_VAN + VINGERS_DUUR) {
            float a = Mth.sin(Mth.PI * (st - VINGERS_VAN) / VINGERS_DUUR) * 0.8f;
            SkyDraw.drawNow("Vadsrijk far fingers", SkyDraw.COLOURED_STARS, vingers(richting(sc)[0], ARGB.colorFromFloat(a, 0.43f, 0.41f, 0.47f)), null,
                    SkyDraw.white(1f), mv);
        }
    }

    /** Azimuth and elevation (degrees, as {@link #dir}) of the look direction of a scene's last keyframe. */
    private static float[] richting(RijkScene sc) {
        RijkScene.Punt l = sc.punten().get(sc.punten().size() - 1);
        double dx = l.kijk().x - l.pos().x, dy = l.kijk().y - l.pos().y, dz = l.kijk().z - l.pos().z;
        return new float[] {(float) Math.toDegrees(Math.atan2(dx, -dz)), (float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))};
    }

    /** A camera-facing textured quad at (az, h): half width hw, half height hh (blocks at distance 92). */
    private static MeshData bord(float az, float h, float hw, float hh) {
        Vector3f mid = dir(az, h, 92f);
        Vector3f rechts = new Vector3f(mid).cross(0, 1, 0).normalize(hw);
        Vector3f op = new Vector3f(rechts).cross(mid).normalize(hh);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Vector3f[] p = {new Vector3f(mid).sub(rechts).sub(op), new Vector3f(mid).add(rechts).sub(op), new Vector3f(mid).add(rechts).add(op),
                new Vector3f(mid).sub(rechts).add(op)};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int[] o : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : o) {
                b.addVertex(p[i].x, p[i].y, p[i].z).setUv(uv[i][0], uv[i][1]);
            }
        }
        return b.buildOrThrow();
    }

    /** A thin grey line on the horizon round azimuth az, with five finger-like bumps a little right of the middle (never named). */
    private static MeshData vingers(float az, int c) {
        Vector3f mid = dir(az, 1.2f, 90f);
        Vector3f rechts = new Vector3f(mid).cross(0, 1, 0).normalize();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // {offset to the right, half width, height} in blocks at distance 90; the first is the long thin line
        float[][] stukken = {{0f, 34f, 0.7f}, {9.2f, 0.65f, 2.6f}, {10.8f, 0.7f, 3.7f}, {12.4f, 0.7f, 4.1f}, {14.0f, 0.7f, 3.5f}, {15.5f, 0.6f, 2.2f}};
        for (float[] s : stukken) {
            Vector3f m = new Vector3f(rechts).mul(s[0]).add(mid);
            Vector3f r = new Vector3f(rechts).mul(s[1]);
            Vector3f[] p = {new Vector3f(m).sub(r).add(0, -0.6f, 0), new Vector3f(m).add(r).add(0, -0.6f, 0), new Vector3f(m).add(r).add(0, s[2], 0),
                    new Vector3f(m).sub(r).add(0, s[2], 0)};
            for (int[] o : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
                for (int i : o) {
                    b.addVertex(p[i].x, p[i].y, p[i].z).setColor(c);
                }
            }
        }
        return b.buildOrThrow();
    }

    private static float nachtFactor(long tijd) {
        // 0 at day, 1 at deep night; soft over dusk (12000-13500) and dawn (22500-24000)
        if (tijd >= 13500 && tijd <= 22500) {
            return 1f;
        }
        if (tijd > 12000 && tijd < 13500) {
            return (tijd - 12000) / 1500f;
        }
        if (tijd > 22500) {
            return 1f - (tijd - 22500) / 1500f;
        }
        return 0f;
    }

    private static Matrix4f m(Matrix4fc mv, PoseStack pose) {
        return new Matrix4f(mv).mul(pose.last().pose());
    }

    /** A soft ring fan at y = 100 (round the sun): from colour c inside to nothing outside. */
    private static MeshData gloed(float r0, float r1, int c) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int weg = ARGB.color(0, ARGB.red(c), ARGB.green(c), ARGB.blue(c));
        int n = 40;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            b.addVertex((float) Math.cos(a0) * r0, 100f, (float) Math.sin(a0) * r0).setColor(c);
            b.addVertex((float) Math.cos(a0) * r1, 100f, (float) Math.sin(a0) * r1).setColor(weg);
            b.addVertex((float) Math.cos(a1) * r1, 100f, (float) Math.sin(a1) * r1).setColor(weg);
            b.addVertex((float) Math.cos(a1) * r0, 100f, (float) Math.sin(a1) * r0).setColor(c);
        }
        return b.buildOrThrow();
    }

    /** A textured quad at y = 100 (both windings). */
    private static MeshData vlak(float s) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float[][] p = {{-s, -s, 0, 0}, {s, -s, 1, 0}, {s, s, 1, 1}, {-s, s, 0, 1}};
        for (int[] o : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : o) {
                b.addVertex(p[i][0], 100f, p[i][1]).setUv(p[i][2], p[i][3]);
            }
        }
        return b.buildOrThrow();
    }

    /** A direction on the sky: az degrees (0 = north = -z), h degrees above the horizon, at distance d. */
    private static Vector3f dir(float az, float h, float d) {
        double a = Math.toRadians(az), hh = Math.toRadians(h);
        return new Vector3f((float) (Math.sin(a) * Math.cos(hh)), (float) Math.sin(hh), (float) (-Math.cos(a) * Math.cos(hh))).mul(d);
    }

    /** A standing quad on the horizon at azimuth az: half width hw, height hh (in blocks at distance 90), foot a bit below. */
    private static MeshData horizon(float az, float hw, float hh) {
        Vector3f mid = dir(az, 0f, 90f);
        Vector3f rechts = new Vector3f(mid).cross(0, 1, 0).normalize(hw);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Vector3f lo = new Vector3f(mid).add(0, -3f, 0), hi = new Vector3f(mid).add(0, hh, 0);
        Vector3f[] p = {new Vector3f(lo).sub(rechts), new Vector3f(lo).add(rechts), new Vector3f(hi).add(rechts), new Vector3f(hi).sub(rechts)};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int[] o : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : o) {
                b.addVertex(p[i].x, p[i].y, p[i].z).setUv(uv[i][0], uv[i][1]);
            }
        }
        return b.buildOrThrow();
    }

    /** The dragon: a quad facing the camera at (az, h), texture with 2 frames stacked (wings up / down). */
    private static MeshData draak(float az, float h, float s, int frame) {
        Vector3f mid = dir(az, h, 92f);
        Vector3f rechts = new Vector3f(mid).cross(0, 1, 0).normalize(s * 1.6f);
        Vector3f op = new Vector3f(rechts).cross(mid).normalize(s * 0.8f);
        float v0 = frame * 0.5f, v1 = v0 + 0.5f;
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Vector3f[] p = {new Vector3f(mid).sub(rechts).sub(op), new Vector3f(mid).add(rechts).sub(op), new Vector3f(mid).add(rechts).add(op),
                new Vector3f(mid).sub(rechts).add(op)};
        float[][] uv = {{0, v1}, {1, v1}, {1, v0}, {0, v0}};
        for (int[] o : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : o) {
                b.addVertex(p[i].x, p[i].y, p[i].z).setUv(uv[i][0], uv[i][1]);
            }
        }
        return b.buildOrThrow();
    }

    // ---------------------------------------------------------------------------------------------------- guh constellations

    /** One guh head in stars, in a 2D frame: unit circle head, ears, eyes and a w-mouth. */
    private static final float[][] GUH = {
            {1f, 0f}, {0.87f, 0.5f}, {0.5f, 0.87f}, {-0.5f, 0.87f}, {-0.87f, 0.5f}, {-1f, 0f}, {-0.87f, -0.5f}, {-0.5f, -0.87f}, {0f, -1f},
            {0.5f, -0.87f}, {0.87f, -0.5f},
            {-0.62f, 1.25f}, {-0.45f, 1.55f}, {-0.2f, 1.2f}, {0.62f, 1.25f}, {0.45f, 1.55f}, {0.2f, 1.2f},
            {-0.38f, 0.18f}, {0.38f, 0.18f},
            {-0.25f, -0.38f}, {0f, -0.28f}, {0.25f, -0.38f}};

    private static void bouwSterren() {
        RandomSource r = RandomSource.create(20261103L);
        float[][] beelden = {{30f, 55f, 7f}, {150f, 40f, 6f}, {250f, 62f, 8f}, {320f, 33f, 5.5f}, {95f, 70f, 5f}};
        int n = 520 + beelden.length * GUH.length;
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(n * 8 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < 520; i++) {
                float az = r.nextFloat() * 360f, h = (float) Math.toDegrees(Math.asin(r.nextFloat() * 2 - 1));
                float a = 0.25f + r.nextFloat() * 0.5f;
                ster(b, dir(az, h, 95f), 0.12f + r.nextFloat() * 0.12f, ARGB.colorFromFloat(a, 1f, 0.85f + r.nextFloat() * 0.15f, 0.95f));
            }
            for (float[] bl : beelden) {
                Vector3f mid = dir(bl[0], bl[1], 95f);
                Vector3f rechts = new Vector3f(mid).cross(0, 1, 0).normalize(bl[2]);
                Vector3f op = new Vector3f(rechts).cross(mid).normalize(bl[2]);
                for (int i = 0; i < GUH.length; i++) {
                    Vector3f p = new Vector3f(mid).add(new Vector3f(rechts).mul(GUH[i][0])).add(new Vector3f(op).mul(GUH[i][1]));
                    boolean oog = i == 17 || i == 18;
                    ster(b, p, oog ? 0.42f : 0.3f, oog ? ARGB.colorFromFloat(1f, 0.75f, 0.9f, 1f) : ARGB.colorFromFloat(1f, 1f, 0.8f, 0.92f));
                }
            }
            MeshData mesh = b.buildOrThrow();
            sterIndices = mesh.drawState().indexCount();
            sterren = SkyDraw.upload("Vadsrijk guh stars", mesh);
        }
    }

    private static void ster(BufferBuilder b, Vector3f p, float s, int kleur) {
        Vector3f d = new Vector3f(p).normalize();
        Vector3f rechts = new Vector3f(d).cross(0, 1, 0);
        if (rechts.lengthSquared() < 1e-4f) {
            rechts.set(1, 0, 0);
        }
        rechts.normalize(s);
        Vector3f op = new Vector3f(rechts).cross(d).normalize(s);
        Vector3f[] q = {new Vector3f(p).sub(op), new Vector3f(p).add(rechts), new Vector3f(p).add(op), new Vector3f(p).sub(rechts)};
        for (int[] o : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : o) {
                b.addVertex(q[i].x, q[i].y, q[i].z).setColor(kleur);
            }
        }
    }
}
