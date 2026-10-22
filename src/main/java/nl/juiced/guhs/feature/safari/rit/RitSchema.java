package nl.juiced.guhs.feature.safari.rit;

import java.util.List;

/**
 * The timetable of the trucks, the gondolas and the glass-bottom boats (S3, round 3): pure arithmetic on the game time, so the server and every client
 * put a ride in exactly the same spot without sending positions (the same path, the same clock). Tested in
 * {@code RitGameTests}.
 * <ul>
 *   <li>Trucks drive the route loop at {@link #TRUCK_SNELHEID} and stand {@link #HALTE_WACHT} ticks at every stop.
 *       {@link #trucks} of them run, spread evenly, so one comes by about every {@link #TRUCK_ELKE} ticks.</li>
 *   <li>{@link #GONDELS} gondolas go round the kabelbaan loop (two parallel lines, a turning loop in each station) and
 *       stand {@link #GONDEL_WACHT} ticks in every station.</li>
 *   <li>One glass-bottom boat per boat route goes round (or out and back) and waits {@link #BOOT_WACHT} at its jetty;
 *       the boat line (round 4) runs {@link #boten} boats that stop {@link #BOOT_LIJN_WACHT} at every jetty.</li>
 * </ul>
 */
public final class RitSchema {
    /** Blocks per tick (4.4 blocks a second: a calm safari pace). */
    public static final double TRUCK_SNELHEID = 0.22;
    /** Ticks a truck stands at a stop (hop on, hop off, a fact from the guide). */
    public static final int HALTE_WACHT = 160;
    /** About one truck a minute at every stop. */
    public static final int TRUCK_ELKE = 1200;
    public static final double GONDEL_SNELHEID = 0.18;
    /** A gondola stands still about 5 seconds in a station (it turns round in the station's loop along the platform). */
    public static final int GONDEL_WACHT = 100, GONDELS = 6;
    /** The glass-bottom boat: slow (so you can look down) and a long stop at the jetty. */
    public static final double BOOT_SNELHEID = 0.135;   // test round 5: ~50% faster (was 0.09)
    public static final int BOOT_WACHT = 240;
    /** Round 4: the boat LINE up the river (hop on, hop off at every jetty): a little faster, about one boat every 2 minutes. */
    public static final double BOOT_LIJN_SNELHEID = 0.225;   // test round 5: ~50% faster (was 0.15)
    public static final int BOOT_LIJN_WACHT = 200, BOOT_ELKE = 2400;

    /** Where a ride is: how far along, and (when it stands still) which stop and how long it still waits. */
    public record Stand(double afstand, int halte, int wachtNog) {
        public boolean staatStil() {
            return halte >= 0;
        }
    }

    /** One loop of a ride along a loop of this length with this many stops, in ticks. */
    public static long ronde(double lengte, int haltes, double snelheid, int wacht) {
        return Math.max(1L, Math.round(lengte / snelheid) + (long) haltes * wacht);
    }

    /**
     * Ride k (of n, spread evenly) at game time t on a loop of this length with these stop distances (sorted): it drives
     * at snelheid blocks a tick and stands wacht ticks at every stop.
     */
    public static Stand lus(long t, int k, int n, double lengte, List<Double> haltes, double snelheid, int wacht) {
        long ronde = ronde(lengte, haltes.size(), snelheid, wacht);
        long fase = Math.floorMod(t + (long) Math.floor((double) ronde * k / Math.max(1, n)), ronde);
        double vorige = 0;
        double klok = 0;
        for (int i = 0; i < haltes.size(); i++) {
            double d = haltes.get(i);
            double rijden = (d - vorige) / snelheid;
            if (fase < klok + rijden) {
                return new Stand(vorige + (fase - klok) * snelheid, -1, 0);
            }
            klok += rijden;
            if (fase < klok + wacht) {
                return new Stand(d, i, (int) Math.ceil(klok + wacht - fase));
            }
            klok += wacht;
            vorige = d;
        }
        return new Stand(Math.min(lengte, vorige + (fase - klok) * snelheid), -1, 0);
    }

    /** One loop of a truck, in ticks. */
    public static long truckRonde(double lengte, int haltes) {
        return ronde(lengte, haltes, TRUCK_SNELHEID, HALTE_WACHT);
    }

    public static int trucks(double lengte, int haltes) {
        return (int) Math.max(1, Math.round((double) truckRonde(lengte, haltes) / TRUCK_ELKE));
    }

    /** Truck k (of n) at game time t on a loop of this length with these stop distances (sorted). */
    public static Stand truck(long t, int k, int n, double lengte, List<Double> haltes) {
        return lus(t, k, n, lengte, haltes, TRUCK_SNELHEID, HALTE_WACHT);
    }

    /** Gondola k (of {@link #GONDELS}) on the kabelbaan loop. */
    public static Stand gondel(long t, int k, double lengte, List<Double> stations) {
        return lus(t, k, GONDELS, lengte, stations, GONDEL_SNELHEID, GONDEL_WACHT);
    }

    /** The glass-bottom boat (one per boat route, calm) with its jetty at distance 0. */
    public static Stand boot(long t, double lengte) {
        return lus(t, 0, 1, lengte, List.of(0.0), BOOT_SNELHEID, BOOT_WACHT);
    }

    /** How many boats run the boat line (spread evenly, so one comes by about every {@link #BOOT_ELKE} ticks). */
    public static int boten(double lengte, int haltes) {
        long ronde = ronde(lengte, haltes, BOOT_LIJN_SNELHEID, BOOT_LIJN_WACHT);
        return (int) Math.max(1, Math.round((double) ronde / BOOT_ELKE));
    }

    /** Boat k (of n) on the boat line at game time t, its jetties at these distances (sorted, the first at 0). */
    public static Stand bootLijn(long t, int k, int n, double lengte, List<Double> haltes) {
        return lus(t, k, n, lengte, haltes, BOOT_LIJN_SNELHEID, BOOT_LIJN_WACHT);
    }

    private RitSchema() {
    }
}
