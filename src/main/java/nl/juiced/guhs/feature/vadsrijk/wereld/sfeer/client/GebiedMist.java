package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.world.GuhTime;

/**
 * Idea 10 "Sfeer per gebied": fog colour and density per area, blended softly over ~20 blocks at the borders (kern's area boxes;
 * the Kloof is the ravine polyline of the map generator). The city, the camp and the harbour are warm pink-gold, the
 * Klodderweide bright golden, the Sporenwoud green-gold and misty, the Druipgrotten blue-turquoise, the Kloof cool grey, the
 * Zwamtempel purple. The spore storm turns the Sporenwoud orange-purple, the pink shower the meadow pink.
 */
final class GebiedMist {
    /** id, r, g, b, fog distance factor (1 = vanilla). */
    private record Profiel(String id, float r, float g, float b, float ver) {
    }

    private static final Profiel[] PROFIELEN = {
            new Profiel("zwamzoet", 1.0f, 0.80f, 0.70f, 1.0f),
            new Profiel("knuffelkamp", 1.0f, 0.80f, 0.70f, 1.0f),
            new Profiel("haven", 1.0f, 0.82f, 0.72f, 1.0f),
            new Profiel("klodderweide", 1.0f, 0.88f, 0.50f, 1.0f),
            new Profiel("sporenwoud", 0.66f, 0.80f, 0.42f, 0.5f),
            new Profiel("druipgrotten", 0.32f, 0.72f, 0.80f, 0.55f),
            new Profiel("zwamtempel", 0.62f, 0.42f, 0.86f, 0.62f)};
    private static final Profiel KLOOF = new Profiel("kloof", 0.66f, 0.70f, 0.77f, 0.6f);
    private static final Profiel STORM = new Profiel("storm", 0.92f, 0.50f, 0.64f, 0.42f);
    private static final Profiel BUI = new Profiel("bui", 1.0f, 0.72f, 0.86f, 0.62f);
    /** The Kloof ravine (tools/vadsrijk/wereld/kaart.py KLOOF, world coordinates; radius ~15-20). */
    private static final int[][] KLOOF_LIJN = {{40, -130}, {100, -170}, {170, -230}, {240, -300}, {280, -350}, {360, -260}, {450, -220}};
    private static final float RAND = 10f;

    /** Smoothed result at the camera: colour, strength 0..1, fog factor. */
    private static float r, g, b, kracht, ver = 1f;
    /** Per-area weight at the camera (for the other effects): klodderweide, sporenwoud, zwamzoet. */
    static float wWeide, wWoud, wStad;
    private static boolean klaar;

    private GebiedMist() {
    }

    /** The weight of a box at (x, z): 1 inside (10 blocks from the edge), 0 at 10 blocks outside, smooth in between. */
    static float gewicht(Gebieden.Gebied gb, double x, double z) {
        double s = Math.min(Math.min(x - gb.minX(), gb.maxX() + 1 - x), Math.min(z - gb.minZ(), gb.maxZ() + 1 - z));
        float t = Mth.clamp((float) ((s + RAND) / (2 * RAND)), 0f, 1f);
        return t * t * (3 - 2 * t);
    }

    static float gewicht(String id, double x, double z) {
        Gebieden.Gebied gb = Gebieden.van(id);
        return gb == null ? 0f : gewicht(gb, x, z);
    }

