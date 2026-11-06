package nl.juiced.guhs.feature.vadsrijk.vfx.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vorm;

/**
 * Ground telegraph DECALS (roadmap 3.2): flat textured quads that follow the ground (a height per block column, looked up once),
 * with three layers: a faint fill of the whole area, the "progress" fill that grows until the hit (circle/cone: outward; donut:
 * from the safe edge out; strip/cross: along its length), and a glowing rim (chevrons, or hearts for a safe spot). The last
 * ticks flash. Two textures, one batch each; no depth write, no culling (the emissive translucent entity pipeline).
 * <p>
 * Decals are gameplay: they always draw (also at VFX level UIT, then without the pulse), up to {@link #MAX} at once.
 */
final class Decals {
    static final Identifier VLAK = Guhs.id("textures/vadsrijk/vfx/decal_vlak.png"), RAND = Guhs.id("textures/vadsrijk/vfx/decal_rand.png"),
            VEILIG = Guhs.id("textures/vadsrijk/vfx/decal_veilig.png");
    static final int MAX = 64, NA = 5;
    private static final int LICHT = 0xF000F0;

    static final class Decal {
        final Vorm vorm;
        final Vec3 anker;
        final double richting;
        final int ticks, kleur, groep;
        final long start;
        final Map<Long, Float> hoogte = new HashMap<>();

        Decal(Vorm vorm, Vec3 anker, double richting, int ticks, int kleur, int groep, long start) {
            this.vorm = vorm;
            this.anker = anker;
            this.richting = richting;
            this.ticks = ticks;
            this.kleur = kleur;
            this.groep = groep;
            this.start = start;
        }
    }

    static final List<Decal> ALLE = new ArrayList<>();

    private Decals() {
    }

    static void nieuw(CompoundTag d, long klok) {
        Vorm v = new Vorm(Vorm.Soort.valueOf(d.getStringOr("vorm", "CIRKEL")), d.getDoubleOr("r", 2), d.getDoubleOr("r2", 0), d.getDoubleOr("hoek", 60),
                d.getDoubleOr("lengte", 0), d.getDoubleOr("breedte", 2));
        if (ALLE.size() >= MAX) {
            ALLE.remove(0);
        }
        ALLE.add(new Decal(v, new Vec3(d.getDoubleOr("x", 0), d.getDoubleOr("y", 0), d.getDoubleOr("z", 0)), d.getDoubleOr("richting", 0),
                d.getIntOr("ticks", 20), d.getIntOr("kleur", 0xFF2A2A), d.getIntOr("groep", 0), klok));
    }

    static void weg(int groep) {
        ALLE.removeIf(x -> x.groep == groep);
    }

    static void tick(long klok) {
        ALLE.removeIf(x -> klok - x.start > x.ticks + NA);
    }

    // ---- the ground ------------------------------------------------------------------------------------------------------------

