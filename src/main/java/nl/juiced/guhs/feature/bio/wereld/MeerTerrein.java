package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;

/**
 * biomes3 wereld, the Bloesemmeertje: the shape of the lake in the middle of a dal (pure maths on {@link BioModel}).
 * <p>
 * The lake starts where the valley floor ends ({@link DalTerrein#TRAP}[3]); d = how far past that. Its top water block is
 * at {@link #WATER}, one below the valley floor.
 * <ul>
 *   <li><b>The shore.</b> The first blocks of the lake's own columns are LAND at the valley floor's height ({@link #OEVER},
 *       with a sand edge: flag {@link #STRAND}), and a slow noise pushes it out into headlands ({@link #BOCHT}) with bays
 *       between them. Where a river of the valley arrives the strip gives way, so every river still runs into the lake.</li>
 *   <li><b>The depth.</b> All in blocks from the shore ({@link #afstand}); s = how far past the real shore: {@link #DIEP_OEVER} deep there (1-2 for the first blocks) and
 *       sloping to about {@link #DIEP_MIDDEN} over {@link #DIEP_OVER}, with a little relief; never more than 8.</li>
 *   <li><b>Large islands</b> ({@link Eiland#groot}): at most one per cell of {@link #CEL} blocks, where the lake is wide
 *       enough, and never two within {@link #GROOT_AFSTAND}: a few per lake, not an archipelago. A main lobe (19-24
 *       wide) with a smaller second lobe (the big tree stands there, {@link Eiland#boomX}), flat buildable ground two above
 *       the water on the main lobe, a sand rim that widens into a little beach, and per island a small hill and / or a low
 *       sand spit. Some have a tiny neighbour island with stepping stones to it ({@link #STEEN} columns at water level).</li>
 *   <li><b>Small accent islands</b>, a few, and smooth boulders in the shallows ({@link #STEEN}).</li>
 * </ul>
 * Around every island the floor rises to it, so the water is 1-2 deep there. Everything solid here is known to the model:
 * stepping stones and boulders are columns of it, never blocks a feature adds.
 * <p>
 * The trees are part of the model too ({@link #bomen}): where each stands, how big it is and which way it leans, so the
 * big tree's structure spot, the petals on the water and the tree-free building ground all follow from the same numbers.
 * <p>
 * Owner: the Bloesemmeertje polish agent (this file, {@link MeerVulling}, {@link MeerLeven}, tools/features/bio_wereld_meer.py).
 */
public final class MeerTerrein {
    // <meer-terrein>
    /** The top water block of every lake (= {@link DalTerrein#HOOGTE}[0] - 1). */
    public static final int WATER = 49;
    /** The depth at the shore and in the middle, and over how many blocks from the shore it gets there. */
    public static final double DIEP_OEVER = 1.2, DIEP_MIDDEN = 6.6, DIEP_OVER = 34;
    /** The middle is not one flat floor: broad shallows and hollows of this many blocks up and down. */
    public static final double DIEP_GOLF = 1.3;
    /** The deepest the lake ever is (blocks of water). */
    public static final int DIEP_MAX = 8;
    /** In blocks: the strip of shore land inside the lake's edge, the sand edge of it, and how far a headland reaches. */
    public static final double OEVER = 2.5, STRAND_BREED = 1.8, BOCHT = 15, BOCHT_GROOT = 12;
    /** The scales of the two headland noises (the detail noise, stretched: about 100 and 300 blocks from bay to bay). */
    public static final double BOCHT_SCHAAL = 0.3, BOCHT_GROOT_SCHAAL = 0.1;
    public static final int CEL = 88;
    /** A large island needs this many blocks of lake around its middle, a small one this many. */
    public static final double GROOT_VANAF = 34, KLEIN_VANAF = 14;
    public static final double GROOT_KANS = 0.8, KLEIN_KANS = 0.3, TWEEDE_KANS = 0.25;
    /** Two large islands lie at least this far apart (middle to middle). */
    public static final int GROOT_AFSTAND = 84;
    /** How many large islands carry the rare giant overhanging tree; the others get a middle-sized leaning one. */
    public static final double REUS_KANS = 0.3;
    /** Boulders: one candidate per cell of this size, in water at most this deep. */
    public static final int KEI_CEL = 26;
    public static final double KEI_KANS = 0.5;
    /** Shore trees: one candidate per cell of this size. */
    public static final int BOOM_CEL = 11;
    public static final double BOOM_KANS = 0.5;
    // </meer-terrein>

