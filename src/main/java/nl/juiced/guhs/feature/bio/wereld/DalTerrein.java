package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;

/**
 * biomes3 wereld, the Klaterdal: the shape of the terraced valley (pure maths on {@link BioModel}; no blocks here).
 * <p>
 * The valley is the ring of a "dal" region around its lake. Going in (the value e of {@link BioModel#eDal} rising) the
 * land first blends from the Guhmensie's own terrain to the rim terrace ({@link #RAND}), then steps down
 * {@link #HOOGTE}[3] .. [0] with a rock face of 7 blocks at each edge, and at {@link #TRAP}[3] the lake of
 * {@link MeerTerrein} starts, one block below the valley floor.
 * <p>
 * <b>Broad terraces, or fewer of them.</b> How wide a terrace gets follows from how steeply e rises ({@link #steilte},
 * measured over 32 blocks so it changes slowly). Where the valley has room, the four terraces are each
 * {@link #TRAP_E} of e wide (18-45 blocks). Where it is squeezed (against an older region, or the dal noise itself is
 * steep) the terraces are not made narrower, the valley is made SHALLOWER: first the rim terrace goes ({@link #KRAP_A}:
 * its edge moves out through the rim's blend, so the valley starts one step lower and the three terraces that are left
 * share the room), then terrace 2 as well ({@link #KRAP_B}: two broad levels and one face). The squeezed side of a dal is
 * so a gentle, shallow piece of the same valley instead of a steep narrow stair. Here and there the "knijp" noise
 * pinches the middle terrace to a rock ledge: the two faces around it are one wall of 14, the place of the tall
 * waterfall (in a roomy valley now and then; in a squeezed one more often, and there it trades the shallowest shape, two
 * levels 7 apart, for a rim of 64 over one white wall). An edge is always the level line of a smooth function, and the
 * distance to it in BLOCKS ({@link Vorm#s}) is what everything near an edge is cut from.
 * <p>
 * <b>The water.</b> A river is the zero line of the noise {@link BioModel#R_RIVIER} ({@link #RIVIER_BREED} to each
 * side), a brook that of the same noise far away ({@link #BEEK_BREED}); both start thin (a spring) just inside the rim,
 * widen into a pond on the valley floor and meet the lake at its own level. Koi pools ({@link Poel}) hang on the river
 * like beads. The water's level is the rim's minus what each edge has dropped so far ({@link Vorm#val}): the bed is cut
 * BACK into the upper terrace as a gully that steps down, so a face of 7 is a low cascade (2 + 2 + 3: {@link #ZACHT}) or
 * a cascade with a real little fall and a plunge pool at its foot (2 + 5: {@link #STEVIG}), and where a terrace is only a
 * ledge the river takes the wall in one tall waterfall (5 + 5 + 4: {@link #HOOG}) with a deep plunge pool. Steps are
 * never one block (a one-block step makes new water sources and the lower river would fill up).
 * The water cannot leave its bed: a water column next to lower DRY ground becomes a rock lip ({@link Kaart#LIP}), in a
 * few rounds; where the water of the upper bed lies next to lower WATER it falls ({@link Kaart#VAL}).
 * <p>
 * <b>The rock.</b> Faces have a rounded foot and shoulder, boulders lie at their feet and flank the falls
 * ({@link Kei}), stepping stones cross the river in calm stretches ({@link Stap}), and beside every cascade of the river
 * natural steps climb the face ({@link #TREDE}). All of that is on the river's LEFT bank (the side where its noise is
 * negative) or away from the water: the right bank stays plain flat ground, the clear building spots, and the spots of
 * {@link BioPlekken} (which only accept columns without a flag) stay plentiful there.
 * <p>
 * Owner: the Klaterdal polish agent (this file, {@link DalVulling}, {@link DalPlanten}, tools/features/bio_wereld_dal.py).
 * The numbers between the markers may be tuned freely; {@link #HOOGTE}[0] must stay {@link MeerTerrein#WATER} + 1, and
 * {@link #TRAP}[3] / {@link #MEER_BIOME} decide the two biomes' shares (BioWereldGameTests.bioWereldAandeel).
 */
