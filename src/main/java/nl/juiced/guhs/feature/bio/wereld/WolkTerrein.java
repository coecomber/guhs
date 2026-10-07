package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;

/**
 * biomes3 wereld, the Wolkenweide: a calm meadow, floating islands at many heights, cloud banks and the ways up (pure
 * maths on {@link BioModel}; the islands are solid terrain through the density function, the clouds and lift columns
 * are placed by {@link WolkVulling}).
 * <p>
 * <b>Meadow</b>: the land blends ({@link #RAND}) into a bed at {@link #WEIDE_Y} that rolls +-{@link #GLOOIING} blocks.
 * <p>
 * <b>Islands</b> ({@link Eiland}) come from a grid of {@link #CEL} blocks, at most one island and one loose rock per
 * cell, tops from {@link #LAAG} to {@link #LAAG} + {@link #HOOG} above the meadow (most of them low). Shapes: round,
 * elongated, crescent, double, with a hole ({@link Eiland#binnen}). The underside is drip-shaped: deepest under the
 * middle, with a few long drip points.
 * <p>
 * <b>Ways up</b>: an island up to {@link #TRAP_TOT} above the meadow gets a stair of small rocks (3 x 3, each one block
 * higher and {@link #TRAP_STAP} further: a gap of one block, a normal jump) from the meadow to its edge; a higher one
 * gets (mostly) a wolkenlift column from the meadow to its edge, 3 x 3 as in the zwevende_eilanden structure, and some of
 * those a wolkenstroom column down. A lift whose air is not free (another island in the way) is left out, in every chunk
 * alike ({@link #liftVrij}).
 * <p>
 * <b>Clouds</b> ({@link Wolk}): banks of two or three flattened blobs from a grid of {@link #WOLK_CEL}, white and some
 * pink, plus a thin "cloud sea" at {@link #ZEE_HOOGTE} above the meadow where a stretched noise is high.
 * <p>
 * <b>Buildings in the air</b>: {@link Luchtruim} knows where a {@code guhs:bio_plek} structure of kind {@code lucht} may
 * start; no island, stair, lift or cloud is made within its room, so such a building (it brings its own island) always
 * has free air from the meadow to the sky.
 * <p>
 * Owner after the kern: the Wolkenweide polish agent (this file, {@link WolkVulling}, tools/features/bio_wereld_wolk.py,
 * and the look of tools/features/bio_wereld_eiland.py).
 */
public final class WolkTerrein {
    // <wolk-terrein>
    public static final double RAND = 0.025;
    /** Islands and clouds only this far (in e) inside the region. */
    public static final double BINNEN = 0.03;
    public static final int WEIDE_Y = 70;
    public static final double GLOOIING = 2.5;
    public static final int CEL = 36;
    public static final double EILAND_KANS = 0.65, ROTS_KANS = 0.35;
    public static final int LAAG = 9, HOOG = 79;
    public static final int TRAP_TOT = 14, TRAP_STAP = 4;
    public static final double LIFT_KANS = 0.75, STROOM_KANS = 0.4;
    public static final int WOLK_CEL = 48;
    public static final double WOLK_KANS = 0.6, ROZE_KANS = 0.2;
    public static final int ZEE_HOOGTE = 34;
    // </wolk-terrein>

    public static final int ROND = 0, LANG = 1, MAAN = 2, DUBBEL = 3, GAT = 4;
    private static final int SOORT_EILAND = 2, SOORT_WOLK = 3;
    private static final Direction[] RICHTINGEN = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    /** How far anything of an island (its stair included) can lie from its middle. */
    static final int BEREIK = 16 + TRAP_TOT * TRAP_STAP + 4;

    /** The meadow's top block at (x, z). */
    public static int grond(BioModel m, int x, int z) {
        return WEIDE_Y + (int) Math.round(GLOOIING * m.ruis(BioModel.R_RIVIER, x * 0.7 + 4000, z * 0.7 - 4000));
    }

    /** A floating island. trap/lift/stroom: the side where its stair, lift or stream stands, or null. */
    public static final class Eiland {
        public final int x, z, top, vorm;
        public final double rx, rz;
        final double cos, sin, f1, f2;
        final double[] drup;
        public final Direction trap, lift, stroom;
        public final int trapStappen;
        /** The lift's and stream's middle column and ground height (valid when lift / stroom is set). */
        public final int liftX, liftZ, liftGrond, stroomX, stroomZ, stroomGrond;
        final int[] trapX, trapZ;

