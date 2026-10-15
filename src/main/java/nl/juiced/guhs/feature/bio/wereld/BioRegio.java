package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;

/**
 * biomes3 fix-plaatsing: ONE region of our biomes: a dal (a Bloesemmeertje with its ring of Klaterdal) or a Wolkenweide.
 * <p>
 * The regions used to be the tops of two low noises. A noise top has no size: lakes came out from 8 to 1700 blocks
 * across and four Wolkenweides in ten were slivers without room for one stack of islands. A region is a SHAPE now, with
 * a middle and a size that are drawn from the world seed:
 * <ul>
 *   <li>the world is cut in cells of {@link #CEL} blocks; a cell may try for a dal (chance {@link #DAL_KANS}) or else
 *       for a Wolkenweide ({@link #WEIDE_KANS});</li>
 *   <li>the older regions of the Guhmensie (sea, Knuffeldal, polder, tundra, Guhwai'i, Bleekwoud) leave only pockets
 *       free, most of them 200-400 blocks across. So a cell tries {@link #POGINGEN} places, and a region is made to FIT
 *       the pocket it finds ({@link #maak}): a lake as big as its draw says or as the pocket allows, never smaller than
 *       {@link #MEER_STRAAL}[0]; its heart must be whole and at most a quarter of its outline may be cut by a neighbour
 *       (there the valley is the squeezed, shallow kind). A Wolkenweide must be whole to half its radius and nearly
 *       whole to 0.8 of it. So no half lakes, no puddles and no slivers;</li>
 *   <li>of two candidates that would touch, the one with the higher draw stays ({@link #winnaar}). Regions never
 *       overlap, so a column belongs to at most one, and every region has a name ({@link #id}).</li>
 * </ul>
 * <b>A dal</b>: around the middle a lake outline {@code L(angle)} (a mean radius {@link #straal} of
 * {@link #MEER_STRAAL}, stretched to an ellipse by at most {@link #REK} and bent by three harmonics) and outside it a
 * valley {@code V(angle)} wide ({@link #DAL_BREED}, wider on one side than on the other). The value everything reads,
 * "how far into the dal" (e), is {@link DalTerrein#TRAP}[3] at the lake's outline and falls to 0 over the valley's
 * width; so the terraces are level rings around the lake, the river crosses them all and ends in the lake.
 * <b>A Wolkenweide</b>: one outline {@code R(angle)} (mean radius {@link #WEIDE_STRAAL}); e rises inward by
 * {@link #WEIDE_HELLING} per block (the slope the old noise had at the meadow's rim).
 * Both are read in coordinates that are pushed about by a slow noise ({@link #WARP} blocks at most), so an outline is not
 * a neat star: it has bays and bulges. The push is smooth and small, so an outline stays one closed line.
 * <p>
 * Sizes: measured by BioPlaatsingGameTests.bioPlaatsingMaten (and asserted there).
 */
public final class BioRegio {
    // <wereld-plaatsing> (the shares and sizes: BioPlaatsingGameTests, BioWereldGameTests.bioWereldAandeel; only the placement owner edits these)
    /** The grid of the candidates. Nothing of a region lies further than {@link #BUITEN_MAX} from its middle, which must stay below this. */
    public static final int CEL = 512;
    /** The chance that a cell tries for a dal, and (else) for a Wolkenweide; and how many places it tries. */
    public static final double DAL_KANS = 0.30, WEIDE_KANS = 0.11;
    public static final int POGINGEN = 6;
    /** The mean radius of a lake: from .. to, and the power that makes small ones more common. */
    public static final double[] MEER_STRAAL = {68, 150};
    public static final double MEER_MACHT = 1.0;
    /** The mean width of the valley around the lake (blocks from the lake's edge to the rim). */
    public static final double[] DAL_BREED = {95, 170};
    /** The mean radius of a Wolkenweide, and its power. */
    public static final double[] WEIDE_STRAAL = {160, 250};
    public static final double WEIDE_MACHT = 1.2;
    /** How much an outline is stretched along one axis (and squeezed across) at most. */
    public static final double REK = 1.22, WEIDE_REK = 1.2;
    /** The largest sizes of the three bends of an outline (3, 4 and 5 to a turn) and of a little second-order one. */
    private static final double[] BOCHT = {0.02, 0.07, 0.05, 0.03};
    /** How uneven the valley's width is round the lake (1, 2 and 3 to a turn). */
    private static final double[] ONGELIJK = {0.12, 0.08, 0.05};
    /** How fast the Wolkenweide's value rises inward (per block). */
    public static final double WEIDE_HELLING = 0.0018;
    /** The slow push of the coordinates: at most this many blocks each way. */
    public static final double WARP = 20;
    /** Blocks kept between two dals, and between a Wolkenweide and anything else of ours. */
    public static final int TUSSEN = 40, TUSSEN_WEIDE = 80;
    // </wereld-plaatsing>
    /**
     * The most a region's own value can change per block (the narrowest valley, and the pushed coordinates and the bends
     * of the outline steepening it by 0.6 at most; the meadow likewise): what "clearly outside" is measured with.
     */
    public static final double HELLING_MAX = DalTerrein.TRAP[3] / (DAL_BREED[0] * (1 - ONGELIJK[0] - ONGELIJK[1] - ONGELIJK[2])) * 1.6,
            HELLING_MAX_WEIDE = WEIDE_HELLING * 1.6;
    /** No region reaches further from its middle than this (checked when one is made). */
    public static final double BUITEN_MAX = 470;
    static {
        if (BUITEN_MAX >= CEL) {
            throw new IllegalStateException("biomes3: BUITEN_MAX must stay below CEL (a column asks the 3 x 3 cells around it)");
        }
    }

