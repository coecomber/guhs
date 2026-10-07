package nl.juiced.guhs.feature.guhpixel.among.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * The ship "De Vadsvaarder" as the game knows it: zones (rooms and corridors), the walking graph of the guh NPCs, doors,
 * task panels, vents, the emergency button, meeting spots, beds and the task list. Read from
 * {@code data/guhs/guhpixel/among_schip.txt}, which tools/features/guhpixel_among_bouw.py writes together with the arena
 * template: the Python module is the only source of these numbers.
 * <p>
 * This package ({@code among.model}) is plain Java without any Minecraft class, so whole rounds can be simulated headless
 * ({@link Simulatie}). Coordinates are template cells; a position (x, z) is in blocks, cell centres at +0.5.
 */
public final class Schip {
    public static final String BESTAND = "/data/guhs/guhpixel/among_schip.txt";

    public record Zone(int idx, String id, boolean kamer, int x0, int z0, int x1, int z1) {
        public boolean bevat(double x, double z) {
            return x >= x0 && x < x1 + 1 && z >= z0 && z < z1 + 1;
        }

        public double middenX() {
            return (x0 + x1 + 1) / 2.0;
        }

        public double middenZ() {
            return (z0 + z1 + 1) / 2.0;
        }
    }

    public record Knoop(int idx, String id, double x, double z, int zone) {
    }

    public record Deur(int idx, String id, int kamer, int knoop, int x0, int z0, int x1, int z1) {
    }

    public enum PaneelSoort { TAAK, LICHT, ALARM }

    public record Paneel(int idx, String id, int kamer, PaneelSoort soort, int bx, int by, int bz, String facing, int knoop) {
    }

    public record Luik(int idx, String id, int kamer, String netwerk, int x, int y, int z, int knoop) {
    }

    public record Plek(double x, double z, float yaw) {
    }

    public record Taak(int idx, String id, String soort, int duur, int[] panelen) {
    }

    public int breedte, hoogte, diepte, voet;
    public double startX, startY, startZ;
    public float startYaw;
    public final List<Zone> zones = new ArrayList<>();
    public final List<Knoop> knopen = new ArrayList<>();
    public final List<Deur> deuren = new ArrayList<>();
    public final List<Paneel> panelen = new ArrayList<>();
    public final List<Luik> luiken = new ArrayList<>();
    public final List<Plek> stoelen = new ArrayList<>();
    public final List<Plek> bedden = new ArrayList<>();
    public final List<Taak> taken = new ArrayList<>();
    public int knopX, knopY, knopZ, knopKnoop;
    public Paneel lichtPaneel;
    public final List<Paneel> alarmPanelen = new ArrayList<>();

    private final Map<String, Integer> zoneIdx = new LinkedHashMap<>();
    private final Map<String, Integer> knoopIdx = new LinkedHashMap<>();
    private final Map<String, Integer> paneelIdx = new LinkedHashMap<>();
    private List<int[]> buren = new ArrayList<>();
    private double[][] randLengte;
    /** For each node: the door it is (index), or -1. */
    private int[] knoopDeur;
    /** Zone adjacency (through a door). */
    private boolean[][] naast;

    private static Schip gedeeld;