        Eiland(BioModel m, long h, int x, int z, boolean rots) {
            this.x = x;
            this.z = z;
            int grond = grond(m, x, z);
            double u = BioModel.kans(h, 2);
            this.top = grond + LAAG + (int) (HOOG * Math.pow(u, 1.7));
            if (rots) {
                vorm = ROND;
                rx = 1.6 + 1.6 * BioModel.kans(h, 3);
                rz = rx * (0.8 + 0.4 * BioModel.kans(h, 4));
            } else {
                double s = BioModel.kans(h, 5);
                vorm = s < 0.34 ? ROND : s < 0.54 ? LANG : s < 0.72 ? MAAN : s < 0.88 ? DUBBEL : GAT;
                double r = 4.5 + 7 * BioModel.kans(h, 3);
                if (vorm == LANG) {
                    rx = r * 1.35;
                    rz = r * 0.55;
                } else if (vorm == GAT || vorm == MAAN) {
                    rx = rz = Math.max(r, 7.5);
                } else {
                    rx = r;
                    rz = r * (0.75 + 0.5 * BioModel.kans(h, 4));
                }
            }
            double hoek = BioModel.kans(h, 6) * Math.PI * 2;
            cos = Math.cos(hoek);
            sin = Math.sin(hoek);
            f1 = BioModel.kans(h, 7) * 6.283;
            f2 = BioModel.kans(h, 8) * 6.283;
            int drups = rots ? 1 : 2 + (int) (BioModel.kans(h, 9) * 3);
            drup = new double[drups * 3];
            for (int i = 0; i < drups; i++) {
                double a = BioModel.kans(h, 20 + i * 3) * 6.283, d = BioModel.kans(h, 21 + i * 3) * 0.55;
                drup[i * 3] = Math.cos(a) * d * rx;
                drup[i * 3 + 1] = Math.sin(a) * d * rz;
                drup[i * 3 + 2] = rots ? 2 + 3 * BioModel.kans(h, 22) : 4 + 6 * BioModel.kans(h, 22 + i * 3);
            }
            // the way up
            Direction eerste = RICHTINGEN[(int) (BioModel.kans(h, 10) * 4)];
            Direction t = null, l = null, s = null;
            int stappen = 0, lx = 0, lz = 0, lg = 0, sx = 0, sz = 0, sg = 0;
            int[] tx = null, tz = null;
            if (!rots) {
                if (top - grond <= TRAP_TOT) {
                    t = eerste;
                    int rand = rand(t);
                    tx = new int[TRAP_TOT + 2];
                    tz = new int[TRAP_TOT + 2];
                    for (int k = 1; k <= TRAP_TOT + 1; k++) {
                        int px = x + t.getStepX() * (rand + 2 + (k - 1) * TRAP_STAP), pz = z + t.getStepZ() * (rand + 2 + (k - 1) * TRAP_STAP);
                        if (top - k <= grond(m, px, pz)) {
                            break;
                        }
                        tx[k - 1] = px;
                        tz[k - 1] = pz;
                        stappen = k;
                    }
                } else if (BioModel.kans(h, 11) < LIFT_KANS) {
                    l = eerste;
                    int rand = rand(l);
                    lx = x + l.getStepX() * (rand + 2);
                    lz = z + l.getStepZ() * (rand + 2);
                    lg = grond(m, lx, lz);
                    if (BioModel.kans(h, 12) < STROOM_KANS) {
                        s = eerste.getOpposite();
                        rand = rand(s);
                        sx = x + s.getStepX() * (rand + 2);
                        sz = z + s.getStepZ() * (rand + 2);
                        sg = grond(m, sx, sz);
                    }
                }
            }
            trap = t;
            lift = l;
            stroom = s;
            trapStappen = stappen;
            trapX = tx;
            trapZ = tz;
            liftX = lx;
            liftZ = lz;
            liftGrond = lg;
            stroomX = sx;
            stroomZ = sz;
            stroomGrond = sg;
        }

        /** How far the island reaches from its middle in a direction (the last column that is island). */
        public int rand(Direction d) {
            for (int a = (int) (Math.max(rx, rz) * 1.4) + 2; a > 0; a--) {
                if (binnen(d.getStepX() * a, d.getStepZ() * a) > 0) {
                    return a;
                }
            }
            return 0;
        }

