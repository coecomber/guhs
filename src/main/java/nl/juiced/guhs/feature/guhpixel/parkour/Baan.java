package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.phys.Vec3;

/**
 * The way a guh takes over one obstacle: pieces of path in the obstacle's LOCAL coordinates (blocks, as if it faces north:
 * x to the right, y up, z to the back, the controller block spans 0..1), each with how many ticks it takes, an arc for
 * jumps and a sound at its start. {@link #plek} is a pure function of the tick, so a run is always the same.
 */
public final class Baan {
    /** What you hear (and see) at the start of a piece. */
    public enum Geluid { GEEN, SPRONG, BOING, LANDING, TUNNEL, TRIP, WIEBEL, PLOF, OEPS }

    /** One piece: from, to, ticks, the arc's height (0 = a straight line), its sound, and whether the guh stands still. */
    public record Deel(Vec3 van, Vec3 naar, int ticks, double boog, Geluid geluid) {
        Vec3 plek(float f) {
            f = Math.max(0f, Math.min(1f, f));
            Vec3 p = van.lerp(naar, f);
            return boog == 0 ? p : p.add(0, boog * 4 * f * (1 - f), 0);
        }
    }

    private final List<Deel> delen = new ArrayList<>();
    private Vec3 nu;
    private int duur;

    private Baan(Vec3 start) {
        this.nu = start;
    }

    public static Baan van(double x, double y, double z) {
        return new Baan(new Vec3(x, y, z));
    }

    /** Walks on to this point at this speed (blocks a tick). */
    public Baan loop(double x, double y, double z, double snelheid, Geluid geluid) {
        Vec3 naar = new Vec3(x, y, z);
        return deel(naar, Math.max(1, (int) Math.ceil(nu.distanceTo(naar) / snelheid)), 0, geluid);
    }

    public Baan loop(double x, double y, double z, double snelheid) {
        return loop(x, y, z, snelheid, Geluid.GEEN);
    }

    /** Jumps to this point in an arc. */
    public Baan sprong(double x, double y, double z, int ticks, double boog, Geluid geluid) {
        return deel(new Vec3(x, y, z), ticks, boog, geluid);
    }

    /** Stays where it is for a moment. */
    public Baan wacht(int ticks, Geluid geluid) {
        return deel(nu, ticks, 0, geluid);
    }

    private Baan deel(Vec3 naar, int ticks, double boog, Geluid geluid) {
        delen.add(new Deel(nu, naar, ticks, boog, geluid));
        nu = naar;
        duur += ticks;
        return this;
    }

    /** The same way from the other end (turned half round about the middle of the controller block). */
    public Baan gespiegeld() {
        Baan b = new Baan(spiegel(delen.isEmpty() ? nu : delen.get(0).van()));
        for (Deel d : delen) {
            b.deel(spiegel(d.naar()), d.ticks(), d.boog(), d.geluid());
        }
        return b;
    }

    private static Vec3 spiegel(Vec3 p) {
        return new Vec3(1 - p.x, p.y, 1 - p.z);
    }

    public int duur() {
        return duur;
    }

    public Vec3 start() {
        return delen.isEmpty() ? nu : delen.get(0).van();
    }

    public Vec3 eind() {
        return nu;
    }

    /** Where the guh is after this many ticks (local). */
    public Vec3 plek(float tick) {
        float t = tick;
        for (Deel d : delen) {
            if (t <= d.ticks()) {
                return d.plek(t / d.ticks());
            }
            t -= d.ticks();
        }
        return nu;
    }

    /** The sound of the piece that starts exactly at this tick, or null. */
    @Nullable
    public Geluid geluidOp(int tick) {
        int t = 0;
        for (Deel d : delen) {
            if (t == tick) {
                return d.geluid() == Geluid.GEEN ? null : d.geluid();
            }
            t += d.ticks();
            if (t > tick) {
                return null;
            }
        }
        return null;
    }

    /** Does the guh stand still at this tick (a piece that goes nowhere)? */
    public boolean stil(int tick) {
        int t = tick;
        for (Deel d : delen) {
            if (t < d.ticks()) {
                return d.van().equals(d.naar());
            }
            t -= d.ticks();
        }
        return true;
    }
}
