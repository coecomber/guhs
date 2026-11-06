package nl.juiced.guhs.feature.vadsrijk.tapijt;

import net.minecraft.util.Mth;

/**
 * U-TAPIJT: the flying carpet's movement model, pure (no world), so the client that steers it, the server that checks it and the
 * gametests all use the exact same numbers ({@link TapijtBalans.Knoppen}). Blocks per tick, degrees, yaw like an entity's (0 = +z).
 * <p><b>Horizontal</b> ({@link #stap}): the carpet has its own momentum (vx, vz) apart from where it points (yaw).
 * <ul>
 *   <li>the yaw follows your view, smoothly and at most {@code draai_max} degrees a tick;</li>
 *   <li>W pushes along the yaw: the push fades as you near the top speed (strong at first, smooth at the end);</li>
 *   <li>no W: it glides on and slows down slowly ({@code uitglijden} per tick);</li>
 *   <li>S: brakes; standing still it backs off slowly. A/D: a gentle sideways push;</li>
 *   <li>drift: the momentum turns only gradually toward the yaw ({@code grip}, much less while gliding), so you can turn the carpet
 *       and keep sliding the old way, like a hovercraft on ice. The speed never goes over the top speed.</li>
 * </ul>
 * <p><b>Vertical</b> ({@link #verticaal}): it is pulled softly to {@code zweefhoogte} above the ground or water below it (a spring with
 * a capped climb and a gentle, capped sink: steps make it glide up, ledges make it float down slowly). It can never rise above the
 * hover height except for a small hop (at most {@code hop_max} more). There is no flying.
 * <p><b>Looks</b> ({@link #kanteling}): it banks into turns and dips its nose a little when it speeds up or brakes.
 * <p><b>Server</b> ({@link #teSnel}, {@link #teHoog}): what the server allows before it puts the carpet back.
 */
public final class TapijtFysica {
    /** The rider needs this much room above the carpet; under a low ceiling the carpet hovers lower (down to {@link #MIN_HOOGTE}). */
    public static final double RUITER = 1.6;
    public static final double MIN_HOOGTE = 0.15;
    /** A hop only starts when the carpet is this close to its hover height (no hopping in the air). */
    public static final double HOP_VENSTER = 0.3;
    /** The momentum only turns toward the yaw when the angle is smaller than this (bigger: W/S simply push against it). */
    public static final double DRIFT_HOEK = 110;

    private TapijtFysica() {
    }

    /** One tick horizontally: the new momentum, the new yaw and how much it turned (for the banking). */
    public record Horizontaal(double vx, double vz, float yaw, float draai) {
        public double snelheid() {
            return Math.sqrt(vx * vx + vz * vz);
        }
    }

    /** The unit vector of a yaw (0 = +z, 90 = -x: Minecraft's convention). */
    public static double[] richting(float yaw) {
        double r = yaw * Mth.DEG_TO_RAD;
        return new double[] {-Math.sin(r), Math.cos(r)};
    }

    /**
     * One tick of the carpet's horizontal movement.
     *
     * @param kijkYaw where the rider looks (the carpet turns toward it)
     * @param gas     the forward input: +1 = W, -1 = S, 0 = nothing
     * @param zij     the strafe input: +1 = A (left), -1 = D (right)
     * @param top     the top speed (blocks a tick)
     */
    public static Horizontaal stap(double vx, double vz, float yaw, float kijkYaw, double gas, double zij, double top, TapijtBalans.Knoppen k) {
        // the carpet turns toward your view, smoothly
        float verschil = Mth.wrapDegrees(kijkYaw - yaw);
        float draai = (float) Mth.clamp(verschil * k.draaiVolg(), -k.draaiMax(), k.draaiMax());
        float nieuwYaw = yaw + draai;
        double r = nieuwYaw * Mth.DEG_TO_RAD, sin = Math.sin(r), cos = Math.cos(r);
        double fx = -sin, fz = cos;   // forward
        double v = Math.sqrt(vx * vx + vz * vz);
        double vooruit = vx * fx + vz * fz;
        boolean duwt = false;
        if (gas < 0 && vooruit > 0.01) {
            // S while moving forward: brake (the whole momentum, also the drift)
            double neem = Math.min(v, k.remmen() * top * -gas);
            if (v > 1e-9) {
                vx -= vx / v * neem;
                vz -= vz / v * neem;
            }
            gas = 0;
        }
        // the push in the carpet's own frame (like a player's moveRelative: +x = left): W forward, S backwards (slowly), A/D sideways
        double in = gas > 0 ? Math.min(1, gas) : gas < 0 ? -k.achteruit() : 0;
        double zijIn = Mth.clamp(zij, -1, 1) * k.zijwaarts();
        double l = Math.sqrt(in * in + zijIn * zijIn);
        if (l > 1e-9 && top > 0) {
            double dx = (zijIn * cos - in * sin) / l, dz = (in * cos + zijIn * sin) / l;
            double doel = top * Math.min(1, l);
            double langs = vx * dx + vz * dz;   // the speed it already has along the push
            double duw = k.versnelling() * top * Math.max(0, 1 - langs / doel);   // strong from standstill, fading near the goal
            vx += dx * duw;
            vz += dz * duw;
            duwt = gas > 0;
        }
        if (!duwt) {
            // no W: it glides on and slowly loses speed
            vx *= k.uitglijden();
            vz *= k.uitglijden();
        }
        // drift: the momentum turns only gradually toward where the carpet points
        v = Math.sqrt(vx * vx + vz * vz);
        if (v > 1e-6 && gas >= 0) {
            double hoek = Math.atan2(fx * vz - fz * vx, vx * fx + vz * fz);   // signed angle from forward to the momentum (same sense as the rotation below)
            if (Math.abs(hoek) < Math.toRadians(DRIFT_HOEK)) {
                double grip = gas > 0 ? k.grip() : k.gripGlijden();
                double draaiV = -hoek * grip;
                double c = Math.cos(draaiV), s = Math.sin(draaiV);
                double nx = vx * c - vz * s, nz = vx * s + vz * c;
                vx = nx;
                vz = nz;
            }
        }
        // never over the top speed (also when the top speed was tuned down)
        v = Math.sqrt(vx * vx + vz * vz);
        if (v > top && v > 1e-9) {
            vx *= top / v;
            vz *= top / v;
        }
        if (v < 1e-4 && in == 0 && zijIn == 0) {
            vx = vz = 0;
        }
        return new Horizontaal(vx, vz, nieuwYaw, draai);
    }