    /** The ship of this jar (read once). */
    public static synchronized Schip standaard() {
        if (gedeeld == null) {
            try (InputStream in = Schip.class.getResourceAsStream(BESTAND)) {
                if (in == null) {
                    throw new IllegalStateException("missing " + BESTAND);
                }
                gedeeld = lees(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("cannot read " + BESTAND, e);
            }
        }
        return gedeeld;
    }

    public static Schip lees(Reader bron) throws IOException {
        Schip s = new Schip();
        List<String[]> randen = new ArrayList<>();
        BufferedReader r = new BufferedReader(bron);
        for (String regel = r.readLine(); regel != null; regel = r.readLine()) {
            String[] d = regel.trim().split("\\s+");
            if (d.length < 2) {
                continue;
            }
            switch (d[0]) {
                case "maat" -> {
                    s.breedte = Integer.parseInt(d[1]);
                    s.hoogte = Integer.parseInt(d[2]);
                    s.diepte = Integer.parseInt(d[3]);
                }
                case "voet" -> s.voet = Integer.parseInt(d[1]);
                case "start" -> {
                    s.startX = Double.parseDouble(d[1]);
                    s.startY = Double.parseDouble(d[2]);
                    s.startZ = Double.parseDouble(d[3]);
                    s.startYaw = Float.parseFloat(d[4]);
                }
                case "zone" -> {
                    s.zoneIdx.put(d[1], s.zones.size());
                    s.zones.add(new Zone(s.zones.size(), d[1], d[2].equals("kamer"), i(d[3]), i(d[4]), i(d[5]), i(d[6])));
                }
                case "knoop" -> {
                    s.knoopIdx.put(d[1], s.knopen.size());
                    s.knopen.add(new Knoop(s.knopen.size(), d[1], i(d[2]) + 0.5, i(d[3]) + 0.5, s.zone(d[4])));
                }
                case "rand" -> randen.add(d);
                case "deur" -> s.deuren.add(new Deur(s.deuren.size(), d[1], s.zone(d[2]), s.knoop(d[3]), i(d[4]), i(d[5]), i(d[6]), i(d[7])));
                case "paneel" -> {
                    Paneel p = new Paneel(s.panelen.size(), d[1], s.zone(d[2]), PaneelSoort.valueOf(d[3].toUpperCase()), i(d[4]), i(d[5]), i(d[6]), d[7], s.knoop(d[8]));
                    s.paneelIdx.put(p.id(), p.idx());
                    s.panelen.add(p);
                    if (p.soort() == PaneelSoort.LICHT) {
                        s.lichtPaneel = p;
                    } else if (p.soort() == PaneelSoort.ALARM) {
                        s.alarmPanelen.add(p);
                    }
                }
                case "luik" -> s.luiken.add(new Luik(s.luiken.size(), d[1], s.zone(d[2]), d[3], i(d[4]), i(d[5]), i(d[6]), s.knoop(d[7])));
                case "knop" -> {
                    s.knopX = i(d[1]);
                    s.knopY = i(d[2]);
                    s.knopZ = i(d[3]);
                    s.knopKnoop = s.knoop(d[4]);
                }
                case "stoel" -> s.stoelen.add(new Plek(Double.parseDouble(d[2]), Double.parseDouble(d[3]), 0f));
                case "bed" -> s.bedden.add(new Plek(Double.parseDouble(d[2]), Double.parseDouble(d[3]), Float.parseFloat(d[4])));
                case "taak" -> {
                    int[] panelen = new int[d.length - 4];
                    for (int k = 0; k < panelen.length; k++) {
                        panelen[k] = s.paneelIdx.get(d[4 + k]);
                    }
                    s.taken.add(new Taak(s.taken.size(), d[1], d[2], i(d[3]), panelen));
                }
                default -> {
                }
            }
        }
        int n = s.knopen.size();
        List<List<Integer>> lijst = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            lijst.add(new ArrayList<>());
        }
        s.randLengte = new double[n][n];
        for (String[] d : randen) {
            int a = s.knoop(d[1]), b = s.knoop(d[2]);
            lijst.get(a).add(b);
            lijst.get(b).add(a);
            double len = Math.hypot(s.knopen.get(a).x() - s.knopen.get(b).x(), s.knopen.get(a).z() - s.knopen.get(b).z());
            s.randLengte[a][b] = s.randLengte[b][a] = len;
        }
        s.buren = new ArrayList<>();
        for (List<Integer> l : lijst) {
            s.buren.add(l.stream().mapToInt(Integer::intValue).toArray());
        }
        s.knoopDeur = new int[n];
        Arrays.fill(s.knoopDeur, -1);
        s.naast = new boolean[s.zones.size()][s.zones.size()];
        for (Deur d : s.deuren) {
            s.knoopDeur[d.knoop()] = d.idx();
            int gang = s.knopen.get(d.knoop()).zone();
            s.naast[gang][d.kamer()] = s.naast[d.kamer()][gang] = true;
        }
        if (s.lichtPaneel == null || s.alarmPanelen.size() != 2 || s.stoelen.size() < 10 || s.bedden.size() < 10 || s.taken.isEmpty()) {
            throw new IllegalStateException("the ship table is incomplete");
        }
        return s;
    }

    private static int i(String s) {
        return Integer.parseInt(s);
    }

    private int zone(String id) {
        Integer z = zoneIdx.get(id);
        if (z == null) {
            throw new IllegalStateException("unknown zone " + id);
        }
        return z;
    }