    private static float kloof(double x, double z) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < KLOOF_LIJN.length; i++) {
            double ax = KLOOF_LIJN[i][0], az = KLOOF_LIJN[i][1], bx = KLOOF_LIJN[i + 1][0], bz = KLOOF_LIJN[i + 1][1];
            double vx = bx - ax, vz = bz - az, l2 = vx * vx + vz * vz;
            double t = Mth.clamp(((x - ax) * vx + (z - az) * vz) / l2, 0, 1);
            double dx = x - (ax + t * vx), dz = z - (az + t * vz);
            best = Math.min(best, dx * dx + dz * dz);
        }
        float t = Mth.clamp((float) ((38 - Math.sqrt(best)) / 20), 0f, 1f);
        return t * t * (3 - 2 * t);
    }

    /** Client tick: blend the profiles at the player and ease towards it. */
    static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        double x = mc.player.getX(), z = mc.player.getZ();
        float sw = 0, sr = 0, sg = 0, sb = 0, sv = 0;
        float storm = Momenten.storm(mc.level), bui = Momenten.bui(mc.level);
        wWeide = gewicht("klodderweide", x, z);
        wWoud = gewicht("sporenwoud", x, z);
        wStad = gewicht("zwamzoet", x, z);
        for (Profiel p : PROFIELEN) {
            float w = gewicht(p.id(), x, z);
            if (w <= 0) {
                continue;
            }
            Profiel q = p;
            if (p.id().equals("sporenwoud") && storm > 0) {
                q = mix(p, STORM, storm);
            } else if (p.id().equals("klodderweide") && bui > 0) {
                q = mix(p, BUI, bui);
            }
            sw += w;
            sr += w * q.r();
            sg += w * q.g();
            sb += w * q.b();
            sv += w * q.ver();
        }
        float k = kloof(x, z);
        if (k > 0) {
            // the Kloof wins over the box it is in (it cuts through the ring and the city's edge)
            float rest = 1 - k;
            sr = sr * rest + k * KLOOF.r();
            sg = sg * rest + k * KLOOF.g();
            sb = sb * rest + k * KLOOF.b();
            sv = sv * rest + k * KLOOF.ver();
            sw = sw * rest + k;
        }
        float tr, tg, tb, tv, tk;
        if (sw > 0.001f) {
            tr = sr / sw;
            tg = sg / sw;
            tb = sb / sw;
            tv = sv / sw;
            tk = Math.min(1f, sw);
        } else {
            tr = r;
            tg = g;
            tb = b;
            tv = 1f;
            tk = 0f;
        }
        if (!klaar) {
            r = tr;
            g = tg;
            b = tb;
            ver = tv;
            kracht = tk;
            klaar = true;
            return;
        }
        float e = 0.08f;
        r += (tr - r) * e;
        g += (tg - g) * e;
        b += (tb - b) * e;
        ver += (tv - ver) * e;
        kracht += (tk - kracht) * e;
    }

    private static Profiel mix(Profiel a, Profiel c, float f) {
        return new Profiel(a.id(), Mth.lerp(f, a.r(), c.r()), Mth.lerp(f, a.g(), c.g()), Mth.lerp(f, a.b(), c.b()), Mth.lerp(f, a.ver(), c.ver()));
    }

    static void wis() {
        klaar = false;
        kracht = 0;
    }

    /** Daylight 0.06..1 (vanilla-like: 0 = night). */
    static float daglicht() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 1f;
        }
        float a = GuhTime.celestialAngle(mc.level);
        return Mth.clamp(Mth.cos(a * Mth.TWO_PI) * 2f + 0.5f, 0.06f, 1f);
    }

    static void kleur(ViewportEvent.ComputeFogColor e) {
        if (kracht <= 0.001f || e.getCamera().getFluidInCamera() != FogType.ATMOSPHERIC && e.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float m = kracht * 0.78f * SfeerConfig.sterkte();
        float d = daglicht();
        e.setRed(Mth.lerp(m, e.getRed(), r * d));
        e.setGreen(Mth.lerp(m, e.getGreen(), g * d));
        e.setBlue(Mth.lerp(m, e.getBlue(), b * d));
    }

    static void afstand(ViewportEvent.RenderFog e) {
        if (kracht <= 0.001f || ver >= 0.999f || e.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        float f = Mth.lerp(kracht * SfeerConfig.sterkte(), 1f, ver);
        e.setFarPlaneDistance(e.getFarPlaneDistance() * f);
        e.setNearPlaneDistance(e.getNearPlaneDistance() * f);
    }
}