    /** Extra column flags of the lake, next to {@link Kaart#EILAND} and {@link Kaart#GROOT} (bits {@link Kaart} does not use). */
    public static final byte STEEN = (byte) Kaart.MEER_STEEN, STRAND = (byte) Kaart.MEER_STRAND; // biomes3 merge: named in Kaart (must stay below 256: kolom packs them in 8 bits)

    private static final int SOORT = 1, SOORT_KEI = 6; // biomes3 merge: KEI was 5, which is WolkTerrein.SOORT_KANDIDAAT (one key space: BioModel.cellen; lake 1 and 6, Wolkenweide 2-5, Klaterdal 20-23)
    /** The furthest anything of an island (its underwater slope included) lies from its middle. */
    private static final int BEREIK = 48;

    /** A lobe of an island's outline: a wobbly circle. laag: a sand spit, never higher than the beach. */
    public record Lob(double x, double z, double r, double f1, double f2, boolean laag) {
        /** How far inside this lobe (dx, dz from the island's reference) lies: positive inside, in blocks. */
        double in(double px, double pz) {
            double dx = px - x, dz = pz - z, afstand = Math.sqrt(dx * dx + dz * dz);
            if (afstand > r * 1.25 + 14) {
                return -99;
            }
            double hoek = Math.atan2(dz, dx), k = r < 4 ? 0.6 : 1;
            return r * (1 + k * (0.12 * Math.sin(2 * hoek + f1) + 0.08 * Math.sin(3 * hoek + f2))) - afstand;
        }
    }

    /** A tree of the lake: its foot column, its size (0 small .. 2 large, 3 the giant), the way it leans, and whether it is an island's big tree. */
    public record Boom(int x, int z, int maat, int leunX, int leunZ, boolean vast, long zaad) {
    }

