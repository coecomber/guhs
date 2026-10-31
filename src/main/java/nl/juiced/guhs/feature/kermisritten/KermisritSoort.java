package nl.juiced.guhs.feature.kermisritten;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * The four turning rides of the Guhkermis. All geometry lives here, as pure functions of the time, in the ride's own frame:
 * origin at the middle of the bottom of the controller block, drawn for facing south (the ride's gate side). The numbers
 * follow the designs in kermis-ontwerp/_bron/kermiskit_modules.py. The stamp builds the parts that stand still; the
 * renderer draws the parts that move.
 */
public enum KermisritSoort {
    /** A tower with a long arm that swings over: two cages of three seats, kept level. */
    BOOSTER("booster", new RitProfiel(800, 120, 6), 6, 44, 48, new Vec3(3, 1, 9), new Vec3(0, 1, 8)) {
        @Override
        Vec3 stoel(int i, double t) {
            Vec3 midden = gondel(i / 3, t);
            return new Vec3(midden.x, midden.y - 2.5, midden.z + i % 3 - 1);
        }
    },
    /** Twelve swings around a standing column; they fly outwards as it speeds up. */
    ZWEEFMOLEN("zweefmolen", new RitProfiel(700, 100, 8), 12, 24, 22, new Vec3(3, 1, 11), new Vec3(0, 1, 11)) {
        @Override
        Vec3 stoel(int i, double t) {
            double hoek = zweefHoek(i, t), uit = zweefUit(t);
            double r = ZWEEF_R + ZWEEF_TOUW * Math.sin(uit);
            return new Vec3(r * Math.cos(hoek), ZWEEF_TOP - ZWEEF_TOUW * Math.cos(uit), r * Math.sin(hoek));
        }
    },
    /** A big wheel of eight hanging cabins with two seats each. */
    REUZENRAD("reuzenrad", new RitProfiel(1200, 160, 2), 16, 36, 22, new Vec3(3, 1, 6), new Vec3(0, 1, 5)) {
        @Override
        Vec3 stoel(int i, double t) {
            Vec3 midden = cabine(i / 2, t);
            return new Vec3(midden.x, midden.y - 2.5, i % 2 == 0 ? -0.6 : 0.6);
        }
    },
    /** Three arms around the middle, on each end a star of three little cars that turns the other way. */
    TWISTER("twister", new RitProfiel(800, 120, 7), 9, 12, 16, new Vec3(3, 1, 12.5), new Vec3(0, 1, 12)) {
        @Override
        Vec3 stoel(int i, double t) {
            int arm = i / 3;
            Vec3 naaf = twisterNaaf(arm, t);
            double b = twisterKruis(arm, t) + (i % 3) * Math.PI * 2 / 3;
            return new Vec3(naaf.x + KRUIS_R * Math.cos(b), 1.5, naaf.z + KRUIS_R * Math.sin(b));
        }
    };

    /** Booster: the arm's axle, and how far out the two cages hang. */
    public static final double BOOSTER_AS_Y = 40.5, BOOSTER_AS_Z = 4, BOOSTER_ARM = 20, BOOSTER_RUST = Math.toRadians(58);
    /** Zweefmolen: the ring the ropes hang from, and the ropes; how far they swing out at full speed. */
    public static final double ZWEEF_R = 10, ZWEEF_TOP = 12, ZWEEF_TOUW = 6, ZWEEF_UIT = Math.toRadians(38);
    /** Reuzenrad: its axle and where the cabins hang. */
    public static final double RAD_AS_Y = 19.5, RAD_R = 13;
    /** Twister: where the stars sit and how far out their cars. */
    public static final double TWIST_ARM = 7.5, KRUIS_R = 3.4;
    public static final int RAD_CABINES = 8, ZWEEF_STOELEN = 12, TWIST_ARMEN = 3;

    public final String id;
    public final RitProfiel profiel;
    public final int stoelen;
    /** How high above the controller the ride reaches (its light is read just above that). */
    public final int hoogte;
    /** How far the moving parts reach sideways (draw box, finding its own seats). */
    public final int bereik;
    /** The ticket post: the place to get on. */
    public final Vec3 loket;
    /** Where riders are let out. */
    public final Vec3 uitstap;

    KermisritSoort(String id, RitProfiel profiel, int stoelen, int hoogte, int bereik, Vec3 loket, Vec3 uitstap) {
        this.id = id;
        this.profiel = profiel;
        this.stoelen = stoelen;
        this.hoogte = hoogte;
        this.bereik = bereik;
        this.loket = loket;
        this.uitstap = uitstap;
    }

    public String blokId() {
        return "kermisrit_" + id;
    }

    /** Where seat i is after t ticks of riding (t &lt;= 0: at rest), in the ride's frame. */
    abstract Vec3 stoel(int i, double t);

    /** Seat i, or the ticket post for index -1, in the ride's frame. */
    public Vec3 plek(int index, double t) {
        return index < 0 ? loket : stoel(index, t);
    }

    /** The ride's frame to the world: turned by the controller's facing (south stays as it is). */
    public static Vec3 draai(Vec3 v, Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(-v.x, v.y, -v.z);
            case EAST -> new Vec3(v.z, v.y, -v.x);
            case WEST -> new Vec3(-v.z, v.y, v.x);
            default -> v;
        };
    }

    // --- the moving parts (the renderer uses the same numbers as the seats) ------------------------------------------------

    /** Booster: the arm's angle. */
    public double armHoek(double t) {
        return BOOSTER_RUST + profiel.hoek(t);
    }

    /** Booster: the middle of cage 0 or 1. */
    public Vec3 gondel(int g, double t) {
        double a = armHoek(t), s = g == 0 ? BOOSTER_ARM : -BOOSTER_ARM;
        return new Vec3(s * Math.cos(a), BOOSTER_AS_Y + s * Math.sin(a), BOOSTER_AS_Z);
    }

    /** Zweefmolen: around the column, swing i. */
    public double zweefHoek(int i, double t) {
        return i * Math.PI * 2 / ZWEEF_STOELEN + Math.toRadians(7.4) + profiel.hoek(t);
    }

    /** Zweefmolen: how far the swings fly out. */
    public double zweefUit(double t) {
        return ZWEEF_UIT * profiel.vaartNu(t);
    }

    /** Reuzenrad: how far the wheel turned. */
    public double radHoek(double t) {
        return profiel.hoek(t);
    }

    /** Reuzenrad: the point cabin g hangs from. */
    public Vec3 cabine(int g, double t) {
        double a = g * Math.PI * 2 / RAD_CABINES + Math.PI / 8 + radHoek(t);
        return new Vec3(RAD_R * Math.cos(a), RAD_AS_Y + RAD_R * Math.sin(a), 0);
    }

    /** Twister: the big three-armed star's angle. */
    public double twisterHoek(double t) {
        return Math.toRadians(25) + profiel.hoek(t);
    }

    /** Twister: the hub at the end of an arm. */
    public Vec3 twisterNaaf(int arm, double t) {
        double a = twisterHoek(t) + arm * Math.PI * 2 / TWIST_ARMEN;
        return new Vec3(TWIST_ARM * Math.cos(a), 0, TWIST_ARM * Math.sin(a));
    }

    /** Twister: the little star on an arm: it counts back three times, a whole number, so the rest pose comes back. */
    public double twisterKruis(int arm, double t) {
        return twisterHoek(t) + arm * Math.PI * 2 / TWIST_ARMEN + Math.toRadians(40) - 3 * profiel.hoek(t);
    }
}
