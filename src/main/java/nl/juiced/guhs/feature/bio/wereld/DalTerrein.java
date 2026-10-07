package nl.juiced.guhs.feature.bio.wereld;

import java.util.List;

/**
 * biomes3 wereld, the Klaterdal: the shape of the terraced valley (pure maths on {@link BioModel}; no blocks here).
 * <p>
 * The valley is the ring of a "dal" region around its lake. Going in (the value e of {@link BioModel#eDal} rising) the
 * land first blends from the Guhmensie's own terrain to the rim terrace ({@link #RAND}), then steps down
 * {@link #HOOGTE}[3] .. [0] at the thresholds {@link #TRAP} (a rock face of 7 blocks at each), and at TRAP[3] the lake of
 * {@link MeerTerrein} starts, one block below the valley floor. A small noise frays the edges ({@link #RAFEL}); where the
 * "knijp" noise is high the third terrace is pinched to a ledge, so two faces stand on top of each other: the tall
 * waterfall (14 blocks in two steps).
 * <p>
 * The river is the zero line of the noise {@link BioModel#R_RIVIER} inside the valley: {@link #RIVIER_BREED} blocks to
 * each side, one block of water at the banks and two in the middle, its water one block below the terrace. On the valley
 * floor it widens into a pond ({@link #VIJVER_BREED}) and it meets the lake at the lake's own level. At a terrace edge
 * the water of the upper bed falls onto the water of the lower bed (flag {@link Kaart#VAL}); an upper river column that
 * would spill onto DRY lower ground becomes a rock lip instead ({@link Kaart#LIP}), so the water always stays in its bed.
 * <p>
 * Owner after the kern: the Klaterdal polish agent (this file, {@link DalVulling}, tools/features/bio_wereld_dal.py).
 * The numbers between the markers may be tuned freely; {@link #HOOGTE}[0] must stay {@link MeerTerrein#WATER} + 1, and
 * {@link #TRAP}[3] / {@link #MEER_BIOME} decide the two biomes' shares (BioWereldGameTests.bioWereldAandeel).
 */
public final class DalTerrein {
    // <dal-terrein>
    /** e below this: the rim, where our terrain blends in. */
    public static final double RAND = 0.03;
    /** No river nearer to the edge than this (its neighbours must be ours, or it could leak). */
    public static final double RIVIER_VANAF = 0.042;
    /** e where terrace 3 ends, 2 ends, 1 ends, and the valley floor ends (the lake starts). */
    public static final double[] TRAP = {0.065, 0.10, 0.135, 0.17};
    /** The top block of terrace 0 (valley floor) .. 3 (rim). */
    public static final int[] HOOGTE = {50, 57, 64, 71};
    /** From this e on the biome is the Bloesemmeertje (a strip of shore before the water). */
    public static final double MEER_BIOME = 0.155;
    /** How much the small noise frays the terrace edges (in e). */
    public static final double RAFEL = 0.005;
    /** Half the width of the river, and what the pond on the valley floor adds. */
    public static final double RIVIER_BREED = 2.3, VIJVER_BREED = 5.0;
    /** The knijp noise (the detail noise, stretched) above this pinches terrace 2 to a ledge: a tall waterfall. */
    public static final double KNIJP_VANAF = 0.15;
    // </dal-terrein>

    static final int MUUR = 100000;

    /** The terrace of a (frayed) e: 3 .. 0, or -1 for the lake. */
    public static int terras(double e, double knijp) {
        double e2 = TRAP[1] + (TRAP[0] + 0.005 - TRAP[1]) * knijp;
        if (e < TRAP[0]) {
            return 3;
        }
        if (e < e2) {
            return 2;
        }
        if (e < TRAP[2]) {
            return 1;
        }
        return e < TRAP[3] ? 0 : -1;
    }

    /** The frayed e of a column (what the terraces and the lake are cut from). */
    public static double rafel(BioModel m, int x, int z, double e) {
        return e + RAFEL * m.ruis(BioModel.R_DETAIL, x, z);
    }

    public static double knijp(BioModel m, int x, int z) {
        return BioModel.zacht((m.ruis(BioModel.R_DETAIL, x * 0.15 + 300, z * 0.15 - 700) - KNIJP_VANAF) / 0.15);
    }

