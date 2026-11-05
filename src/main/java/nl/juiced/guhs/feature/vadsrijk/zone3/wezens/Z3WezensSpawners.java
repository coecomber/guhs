package nl.juiced.guhs.feature.vadsrijk.zone3.wezens;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The zone-3 spawners, all in kern's {@link Spawners} (it ticks them), PER LAYER and only inside z3-wereld's mob zones
 * ({@link Z3WezensPlekken#MOBZONES}). Every spawner is in cave/interior mode ({@code grot(minY, maxY)}) so its floor search stays
 * inside its own layer (the crown never takes the ground's spawns, the ground never finds the crown's leaves or a branch) and "a
 * player near" counts only inside that y-range.
 * <ul>
 *   <li>Kruinzee: a jittered grid over the 3 crown zones, floor search down from y 300 to the crown;</li>
 *   <li>Takkenbanen: points along the W/NW/ZW branch ridges (the Takbaan), set off to the side of the ride path on the branch top,
 *       inside the branch zones;</li>
 *   <li>Schorsstad: 6 spawners per ring terrace (7 rings, y 81-177) along the inner wall;</li>
 *   <li>Wortelvelden: a jittered grid over the 4 valley zones; each valley its own mix (the mould farm = Mika's, the marsh and
 *       the blossom dale = snakes, the Lichtkeverbos = boars);</li>
 *   <li>Wortelduister: a jittered grid over the tunnel and the caves, floor search down from y 60.</li>
 * </ul>
 * MMO-dense: the spacing is chosen so each layer holds 70-100 creatures at most (ronde 14). No spawner circle comes within
 * {@link Z3WezensPlekken#PAD_VRIJ} of a riding route AT ITS HEIGHT (a spawner near a path gets a smaller circle, min 4) or near a
 * safe spot, and at run time a kern VERBODEN rule refuses any zone-3 spot within 5 of a path at that height or inside a safe spot.
 * The 5 named area leaders stand at their landmarks (always elite, 5 min respawn). The 8 contract elites are NEVER here: only
 * {@link Z3Contracten#spawnElite}.
 */
public final class Z3WezensSpawners {
    public static final double ELITE = 0.06;
    public static final int MAX_LEVEND = 100;

    /** One kind in a mix: type, weight, max alive, respawn ticks, radius. */
    record Soort(String naam, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int gewicht, int max, int respawn, int straal) {
        Soort metGewicht(int g) {
            return new Soort(naam, type, g, max, respawn, straal);
        }
    }

    /** Layer id -> the sum of its spawners' max (dev command + gametest). */
    public static final Map<String, Integer> MAX_PER_GEBIED = new LinkedHashMap<>();
    public static final List<Spawners.Spawner> ALLE = new ArrayList<>();
    /** Which layer a spawner belongs to (spawner id -> layer). */
    public static final Map<String, Z3WezensPlekken.Laag> LAAG_VAN = new LinkedHashMap<>();

    private Z3WezensSpawners() {
    }

    private static Soort s(String naam, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int gewicht, int max, int respawn, int straal) {
        return new Soort(naam, type, gewicht, max, respawn, straal);
    }

    private static boolean geregistreerd;

    /** From Z3WezensRegistry.register. */
    public static void registreer() {
        ALLE.clear();
        MAX_PER_GEBIED.clear();
        LAAG_VAN.clear();
        if (!geregistreerd) {
            geregistreerd = true;
            // run time: no zone-3 creature next to a path at its height, nor inside a safe spot
            Spawners.VERBODEN.add((sl, p) -> Z3WezensPlekken.inZone(p.getX(), p.getZ())
                    && (Z3WezensPlekken.padAfstand(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) < Z3WezensPlekken.PAD_VRIJ
                            || Z3WezensPlekken.veilig(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 4)));
        }
        raster(Z3WezensPlekken.KRUINZEE, 3001, Z3WezensSpawners::kruinMix);
        takken();
        schorsstad();
        raster(Z3WezensPlekken.WORTELVELDEN, 3004, Z3WezensSpawners::veldMix);
        raster(Z3WezensPlekken.WORTELDUISTER, 3005, Z3WezensSpawners::duisterMix);
        leiders();
    }

    // ---- the mixes per zone ------------------------------------------------------------------------------------------------

    private static List<Soort> kruinMix(Z3WezensPlekken.Zone z) {
        Soort rover = s("bladrover", Z3WezensRegistry.BLADROVER, 2, 3, 700, 14);
        Soort rups = s("grauwe_rups", Z3WezensRegistry.GRAUWE_RUPS, 3, 2, 800, 16);
        Soort bloem = s("bijtbloesem", Z3WezensRegistry.BIJTBLOESEM, 3, 3, 700, 12);
        if (z.id().contains("zeppelin")) {
            return List.of(rover.metGewicht(6), rups.metGewicht(1));   // the Mika dock: pirates
        }
        return List.of(rover.metGewicht(1), rups, bloem);
    }

    private static List<Soort> veldMix(Z3WezensPlekken.Zone z) {
        Soort zwijn = s("rotzwijn", Z3WezensRegistry.ROTZWIJN, 3, 2, 800, 16);
        Soort slang = s("wortelslang", Z3WezensRegistry.WORTELSLANG, 2, 2, 800, 14);
        Soort boer = s("mika_schimmelboer", Z3WezensRegistry.MIKA_SCHIMMELBOER, 2, 2, 800, 14);
        if (z.id().contains("schimmel")) {
            return List.of(zwijn.metGewicht(2), boer.metGewicht(5));
        }
        if (z.id().contains("honingmoeras") || z.id().contains("paddenbloesem")) {
            return List.of(zwijn.metGewicht(2), slang.metGewicht(4), boer.metGewicht(1));
        }
        return List.of(zwijn.metGewicht(4), slang.metGewicht(1), boer.metGewicht(1));
    }

    private static List<Soort> duisterMix(Z3WezensPlekken.Zone z) {
        Soort schim = s("wortelschim", Z3WezensRegistry.WORTELSCHIM, 4, 3, 700, 14);
        Soort ziener = s("dove_ziener", Z3WezensRegistry.DOVE_ZIENER, 1, 1, 1200, 10);
        Soort boer = s("mika_schimmelboer", Z3WezensRegistry.MIKA_SCHIMMELBOER, 2, 2, 800, 12);
        if (z.id().contains("gang")) {
            return List.of(schim.metGewicht(5), boer.metGewicht(1));   // the riding tunnel's sides: no blind giants there
        }
        return List.of(schim, ziener, boer);
    }

    interface Mix {
        List<Soort> op(Z3WezensPlekken.Zone z);
    }

    // ---- planning ----------------------------------------------------------------------------------------------------------

    /** A planned spawner before registering. */
    private record Plan(int x, int z, Soort s, int straal, int minY, int maxY, int minLevel, int maxLevel) {
    }

    /** Is a flat circle (with margin) safe anywhere within these heights? */
    private static boolean veiligInLaag(double x, double z, double straal, int minY, int maxY) {
        for (int y = minY; y <= maxY; y += 8) {
            if (Z3WezensPlekken.veilig(x, y, z, straal + 6)) {
                return true;
            }
        }
        return Z3WezensPlekken.veilig(x, maxY, z, straal + 6);
    }

    @Nullable
    private static Z3WezensPlekken.Zone zoneOp(List<Z3WezensPlekken.Zone> zones, double x, double z) {
        for (Z3WezensPlekken.Zone g : zones) {
            if (g.bevatPlat(x, z)) {
                return g;
            }
        }
        return null;
    }

    /** A jittered grid over a layer's mob zones whose spacing gives at most {@link #MAX_LEVEND} alive. */
    private static void raster(Z3WezensPlekken.Laag laag, long seed, Mix mix) {
        List<Z3WezensPlekken.Zone> zones = Z3WezensPlekken.zonesVan(laag);
        List<Plan> plan = List.of();
        for (int afstand = 18; afstand <= 200; afstand += 2) {
            plan = plan(zones, seed, mix, afstand);
            if (plan.stream().mapToInt(p -> p.s().max()).sum() <= MAX_LEVEND) {
                break;
            }
        }
        registreer(laag, plan);
    }

    private static List<Plan> plan(List<Z3WezensPlekken.Zone> zones, long seed, Mix mix, int afstand) {
        Random r = new Random(seed);
        int jitter = Math.max(3, afstand / 3);
        int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (Z3WezensPlekken.Zone g : zones) {
            int[] d = g.doos();
            x0 = Math.min(x0, d[0]);
            z0 = Math.min(z0, d[1]);
            x1 = Math.max(x1, d[2]);
            z1 = Math.max(z1, d[3]);
        }
        List<Plan> uit = new ArrayList<>();
        for (int x = x0 + afstand / 2; x <= x1; x += afstand) {
            for (int z = z0 + afstand / 2; z <= z1; z += afstand) {
                int px = x + r.nextInt(2 * jitter + 1) - jitter, pz = z + r.nextInt(2 * jitter + 1) - jitter;
                int k = r.nextInt(1000);
                Z3WezensPlekken.Zone g = zoneOp(zones, px, pz);
                if (g == null || !Z3WezensPlekken.inZone(px, pz)) {
                    continue;
                }
                Soort s = kies(mix.op(g), k);
                int straal = (int) Math.floor(Math.min(s.straal(), Z3WezensPlekken.padAfstand(px, pz, g.minY(), g.maxY()) - Z3WezensPlekken.PAD_VRIJ));
                if (straal < 4 || veiligInLaag(px, pz, straal, g.minY(), g.maxY())) {
                    continue;
                }
                uit.add(new Plan(px, pz, s, straal, g.minY(), g.maxY(), g.minLevel(), g.maxLevel()));
            }
        }
        return uit;
    }

    private static Soort kies(List<Soort> mix, int k) {
        int som = mix.stream().mapToInt(Soort::gewicht).sum();
        int t = k % som;
        for (Soort m : mix) {
            t -= m.gewicht();
            if (t < 0) {
                return m;
            }
        }
        return mix.get(0);
    }

    private static void registreer(Z3WezensPlekken.Laag laag, List<Plan> plan) {
        int max = MAX_PER_GEBIED.getOrDefault(laag.id(), 0), i = ALLE.size();
        for (Plan p : plan) {
            Soort s = p.s();
            BlockPos c = new BlockPos(p.x(), (p.minY() + p.maxY()) / 2, p.z());
            Spawners.Spawner sp = new Spawners.Spawner("z3wezens:" + laag.id() + "_" + i++ + "_" + s.naam(), c, p.straal(), s.type(), s.max(), s.respawn(),
                    p.minLevel(), p.maxLevel(), null, ELITE).grot(p.minY(), p.maxY());
            Spawners.registreer(sp);
            ALLE.add(sp);
            LAAG_VAN.put(sp.id(), laag);
            max += s.max();
        }
        MAX_PER_GEBIED.put(laag.id(), max);
    }

    /** The branches: points every {@code stap} blocks along each ridge path, alternately left and right of it. */
    private static void takken() {
        Z3WezensPlekken.Laag laag = Z3WezensPlekken.TAKKENBANEN;
        List<Soort> mix = List.of(s("kaasschutter", Z3WezensRegistry.KAASSCHUTTER, 3, 3, 700, 7),
                s("schorsmarter", Z3WezensRegistry.SCHORSMARTER, 3, 2, 700, 7),
                s("grauwe_wesp", Z3WezensRegistry.GRAUWE_WESP, 2, 2, 700, 7));
        List<Plan> plan = List.of();
        for (int stap = 10; stap <= 120; stap += 2) {
            plan = takPlan(mix, stap);
            if (plan.stream().mapToInt(p -> p.s().max()).sum() <= MAX_LEVEND) {
                break;
            }
        }
        registreer(laag, plan);
    }

    private static List<Plan> takPlan(List<Soort> mix, int stap) {
        // (P-VERHUIS) the revamped big branches: narrow (r ~11 at the trunk, ~5.5 at the tip); spawners on the flanks beside the
        // ridge path, alternately left and right, the floor search over the branch layer finds the branch surface
        List<Z3WezensPlekken.Zone> zones = Z3WezensPlekken.zonesVan(Z3WezensPlekken.TAKKENBANEN);
        Random r = new Random(3002);
        List<Plan> uit = new ArrayList<>();
        int kant = 1;
        for (Z3WezensPlekken.Tak tak : Z3WezensPlekken.TAKKEN) {
            Z3WezensPlekken.Pad pad = Z3WezensPlekken.pad(tak.pad());
            if (pad == null) {
                continue;
            }
            int[][] p = pad.punten();
            double totaal = 0;
            for (int i = 0; i + 1 < p.length; i++) {
                totaal += Math.hypot(p[i + 1][0] - p[i][0], p[i + 1][1] - p[i][1]);
            }
            double gedaan = 0;
            for (int i = 0; i + 1 < p.length; i++) {
                double ax = p[i][0], az = p[i][1], bx = p[i + 1][0], bz = p[i + 1][1];
                double len = Math.hypot(bx - ax, bz - az);
                if (len < 1) {
                    continue;
                }
                for (double t = (i == 0 ? stap / 2.0 : 0); t < len; t += stap) {
                    double f = t / len, langs = (gedaan + t) / Math.max(1, totaal);
                    double x = ax + (bx - ax) * f, z = az + (bz - az) * f;
                    double rt = tak.rStam() + (tak.rPunt() - tak.rStam()) * Math.min(1, langs * 1.4);
                    Soort s = kies(mix, r.nextInt(1000));
                    int straal = (int) Math.max(4, Math.min(s.straal(), rt * 0.55));
                    double zij = Math.max(Z3WezensPlekken.PAD_BREEDTE / 2 + Z3WezensPlekken.TAK_VRIJ + 1, rt * 0.55);
                    kant = -kant;
                    double nx = -(bz - az) / len, nz = (bx - ax) / len;
                    int wx = (int) Math.round(x + nx * zij * kant), wz = (int) Math.round(z + nz * zij * kant);
                    Z3WezensPlekken.Zone g = zoneOp(zones, wx, wz);
                    if (g == null) {
                        continue;
                    }
                    int minY = Z3WezensPlekken.TAK_MIN_Y, maxY = Z3WezensPlekken.TAK_MAX_Y;
                    // the ridge path itself is checked per spawn (TAK_VRIJ, see Z3WezensSpawners' tick); other routes keep their full margin
                    int st = (int) Math.floor(Math.min(straal, Z3WezensPlekken.padAfstandBuitenTakken(wx, wz, minY, maxY) - Z3WezensPlekken.PAD_VRIJ));
                    if (st < 4 || veiligInLaag(wx, wz, st, minY, maxY)) {
                        continue;
                    }
                    uit.add(new Plan(wx, wz, s, st, minY, maxY, g.minLevel(), g.maxLevel()));
                }
                gedaan += len;
            }
        }
        return uit;
    }

    /** The Schorsstad mob zone (the dark stair section above Honingtak): per height step 6 spawners on the stair (turned a little per step). */
    private static void schorsstad() {
        Z3WezensPlekken.Laag laag = Z3WezensPlekken.SCHORSSTAD;
        List<Soort> mix = List.of(s("schorskruiper", Z3WezensRegistry.SCHORSKRUIPER, 3, 2, 800, 7),
                s("lantaarnloze", Z3WezensRegistry.LANTAARNLOZE, 2, 2, 800, 7),
                s("harsgolem", Z3WezensRegistry.HARSGOLEM, 2, 2, 900, 7));
        Random r = new Random(3003);
        List<Plan> plan = new ArrayList<>();
        for (int ring = 0; ring < Z3WezensPlekken.RINGEN.length; ring++) {
            int y = Z3WezensPlekken.RINGEN[ring];
            for (int k = 0; k < 6; k++) {
                double a = Math.toRadians(k * 60 + ring * 23 + 11);
                int x = (int) Math.round(Z3WezensPlekken.TX + Math.cos(a) * Z3WezensPlekken.RING_STRAAL);
                int z = (int) Math.round(Z3WezensPlekken.TZ + Math.sin(a) * Z3WezensPlekken.RING_STRAAL);
                Soort s = kies(mix, r.nextInt(1000));
                int minY = y - 4, maxY = y + 8;
                int st = (int) Math.floor(Math.min(s.straal(), Z3WezensPlekken.padAfstand(x, z, minY, maxY) - Z3WezensPlekken.PAD_VRIJ));
                if (st < 4 || veiligInLaag(x, z, st, minY, maxY)) {
                    continue;
                }
                plan.add(new Plan(x, z, s, st, minY, maxY, laag.minLevel(), laag.maxLevel()));
            }
        }
        registreer(laag, plan);
    }

    /** The five named area leaders at their landmarks: always elite, level = the area's top + 3 (max 50), 5 min respawn. */
    private static void leiders() {
        leider("rupsenmoeder", Z3WezensRegistry.GRAUWE_RUPS, Z3WezensPlekken.KRUINZEE);
        leider("kapitein_bladrover", Z3WezensRegistry.BLADROVER, Z3WezensPlekken.KRUINZEE);
        leider("harskolos", Z3WezensRegistry.HARSGOLEM, Z3WezensPlekken.SCHORSSTAD);
        leider("wilde_rotkeiler", Z3WezensRegistry.ROTZWIJN, Z3WezensPlekken.WORTELVELDEN);
        leider("ziener_oudste", Z3WezensRegistry.DOVE_ZIENER, Z3WezensPlekken.WORTELDUISTER);
    }

    private static void leider(String naam, Supplier<? extends EntityType<? extends VadsrijkMob>> type, Z3WezensPlekken.Laag laag) {
        BlockPos p = Z3WezensPlekken.LEIDERS.get(naam);
        int lv = Math.min(50, laag.maxLevel() + 3);
        Spawners.Spawner sp = new Spawners.Spawner("z3wezens:leider_" + naam, p, 5, type, 1, 6000, lv, lv, null, 1.0).grot(p.getY() - 24, p.getY() + 10);
        Spawners.registreer(sp);
        ALLE.add(sp);
        LAAG_VAN.put(sp.id(), laag);
        MAX_PER_GEBIED.merge(laag.id(), 1, Integer::sum);
    }

    /** The layer a spawner belongs to (or null). */
    public static Z3WezensPlekken.Laag laagVan(Spawners.Spawner s) {
        return LAAG_VAN.get(s.id());
    }
}
