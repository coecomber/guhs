package nl.juiced.guhs.feature.race;

import java.util.Locale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * A player's guhrace records (kept in {@link GuhQuests#saved}, so they survive dying): the best total time with its
 * checkpoint times and the recording of that race (the ghost), the best lap, how many races, and whether the ghost
 * should drive along.
 * <p>
 * 2.9: records are kept per track and level. The old racebaan at medium keeps its 2.4 keys (suffix ""); every other
 * track / level has the suffix {@link RaceBaan#records} (e.g. "_regenboog_lastig"). Without a suffix the methods read the
 * old keys. The golden ghost (the track record holder's race) can be switched off per player ({@link #goudOn}).
 */
public final class RaceRecords {
    static final String BEST = "guhs_race_best", BEST_LAP = "guhs_race_best_lap", RACES = "guhs_race_races", FIRST = "guhs_race_first",
            GHOST = "guhs_race_ghost", SPLITS = "guhs_race_splits", GHOST_OFF = "guhs_race_ghost_off", GOUD_OFF = "guhs_race_goud_off";

    /** Best total time in ticks, or -1. */
    public static int best(Player player) {
        return best(player, "");
    }

    public static int best(Player player, String suffix) {
        CompoundTag data = GuhQuests.saved(player);
        return data.contains(BEST + suffix) ? data.getIntOr(BEST + suffix, 0) : -1;
    }

    public static int bestLap(Player player) {
        return bestLap(player, "");
    }

    public static int bestLap(Player player, String suffix) {
        CompoundTag data = GuhQuests.saved(player);
        return data.contains(BEST_LAP + suffix) ? data.getIntOr(BEST_LAP + suffix, 0) : -1;
    }

    public static int races(Player player) {
        return races(player, "");
    }

    public static int races(Player player, String suffix) {
        return GuhQuests.saved(player).getIntOr(RACES + suffix, 0);
    }

    /** The recorded positions of the best race (x, y, z in 1/16 blocks in the track's frame, every RaceGame.SAMPLE ticks). */
    public static int[] ghost(Player player) {
        return ghost(player, "");
    }

    public static int[] ghost(Player player, String suffix) {
        return GuhQuests.saved(player).getIntArray(GHOST + suffix).orElse(new int[0]);
    }

    /** The race time at every checkpoint of the best race. */
    public static int[] splits(Player player) {
        return splits(player, "");
    }

    public static int[] splits(Player player, String suffix) {
        return GuhQuests.saved(player).getIntArray(SPLITS + suffix).orElse(new int[0]);
    }

    public static boolean ghostOn(Player player) {
        return !GuhQuests.saved(player).getBooleanOr(GHOST_OFF, false);
    }

    public static void toggleGhost(Player player) {
        CompoundTag data = GuhQuests.saved(player);
        data.putBoolean(GHOST_OFF, !data.getBooleanOr(GHOST_OFF, false));
    }

    /** Does the golden ghost (the track record, someone else's) race along? On unless switched off. */
    public static boolean goudOn(Player player) {
        return !GuhQuests.saved(player).getBooleanOr(GOUD_OFF, false);
    }

    public static void toggleGoud(Player player) {
        CompoundTag data = GuhQuests.saved(player);
        data.putBoolean(GOUD_OFF, !data.getBooleanOr(GOUD_OFF, false));
    }

    /** Saves a finished race; returns true when it's a new personal best (the ghost and splits are only kept then). */
    static boolean save(Player player, int total, int bestLapOfRace, int[] splits, int[] ghost) {
        return save(player, "", total, bestLapOfRace, splits, ghost);
    }

    static boolean save(Player player, String suffix, int total, int bestLapOfRace, int[] splits, int[] ghost) {
        CompoundTag data = GuhQuests.saved(player);
        data.putInt(RACES + suffix, data.getIntOr(RACES + suffix, 0) + 1);
        int lap = bestLap(player, suffix);
        if (lap < 0 || bestLapOfRace < lap) {
            data.putInt(BEST_LAP + suffix, bestLapOfRace);
        }
        int best = best(player, suffix);
        if (best >= 0 && total >= best) {
            return false;
        }
        data.putInt(BEST + suffix, total);
        data.putIntArray(SPLITS + suffix, splits);
        data.putIntArray(GHOST + suffix, ghost);
        return true;
    }

    /** First finished race ever (on the old racebaan)? (Then it's marked as done.) */
    static boolean firstFinish(Player player) {
        return firstFinish(player, "");
    }

    /** First finished race ever on this track (key suffix e.g. "_regenboog")? (Then it's marked as done.) */
    static boolean firstFinish(Player player, String baanSuffix) {
        CompoundTag data = GuhQuests.saved(player);
        if (data.getBooleanOr(FIRST + baanSuffix, false)) {
            return false;
        }
        data.putBoolean(FIRST + baanSuffix, true);
        return true;
    }

    /** Has this player ever finished a race on this track (a circuit track id)? */
    public static boolean finishedOnce(Player player, String baanId) {
        return GuhQuests.saved(player).getBooleanOr(FIRST + "_" + baanId, false);
    }

    /** Race time as m:ss.hh (a tick is 5 hundredths). */
    public static String time(int ticks) {
        if (ticks < 0) {
            return "-";
        }
        int hundredths = ticks * 5;
        return String.format(Locale.ROOT, "%d:%02d.%02d", hundredths / 6000, hundredths / 100 % 60, hundredths % 100);
    }

    /** A difference to your record: +1.25 / -0.40 (seconds). */
    public static String delta(int ticks) {
        int hundredths = Math.abs(ticks) * 5;
        return String.format(Locale.ROOT, "%s%d.%02d", ticks > 0 ? "+" : ticks < 0 ? "-" : "±", hundredths / 100, hundredths % 100);
    }

    private RaceRecords() {
    }
}