public final class DalTerrein {
    // <dal-terrein>
    /** e below this: the rim, where our terrain blends in. */
    public static final double RAND = 0.03;
    /** A river starts this many blocks inside the rim's blend (as a spring), and is at full width this many further. */
    public static final double BRON_VANAF = 2.0, BRON_OVER = 3.0;
    /** e where terrace 3 ends, 2 ends, 1 ends (in a roomy valley), and the valley floor ends (the lake starts). */
    public static final double[] TRAP = {0.05, 0.075, 0.10, 0.125};
    /** The top block of terrace 0 (valley floor) .. 3 (rim). */
    public static final int[] HOOGTE = {50, 57, 64, 71};
    /** From this e on the biome is the Bloesemmeertje (a strip of shore before the water). */
    public static final double MEER_BIOME = 0.112;
    /** How much the small noise frays the lake's edge (in e), and at most the terrace edges (in blocks). */
    public static final double RAFEL = 0.005, RAFEL_BLOK = 3.0;
    /** A roomy terrace is this much e wide. */
    public static final double TRAP_E = 0.025;
    /** The rim terrace goes between these natural terrace widths (blocks; 0.025 of e), terrace 2 between the next two. */
    public static final double[] KRAP_A = {19, 14}, KRAP_B = {13, 9.5};
    /** Where a terrace is gone its edge lies at this e (outside the dal, beyond every fray). */
    public static final double WEG = -0.012;
    /** A terrace thinner than this is a ledge: the river takes the faces around it as one tall waterfall. */
    public static final double RICHEL = 6.0;
    /** Half the width of the river and of a brook, and what the pond on the valley floor adds. */
    public static final double RIVIER_BREED = 3.0, BEEK_BREED = 1.6, VIJVER_BREED = 5.0;
    /** The knijp noise (the detail noise, stretched) above this pinches the middle terrace to a ledge: a tall waterfall. In a squeezed stretch the bar is this much lower. */
    public static final double KNIJP_VANAF = 0.18, KNIJP_KRAP = 0.30;
    /** The cascades: {blocks before the edge, how far the water has dropped from there}. Never a step of one. */
    static final double[][] ZACHT = {{-8, 2}, {-4, 4}, {0, 7}}, STEVIG = {{-4, 2}, {0, 7}}, HOOG = {{-3.6, 5}, {-1.8, 10}, {0, 14}};
    /** The type noise above these gives a cascade a real fall at its foot (edges 0 and 1; the edge to the valley floor). */
    public static final double STEVIG_VANAF = -0.05, STEVIG_VLOER = 0.0;
    /** Koi pools: one try per cell of this size, and how often it is taken. */
    public static final int POEL_CEL = 24;
    public static final double POEL_KANS = 0.85;
    /** Stepping stones: one try per cell. */
    public static final int STAP_CEL = 16;
    public static final double STAP_KANS = 0.7;
    /** Boulders: one try per cell; the chance on open terrace, at the foot of a face, beside a fall. */
    public static final int KEI_CEL = 8;
    public static final double KEI_LOS = 0.035, KEI_VOET = 0.28, KEI_VAL = 0.8;
    /** The rounded foot of a face bulges out at most this far (blocks). */
    public static final double BOL = 2.6;
    /** The right bank is kept plain this far from the water (the building side). */
    public static final double VRIJ = 9.0;
    /** The natural steps beside a cascade: from .. to this far from the left bank. */
    public static final double TREDE_VAN = 3.5, TREDE_TOT = 6.5;
    // </dal-terrein>

    /** Flag in {@link Kaart#vlag} (a bit of ours beside the kern's): the column is sculpted (not plain terrace or plain river). */
    public static final short VORM = Kaart.DAL_VORM; // biomes3 merge: was 32, the lake's STEEN
    /** Flag: a natural step beside a cascade ({@link DalVulling} lays a stair block on it). */
    public static final short TREDE = Kaart.DAL_TREDE; // biomes3 merge: was 64, the lake's STRAND

    static final int MUUR = 100000;
    /** Columns around a chunk that are worked out too (the gradients, three rounds of lips, the fall flags). */
    static final int MARGE = 5;
    private static final int LIP_RONDES = 3;
    /** The lattice of {@link #steilte}. */
    private static final int ROOSTER = 16;
    private static final int SOORT_STEILTE = 20, SOORT_POEL = 21, SOORT_STAP = 22, SOORT_KEI = 23;

    /** What the shape rules say about one column (everything except "is it water", which needs the river's distance). */
    public static final class Vorm {
        /** Inside the terraces proper (not the rim's blend, not the lake, not outside)? */
        public boolean kern;
        /** The terrace 3 .. 0 (also in the rim's blend: the terrace the blend ends in), or -1. */
        public int t;
        /** How steep e is here (per block), the frayed e, and the detail noise it is frayed with. */
        public double g, ew, det;
        /** The e where the rim terrace, terrace 2 and terrace 1 end here. */
        public double t0, t1, t2;
        /** Blocks past edge 0 (rim to terrace 2), 1, 2 (to the valley floor); negative before it. */
        public final double[] s = new double[3];
        /** The distance to the nearest edge (signed like s). */
        public double sn;
        /** How far the water has dropped below the rim's level here, and its top water block. */
        public int val, peil;
        /** The nearest edge has a real fall at its foot (a plunge pool); it is the tall waterfall. */
        public boolean echt, hoog;
        /** 1 where the river may be at full width; less at its spring just inside the rim (0: no river). */
        public double bron;
        /** How much the river fans out near a cascade, and into the plunge pool; the pond of the valley floor. */
        public double waaier, kom, vijver;
        /** How far the shoulder of the face below is rounded off (blocks). */
        public double schouder;