        /** How deep inside the outline a column lies: above 0 inside (1 = the middle), 0 or less outside. */
        public double binnen(double dx, double dz) {
            double uu = dx * cos + dz * sin, v = -dx * sin + dz * cos;
            if (vorm == DUBBEL) {
                return Math.max(bol(uu - 0.55 * rx, v, 0.62), bol(uu + 0.55 * rx, v, 0.5));
            }
            double f = bol(uu, v, 1.0);
            if (vorm == MAAN) {
                double du = uu - 0.55 * rx;
                f = Math.min(f, (Math.sqrt(du * du + v * v) / (0.7 * rx) - 1) * 0.9);
            } else if (vorm == GAT) {
                f = Math.min(f, (Math.sqrt(uu * uu + v * v) / (0.32 * rx) - 1) * 0.6);
            }
            return f;
        }

        private double bol(double uu, double v, double schaal) {
            double a = Math.atan2(v / rz, uu / rx);
            double golf = 1 + 0.14 * Math.sin(2 * a + f1) + 0.10 * Math.sin(3 * a + f2);
            return 1 - Math.sqrt(uu * uu / (rx * rx) + v * v / (rz * rz)) / (golf * schaal);
        }

        /** The lowest solid block of a column with inside-ness f (above 0). */
        public int onder(double dx, double dz, double f) {
            double diep = 1 + (0.9 * Math.min(rx, rz) + 2) * Math.pow(Math.min(1, f), 0.55);
            if (f > 0.12) {
                for (int i = 0; i < drup.length; i += 3) {
                    double a = dx - (drup[i] * cos - drup[i + 1] * sin), b = dz - (drup[i] * sin + drup[i + 1] * cos);
                    double q = 1 - Math.sqrt(a * a + b * b) / 2.6;
                    if (q > 0) {
                        diep += drup[i + 2] * q * q;
                    }
                }
            }
            return top - (int) diep;
        }

        double straal() {
            return Math.max(rx, rz) * 1.3 + 1;
        }
    }

    /** One blob of a cloud bank. */
    public record Wolk(int x, int y, int z, double rx, double ry, double rz, boolean roze) {
    }

