package nl.juiced.guhs.world;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;

/**
 * 26.1 port: the day clock. 26.1 replaced {@code Level#getDayTime/setDayTime/getMoonPhase/getTimeOfDay/isDay/isNight} by
 * world clocks ("not one to one"). In 1.21.1 every dimension (Guhmension, Guheinde, Barbecuether, Guhmaag included) read
 * the overworld's day time (DerivedLevelData), so the overworld clock is the exact replacement everywhere:
 * <pre>
 * level.getDayTime()                    -> GuhTime.dayTime(level)                 (= level.getOverworldClockTime())
 * level.getDayTime() % 24000            -> GuhTime.timeOfDay(level)               (floorMod, 0..23999)
 * level.getDayTime() / 24000            -> GuhTime.day(level)                     (floorDiv)
 * serverLevel.setDayTime(t)             -> GuhTime.setDayTime(serverLevel, t)     (sets the overworld clock: all dimensions, like 1.21.1)
 * level.getMoonPhase()                  -> GuhTime.moonPhase(level)               (0..7, same formula as 1.21.1)
 * level.getTimeOfDay(partialTick)       -> GuhTime.celestialAngle(level)          (0..1 like 1.21.1's DimensionType#timeOfDay)
 * level.isDay() / level.isNight()       -> level.isBrightOutside() / level.isDarkOutside()   (identical code)
 * </pre>
 * Works on both sides (the client gets the clocks from the server).
 */
public final class GuhTime {
    public static final long DAY = 24000L;

    private GuhTime() {
    }

    /** 1.21.1 {@code level.getDayTime()}: total ticks of the overworld day clock. */
    public static long dayTime(Level level) {
        return level.getOverworldClockTime();
    }

    /** Time of day in ticks, 0..23999 (0 = sunrise-ish 6:00, 6000 noon, 18000 midnight). */
    public static long timeOfDay(Level level) {
        return Math.floorMod(dayTime(level), DAY);
    }

    /** The day number (floorDiv of the day time). */
    public static long day(Level level) {
        return Math.floorDiv(dayTime(level), DAY);
    }

    /** 1.21.1 {@code level.getMoonPhase()} (0 = full moon .. 7). */
    public static int moonPhase(Level level) {
        return (int) (dayTime(level) / DAY % 8L + 8L) % 8;   // (exactly 1.21.1's DimensionType#moonPhase)
    }

    /** 1.21.1 {@code level.getTimeOfDay(pt)} (DimensionType#timeOfDay: 0 = noon, 0.5 = midnight, eased). 26.1 dimension
     *  types have no fixed_time value any more (only has_fixed_time), so this always follows the overworld clock. */
    public static float celestialAngle(Level level) {
        double d = Mth.frac(dayTime(level) / (double) DAY - 0.25);
        double e = 0.5 - Math.cos(d * Math.PI) / 2.0;
        return (float) (d * 2.0 + e) / 3.0F;
    }

    /** The overworld clock holder. */
    public static Holder<WorldClock> overworldClock(Level level) {
        return level.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
    }

    /** 1.21.1 {@code serverLevel.setDayTime(t)}: sets the overworld day clock (shared by all dimensions, like 1.21.1). */
    public static void setDayTime(ServerLevel level, long ticks) {
        level.clockManager().setTotalTicks(overworldClock(level), ticks);
    }
}
