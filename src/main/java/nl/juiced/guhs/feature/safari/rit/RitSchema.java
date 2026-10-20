package nl.juiced.guhs.feature.safari.rit;

import java.util.List;

/**
 * The timetable of the trucks and the gondolas (S3): pure arithmetic on the game time, so the server and every client
 * put a ride in exactly the same spot without sending positions (the same path, the same clock). Tested in
 * {@code RitGameTests}.
 * <ul>
 *   <li>Trucks drive the route loop at {@link #TRUCK_SNELHEID} and stand {@link #HALTE_WACHT} ticks at every stop.
 *       {@link #trucks} of them run, spread evenly, so one comes by about every {@link #TRUCK_ELKE} ticks.</li>
 *   <li>Two gondolas shuttle between the stations (one at each end), {@link #GONDEL_WACHT} ticks in each station.</li>
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
    public static final int GONDEL_WACHT = 200;

    /** Where a ride is: how far along, and (when it stands still) which stop and how long it still waits. */
    public record Stand(double afstand, int halte, int wachtNog) {
        public boolean staatStil() {
            return halte >= 0;
        }
    }

    /** One loop of a truck, in ticks. */
    public static long truckRonde(double lengte, int haltes) {
        return Math.max(1L, Math.round(lengte / TRUCK_SNELHEID) + (long) haltes * HALTE_WACHT);
    }

    public static int trucks(double lengte, int haltes) {
        return (int) Math.max(1, Math.round((double) truckRonde(lengte, haltes) / TRUCK_ELKE));
    }

    /** Truck k (of n) at game time t on a loop of this length with these stop distances (sorted). */
    public static Stand truck(long t, int k, int n, double lengte, List<Double> haltes) {
        long ronde = truckRonde(lengte, haltes.size());
        long fase = Math.floorMod(t + (long) Math.floor((double) ronde * k / Math.max(1, n)), ronde);
        double vorige = 0;
        double klok = 0;
        for (int i = 0; i < haltes.size(); i++) {
            double d = haltes.get(i);
            double rijden = (d - vorige) / TRUCK_SNELHEID;
            if (fase < klok + rijden) {
                return new Stand(vorige + (fase - klok) * TRUCK_SNELHEID, -1, 0);
            }
            klok += rijden;
            if (fase < klok + HALTE_WACHT) {
                return new Stand(d, i, (int) Math.ceil(klok + HALTE_WACHT - fase));
            }
            klok += HALTE_WACHT;
            vorige = d;
        }
        return new Stand(Math.min(lengte, vorige + (fase - klok) * TRUCK_SNELHEID), -1, 0);
    }

    public static long gondelRonde(double lengte) {
        return 2L * (GONDEL_WACHT + Math.round(lengte / GONDEL_SNELHEID));
    }

    /**
     * Gondola k (0 or 1) at game time t: the fraction from station A (0) to B (1), and the station it waits in (0 = A,
     * 1 = B, -1 = under way).
     */
    public static double[] gondel(long t, int k, double lengte) {
        long ronde = gondelRonde(lengte);
        long reis = Math.round(lengte / GONDEL_SNELHEID);
        long fase = Math.floorMod(t + (k == 0 ? 0 : ronde / 2), ronde);
        if (fase < GONDEL_WACHT) {
            return new double[]{0, 0, GONDEL_WACHT - fase};
        }
        fase -= GONDEL_WACHT;
        if (fase < reis) {
            return new double[]{(double) fase / reis, -1, 0};
        }
        fase -= reis;
        if (fase < GONDEL_WACHT) {
            return new double[]{1, 1, GONDEL_WACHT - fase};
        }
        fase -= GONDEL_WACHT;
        return new double[]{1 - (double) fase / reis, -1, 0};
    }

    private RitSchema() {
    }
}