    public final boolean weide;
    /** The cell, the middle, and a number of its own (the same in every run of this world). */
    public final int cx, cz;
    public final double x, z;
    public final long id;
    /** Lake: its mean radius. Wolkenweide: its mean radius. */
    public final double straal;
    /** Dal: the mean width of the valley. */
    public final double breed;
    /** Nothing of the region lies further from its middle than this. */
    public final double buiten;
    final double prioriteit;
    private final double rek, rekCos, rekSin;
    private final double[] bocht = new double[4], bochtFase = new double[4], ongelijk = new double[3], ongelijkFase = new double[3];

    private BioRegio(int cx, int cz, long h, boolean weide, double x, double z, double straal) {
        this.weide = weide;
        this.cx = cx;
        this.cz = cz;
        this.id = h;
        this.x = x;
        this.z = z;
        this.straal = straal;
        prioriteit = BioModel.kans(h, 3);
        breed = weide ? 0 : DAL_BREED[0] + (DAL_BREED[1] - DAL_BREED[0]) * BioModel.kans(h, 5);
        rek = 1 + ((weide ? WEIDE_REK : REK) - 1) * BioModel.kans(h, 6);
        double hoek = BioModel.kans(h, 7) * Math.PI;
        rekCos = Math.cos(hoek);
        rekSin = Math.sin(hoek);
        double som = 0, somV = 0;
        for (int i = 0; i < 4; i++) {
            bocht[i] = BOCHT[i] * (weide ? 1.3 : 1.0) * (0.35 + 0.65 * BioModel.kans(h, 10 + i));
            bochtFase[i] = BioModel.kans(h, 20 + i) * 2 * Math.PI;
            som += bocht[i];
        }
        for (int i = 0; i < 3; i++) {
            ongelijk[i] = ONGELIJK[i] * BioModel.kans(h, 30 + i);
            ongelijkFase[i] = BioModel.kans(h, 40 + i) * 2 * Math.PI;
            somV += ongelijk[i];
        }
        buiten = straal * rek * (1 + som) + breed * (1 + somV) + WARP * 1.5 + 2;
        if (buiten > BUITEN_MAX) {
            throw new IllegalStateException("biomes3: a region reaches " + buiten + " blocks from its middle, more than BUITEN_MAX");
        }
    }

    /** The mask must be this high for a lake column (the lake's edge, its fraying and a little room), and for a meadow column. */
    private static final double MEER_VRIJ = DalTerrein.TRAP[3] + 0.02, WEIDE_VRIJ = WolkTerrein.BINNEN + 0.01;
    private static final int RICHTINGEN = 16;