    /**
     * A lake island. heuvelHoek: the direction (radians) of its hill; the big tree stands on the other side, on the second
     * lobe, leaning out over the water. kei: a boulder (lobben empty, straal and hoog give the dome).
     */
    public record Eiland(int x, int z, double straal, boolean groot, boolean heuvel, double heuvelHoek, int boomX, int boomZ, Lob[] lobben,
                         double strandX, double strandZ, boolean reus, long[] stenen, Boom[] bomen, boolean kei, double hoog, double bereik) {
        /** How far inside the island a column lies (positive: land), in blocks; hoog[0] gets the same for the lobes that carry the plateau. */
        double in(double px, double pz, double[] hoogUit) {
            double best = -99, h = -99;
            for (Lob l : lobben) {
                double v = l.in(px, pz);
                best = Math.max(best, v);
                if (!l.laag()) {
                    h = Math.max(h, v);
                }
            }
            hoogUit[0] = h;
            return best;
        }

        boolean steen(int px, int pz) {
            long s = pak(px, pz);
            for (long st : stenen) {
                if (st == s) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final long[] GEEN_STENEN = {};
    private static final Boom[] GEEN_BOMEN = {};
    private static final Lob[] GEEN_LOBBEN = {};

    static long pak(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /** The lake value of a column: how far past the valley floor (what {@link DalTerrein} hands to {@link #kolom}); negative outside the lake. */
    public static double d(BioModel m, int x, int z) {
        double e = m.eDal(x, z);
        return e < DalTerrein.RAND ? -1 : DalTerrein.rafel(m, x, z, e) - DalTerrein.TRAP[3];
    }

    /**
     * About how many blocks (x, z) lies inside the lake's edge, given its d: d over the slope of the dal noise there. (The
     * dal noise is steep at one lake and gentle at the next; in blocks the shore, the depth and the islands are the same
     * everywhere.)
     */
    static double afstand(BioModel m, int x, int z, double d) {
        // biomes3 fix-plaatsing: the dal's own value comes from its region's shape now (BioModel.dalEigen), no longer from a noise
        double n = m.dalEigen(x, z), eigen = n;
        double gx = (m.dalEigen(x + 3, z) - n) / 3, gz = (m.dalEigen(x, z + 3) - n) / 3;
        double a, e = eigen;
        // beside an older region the dal is squeezed and the lake's edge runs where that region says: the nearest of all
        // the edges (each older region on its own, so the distance has no jumps where two of them meet)
        double[] buur = new double[BioModel.MASKERS.length];
        for (int i = 0; i < buur.length; i++) {
            double[] k = BioModel.MASKERS[i];
            buur[i] = (k[1] - m.ruis((int) k[0], x, z)) * BioModel.MASKER_SCHAAL;
            e = Math.min(e, buur[i]);
        }
        // (d holds the fraying of the edge: the same on every line)
        double af = DalTerrein.TRAP[3] - (d - (e - DalTerrein.TRAP[3]));
        a = (eigen - af) / Math.max(0.0004, Math.sqrt(gx * gx + gz * gz));
        for (int i = 0; i < buur.length; i++) {
            if (buur[i] < 0.5) {
                int r = (int) BioModel.MASKERS[i][0];
                double v = m.ruis(r, x, z), mx = (m.ruis(r, x + 3, z) - v) / 3, mz = (m.ruis(r, x, z + 3) - v) / 3;
                a = Math.min(a, (buur[i] - af) / Math.max(0.0004, Math.sqrt(mx * mx + mz * mz) * BioModel.MASKER_SCHAAL));
            }
        }
        return a;
    }

    /** Blocks of lake around (x, z): negative outside the lake. */
    public static double ruimte(BioModel m, int x, int z) {
        double d = d(m, x, z);
        return d <= 0 ? -1 : afstand(m, x, z, d);
    }

    /** {eligible for a large island (0/1), x, z, priority} of a cell: pure hashing and one look at the dal noise. */
    private static double[] kandidaat(BioModel m, int cx, int cz) {
        long h = m.hash(cx, cz, 4101);
        int x = cx * CEL + 12 + (int) (BioModel.kans(h, 0) * (CEL - 24)), z = cz * CEL + 12 + (int) (BioModel.kans(h, 1) * (CEL - 24));
        boolean kan = BioModel.kans(h, 2) < GROOT_KANS && ruimte(m, x, z) >= GROOT_VANAF;
        return new double[]{kan ? 1 : 0, x, z, BioModel.kans(h, 20)};
    }

    /** The islands of a grid cell (cached in the model). */
    public static Eiland[] cel(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return (Eiland[]) bekend;
        }
        long h = m.hash(cx, cz, 4101);
        List<Eiland> uit = new ArrayList<>(3);
        double[] k = kandidaat(m, cx, cz);
        int x = (int) k[1], z = (int) k[2];
        boolean groot = k[0] > 0;
        if (groot) {
            // of two large islands too close together, the one with the higher draw stays
            for (int ax = -1; ax <= 1 && groot; ax++) {
                for (int az = -1; az <= 1 && groot; az++) {
                    if (ax != 0 || az != 0) {
                        double[] b = kandidaat(m, cx + ax, cz + az);
                        groot = !(b[0] > 0 && Math.hypot(b[1] - x, b[2] - z) < GROOT_AFSTAND && b[3] > k[3]);
                    }
                }
            }
        }
        if (groot) {
            groot(m, h, x, z, uit);
        } else if (BioModel.kans(h, 2) < KLEIN_KANS && ruimte(m, x, z) >= KLEIN_VANAF) {
            uit.add(klein(m, h, 30, x, z, 2.4 + 1.6 * BioModel.kans(h, 3)));
        }
        // a second, small one elsewhere in the cell
        int x2 = cx * CEL + 6 + (int) (BioModel.kans(h, 8) * (CEL - 12)), z2 = cz * CEL + 6 + (int) (BioModel.kans(h, 9) * (CEL - 12));
        if (BioModel.kans(h, 10) < TWEEDE_KANS && ruimte(m, x2, z2) >= KLEIN_VANAF) {
            boolean vrij = true;
            for (Eiland ei : uit) {
                vrij &= Math.hypot(x2 - ei.x(), z2 - ei.z()) > (ei.groot() ? ei.straal() * 2.2 + 14 : 16);
            }
            if (vrij) {
                uit.add(klein(m, h, 40, x2, z2, 2.0 + 1.6 * BioModel.kans(h, 11)));
            }
        }
        Eiland[] r = uit.toArray(new Eiland[0]);
        if (m.cellen.size() > 60000) {
            m.cellen.clear();
        }
        m.cellen.put(sleutel, r);
        return r;
    }

    private static Eiland klein(BioModel m, long h, int n, int x, int z, double straal) {
        Lob[] lobben = {new Lob(x, z, straal, BioModel.kans(h, n) * 6.283, BioModel.kans(h, n + 1) * 6.283, false)};
        // about half of the accent islands that are big enough carry one small tree
        Boom[] bomen = straal >= 2.7 && BioModel.kans(h, n + 2) < 0.55
                ? new Boom[]{new Boom(x, z, BioModel.kans(h, n + 3) < 0.6 ? 0 : 1, 0, 0, false, BioModel.mix(h + n))} : GEEN_BOMEN;
        return new Eiland(x, z, straal, false, false, 0, x, z, lobben, x, z, false, GEEN_STENEN, bomen, false, 0, straal * 1.25 + 16);
    }

    /** A large island at (x, z), and maybe its tiny neighbour with the stepping stones to it. */
    private static void groot(BioModel m, long h, int x, int z, List<Eiland> uit) {
        double straal = 9.5 + 2.5 * BioModel.kans(h, 3), hoek = BioModel.kans(h, 6) * Math.PI * 2;
        boolean spit = BioModel.kans(h, 14) < 0.5, heuvel = !spit || BioModel.kans(h, 7) < 0.3;
        // the tree side (away from the hill), and the beach side (across both)
        double boomHoek = hoek + Math.PI, strandHoek = hoek + (BioModel.kans(h, 15) < 0.5 ? 1 : -1) * (1.35 + 0.5 * BioModel.kans(h, 16));
        double bc = Math.cos(boomHoek), bs = Math.sin(boomHoek), sc = Math.cos(strandHoek), ss = Math.sin(strandHoek);
        List<Lob> lobben = new ArrayList<>(4);
        lobben.add(new Lob(x, z, straal, BioModel.kans(h, 4) * 6.283, BioModel.kans(h, 5) * 6.283, false));
        double r2 = straal * (0.54 + 0.08 * BioModel.kans(h, 17));
        lobben.add(new Lob(x + bc * straal * 0.62, z + bs * straal * 0.62, r2, BioModel.kans(h, 18) * 6.283, BioModel.kans(h, 19) * 6.283, false));
        double uiteinde = straal;
        if (spit) {
            double buig = (BioModel.kans(h, 21) - 0.5) * 0.7;
            lobben.add(new Lob(x + sc * (straal + 1.2), z + ss * (straal + 1.2), 3.3, 1, 2, true));
            lobben.add(new Lob(x + Math.cos(strandHoek + buig * 0.3) * (straal + 5.2), z + Math.sin(strandHoek + buig * 0.3) * (straal + 5.2), 2.5, 3, 4, true));
            uiteinde = straal + 7.5;
        }
        int boomX = x + (int) Math.round(bc * straal * 0.8), boomZ = z + (int) Math.round(bs * straal * 0.8);
        boolean reus = BioModel.kans(h, 22) < REUS_KANS;
        int lx = (int) Math.round(bc), lz = (int) Math.round(bs);
        List<Boom> bomen = new ArrayList<>(3);
        bomen.add(new Boom(boomX, boomZ, reus ? 3 : 2, lx, lz, true, BioModel.mix(h + 77)));
        // one or two small trees on the rim, away from the beach, the hill top and the big tree: the middle stays free
        double zij = strandHoek + Math.PI;
        if (BioModel.kans(h, 23) < 0.75) {
            int tx = x + (int) Math.round(Math.cos(zij) * straal * 0.86), tz = z + (int) Math.round(Math.sin(zij) * straal * 0.86);
            bomen.add(new Boom(tx, tz, BioModel.kans(h, 24) < 0.5 ? 0 : 1, (int) Math.round(Math.cos(zij)), (int) Math.round(Math.sin(zij)), false, BioModel.mix(h + 78)));
        }
        if (heuvel && BioModel.kans(h, 25) < 0.4) {
            // a little one behind the hill, on the rim
            int tx = x + (int) Math.round(Math.cos(hoek + 0.6) * straal * 0.88), tz = z + (int) Math.round(Math.sin(hoek + 0.6) * straal * 0.88);
            bomen.add(new Boom(tx, tz, 0, 0, 0, false, BioModel.mix(h + 79)));
        }
        long[] stenen = GEEN_STENEN;
        Eiland buur = null;
        if (BioModel.kans(h, 26) < 0.5) {
            // the neighbour lies off the beach side; stones every other block from the rim to it
            double ver = uiteinde + 9 + 4 * BioModel.kans(h, 27), bh = strandHoek + (BioModel.kans(h, 28) - 0.5) * 0.5;
            int nx = x + (int) Math.round(Math.cos(bh) * ver), nz = z + (int) Math.round(Math.sin(bh) * ver);
            if (ruimte(m, nx, nz) >= KLEIN_VANAF) {
                buur = klein(m, h, 50, nx, nz, 2.6 + 1.2 * BioModel.kans(h, 29));
                List<Long> st = new ArrayList<>();
                double lengte = Math.hypot(nx - x, nz - z);
                for (double t = straal * 0.7; t < lengte; t += 2.0) {
                    double px = x + (nx - x) * t / lengte + Math.sin(t * 0.9) * 0.8, pz = z + (nz - z) * t / lengte + Math.cos(t * 0.7) * 0.8;
                    st.add(pak((int) Math.round(px), (int) Math.round(pz)));
                }
                stenen = st.stream().mapToLong(Long::longValue).toArray();
            }
        }
        uit.add(new Eiland(x, z, straal, true, heuvel, hoek, boomX, boomZ, lobben.toArray(GEEN_LOBBEN), x + sc * straal * 0.95, z + ss * straal * 0.95, reus,
                stenen, bomen.toArray(GEEN_BOMEN), false, 0, uiteinde + 26));
        if (buur != null) {
            uit.add(buur);
        }
    }

    /** The boulders of a boulder cell (cached): smooth domes in shallow water, as tiny "islands" of stone. */
    private static Eiland[] keien(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_KEI, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return (Eiland[]) bekend;
        }
        long h = m.hash(cx, cz, 4177);
        Eiland[] r = {};
        if (BioModel.kans(h, 0) < KEI_KANS) {
            int x = cx * KEI_CEL + 3 + (int) (BioModel.kans(h, 1) * (KEI_CEL - 6)), z = cz * KEI_CEL + 3 + (int) (BioModel.kans(h, 2) * (KEI_CEL - 6));
            double d = d(m, x, z);
            if (d > 0) {
                long v = vorm(m, x, z, d, eilandenBij(m, x - 4, z - 4, x + 5, z + 5));
                int diepte = WATER - (int) (v >> 8);
                // in the shallows (1-2 deep), but not against the land
                if (diepte >= 1 && diepte <= 2 && droogBinnen(m, x, z, 3) == 0) {
                    double straal = 1.2 + 1.2 * BioModel.kans(h, 3);
                    r = new Eiland[]{new Eiland(x, z, straal, false, false, 0, x, z, GEEN_LOBBEN, x, z, false, GEEN_STENEN, GEEN_BOMEN, true,
                            0.7 * straal + 0.1, straal + 2)};
                    if (BioModel.kans(h, 4) < 0.4) {
                        // a smaller one beside it
                        double a = BioModel.kans(h, 5) * 6.283;
                        int x2 = x + (int) Math.round(Math.cos(a) * (straal + 2.2)), z2 = z + (int) Math.round(Math.sin(a) * (straal + 2.2));
                        double d2 = d(m, x2, z2);
                        if (d2 > 0 && (int) (vorm(m, x2, z2, d2, eilandenBij(m, x2 - 4, z2 - 4, x2 + 5, z2 + 5)) >> 8) < WATER) {
                            r = new Eiland[]{r[0], new Eiland(x2, z2, 1.2, false, false, 0, x2, z2, GEEN_LOBBEN, x2, z2, false, GEEN_STENEN, GEEN_BOMEN, true, 0.8, 3.2)};
                        }
                    }
                }
            }
        }
        m.cellen.put(sleutel, r);
        return r;
    }

    /** How many of the eight columns at this distance are not lake water (boulders not counted: they are not there yet). */
    private static int droogBinnen(BioModel m, int x, int z, int afstand) {
        int n = 0;
        for (int a = 0; a < 8; a++) {
            int px = x + RX[a] * afstand, pz = z + RZ[a] * afstand;
            double d = d(m, px, pz);
            if (d <= 0 || (int) (vorm(m, px, pz, d, eilandenBij(m, px, pz, px + 1, pz + 1)) >> 8) >= WATER) {
                n++;
            }
        }
        return n;
    }

    static final int[] RX = {1, 1, 0, -1, -1, -1, 0, 1}, RZ = {0, 1, 1, 1, 0, -1, -1, -1};

    /** Every island (no boulders) that can reach into the box [x0, x1) x [z0, z1). */
    public static List<Eiland> eilandenBij(BioModel m, int x0, int z0, int x1, int z1) {
        List<Eiland> uit = new ArrayList<>();
        for (int cx = Math.floorDiv(x0 - BEREIK, CEL); cx <= Math.floorDiv(x1 + BEREIK, CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - BEREIK, CEL); cz <= Math.floorDiv(z1 + BEREIK, CEL); cz++) {
                for (Eiland ei : cel(m, cx, cz)) {
                    double b = ei.bereik();
                    if (ei.x() + b >= x0 && ei.x() - b < x1 && ei.z() + b >= z0 && ei.z() - b < z1) {
                        uit.add(ei);
                    }
                }
            }
        }
        return uit;
    }

    /** Every island AND boulder that can reach into the box [x0, x1) x [z0, z1) (what {@link #kolom} wants). */
    public static List<Eiland> bij(BioModel m, int x0, int z0, int x1, int z1) {
        List<Eiland> uit = eilandenBij(m, x0, z0, x1, z1);
        for (int cx = Math.floorDiv(x0 - 4, KEI_CEL); cx <= Math.floorDiv(x1 + 4, KEI_CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - 4, KEI_CEL); cz <= Math.floorDiv(z1 + 4, KEI_CEL); cz++) {
                for (Eiland ei : keien(m, cx, cz)) {
                    uit.add(ei);
                }
            }
        }
        return uit;
    }

