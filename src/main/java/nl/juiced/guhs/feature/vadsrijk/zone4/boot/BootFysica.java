package nl.juiced.guhs.feature.vadsrijk.zone4.boot;

import net.minecraft.util.Mth;

/**
 * T-Z4BOOT: the sailing maths of the Zeilbootje, pure (no world), so the client that steers and the gametests use the exact same
 * numbers. One {@link #stap} per tick:
 * <ul>
 *   <li><b>sail feel:</b> W hoists the sail (the speed builds up over ~3 s to the top speed), S reefs and rows back slowly, letting
 *       go glides out (it keeps drifting a while); A/D turn the bow (slower turns at full speed);</li>
 *   <li><b>wind:</b> a slowly turning wind ({@link #windRichting}); with the wind behind you the boat is ~15 % faster, against it ~15 %
 *       slower ({@link #windFactor});</li>
 *   <li><b>De Kringstroom:</b> an ellipse round the island chain of zone 4 ({@link Kringstroom}). In its band (from {@code binnen}
 *       to {@code buiten}, as a fraction of the ellipse) the current takes away the outward part of your speed, pushes you back
 *       inward and softly turns the bow inward. At the outer line the push (0.3 blocks a tick) is always more than the outward
 *       speed you have left (0), so you can never get through and never get stuck: the push points to open water inside.</li>
 * </ul>
 */
public final class BootFysica {
    /** Top speed in blocks a tick = soort speed (RijdierSoort.snelheid(level)) x this (0.30 -> 0.6 b/t = 12 blocks a second). */
    public static final double TOP_FACTOR = 2.0;
    /** Speed gained per tick with the sail up (0 -> 0.6 in ~50 ticks) and lost per tick when braking. */
    public static final double ACC = 0.012, REM = 0.025;
    /** Backwards (S): this part of the top speed. */
    public static final double ACHTERUIT = 0.25;
    /** Gliding with no key: the speed per tick is multiplied by this. */
    public static final double UITROL = 0.985;
    /** Degrees a tick: turning when slow / at full speed. */
    public static final float DRAAI_LANGZAAM = 3.6f, DRAAI_SNEL = 2.0f;
    /** The current's push inward (blocks a tick) at the outer line, and how hard it turns the bow inward (degrees a tick). */
    public static final double DUW_MAX = 0.3, DRAAI_STROOM = 2.5;

    private BootFysica() {
    }

    /** The ellipse of the current. The band runs from {@code binnen} to {@code buiten} (fractions of the ellipse radius). */
    public record Kringstroom(double cx, double cz, double a, double b, double binnen, double buiten) {
        /** Where you are as a fraction of the ellipse (0 = the middle, 1 = on the ellipse). */
        public double r(double x, double z) {
            double u = (x - cx) / a, v = (z - cz) / b;
            return Math.sqrt(u * u + v * v);
        }

        /** How deep in the band: 0 inside it, 1 at the outer line, more beyond it. */
        public double diepte(double x, double z) {
            double r = r(x, z);
            return r <= binnen ? 0 : (r - binnen) / (buiten - binnen);
        }

        /** The unit vector pointing outward (the ellipse's normal, x and z). */
        public double[] naarBuiten(double x, double z) {
            double nx = (x - cx) / (a * a), nz = (z - cz) / (b * b);
            double l = Math.sqrt(nx * nx + nz * nz);
            return l < 1e-9 ? new double[] {1, 0} : new double[] {nx / l, nz / l};
        }

        public boolean binnen(double x, double z) {
            return r(x, z) < buiten;
        }
    }

    /** One tick's result: the new forward speed, the turn (degrees) and the horizontal motion. */
    public record Stap(double vaart, float draai, double dx, double dz, double diepte) {
    }

    /** Where the wind blows TO, as a yaw (degrees; same convention as an entity's yaw): turns round slowly over two days, swaying. */
    public static float windRichting(long tijd) {
        double d = tijd / 48000.0 * 360.0 + 35.0 * Math.sin(tijd / 2400.0);
        return (float) Mth.wrapDegrees(d);
    }

    /** How well the wind sits: 1 = straight behind you, -1 = straight against you. */
    public static double windMee(float yaw, long tijd) {
        return Math.cos(Math.toRadians(Mth.wrapDegrees(yaw - windRichting(tijd))));
    }

    /** Speed factor from the wind: 0.85 (against) .. 1.15 (behind). */
    public static double windFactor(float yaw, long tijd) {
        return 0.85 + 0.30 * (1 + windMee(yaw, tijd)) / 2;
    }

    /**
     * One tick of sailing. gas: +1 = W (sail up), -1 = S, 0 = glide; stuur: the strafe input (A = +1 = turn left). top = the top
     * speed (blocks a tick, before wind). stroom may be null (no current, e.g. a dev boat outside zone 4).
     */
    public static Stap stap(double x, double z, float yaw, double vaart, double gas, double stuur, double top, long tijd, Kringstroom stroom) {
        double frac = top <= 0 ? 0 : Mth.clamp(Math.abs(vaart) / top, 0, 1);
        float draai = (float) (-stuur * (DRAAI_LANGZAAM + (DRAAI_SNEL - DRAAI_LANGZAAM) * frac));
        double diepte = stroom == null ? 0 : stroom.diepte(x, z);
        double[] uit = stroom == null ? null : stroom.naarBuiten(x, z);
        if (diepte > 0) {
            // the current softly turns the bow inward
            float binnenYaw = (float) (Mth.atan2(uit[0], -uit[1]) * Mth.RAD_TO_DEG);   // yaw of the direction -uit
            float verschil = Mth.wrapDegrees(binnenYaw - (yaw + draai));
            draai += (float) Mth.clamp(verschil, -DRAAI_STROOM * Math.min(1, diepte), DRAAI_STROOM * Math.min(1, diepte));
        }
        float nieuwYaw = yaw + draai;
        double doel;
        if (gas > 0) {
            doel = top * windFactor(nieuwYaw, tijd) * Math.min(1, gas);
        } else if (gas < 0) {
            doel = -top * ACHTERUIT;
        } else {
            doel = vaart * UITROL;
        }
        double v = vaart;
        if (gas == 0) {
            v = doel;
        } else if (doel > v) {
            v += Math.min(ACC, doel - v);
        } else {
            v -= Math.min(REM, v - doel);
        }
        if (Math.abs(v) < 1e-4) {
            v = 0;
        }
        double rad = nieuwYaw * Mth.DEG_TO_RAD;
        double dx = -Math.sin(rad) * v, dz = Math.cos(rad) * v;
        if (diepte > 0) {
            double weg = Math.min(1, diepte);
            double naarBuiten = dx * uit[0] + dz * uit[1];
            if (naarBuiten > 0) {   // the outward part of your speed fades away in the band (gone at the outer line)
                dx -= uit[0] * naarBuiten * weg;
                dz -= uit[1] * naarBuiten * weg;
            }
            double duw = DUW_MAX * weg + (diepte > 1 ? Math.min(0.3, (diepte - 1) * 0.3) : 0);
            dx -= uit[0] * duw;
            dz -= uit[1] * duw;
        }
        return new Stap(v, draai, dx, dz, diepte);
    }
}
