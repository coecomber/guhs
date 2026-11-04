package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.wezens.draak.Vahoegnir;
import nl.juiced.guhs.world.GuhTime;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The world-space layer of the atmosphere (after the translucent blocks): the god rays of the Sporenwoud, the halos of the
 * lights ({@link Lichten}), the dragon's shadow on the ground and the pink rain of the Klodderweide.
 */
final class SfeerWereld {
    /** Flyover dragons in this level (refreshed every 10 ticks). */
    static final List<Vahoegnir> DRAKEN = new ArrayList<>();

    private SfeerWereld() {
    }

    static void tick(Minecraft mc) {
        if (mc.level == null || mc.level.getGameTime() % 10 != 0) {
            return;
        }
        DRAKEN.clear();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof Vahoegnir d && !d.isRemoved()) {
                DRAKEN.add(d);
            }
        }
    }

    static void teken(RenderLevelStageEvent.AfterTranslucentBlocks e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || mc.player == null) {
            return;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.position();
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float tijd = l.getGameTime() + pt;
        Matrix4f mv = new Matrix4f(e.getModelViewMatrix()).mul(e.getPoseStack().last().pose());
        Vector3f rechts = new Vector3f(1, 0, 0).rotate(camera.rotation());
        Vector3f op = new Vector3f(0, 1, 0).rotate(camera.rotation());

        // 1. additive: halos, beacon, god rays
        BufferBuilder b = Teken.begin();
        Lichten.teken(b, cam, rechts, op, tijd, l);
        stralen(b, cam, l);
        Teken.klaar("Vadsrijk sfeer gloed", b, true, mv);

        // 2. blended: the dragon's shadow, the pink rain
        if (!DRAKEN.isEmpty() || Momenten.bui(l) > 0 && GebiedMist.wWeide > 0) {
            BufferBuilder s = Teken.begin();
            for (Vahoegnir d : DRAKEN) {
                schaduw(s, d, cam, l, pt);
            }
            regen(s, cam, rechts, tijd, l);
            Teken.klaar("Vadsrijk sfeer schaduw", s, false, mv);
        }
    }

    /** Slanted shafts of green-gold light through the gaps in the caps (WereldPlekken.LICHTSTRALEN), by day. */
    private static void stralen(BufferBuilder b, Vec3 cam, ClientLevel l) {
        float dag = GebiedMist.daglicht();
        if (dag < 0.2f || GebiedMist.wWoud <= 0.01f) {
            return;
        }
        float storm = Momenten.storm(l);
        float hoek = GuhTime.celestialAngle(l) * Mth.TWO_PI;
        float scheef = Mth.sin(hoek) * 14f;     // the sun moves east -> west: the shafts lean the other way
        float a = 0.16f * dag * GebiedMist.wWoud * SfeerConfig.sterkte();
        float r = Mth.lerp(storm, 0.85f, 1f), g = Mth.lerp(storm, 0.95f, 0.55f), bl = Mth.lerp(storm, 0.55f, 0.7f);
        for (BlockPos p : WereldPlekken.LICHTSTRALEN) {
            double d2 = p.distToCenterSqr(cam.x, cam.y, cam.z);
            if (d2 > 160 * 160) {
                continue;
            }
            float x = (float) (p.getX() + 0.5 - cam.x), y = (float) (p.getY() - cam.y), z = (float) (p.getZ() + 0.5 - cam.z);
            float h = 42f, w = 2.4f;
            int boven = Teken.kleur(0f, r, g, bl), midden = Teken.kleur(a, r, g, bl), onder = Teken.kleur(a * 0.3f, r, g, bl);
            for (float[] as : new float[][]{{w, 0}, {0, w}}) {
                Vector3f o0 = new Vector3f(x - as[0], y, z - as[1]), o1 = new Vector3f(x + as[0], y, z + as[1]);
                Vector3f m0 = new Vector3f(x - as[0] + scheef * 0.5f, y + h * 0.5f, z - as[1]), m1 = new Vector3f(x + as[0] + scheef * 0.5f, y + h * 0.5f, z + as[1]);
                Vector3f t0 = new Vector3f(x - as[0] * 1.4f + scheef, y + h, z - as[1] * 1.4f), t1 = new Vector3f(x + as[0] * 1.4f + scheef, y + h, z + as[1] * 1.4f);
                Teken.quad(b, o0, o1, m1, m0, onder, onder, midden, midden);
                Teken.quad(b, m0, m1, t1, t0, midden, midden, boven, boven);
            }
        }
    }

    /** A soft elliptic shadow on the ground under a low dragon, following the terrain in 2x2 cells. */
    private static void schaduw(BufferBuilder b, Vahoegnir d, Vec3 cam, ClientLevel l, float pt) {
        double dx = Mth.lerp(pt, d.xo, d.getX()), dy = Mth.lerp(pt, d.yo, d.getY()), dz = Mth.lerp(pt, d.zo, d.getZ());
        if ((dx - cam.x) * (dx - cam.x) + (dz - cam.z) * (dz - cam.z) > 140 * 140 || !l.hasChunk((int) dx >> 4, (int) dz >> 4)) {
            return;
        }
        int grond = l.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(dx), (int) Math.floor(dz));
        double hoog = dy - grond;
        if (hoog > 110 || hoog < -5) {
            return;
        }
        float sterkte = (float) Mth.clamp(1 - hoog / 110, 0.25, 1) * 0.5f;
        double yaw = Math.toRadians(d.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);     // body direction
        double lang = 11 + hoog * 0.06, breed = 17 + hoog * 0.1;
        int n = 14;
        double cel = 2.0, half = n * cel / 2;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double ox = -half + i * cel, oz = -half + j * cel;
                double u = (ox * fx + oz * fz) / lang, v = (ox * fz - oz * fx) / breed;
                double rr = u * u + v * v;
                if (rr > 1) {
                    continue;
                }
                float a = (float) (sterkte * (1 - rr));
                int wx = (int) Math.floor(dx + ox + cel / 2), wz = (int) Math.floor(dz + oz + cel / 2);
                float y = l.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz) + 0.03f - (float) cam.y;
                float x0 = (float) (dx + ox - cam.x), z0 = (float) (dz + oz - cam.z);
                int c = Teken.kleur(a, 0.12f, 0.08f, 0.16f);
                Teken.quad(b, new Vector3f(x0, y, z0), new Vector3f(x0 + (float) cel, y, z0), new Vector3f(x0 + (float) cel, y, z0 + (float) cel),
                        new Vector3f(x0, y, z0 + (float) cel), c, c, c, c);
            }
        }
    }

    /** Pink rain streaks round the camera (anchored to the block grid like vanilla rain), stopping at the ground. */
    private static void regen(BufferBuilder b, Vec3 cam, Vector3f rechts, float tijd, ClientLevel l) {
        float bui = Momenten.bui(l) * GebiedMist.wWeide;
        if (bui <= 0.01f) {
            return;
        }
        int n = (int) (320 * bui * SfeerConfig.sterkte());
        int cx = Mth.floor(cam.x), cz = Mth.floor(cam.z);
        Vector3f w = new Vector3f(rechts.x, 0, rechts.z);
        if (w.lengthSquared() < 1e-4f) {
            w.set(1, 0, 0);
        }
        w.normalize(0.04f);
        int c0 = Teken.kleur(0.0f, 1f, 0.62f, 0.86f), c1 = Teken.kleur(0.6f, 1f, 0.62f, 0.86f);
        for (int i = 0; i < n; i++) {
            long h = i * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
            h ^= h >>> 29;
            h *= 0xBF58476D1CE4E5B9L;
            h ^= h >>> 32;
            int ox = (int) ((h & 0x3F) % 37) - 18, oz = (int) (((h >>> 8) & 0x3F) % 37) - 18;
            float seed = ((h >>> 16) & 0xFFFF) / 65536f;
            int wx = cx + ox, wz = cz + oz;
            int grond = l.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
            float top = (float) cam.y + 16;
            float y = top - ((tijd * 0.9f + seed * 30f) % 30f);
            if (y < grond) {
                continue;
            }
            float x = wx + 0.5f + (seed - 0.5f) * 0.8f - (float) cam.x, z = wz + 0.5f + (seed * 7 % 1 - 0.5f) * 0.8f - (float) cam.z;
            float ry = y - (float) cam.y;
            Teken.quad(b, new Vector3f(x - w.x, ry + 1.5f, z - w.z), new Vector3f(x + w.x, ry + 1.5f, z + w.z), new Vector3f(x + w.x, ry, z + w.z),
                    new Vector3f(x - w.x, ry, z - w.z), c0, c0, c1, c1);
        }
    }
}