    /** How far the shore land reaches into the lake at (x, z), in blocks; 0 where a river comes in. a = blocks inside the lake's edge. */
    static double bocht(BioModel m, int x, int z, double a) {
        if (a > OEVER + BOCHT + BOCHT_GROOT) {
            return 0;
        }
        // small headlands on broad ones: a shore of points and bays
        double n = m.ruis(BioModel.R_DETAIL, x * BOCHT_SCHAAL + 811.5, z * BOCHT_SCHAAL - 433.5);
        double n2 = m.ruis(BioModel.R_DETAIL, x * BOCHT_GROOT_SCHAAL - 277.5, z * BOCHT_GROOT_SCHAAL + 619.5);
        double b = OEVER + BOCHT * BioModel.zacht((n - 0.08) / 0.34) + BOCHT_GROOT * BioModel.zacht((n2 - 0.05) / 0.4);
        if (a < b) {
            // a river mouth stays open (the same line as DalTerrein's river)
            double r = m.ruis(BioModel.R_RIVIER, x, z);
            double gx = m.ruis(BioModel.R_RIVIER, x + 1, z) - r, gz = m.ruis(BioModel.R_RIVIER, x, z + 1) - r;
            double g = Math.sqrt(gx * gx + gz * gz);
            if (g > 0.0012) {
                b *= BioModel.zacht((Math.abs(r) / g - 8.5) / 14);
            }
        }
        return b;
    }