        /** Half the width of the river here, and of a brook. */
        public double rivier() {
            return (RIVIER_BREED + 1.3 * waaier + (echt ? (hoog ? 2.6 : 1.6) : 0.5) * kom) * bron + vijver;
        }

        public double beek() {
            return (BEEK_BREED + 0.5 * waaier + (echt ? 1.2 : 0.4) * kom) * bron + 0.35 * vijver;
        }
    }

    private static double trap(double s, double[][] stappen) {
        double uit = 0;
        for (double[] st : stappen) {
            if (s >= st[0]) {
                uit = st[1];
            }
        }
        return uit;
    }

    /** The frayed e the lake's edge and the two biomes are cut from (the kern's; the terrace edges use {@link Vorm#ew}). */
    public static double rafel(BioModel m, int x, int z, double e) {
        return e + RAFEL * m.ruis(BioModel.R_DETAIL, x, z);
    }

    /** |grad e| at a lattice point, over 32 blocks (cached in the model). */
    private static double knoop(BioModel m, int rx, int rz) {
        long sleutel = BioModel.sleutel(SOORT_STEILTE, rx, rz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return (Double) bekend;
        }
        double x = rx * ROOSTER, z = rz * ROOSTER;
        double gx = m.eDal(x + ROOSTER, z) - m.eDal(x - ROOSTER, z), gz = m.eDal(x, z + ROOSTER) - m.eDal(x, z - ROOSTER);
        double g = Math.max(0.0004, Math.sqrt(gx * gx + gz * gz) / (2.0 * ROOSTER));
        bewaar(m, sleutel, g);
        return g;
    }

    private static void bewaar(BioModel m, long sleutel, Object wat) {
        if (m.cellen.size() > 60000) {
            m.cellen.clear();
        }
        m.cellen.put(sleutel, wat);
    }

    /** How steeply e rises at (x, z), per block: smooth (bilinear between lattice points 16 apart, each measured over 32). */
    public static double steilte(BioModel m, double x, double z) {
        int rx = (int) Math.floor(x / ROOSTER), rz = (int) Math.floor(z / ROOSTER);
        double tx = x / ROOSTER - rx, tz = z / ROOSTER - rz;
        return (knoop(m, rx, rz) * (1 - tx) + knoop(m, rx + 1, rz) * tx) * (1 - tz) + (knoop(m, rx, rz + 1) * (1 - tx) + knoop(m, rx + 1, rz + 1) * tx) * tz;
    }

