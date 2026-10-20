package nl.juiced.guhs.feature.safari.momenten;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.juiced.guhs.feature.safari.ParkData;

/**
 * The day plan of the park's moments (S3, design ronde 26): pure arithmetic on the time of day (0..23999) and the day
 * number, the same for every player, every client and the schemabord. Tested in {@code RitGameTests}.
 * <ul>
 *   <li><b>Voedertijden</b>: one per zone, every in-game day, at {@link #voederStart}; {@link #DUUR} ticks; the ranger feeds
 *       {@link #VOEDER_SOORT} of that zone, and the shy animals of the zone come out.</li>
 *   <li><b>Rangerpraatje</b>: twice a day ({@link #PRAATJES}) at the truck stop of a zone that changes every day.</li>
 *   <li><b>Nachtsafari</b>: every third night ({@link #nachtsafariNacht}), lantern trucks from {@link #NACHT_VAN} to
 *       {@link #NACHT_TOT}.</li>
 *   <li><b>Noorderlicht</b>: above the IJskust on most nights ({@link #noorderlichtNacht}).</li>
 *   <li><b>Dierenverjaardag</b>: one dex species per REAL day ({@link #jarig}).</li>
 * </ul>
 */
public final class MomentSchema {
    public static final int DAG = 24000, DUUR = 900, MELDING = 1200;
    public static final int[] PRAATJES = {6000, 9600};
    public static final int NACHT_VAN = 13500, NACHT_TOT = 22500;
    public static final Map<String, String> VOEDER_SOORT = Map.of("entree", "geitje", "savanne", "olifant", "delta", "nijlpaard",
            "regenwoud", "orang_oetan", "bamboe", "reuzenpanda", "ijskust", "pinguin", "oertijd", "mammoet", "europa", "otter",
            "australie", "kangoeroe");

    public enum Soort {
        VOEDERTIJD, PRAATJE, NACHTSAFARI
    }

    /** One moment of a day: what, in which zone, from..to (time of day). */
    public record Moment(Soort soort, String zone, int van, int tot) {
        public boolean bezig(int tijd) {
            return tijd >= van && tijd < tot;
        }
    }

    public static int voederStart(String zone) {
        int i = Math.max(0, ParkData.ZONE_NAMEN.indexOf(zone));
        return 1400 + i * 1250;      // 07:24 .. 17:24 (the notice a minute before is still today)
    }

    /** The zone of praatje slot s (0, 1) on day d (never the village). */
    public static String praatjeZone(long dag, int s) {
        int n = ParkData.ZONE_NAMEN.size() - 1;
        return ParkData.ZONE_NAMEN.get(1 + (int) Math.floorMod(dag * 2 + s, n));
    }

    public static boolean nachtsafariNacht(long dag) {
        return Math.floorMod(dag, 3) == 0;
    }

    public static boolean nachtsafari(long dag, int tijd) {
        return nachtsafariNacht(dag) && tijd >= NACHT_VAN && tijd < NACHT_TOT;
    }

    public static boolean noorderlichtNacht(long dag) {
        return Math.floorMod(dag, 4) != 3;
    }

    public static boolean nacht(int tijd) {
        return tijd >= 13000 && tijd < 23000;
    }

    /** All moments of day d, in time order. */
    public static List<Moment> dag(long dag) {
        List<Moment> uit = new ArrayList<>();
        for (String zone : ParkData.ZONE_NAMEN) {
            int v = voederStart(zone);
            uit.add(new Moment(Soort.VOEDERTIJD, zone, v, v + DUUR));
        }
        for (int s = 0; s < PRAATJES.length; s++) {
            uit.add(new Moment(Soort.PRAATJE, praatjeZone(dag, s), PRAATJES[s], PRAATJES[s] + DUUR));
        }
        if (nachtsafariNacht(dag)) {
            uit.add(new Moment(Soort.NACHTSAFARI, "", NACHT_VAN, NACHT_TOT));
        }
        uit.sort((a, b) -> Integer.compare(a.van(), b.van()));
        return uit;
    }

    /** The birthday species of real day r (index into the sorted dex species). */
    public static String jarig(long realDag, List<String> soorten) {
        if (soorten.isEmpty()) {
            return "";
        }
        List<String> s = new ArrayList<>(soorten);
        s.sort(String::compareTo);
        return s.get((int) Math.floorMod(realDag * 7 + 3, s.size()));
    }

    /** The clock text of a time of day ("07:00"; 0 = 06:00 like the game). */
    public static String klok(int tijd) {
        int min = (int) ((tijd / (double) DAG * 24 * 60 + 6 * 60) % (24 * 60));
        return String.format(java.util.Locale.ROOT, "%02d:%02d", min / 60, min % 60);
    }

    private MomentSchema() {
    }
}