    /**
     * The shape of one lake column without boulders: (top solid block << 8) | flags. d = e past the valley floor.
     * eilanden: the islands near (boulders in the list are skipped).
     */
    static long vorm(BioModel m, int x, int z, double d, List<Eiland> eilanden) {
        double relief = m.ruis(BioModel.R_DETAIL, x * 0.5, z * 0.5);
        double a = afstand(m, x, z, d), s = a - bocht(m, x, z, a);
        int top, v = 0;
        if (s < 0) {
            top = WATER + 1;
            if (s > -STRAND_BREED * (1 + 0.7 * relief)) {
                v = STRAND;
            }
        } else {
            double diep = BioModel.zacht(s / DIEP_OVER);
            double midden = DIEP_MIDDEN + (diep > 0.5 ? DIEP_GOLF * m.ruis(BioModel.R_DETAIL, x * 0.16 + 133.5, z * 0.16 + 271.5) * (diep - 0.5) * 2 : 0);
            double diepte = DIEP_OEVER + (midden - DIEP_OEVER) * diep + 0.9 * relief;
            // a shelf: the first blocks from the shore stay 1-2 deep
            diepte = Math.min(diepte, 1.2 + s / 3.5);
            top = WATER - Math.max(1, Math.min(DIEP_MAX, (int) Math.round(diepte)));
        }
        double[] hoog = new double[1];
        for (Eiland ei : eilanden) {
            if (ei.kei()) {
                continue;
            }
            double dx = x - ei.x(), dz = z - ei.z();
            if (Math.abs(dx) > ei.bereik() || Math.abs(dz) > ei.bereik()) {
                continue;
            }
            double in = ei.in(x, z, hoog);
            if (in > 0) {
                int y = WATER + 1;
                int vl = Kaart.EILAND | (ei.groot() ? Kaart.GROOT : 0);
                if (ei.groot()) {
                    double sx = x - ei.strandX(), sz = z - ei.strandZ();
                    // the sand rim, widening into the little beach
                    double rand = 1.7 + 0.5 * relief + 3.4 * Math.exp(-(sx * sx + sz * sz) / 30.0);
                    if (hoog[0] > rand) {
                        y = WATER + 2;
                        if (ei.heuvel()) {
                            double hx = dx - Math.cos(ei.heuvelHoek()) * ei.straal() * 0.62, hz = dz - Math.sin(ei.heuvelHoek()) * ei.straal() * 0.62;
                            double q = Math.sqrt(hx * hx + hz * hz) / (ei.straal() * 0.37);
                            if (q < 1) {
                                y += (int) Math.round(Math.min(3.3 * (1 - q * q), (hoog[0] - rand) * 0.9));
                            }
                        }
                    } else {
                        vl |= STRAND;
                    }
                } else if (in < 1.3 + 0.5 * relief) {
                    vl |= STRAND;
                }
                if (y > top) {
                    top = y;
                    v = vl;
                }
            } else if (in > -20) {
                // the floor rises to the island: 1 deep for the first two blocks, then down
                int y = WATER - Math.max(1, (int) Math.round(1 + Math.max(0, -in - 2.2) * 0.55 + 0.6 * relief));
                if (y > top) {
                    top = y;
                    v = 0;
                }
                if (top < WATER && ei.stenen().length > 0 && ei.steen(x, z)) {
                    top = WATER;
                    v = Kaart.EILAND | STEEN;
                }
            }
        }
        return ((long) top << 8) | (v & 0xFF);
    }