    /**
     * The region a cell's try makes at (x, z), or null when the older regions leave no fitting room there.
     * A dal: how far the lake can reach is measured in sixteen directions; the lake's mean radius is its draw or, if
     * that is too big for the pocket, what three quarters of the directions allow; the middle half must be free all
     * round; and the first third of the valley must have room in most directions.
     */
    private static BioRegio maak(BioModel m, int cx, int cz, long h, boolean weide, double x, double z) {
        if (weide) {
            double straal = WEIDE_STRAAL[0] + (WEIDE_STRAAL[1] - WEIDE_STRAAL[0]) * Math.pow(BioModel.kans(h, 4), WEIDE_MACHT);
            if (m.masker(x, z) < 0.15) {
                return null;
            }
            BioRegio r = new BioRegio(cx, cz, h, true, x, z, straal);
            int mis = 0;
            for (int ring = 0; ring < 2; ring++) {
                int n = ring == 0 ? 10 : 24;
                double deel = ring == 0 ? 0.5 : 0.8;
                for (int i = 0; i < n; i++) {
                    double hoek = (i + 0.5 * ring) * 2 * Math.PI / n, af = r.omtrek(hoek) * deel;
                    if (m.masker(x + Math.cos(hoek) * af, z + Math.sin(hoek) * af) < WEIDE_VRIJ && (ring == 0 || ++mis > 3)) {
                        return null;
                    }
                }
            }
            return r;
        }
        if (m.masker(x, z) < MEER_VRIJ) {
            return null;
        }
        double wens = MEER_STRAAL[0] + (MEER_STRAAL[1] - MEER_STRAAL[0]) * Math.pow(BioModel.kans(h, 4), MEER_MACHT);
        double[] vrij = new double[RICHTINGEN];
        double tot = MEER_STRAAL[1] * 1.45;
        for (int i = 0; i < RICHTINGEN; i++) {
            double c = Math.cos(i * 2 * Math.PI / RICHTINGEN), s = Math.sin(i * 2 * Math.PI / RICHTINGEN), af = 12;
            while (af < tot && m.masker(x + c * af, z + s * af) >= MEER_VRIJ) {
                af += 12;
            }
            vrij[i] = af - 12;
        }
        double[] op = vrij.clone();
        java.util.Arrays.sort(op);
        // (the outline may be cut where the pocket is narrow: a quarter of the directions at most; the middle half never)
        double straal = Math.min(wens, op[RICHTINGEN / 4] - WARP * 0.5 - 6);
        if (straal < MEER_STRAAL[0] || op[0] < 0.5 * straal) {
            return null;
        }
        BioRegio r = new BioRegio(cx, cz, h, false, x, z, straal);
        int mis = 0;
        for (int i = 0; i < 24; i++) {
            double[] p = r.inDal(i * 2 * Math.PI / 24, 0.33);
            if (m.masker(p[0], p[1]) < 0.07 && ++mis > 9) {
                return null;
            }
        }
        return r;
    }

    /** The outline (lake or meadow) in a direction: its distance from the middle. */
    private double omtrek(double hoek) {
        double c = Math.cos(hoek) * rekCos + Math.sin(hoek) * rekSin, s = Math.sin(hoek) * rekCos - Math.cos(hoek) * rekSin;
        double ellips = 1.0 / Math.sqrt(c * c / (rek * rek) + s * s * rek * rek);
        double b = 0;
        for (int i = 0; i < 4; i++) {
            b += bocht[i] * Math.cos((i + 2) * hoek + bochtFase[i]);
        }
        return straal * ellips * (1 + b);
    }

    /** The valley's width in a direction. */
    private double breedte(double hoek) {
        double b = 0;
        for (int i = 0; i < 3; i++) {
            b += ongelijk[i] * Math.cos((i + 1) * hoek + ongelijkFase[i]);
        }
        return breed * (1 + b);
    }

    /**
     * The region's own value at a column (before the older regions cut into it): above 0 inside, rising inward. For a
     * dal {@link DalTerrein#TRAP}[3] at the lake's edge; one block is {@code TRAP[3] / breedte} of it everywhere on a
     * line from the middle, so a distance in blocks is this value over its slope.
     */
    public double eigen(BioModel m, double px, double pz) {
        // (the slow push: two samples of the river noise, read far from where the rivers read it)
        double wx = WARP * Math.max(-1, Math.min(1, m.ruis(BioModel.R_RIVIER, px + 11000, pz + 3000) * 1.4));
        double wz = WARP * Math.max(-1, Math.min(1, m.ruis(BioModel.R_RIVIER, px - 7000, pz - 13000) * 1.4));
        return recht(px + wx - x, pz + wz - z);
    }

    /** {@link #eigen} without the push, from the offset to the middle. */
    private double recht(double dx, double dz) {
        double d = Math.sqrt(dx * dx + dz * dz), hoek = Math.atan2(dz, dx);
        // (close to the middle the direction means nothing: the outline fades to its mean there, far inside the lake / meadow)
        double mate = BioModel.zacht(d / (0.45 * straal));
        double rand = straal + (omtrek(hoek) - straal) * mate;
        if (weide) {
            return WEIDE_HELLING * (rand - d);
        }
        double v = breed + (breedte(hoek) - breed) * mate;
        return DalTerrein.TRAP[3] * (1 + (rand - d) / v);
    }

