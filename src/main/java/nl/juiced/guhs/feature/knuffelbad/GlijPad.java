package nl.juiced.guhs.feature.knuffelbad;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The path of a water slide (written by tools/features/knuffelbad_glij.py, which builds the slide's blocks from the very
 * same path): samples every {@link #stap} blocks, in the frame of the slide's start block facing north, relative to the
 * bottom centre of that block. Per sample: the running surface under the middle of the ride (p), the direction of the
 * ride (t), "right" (b, horizontal) and the cross-section of the slide there:
 * <ul>
 *   <li>{@link #GOOT} an open chute, {@link #TRECHTER} a funnel's wall: the surface is h(d) = k d&sup2; + j d above the
 *       middle, d sideways (lateral &times; dmax blocks);</li>
 *   <li>{@link #BUIS} a round tube of radius R: the ride goes round it (lateral &times; dmax radians);</li>
 *   <li>{@link #LUCHT} flying (the drop through a funnel, a launch), {@link #WATER} floating on the pool at the end.</li>
 * </ul>
 * The ride's speed is the same on both sides (a table of the distance per tick, {@link #sAt}): gravity along the path,
 * and on a surface a pull towards the section's preferred speed (water pushes you on, the slide's friction holds you).
 */
public final class GlijPad {
    public static final int GOOT = 0, TRECHTER = 1, BUIS = 2, LUCHT = 3, WATER = 4;
    public static final int FX_STERREN = 1, FX_SPETTERS = 2, FX_DRAAI = 4, FX_PLONS = 8, FX_DONKER = 16, FX_LICHTRING = 32,
            FX_VAL = 64, FX_SCHUIM = 128, FX_START = 256;
    /** Blocks after a change of profile in which the ride glides from the old cross-section into the new one (like the
     * generator's OVERGANG): a rider off the middle never jumps (chute into funnel, tube into flight, ...). */
    public static final double OVERGANG = 2.5;
    /** Blocks of a flight in which the rider turns upright (the normal goes to straight up; RECHTOP in the generator). */
    public static final double RECHTOP = 1.5;
    private static final Vec3 OMHOOG = new Vec3(0, 1, 0);

    public final String id;
    public final double stap, lengte, g, relax, vMin, vMax, ring;
    private final int n;
    private final float[] p, t, b;
    private final byte[] prof;
    private final float[] k, j, dmax, radius, vpref, dyds;
    private final int[] fx;
    /** Per sample: the first sample of its section (the same profile). */
    private final int[] sectie;
    /** Duck spots: {s, lateral}. */
    public final List<float[]> eendjes;
    /** Where the rider gets off (local) and which way they look then (yaw, in the local frame). */
    public final Vec3 uitstap;
    public final float uitstapYaw;
    /** Named spots along the path (s): "plons" (the big splash), "lanceer", ... */
    public final Map<String, Double> merk;
    /** The ride: how far along the path (s) after n ticks. */
    private final double[] ritS;

    private static final Map<Glijbaan, GlijPad> CACHE = Collections.synchronizedMap(new EnumMap<>(Glijbaan.class));

    /** A spot on the ride: where the ring touches the surface, the surface's normal, and the ride's frame there. */
    public record Stand(Vec3 pos, Vec3 normal, Vec3 tangent, Vec3 rechts, int prof, int fx) {
        /** Where the middle of the ring is. */
        public Vec3 midden(double ring) {
            return pos.add(normal.scale(ring));
        }
    }

    public static GlijPad van(Glijbaan baan) {
        return CACHE.computeIfAbsent(baan, bb -> laad(bb.id()));
    }

    /** Reads assets/guhs/knuffelbad/&lt;id&gt;.json from the mod's own files (on the client and on a dedicated server). */
    public static GlijPad laad(String id) {
        String path = "/assets/guhs/knuffelbad/" + id + ".json";
        try (InputStream in = GlijPad.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing slide path " + path);
            }
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return new GlijPad(o);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("can't read slide path " + path, e);
        }
    }

    private static float[] floats(JsonObject o, String key) {
        JsonArray a = o.getAsJsonArray(key);
        float[] out = new float[a.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = a.get(i).getAsFloat();
        }
        return out;
    }

    GlijPad(JsonObject o) {
        this.id = o.get("id").getAsString();
        this.stap = o.get("stap").getAsDouble();
        this.lengte = o.get("lengte").getAsDouble();
        this.g = o.get("g").getAsDouble();
        this.relax = o.get("relax").getAsDouble();
        this.vMin = o.get("v_min").getAsDouble();
        this.vMax = o.get("v_max").getAsDouble();
        this.ring = o.get("ring").getAsDouble();
        this.p = floats(o, "p");
        this.t = floats(o, "t");
        this.b = floats(o, "b");
        this.n = p.length / 3;
        float[] pr = floats(o, "prof");
        this.prof = new byte[n];
        for (int i = 0; i < n; i++) {
            prof[i] = (byte) pr[i];
        }
        this.k = floats(o, "k");
        this.j = floats(o, "j");
        this.dmax = floats(o, "dmax");
        this.radius = floats(o, "R");
        this.vpref = floats(o, "vpref");
        float[] f = floats(o, "fx");
        this.fx = new int[n];
        for (int i = 0; i < n; i++) {
            fx[i] = (int) f[i];
        }
        this.sectie = new int[n];
        for (int i = 1; i < n; i++) {
            sectie[i] = prof[i] == prof[i - 1] ? sectie[i - 1] : i;
        }
        List<float[]> ducks = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("eendjes")) {
            JsonArray a = e.getAsJsonArray();
            ducks.add(new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()});
        }
        this.eendjes = List.copyOf(ducks);
        float[] u = floats(o, "uitstap");
        this.uitstap = new Vec3(u[0], u[1], u[2]);
        this.uitstapYaw = u[3];
        Map<String, Double> m = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("merk").entrySet()) {
            m.put(e.getKey(), e.getValue().getAsDouble());
        }
        this.merk = Map.copyOf(m);
        // the slope per sample (like numpy.gradient: central differences, one-sided at the ends)
        this.dyds = new float[n];
        for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - 1), c = Math.min(n - 1, i + 1);
            dyds[i] = (float) ((p[c * 3 + 1] - p[a * 3 + 1]) / ((c - a) * stap));
        }
        this.ritS = simuleer();
    }

    // =================================================================================================================
    // the ride's speed: the same on the server and on the client (and in the generator, knuffelbad_glij.Samples.rit)
    // =================================================================================================================
    private double[] simuleer() {
        List<Double> ss = new ArrayList<>();
        double s = 0, v = 0.25;
        ss.add(0.0);
        while (s < lengte - 1e-6 && ss.size() < 20000) {
            int i = Math.min(n - 1, (int) (s / stap));
            double f = s / stap - i;
            int i2 = Math.min(n - 1, i + 1);
            double slope = dyds[i] * (1 - f) + dyds[i2] * f;
            double a = prof[i] == LUCHT ? -g * slope : -g * slope + (vpref[i] - v) * relax;
            v = Math.min(vMax, v + a);
            if (prof[i] != LUCHT) {
                v = Math.max(vMin, v);
            }
            v = Math.max(0.02, v);
            s = Math.min(lengte, s + v);
            ss.add(s);
        }
        double[] out = new double[ss.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = ss.get(i);
        }
        return out;
    }

    /** How many ticks the ride takes from the start to the end of the path. */
    public int duur() {
        return ritS.length - 1;
    }

    /** How far along the path (blocks) after tau ticks (a fraction is fine: for smooth frames). */
    public double sAt(double tau) {
        if (tau <= 0) {
            return 0;
        }
        int i = (int) Math.floor(tau);
        if (i >= ritS.length - 1) {
            return lengte;
        }
        return Mth.lerp(tau - i, ritS[i], ritS[i + 1]);
    }

    /** The speed (blocks per tick) around tick tau. */
    public double snelheid(double tau) {
        int i = Mth.clamp((int) Math.floor(tau), 0, ritS.length - 2);
        return ritS[i + 1] - ritS[i];
    }

    /** The first tick at which the ride is at least at s. */
    public double tauAt(double s) {
        int lo = 0, hi = ritS.length - 1;
        if (s <= 0) {
            return 0;
        }
        if (s >= ritS[hi]) {
            return hi;
        }
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (ritS[mid] < s) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double span = ritS[hi] - ritS[lo];
        return lo + (span <= 0 ? 0 : (s - ritS[lo]) / span);
    }

    // =================================================================================================================
    // the samples
    // =================================================================================================================
    public int samples() {
        return n;
    }

    private int index(double s) {
        return Mth.clamp((int) Math.floor(s / stap), 0, n - 1);
    }

    private Vec3 vec(float[] a, int i) {
        return new Vec3(a[i * 3], a[i * 3 + 1], a[i * 3 + 2]);
    }

    private Vec3 lerpVec(float[] a, double s) {
        int i = index(s);
        int i2 = Math.min(n - 1, i + 1);
        double f = Mth.clamp(s / stap - i, 0, 1);
        return new Vec3(Mth.lerp(f, a[i * 3], a[i2 * 3]), Mth.lerp(f, a[i * 3 + 1], a[i2 * 3 + 1]), Mth.lerp(f, a[i * 3 + 2], a[i2 * 3 + 2]));
    }

    public int profiel(double s) {
        return prof[index(s + stap * 0.5)];
    }

    public int effecten(double s) {
        return fx[index(s + stap * 0.5)];
    }

    public double voorkeur(double s) {
        return vpref[index(s)];
    }

    /**
     * The ride at s (blocks along the path) with lateral lat (-1 left .. 1 right), in the local frame: where the ring
     * touches the surface, the surface's normal there, the ride's direction and right. Just after a change of profile the
     * old cross-section glides into the new one (knuffelbad_glij.Samples.punt does the same).
     */
    public Stand stand(double s, double lat) {
        s = Mth.clamp(s, 0, lengte);
        int i = index(s + stap * 0.5);
        Vec3 pos0 = lerpVec(p, s);
        Vec3 tan = lerpVec(t, s).normalize();
        Vec3 right = lerpVec(b, s);
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(tan).normalize();
        lat = Mth.clamp(lat, -1, 1);
        int i0 = sectie[i];
        double inSectie = s - (i0 - 0.5) * stap;
        Vec3[] nu = doorsnede(i, pos0, right, up, lat, inSectie);
        Vec3 pos = nu[0], normal = nu[1];
        if (i0 > 0 && inSectie < OVERGANG) {
            double w = glad(inSectie / OVERGANG);
            Vec3[] oud = doorsnede(i0 - 1, pos0, right, up, lat, Double.MAX_VALUE);
            pos = oud[0].lerp(pos, w);
            normal = oud[1].lerp(normal, w).normalize();
        }
        return new Stand(pos, normal, tan, right, prof[i], fx[i]);
    }

    /** The surface point and its normal in this frame with the cross-section of sample q ({pos, normal}). */
    private Vec3[] doorsnede(int q, Vec3 pos0, Vec3 right, Vec3 up, double lat, double inSectie) {
        if (prof[q] == BUIS) {
            double r = radius[q];
            double phi = lat * dmax[q];
            Vec3 c = pos0.add(up.scale(r));
            Vec3 pos = c.add(right.scale(r * Math.sin(phi))).subtract(up.scale(r * Math.cos(phi)));
            return new Vec3[]{pos, c.subtract(pos).normalize()};
        }
        double d = lat * dmax[q];
        double kk = k[q], jj = j[q];
        Vec3 pos = pos0.add(right.scale(d)).add(up.scale(kk * d * d + jj * d));
        Vec3 normal = up.subtract(right.scale(2 * kk * d + jj)).normalize();
        if (prof[q] == LUCHT) {          // flying: the rider turns upright (the ring falls flat into the water: PLONS)
            normal = normal.lerp(OMHOOG, glad(inSectie / RECHTOP)).normalize();
        }
        return new Vec3[]{pos, normal};
    }

    private static double glad(double x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** How sharply the ride turns (radians per block, horizontal; positive = to the right) around s. */
    public double bocht(double s) {
        double ds = Math.max(stap, 1.0);
        Vec3 a = lerpVec(t, Math.max(0, s - ds)), c = lerpVec(t, Math.min(lengte, s + ds));
        double ha = Math.atan2(a.x, -a.z), hc = Math.atan2(c.x, -c.z);
        return Mth.wrapDegrees(Math.toDegrees(hc - ha)) * Mth.DEG_TO_RAD / (2 * ds);
    }

    // =================================================================================================================
    // from the slide's frame to the world: the start block's position and facing
    // =================================================================================================================
    /** Turns a vector of the slide's frame (made facing north) to face another way (like the start block). */
    public static Vec3 draai(Vec3 v, Direction facing) {
        return switch (facing) {
            case SOUTH -> new Vec3(-v.x, v.y, -v.z);
            case EAST -> new Vec3(-v.z, v.y, v.x);
            case WEST -> new Vec3(v.z, v.y, -v.x);
            default -> v;
        };
    }

    /** A slide as it stands in the world: its path, the start block and which way it faces. */
    public record Baan(GlijPad pad, BlockPos start, Direction facing) {
        public Vec3 oorsprong() {
            return Vec3.atBottomCenterOf(start);
        }

        public Vec3 wereld(Vec3 local) {
            return oorsprong().add(draai(local, facing));
        }

        public Vec3 richting(Vec3 local) {
            return draai(local, facing);
        }

        /** The ride's spot in the world. */
        public Stand stand(double s, double lat) {
            Stand l = pad.stand(s, lat);
            return new Stand(wereld(l.pos()), richting(l.normal()), richting(l.tangent()), richting(l.rechts()), l.prof(), l.fx());
        }

        /** Where the rider gets off, and the yaw (minecraft degrees) they look then. */
        public Vec3 uitstap() {
            return wereld(pad.uitstap);
        }

        public float uitstapYaw() {
            return pad.uitstapYaw + facing.toYRot() - 180f;
        }
    }
}
