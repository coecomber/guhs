package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;

/**
 * biomes3 wereld, the Bloesemmeertje: the shape of the lake in the middle of a dal (pure maths on {@link BioModel}).
 * <p>
 * The lake starts where the valley floor ends ({@link DalTerrein#TRAP}[3]); d = how far past that. Its top water block is
 * at {@link #WATER}, one below the valley floor, so the shore is one step and the rivers of the floor run into it level.
 * The floor slopes from {@link #DIEP_OEVER} at the shore to about {@link #DIEP_MIDDEN} over {@link #DIEP_OVER} of d, with
 * a little relief. Islands come from a grid of {@link #CEL} blocks: per cell at most one LARGE island (irregular, radius
 * 9-13: a beach ring one above the water, flat buildable ground two above, half of them with a small hill on one side;
 * the spot of the big tree, {@link Eiland#boomX}, on the other side) where the lake is wide enough, else maybe a small
 * accent island. Around every island the floor rises to it, so the water is 1-2 deep there.
 * <p>
 * Owner after the kern: the Bloesemmeertje polish agent (this file, {@link MeerVulling}, tools/features/bio_wereld_meer.py).
 */
public final class MeerTerrein {
    // <meer-terrein>
    /** The top water block of every lake (= {@link DalTerrein#HOOGTE}[0] - 1). */
    public static final int WATER = 49;
    public static final double DIEP_OEVER = 1.3, DIEP_MIDDEN = 7.0, DIEP_OVER = 0.05;
    public static final int CEL = 96;
    /** A large island needs the lake this far (in d) around its middle, a small one this far. */
    public static final double GROOT_VANAF = 0.04, KLEIN_VANAF = 0.012;
    public static final double GROOT_KANS = 0.5, KLEIN_KANS = 0.6;
    // </meer-terrein>

    private static final int SOORT = 1;

    /** A lake island. heuvelHoek: the direction (radians) of its hill; the big tree stands on the other side. */
    public record Eiland(int x, int z, double straal, boolean groot, double f1, double f2, boolean heuvel, double heuvelHoek, int boomX, int boomZ) {
        /** The radius in a direction (the irregular outline). */
        public double straalBij(double hoek) {
            return straal * (1 + 0.16 * Math.sin(2 * hoek + f1) + 0.11 * Math.sin(3 * hoek + f2));
        }

        /** How far from the middle anything of the island (its underwater slope included) can lie. */
        public double bereik() {
            return straal * 1.3 + 9;
        }
    }

    /** The islands of a grid cell (cached in the model). */
    public static Eiland[] cel(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return (Eiland[]) bekend;
        }
        long h = m.hash(cx, cz, 4101);
        List<Eiland> uit = new ArrayList<>(2);
        int x = cx * CEL + 14 + (int) (BioModel.kans(h, 0) * (CEL - 28)), z = cz * CEL + 14 + (int) (BioModel.kans(h, 1) * (CEL - 28));
        double d = m.eDal(x, z) - DalTerrein.TRAP[3];
        if (d >= GROOT_VANAF && BioModel.kans(h, 2) < GROOT_KANS) {
            double straal = 9 + 4 * BioModel.kans(h, 3), hoek = BioModel.kans(h, 6) * Math.PI * 2;
            uit.add(new Eiland(x, z, straal, true, BioModel.kans(h, 4) * 6.283, BioModel.kans(h, 5) * 6.283, BioModel.kans(h, 7) < 0.5, hoek,
                    x - (int) Math.round(Math.cos(hoek) * straal * 0.42), z - (int) Math.round(Math.sin(hoek) * straal * 0.42)));
        } else if (d >= KLEIN_VANAF && BioModel.kans(h, 2) < KLEIN_KANS) {
            uit.add(new Eiland(x, z, 2.2 + 1.8 * BioModel.kans(h, 3), false, BioModel.kans(h, 4) * 6.283, BioModel.kans(h, 5) * 6.283, false, 0, x, z));
        }
        // a second, small one elsewhere in the cell
        int x2 = cx * CEL + 6 + (int) (BioModel.kans(h, 8) * (CEL - 12)), z2 = cz * CEL + 6 + (int) (BioModel.kans(h, 9) * (CEL - 12));
        if (BioModel.kans(h, 10) < 0.45 && (uit.isEmpty() || Math.hypot(x2 - x, z2 - z) > uit.get(0).straal() * 1.4 + 8)
                && m.eDal(x2, z2) - DalTerrein.TRAP[3] >= KLEIN_VANAF) {
            uit.add(new Eiland(x2, z2, 1.8 + 1.6 * BioModel.kans(h, 11), false, BioModel.kans(h, 12) * 6.283, BioModel.kans(h, 13) * 6.283, false, 0, x2, z2));
        }
        Eiland[] r = uit.toArray(new Eiland[0]);
        if (m.cellen.size() > 60000) {
            m.cellen.clear();
        }
        m.cellen.put(sleutel, r);
        return r;
    }

    /** Every island that can reach into the box [x0, x1) x [z0, z1). */
    public static List<Eiland> bij(BioModel m, int x0, int z0, int x1, int z1) {
        List<Eiland> uit = new ArrayList<>();
        int bereik = 27;
        for (int cx = Math.floorDiv(x0 - bereik, CEL); cx <= Math.floorDiv(x1 + bereik, CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - bereik, CEL); cz <= Math.floorDiv(z1 + bereik, CEL); cz++) {
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

    /** One lake column: its top solid block, its water and its flags, into the arrays at idx. d = e past the valley floor. */
    static void kolom(BioModel m, int x, int z, double d, List<Eiland> eilanden, int[] h, int[] wat, byte[] vl, int idx) {
        double diepte = DIEP_OEVER + (DIEP_MIDDEN - DIEP_OEVER) * BioModel.zacht(d / DIEP_OVER) + 0.9 * m.ruis(BioModel.R_DETAIL, x * 0.5, z * 0.5);
        int top = WATER - Math.max(1, (int) Math.round(diepte));
        byte v = 0;
        for (Eiland ei : eilanden) {
            double dx = x - ei.x(), dz = z - ei.z(), afstand = Math.sqrt(dx * dx + dz * dz);
            if (afstand > ei.bereik()) {
                continue;
            }
            double r = ei.straalBij(Math.atan2(dz, dx));
            int y;
            if (afstand < r) {
                y = ei.groot() && afstand / r < 0.72 ? WATER + 2 : WATER + 1;
                if (ei.heuvel()) {
                    double hx = dx - Math.cos(ei.heuvelHoek()) * ei.straal() * 0.38, hz = dz - Math.sin(ei.heuvelHoek()) * ei.straal() * 0.38;
                    double q = Math.sqrt(hx * hx + hz * hz) / (ei.straal() * 0.42);
                    if (q < 1) {
                        y = Math.max(y, WATER + 2 + (int) Math.round(3.2 * (1 - q * q)));
                    }
                }
                v |= (byte) (Kaart.EILAND | (ei.groot() ? Kaart.GROOT : 0));
            } else {
                y = WATER - (int) ((afstand - r) * 0.85);
            }
            top = Math.max(top, y);
        }
        h[idx] = top;
        wat[idx] = top >= WATER ? Kaart.GEEN : WATER;
        vl[idx] = top > WATER ? v : 0;
    }

    private MeerTerrein() {
    }
}