    /** A column's {@link #vorm} with the boulders of the list on it: (top solid block << 8) | flags. */
    private static long metKeien(long vorm, int x, int z, List<Eiland> eilanden) {
        int top = (int) (vorm >> 8), v = (int) (vorm & 0xFF);
        if (top < WATER) {
            for (Eiland ei : eilanden) {
                if (!ei.kei()) {
                    continue;
                }
                double dx = x - ei.x(), dz = z - ei.z(), q = (dx * dx + dz * dz) / (ei.straal() * ei.straal());
                if (q <= 1) {
                    int y = WATER + (int) Math.round(ei.hoog() * Math.sqrt(1 - q) - 0.2);
                    if (y > top) {
                        top = y;
                        v = Kaart.EILAND | STEEN;
                    }
                }
            }
        }
        return ((long) top << 8) | (top >= WATER ? v & 0xFF : 0);
    }

    /** One lake column: its top solid block, its water and its flags, into the arrays at idx. d = e past the valley floor. */
    static void kolom(BioModel m, int x, int z, double d, List<Eiland> eilanden, int[] h, int[] wat, short[] vl, int idx) {
        long k = metKeien(vorm(m, x, z, d, eilanden), x, z, eilanden);
        int top = (int) (k >> 8);
        h[idx] = top;
        wat[idx] = top >= WATER ? Kaart.GEEN : WATER;
        vl[idx] = (short) (k & 0xFF);
    }

