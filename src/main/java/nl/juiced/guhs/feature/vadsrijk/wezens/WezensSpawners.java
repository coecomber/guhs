package nl.juiced.guhs.feature.vadsrijk.wezens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * The zone-1 spawners (CONTRACT_VADSRIJK 5, 13), all in kern's {@link Spawners}. Fix round 1 ("leven"): the zone is planned
 * like an MMO field from the real map ({@link WezensPlekken}: the generator's paths, cave tunnels and knots, ponds,
 * landmarks):
 * <ul>
 * <li>small groups along and next to every path (some right ON the path), patrolling pairs walking up and down a path;</li>
 * <li>camps of 3-6 (two kinds together) between the paths;</li>
 * <li>the caves: groups along every tunnel (Druipslijm hang from the tunnel ceilings), camps in the four knots;</li>
 * <li>an elite at every landmark (light beams, the waterfall, viewpoints, cave knots, the mountain path);</li>
 * <li>respawn 10-20 s near paths, 25-30 s in camps, 3 min for landmark elites (Moederklodder 2 min).</li>
 * </ul>
 * Area caps ({@link Spawners#GEBIED_MAX}) and the safe spots (campfires + any guh/NPC within 12) are set here too.
 * Spawner ids are "wezens:&lt;group&gt;#&lt;kind&gt;": mobs of one group help each other (social aggro, {@link #groep}).
 */
public final class WezensSpawners {
    public static final double ELITE = 0.06;
    /** Max alive per area (all spawners of the area summed; only spawners near a player are awake). */
    public static final Map<String, Integer> GEBIED_MAX = Map.of("klodderweide", 80, "sporenwoud", 90, "druipgrotten", 80, "zwamtempel", 30, "ring", 30);

    private static int oppervlak, vloer;
    /** Spawner id -> its patrol route (two ends). */
    private static final Map<String, BlockPos[]> ROUTES = new HashMap<>();
    /** Per area: the sum of the spawner maxima (= alive when everything is awake), for the notes and the dev command. */
    public static final Map<String, Integer> PLAN = new LinkedHashMap<>();

    private WezensSpawners() {
    }

    /** A kind with its level range and element. */
    private record Soort(String naam, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int min, int max, Element e) {
    }

    private static final Soort KLODDER = new Soort("klodder", WezensRegistry.KLODDER, 1, 4, Element.WATER);
    private static final Soort BUBBEL = new Soort("bubbelklodder", WezensRegistry.BUBBELKLODDER, 2, 5, Element.WATER);
    private static final Soort STUITER = new Soort("stuiterklodder", WezensRegistry.STUITERKLODDER, 3, 6, Element.WATER);
    private static final Soort MOEDER = new Soort("moederklodder", WezensRegistry.MOEDERKLODDER, 5, 6, Element.WATER);
    private static final Soort HOEDJES = new Soort("hoedjesbol", WezensRegistry.HOEDJESBOL, 6, 10, Element.NATUUR);
    private static final Soort PUFFER = new Soort("sporenpuffer", WezensRegistry.SPORENPUFFER, 7, 11, Element.NATUUR);
    private static final Soort WACHTER = new Soort("zwamwachter", WezensRegistry.ZWAMWACHTER, 9, 13, Element.NATUUR);
    private static final Soort MOSGEEST = new Soort("mosgeest", WezensRegistry.MOSGEEST, 8, 13, Element.NATUUR);
    private static final Soort DRUIP = new Soort("druipslijm", WezensRegistry.DRUIPSLIJM, 12, 16, Element.WATER);
    private static final Soort KRISTAL = new Soort("kristalklodder", WezensRegistry.KRISTALKLODDER, 13, 17, Element.WATER);
    private static final Soort KNAAG = new Soort("knaag", WezensRegistry.KNAAG, 12, 17, Element.GEEN);
    private static final Soort GLIM = new Soort("glimzwam", WezensRegistry.GLIMZWAM, 13, 18, Element.NATUUR);
    private static final Soort RIDDER = new Soort("tempelridder", WezensRegistry.TEMPELRIDDER, 18, 20, Element.NATUUR);
    private static final Soort PRIESTER = new Soort("sporenpriester", WezensRegistry.SPORENPRIESTER, 18, 20, Element.NATUUR);

    // ---- registering ---------------------------------------------------------------------------------------------------

    private static void zet(String gebied, String groep, Soort s, BlockPos c, int straal, int max, int respawn, double elite, boolean grot,
            int minY, int maxY) {
        Spawners.Spawner sp = new Spawners.Spawner("wezens:" + groep + "#" + s.naam(), c, straal, s.type(), max, respawn, s.min(), s.max(), s.e(), elite);
        if (grot) {
            sp = sp.grot(minY, maxY);
            vloer++;
        } else {
            oppervlak++;
        }
        Spawners.registreer(sp);
        PLAN.merge(gebied, max, Integer::sum);
    }

    private static void opp(String gebied, String groep, Soort s, BlockPos c, int straal, int max, int respawn, double elite) {
        zet(gebied, groep, s, c, straal, max, respawn, elite, false, 0, 0);
    }

    /** The cave/room form of a spawner: floor search from y+1 down to y-24 (a roof two blocks up is never found). */
    public static Spawners.Spawner grot(Spawners.Spawner s) {
        return s.grot(s.centrum().getY() - 24, s.centrum().getY() + 1);
    }

    /** The social group of a spawner id ("" = none): the part before '#'. */
    public static String groep(String spawnerId) {
        int i = spawnerId.indexOf('#');
        return i < 0 ? spawnerId : spawnerId.substring(0, i);
    }

    /** The patrol route of this spawner's mobs (two ends), or null. */
    @Nullable
    public static BlockPos[] route(String spawnerId) {
        return ROUTES.get(spawnerId);
    }

    /** From WezensRegistry.register. */
    public static void registreer() {
        oppervlak = vloer = 0;
        PLAN.clear();
        ROUTES.clear();
        Spawners.GEBIED_MAX.putAll(GEBIED_MAX);
        Spawners.VEILIGE_PLEKKEN.addAll(WereldPlekken.KAMPVUREN);
        Spawners.VERBODEN.add(WezensSpawners::npcInBuurt);

        Random r = new Random(1100);
        klodderweide(r);
        sporenwoud(r);
        druipgrotten(r);
        zwamtempel(r);
    }

    /** A guh (NPC, ridder, Kampklodder...) within the safe radius. */
    private static boolean npcInBuurt(ServerLevel sl, BlockPos p) {
        AABB box = new AABB(p).inflate(Spawners.VEILIG_STRAAL, 8, Spawners.VEILIG_STRAAL);
        return !sl.getEntitiesOfClass(GuhEntity.class, box, e -> true).isEmpty() || !sl.getEntitiesOfClass(Kampklodder.class, box, e -> true).isEmpty();
    }

    // ---- planning helpers ----------------------------------------------------------------------------------------------

    /** Is this centre usable: inside the area (6 in from the edge), clear of campfires and the extra no-go circles. */
    private static boolean vrij(String gebied, double x, double z, int straal, int[][] nietHier) {
        Gebieden.Gebied g = Gebieden.op(x, z);
        if (g == null || !g.id().equals(gebied) || x < g.minX() + 6 || x > g.maxX() - 6 || z < g.minZ() + 6 || z > g.maxZ() - 6) {
            return false;
        }
        for (BlockPos v : WereldPlekken.KAMPVUREN) {
            if (Math.hypot(v.getX() - x, v.getZ() - z) < Spawners.VEILIG_STRAAL + straal + 2) {
                return false;
            }
        }
        for (int[] n : nietHier) {
            if (Math.hypot(n[0] - x, n[1] - z) < n[2] + straal) {
                return false;
            }
        }
        return true;
    }

    private static Soort kies(Random r, Object... gewogen) {
        int som = 0;
        for (int i = 1; i < gewogen.length; i += 2) {
            som += (Integer) gewogen[i];
        }
        int k = r.nextInt(som);
        for (int i = 0; i < gewogen.length; i += 2) {
            k -= (Integer) gewogen[i + 1];
            if (k < 0) {
                return (Soort) gewogen[i];
            }
        }
        return (Soort) gewogen[0];
    }

    /** One point along a polyline every {@code stap} blocks: {x, z, dirX, dirZ}. */
    private static List<double[]> langs(int[][] pad, double stap, double begin) {
        List<double[]> uit = new ArrayList<>();
        double t = begin;
        double acc = 0;
        for (int i = 0; i + 1 < pad.length; i++) {
            double ax = pad[i][0], az = pad[i][1], bx = pad[i + 1][0], bz = pad[i + 1][1];
            double L = Math.hypot(bx - ax, bz - az);
            while (t <= acc + L) {
                double f = (t - acc) / L;
                uit.add(new double[] {ax + (bx - ax) * f, az + (bz - az) * f, (bx - ax) / L, (bz - az) / L});
                t += stap;
            }
            acc += L;
        }
        return uit;
    }

    private static double padAfstand(double x, double z, List<int[][]> paden) {
        double best = Double.MAX_VALUE;
        for (int[][] pad : paden) {
            for (int i = 0; i + 1 < pad.length; i++) {
                double ax = pad[i][0], az = pad[i][1], dx = pad[i + 1][0] - ax, dz = pad[i + 1][1] - az;
                double t = Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz)));
                best = Math.min(best, Math.hypot(x - (ax + t * dx), z - (az + t * dz)));
            }
        }
        return best;
    }

    /**
     * Groups along the paths of an area: every {@code stap} blocks, in turn a group ON the path (1-2), a group next to it
     * (2-3, 7-13 blocks to the side) and every fourth point a patrolling pair walking 12 blocks up and down the path.
     */
    private static void langsPaden(String gebied, Random r, List<int[][]> paden, double stap, int[][] nietHier, Object... gewogen) {
        int n = 0;
        for (int[][] pad : paden) {
            int i = 0;
            for (double[] p : langs(pad, stap, stap * 0.5)) {
                i++;
                double nx = -p[3], nz = p[2];
                int soortVak = i % 4;
                Soort s = kies(r, gewogen);
                if (soortVak == 0) {
                    BlockPos a = new BlockPos((int) Math.round(p[0] - p[2] * 12), Spawners.OPPERVLAK_MIN_Y + 6, (int) Math.round(p[1] - p[3] * 12));
                    BlockPos b = new BlockPos((int) Math.round(p[0] + p[2] * 12), Spawners.OPPERVLAK_MIN_Y + 6, (int) Math.round(p[1] + p[3] * 12));
                    if (vrij(gebied, p[0], p[1], 14, nietHier) && vrij(gebied, a.getX(), a.getZ(), 3, nietHier) && vrij(gebied, b.getX(), b.getZ(), 3, nietHier)) {
                        String groep = gebied + "_patrouille_" + n++;
                        opp(gebied, groep, s, new BlockPos((int) p[0], Spawners.OPPERVLAK_MIN_Y + 6, (int) p[1]), 3, 2, 300, ELITE);
                        ROUTES.put("wezens:" + groep + "#" + s.naam(), new BlockPos[] {a, b});
                    }
                    continue;
                }
                boolean op = soortVak == 2;
                double kant = op ? r.nextInt(5) - 2 : (r.nextBoolean() ? 1 : -1) * (7 + r.nextInt(7));
                double x = p[0] + nx * kant, z = p[1] + nz * kant;
                int straal = op ? 4 : 6;
                if (!vrij(gebied, x, z, straal, nietHier)) {
                    continue;
                }
                int max = op ? 1 + r.nextInt(2) : 2 + r.nextInt(2);
                opp(gebied, gebied + (op ? "_pad_" : "_berm_") + n++, s, new BlockPos((int) x, Spawners.OPPERVLAK_MIN_Y + 6, (int) z), straal, max,
                        200 + r.nextInt(201), ELITE);
            }
        }
    }

    /** Camps of two kinds together (3-6), on free spots at least 22 from a path and 45 from each other. */
    private static void kampen(String gebied, Random r, int aantal, List<int[][]> paden, int[][] nietHier, Soort[][] samenstellingen,
            int[][] aantallen) {
        Gebieden.Gebied g = Gebieden.alle().stream().filter(x -> x.id().equals(gebied)).findFirst().orElseThrow();
        List<int[]> gezet = new ArrayList<>();
        for (int poging = 0; poging < 4000 && gezet.size() < aantal; poging++) {
            int x = g.minX() + 20 + r.nextInt(g.maxX() - g.minX() - 40), z = g.minZ() + 20 + r.nextInt(g.maxZ() - g.minZ() - 40);
            if (!vrij(gebied, x, z, 10, nietHier) || padAfstand(x, z, paden) < 22) {
                continue;
            }
            boolean dichtbij = false;
            for (int[] k : gezet) {
                dichtbij |= Math.hypot(k[0] - x, k[1] - z) < 45;
            }
            if (dichtbij) {
                continue;
            }
            gezet.add(new int[] {x, z});
            int k = gezet.size() - 1, s = k % samenstellingen.length;
            String groep = gebied + "_kamp_" + k;
            BlockPos c = new BlockPos(x, Spawners.OPPERVLAK_MIN_Y + 6, z);
            for (int j = 0; j < samenstellingen[s].length; j++) {
                opp(gebied, groep, samenstellingen[s][j], c, 7, aantallen[s][j], 500 + r.nextInt(101), ELITE);
            }
        }
    }

    private static void elite(String gebied, String naam, Soort s, BlockPos c, int straal) {
        opp(gebied, gebied + "_elite_" + naam, s, c, straal, 1, 3600, 1.0);
    }

    private static BlockPos p(double x, double z) {
        return new BlockPos((int) Math.round(x), Spawners.OPPERVLAK_MIN_Y + 6, (int) Math.round(z));
    }

    // ---- Klodderweide, lv 1-6 --------------------------------------------------------------------------------------------

    private static void klodderweide(Random r) {
        String g = "klodderweide";
        List<int[][]> paden = WezensPlekken.paden("weide", "weide2", "weide3", "slijm", "slijm2");
        int[][] niet = {WezensPlekken.ARENA_KONING, WezensPlekken.SLAAPHEUVEL};
        langsPaden(g, r, paden, 34, niet, KLODDER, 6, BUBBEL, 2, STUITER, 2);
        kampen(g, r, 5, paden, niet, new Soort[][] {{KLODDER, BUBBEL}, {KLODDER, STUITER}, {STUITER, BUBBEL}},
                new int[][] {{3, 1}, {2, 2}, {2, 2}});
        // the pond rims: bubbelklodders
        int v = 0;
        for (int[] vijver : WezensPlekken.VIJVERS) {
            for (int a = 0; a < 1; a++) {
                double hoek = a * Math.PI + vijver[0] * 0.01;
                double x = vijver[0] + Math.cos(hoek) * (vijver[2] + 4), z = vijver[1] + Math.sin(hoek) * (vijver[2] + 4);
                if (vrij(g, x, z, 5, niet)) {
                    opp(g, g + "_vijver_" + v++, BUBBEL, p(x, z), 5, 2, 400, ELITE);
                }
            }
        }
        for (BlockPos m : WezensPlekken.MOEDERKLODDER) {
            opp(g, g + "_moeder_" + m.getX(), MOEDER, m, 10, 1, 2400, 1.0);
        }
        elite(g, "klodderkijk", STUITER, p(506, -226), 6);
        elite(g, "slijmnaald", KLODDER, p(446, 366), 6);
        elite(g, "hoedhupperwei", STUITER, p(520, 285), 6);
        elite(g, "vijver_noord", BUBBEL, p(365, -70), 5);
    }

    // ---- Sporenwoud, lv 6-13 ---------------------------------------------------------------------------------------------

    private static void sporenwoud(Random r) {
        String g = "sporenwoud";
        List<int[][]> paden = WezensPlekken.paden("woud", "woud2");
        int[][] niet = {};
        langsPaden(g, r, paden, 26, niet, HOEDJES, 4, PUFFER, 3, WACHTER, 2, MOSGEEST, 2);
        kampen(g, r, 8, paden, niet, new Soort[][] {{HOEDJES, PUFFER}, {WACHTER, PUFFER}, {MOSGEEST, HOEDJES}, {HOEDJES, WACHTER}},
                new int[][] {{3, 2}, {2, 2}, {2, 2}, {3, 1}});
        Soort[] rond = {WACHTER, MOSGEEST, HOEDJES, PUFFER, WACHTER, MOSGEEST};
        for (int i = 0; i < WereldPlekken.LICHTSTRALEN.size(); i++) {
            BlockPos l = WereldPlekken.LICHTSTRALEN.get(i);
            elite(g, "lichtstraal_" + (i + 1), rond[i], p(l.getX() + 6, l.getZ() + 6), 5);
        }
        BlockPos w = WereldPlekken.plek("waterval");
        elite(g, "waterval", MOSGEEST, p(w.getX() + 14, w.getZ() + 10), 6);
        elite(g, "zwamnaald", WACHTER, p(-496, -28), 6);
    }

    // ---- Druipgrotten, lv 12-18: the rock hills + the caves --------------------------------------------------------------

    private static void druipgrotten(Random r) {
        String g = "druipgrotten";
        List<int[][]> paden = WezensPlekken.paden("grotten", "grotten2", "grotten3", "grotten4", "druip");
        int[][] niet = {};
        langsPaden(g, r, paden, 55, niet, KNAAG, 3, KRISTAL, 2, GLIM, 1);
        kampen(g, r, 2, paden, niet, new Soort[][] {{KNAAG, GLIM}, {KRISTAL, GLIM}, {KNAAG, KRISTAL}}, new int[][] {{3, 1}, {2, 1}, {3, 1}});
        // the caves: along every tunnel, groups every 26 blocks (druipslijm on the low tunnel ceilings)
        int n = 0;
        for (WezensPlekken.Gang gang : WezensPlekken.gangen()) {
            double L = gang.lengte();
            int stuk = 0;
            for (double t = 10; t < L - 8; t += 26) {
                int[] c = gang.punt(t / L);
                Soort s = stuk++ % 3 == 0 ? KNAAG : kies(r, DRUIP, 4, KRISTAL, 1, GLIM, 1);
                int max = s == KNAAG ? 3 : s == GLIM ? 1 : 2;
                String groep = g + "_gang_" + n++;
                zet(g, groep, s, new BlockPos(c[0], c[1] + 2, c[2]), 3, max, 300 + r.nextInt(201), ELITE, true, c[1] - 6, c[1] + 2);
            }
        }
        // the four knots: a camp each (+ an elite), on the knot floor and its ledges
        Soort[][] knoopKamp = {{DRUIP, KNAAG, GLIM}, {KRISTAL, DRUIP}, {KNAAG, KRISTAL}, {DRUIP, KRISTAL, GLIM}};
        int[][] knoopAantal = {{2, 3, 1}, {2, 2}, {3, 2}, {2, 2, 1}};
        Soort[] knoopElite = {KNAAG, KRISTAL, DRUIP, KRISTAL};
        for (int i = 0; i < WezensPlekken.KNOPEN.length; i++) {
            int[] k = WezensPlekken.KNOPEN[i];   // centre x, y, z, r
            int straal = k[3] - 3, maxY = k[1] + 3, minY = k[1] - k[3];
            String groep = g + "_knoop_" + (i + 1);
            for (int j = 0; j < knoopKamp[i].length; j++) {
                zet(g, groep, knoopKamp[i][j], new BlockPos(k[0], maxY, k[2]), straal, knoopAantal[i][j], 500, ELITE, true, minY, maxY);
            }
            zet(g, g + "_elite_knoop_" + (i + 1), knoopElite[i], new BlockPos(k[0], maxY, k[2]), straal, 1, 3600, 1.0, true, minY, maxY);
        }
        // the crystal chamber
        BlockPos kk = WereldPlekken.plek("kristalkamer");
        zet(g, g + "_kristalkamer", KRISTAL, kk.above(4), 6, 2, 600, ELITE, true, kk.getY() - 3, kk.getY() + 4);
        zet(g, g + "_kristalkamer", GLIM, kk.above(4), 6, 1, 600, ELITE, true, kk.getY() - 3, kk.getY() + 4);
        // elites outside the three cave mouths
        int e = 0;
        for (int[] in : WezensPlekken.GROT_INGANGEN) {
            elite(g, "ingang_" + ++e, e == 2 ? KRISTAL : KNAAG, p(in[0] + 14, in[1] + 10), 5);
        }
    }

    // ---- Zwamtempel, lv 18-20: the mountain path + the voorhal ----------------------------------------------------------

    private static void zwamtempel(Random r) {
        String g = "zwamtempel";
        int[][] berg = WezensPlekken.BERGPAD;
        double tot = 0;
        for (int i = 0; i + 1 < berg.length; i++) {
            tot += Math.hypot(berg[i + 1][0] - berg[i][0], berg[i + 1][1] - berg[i][1]);
        }
        int n = 0;
        for (double[] p : langs(berg, 20, 24)) {
            double langsT = WezensPlekken.bergLangs(p[0], p[1]);
            int y = (int) Math.round(WezensPlekken.bergY(langsT / tot)) + 1;
            Soort s = n % 4 == 3 ? PRIESTER : RIDDER;
            String gebied = Gebieden.op(p[0], p[1]) == null ? "ring" : Gebieden.op(p[0], p[1]).id();
            if (!vrij(gebied, p[0], p[1], 3, new int[0][])) {
                continue;
            }
            zet(gebied, g + "_bergpad_" + n++, s, new BlockPos((int) p[0], y + 3, (int) p[1]), 2, s == PRIESTER ? 1 : 2, 400, ELITE, true, y - 4, y + 3);
        }
        // elites: at the fossil bridge end and the first hairpin
        int y1 = (int) Math.round(WezensPlekken.bergY(0.08)) + 1;
        zet("zwamtempel", g + "_elite_brug", RIDDER, new BlockPos(225, y1 + 3, -278), 2, 1, 3600, 1.0, true, y1 - 6, y1 + 6);
        int y2 = (int) Math.round(WezensPlekken.bergY(WezensPlekken.bergLangs(405, -312) / tot)) + 1;
        zet("zwamtempel", g + "_elite_bocht", PRIESTER, new BlockPos(405, y2 + 3, -312), 2, 1, 3600, 1.0, true, y2 - 6, y2 + 6);
        // the voorhal (stand y 152): two knights + a priest, like before
        for (int i = 0; i < WezensPlekken.TEMPELRIDDER.length; i++) {
            BlockPos c = WezensPlekken.TEMPELRIDDER[i];
            zet(g, g + "_voorhal_" + i, RIDDER, c, 6, 1, 900, ELITE, true, c.getY() - 24, c.getY() + 1);
        }
        for (BlockPos c : WezensPlekken.SPORENPRIESTER) {
            zet(g, g + "_voorhal_p", PRIESTER, c, 5, 1, 1200, ELITE, true, c.getY() - 24, c.getY() + 1);
        }
    }

    public static int aantalOppervlak() {
        return oppervlak;
    }

    public static int aantalVloer() {
        return vloer;
    }

    /** One spawn of this spawner now (kern's spawn; the gametest and the dev command). */
    @Nullable
    public static VadsrijkMob spawn(ServerLevel sl, Spawners.Spawner s) {
        return Spawners.spawn(sl, s);
    }
}
