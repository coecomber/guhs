package nl.juiced.guhs.feature.vadsrijk.tijd;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import nl.juiced.guhs.world.GuhTime;

/**
 * 1.10.1 (p1101-tijd): the Vadsrijk's own day/night.
 * <p>
 * <b>How.</b> 26.1 has data-driven world clocks: every dimension type names its {@code default_clock}, and its sky, sun, moon,
 * stars, sky light (so {@code isDarkOutside}, monster light, bees, ...) come from timelines that run on a clock. The Vadsrijk
 * type now names its own clock {@code guhs:vadsrijk} (data/guhs/world_clock/vadsrijk.json) with copies of the vanilla day/moon/
 * early_game timelines on that clock (tag {@code #guhs:in_vadsrijk}). So:
 * <ul>
 *   <li>the overworld clock (sleeping, {@code /time set} in the overworld) never touches it, and {@code /time set} inside the
 *       Vadsrijk only moves the Vadsrijk clock;</li>
 *   <li>the vanilla clock sync ({@code ClientboundSetTimePacket}, sent at login, on every clock change and every second) carries
 *       it to the client, which runs it on with its rate between packets: smooth sky, no mixin, no own packet;</li>
 *   <li>the clock is saved by vanilla's own SavedData {@code world_clocks} (data/world_clocks.dat) with the overworld clock:
 *       total ticks, partial tick, rate, paused.</li>
 * </ul>
 * <b>Non-linear.</b> The clock keeps the vanilla 24000 scale (0 = sunrise, 6000 noon, 12000 sunset starts, 13000 night,
 * 18000 midnight, 23000 dawn), so every threshold in the mod (Rooster 13000..23000, music, critters, quests) keeps meaning the
 * same moment. Only its speed (the NeoForge clock rate) changes, per half: clock 0..12000 (day) runs at
 * {@code 12000 / (dag_deel * cycle)} clock ticks per game tick, clock 12000..24000 (dusk, night, dawn) at
 * {@code 12000 / ((1 - dag_deel) * cycle)}. {@link #pas} sets the rate of the half the clock is in, every tick (a change is
 * sent to every client at once: twice per cycle, or when the config changes).
 */
public final class VadsrijkTijd {
    public static final long DAG = GuhTime.DAY;
    /** Clock tick where the day half ends (vanilla: sunset starts; dusk + night + dawn = 12000..24000). */
    public static final long DAG_EINDE = 12000L;
    public static final double TICKS_PER_MINUUT = 1200.0;

    private VadsrijkTijd() {
    }

    // ---- the mapping (pure) --------------------------------------------------------------------------------------------------

    /** Is this clock time in the day half (0..11999 of the day)? */
    public static boolean dagHelft(long klok) {
        return Math.floorMod(klok, DAG) < DAG_EINDE;
    }

    /** Clock ticks per game tick at this clock time for this cycle (real minutes) and day share. */
    public static double tempo(long klok, double cyclusMinuten, double dagDeel) {
        double deel = dagHelft(klok) ? dagDeel : 1.0 - dagDeel;
        return DAG_EINDE / (deel * cyclusMinuten * TICKS_PER_MINUUT);
    }

    /** Real game ticks of the day half. */
    public static double dagTicks(double cyclusMinuten, double dagDeel) {
        return DAG_EINDE / tempo(0, cyclusMinuten, dagDeel);
    }

    /** Real game ticks of the dusk + night + dawn half. */
    public static double nachtTicks(double cyclusMinuten, double dagDeel) {
        return (DAG - DAG_EINDE) / tempo(DAG_EINDE, cyclusMinuten, dagDeel);
    }

    /** Clock time as a 24h clock text (0 = 06:00, 6000 = 12:00, 18000 = 00:00). */
    public static String uur(long klok) {
        long t = Math.floorMod(klok, DAG);
        long uur = (t / 1000 + 6) % 24, min = (t % 1000) * 60 / 1000;
        return String.format(java.util.Locale.ROOT, "%02d:%02d", uur, min);
    }

    // ---- the server ----------------------------------------------------------------------------------------------------------

    public static Optional<Holder<WorldClock>> klok(MinecraftServer server) {
        return server.registryAccess().get(GuhTime.VADSRIJK_KLOK).map(h -> (Holder<WorldClock>) h);
    }

    /** The Vadsrijk clock's total ticks (-1 when the clock is missing). */
    public static long nu(MinecraftServer server) {
        return klok(server).map(h -> server.clockManager().getTotalTicks(h)).orElse(-1L);
    }

    /** The rate the clock should run at now (config + which half it is in). */
    public static float gewenstTempo(long klok) {
        return (float) tempo(klok, TijdConfig.cyclus(), TijdConfig.dagDeel());
    }

    /** Every server tick: give the Vadsrijk clock the rate of the half it is in (no-op when it already has it). */
    public static void pas(MinecraftServer server) {
        Optional<Holder<WorldClock>> h = klok(server);
        if (h.isEmpty()) {
            return;
        }
        ServerClockManager m = server.clockManager();
        float r = gewenstTempo(m.getTotalTicks(h.get()));
        if (Math.abs(m.getRate(h.get()) - r) > 1.0e-6f) {
            m.setRate(h.get(), r);
        }
    }

    /** {@code zet dag|nacht}: on to the next morning (1000) or evening (13000) of the Vadsrijk, like vanilla's /time set day. */
    public static boolean zet(MinecraftServer server, boolean nacht) {
        Optional<Holder<WorldClock>> h = klok(server);
        if (h.isEmpty()) {
            return false;
        }
        boolean ok = server.clockManager().moveToTimeMarker(h.get(), nacht ? ClockTimeMarkers.NIGHT : ClockTimeMarkers.DAY);
        pas(server);
        return ok;
    }
}