    /** The islands of a grid cell (cached in the model). */
    public static Eiland[] cel(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_EILAND, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return (Eiland[]) bekend;
        }
        List<Eiland> uit = new ArrayList<>(2);
        for (int welke = 0; welke < 2; welke++) {
            long h = m.hash(cx, cz, 5201 + welke);
            if (BioModel.kans(h, 0) >= (welke == 0 ? EILAND_KANS : ROTS_KANS)) {
                continue;
            }
            int x = cx * CEL + (int) (BioModel.kans(h, 1) * CEL), z = cz * CEL + (int) (BioModel.kans(h, 13) * CEL);
            if (m.eWeide(x, z) < BINNEN) {
                continue;
            }
            Eiland ei = new Eiland(m, h, x, z, welke == 1);
            if (!uit.isEmpty() && Math.hypot(x - uit.get(0).x, z - uit.get(0).z) < uit.get(0).straal() + ei.straal() + 2 && Math.abs(ei.top - uit.get(0).top) < 12) {
                continue;
            }
            if (Luchtruim.bezet(x, z, (int) ei.straal() + (ei.trap != null ? ei.trapStappen * TRAP_STAP + 4 : 6))) {
                continue;
            }
            uit.add(ei);
        }
        Eiland[] r = uit.toArray(new Eiland[0]);
        if (m.cellen.size() > 60000) {
            m.cellen.clear();
        }
        m.cellen.put(sleutel, r);
        return r;
    }

    /** Every island that can reach into the box [x0, x1) x [z0, z1) (stairs included). */
    public static List<Eiland> bij(BioModel m, int x0, int z0, int x1, int z1) {
        List<Eiland> uit = new ArrayList<>();
        for (int cx = Math.floorDiv(x0 - BEREIK, CEL); cx <= Math.floorDiv(x1 + BEREIK, CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - BEREIK, CEL); cz <= Math.floorDiv(z1 + BEREIK, CEL); cz++) {
                for (Eiland ei : cel(m, cx, cz)) {
                    uit.add(ei);
                }
            }
        }
        return uit;
    }

    /** The cloud blobs of a grid cell (cached in the model). */
    public static Wolk[] wolken(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_WOLK, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return (Wolk[]) bekend;
        }
        long h = m.hash(cx, cz, 6301);
        List<Wolk> uit = new ArrayList<>(3);
        int x = cx * WOLK_CEL + (int) (BioModel.kans(h, 1) * WOLK_CEL), z = cz * WOLK_CEL + (int) (BioModel.kans(h, 2) * WOLK_CEL);
        if (BioModel.kans(h, 0) < WOLK_KANS && m.eWeide(x, z) >= BINNEN && !Luchtruim.bezet(x, z, 18)) {
            int y = grond(m, x, z) + 12 + (int) (95 * BioModel.kans(h, 3));
            boolean roze = BioModel.kans(h, 4) < ROZE_KANS;
            int n = 2 + (int) (BioModel.kans(h, 5) * 2);
            for (int i = 0; i < n; i++) {
                double a = BioModel.kans(h, 10 + i * 5) * 6.283, d = i == 0 ? 0 : 3 + 4 * BioModel.kans(h, 11 + i * 5);
                uit.add(new Wolk(x + (int) Math.round(Math.cos(a) * d), y + (i == 0 ? 0 : (int) (BioModel.kans(h, 12 + i * 5) * 3) - 1),
                        z + (int) Math.round(Math.sin(a) * d), 5 + 4 * BioModel.kans(h, 13 + i * 5), 1.8 + 1.4 * BioModel.kans(h, 14 + i * 5),
                        4 + 3.5 * BioModel.kans(h, 15 + i * 5), roze));
            }
        }
        Wolk[] r = uit.toArray(new Wolk[0]);
        m.cellen.put(sleutel, r);
        return r;
    }

    /** Is the thin cloud sea at this column (0: no; 1 or 2: that many blocks thick)? */
    public static int wolkenzee(BioModel m, int x, int z) {
        double v = m.ruis(BioModel.R_RIVIER, x * 0.9 - 9000, z * 0.9 + 9000);
        return v > 0.42 ? 2 : v > 0.3 ? 1 : 0;
    }

    /** Is the air of a 3 x 3 lift column (around x, z, from its ground to yBoven) free of every island? The same answer in every chunk. */
    public static boolean liftVrij(BioModel m, int x, int z, int grond, int yBoven) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (m.meng(x + dx, z + dz) < 1f || !m.luchtVrij(x + dx, z + dz, grond + 1, yBoven)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Fills the Wolkenweide columns of a chunk map; false when the chunk has none. */
    static boolean vul(BioModel m, Kaart k) {
        int x0 = k.cx << 4, z0 = k.cz << 4;
        boolean iets = false;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++) {
                double e = m.eWeide(x0 + i, z0 + j);
                if (e <= 0) {
                    continue;
                }
                int o = i | j << 4;
                iets = true;
                k.soort[o] = Kaart.WEIDE;
                k.meng[o] = (float) BioModel.zacht(e / RAND);
                k.hoogte[o] = grond(m, x0 + i, z0 + j);
            }
        }
        if (!iets) {
            return false;
        }
        for (Eiland ei : bij(m, x0, z0, x0 + 16, z0 + 16)) {
            int r = (int) ei.straal() + 1;
            for (int x = Math.max(x0, ei.x - r); x <= Math.min(x0 + 15, ei.x + r); x++) {
                for (int z = Math.max(z0, ei.z - r); z <= Math.min(z0 + 15, ei.z + r); z++) {
                    int o = Kaart.index(x, z);
                    if (k.meng[o] <= 0) {
                        continue;
                    }
                    double f = ei.binnen(x - ei.x, z - ei.z);
                    if (f > 0) {
                        // (a low island floats: its underside stays three blocks off the meadow)
                        int onder = Math.max(ei.onder(x - ei.x, z - ei.z, f), k.hoogte[o] + 4);
                        if (onder <= ei.top) {
                            k.span(o, onder, ei.top);
                        }
                    }
                }
            }
            for (int s = 0; s < ei.trapStappen; s++) {
                int top = ei.top - (s + 1);
                for (int x = Math.max(x0, ei.trapX[s] - 1); x <= Math.min(x0 + 15, ei.trapX[s] + 1); x++) {
                    for (int z = Math.max(z0, ei.trapZ[s] - 1); z <= Math.min(z0 + 15, ei.trapZ[s] + 1); z++) {
                        int o = Kaart.index(x, z);
                        if (k.meng[o] > 0) {
                            boolean midden = x == ei.trapX[s] && z == ei.trapZ[s];
                            k.span(o, top - (midden ? 2 + s % 2 : 1), top);
                        }
                    }
                }
            }
        }
        return true;
    }

    private WolkTerrein() {
    }
}