    /** Is the point within the region's reach (its value can be above 0 only there)? */
    public boolean bereikt(double px, double pz) {
        double dx = px - x, dz = pz - z;
        return dx * dx + dz * dz <= buiten * buiten;
    }

    /** The point of the lake's edge (dal) or of the meadow's rim (Wolkenweide) in a direction, pushed out by {@code verder} blocks (the push of the coordinates left out). */
    public double[] rand(double hoek, double verder) {
        double r = omtrek(hoek) + verder;
        return new double[]{x + Math.cos(hoek) * r, z + Math.sin(hoek) * r};
    }

    /** A point of the valley in a direction: {@code deel} of the way from the lake's edge (0) to the rim (1). */
    public double[] inDal(double hoek, double deel) {
        double r = omtrek(hoek) + breedte(hoek) * deel;
        return new double[]{x + Math.cos(hoek) * r, z + Math.sin(hoek) * r};
    }

    /** About how big the lake (or the meadow) is, in blocks squared. */
    public double oppervlak() {
        return Math.PI * straal * straal;
    }

    // --- the cells ------------------------------------------------------------------------------------------------------------
    private static final Object GEEN = new Object();
    private static final int SOORT_KANDIDAAT = 1, SOORT_WINNAAR = 2;

    /** The candidate of a cell if the older regions leave it whole, else null (cached in the model). */
    private static BioRegio kandidaat(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_KANDIDAAT, cx, cz);
        Object bekend = m.regios.get(sleutel);
        if (bekend != null) {
            return bekend == GEEN ? null : (BioRegio) bekend;
        }
        long h = m.hash(cx, cz, 9101);
        double k = BioModel.kans(h, 0);
        BioRegio r = null;
        if (k < DAL_KANS + WEIDE_KANS) {
            for (int poging = 0; poging < POGINGEN && r == null; poging++) {
                r = maak(m, cx, cz, h, k >= DAL_KANS, cx * (double) CEL + BioModel.kans(h, 50 + 2 * poging) * CEL, cz * (double) CEL + BioModel.kans(h, 51 + 2 * poging) * CEL);
            }
        }
        m.bewaarRegio(sleutel, r == null ? GEEN : r);
        return r;
    }

    /** The region of a cell, or null: its candidate, unless a candidate with a higher draw lies too close (cached in the model). */
    static BioRegio winnaar(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_WINNAAR, cx, cz);
        Object bekend = m.regios.get(sleutel);
        if (bekend != null) {
            return bekend == GEEN ? null : (BioRegio) bekend;
        }
        BioRegio r = kandidaat(m, cx, cz);
        if (r != null) {
            zoek:
            for (int ax = -2; ax <= 2; ax++) {
                for (int az = -2; az <= 2; az++) {
                    if (ax == 0 && az == 0) {
                        continue;
                    }
                    BioRegio b = kandidaat(m, cx + ax, cz + az);
                    if (b == null || b.prioriteit < r.prioriteit || b.prioriteit == r.prioriteit && (ax < 0 || ax == 0 && az < 0)) {
                        continue;
                    }
                    double ruim = r.buiten + b.buiten + (r.weide || b.weide ? TUSSEN_WEIDE : TUSSEN);
                    if ((b.x - r.x) * (b.x - r.x) + (b.z - r.z) * (b.z - r.z) < ruim * ruim) {
                        r = null;
                        break zoek;
                    }
                }
            }
        }
        m.bewaarRegio(sleutel, r == null ? GEEN : r);
        return r;
    }

    /** Every region that can reach into the box [x0, x1] x [z0, z1]. */
    public static List<BioRegio> bij(BioModel m, int x0, int z0, int x1, int z1) {
        List<BioRegio> uit = new ArrayList<>();
        int marge = (int) BUITEN_MAX + 1;
        for (int cx = Math.floorDiv(x0 - marge, CEL); cx <= Math.floorDiv(x1 + marge, CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - marge, CEL); cz <= Math.floorDiv(z1 + marge, CEL); cz++) {
                BioRegio r = winnaar(m, cx, cz);
                if (r != null && r.x + r.buiten >= x0 && r.x - r.buiten <= x1 && r.z + r.buiten >= z0 && r.z - r.buiten <= z1) {
                    uit.add(r);
                }
            }
        }
        return uit;
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s at %d %d (%s %.0f%s)", weide ? "Wolkenweide" : "dal", Math.round(x), Math.round(z),
                weide ? "radius" : "lake radius", straal, weide ? "" : String.format(java.util.Locale.ROOT, ", valley %.0f wide", breed));
    }
}