    /** The hover height under a ceiling: the rider must fit ({@code ruimte} = free blocks above the ground, +inf = open sky). */
    public static double doelHoogte(double ruimte, TapijtBalans.Knoppen k) {
        return Mth.clamp(ruimte - RUITER, MIN_HOOGTE, k.zweefhoogte());
    }

    /** May a hop start (the carpet is about at its hover height)? */
    public static boolean magHoppen(double hoogte, double doel) {
        return !Double.isNaN(hoogte) && !Double.isInfinite(hoogte) && Math.abs(hoogte - doel) < HOP_VENSTER;
    }

    /**
     * One tick vertically: the new upward speed.
     *
     * @param hoogte the carpet's height above the ground/water below it (+inf or NaN = nothing below in reach: it glides down)
     * @param vy     its upward speed now
     * @param hopV   the upward speed of a hop that starts now, or 0 (the caller checked {@link #magHoppen})
     * @param doel   the hover height ({@link #doelHoogte})
     */
    public static double verticaal(double hoogte, double vy, double hopV, double doel, TapijtBalans.Knoppen k) {
        if (hopV > 0) {
            vy = hopV;
        } else {
            boolean vrij = Double.isNaN(hoogte) || Double.isInfinite(hoogte);
            double wil = vrij ? -k.valMax() : Mth.clamp((doel - hoogte) * k.veer(), -k.valMax(), k.omhoogMax());
            vy = wil > vy ? Math.min(wil, vy + k.omhoogAcc()) : Math.max(wil, vy - k.zwaarte());
        }
        if (!Double.isNaN(hoogte) && !Double.isInfinite(hoogte)) {
            double plafond = doel + k.hopMax();   // never higher than the hover height (+ a hop)
            if (hoogte + vy > plafond) {
                vy = hoogte > plafond ? Math.min(vy, 0) : plafond - hoogte;   // (above it, e.g. off a ledge: no rising, the spring sinks it gently)
            }
        }
        return vy;
    }

    /**
     * The tilt for the looks: {roll, pitch} in degrees, eased from the old values. Roll from the turn (degrees this tick) and the
     * sideways drift; pitch from the speed change (nose up when speeding up).
     */
    public static float[] kanteling(float roll, float pitch, float draai, double zijDrift, double versnelling, TapijtBalans.Knoppen k) {
        double max = k.kantelingMax();
        double doelRoll = Mth.clamp(draai * k.kanteling() + zijDrift * 18 * Math.signum(k.kanteling()), -max, max);
        double doelPitch = Mth.clamp(-versnelling * 120, -7, 7);
        return new float[] {(float) (roll + (doelRoll - roll) * 0.15), (float) (pitch + (doelPitch - pitch) * 0.12)};
    }

    /** Server: moved further in {@code ticks} ticks than the top speed (with the lag margin) allows? */
    public static boolean teSnel(double afstand, int ticks, double top, TapijtBalans.Knoppen k) {
        return afstand > top * k.serverMarge() * ticks + 1.0;
    }

    /** Server: higher above the ground than a carpet can be (hover + hop + slack) while not sinking? */
    public static boolean teHoog(double hoogte, double dalen, TapijtBalans.Knoppen k) {
        return !Double.isNaN(hoogte) && !Double.isInfinite(hoogte) && hoogte > k.zweefhoogte() + k.hopMax() + 1.25 && dalen < 0.3;
    }

    /** How far the carpet glides on from this speed with no key held (blocks): the N screen's "uitglijden" line. */
    public static double uitglijAfstand(double v, TapijtBalans.Knoppen k) {
        double f = k.uitglijden();
        return f >= 1 ? Double.POSITIVE_INFINITY : v * f / (1 - f);
    }
}
