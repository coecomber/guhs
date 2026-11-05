package nl.juiced.guhs.feature.vadsrijk.vfx.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The VFX kit's own particles and meshes (roadmap 3.2), drawn in two batches:
 * <ul>
 *   <li>particles: camera-facing quads from the sprite sheet {@code deeltjes.png} (8 x 4 cells, white, tinted), with gravity,
 *       drag, spin, frames and a size/alpha curve; a hard budget ({@link VfxConfig#budget}: oldest go first) and LOD (nothing
 *       spawns further than {@link #LOD_DEELTJES} blocks from the camera);</li>
 *   <li>meshes with {@code gloed.png} (a soft band, u across): shockwave rings ({@code golf}), beams ({@code straal}), impact flashes
 *       ({@code flits}, a radial glow), ribbon trails behind entities ({@code spoor}) and light pillars ({@code zuil}).</li>
 * </ul>
 * At VFX level UIT nothing of this spawns.
 */
final class Effecten {
    static final Identifier DEELTJES = Guhs.id("textures/vadsrijk/vfx/deeltjes.png"), GLOED = Guhs.id("textures/vadsrijk/vfx/gloed.png"),
            FLITS = Guhs.id("textures/vadsrijk/vfx/flits.png");
    static final double LOD_DEELTJES = 48, LOD_MESH = 64;
    private static final int LICHT = 0xF000F0;
    private static final Random RND = new Random();

    static final class D {
        double x, y, z, vx, vy, vz;
        float grootte, rol, rolSnel;
        int leven, max, kleur, beeld;
        Deeltje soort;
    }

    /** A mesh effect: kind, place(s), size, colour, life. */
    static final class M {
        String soort;
        Vec3 a, b;
        double r, breedte;
        int kleur, ticks, id;
        long start;
        final ArrayDeque<Vec3> spoor = new ArrayDeque<>();
    }

    static final ArrayDeque<D> DEELTJES_LIJST = new ArrayDeque<>();
    static final List<M> MESHES = new ArrayList<>();

    private Effecten() {
    }

    // ---- spawning ------------------------------------------------------------------------------------------------------------

    static void burst(CompoundTag d, Vec3 cam) {
        Vec3 v = new Vec3(d.getDoubleOr("x", 0), d.getDoubleOr("y", 0), d.getDoubleOr("z", 0));
        if (!VfxConfig.cosmetisch() || v.distanceToSqr(cam) > LOD_DEELTJES * LOD_DEELTJES) {
            return;
        }
        int n = Math.round(d.getIntOr("n", 10) * VfxConfig.dichtheid());
        Deeltje s = Deeltje.van(d.getStringOr("soort", "STER"));
        int kleur = d.getIntOr("kleur", 0xFFFFFF);
        double sp = d.getDoubleOr("spreid", 0.5), snel = d.getDoubleOr("snel", 0.1);
        for (int i = 0; i < n; i++) {
            deeltje(s, v.x + (RND.nextDouble() * 2 - 1) * sp, v.y + (RND.nextDouble() * 2 - 1) * sp * 0.5, v.z + (RND.nextDouble() * 2 - 1) * sp,
                    (RND.nextDouble() * 2 - 1) * snel, RND.nextDouble() * snel * 1.2 + 0.02, (RND.nextDouble() * 2 - 1) * snel, kleur(s, kleur));
        }
    }

    /** Confetti gets random party colours (the tint is ignored unless it is not white). */
    private static int kleur(Deeltje s, int kleur) {
        if (s == Deeltje.CONFETTI && kleur == 0xFFFFFF) {
            int[] k = {0xFF7AC8, 0xFFD62A, 0x8AD0FF, 0x9AF08A, 0xC8A0FF, 0xFFFFFF};
            return k[RND.nextInt(k.length)];
        }
        return kleur;
    }

    static void deeltje(Deeltje s, double x, double y, double z, double vx, double vy, double vz, int kleur) {
        int budget = VfxConfig.budget();
        if (budget <= 0) {
            return;
        }
        while (DEELTJES_LIJST.size() >= budget) {
            DEELTJES_LIJST.pollFirst();   // the budget: the oldest go first
        }
        D d = new D();
        d.soort = s;
        d.x = x;
        d.y = y;
        d.z = z;
        d.vx = vx;
        d.vy = vy;
        d.vz = vz;
        d.max = (int) (s.leven * (0.75 + RND.nextDouble() * 0.5) * (VfxConfig.niveau() == VfxConfig.Niveau.ZACHT ? 0.7 : 1));
        d.grootte = (float) (0.18 + RND.nextDouble() * 0.16) * (s == Deeltje.ROOK || s == Deeltje.SPOOR || s == Deeltje.PLUIS ? 1.8f
                : s == Deeltje.GLOED ? 2.2f : 1f);
        d.rol = RND.nextFloat() * 6.28f;
        d.rolSnel = (RND.nextFloat() - 0.5f) * (s == Deeltje.CONFETTI || s == Deeltje.VEER || s == Deeltje.BLAD ? 0.5f : 0.12f);
        d.kleur = kleur;
        d.beeld = RND.nextInt(Math.max(1, s.beelden));
        DEELTJES_LIJST.addLast(d);
    }

    static void mesh(String soort, CompoundTag d, long klok, Vec3 cam) {
        if (!VfxConfig.cosmetisch()) {
            return;
        }
        M m = new M();
        m.soort = soort;
        m.a = new Vec3(d.getDoubleOr("x", d.getDoubleOr("ax", 0)), d.getDoubleOr("y", d.getDoubleOr("ay", 0)), d.getDoubleOr("z", d.getDoubleOr("az", 0)));
        m.b = new Vec3(d.getDoubleOr("bx", m.a.x), d.getDoubleOr("by", m.a.y), d.getDoubleOr("bz", m.a.z));
        m.r = d.getDoubleOr("r", d.getDoubleOr("grootte", d.getDoubleOr("hoogte", 2)));
        m.breedte = d.getDoubleOr("breedte", 0.5);
        m.kleur = d.getIntOr("kleur", 0xFFFFFF);
        m.ticks = d.getIntOr("ticks", soort.equals("flits") ? 7 : 12);
        m.id = d.getIntOr("id", -1);
        m.start = klok;
        if (!soort.equals("spoor") && m.a.lerp(m.b, 0.5).distanceToSqr(cam) > Math.pow(LOD_MESH + m.r, 2)) {
            return;
        }
        if (MESHES.size() > 80) {
            MESHES.remove(0);
        }
        MESHES.add(m);
    }

    // ---- ticking -------------------------------------------------------------------------------------------------------------

    static void tick(long klok, ClientLevel level) {
        for (Iterator<D> it = DEELTJES_LIJST.iterator(); it.hasNext(); ) {
            D d = it.next();
            if (++d.leven >= d.max) {
                it.remove();
                continue;
            }
            d.x += d.vx;
            d.y += d.vy;
            d.z += d.vz;
            d.vy -= d.soort.zwaarte;
            d.vx *= d.soort.rem;
            d.vy *= d.soort.rem;
            d.vz *= d.soort.rem;
            d.rol += d.rolSnel;
            if (d.soort == Deeltje.VEER || d.soort == Deeltje.BLAD || d.soort == Deeltje.CONFETTI) {   // flutter
                d.vx += Math.sin((d.leven + d.beeld * 7) * 0.3) * 0.004;
            }
        }
        for (Iterator<M> it = MESHES.iterator(); it.hasNext(); ) {
            M m = it.next();
            long t = klok - m.start;
            if (t > m.ticks + (m.soort.equals("spoor") ? 12 : 0)) {
                it.remove();
                continue;
            }
            if (m.soort.equals("spoor")) {
                Entity e = level.getEntity(m.id);
                if (e != null && t <= m.ticks) {
                    m.spoor.addLast(e.position().add(0, e.getBbHeight() * 0.5, 0));
                } else if (!m.spoor.isEmpty()) {
                    m.spoor.pollFirst();
                }
                while (m.spoor.size() > 14) {
                    m.spoor.pollFirst();
                }
            } else if (m.soort.equals("zuil") && t % 3 == 0) {   // a few sparkles rising in the pillar
                deeltje(Deeltje.STER, m.a.x + (RND.nextDouble() - 0.5) * 0.8, m.a.y + RND.nextDouble() * m.r * 0.3, m.a.z + (RND.nextDouble() - 0.5) * 0.8, 0,
                        0.12, 0, m.kleur);
            }
        }
    }

    // ---- drawing -------------------------------------------------------------------------------------------------------------

    private static void v(VertexConsumer vc, PoseStack.Pose p, Vec3 cam, double x, double y, double z, float u, float vv, int argb) {
        vc.addVertex(p, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).setColor(argb).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LICHT).setNormal(p, 0, 1, 0);
    }

    static void teken(PoseStack pose, MultiBufferSource.BufferSource buffers, Vec3 cam, Quaternionf rot, float pt, long klok) {
        PoseStack.Pose p = pose.last();
        Vector3f rechts = rot.transform(new Vector3f(1, 0, 0)), op = rot.transform(new Vector3f(0, 1, 0));
        if (!DEELTJES_LIJST.isEmpty()) {
            RenderType t = RenderTypes.entityTranslucentEmissive(DEELTJES, false);
            VertexConsumer vc = buffers.getBuffer(t);
            for (D d : DEELTJES_LIJST) {
                float f = (d.leven + pt) / d.max;
                float a = f < 0.15f ? f / 0.15f : f > 0.6f ? (1 - f) / 0.4f : 1f;
                float s = d.grootte * (d.soort == Deeltje.GLOED ? 1.2f - f * 0.5f : d.soort == Deeltje.ROOK || d.soort == Deeltje.SPOOR ? 0.7f + f * 0.8f : 1f);
                int cel = d.soort.cel + (d.soort.beelden <= 1 ? 0 : (d.beeld + (int) (d.leven / 4f)) % d.soort.beelden);
                float u0 = (cel % 8) / 8f, v0 = (cel / 8) / 4f, u1 = u0 + 1 / 8f, v1 = v0 + 1 / 4f;
                double x = d.x + d.vx * pt, y = d.y + d.vy * pt, z = d.z + d.vz * pt;
                float cs = (float) Math.cos(d.rol), sn = (float) Math.sin(d.rol);
                int k = Decals.argb(Math.max(0, Math.min(1, a)), d.kleur);
                float[][] hoek = {{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
                float[][] uv = {{u0, v1}, {u0, v0}, {u1, v0}, {u1, v1}};
                for (int i = 0; i < 4; i++) {
                    float cx = hoek[i][0] * s, cy = hoek[i][1] * s;
                    float rx = cx * cs - cy * sn, ry = cx * sn + cy * cs;
                    v(vc, p, cam, x + rechts.x * rx + op.x * ry, y + rechts.y * rx + op.y * ry, z + rechts.z * rx + op.z * ry, uv[i][0], uv[i][1], k);
                }
            }
            buffers.endBatch(t);
        }
        if (MESHES.isEmpty()) {
            return;
        }
        RenderType g = RenderTypes.entityTranslucentEmissive(GLOED, false);
        VertexConsumer vc = buffers.getBuffer(g);
        List<M> flitsen = new ArrayList<>();
        for (M m : MESHES) {
            float t = klok - m.start + pt, f = Math.min(1f, t / Math.max(1, m.ticks));
            switch (m.soort) {
                case "golf" -> golf(vc, p, cam, m, f);
                case "straal" -> straal(vc, p, cam, m.a, m.b, (float) m.breedte * (1f + 0.25f * (float) Math.sin(t * 0.8f)),
                        Decals.argb(f < 0.15f ? f / 0.15f : f > 0.6f ? (1 - f) / 0.4f : 1f, m.kleur));
                case "zuil" -> {
                    float a = (f < 0.2f ? f / 0.2f : f > 0.7f ? (1 - f) / 0.3f : 1f) * 0.8f;
                    zuil(vc, p, cam, m, a);
                }
                case "spoor" -> spoor(vc, p, cam, m, f);
                default -> flitsen.add(m);
            }
        }
        buffers.endBatch(g);
        if (!flitsen.isEmpty()) {
            RenderType ft = RenderTypes.entityTranslucentEmissive(FLITS, false);
            VertexConsumer fc = buffers.getBuffer(ft);
            for (M m : flitsen) {
                float f = Math.min(1f, (klok - m.start + pt) / Math.max(1, m.ticks));
                float s = (float) m.r * (0.5f + 0.7f * f);
                int k = Decals.argb((1 - f) * 0.95f, Decals.mengWit(m.kleur, 0.4f));
                double x = m.a.x, y = m.a.y, z = m.a.z;
                v(fc, p, cam, x - rechts.x * s - op.x * s, y - rechts.y * s - op.y * s, z - rechts.z * s - op.z * s, 0, 1, k);
                v(fc, p, cam, x - rechts.x * s + op.x * s, y - rechts.y * s + op.y * s, z - rechts.z * s + op.z * s, 0, 0, k);
                v(fc, p, cam, x + rechts.x * s + op.x * s, y + rechts.y * s + op.y * s, z + rechts.z * s + op.z * s, 1, 0, k);
                v(fc, p, cam, x + rechts.x * s - op.x * s, y + rechts.y * s - op.y * s, z + rechts.z * s - op.z * s, 1, 1, k);
            }
            buffers.endBatch(ft);
        }
    }

    /** The expanding ring on the ground (flat at the anchor's height). */
    private static void golf(VertexConsumer vc, PoseStack.Pose p, Vec3 cam, M m, float f) {
        double r = m.r * (0.15 + 0.85 * Math.sqrt(f)), w = 0.5 + m.r * 0.09;
        int k = Decals.argb((1 - f) * 0.95f, m.kleur);
        int seg = (int) Math.max(16, Math.min(64, r * 4));
        double y = m.a.y + 0.08;
        for (int j = 0; j < seg; j++) {
            double b0 = Math.PI * 2 * j / seg, b1 = Math.PI * 2 * (j + 1) / seg;
            double ri = Math.max(0, r - w);
            v(vc, p, cam, m.a.x + Math.cos(b0) * ri, y, m.a.z + Math.sin(b0) * ri, 0, 0, k);
            v(vc, p, cam, m.a.x + Math.cos(b0) * r, y, m.a.z + Math.sin(b0) * r, 1, 0, k);
            v(vc, p, cam, m.a.x + Math.cos(b1) * r, y, m.a.z + Math.sin(b1) * r, 1, 1, k);
            v(vc, p, cam, m.a.x + Math.cos(b1) * ri, y, m.a.z + Math.sin(b1) * ri, 0, 1, k);
            // a low standing wall too (reads from the side)
            double h = 0.6 * (1 - f);
            v(vc, p, cam, m.a.x + Math.cos(b0) * r, y, m.a.z + Math.sin(b0) * r, 1, 0, k);
            v(vc, p, cam, m.a.x + Math.cos(b0) * r, y + h, m.a.z + Math.sin(b0) * r, 0, 0, k);
            v(vc, p, cam, m.a.x + Math.cos(b1) * r, y + h, m.a.z + Math.sin(b1) * r, 0, 1, k);
            v(vc, p, cam, m.a.x + Math.cos(b1) * r, y, m.a.z + Math.sin(b1) * r, 1, 1, k);
        }
    }

    /** A camera-facing strip from a to b (u across: the soft band). */
    private static void straal(VertexConsumer vc, PoseStack.Pose p, Vec3 cam, Vec3 a, Vec3 b, float w, int k) {
        Vec3 d = b.subtract(a);
        Vec3 zij = d.cross(a.lerp(b, 0.5).subtract(cam));
        if (zij.lengthSqr() < 1e-6) {
            return;
        }
        zij = zij.normalize().scale(w / 2);
        v(vc, p, cam, a.x - zij.x, a.y - zij.y, a.z - zij.z, 0, 0, k);
        v(vc, p, cam, a.x + zij.x, a.y + zij.y, a.z + zij.z, 1, 0, k);
        v(vc, p, cam, b.x + zij.x, b.y + zij.y, b.z + zij.z, 1, 1, k);
        v(vc, p, cam, b.x - zij.x, b.y - zij.y, b.z - zij.z, 0, 1, k);
    }

    private static void zuil(VertexConsumer vc, PoseStack.Pose p, Vec3 cam, M m, float a) {
        Vec3 onder = m.a, boven = m.a.add(0, m.r, 0);
        straal(vc, p, cam, onder, boven, 1.4f, Decals.argb(a * 0.5f, m.kleur));
        straal(vc, p, cam, onder, boven, 0.5f, Decals.argb(a, Decals.mengWit(m.kleur, 0.5f)));
    }

    private static void spoor(VertexConsumer vc, PoseStack.Pose p, Vec3 cam, M m, float f) {
        if (m.spoor.size() < 2) {
            return;
        }
        Vec3[] pt = m.spoor.toArray(new Vec3[0]);
        for (int i = 1; i < pt.length; i++) {
            float a = (float) i / pt.length;
            straal(vc, p, cam, pt[i - 1], pt[i], (float) m.breedte * (0.3f + 0.7f * a), Decals.argb(a * 0.8f, m.kleur));
        }
    }

    static int aantalDeeltjes() {
        return DEELTJES_LIJST.size();
    }

    static void wis() {
        DEELTJES_LIJST.clear();
        MESHES.clear();
    }

    static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
