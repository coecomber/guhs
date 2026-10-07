package nl.juiced.guhs.feature.guhpixel;

import java.util.TimeZone;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Real time for everything guhpixel (trips, payslips, the daily cap, the daily offer): {@link #nu} is the wall clock plus
 * an offset that only the dev command and the tests change ({@link #spoel}); the offset is saved (PxData), so skipped time
 * stays skipped after a restart. <b>Every real-time feature stores absolute {@code Klok.nu()} stamps and compares with
 * {@code Klok.nu()}</b>: never System.currentTimeMillis(), never counted ticks. Nothing needs ticking while a player is offline.
 */
public final class Klok {
    public static final long UUR = 3_600_000L, DAG = 24 * UUR;
    private static final String SLEUTEL = "KlokOffset";
    private static volatile long offset;

    /** Milliseconds. */
    public static long nu() {
        return System.currentTimeMillis() + offset;
    }

    /** The real day number in the server's time zone. */
    public static long dag() {
        long nu = nu();
        return Math.floorDiv(nu + TimeZone.getDefault().getOffset(nu), DAG);
    }

    /** Milliseconds until the next real day starts. */
    public static long totMorgen() {
        long nu = nu();
        return DAG - Math.floorMod(nu + TimeZone.getDefault().getOffset(nu), DAG);
    }

    /** (Dev command, tests) skips forward (or back, when negative). */
    public static void spoel(long ms) {
        offset += ms;
        bewaar();
    }

    /** (Dev command, tests) back to the real time. */
    public static void herstel() {
        offset = 0;
        bewaar();
    }

    /** The skipped time in milliseconds. */
    public static long offset() {
        return offset;
    }

    /** Server start: the saved offset. */
    static void laad(MinecraftServer server) {
        offset = PxData.algemeen(server).getLongOr(SLEUTEL, 0L);
    }

    private static void bewaar() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            PxData.algemeen(server).putLong(SLEUTEL, offset);
            PxData.vuil(server);
        }
    }

    private Klok() {
    }
}