    /** {@link #los} of a column that is not a lake column. */
    public static final long GEEN_MEER = Long.MIN_VALUE;

    /**
     * ONE lake column on its own, without building the chunk's map: (top solid block << 8) | flags, exactly what
     * {@link #kolom} gives, or {@link #GEEN_MEER} outside the lake. For the few questions about columns of OTHER chunks
     * (is there water in front of this tree, is this corner sheltered): a chunk map costs hundreds of these.
     */
    public static long los(BioModel m, int x, int z) {
        double e = m.eDal(x, z);
        if (e < DalTerrein.RAND) {
            return GEEN_MEER;
        }
        double ew = DalTerrein.rafel(m, x, z, e);
        if (ew < DalTerrein.TRAP[3]) { // biomes3 merge: was DalTerrein.terras(ew, 0) >= 0, which the valley's rewrite dropped; the lake still starts where the frayed e reaches TRAP[3] (DalTerrein.vorm: "e + RAFEL * det >= TRAP[3]")
            return GEEN_MEER;
        }
        List<Eiland> eilanden = bij(m, x, z, x + 1, z + 1);
        return metKeien(vorm(m, x, z, ew - DalTerrein.TRAP[3], eilanden), x, z, eilanden);
    }

    /** Lake water at (x, z)? (One column, see {@link #los}.) */
    public static boolean nat(BioModel m, int x, int z) {
        long k = los(m, x, z);
        return k != GEEN_MEER && (int) (k >> 8) < WATER;
    }

