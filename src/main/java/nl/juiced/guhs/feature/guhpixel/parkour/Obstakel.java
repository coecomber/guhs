package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.List;

import nl.juiced.guhs.feature.guhpixel.parkour.Baan.Geluid;

/**
 * The six new obstacles of the Guh-parkour: their size (parts and shapes, local and in model pixels like every
 * ToestelBlock) and the way a guh takes them ({@link #baan}). The wip of the design is the existing Guh-wip.
 * <ul>
 *   <li>HORDE: a run-up and a jump over the bar;</li>
 *   <li>SPRINGPLANK: up the little ramp and boing, far through the air (one way only: up the ramp);</li>
 *   <li>KRUIPTUNNEL: three blocks of cloth tunnel, the guh crawls through as a bump;</li>
 *   <li>SLALOMPAALTJES: three poles, left-right-left;</li>
 *   <li>EVENWICHTSBALK: hop on, carefully across, hop off; one time in four it falls off halfway (oeps) and climbs back;</li>
 *   <li>KNABBELTAFELTJE: the mandatory pit stop: it eats first, slowly (see RouteStukken.Tafel).</li>
 * </ul>
 */
public enum Obstakel {
    HORDE("horde", List.of(), true,
            List.of(box(1, 0, 7, 3, 9, 9), box(13, 0, 7, 15, 9, 9), box(3, 6, 7.5, 13, 8, 8.5)),
            List.of(box(1, 0, 6.5, 15, 10, 9.5))),
    SPRINGPLANK("springplank", List.of(), false,
            List.of(box(2, 0, 0, 14, 2, 16), box(2, 2, 5, 14, 5, 16), box(2, 5, 10, 14, 8, 16)),
            List.of(box(2, 0, 0, 14, 8, 16))),
    KRUIPTUNNEL("kruiptunnel", List.of(new int[]{0, 0, -1}, new int[]{0, 0, 1}), true,
            List.of(box(1, 0, -16, 2.5, 9, 32), box(13.5, 0, -16, 15, 9, 32), box(1, 9, -16, 15, 10.5, 32)),
            List.of(box(1, 0, -16, 15, 10.5, 32))),
    SLALOMPAALTJES("slalompaaltjes", List.of(new int[]{0, 0, -1}, new int[]{0, 0, 1}), true,
            List.of(box(6.5, 0, -9.5, 9.5, 15, -6.5), box(6.5, 0, 6.5, 9.5, 15, 9.5), box(6.5, 0, 22.5, 9.5, 15, 25.5)),
            List.of(box(5.5, 0, -10.5, 10.5, 16, -5.5), box(5.5, 0, 5.5, 10.5, 16, 10.5), box(5.5, 0, 21.5, 10.5, 16, 26.5))),
    EVENWICHTSBALK("evenwichtsbalk", List.of(new int[]{0, 0, -1}, new int[]{0, 0, 1}), true,
            List.of(box(6, 6, -16, 10, 8, 32), box(5, 0, -14, 11, 6, -11), box(5, 0, 27, 11, 6, 30)),
            List.of(box(5, 0, -16, 11, 8, 32))),
    KNABBELTAFELTJE("knabbeltafeltje", List.of(), false,
            List.of(box(3, 0, 3, 13, 7, 13)),
            List.of(box(3, 0, 3, 13, 10, 13)));

    /** How fast a guh trots through an obstacle (blocks a tick), and how slowly it shuffles over the beam. */
    private static final double DRAF = 0.2, SCHUIFEL = 0.07, KRUIP = 0.1;

    private final String id;
    private final List<int[]> delen;
    private final boolean tweeKanten;
    private final List<double[]> botsing, omlijning;

    Obstakel(String id, List<int[]> delen, boolean tweeKanten, List<double[]> botsing, List<double[]> omlijning) {
        this.id = id;
        this.delen = delen;
        this.tweeKanten = tweeKanten;
        this.botsing = botsing;
        this.omlijning = omlijning;
    }

    private static double[] box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new double[]{x0, y0, z0, x1, y1, z1};
    }

    /** The registry id is {@code guhparkour_<id>}. */
    public String id() {
        return id;
    }

    public List<int[]> delen() {
        return delen;
    }

    /** Can a guh take it from both ends? (Not the springplank, not the tafeltje.) */
    public boolean tweeKanten() {
        return tweeKanten;
    }

    public List<double[]> botsing() {
        return botsing;
    }

    public List<double[]> omlijning() {
        return omlijning;
    }

    /** One time in this many the guh falls off (0: never). */
    public int valKans() {
        return this == EVENWICHTSBALK ? 4 : 0;
    }

    /** The way over it from the front (local z below 0) to the back; valt: it falls off halfway (the evenwichtsbalk). */
    public Baan baan(boolean valt) {
        return switch (this) {
            case HORDE -> Baan.van(0.5, 0, -0.8)
                    .loop(0.5, 0, -0.1, DRAF)
                    .sprong(0.5, 0, 1.1, 10, 0.85, Geluid.SPRONG)
                    .loop(0.5, 0, 1.8, DRAF, Geluid.LANDING);
            case SPRINGPLANK -> Baan.van(0.5, 0, -0.8)
                    .loop(0.5, 0.1, 0.0, DRAF)
                    .loop(0.5, 0.5, 0.75, DRAF)
                    .sprong(0.5, 0, 3.4, 20, 1.5, Geluid.BOING)
                    .wacht(4, Geluid.LANDING);
            case KRUIPTUNNEL -> Baan.van(0.5, 0, -1.6)
                    .loop(0.5, 0, -1.0, DRAF)
                    .loop(0.5, 0, 2.0, KRUIP, Geluid.TUNNEL)
                    .loop(0.5, 0, 2.6, DRAF, Geluid.TRIP);
            case SLALOMPAALTJES -> Baan.van(0.5, 0, -1.7)
                    .loop(1.0, 0, -0.5, 0.17, Geluid.TRIP)
                    .loop(0.0, 0, 0.5, 0.17, Geluid.TRIP)
                    .loop(1.0, 0, 1.5, 0.17, Geluid.TRIP)
                    .loop(0.5, 0, 2.7, 0.17, Geluid.TRIP);
            case EVENWICHTSBALK -> {
                Baan b = Baan.van(0.5, 0, -1.6).sprong(0.5, 0.5, -0.85, 6, 0.3, Geluid.SPRONG);
                if (valt) {
                    b.loop(0.5, 0.5, 0.5, SCHUIFEL, Geluid.WIEBEL)
                            .wacht(8, Geluid.WIEBEL)
                            .sprong(1.4, 0, 0.5, 7, 0.2, Geluid.PLOF)
                            .wacht(22, Geluid.OEPS)
                            .sprong(0.5, 0.5, 0.5, 8, 0.45, Geluid.SPRONG);
                }
                yield b.loop(0.5, 0.5, 1.85, SCHUIFEL, Geluid.WIEBEL).sprong(0.5, 0, 2.6, 6, 0.25, Geluid.LANDING);
            }
            case KNABBELTAFELTJE -> Baan.van(0.5, 0, -0.8).wacht(RouteStukken.EET_TICKS, Geluid.GEEN);
        };
    }
}
