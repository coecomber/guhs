package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.world.GuhTime;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * The sky layer of the atmosphere (after the vanilla sky and the wereld slice's sky): the orange-purple sky of the spore storm,
 * the soft pink sky of a shower, the rainbow after it, the aurora above the Zwamtempel at night and, for a player who beat the
 * Opperzwam, the light pillar from the temple dome. Everything is a direction on the sky (distance ~92), so the terrain in
 * front of it hides it, like the sky.
 */
final class SfeerLucht {
    private static final float AFST = 92f;
    /** The Opperzwam hall under the dome (arena centre) and the dome top. */
    private static final double TEMPEL_X, TEMPEL_Z;
    private static final double KOEPEL_Y = 195;

    static {
        var c = WereldPlekken.ALLE.get("arena_opperzwam.centrum");
        TEMPEL_X = c == null ? 330.5 : c.getX() + 0.5;
        TEMPEL_Z = c == null ? -496.5 : c.getZ() + 0.5;
    }

    private SfeerLucht() {
    }

    static void teken(RenderLevelStageEvent.AfterSky e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        float tijd = l.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Matrix4fc mv = e.getModelViewMatrix();
        float storm = Momenten.storm(l) * GebiedMist.wWoud, bui = Momenten.bui(l) * GebiedMist.wWeide;
        if (storm > 0.01f || bui > 0.01f) {
            BufferBuilder b = Teken.begin();
            if (storm > 0.01f) {
                koepel(b, storm, new float[]{1f, 0.55f, 0.28f}, new float[]{0.62f, 0.28f, 0.78f}, 0.55f);
            }
            if (bui > 0.01f) {
                koepel(b, bui, new float[]{1f, 0.78f, 0.88f}, new float[]{0.85f, 0.7f, 0.82f}, 0.35f);
            }
            Teken.klaar("Vadsrijk sfeer lucht", b, false, mv);
        }
        BufferBuilder g = Teken.begin();
        float boog = Math.max(Momenten.regenboog(l) * Math.max(GebiedMist.wWeide, 0.0f), (float) SfeerClient.U_REGENBOOG.getAsDouble());   // U-LEVEN-RITME: + zone 2-4
        if (boog > 0.01f) {
            regenboog(g, boog * SfeerConfig.sterkte() + 0.25f * boog, l);
        }
        float nacht = Momenten.noorderlicht(l);
        if (nacht > 0.01f) {
            noorderlicht(g, cam, tijd, nacht);
        }
        if (Momenten.opperzwam) {
            zuil(g, cam, tijd);
        }
        Teken.klaar("Vadsrijk sfeer gloed lucht", g, true, mv);
    }

    /** A direction on the sky: az degrees (0 = north = -z, clockwise), h degrees above the horizon, at distance d. */
    static Vector3f dir(float az, float h, float d) {
        double a = Math.toRadians(az), hh = Math.toRadians(h);
        return new Vector3f((float) (Math.sin(a) * Math.cos(hh)), (float) Math.sin(hh), (float) (-Math.cos(a) * Math.cos(hh))).mul(d);
    }

    /** A tinted sky bowl: colour {@code laag} at the horizon to {@code hoog} higher up, fading out towards the zenith. */
    private static void koepel(BufferBuilder b, float k, float[] laag, float[] hoog, float alfa) {
        float[] h = {-8f, 6f, 28f, 60f, 88f};
        float[] a = {alfa, alfa, alfa * 0.75f, alfa * 0.35f, 0f};
        int n = 32;
        for (int ring = 0; ring + 1 < h.length; ring++) {
            float f0 = ring / (float) (h.length - 1), f1 = (ring + 1) / (float) (h.length - 1);
            int c0 = Teken.kleur(a[ring] * k, Mth.lerp(f0, laag[0], hoog[0]), Mth.lerp(f0, laag[1], hoog[1]), Mth.lerp(f0, laag[2], hoog[2]));
            int c1 = Teken.kleur(a[ring + 1] * k, Mth.lerp(f1, laag[0], hoog[0]), Mth.lerp(f1, laag[1], hoog[1]), Mth.lerp(f1, laag[2], hoog[2]));
            for (int i = 0; i < n; i++) {
                float az0 = i * 360f / n, az1 = (i + 1) * 360f / n;
                Teken.quad(b, dir(az0, h[ring], AFST), dir(az1, h[ring], AFST), dir(az1, h[ring + 1], AFST), dir(az0, h[ring + 1], AFST), c0, c0, c1, c1);
            }
        }
    }

    /** The rainbow: 7 bands on a 42 degree circle round the point opposite the sun (west in the morning, east in the afternoon). */
    private static void regenboog(BufferBuilder b, float k, ClientLevel l) {
        long t = GuhTime.timeOfDay(l);
        float az = t < 6000 || t > 18000 ? 270f : 90f;
        Vector3f c = dir(az, -12f, 1f).normalize();
        Vector3f r = new Vector3f(c).cross(0, 1, 0).normalize();
        Vector3f u = new Vector3f(r).cross(c).normalize();
        float[][] kleuren = {{1f, 0.25f, 0.3f}, {1f, 0.6f, 0.2f}, {1f, 0.95f, 0.3f}, {0.35f, 0.95f, 0.4f}, {0.3f, 0.6f, 1f}, {0.45f, 0.35f, 0.95f},
                {0.8f, 0.4f, 1f}};
        float buiten = 44f, band = 1.4f;
        int n = 64;
        for (int i = 0; i < kleuren.length; i++) {
            float r0 = (float) Math.toRadians(buiten - i * band), r1 = (float) Math.toRadians(buiten - (i + 1) * band);
            float[] kl = kleuren[i];
            for (int s = 0; s < n; s++) {
                float t0 = (float) (Math.PI * s / n), t1 = (float) (Math.PI * (s + 1) / n);
                Vector3f a0 = boogPunt(c, r, u, t0, r0), a1 = boogPunt(c, r, u, t1, r0), b0 = boogPunt(c, r, u, t0, r1), b1 = boogPunt(c, r, u, t1, r1);
                if (a0.y < -0.05f * AFST && a1.y < -0.05f * AFST) {
                    continue;
                }
                float rand0 = Math.min(1f, Math.max(0f, a0.y / AFST + 0.05f) * 6f), rand1 = Math.min(1f, Math.max(0f, a1.y / AFST + 0.05f) * 6f);
                int k0 = Teken.kleur(0.32f * k * rand0, kl[0], kl[1], kl[2]), k1 = Teken.kleur(0.32f * k * rand1, kl[0], kl[1], kl[2]);
                Teken.quad(b, a0, a1, b1, b0, k0, k1, k1, k0);
            }
        }
    }

    private static Vector3f boogPunt(Vector3f c, Vector3f r, Vector3f u, float theta, float rho) {
        Vector3f p = new Vector3f(c).mul((float) Math.cos(rho));
        Vector3f rand = new Vector3f(r).mul((float) Math.cos(theta)).add(new Vector3f(u).mul((float) Math.sin(theta))).mul((float) Math.sin(rho));
        return p.add(rand).normalize(AFST);
    }

    /** Faint waving pink ribbons above the Zwamtempel mountain (bright pink after the Opperzwam). */
    private static void noorderlicht(BufferBuilder b, Vec3 cam, float tijd, float nacht) {
        double dx = TEMPEL_X - cam.x, dz = TEMPEL_Z - cam.z;
        double d = Math.max(30, Math.sqrt(dx * dx + dz * dz));
        float az = (float) Math.toDegrees(Math.atan2(dx, -dz));
        float half = (float) Math.min(80, Math.toDegrees(Math.atan(150 / d)));
        boolean fel = Momenten.opperzwam;
        float sterk = nacht * (fel ? 0.85f : 0.42f) * (0.4f + 0.6f * SfeerConfig.sterkte());
        int n = 56;
        for (int lint = 0; lint < 3; lint++) {
            double yo = 250 + lint * 22, yb = yo + 70;
            float e0 = (float) Math.toDegrees(Math.atan2(yo - cam.y, d)), e1 = (float) Math.toDegrees(Math.atan2(yb - cam.y, d));
            e0 = Math.min(e0, 78f);
            e1 = Math.min(Math.max(e1, e0 + 4f), 86f);
            float rr = fel ? 1f : 0.95f, gg = fel ? 0.35f : 0.55f + lint * 0.12f, bb = fel ? 0.78f : 0.85f;
            Vector3f p0 = null, q0 = null;
            int c0 = 0;
            for (int s = 0; s <= n; s++) {
                float f = s / (float) n;
                float golf = Mth.sin(f * 9f + tijd * 0.02f + lint * 2f) * 3.5f + Mth.sin(f * 23f - tijd * 0.035f) * 1.2f;
                float a = az + (f * 2 - 1) * half + Mth.sin(f * 5f + tijd * 0.01f + lint) * half * 0.08f;
                Vector3f p = dir(a, e0 + golf, AFST), q = dir(a, e1 + golf * 1.4f, AFST);
                float omh = Mth.sin(f * Mth.PI);
                float flikker = 0.55f + 0.45f * Mth.sin(f * 14f + tijd * 0.06f + lint * 1.7f);
                int c = Teken.kleur(sterk * omh * flikker * (lint == 0 ? 1f : 0.7f), rr, gg, bb);
                if (p0 != null) {
                    int weg0 = c0 & 0x00FFFFFF, weg = c & 0x00FFFFFF;
                    Teken.quad(b, p0, p, q, q0, c0, c, weg, weg0);
                }
                p0 = p;
                q0 = q;
                c0 = c;
            }
        }
    }

    /** The light pillar from the dome of the Zwamtempel (this player beat the Opperzwam). */
    private static void zuil(BufferBuilder b, Vec3 cam, float tijd) {
        float nacht = Momenten.noorderlicht(Minecraft.getInstance().level);
        float sterk = (0.32f + 0.4f * nacht) * (0.9f + 0.1f * Mth.sin(tijd * 0.1f));
        double[] ys = {KOEPEL_Y, KOEPEL_Y + 12, KOEPEL_Y + 35, KOEPEL_Y + 80, KOEPEL_Y + 160, KOEPEL_Y + 320, KOEPEL_Y + 700};
        Vector3f vorigL = null, vorigR = null;
        int vorigC = 0;
        for (int i = 0; i < ys.length; i++) {
            Vector3f v = new Vector3f((float) (TEMPEL_X - cam.x), (float) (ys[i] - cam.y), (float) (TEMPEL_Z - cam.z));
            float afst = v.length();
            if (afst < 1) {
                continue;
            }
            Vector3f zij = new Vector3f(v).cross(0, 1, 0);
            if (zij.lengthSquared() < 1e-4f) {
                zij.set(1, 0, 0);
            }
            zij.normalize(AFST * 2.6f / afst);
            v.normalize(AFST);
            Vector3f lft = new Vector3f(v).sub(zij), rgt = new Vector3f(v).add(zij);
            float f = i / (float) (ys.length - 1);
            int c = Teken.kleur(sterk * (1 - f), 1f, 0.72f, 0.92f);
            if (vorigL != null) {
                Teken.quad(b, vorigL, vorigR, rgt, lft, vorigC, vorigC, c, c);
            }
            vorigL = lft;
            vorigR = rgt;
            vorigC = c;
        }
    }
}