    /** The depth of the lake at (x, z): 0 where there is no lake water. (One column, see {@link #los}.) */
    public static int diepte(BioModel m, int x, int z) {
        long k = los(m, x, z);
        return k == GEEN_MEER ? 0 : Math.max(0, WATER - (int) (k >> 8));
    }

    // --- the trees ------------------------------------------------------------------------------------------------------------
    /**
     * Every tree whose foot stands in the box [x0, x1) x [z0, z1): the trees of the islands (the big one on its tree spot
     * first) and the shore trees (a jittered grid over the lake biome's dry shore; one within a few blocks of the water
     * leans out over it). ookDal: also the trees on the valley floor's strip of the biome, behind the lake's own shore
     * (those read the chunk's map; without them nothing here does, so a wide box is cheap).
     */
    public static List<Boom> bomen(BioModel m, int x0, int z0, int x1, int z1, boolean ookDal) {
        List<Boom> uit = new ArrayList<>();
        for (int cx = Math.floorDiv(x0 - 16, CEL); cx <= Math.floorDiv(x1 + 16, CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - 16, CEL); cz <= Math.floorDiv(z1 + 16, CEL); cz++) {
                for (Eiland ei : cel(m, cx, cz)) {
                    for (Boom b : ei.bomen()) {
                        if (b.x() >= x0 && b.x() < x1 && b.z() >= z0 && b.z() < z1) {
                            long k = los(m, b.x(), b.z());
                            if (k != GEEN_MEER && (int) (k >> 8) > WATER && (k & Kaart.EILAND) != 0 && (k & STEEN) == 0) {
                                uit.add(b);
                            }
                        }
                    }
                }
            }
        }
        for (int cx = Math.floorDiv(x0, BOOM_CEL); cx <= Math.floorDiv(x1 - 1, BOOM_CEL); cx++) {
            for (int cz = Math.floorDiv(z0, BOOM_CEL); cz <= Math.floorDiv(z1 - 1, BOOM_CEL); cz++) {
                long h = m.hash(cx, cz, 4190);
                if (BioModel.kans(h, 0) >= BOOM_KANS) {
                    continue;
                }
                int x = cx * BOOM_CEL + (int) (BioModel.kans(h, 1) * (BOOM_CEL - 4)), z = cz * BOOM_CEL + (int) (BioModel.kans(h, 2) * (BOOM_CEL - 4));
                if (x < x0 || x >= x1 || z < z0 || z >= z1) {
                    continue;
                }
                // a cheap look first: only the strip of shore can carry one (open water and the valley are most of the cells)
                double d = d(m, x, z);
                if (d > 0) {
                    // the lake's own shore land: flat, no sand
                    if (afstand(m, x, z, d) > OEVER + BOCHT + BOCHT_GROOT) {
                        continue;
                    }
                    long k = los(m, x, z);
                    if (k == GEEN_MEER || (int) (k >> 8) != WATER + 1 || (k & 0xFF) != 0) {
                        continue;
                    }
                } else if (!ookDal || d < DalTerrein.MEER_BIOME - DalTerrein.TRAP[3] - 0.001 || !dalgrond(m, x, z)) {
                    continue;
                }
                // the nearest water within five blocks: lean to it
                int lx = 0, lz = 0;
                zoek:
                for (int a = 2; a <= 5; a++) {
                    for (int r = 0; r < 8; r++) {
                        int ri = (r + (int) (h & 7)) & 7;
                        if (nat(m, x + RX[ri] * a, z + RZ[ri] * a)) {
                            lx = RX[ri];
                            lz = RZ[ri];
                            break zoek;
                        }
                    }
                }
                double k = BioModel.kans(h, 3);
                int maat = lx != 0 || lz != 0 ? (k < 0.15 ? 0 : k < 0.65 ? 1 : 2) : (k < 0.38 ? 0 : k < 0.8 ? 1 : 2);
                uit.add(new Boom(x, z, maat, lx, lz, false, BioModel.mix(h + 5)));
            }
        }
        return uit;
    }

    /** Can a tree stand here on the valley floor inside the lake biome: flat, dry, no river (this one reads the chunk's map). */
    private static boolean dalgrond(BioModel m, int x, int z) {
        Kaart k = m.kaart(x >> 4, z >> 4);
        if (k.leeg) {
            return false;
        }
        int o = Kaart.index(x, z);
        return k.soort[o] == Kaart.MEER && k.meng[o] >= 1f && k.water[o] == Kaart.GEEN && k.vlag[o] == 0 && k.hoogte[o] == WATER + 1;
    }

    private MeerTerrein() {
    }
}
