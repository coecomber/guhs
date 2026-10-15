package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * biomes3 fix-plaatsing: measures WHERE the three biomes lie and how big each lake and each Wolkenweide is, from the
 * model's two region values alone ({@link BioModel#eDal}, {@link BioModel#eWeide}), over a large square. No chunk map
 * is built, so tens of millions of columns take seconds. Used by the game test and by {@code /guhs bio wereld regios}.
 * <p>
 * A lake is a connected piece of columns where the dal value has passed the valley floor ({@link DalTerrein#TRAP}[3];
 * the fraying of its edge, a few blocks, is left out); a Wolkenweide a connected piece where the meadow value is above 0.
 * Pieces that touch the border of the square are left out of the size tables (they are cut off).
 */
public final class PlaatsingMeting {
    /** One connected piece: its bounding box (blocks), its area (blocks squared) and what else was counted in it. */
    public record Stuk(int x0, int z0, int x1, int z1, long oppervlak, int extra, boolean afgesneden) {
        public int breed() {
            return Math.max(x1 - x0, z1 - z0);
        }

        public int smal() {
            return Math.min(x1 - x0, z1 - z0);
        }

        public int mx() {
            return (x0 + x1) / 2;
        }

        public int mz() {
            return (z0 + z1) / 2;
        }
    }

    public static final class Uitkomst {
        public final List<Stuk> meren = new ArrayList<>(), dalen = new ArrayList<>(), weiden = new ArrayList<>();
        public double deelDal, deelMeer, deelWeide;
        public double[] steilDal = new double[3], steilWeide = new double[3];
        public String tekst = "";
    }

    private static final int MEER = 1, DAL = 2, WEIDE = 4, WEIDE_BINNEN = 8;

    // --- the regions as they were before biomes3 fix-plaatsing: the tops of two noises (kept to measure and draw "before") ---
    private static final double OUD_DAL_VANAF = 0.39, OUD_WEIDE_VANAF = 0.62, OUD_WEIDE_DAL_AF = 0.05;

    public static double oudDal(BioModel m, double x, double z) {
        double r = m.ruis(BioModel.R_DAL, x, z) - OUD_DAL_VANAF;
        return r <= 0 ? r : Math.min(r, m.masker(x, z));
    }

    public static double oudWeide(BioModel m, double x, double z) {
        double r = m.ruis(BioModel.R_WEIDE, x, z) - OUD_WEIDE_VANAF;
        if (r <= 0) {
            return r;
        }
        r = Math.min(r, -OUD_WEIDE_DAL_AF - (m.ruis(BioModel.R_DAL, x, z) - OUD_DAL_VANAF));
        return r <= 0 ? r : Math.min(r, m.masker(x, z));
    }

    /** Measures the square [-straal, straal] around (mx, mz), every {@code stap} blocks. */
    public static Uitkomst meet(BioModel m, int mx, int mz, int straal, int stap) {
        return meet(m, mx, mz, straal, stap, false, null);
    }

    /**
     * As {@link #meet(BioModel, int, int, int, int)}; {@code oud}: the regions as they were (two noise tops); {@code kaart}:
     * a file that gets the map, one byte per sample (0 other land, 1 Klaterdal, 2 the lake's shore strip, 3 lake water,
     * 4 Wolkenweide, 5 an older region of the Guhmensie that ours give way to), after three ints: samples per side,
     * blocks per sample, the x and z of the first.
     */
    public static Uitkomst meet(BioModel mm, int mx, int mz, int straal, int stap, boolean oud, java.nio.file.Path kaart) {
        Waarden m = oud ? new Waarden() {
            @Override
            public double eDal(double x, double z) {
                return oudDal(mm, x, z);
            }

            @Override
            public double eWeide(double x, double z) {
                return oudWeide(mm, x, z);
            }
        } : new Waarden() {
            @Override
            public double eDal(double x, double z) {
                return mm.eDal(x, z);
            }

            @Override
            public double eWeide(double x, double z) {
                return mm.eWeide(x, z);
            }
        };
        int n = 2 * straal / stap + 1;
        byte[] wat = new byte[n * n];
        long nDal = 0, nMeer = 0, nWeide = 0;
        List<Double> gDal = new ArrayList<>(), gWeide = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int x = mx - straal + i * stap, z = mz - straal + j * stap;
                double e = m.eDal(x, z);
                byte w = 0;
                if (e > 0) {
                    // (the biome: DalTerrein.MEER_BIOME; the water: TRAP[3])
                    if (e >= DalTerrein.MEER_BIOME) {
                        nMeer++;
                    } else {
                        nDal++;
                    }
                    w |= DAL;
                    if (e >= DalTerrein.TRAP[3]) {
                        w |= MEER;
                    } else if (((i ^ j) & 15) == 0 && e > DalTerrein.RAND) {
                        gDal.add(Math.hypot(m.eDal(x + 16, z) - m.eDal(x - 16, z), m.eDal(x, z + 16) - m.eDal(x, z - 16)) / 32);
                    }
                } else {
                    double ew = m.eWeide(x, z);
                    if (ew > 0) {
                        nWeide++;
                        w |= WEIDE;
                        if (ew >= WolkTerrein.BINNEN) {
                            w |= WEIDE_BINNEN;
                        } else if (((i ^ j) & 3) == 0) {
                            gWeide.add(Math.hypot(m.eWeide(x + 8, z) - m.eWeide(x - 8, z), m.eWeide(x, z + 8) - m.eWeide(x, z - 8)) / 16);
                        }
                    }
                }
                wat[i + j * n] = w;
            }
        }
        if (kaart != null) {
            try (java.io.DataOutputStream o = new java.io.DataOutputStream(new java.io.BufferedOutputStream(java.nio.file.Files.newOutputStream(kaart), 1 << 20))) {
                o.writeInt(n);
                o.writeInt(stap);
                o.writeInt(mx - straal);
                o.writeInt(mz - straal);
                byte[] rij = new byte[n];
                for (int j = 0; j < n; j++) {
                    for (int i = 0; i < n; i++) {
                        int w = wat[i + j * n], x = mx - straal + i * stap, z = mz - straal + j * stap;
                        rij[i] = (byte) ((w & MEER) != 0 ? 3 : (w & DAL) != 0 ? (m.eDal(x, z) >= DalTerrein.MEER_BIOME ? 2 : 1) : (w & WEIDE) != 0 ? 4
                                : (i & 1) == 0 && (j & 1) == 0 && mm.masker(x, z) <= 0 ? 5 : 0);
                    }
                    o.write(rij);
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
        Uitkomst u = new Uitkomst();
        double alles = (double) n * n;
        u.deelDal = nDal / alles;
        u.deelMeer = nMeer / alles;
        u.deelWeide = nWeide / alles;
        u.steilDal = kwantielen(gDal);
        u.steilWeide = kwantielen(gWeide);
        stukken(wat, n, MEER, 0, mx - straal, mz - straal, stap, u.meren, null);
        stukken(wat, n, DAL, 0, mx - straal, mz - straal, stap, u.dalen, null);
        // (extra of a Wolkenweide: how many stacks of islands it has room for: the middles of WolkTerrein's stack cells that lie well inside it)
        stukken(wat, n, WEIDE, WEIDE_BINNEN, mx - straal, mz - straal, stap, u.weiden, m);
        return u;
    }

    private static double[] kwantielen(List<Double> v) {
        if (v.isEmpty()) {
            return new double[]{0, 0, 0};
        }
        v.sort(null);
        return new double[]{v.get(v.size() / 10), v.get(v.size() / 2), v.get(v.size() * 9 / 10)};
    }

    /** The two region values, as they are or as they were. */
    private interface Waarden {
        double eDal(double x, double z);

        double eWeide(double x, double z);
    }

    private static void stukken(byte[] wat, int n, int bit, int binnenBit, int x0, int z0, int stap, List<Stuk> uit, Waarden m) {
        boolean[] gezien = new boolean[n * n];
        int[] rij = new int[n * n];
        for (int start = 0; start < n * n; start++) {
            if ((wat[start] & bit) == 0 || gezien[start]) {
                continue;
            }
            int kop = 0, staart = 0;
            rij[staart++] = start;
            gezien[start] = true;
            int i0 = n, j0 = n, i1 = -1, j1 = -1;
            long aantal = 0;
            boolean rand = false;
            while (kop < staart) {
                int p = rij[kop++], i = p % n, j = p / n;
                aantal++;
                i0 = Math.min(i0, i);
                i1 = Math.max(i1, i);
                j0 = Math.min(j0, j);
                j1 = Math.max(j1, j);
                rand |= i == 0 || j == 0 || i == n - 1 || j == n - 1;
                if (i > 0 && (wat[p - 1] & bit) != 0 && !gezien[p - 1]) {
                    gezien[p - 1] = true;
                    rij[staart++] = p - 1;
                }
                if (i < n - 1 && (wat[p + 1] & bit) != 0 && !gezien[p + 1]) {
                    gezien[p + 1] = true;
                    rij[staart++] = p + 1;
                }
                if (j > 0 && (wat[p - n] & bit) != 0 && !gezien[p - n]) {
                    gezien[p - n] = true;
                    rij[staart++] = p - n;
                }
                if (j < n - 1 && (wat[p + n] & bit) != 0 && !gezien[p + n]) {
                    gezien[p + n] = true;
                    rij[staart++] = p + n;
                }
            }
            int bx0 = x0 + i0 * stap, bz0 = z0 + j0 * stap, bx1 = x0 + (i1 + 1) * stap, bz1 = z0 + (j1 + 1) * stap;
            int extra = 0;
            if (m != null) {
                int c = WolkTerrein.STAPEL_CEL;
                for (int cx = Math.floorDiv(bx0, c); cx <= Math.floorDiv(bx1, c); cx++) {
                    for (int cz = Math.floorDiv(bz0, c); cz <= Math.floorDiv(bz1, c); cz++) {
                        if (m.eWeide(cx * c + c / 2, cz * c + c / 2) >= WolkTerrein.BINNEN) {
                            // (a rough count: a neighbouring meadow's cell inside the same box would count too; they lie far apart)
                            extra++;
                        }
                    }
                }
            }
            uit.add(new Stuk(bx0, bz0, bx1, bz1, aantal * stap * stap, extra, rand));
        }
    }

    /**
     * How many of each {@code guhs:bio_plek} structure every region within the square gets, from the model: a
     * one-per-region structure where {@link RegioKeuze} chose it; any other at each chunk its structure set can start
     * in (this server's sets and seed) that has a spot of its kind. What two buildings on one spot or a biome border
     * would still refuse is not seen here. At most {@code maxDal} dals and {@code maxWeide} Wolkenweides, the nearest first.
     */
    public static String telling(BioModel m, int mx, int mz, int straal, int maxDal, int maxWeide) {
        List<BioRegio> regios = new ArrayList<>(BioRegio.bij(m, mx - straal, mz - straal, mx + straal, mz + straal));
        regios.sort(java.util.Comparator.comparingDouble((BioRegio r) -> Math.hypot(r.x - mx, r.z - mz)));
        List<Luchtruim.Inschrijving> alle = Luchtruim.alle();
        int[][][] verdeling = new int[2][alle.size()][5];
        long[][] som = new long[2][alle.size()];
        int[] aantal = new int[2];
        StringBuilder eerste = new StringBuilder();
        for (BioRegio g : regios) {
            int w = g.weide ? 1 : 0;
            if (aantal[w] >= (g.weide ? maxWeide : maxDal)) {
                continue;
            }
            aantal[w]++;
            StringBuilder regel = new StringBuilder();
            for (int i = 0; i < alle.size(); i++) {
                Luchtruim.Inschrijving in = alle.get(i);
                BioPlekStructure s = in.structuur();
                if (RegioKeuze.weide(s.soort()) != g.weide) {
                    continue;
                }
                int n = 0;
                if (s.perRegio().isPresent()) {
                    n = RegioKeuze.plekken(m, g, s).size();
                } else {
                    int sp = in.plaatsing().spacing(), reik = (int) g.buiten + 16;
                    for (int rx = Math.floorDiv(((int) g.x - reik) >> 4, sp); rx <= Math.floorDiv(((int) g.x + reik) >> 4, sp); rx++) {
                        for (int rz = Math.floorDiv(((int) g.z - reik) >> 4, sp); rz <= Math.floorDiv(((int) g.z + reik) >> 4, sp); rz++) {
                            net.minecraft.world.level.ChunkPos c = in.plaatsing().getPotentialStructureChunk(Luchtruim.seed(), rx * sp, rz * sp);
                            int px = c.getMiddleBlockX(), pz = c.getMiddleBlockZ();
                            if (m.regio(px, pz, g.weide) == g && BioPlekken.zoek(m, s.soort(), c.x(), c.z(), s.hoogte(), s.vlak()).isPresent()
                                    && (s.soort() != BioPlekken.Soort.LUCHT || RegioKeuze.heleWeide(m, px, pz, s.ruimte()))) {
                                n++;
                            }
                        }
                    }
                }
                verdeling[w][i][Math.min(n, 4)]++;
                som[w][i] += n;
                regel.append(' ').append(in.naam()).append(' ').append(n);
            }
            if (aantal[w] <= 6) {
                eerste.append(String.format(Locale.ROOT, "%n     %s:%s", g, regel));
            }
        }
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "buildings per region (%d dals, %d Wolkenweides; regions with none / 1 / 2 / 3 / 4 or more; mean):", aantal[0], aantal[1]));
        for (int w = 0; w < 2; w++) {
            for (int i = 0; i < alle.size(); i++) {
                BioPlekStructure s = alle.get(i).structuur();
                if (RegioKeuze.weide(s.soort()) != (w == 1) || aantal[w] == 0) {
                    continue;
                }
                int[] v = verdeling[w][i];
                sb.append(String.format(Locale.ROOT, "%n   %-22s %-12s %s  %3d / %3d / %3d / %3d / %3d   mean %.2f", alle.get(i).naam(), s.soort().getSerializedName(),
                        s.perRegio().map(k -> String.format(Locale.ROOT, "per_regio %.2f", k)).orElse("spacing " + alle.get(i).plaatsing().spacing() + "   "), v[0], v[1], v[2], v[3], v[4],
                        som[w][i] / (double) aantal[w]));
            }
        }
        return sb.append(String.format(Locale.ROOT, "%n   the nearest regions:")).append(eerste).toString();
    }

    /**
     * What floats above the Wolkenweides in the square, from the model: {meadows, stacks, islands in stacks, loose
     * islands, rocks, cloud banks, the highest island above its meadow, buildings in the air}. At most {@code max} meadows,
     * the nearest first.
     */
    public static int[] lucht(BioModel m, int mx, int mz, int straal, int max) {
        List<BioRegio> regios = new ArrayList<>(BioRegio.bij(m, mx - straal, mz - straal, mx + straal, mz + straal));
        regios.removeIf(r -> !r.weide);
        regios.sort(java.util.Comparator.comparingDouble((BioRegio r) -> Math.hypot(r.x - mx, r.z - mz)));
        int[] uit = new int[8];
        for (BioRegio g : regios.subList(0, Math.min(max, regios.size()))) {
            uit[0]++;
            int x0 = (int) (g.x - g.buiten), z0 = (int) (g.z - g.buiten), x1 = (int) (g.x + g.buiten), z1 = (int) (g.z + g.buiten);
            for (int cx = Math.floorDiv(x0, WolkTerrein.STAPEL_CEL); cx <= Math.floorDiv(x1, WolkTerrein.STAPEL_CEL); cx++) {
                for (int cz = Math.floorDiv(z0, WolkTerrein.STAPEL_CEL); cz <= Math.floorDiv(z1, WolkTerrein.STAPEL_CEL); cz++) {
                    WolkTerrein.Stapel s = WolkTerrein.stapel(m, cx, cz);
                    if (s.eilanden.isEmpty() || m.regio(s.x, s.z, true) != g) {
                        continue;
                    }
                    uit[1]++;
                    uit[2] += s.eilanden.size();
                    uit[6] = Math.max(uit[6], s.hoogste().top - WolkTerrein.grond(m, s.hoogste().x, s.hoogste().z));
                }
            }
            for (int cx = Math.floorDiv(x0, WolkTerrein.CEL); cx <= Math.floorDiv(x1, WolkTerrein.CEL); cx++) {
                for (int cz = Math.floorDiv(z0, WolkTerrein.CEL); cz <= Math.floorDiv(z1, WolkTerrein.CEL); cz++) {
                    for (WolkTerrein.Eiland e : WolkTerrein.cel(m, cx, cz)) {
                        if (m.regio(e.x, e.z, true) == g) {
                            uit[e.rots ? 4 : 3]++;
                        }
                    }
                }
            }
            for (int cx = Math.floorDiv(x0, WolkTerrein.WOLK_CEL); cx <= Math.floorDiv(x1, WolkTerrein.WOLK_CEL); cx++) {
                for (int cz = Math.floorDiv(z0, WolkTerrein.WOLK_CEL); cz <= Math.floorDiv(z1, WolkTerrein.WOLK_CEL); cz++) {
                    java.util.Set<Integer> banken = new java.util.HashSet<>();
                    for (WolkTerrein.Wolk w : WolkTerrein.wolken(m, cx, cz)) {
                        if (m.regio(w.x(), w.z(), true) == g) {
                            banken.add(w.bank());
                        }
                    }
                    uit[5] += banken.size();
                }
            }
            uit[7] += RegioKeuze.lucht(m, g).size();
        }
        return uit;
    }

    /** The regions that can reach into the square, the nearest first, each with the place chosen for every one-per-region building. */
    public static List<String> lijst(BioModel m, int mx, int mz, int straal) {
        List<BioRegio> regios = new ArrayList<>(BioRegio.bij(m, mx - straal, mz - straal, mx + straal, mz + straal));
        regios.sort(java.util.Comparator.comparingDouble((BioRegio r) -> Math.hypot(r.x - mx, r.z - mz)));
        List<String> uit = new ArrayList<>();
        for (BioRegio g : regios) {
            StringBuilder sb = new StringBuilder(g.toString()).append(':');
            for (Luchtruim.Inschrijving in : Luchtruim.alle()) {
                for (BioPlekken.Plek p : RegioKeuze.plekken(m, g, in.structuur())) {
                    sb.append(String.format(Locale.ROOT, " %s %d %d %d;", in.naam(), p.x(), p.y(), p.z()));
                }
            }
            uit.add(sb.toString());
        }
        return uit;
    }

    /** A table of sizes: how many pieces, and the spread of their longest and shortest side. */
    public static String tabel(String naam, List<Stuk> stukken, int[] grenzen, boolean metExtra) {
        List<Stuk> heel = new ArrayList<>();
        for (Stuk s : stukken) {
            if (!s.afgesneden()) {
                heel.add(s);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (heel.isEmpty()) {
            return naam + ": none";
        }
        int[] breed = heel.stream().mapToInt(Stuk::breed).sorted().toArray(), smal = heel.stream().mapToInt(Stuk::smal).sorted().toArray();
        long[] opp = heel.stream().mapToLong(Stuk::oppervlak).sorted().toArray();
        int k = breed.length;
        sb.append(String.format(Locale.ROOT, "%s: %d whole pieces. Longest side min %d / 10%% %d / median %d / 90%% %d / max %d; shortest side min %d / 10%% %d / median %d / 90%% %d / max %d; "
                        + "area median %d, mean %d", naam, k, breed[0], breed[k / 10], breed[k / 2], breed[k * 9 / 10], breed[k - 1], smal[0], smal[k / 10], smal[k / 2],
                smal[k * 9 / 10], smal[k - 1], opp[k / 2], java.util.Arrays.stream(opp).sum() / k));
        sb.append(String.format(Locale.ROOT, "%n     longest side, pieces per class:"));
        int vorige = 0;
        for (int g = 0; g <= grenzen.length; g++) {
            int tot = g < grenzen.length ? grenzen[g] : Integer.MAX_VALUE, van = vorige;
            long aantal = java.util.Arrays.stream(breed).filter(b -> b >= van && b < tot).count();
            sb.append(g < grenzen.length ? String.format(Locale.ROOT, " %d-%d: %d;", van, tot, aantal) : String.format(Locale.ROOT, " %d+: %d", van, aantal));
            vorige = tot;
        }
        if (metExtra) {
            int[] ex = heel.stream().mapToInt(Stuk::extra).sorted().toArray();
            sb.append(String.format(Locale.ROOT, "%n     room for stacks of islands (stack cells well inside): min %d / 10%% %d / median %d / 90%% %d / max %d; with 0: %d, 1-2: %d, 3-5: %d, 6+: %d",
                    ex[0], ex[k / 10], ex[k / 2], ex[k * 9 / 10], ex[k - 1], java.util.Arrays.stream(ex).filter(a -> a == 0).count(),
                    java.util.Arrays.stream(ex).filter(a -> a >= 1 && a <= 2).count(), java.util.Arrays.stream(ex).filter(a -> a >= 3 && a <= 5).count(),
                    java.util.Arrays.stream(ex).filter(a -> a >= 6).count()));
        }
        return sb.toString();
    }

    /** The whole report of one measurement. */
    public static String verslag(Uitkomst u, int straal, int stap) {
        return String.format(Locale.ROOT, "square of %d x %d blocks, every %d: Klaterdal %.2f%%, Bloesemmeertje %.2f%%, Wolkenweide %.2f%%"
                        + "%n   slope of the dal value on the terraces (per block; 10%% / median / 90%%): %.5f / %.5f / %.5f; of the meadow's rim: %.5f / %.5f / %.5f"
                        + "%n   %s%n   %s%n   %s",
                2 * straal, 2 * straal, stap, 100 * u.deelDal, 100 * u.deelMeer, 100 * u.deelWeide, u.steilDal[0], u.steilDal[1], u.steilDal[2], u.steilWeide[0],
                u.steilWeide[1], u.steilWeide[2], tabel("lakes", u.meren, new int[]{60, 120, 200, 350, 450, 700}, false),
                tabel("dals (valley + lake)", u.dalen, new int[]{300, 500, 700, 900, 1200, 1600}, false),
                tabel("Wolkenweides", u.weiden, new int[]{100, 200, 300, 400, 500, 700}, true));
    }

    private PlaatsingMeting() {
    }
}
