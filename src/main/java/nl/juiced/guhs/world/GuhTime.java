package nl.juiced.guhs.world;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
 * <p>
 * 1.10.1 (p1101-tijd): the Vadsrijk (guhs:vadsrijk) is the one exception. Its dimension type has its own world clock
 * {@link #VADSRIJK_KLOK} (default_clock), so for a Vadsrijk level every method here reads/sets THAT clock; every other level
 * keeps the overworld clock. The Vadsrijk clock still counts on the vanilla 24000 scale (0 sunrise, 13000 night), only its
 * speed changes (feature/vadsrijk/tijd/VadsrijkTijd), so every "is it night" threshold keeps working.
 */
public final class GuhTime {
    public static final long DAY = 24000L;
    /** 1.10.1: the Vadsrijk's own world clock (data/guhs/world_clock/vadsrijk.json). */
    public static final ResourceKey<net.minecraft.world.clock.WorldClock> VADSRIJK_KLOK = ResourceKey.create(Registries.WORLD_CLOCK,
            Identifier.fromNamespaceAndPath("guhs", "vadsrijk"));

    private GuhTime() {
    }

    /** 1.21.1 {@code level.getDayTime()}: total ticks of the overworld day clock (1.10.1: of the Vadsrijk clock in the Vadsrijk). */
    public static long dayTime(Level level) {
        return level.clockManager().getTotalTicks(klok(level));
    }

    /** The day clock of this level: the Vadsrijk's own clock when that is its default clock, else the overworld clock. */
    public static Holder<WorldClock> klok(Level level) {
        return klok(level.dimensionType(), level.registryAccess());
    }

    /** {@link #klok(Level)} for a dimension type (also without a loaded level: the gametest server has no Vadsrijk level). */
    public static Holder<WorldClock> klok(net.minecraft.world.level.dimension.DimensionType type, net.minecraft.core.RegistryAccess registries) {
        Optional<Holder<WorldClock>> eigen = type.defaultClock();
        if (eigen.isPresent() && eigen.get().is(VADSRIJK_KLOK)) {
            return eigen.get();
        }
        return registries.getOrThrow(WorldClocks.OVERWORLD);
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

    /** 1.21.1 {@code serverLevel.setDayTime(t)}: sets the overworld day clock (shared by all dimensions, like 1.21.1);
     *  1.10.1: on the Vadsrijk level it sets the Vadsrijk clock only. */
    public static void setDayTime(ServerLevel level, long ticks) {
        level.clockManager().setTotalTicks(klok(level), ticks);
    }
}