    /** The ground height at (x, z) near the decal's anchor (top of the first solid block within +2..-4), cached per column. */
    private static float grond(ClientLevel level, Decal d, double x, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        long k = BlockPos.asLong(bx, 0, bz);
        Float h = d.hoogte.get(k);
        if (h != null) {
            return h;
        }
        float y = (float) d.anker.y;
        int y0 = (int) Math.floor(d.anker.y + 0.01);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = 2; dy >= -4; dy--) {
            m.set(bx, y0 + dy - 1, bz);
            BlockState st = level.getBlockState(m);
            VoxelShape s = st.getCollisionShape(level, m);
            if (!s.isEmpty()) {
                BlockState boven = level.getBlockState(m.above());
                if (boven.getCollisionShape(level, m.above()).isEmpty()) {
                    y = (float) (m.getY() + s.max(Direction.Axis.Y));
                    break;
                }
            }
        }
        d.hoogte.put(k, y);
        return y;
    }

    // ---- drawing -------------------------------------------------------------------------------------------------------------

    /** One quad vertex in camera space with a world (x, y, z). */
    private record Ctx(PoseStack.Pose pose, VertexConsumer vc, Vec3 cam, ClientLevel level, Decal d, float lift) {
        void v(double x, double z, float u, float vv, int argb) {
            float y = grond(level, d, x, z) + lift;
            vc.addVertex(pose, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).setColor(argb).setUv(u, vv)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LICHT).setNormal(pose, 0, 1, 0);
        }
    }

    static int argb(float a, int rgb) {
        int al = Math.max(0, Math.min(255, (int) (a * 255)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    static int mengWit(int rgb, float f) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        r += (int) ((255 - r) * f);
        g += (int) ((255 - g) * f);
        b += (int) ((255 - b) * f);
        return (r << 16) | (g << 8) | b;
    }

    static void teken(PoseStack pose, MultiBufferSource.BufferSource buffers, Vec3 cam, float tijd, ClientLevel level) {
        if (ALLE.isEmpty()) {
            return;
        }
        boolean zacht = VfxConfig.cosmetisch();
        RenderType vlakType = RenderTypes.entityTranslucentEmissive(VLAK, false);
        VertexConsumer vlak = buffers.getBuffer(vlakType);
        PoseStack.Pose p = pose.last();
        List<Decal> zichtbaar = new ArrayList<>();
        for (Decal d : ALLE) {
            if (d.anker.distanceToSqr(cam) < Math.pow(96 + d.vorm.bereik(), 2)) {
                zichtbaar.add(d);
            }
        }
        for (Decal d : zichtbaar) {
            float f = Math.min(1f, (tijd - d.start) / d.ticks);
            float puls = zacht ? 0.5f + 0.5f * (float) Math.sin(tijd * 0.35f) : 0.5f;
            boolean veilig = d.vorm.soort() == Vorm.Soort.VEILIG;
            float na = Math.max(0, (tijd - d.start - d.ticks) / NA);   // after the hit: fade out
            float uit = 1f - Math.min(1f, na);
            float basis = (veilig ? 0.42f : 0.30f + 0.06f * puls) * uit;
            float vol = (veilig ? 0.35f : 0.55f) * uit * (f >= 1 ? 1.4f : 1f);
            Ctx c0 = new Ctx(p, vlak, cam, level, d, 0.03f), c1 = new Ctx(p, vlak, cam, level, d, 0.036f);
            vlak(c0, d, 1f, argb(basis, d.kleur));
            vlak(c1, d, veilig ? 1f - f : f, argb(vol, mengWit(d.kleur, 0.15f)));
        }
        buffers.endBatch(vlakType);
        // the rims (chevrons; hearts for safe spots)
        for (boolean veiligLaag : new boolean[] {false, true}) {
            RenderType t = RenderTypes.entityTranslucentEmissive(veiligLaag ? VEILIG : RAND, false);
            VertexConsumer vc = buffers.getBuffer(t);
            boolean iets = false;
            for (Decal d : zichtbaar) {
                if ((d.vorm.soort() == Vorm.Soort.VEILIG) != veiligLaag) {
                    continue;
                }
                float f = Math.min(1f, (tijd - d.start) / d.ticks);
                float na = Math.max(0, (tijd - d.start - d.ticks) / NA);
                float flits = f > 0.85f ? (f - 0.85f) / 0.15f : 0f;
                float a = (0.75f + (zacht ? 0.2f * (float) Math.sin(tijd * 0.5f) : 0f) + 0.25f * flits) * (1f - Math.min(1f, na));
                rand(new Ctx(p, vc, cam, level, d, 0.042f), d, argb(Math.min(1f, a), mengWit(d.kleur, 0.25f + 0.5f * flits)), zacht ? tijd * 0.04f : 0f);
                iets = true;
            }
            if (iets) {
                buffers.endBatch(t);
            }
        }
    }

    /** The area, or the part {@code f} (0..1) of it that the progress covers. u = 0 middle .. 1 edge (the texture's gradient). */
    private static void vlak(Ctx c, Decal d, float f, int kleur) {
        if (f <= 0.001f) {
            return;
        }
        Vorm v = d.vorm;
        switch (v.soort()) {
            case CIRKEL, VEILIG -> wig(c, d, 0, v.r() * f, v.r(), -Math.PI, Math.PI, kleur);
            case KEGEL -> {
                double h = Math.toRadians(v.hoek()) / 2;
                wig(c, d, 0, v.r() * f, v.r(), d.richting - h, d.richting + h, kleur);
            }
            case DONUT -> wig(c, d, v.r(), v.r() + (v.r2() - v.r()) * f, v.r2(), -Math.PI, Math.PI, kleur);
            case LIJN -> strook(c, d, d.richting, 0, v.lengte() * f, v.breedte() / 2, kleur);
            case KRUIS -> {
                for (int i = 0; i < 4; i++) {
                    strook(c, d, d.richting + Math.PI / 2 * i, i == 0 || i == 2 ? 0 : v.breedte() / 2, v.lengte() * f, v.breedte() / 2, kleur);
                }
            }
        }
    }

    /** A ring/wedge piece from radius r0 to r1 between angles a0..a1 (u = r / rMax). */
    private static void wig(Ctx c, Decal d, double r0, double r1, double rMax, double a0, double a1, int kleur) {
        if (r1 <= r0 + 0.01) {
            return;
        }
        int ringen = (int) Math.max(1, Math.min(10, Math.ceil((r1 - r0) / 1.2)));
        int seg = (int) Math.max(8, Math.min(64, Math.ceil((a1 - a0) * r1 / 1.1)));
        for (int i = 0; i < ringen; i++) {
            double ra = r0 + (r1 - r0) * i / ringen, rb = r0 + (r1 - r0) * (i + 1) / ringen;
            float ua = (float) (ra / rMax), ub = (float) (rb / rMax);
            for (int j = 0; j < seg; j++) {
                double b0 = a0 + (a1 - a0) * j / seg, b1 = a0 + (a1 - a0) * (j + 1) / seg;
                float v0 = (float) j / seg, v1 = (float) (j + 1) / seg;
                double cx = d.anker.x, cz = d.anker.z;
                c.v(cx + Math.cos(b0) * ra, cz + Math.sin(b0) * ra, ua * 0.98f, v0, kleur);
                c.v(cx + Math.cos(b0) * rb, cz + Math.sin(b0) * rb, ub * 0.98f, v0, kleur);
                c.v(cx + Math.cos(b1) * rb, cz + Math.sin(b1) * rb, ub * 0.98f, v1, kleur);
                c.v(cx + Math.cos(b1) * ra, cz + Math.sin(b1) * ra, ua * 0.98f, v1, kleur);
            }
        }
    }

    /** A strip from the anchor along {@code richting}, from s0 to s1, half width w (u = |across| / w). */
    private static void strook(Ctx c, Decal d, double richting, double s0, double s1, double w, int kleur) {
        if (s1 <= s0 + 0.01) {
            return;
        }
        double ux = Math.cos(richting), uz = Math.sin(richting), nx = -uz, nz = ux;
        int n = (int) Math.max(1, Math.min(24, Math.ceil((s1 - s0) / 1.0)));
        for (int i = 0; i < n; i++) {
            double a = s0 + (s1 - s0) * i / n, b = s0 + (s1 - s0) * (i + 1) / n;
            float v0 = (float) i / n, v1 = (float) (i + 1) / n;
            for (int kant = -1; kant <= 1; kant += 2) {
                double ax = d.anker.x + ux * a, az = d.anker.z + uz * a, bx = d.anker.x + ux * b, bz = d.anker.z + uz * b;
                c.v(ax, az, 0f, v0, kleur);
                c.v(ax + nx * w * kant, az + nz * w * kant, 0.98f, v0, kleur);
                c.v(bx + nx * w * kant, bz + nz * w * kant, 0.98f, v1, kleur);
                c.v(bx, bz, 0f, v1, kleur);
            }
        }
    }

    /** The glowing rim: strips along the outline, u = 0 at the edge .. 1 inside, v = 0..1 per segment (one chevron/heart each). */
    private static void rand(Ctx c, Decal d, int kleur, float schuif) {
        Vorm v = d.vorm;
        double w = 0.45;
        switch (v.soort()) {
            case CIRKEL, VEILIG -> boog(c, d, v.r(), -1, -Math.PI, Math.PI, w, kleur, schuif);
            case DONUT -> {
                boog(c, d, v.r2(), -1, -Math.PI, Math.PI, w, kleur, schuif);
                boog(c, d, v.r(), 1, -Math.PI, Math.PI, w, kleur, schuif);
            }
            case KEGEL -> {
                double h = Math.toRadians(v.hoek()) / 2;
                boog(c, d, v.r(), -1, d.richting - h, d.richting + h, w, kleur, schuif);
                rechtRand(c, d, d.anker.x, d.anker.z, d.richting - h, v.r(), 1, w, kleur);
                rechtRand(c, d, d.anker.x, d.anker.z, d.richting + h, v.r(), -1, w, kleur);
            }
            case LIJN -> rechthoek(c, d, d.richting, 0, v.lengte(), v.breedte() / 2, w, kleur);
            case KRUIS -> {
                rechthoek(c, d, d.richting, -v.lengte(), v.lengte(), v.breedte() / 2, w, kleur);
                rechthoek(c, d, d.richting + Math.PI / 2, -v.lengte(), v.lengte(), v.breedte() / 2, w, kleur);
            }
        }
    }

    /** An arc of rim at radius r; {@code binnen} = -1: the inside is towards the centre, +1: away from it. */
    private static void boog(Ctx c, Decal d, double r, int binnen, double a0, double a1, double w, int kleur, float schuif) {
        int seg = (int) Math.max(8, Math.min(72, Math.ceil((a1 - a0) * r / 1.0)));
        double ri = r + binnen * w;
        for (int j = 0; j < seg; j++) {
            double b0 = a0 + (a1 - a0) * j / seg, b1 = a0 + (a1 - a0) * (j + 1) / seg;
            float v0 = 0f, v1 = 1f;   // one mark per segment
            double cx = d.anker.x, cz = d.anker.z;
            c.v(cx + Math.cos(b0) * r, cz + Math.sin(b0) * r, 0f, v0, kleur);
            c.v(cx + Math.cos(b0) * ri, cz + Math.sin(b0) * ri, 0.98f, v0, kleur);
            c.v(cx + Math.cos(b1) * ri, cz + Math.sin(b1) * ri, 0.98f, v1, kleur);
            c.v(cx + Math.cos(b1) * r, cz + Math.sin(b1) * r, 0f, v1, kleur);
        }
    }

    /** A straight rim from (x, z) along angle a for length l; the inside is on side {@code kant} (+1 left, -1 right). */
    private static void rechtRand(Ctx c, Decal d, double x, double z, double a, double l, int kant, double w, int kleur) {
        double ux = Math.cos(a), uz = Math.sin(a), nx = -uz * kant, nz = ux * kant;
        int n = (int) Math.max(1, Math.ceil(l));
        for (int i = 0; i < n; i++) {
            double s0 = l * i / n, s1 = l * (i + 1) / n;
            c.v(x + ux * s0, z + uz * s0, 0f, 0f, kleur);
            c.v(x + ux * s0 + nx * w, z + uz * s0 + nz * w, 0.98f, 0f, kleur);
            c.v(x + ux * s1 + nx * w, z + uz * s1 + nz * w, 0.98f, 1f, kleur);
            c.v(x + ux * s1, z + uz * s1, 0f, 1f, kleur);
        }
    }

    /** The rim of a strip from s0 to s1 (half width hw) along a. */
    private static void rechthoek(Ctx c, Decal d, double a, double s0, double s1, double hw, double w, int kleur) {
        double ux = Math.cos(a), uz = Math.sin(a), nx = -uz, nz = ux;
        double x0 = d.anker.x + ux * s0, z0 = d.anker.z + uz * s0;
        rechtRand(c, d, x0 + nx * hw, z0 + nz * hw, a, s1 - s0, -1, w, kleur);
        rechtRand(c, d, x0 - nx * hw, z0 - nz * hw, a, s1 - s0, 1, w, kleur);
        // the two ends
        double ex = d.anker.x + ux * s1, ez = d.anker.z + uz * s1;
        rechtRand(c, d, ex + nx * hw, ez + nz * hw, a - Math.PI / 2, hw * 2, -1, w, kleur);
        rechtRand(c, d, x0 - nx * hw, z0 - nz * hw, a + Math.PI / 2, hw * 2, -1, w, kleur);
    }

    static int aantal() {
        return ALLE.size();
    }

    static Iterator<Decal> iterator() {
        return ALLE.iterator();
    }
}