    /** The shape rules at one column, given its e and steepness. */
    public static void vorm(BioModel m, int x, int z, double e, double g, Vorm v) {
        v.g = g;
        v.kern = false;
        v.t = -1;
        if (e <= 0) {
            return;
        }
        double det = v.det = m.ruis(BioModel.R_DETAIL, x, z);
        if (e >= RAND && e + RAFEL * det >= TRAP[3]) {
            return;
        }
        // how many terraces there is room for: qA = the rim terrace is gone, qB = terrace 2 as well
        double breed = TRAP_E / g;
        double qA = BioModel.zacht((KRAP_A[0] - breed) / (KRAP_A[0] - KRAP_A[1])), qB = BioModel.zacht((KRAP_B[0] - breed) / (KRAP_B[0] - KRAP_B[1]));
        double ruim = TRAP[3] - RAND;
        double t0 = v.t0 = TRAP[0] + (WEG - TRAP[0]) * qA;
        // the knijp: where it is high the middle terrace is only a rock ledge, the place of the tall waterfall. In a squeezed
        // stretch it is high more often, and there it keeps terrace 2 (a rim of 64 over one wall of 14) where the valley
        // would else be at its shallowest
        double knijp = BioModel.zacht((m.ruis(BioModel.R_DETAIL, x * 0.15 + 300, z * 0.15 - 700) - KNIJP_VANAF + KNIJP_KRAP * qB) / 0.15);
        double qBe = qB * (1 - knijp);
        double t1 = TRAP[1] + (RAND + ruim / 3 - TRAP[1]) * qA;
        t1 += (WEG - t1) * qBe;
        double t2 = TRAP[2] + (RAND + ruim * 2 / 3 - TRAP[2]) * qA;
        t2 += (RAND + ruim / 2 - t2) * qBe;
        if (knijp > 0) {
            double richel = Math.max(0, Math.min(3.5, 1.2 + 3.0 * m.ruis(BioModel.R_DETAIL, x * 0.3 + 5000, z * 0.3 + 100))) * g;
            // (all four terraces: terrace 2 is the ledge; the rim gone: terrace 1 is)
            t1 += (t0 + richel - t1) * knijp * (1 - qA);
            t2 += (t1 + richel - t2) * knijp * qA;
        }
        v.t1 = t1;
        v.t2 = t2;
        double ew = v.ew = e + Math.min(RAFEL, RAFEL_BLOK * g) * det;
        int t = v.t = ew < t0 ? 3 : ew < t1 ? 2 : ew < t2 ? 1 : 0;
        if (e < RAND) {
            // the rim's blend: it ends in the terrace that is the rim here
            v.t = Math.max(1, t);
            return;
        }
        v.kern = true;
        double s0 = v.s[0] = (ew - t0) / g, s1 = v.s[1] = (ew - t1) / g, s2 = v.s[2] = (ew - t2) / g;
        // the river's level: every edge drops it; an edge next to a ledge is taken together with the next one
        double d07 = m.ruis(BioModel.R_DETAIL, x * 0.07 + 77, z * 0.07 - 33);
        boolean stevig = d07 > STEVIG_VANAF, stevigVloer = d07 > STEVIG_VLOER;
        boolean smal1 = (t2 - t1) / g < RICHEL, smal2 = !smal1 && (t1 - t0) / g < RICHEL;
        double c0 = trap(s0, stevig ? STEVIG : ZACHT), c1 = trap(s1, stevig ? STEVIG : ZACHT), c2 = trap(s2, stevigVloer ? STEVIG : ZACHT);
        double val = smal1 ? c0 + trap(s2, HOOG) : smal2 ? trap(s1, HOOG) + c2 : c0 + c1 + c2;
        v.val = (int) val;
        v.peil = HOOGTE[3] - 1 - v.val;
        int dichtst = Math.abs(s0) < Math.abs(s1) ? 0 : 1;
        if (Math.abs(s2) < Math.abs(v.s[dichtst])) {
            dichtst = 2;
        }
        double sn = v.sn = v.s[dichtst];
        v.hoog = smal2 && dichtst == 1 || smal1 && dichtst == 2;
        v.echt = v.hoog || (dichtst == 2 ? stevigVloer : stevig) && !(smal2 && dichtst == 0) && !(smal1 && dichtst == 1);
        v.waaier = BioModel.zacht((sn + 12) / 4) * (1 - BioModel.zacht((sn - 6) / 4));
        v.kom = BioModel.zacht((sn + 0.5) / 1.0) * (1 - BioModel.zacht((sn - 4) / 3.5));
        v.bron = BioModel.zacht((e - RAND - BRON_VANAF * g) / (BRON_OVER * g));
        v.vijver = t == 0 ? VIJVER_BREED * BioModel.zacht((ew - t2 - 0.006) / 0.02) : 0;
        v.schouder = Math.max(0, Math.min(1.6, 0.4 + 2.4 * det));
    }

    /** The shape rules at any column (for the things that hang on a point: pools, stones, boulders; tests and commands). */
    public static Vorm bij(BioModel m, int x, int z) {
        Vorm v = new Vorm();
        vorm(m, x, z, m.eDal(x, z), steilte(m, x, z), v);
        return v;
    }

    /** The signed distance in blocks to a zero line of the river noise (ox, oz: 0 for the river, {@link #BEEK_X} for the brook), and its direction. */
    private static double[] lijn(BioModel m, double x, double z, double ox, double oz) {
        double r = m.ruis(BioModel.R_RIVIER, x + ox, z + oz);
        double gx = (m.ruis(BioModel.R_RIVIER, x + ox + 1, z + oz) - m.ruis(BioModel.R_RIVIER, x + ox - 1, z + oz)) / 2;
        double gz = (m.ruis(BioModel.R_RIVIER, x + ox, z + oz + 1) - m.ruis(BioModel.R_RIVIER, x + ox, z + oz - 1)) / 2;
        double g = Math.sqrt(gx * gx + gz * gz);
        if (g <= 0.0012) {
            return new double[]{99, 1, 0};
        }
        return new double[]{r / g, gx / g, gz / g};
    }

    /** Where the brook's zero lines are read in the river noise. */
    static final double BEEK_X = 7000, BEEK_Z = -3000;

    // --- the things on a point -------------------------------------------------------------------------------------------------
    /** A koi pool beside the river: its middle, radius and the two phases of its irregular outline. */
    public record Poel(double x, double z, double straal, double f1, double f2) {
        double rand(double dx, double dz) {
            double hoek = Math.atan2(dz, dx);
            return straal * (1 + 0.15 * Math.sin(2 * hoek + f1) + 0.10 * Math.sin(3 * hoek + f2)) - Math.sqrt(dx * dx + dz * dz);
        }
    }

    /** A row of stepping stones across the river: a point on its middle line and the direction across. */
    public record Stap(double x, double z, double nx, double nz) {
    }

    /** A boulder: middle, radius, height above the terrace it lies on. */
    public record Kei(int x, int z, double straal, int hoog, int terras) {
    }