    private int knoop(String id) {
        Integer k = knoopIdx.get(id);
        if (k == null) {
            throw new IllegalStateException("unknown node " + id);
        }
        return k;
    }

    public Zone zone(int idx) {
        return zones.get(idx);
    }

    public int zoneVan(String id) {
        return zone(id);
    }

    public int paneelVan(String id) {
        Integer p = paneelIdx.get(id);
        return p == null ? -1 : p;
    }

    /** The zone of a position: rooms first, then corridors; -1 outside the ship. */
    public int zoneOp(double x, double z) {
        for (Zone zo : zones) {
            if (zo.bevat(x, z)) {
                return zo.idx();
            }
        }
        return -1;
    }

    public boolean naast(int zoneA, int zoneB) {
        return zoneA >= 0 && zoneB >= 0 && naast[zoneA][zoneB];
    }

    /** The node closest to a position, inside the same zone when there is one. */
    public int dichtsteKnoop(double x, double z) {
        int zone = zoneOp(x, z);
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (Knoop k : knopen) {
            double d = (k.x() - x) * (k.x() - x) + (k.z() - z) * (k.z() - z) + (k.zone() == zone ? 0 : 400);
            if (d < bestD) {
                bestD = d;
                best = k.idx();
            }
        }
        return best;
    }

    /**
     * The shortest walk from node a to node b as a list of nodes (a excluded, b included); empty when a == b; null when
     * there is none (closed doors: {@code dicht} holds the indices of the closed doors).
     */
    public List<Integer> pad(int a, int b, Set<Integer> dicht) {
        if (a == b) {
            return Collections.emptyList();
        }
        int n = knopen.size();
        double[] afstand = new double[n];
        int[] van = new int[n];
        Arrays.fill(afstand, Double.MAX_VALUE);
        Arrays.fill(van, -1);
        afstand[a] = 0;
        PriorityQueue<double[]> rij = new PriorityQueue<>((p, q) -> Double.compare(p[0], q[0]));
        rij.add(new double[]{0, a});
        while (!rij.isEmpty()) {
            double[] top = rij.poll();
            int k = (int) top[1];
            if (top[0] > afstand[k]) {
                continue;
            }
            if (k == b) {
                break;
            }
            for (int nb : buren.get(k)) {
                if (knoopDeur[nb] >= 0 && dicht.contains(knoopDeur[nb])) {
                    continue;
                }
                double d = afstand[k] + randLengte[k][nb];
                if (d < afstand[nb]) {
                    afstand[nb] = d;
                    van[nb] = k;
                    rij.add(new double[]{d, nb});
                }
            }
        }
        if (van[b] < 0) {
            return null;
        }
        List<Integer> pad = new ArrayList<>();
        for (int k = b; k != a; k = van[k]) {
            pad.add(k);
        }
        Collections.reverse(pad);
        return pad;
    }

    /** The walking distance between two nodes (all doors open); a large number when there is no way. */
    public double loopAfstand(int a, int b) {
        List<Integer> p = pad(a, b, Set.of());
        if (p == null) {
            return 1e6;
        }
        double d = 0;
        int vorige = a;
        for (int k : p) {
            d += randLengte[vorige][k];
            vorige = k;
        }
        return d;
    }

    /** The doors of a room. */
    public List<Deur> deurenVan(int kamer) {
        List<Deur> uit = new ArrayList<>();
        for (Deur d : deuren) {
            if (d.kamer() == kamer) {
                uit.add(d);
            }
        }
        return uit;
    }

    /** The other vents of this vent's network. */
    public List<Luik> netwerk(Luik l) {
        List<Luik> uit = new ArrayList<>();
        for (Luik ander : luiken) {
            if (ander != l && ander.netwerk().equals(l.netwerk())) {
                uit.add(ander);
            }
        }
        return uit;
    }

    public Luik luikIn(int kamer) {
        for (Luik l : luiken) {
            if (l.kamer() == kamer) {
                return l;
            }
        }
        return null;
    }

    /** The hub node of a room (its first). */
    public int hub(int kamer) {
        Integer k = knoopIdx.get("hub_" + zones.get(kamer).id() + "_0");
        return k == null ? 0 : k;
    }

    public List<Zone> kamers() {
        List<Zone> uit = new ArrayList<>();
        for (Zone z : zones) {
            if (z.kamer()) {
                uit.add(z);
            }
        }
        return uit;
    }
}
