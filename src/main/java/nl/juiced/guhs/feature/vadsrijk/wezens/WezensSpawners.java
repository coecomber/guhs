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
 * <li>(balans 2026-11, second playtest) nothing spawns ON a path any more: every open-air spawner whose circle reaches within
 * {@link Spawners#PAD_VRIJ} of a path edge is moved sideways into the wilderness ({@link #naastPad}; the same groups and
 * maxima, so the per-area plan is unchanged), patrols walk a line parallel to the path beside it, the mountain path's
 * knights stand on the flanks beside the ramp, and kern refuses any spawn spot next to a path ({@link Spawners#bijPad}).</li>
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

    /** The no-go circles of the area being planned (for {@link #naastPad}). */
    private static int[][] niet = new int[0][];
    /** How many open-air spawners were moved off a path / could not be moved (kern's spawn check still keeps those off it). */
    public static int verschoven, nietVerschoven;

    /**
     * (balans 2026-11) A spawner centre whose circle (radius {@code straal}) reaches within {@link Spawners#PAD_VRIJ} of a path
     * edge, moved straight away from the nearest path until it clears every path: first to the side it is already on, then the
     * other side, then 4 and 8 further. Returns the centre unchanged when it is clear, null when no free spot was found.
     */
    @Nullable
    static double[] naastPad(String gebied, double x, double z, int straal, int[][] nietHier) {
        double nodig = straal + Spawners.PAD_VRIJ + 1;
        double[] d = Spawners.dichtstbijPad(x, z);
        if (d == null || d[2] >= nodig) {
            return new double[] {x, z};
        }
        double nx = x - d[0], nz = z - d[1], L = Math.hypot(nx, nz);
        if (L < 0.5) {
            nx = -d[4];
            nz = d[3];
        } else {
            nx /= L;
            nz /= L;
        }
        double breedte = L - d[2];   // the half width of that path
        for (int verder = 0; verder <= 8; verder += 4) {
            for (int kant = 1; kant >= -1; kant -= 2) {
                double a = breedte + nodig + verder;
                double tx = d[0] + kant * nx * a, tz = d[1] + kant * nz * a;
                if (Spawners.padAfstand(tx, tz) >= nodig && vrij(gebied, tx, tz, straal, nietHier)) {
                    return new double[] {tx, tz};
                }
            }
        }
        return null;
    }

    /** {@link #naastPad} for a spawner centre: the moved centre (same y), or the old one when nothing was free. */
    private static BlockPos vanPadAf(String gebied, BlockPos c, int straal, boolean tel) {
        double[] n = naastPad(gebied, c.getX(), c.getZ(), straal, niet);
        if (n == null) {
            if (tel) {
                nietVerschoven++;
            }
            return c;
        }
        if ((int) Math.round(n[0]) == c.getX() && (int) Math.round(n[1]) == c.getZ()) {
            return c;
        }
        if (tel) {
            verschoven++;
        }
        return new BlockPos((int) Math.round(n[0]), c.getY(), (int) Math.round(n[1]));
    }

    private static void zet(String gebied, String groep, Soort s, BlockPos c, int straal, int max, int respawn, double elite, boolean grot,
            int minY, int maxY) {
        if (!grot) {
            c = vanPadAf(gebied, c, straal, true);
        }
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
        // 1.10.1: the Druipgrotten tunnels, knots and the crystal chamber are real caves: never a spot under the open sky
        nl.juiced.guhs.feature.vadsrijk.kern.mob.GrotRuimte.ONDERDAK.addAll(java.util.List.of("wezens:druipgrotten_gang_", "wezens:druipgrotten_knoop_",
                "wezens:druipgrotten_elite_knoop_", "wezens:druipgrotten_kristalkamer"));
        WezensPlekken.allePaden().forEach((naam, pad) -> Spawners.registreerPad("z1:" + naam, pad, WezensPlekken.padBreedte(naam)));
        verschoven = nietVerschoven = 0;

        Random r = new Random(1100);
        klodderweide(r);
        sporenwoud(r);
        druipgrotten(r);
        zwamtempel(r);
        verticaal();
    }

    /** A spot of the vertical layer (fix round 1 "verticaal"): only when the map that is loaded has it. */
    @Nullable
    private static BlockPos plekAls(String id) {
        return WereldPlekken.ALLE.get(id);
    }

    /**
     * The Hoedenpaden (the hat layer 30-50 up in the Sporenwoud) and the Druipschacht (f-verticaal; their WereldPlekken ids
     * exist once the map with them is generated, otherwise nothing is added). Narrow y-ranges so the hat layer and the
     * forest floor below never mix.
     */
    private static void verticaal() {
        String w = "sporenwoud", d = "druipgrotten";
        Soort[] hoed = {HOEDJES, PUFFER, MOSGEEST, HOEDJES, WACHTER, PUFFER, HOEDJES, MOSGEEST};
        for (int i = 1; i <= 8; i++) {
            BlockPos b = plekAls("hoedenpad_wezen_" + i);
            if (b != null) {
                zet(w, w + "_hoedenpad_" + i, hoed[i - 1], b.above(1), 3, 2, 300, ELITE, true, b.getY() - 2, b.getY() + 1);
            }
        }
        BlockPos nest = plekAls("hoedenpad_elitenest");
        if (nest != null) {
            zet(w, w + "_elite_hoedennest", WACHTER, nest.above(1), 3, 1, 3600, 1.0, true, nest.getY() - 2, nest.getY() + 1);
            zet(w, w + "_hoedennest", MOSGEEST, nest.above(1), 4, 2, 600, ELITE, true, nest.getY() - 2, nest.getY() + 1);
        }
        Soort[] richel = {KNAAG, KRISTAL, KNAAG, KRISTAL};
        for (int i = 1; i <= 4; i++) {
            BlockPos b = plekAls("druipschacht_richel_" + i);
            if (b != null) {
                zet(d, d + "_schacht_richel_" + i, richel[i - 1], b.above(1), 2, 2, 400, ELITE, true, b.getY() - 2, b.getY() + 1);
            }
        }
        BlockPos bodem = plekAls("druipschacht_bodem");
        if (bodem != null) {
            zet(d, d + "_schacht_bodem", KRISTAL, bodem.above(1), 4, 2, 500, ELITE, true, bodem.getY() - 2, bodem.getY() + 1);
            zet(d, d + "_schacht_bodem", GLIM, bodem.above(1), 4, 1, 500, ELITE, true, bodem.getY() - 2, bodem.getY() + 1);
        }
        BlockPos balkon = plekAls("druipschacht_balkon");
        if (balkon != null) {
            zet(d, d + "_elite_schacht_balkon", KRISTAL, balkon.above(1), 2, 1, 3600, 1.0, true, balkon.getY() - 2, balkon.getY() + 1);
        }
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
                        // (balans 2026-11) the pair walks beside the path, parallel to it, not on it
                        BlockPos c = new BlockPos((int) p[0], Spawners.OPPERVLAK_MIN_Y + 6, (int) p[1]);
                        BlockPos c2 = vanPadAf(gebied, c, 3, false);
                        a = a.offset(c2.getX() - c.getX(), 0, c2.getZ() - c.getZ());
                        b = b.offset(c2.getX() - c.getX(), 0, c2.getZ() - c.getZ());
                        opp(gebied, groep, s, c2, 3, 2, 300, ELITE);
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
        WezensSpawners.niet = niet;
        langsPaden(g, r, paden, 18, niet, KLODDER, 6, BUBBEL, 2, STUITER, 2);
        kampen(g, r, 12, paden, niet, new Soort[][] {{KLODDER, BUBBEL}, {KLODDER, STUITER}, {STUITER, BUBBEL}},
                new int[][] {{3, 1}, {2, 2}, {2, 2}});
        // the pond rims: bubbelklodders
        int v = 0;
        for (int[] vijver : WezensPlekken.VIJVERS) {
            for (int a = 0; a < 2; a++) {
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
        WezensSpawners.niet = niet;
        langsPaden(g, r, paden, 16, niet, HOEDJES, 4, PUFFER, 3, WACHTER, 2, MOSGEEST, 2);
        kampen(g, r, 16, paden, niet, new Soort[][] {{HOEDJES, PUFFER}, {WACHTER, PUFFER}, {MOSGEEST, HOEDJES}, {HOEDJES, WACHTER}},
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
        WezensSpawners.niet = niet;
        langsPaden(g, r, paden, 26, niet, KNAAG, 3, KRISTAL, 2, GLIM, 1);
        kampen(g, r, 6, paden, niet, new Soort[][] {{KNAAG, GLIM}, {KRISTAL, GLIM}, {KNAAG, KRISTAL}}, new int[][] {{3, 1}, {2, 1}, {3, 1}});
        // the caves: along every tunnel, groups every 15 blocks (druipslijm on the low tunnel ceilings)
        int n = 0;
        for (WezensPlekken.Gang gang : WezensPlekken.gangen()) {
            double L = gang.lengte();
            int stuk = 0;
            for (double t = 10; t < L - 8; t += 15) {
                int[] c = gang.punt(t / L);
                Soort s = stuk++ % 3 == 0 ? KNAAG : kies(r, DRUIP, 4, KRISTAL, 1, GLIM, 1);
                int max = s == KNAAG ? 3 : s == GLIM ? 1 : 2;
                String groep = g + "_gang_" + n++;
                // 1.10.1: c = the tunnel's centre line (2 above its floor): search the tunnel itself (floor c - 2 .. c + 1), not the
                // rock 4 blocks above/below it (hollow pockets there filled up with mobs just outside the cave)
                zet(g, groep, s, new BlockPos(c[0], c[1], c[2]), 3, max, 300 + r.nextInt(201), ELITE, true, c[1] - 3, c[1] + 1);
            }
        }
        // the four knots: a camp each (+ an elite), on the knot floor and its ledges
        Soort[][] knoopKamp = {{DRUIP, KNAAG, GLIM}, {KRISTAL, DRUIP}, {KNAAG, KRISTAL}, {DRUIP, KRISTAL, GLIM}};
        int[][] knoopAantal = {{2, 3, 1}, {2, 2}, {3, 2}, {2, 2, 1}};
        Soort[] knoopElite = {KNAAG, KRISTAL, DRUIP, KRISTAL};
        for (int i = 0; i < WezensPlekken.KNOPEN.length; i++) {
            int[] k = WezensPlekken.KNOPEN[i];   // centre x, y, z, r
            // 1.10.1: from the knot's flat floor (voet) up; was centre - r (pockets under the floor) to centre + 3
            int voet = WezensPlekken.knoopVoet(k);
            int straal = k[3] - 3, maxY = voet + 4, minY = voet - 1;
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

    /** Spawn circle of the mountain-path spawners: reaches past the ramp (4.5) + the free strip (5) onto the flanks. */
    static final int BERG_STRAAL = 14;

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
            // (balans 2026-11) on the flanks beside the ramp: a wide circle around the path point, kern refuses the ramp itself
            // (Spawners.bijPad: within 5 of its edge at its height); the floor search covers the slope up and down
            zet(gebied, g + "_bergpad_" + n++, s, new BlockPos((int) p[0], y + 3, (int) p[1]), BERG_STRAAL, s == PRIESTER ? 1 : 2, 400, ELITE, true,
                    y - 10, y + 10);
        }
        // elites: at the fossil bridge end and the first hairpin
        // (integration: the path was rerouted by fix wild; the elites follow the path instead of fixed old coordinates)
        int[] b1 = WezensPlekken.bergPunt(0.08 * tot);
        int y1 = (int) Math.round(WezensPlekken.bergY(0.08)) + 1;
        zet("zwamtempel", g + "_elite_brug", RIDDER, new BlockPos(b1[0], y1 + 3, b1[1]), BERG_STRAAL, 1, 3600, 1.0, true, y1 - 10, y1 + 10);
        int[] bocht = berg[3];
        int y2 = (int) Math.round(WezensPlekken.bergY(WezensPlekken.bergLangs(bocht[0], bocht[1]) / tot)) + 1;
        zet("zwamtempel", g + "_elite_bocht", PRIESTER, new BlockPos(bocht[0], y2 + 3, bocht[1]), BERG_STRAAL, 1, 3600, 1.0, true, y2 - 10, y2 + 10);
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