    /** Is this column clear of every cascade: at least na blocks past each edge above it, at least voor before each edge below? */
    static boolean rustig(Vorm v, double na, double voor) {
        for (double s : v.s) {
            if (s > -voor && s < na) {
                return false;
            }
        }
        return true;
    }

    /** The pool of a grid cell, or null (cached). */
    static Poel poel(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_POEL, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return bekend instanceof Poel p ? p : null;
        }
        Poel uit = null;
        long h = m.hash(cx, cz, 5201);
        if (BioModel.kans(h, 0) < POEL_KANS) {
            double px = cx * POEL_CEL + BioModel.kans(h, 1) * POEL_CEL, pz = cz * POEL_CEL + BioModel.kans(h, 2) * POEL_CEL;
            double[] l = lijn(m, px, pz, 0, 0);
            if (Math.abs(l[0]) <= 14) {
                double straal = 2.8 + 1.6 * BioModel.kans(h, 3);
                // beside the river, on the side the try fell on, just touching it
                double schuif = l[0] - Math.signum(l[0] == 0 ? 1 : l[0]) * (straal - 0.3);
                px -= l[1] * schuif;
                pz -= l[2] * schuif;
                Vorm v = bij(m, (int) Math.round(px), (int) Math.round(pz));
                if (v.kern && v.bron > 0.95 && v.vijver < 2.5 && rustig(v, straal + 1, straal + 8.5)) {
                    uit = new Poel(px, pz, straal, BioModel.kans(h, 4) * 6.283, BioModel.kans(h, 5) * 6.283);
                }
            }
        }
        bewaar(m, sleutel, uit == null ? Boolean.FALSE : uit);
        return uit;
    }

    /** The stepping stones of a grid cell, or null (cached). */
    static Stap stap(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_STAP, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return bekend instanceof Stap s ? s : null;
        }
        Stap uit = null;
        long h = m.hash(cx, cz, 5301);
        if (BioModel.kans(h, 0) < STAP_KANS) {
            double px = cx * STAP_CEL + BioModel.kans(h, 1) * STAP_CEL, pz = cz * STAP_CEL + BioModel.kans(h, 2) * STAP_CEL;
            double[] l = lijn(m, px, pz, 0, 0);
            if (Math.abs(l[0]) <= 10) {
                px -= l[1] * l[0];
                pz -= l[2] * l[0];
                l = lijn(m, px, pz, 0, 0);
                Vorm v = bij(m, (int) Math.round(px), (int) Math.round(pz));
                if (Math.abs(l[0]) < 1.5 && v.kern && v.bron > 0.95 && v.vijver < 0.3 && rustig(v, 5.5, 9)) {
                    uit = new Stap(px, pz, l[1], l[2]);
                }
            }
        }
        bewaar(m, sleutel, uit == null ? Boolean.FALSE : uit);
        return uit;
    }

    /** The boulder of a grid cell, or null (cached). */
    static Kei kei(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_KEI, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return bekend instanceof Kei k ? k : null;
        }
        Kei uit = null;
        long h = m.hash(cx, cz, 5401);
        int px = cx * KEI_CEL + (int) (BioModel.kans(h, 1) * KEI_CEL), pz = cz * KEI_CEL + (int) (BioModel.kans(h, 2) * KEI_CEL);
        double e = m.eDal(px, pz);
        if (e >= RAND + 0.004 && e < TRAP[3]) {
            Vorm v = bij(m, px, pz);
            if (v.kern) {
                double[] r = lijn(m, px, pz, 0, 0), b = lijn(m, px, pz, BEEK_X, BEEK_Z);
                double naast = Math.abs(r[0]) - v.rivier(), naastBeek = Math.abs(b[0]) - v.beek();
                boolean stroomt = v.bron > 0.9;
                double kans = KEI_LOS;
                if (v.sn >= 0.5 && v.sn < 6) {
                    kans = KEI_VOET;
                }
                boolean flank = stroomt && r[0] < 0 && naast < 3.0 && v.sn > -7 && v.sn < 7;
                if (flank) {
                    kans = KEI_VAL;
                }
                boolean vrij = stroomt && (r[0] > 0 && naast < VRIJ || naastBeek < 4 || r[0] < 0 && naast >= 3.0 && naast < TREDE_TOT + 1.5 && v.sn > -16 && v.sn < 3);
                if (naast > 0.8 && naastBeek > 0.8 && !vrij && BioModel.kans(h, 0) < kans) {
                    uit = new Kei(px, pz, 1.4 + 1.5 * BioModel.kans(h, 3), 1 + (int) (BioModel.kans(h, 4) * (flank ? 3.4 : 2.5)), v.t);
                }
            }
        }
        bewaar(m, sleutel, uit == null ? Boolean.FALSE : uit);
        return uit;
    }

    /** How often the model worked out a chunk map with dal columns, and the nanoseconds that took (dev command "kosten"). */
    static final java.util.concurrent.atomic.LongAdder KAARTEN = new java.util.concurrent.atomic.LongAdder(), KAART_NS = new java.util.concurrent.atomic.LongAdder();

    /** Fills the dal columns of a chunk map; false when the chunk has none. */
    static boolean vul(BioModel m, Kaart k) {
        long t0 = System.nanoTime();
        boolean iets = vulKaart(m, k);
        if (iets) {
            KAARTEN.increment();
            KAART_NS.add(System.nanoTime() - t0);
        }
        return iets;
    }

    private static boolean vulKaart(BioModel m, Kaart k) {
        final int marge = MARGE, n = 16 + 2 * marge;
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
        // how steep: the lattice points around the grid, bilinear
        int r0x = Math.floorDiv(x0, ROOSTER), r0z = Math.floorDiv(z0, ROOSTER);
        int rn = Math.floorDiv(x0 + n - 1, ROOSTER) - r0x + 2, rm = Math.floorDiv(z0 + n - 1, ROOSTER) - r0z + 2;
        double[] knopen = new double[rn * rm];
        boolean[] knoopBekend = new boolean[rn * rm];
        // the two water noises on the grid (their gradients come from the grid itself)
        double[] rv = new double[n * n], bk = new double[n * n];
        boolean[] natKan = new boolean[n * n];
        for (int idx = 0; idx < n * n; idx++) {
            if (e[idx] >= RAND - 0.004) {
                int x = x0 + idx % n, z = z0 + idx / n;
                rv[idx] = m.ruis(BioModel.R_RIVIER, x, z);
                bk[idx] = m.ruis(BioModel.R_RIVIER, x + BEEK_X, z + BEEK_Z);
                natKan[idx] = true;
            }
        }
        double[] w = new double[n * n];
        int[] h = new int[n * n], wat = new int[n * n];
        byte[] ter = new byte[n * n];
        short[] vl = new short[n * n]; // biomes3 merge: flags are wider than a byte
        List<MeerTerrein.Eiland> eilanden = null;
        List<Poel> poelen = null;
        List<Stap> stappen = null;
        List<Kei> keien = null;
        Vorm v = new Vorm();
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = i + j * n, x = x0 + i, z = z0 + j;
                wat[idx] = Kaart.GEEN;
                ter[idx] = -1;
                if (e[idx] <= 0) {
                    h[idx] = MUUR;
                    continue;
                }
                // (bilinear steepness)
                int kx = Math.floorDiv(x, ROOSTER) - r0x, kz = Math.floorDiv(z, ROOSTER) - r0z;
                for (int a = 0; a < 4; a++) {
                    int ki = kx + (a & 1) + (kz + (a >> 1)) * rn;
                    if (!knoopBekend[ki]) {
                        knopen[ki] = knoop(m, r0x + kx + (a & 1), r0z + kz + (a >> 1));
                        knoopBekend[ki] = true;
                    }
                }
                double tx = x / (double) ROOSTER - Math.floorDiv(x, ROOSTER), tz = z / (double) ROOSTER - Math.floorDiv(z, ROOSTER);
                double g = (knopen[kx + kz * rn] * (1 - tx) + knopen[kx + 1 + kz * rn] * tx) * (1 - tz)
                        + (knopen[kx + (kz + 1) * rn] * (1 - tx) + knopen[kx + 1 + (kz + 1) * rn] * tx) * tz;
                vorm(m, x, z, e[idx], g, v);
                if (e[idx] < RAND) {
                    // the rim's blend: towards the height of the terrace that is the rim here
                    h[idx] = HOOGTE[v.t];
                    ter[idx] = (byte) v.t;
                    continue;
                }
                w[idx] = e[idx] + RAFEL * v.det;
                if (!v.kern) {
                    if (eilanden == null) {
                        eilanden = MeerTerrein.bij(m, x0, z0, x0 + n, z0 + n);
                    }
                    MeerTerrein.kolom(m, x, z, w[idx] - TRAP[3], eilanden, h, wat, vl, idx);
                    continue;
                }
                int t = v.t, top = HOOGTE[t];
                ter[idx] = (byte) t;
                int hoogte = top;
                short vlag = 0;
                // the water lines: distance to the river and to the brook (central differences on the grid)
                double naast = 99, naastBeek = 99, rnx = 1, rnz = 0;
                boolean links = false, stroomt = v.bron > 0.35;
                if (i > 0 && j > 0 && i < n - 1 && j < n - 1 && natKan[idx] && natKan[idx - 1] && natKan[idx + 1] && natKan[idx - n] && natKan[idx + n]) {
                    double gx = (rv[idx + 1] - rv[idx - 1]) / 2, gz = (rv[idx + n] - rv[idx - n]) / 2, gg = Math.sqrt(gx * gx + gz * gz);
                    if (gg > 0.0012) {
                        naast = Math.abs(rv[idx]) / gg - v.rivier();
                        rnx = gx / gg;
                        rnz = gz / gg;
                        links = rv[idx] < 0;
                    }
                    gx = (bk[idx + 1] - bk[idx - 1]) / 2;
                    gz = (bk[idx + n] - bk[idx - n]) / 2;
                    gg = Math.sqrt(gx * gx + gz * gz);
                    if (gg > 0.0012) {
                        naastBeek = Math.abs(bk[idx]) / gg - v.beek();
                    }
                }
                boolean rivier = stroomt && naast < 0 && v.rivier() > 0.9, beek = stroomt && naastBeek < 0 && v.beek() > 0.7;
                double inWater = Math.max(rivier ? -naast : 0, beek ? -naastBeek : 0);
                if (v.bron > 0.9) {
                    if (poelen == null) {
                        poelen = poelenBij(m, x0, z0, n);
                    }
                    for (Poel p : poelen) {
                        double in = p.rand(x - p.x(), z - p.z());
                        if (in > 0) {
                            inWater = Math.max(inWater, in + 0.4);
                        }
                    }
                }
                if (inWater > 0) {
                    // water: the level of the shape rules, one deep at the banks and in a cascade's gully, two in the middle,
                    // three in a plunge pool or the heart of a koi pool
                    int diep = inWater > 1.3 ? 2 : 1;
                    if (v.peil != top - 1) {
                        diep = 1;
                    } else if (inWater > 2.0 && (v.echt && v.sn >= 0 && v.sn < 6 || inWater > 3.4 && !rivier && !beek)) {
                        diep = 3;
                    }
                    boolean steen = false;
                    if (rivier && v.peil == top - 1) {
                        if (stappen == null) {
                            stappen = stappenBij(m, x0, z0, n);
                        }
                        for (Stap st : stappen) {
                            double dx = x - st.x(), dz = z - st.z(), dwars = dx * st.nx() + dz * st.nz(), langs = -dx * st.nz() + dz * st.nx();
                            if (Math.abs(langs) <= 0.55 && Math.abs(dwars) <= 6 && ((int) Math.floor(dwars + 0.5) & 1) == 0) {
                                steen = true;
                            }
                        }
                    }
                    if (steen) {
                        h[idx] = v.peil;
                        vl[idx] = (short) (Kaart.RIVIER | Kaart.LIP);
                    } else {
                        wat[idx] = v.peil;
                        h[idx] = v.peil - diep;
                        vl[idx] = (short) (Kaart.RIVIER | (v.peil != top - 1 || diep > 2 ? VORM : 0));
                    }
                    continue;
                }
                // dry ground. The right bank near the water, and both banks of a brook, stay plain
                boolean vrij = stroomt && (!links && naast < VRIJ || naastBeek < 4);
                if (!vrij) {
                    int edge = 2 - t;
                    // the rounded foot of the face above this terrace
                    if (t < 3 && v.s[edge] >= 0 && v.s[edge] < BOL) {
                        // (how far the foot bulges out here, in blocks: a noise of its own, only read this close to a face)
                        double bol = Math.min(BOL, 0.3 + 3.2 * m.ruis(BioModel.R_DETAIL, x + 1000, z + 1000));
                        if (v.s[edge] < bol) {
                            double q = v.s[edge] / bol;
                            int op = (int) Math.round(Math.max(1, Math.min(3.6, 1.6 * bol)) * Math.sqrt(1 - q * q));
                            if (op > 0) {
                                hoogte = top + op;
                                vlag |= VORM;
                            }
                        }
                    }
                    // the rounded shoulder of the face below it
                    if (t > 0 && v.s[3 - t] >= -v.schouder) {
                        hoogte = top - 1;
                        vlag |= VORM;
                    }
                    if (keien == null) {
                        keien = keienBij(m, x0, z0, n);
                    }
                    for (Kei kei : keien) {
                        if (kei.terras() == t) {
                            double dx = x - kei.x(), dz = z - kei.z(), q = (dx * dx + dz * dz) / (kei.straal() * kei.straal());
                            if (q < 1) {
                                int op = (int) Math.round(kei.hoog() * Math.sqrt(1 - q));
                                if (op > 0 && top + op > hoogte) {
                                    hoogte = top + op;
                                    vlag |= VORM;
                                }
                            }
                        }
                    }
                }
                // the natural steps beside a cascade of the river: on the left bank, climbing with the distance to the edge
                if (stroomt && links && naast >= TREDE_VAN && naast < TREDE_TOT && v.bron > 0.95 && t > 0) {
                    // (only where the river really crosses the edge: not where it runs along it)
                    double ex = e[Math.min(n * n - 1, idx + 1)] - e[Math.max(0, idx - 1)], ez = e[Math.min(n * n - 1, idx + n)] - e[Math.max(0, idx - n)];
                    double el = Math.sqrt(ex * ex + ez * ez);
                    if (el > 0 && Math.abs(ex * rnx + ez * rnz) / el < 0.6) {
                        for (int edge = 3 - t; edge < 3; edge++) {
                            double s = v.s[edge];
                            int onder = HOOGTE[2 - edge];
                            if (s < 0 && onder + (int) Math.ceil(-s) < hoogte) {
                                hoogte = onder + (int) Math.ceil(-s);
                                vlag = (short) (VORM | TREDE);
                            }
                        }
                    }
                }
                h[idx] = hoogte;
                vl[idx] = vlag;
            }
        }
        // the lips: river water next to lower DRY ground would run out of its bed; such a column becomes rock up to the
        // water's level. So does water next to water exactly ONE lower (a seam between two kinds of cascade): a one-block
        // step would turn the lower water into new sources. A new lip can make the column above it leak in turn: a few rounds.
        int[] buren = {-1, 1, -n, n};
        for (int ronde = 0; ronde < LIP_RONDES; ronde++) {
            boolean[] lip = new boolean[n * n];
            boolean nieuw = false;
            for (int j = 1; j < n - 1; j++) {
                for (int i = 1; i < n - 1; i++) {
                    int idx = i + j * n;
                    if ((vl[idx] & Kaart.RIVIER) == 0 || wat[idx] == Kaart.GEEN) {
                        continue;
                    }
                    for (int b : buren) {
                        if (wat[idx + b] == Kaart.GEEN ? h[idx + b] < wat[idx] || e[idx + b] < RAND : wat[idx + b] == wat[idx] - 1) {
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
                k.water[o] = wat[idx];
                k.terras[o] = ter[idx];
                short vlag = vl[idx];
                int hoogte = h[idx];
                if ((vlag & Kaart.RIVIER) != 0 && wat[idx] != Kaart.GEEN) {
                    for (int b : buren) {
                        if (wat[idx + b] != Kaart.GEEN && wat[idx + b] < wat[idx]) {
                            vlag |= Kaart.VAL;
                        }
                    }
                    if ((vlag & Kaart.VAL) != 0 && hoogte < wat[idx] - 1) {
                        // the lip of a fall is one deep: no second source under the top one
                        hoogte = wat[idx] - 1;
                    }
                }
                k.hoogte[o] = hoogte;
                k.vlag[o] = vlag;
            }
        }
        return true;
    }

    private static List<Poel> poelenBij(BioModel m, int x0, int z0, int n) {
        List<Poel> uit = new ArrayList<>(2);
        int bereik = 22;
        for (int cx = Math.floorDiv(x0 - bereik, POEL_CEL); cx <= Math.floorDiv(x0 + n + bereik, POEL_CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - bereik, POEL_CEL); cz <= Math.floorDiv(z0 + n + bereik, POEL_CEL); cz++) {
                Poel p = poel(m, cx, cz);
                if (p != null && p.x() + 8 >= x0 && p.x() - 8 < x0 + n && p.z() + 8 >= z0 && p.z() - 8 < z0 + n) {
                    uit.add(p);
                }
            }
        }
        return uit;
    }

    private static List<Stap> stappenBij(BioModel m, int x0, int z0, int n) {
        List<Stap> uit = new ArrayList<>(2);
        int bereik = 18;
        for (int cx = Math.floorDiv(x0 - bereik, STAP_CEL); cx <= Math.floorDiv(x0 + n + bereik, STAP_CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - bereik, STAP_CEL); cz <= Math.floorDiv(z0 + n + bereik, STAP_CEL); cz++) {
                Stap s = stap(m, cx, cz);
                if (s != null && s.x() + 8 >= x0 && s.x() - 8 < x0 + n && s.z() + 8 >= z0 && s.z() - 8 < z0 + n) {
                    uit.add(s);
                }
            }
        }
        return uit;
    }

    private static List<Kei> keienBij(BioModel m, int x0, int z0, int n) {
        List<Kei> uit = new ArrayList<>(8);
        int bereik = 4;
        for (int cx = Math.floorDiv(x0 - bereik, KEI_CEL); cx <= Math.floorDiv(x0 + n + bereik, KEI_CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - bereik, KEI_CEL); cz <= Math.floorDiv(z0 + n + bereik, KEI_CEL); cz++) {
                Kei kei = kei(m, cx, cz);
                if (kei != null) {
                    uit.add(kei);
                }
            }
        }
        return uit;
    }

    /** The pools, stones and boulders that can reach into a chunk (for {@link DalVulling} and the tests). */
    public static List<Poel> poelen(BioModel m, int cx, int cz) {
        return poelenBij(m, cx << 4, cz << 4, 16);
    }

    public static List<Kei> keien(BioModel m, int cx, int cz) {
        return keienBij(m, cx << 4, cz << 4, 16);
    }

    private DalTerrein() {
    }
}
