package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import net.minecraft.server.MinecraftServer;

/**
 * The local time adapter of the tekenen slice (kern builds {@code Guhwinter.nu/fase/opMoment} in parallel). Everything in this
 * slice asks the time HERE, so integration wires it to kern in one place: replace the body of {@link #nu} with
 * {@code return Guhwinter.nu(server);} and drop the dev offset (kern's {@code /guhs winter datum} takes over).
 * <p>
 * Until then: the real Europe/Amsterdam time, shifted by a dev offset ({@code /guhs winter tekenen tijd <yyyy-MM-dd HH:mm> | echt});
 * the shifted clock keeps running.
 */
public final class TekenTijd {
    public static final ZoneId ZONE = ZoneId.of("Europe/Amsterdam");
    /** Kerstboom drawings can be submitted and edited until 25 Dec 23:59 (so: before this moment). */
    public static final ZonedDateTime KERSTBOOM_TOT = ZonedDateTime.of(2026, 12, 26, 0, 0, 0, 0, ZONE);
    /** Who logs in from 26 Dec up to and including 2 Jan gets a Kerstalbum. */
    public static final ZonedDateTime ALBUM_VAN = KERSTBOOM_TOT;
    public static final ZonedDateTime ALBUM_TOT = ZonedDateTime.of(2027, 1, 3, 0, 0, 0, 0, ZONE);
    /** The event is over (the plaza melts): the tree's little cards go away. */
    public static final ZonedDateTime EINDE = ALBUM_TOT;

    private static volatile Duration offset = Duration.ZERO;

    private TekenTijd() {
    }

    /** Now, in Europe/Amsterdam. INTEGRATION: {@code return nl.juiced.guhs.feature.guhwinter.Guhwinter.nu(server);} */
    public static ZonedDateTime nu(MinecraftServer server) {
        return ZonedDateTime.now(ZONE).plus(offset);
    }

    /** Dev: pretend it is this moment now (the clock keeps running from there); null = the real time. */
    public static void zetDev(java.time.LocalDateTime moment) {
        offset = moment == null ? Duration.ZERO : Duration.between(ZonedDateTime.now(ZONE), moment.atZone(ZONE));
    }

    public static boolean dev() {
        return !offset.isZero();
    }

    /** May kerstboom drawings still be submitted / edited? */
    public static boolean kerstboomOpen(MinecraftServer server) {
        return nu(server).isBefore(KERSTBOOM_TOT);
    }

    /** 26 Dec up to and including 2 Jan: logging in brings a Kerstalbum. */
    public static boolean albumPeriode(MinecraftServer server) {
        ZonedDateTime n = nu(server);
        return !n.isBefore(ALBUM_VAN) && n.isBefore(ALBUM_TOT);
    }

    public static boolean voorbij(MinecraftServer server) {
        return !nu(server).isBefore(EINDE);
    }
}