    /** Fills the dal columns of a chunk map; false when the chunk has none. */
    static boolean vul(BioModel m, Kaart k) {
        final int marge = BioModel.MARGE, n = 16 + 2 * marge;
        int x0 = (k.cx << 4) - marge, z0 = (k.cz << 4) - marge;
        double[] e = new double[n * n];
        boolean iets = false;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                double v = m.eDal(x0 + i, z0 + j);
                e[i + j * n] = v;
                iets |= v > 0;
            }
        }
        if (!iets) {
            return false;
        }
        double[] w = new double[n * n];
        int[] h = new int[n * n], wat = new int[n * n];
        byte[] ter = new byte[n * n], vl = new byte[n * n];
        List<MeerTerrein.Eiland> eilanden = null;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = i + j * n, x = x0 + i, z = z0 + j;
                wat[idx] = Kaart.GEEN;
                ter[idx] = -1;
                if (e[idx] <= 0) {
                    h[idx] = MUUR;
                    continue;
                }
                if (e[idx] < RAND) {
                    h[idx] = HOOGTE[3];
                    ter[idx] = 3;
                    continue;
                }
                double ew = w[idx] = rafel(m, x, z, e[idx]);
                int t = terras(ew, knijp(m, x, z));
                if (t < 0) {
                    if (eilanden == null) {
                        eilanden = MeerTerrein.bij(m, x0, z0, x0 + n, z0 + n);
                    }
                    MeerTerrein.kolom(m, x, z, ew - TRAP[3], eilanden, h, wat, vl, idx);
                    continue;
                }
                int top = HOOGTE[t];
                ter[idx] = (byte) t;
                h[idx] = top;
                if (e[idx] >= RIVIER_VANAF) {
                    double r = m.ruis(BioModel.R_RIVIER, x, z);
                    double gx = m.ruis(BioModel.R_RIVIER, x + 1, z) - r, gz = m.ruis(BioModel.R_RIVIER, x, z + 1) - r;
                    double g = Math.sqrt(gx * gx + gz * gz);
                    if (g > 0.0012) {
                        double d = Math.abs(r) / g;
                        double breed = RIVIER_BREED + (t == 0 ? VIJVER_BREED * BioModel.zacht((ew - TRAP[2] - 0.006) / 0.02) : 0);
                        if (d < breed) {
                            vl[idx] |= Kaart.RIVIER;
                            wat[idx] = top - 1;
                            h[idx] = d < breed - 1.3 ? top - 3 : top - 2;
                        }
                    }
                }
            }
        }
        // the lips: river water next to lower DRY ground would run out of its bed; such a column becomes rock up to the
        // water's level. A new lip can make the column above it leak in turn (two edges close together): a few rounds.
        int[] buren = {-1, 1, -n, n};
        for (int ronde = 0; ronde < marge - 1; ronde++) {
            boolean[] lip = new boolean[n * n];
            boolean nieuw = false;
            for (int j = 1; j < n - 1; j++) {
                for (int i = 1; i < n - 1; i++) {
                    int idx = i + j * n;
                    if ((vl[idx] & Kaart.RIVIER) == 0 || wat[idx] == Kaart.GEEN) {
                        continue;
                    }
                    for (int b : buren) {
                        if (wat[idx + b] == Kaart.GEEN && h[idx + b] < wat[idx]) {
                            lip[idx] = nieuw = true;
                            break;
                        }
                    }
                }
            }
            if (!nieuw) {
                break;
            }
            for (int idx = 0; idx < n * n; idx++) {
                if (lip[idx]) {
                    h[idx] = wat[idx];
                    wat[idx] = Kaart.GEEN;
                    vl[idx] |= Kaart.LIP;
                }
            }
        }
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++) {
                int idx = (i + marge) + (j + marge) * n, o = i | j << 4;
                if (e[idx] <= 0) {
                    continue;
                }
                k.meng[o] = (float) BioModel.zacht(e[idx] / RAND);
                k.soort[o] = e[idx] >= RAND && w[idx] >= MEER_BIOME ? Kaart.MEER : Kaart.DAL;
                k.hoogte[o] = h[idx];
                k.water[o] = wat[idx];
                k.terras[o] = ter[idx];
                byte v = vl[idx];
                if ((v & Kaart.RIVIER) != 0 && wat[idx] != Kaart.GEEN) {
                    for (int b : buren) {
                        if (wat[idx + b] != Kaart.GEEN && wat[idx + b] < wat[idx]) {
                            v |= Kaart.VAL;
                        }
                    }
                }
                k.vlag[o] = v;
            }
        }
        return true;
    }

    private DalTerrein() {
    }
}
