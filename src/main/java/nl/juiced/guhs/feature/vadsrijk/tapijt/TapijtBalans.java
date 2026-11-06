package nl.juiced.guhs.feature.vadsrijk.tapijt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import net.minecraft.nbt.CompoundTag;

/**
 * U-TAPIJT: every number of the flying carpet's feel in one place. They live in the one balans file
 * ({@code config/guhs-vadsrijk-balans.toml}, section {@code [tapijt]}, read/written by klassen.KlasseBalans) and can be tuned live
 * with {@code /guhs vadsrijk tapijt tune <sleutel> <waarde>} (or {@code /guhs vadsrijk balans tapijt <sleutel> <waarde>}).
 * <p>The riding CLIENT moves the carpet (prediction), so the server sends these numbers to every player whenever they change
 * ({@link #versie()}, {@link TapijtPayloads}); in single player both sides share this class anyway.
 * <p>Speeds are blocks per tick (20 ticks = 1 second), angles degrees per tick.
 */
public final class TapijtBalans {
    /** key -> {default, min, max}; the order is the order in the file and in the command's list. */
    private static final Map<String, double[]> KNOPPEN = new LinkedHashMap<>();

    static {
        knop("topfactor", 2.1, 0.5, 5);          // top speed = mount speed (0.36 at lv 1, + level growth) x this: 0.76 b/t = 15 blocks a second
        knop("versnelling", 0.07, 0.005, 0.5);   // W: part of the way that is left to the top speed, gained per tick (0.07: 63 % in ~0.7 s, 95 % in ~2 s)
        knop("uitglijden", 0.984, 0.8, 0.999);   // no W: speed kept per tick (0.984: half the speed after ~2 s, ~45 blocks from top speed)
        knop("remmen", 0.04, 0.005, 0.5);        // S while moving forward: speed taken off per tick, as a part of the top speed
        knop("achteruit", 0.25, 0, 1);           // S standing still: backwards, this part of the top speed
        knop("zijwaarts", 0.4, 0, 1);            // A/D: sideways push, this part of W's push
        knop("grip", 0.045, 0, 1);               // W held: how fast the momentum turns toward where the carpet points (part of the angle per tick)
        knop("grip_glijden", 0.012, 0, 1);       // no W: the same while gliding (low = it drifts on in the old direction)
        knop("draai_volg", 0.2, 0.01, 1);        // the carpet turns toward where you look: part of the difference per tick
        knop("draai_max", 5.0, 0.5, 30);         // ... at most this many degrees per tick
        knop("zweefhoogte", 1.25, 0.3, 3);       // hover height over ground / water (blocks)
        knop("veer", 0.22, 0.02, 1);             // how hard it is pulled to the hover height (part of the difference per tick)
        knop("omhoog_max", 0.42, 0.05, 1.5);     // fastest climb (steps, slopes)
        knop("omhoog_acc", 0.09, 0.01, 1);       // climbing speed gained per tick
        knop("zwaarte", 0.035, 0.005, 0.2);      // sinking speed gained per tick (floaty: vanilla gravity is 0.08)
        knop("val_max", 0.14, 0.02, 1);          // fastest sink: gliding down from a ledge (0.14 = 2.8 blocks a second)
        knop("hop_max", 1.5, 0, 4);              // a hop (space) lifts it at most this far above the hover height
        knop("hop_kracht", 1.0, 0, 3);           // x the mount's jump stat = the hop's upward speed
        knop("kanteling", 3.5, -10, 10);         // banking: degrees of tilt per degree/tick of turning (negative = the other way)
        knop("kanteling_max", 20, 0, 45);        // most tilt (degrees)
        knop("server_marge", 1.35, 1.05, 5);     // the server allows this x the top speed (lag) before it puts the carpet back
    }

    public static final List<String> SLEUTELS = List.copyOf(KNOPPEN.keySet());
    private static final Map<String, Double> WAARDEN = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile int versie = 1;

    private TapijtBalans() {
    }

    private static void knop(String k, double standaard, double min, double max) {
        KNOPPEN.put(k, new double[] {standaard, min, max});
    }

    public static boolean bestaat(String k) {
        return KNOPPEN.containsKey(k);
    }

    public static double standaard(String k) {
        double[] d = KNOPPEN.get(k);
        return d == null ? 1.0 : d[0];
    }

    public static double get(String k) {
        Double v = WAARDEN.get(k);
        return v != null ? v : standaard(k);
    }

    /** Sets one knob (clamped to its sane range). Returns the value that was set. */
    public static double zet(String k, double v) {
        double[] d = KNOPPEN.get(k);
        if (d == null) {
            return Double.NaN;
        }
        double w = Math.max(d[1], Math.min(d[2], v));
        Double oud = WAARDEN.put(k, w);
        if (oud == null || oud != w) {
            versie++;
        }
        return w;
    }

    /** From KlasseBalans (file / command): every knob at once. */
    public static void zet(ToDoubleFunction<String> bron) {
        for (String k : SLEUTELS) {
            zet(k, bron.applyAsDouble(k));
        }
    }

    /** Back to every default (tests). */
    public static void standaardAlles() {
        WAARDEN.clear();
        versie++;
    }

    /** Goes up whenever a number changes (the server then sends them to the players). */
    public static int versie() {
        return versie;
    }

    public static CompoundTag alsTag() {
        CompoundTag t = new CompoundTag();
        for (String k : SLEUTELS) {
            t.putDouble(k, get(k));
        }
        return t;
    }

    /** Client: the numbers from the server. */
    public static void vanTag(CompoundTag t) {
        for (String k : SLEUTELS) {
            t.getDouble(k).ifPresent(v -> zet(k, v));
        }
    }

    // ---- the knobs as one immutable set (the physics takes one; tests build their own) ----------------------------------------

    /** A snapshot of all knobs, so a tick of physics never sees half a change. */
    public record Knoppen(double topfactor, double versnelling, double uitglijden, double remmen, double achteruit, double zijwaarts, double grip,
                      double gripGlijden, double draaiVolg, double draaiMax, double zweefhoogte, double veer, double omhoogMax, double omhoogAcc,
                      double zwaarte, double valMax, double hopMax, double hopKracht, double kanteling, double kantelingMax, double serverMarge) {
    }

    public static Knoppen nu() {
        return new Knoppen(get("topfactor"), get("versnelling"), get("uitglijden"), get("remmen"), get("achteruit"), get("zijwaarts"), get("grip"),
                get("grip_glijden"), get("draai_volg"), get("draai_max"), get("zweefhoogte"), get("veer"), get("omhoog_max"), get("omhoog_acc"),
                get("zwaarte"), get("val_max"), get("hop_max"), get("hop_kracht"), get("kanteling"), get("kanteling_max"), get("server_marge"));
    }

    /** The defaults (tests: the model's promises are about these). */
    public static Knoppen standaardKnoppen() {
        return new Knoppen(standaard("topfactor"), standaard("versnelling"), standaard("uitglijden"), standaard("remmen"), standaard("achteruit"),
                standaard("zijwaarts"), standaard("grip"), standaard("grip_glijden"), standaard("draai_volg"), standaard("draai_max"),
                standaard("zweefhoogte"), standaard("veer"), standaard("omhoog_max"), standaard("omhoog_acc"), standaard("zwaarte"),
                standaard("val_max"), standaard("hop_max"), standaard("hop_kracht"), standaard("kanteling"), standaard("kanteling_max"),
                standaard("server_marge"));
    }
}
