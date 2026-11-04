package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * Zone-3 per-player state in {@code SpelerData.extra("z3systemen")} (saved with the player): Jachtpenningen, Gouden Veren, the
 * bounty day, contracts. Plus the daily reset service: a "day" runs from 06:00 to 06:00 Europe/Amsterdam ({@link #dag}), a week from
 * Monday 06:00 ({@link #week}). {@link #KLOK} can be swapped in tests.
 */
public final class Z3Data {
    public static final ZoneId TIJDZONE = ZoneId.of("Europe/Amsterdam");
    public static final int RESET_UUR = 6;
    /** The clock (tests set their own). */
    public static volatile Supplier<ZonedDateTime> KLOK = () -> ZonedDateTime.now(TIJDZONE);

    private Z3Data() {
    }

    public static CompoundTag tag(Player p) {
        return Spelers.data(p).extra("z3systemen");
    }

    /** The reset day of a moment: before 06:00 it still counts as the day before. */
    public static LocalDate resetDag(ZonedDateTime t) {
        return t.withZoneSameInstant(TIJDZONE).minusHours(RESET_UUR).toLocalDate();
    }

    /** Today's key ("2026-11-04"), changes at 06:00 Amsterdam time. */
    public static String dag() {
        return resetDag(KLOK.get()).toString();
    }

    /** This week's key ("2026-W45"), changes on Monday 06:00. */
    public static String week() {
        LocalDate d = resetDag(KLOK.get());
        return d.get(IsoFields.WEEK_BASED_YEAR) + "-W" + d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    /** Seconds until the next 06:00 reset (for the board text). */
    public static long secondenTotReset() {
        ZonedDateTime nu = KLOK.get().withZoneSameInstant(TIJDZONE);
        ZonedDateTime volgende = nu.toLocalDate().atTime(RESET_UUR, 0).atZone(TIJDZONE);
        if (!volgende.isAfter(nu)) {
            volgende = nu.toLocalDate().plusDays(1).atTime(RESET_UUR, 0).atZone(TIJDZONE);
        }
        return Math.max(0, java.time.Duration.between(nu, volgende).getSeconds());
    }

    static DayOfWeek weekdag() {
        return resetDag(KLOK.get()).getDayOfWeek();
    }

    // ---- counters ----------------------------------------------------------------------------------------------------------

    public static int penningen(Player p) {
        return tag(p).getIntOr("penningen", 0);
    }

    public static void geefPenningen(Player p, int n) {
        tag(p).putInt("penningen", Math.max(0, penningen(p) + n));
    }

    public static boolean neemPenningen(Player p, int n) {
        if (penningen(p) < n) {
            return false;
        }
        tag(p).putInt("penningen", penningen(p) - n);
        return true;
    }

    public static int veren(Player p) {
        return tag(p).getIntOr("veren", 0);
    }

    static void zetVeren(Player p, int n) {
        tag(p).putInt("veren", Math.max(0, n));
    }
}
