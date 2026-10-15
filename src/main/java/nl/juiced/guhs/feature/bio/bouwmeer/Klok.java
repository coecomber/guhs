package nl.juiced.guhs.feature.bio.bouwmeer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.world.GuhTime;

/**
 * The one clock of the biomes3 slice "bouw-meer": the day number (the visser-guh's koivoer, once a day) and the time of
 * day (the jetty lantern, the visser-guh's mood, the hanami guhs). Everything in this package asks here, so a game test
 * can set its own day and hour inside its own box ({@link #zet}) without touching the clock all tests share.
 */
public final class Klok {
    /** Day ticks: dusk and dawn of the lanterns, and when the hanami guhs go to sleep and wake. */
    public static final int LAMP_AAN = 12300, LAMP_UIT = 23700, SLAAP_VAN = 13000, SLAAP_TOT = 23500;

    /** The four moods of the visser-guh. */
    public enum Dagdeel {
        OCHTEND, MIDDAG, AVOND, NACHT;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private record Gezet(long dag, long tijd) {
    }

    private static final Map<AABB, Gezet> GEZET = new ConcurrentHashMap<>();

    /** (Game tests) inside this box it is this day and this time of day; null hands the box back to the real clock. */
    public static void zet(AABB box, @Nullable Long dag, long tijd) {
        if (dag == null) {
            GEZET.remove(box);
        } else {
            GEZET.put(box, new Gezet(dag, Math.floorMod(tijd, GuhTime.DAY)));
        }
    }

    @Nullable
    private static Gezet gezet(BlockPos pos) {
        if (GEZET.isEmpty()) {
            return null;
        }
        for (var e : GEZET.entrySet()) {
            if (e.getKey().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                return e.getValue();
            }
        }
        return null;
    }

    /** The day number here (the overworld's clock: every dimension of the mod follows it). */
    public static long dag(Level level, BlockPos pos) {
        Gezet g = gezet(pos);
        return g != null ? g.dag() : GuhTime.day(level);
    }

    /** The time of day here, 0..23999 (0 = sunrise, 6000 = noon, 12000 = sunset, 18000 = midnight). */
    public static long tijd(Level level, BlockPos pos) {
        Gezet g = gezet(pos);
        return g != null ? g.tijd() : GuhTime.timeOfDay(level);
    }

    /** Lamp time: from dusk to dawn. */
    public static boolean lampAan(Level level, BlockPos pos) {
        long t = tijd(level, pos);
        return t >= LAMP_AAN && t < LAMP_UIT;
    }

    /** Do the hanami guhs sleep now? */
    public static boolean slaaptijd(Level level, BlockPos pos) {
        long t = tijd(level, pos);
        return t >= SLAAP_VAN && t < SLAAP_TOT;
    }

    public static Dagdeel dagdeel(Level level, BlockPos pos) {
        long t = tijd(level, pos);
        return t < 5000 ? Dagdeel.OCHTEND : t < 11000 ? Dagdeel.MIDDAG : t < 13800 ? Dagdeel.AVOND : t < 23000 ? Dagdeel.NACHT : Dagdeel.OCHTEND;
    }

    private Klok() {
    }
}
