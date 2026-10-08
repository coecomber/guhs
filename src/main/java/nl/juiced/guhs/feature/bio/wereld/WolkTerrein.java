package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.core.Direction;

/**
 * biomes3 wereld, the Wolkenweide: a calm meadow and, above it, a sky full of floating islands, cloud banks and the ways
 * up (pure maths on {@link BioModel}; the islands and stepping stones are solid terrain through the density function,
 * everything else is placed by {@link WolkVulling}).
 * <p>
 * <b>Meadow</b>: the land blends ({@link #RAND}) into a bed at {@link #WEIDE_Y} that rolls +-{@link #GLOOIING} blocks.
 * Where a waterfall arrives it has a pond with a short stream ({@link Plas}).
 * <p>
 * <b>Islands</b> ({@link Eiland}): round, elongated, crescent, double with a natural arch, with a hole; small (8-13 wide),
 * medium (14-20) now and then, rarely larger, and tiny loose rocks. An island is a grid of columns worked out once: per
 * column how far its underside hangs (drip-shaped, with long drip points), a low hill on some medium ones, a tiny pond.
 * <p>
 * <b>Stacks</b> ({@link Stapel}): a grid of {@link #STAPEL_CEL} blocks holds at most one stack, a chain of islands that
 * climbs from 9-13 above the meadow to at most {@link #LAAG} + {@link #HOOG}. Every link of the chain is a way up AND a
 * way down, built so that the jump rule of {@link WolkRoute} holds:
 * <ul>
 *   <li>a <i>wenteltrap</i>: stepping stones ({@link Stap}, 3 x 3, rock or cloud) around a square, each one block higher
 *       and {@link #TRAP_STAP} further (a gap of one block), eight to a turn, for rises of 5-10;</li>
 *   <li>a <i>lift pair</i>: a wolkenlift column from the lower island's rim up to the higher island's rim and a
 *       wolkenstroom column down beside it ({@link Kolom}, 3 x 3 as in the zwevende_eilanden structure) on a cloud
 *       cushion ({@link Kussen}) that also catches whoever steps off the rim, for rises of 14-32. Where the stream has
 *       no room beside the lift it stands at another side of the higher island and goes down to the meadow.</li>
 * </ul>
 * The first island is reached from the meadow by a wenteltrap or a lift pair. Around every stepping stone, column and
 * landing the air is kept free ({@link Stapel#vrij}); a link that does not fit ends the chain, so what stands is always
 * walkable. Between the stacks, loose islands ({@link #cel}) with their own way up, and rocks.
 * <p>
 * <b>Water</b>: some stacks have a tiny pond on a high island whose water spills over the rim ({@link Val}) onto a lower
 * island's catch pool, or onto the meadow's pond. All of it is in the model, so it is placed complete and never floods.
 * <p>
 * <b>Clouds</b> ({@link Wolk}): banks of several flattened blobs from a grid of {@link #WOLK_CEL}, white and some pink,
 * plus a thin cloud sea at {@link #ZEE_HOOGTE} above the meadow.
 * <p>
 * <b>Buildings in the air</b>: {@link Luchtruim} knows where a {@code guhs:bio_plek} structure of kind {@code lucht} may
 * start; nothing is made within its room. A stack is cut off at the first island that would reach into such a room, so
 * a reserved spot costs the top of a stack, never the whole sky.
 */
public final class WolkTerrein {
    // <wolk-terrein>
    public static final double RAND = 0.025;
    /** Islands and clouds only this far (in e) inside the region. */
    public static final double BINNEN = 0.03;
    public static final int WEIDE_Y = 70;
    public static final double GLOOIING = 2.5;
    /** The grid of the loose islands and rocks. */
    public static final int CEL = 36;
    public static final double EILAND_KANS = 0.8, ROTS_KANS = 0.8;
    /** Island tops lie between LAAG and LAAG + HOOG above the meadow. */
    public static final int LAAG = 9, HOOG = 79;
    public static final int TRAP_TOT = 14, TRAP_STAP = 4;
    /** The grid of the stacks, and how far a stack may reach from its middle. */
    public static final int STAPEL_CEL = 84, STAPEL_RUIM = 38;
    public static final double STAPEL_KANS = 0.88, WATER_KANS = 0.6;
    public static final int WOLK_CEL = 48;
    public static final double WOLK_KANS = 0.9, ROZE_KANS = 0.22;
    public static final int ZEE_HOOGTE = 34;
    // </wolk-terrein>

    public static final int ROND = 0, LANG = 1, MAAN = 2, DUBBEL = 3, GAT = 4;
    public static final int ROTSJE = 0, KLEIN = 1, MIDDEL = 2, GROOT = 3;
    private static final int SOORT_EILAND = 2, SOORT_WOLK = 3, SOORT_STAPEL = 4, SOORT_KANDIDAAT = 5;
    private static final Direction[] RICHTINGEN = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    /** The wenteltrap: eight places around a square, and the sides on which each place looks out of the square. */
    private static final int[][] LUS = {{-4, -4}, {0, -4}, {4, -4}, {4, 0}, {4, 4}, {0, 4}, {-4, 4}, {-4, 0}};
    private static final Direction[][] UIT = {{Direction.WEST, Direction.NORTH}, {Direction.NORTH}, {Direction.NORTH, Direction.EAST},
            {Direction.EAST}, {Direction.EAST, Direction.SOUTH}, {Direction.SOUTH}, {Direction.SOUTH, Direction.WEST}, {Direction.WEST}};
    /** How far anything of a loose island (its stair, lift and cushion included) can lie from its middle. */
    static final int BEREIK = 40;
    private static final int GEEN = Integer.MIN_VALUE;

    static final AtomicLong TIJD = new AtomicLong(), KEER = new AtomicLong();

    /** The meadow's top block at (x, z), before ponds and the feet of stairs. */
    public static int grond(BioModel m, int x, int z) {
        return WEIDE_Y + (int) Math.round(GLOOIING * m.ruis(BioModel.R_RIVIER, x * 0.7 + 4000, z * 0.7 - 4000));
    }

    // ==================================================================================================================
    // an island
    // ==================================================================================================================
    /** A floating island. trap/lift/stroom: the side where its stair to the meadow, its lift or its stream stands, or null. */
    public static final class Eiland {
        public int x, z, top;
        public final int vorm, klasse;
        public final double rx, rz;
        public final boolean rots;
        final double cos, sin, f1, f2;
        /** Half the size of the column grid, and per column ({@code (dx + r) + (dz + r) * (2r + 1)}): how many blocks hang under
         * the top block (-1: no island here), and the top relative to {@link #top} (a hill: 1-3, a pond's bed: -1). */
        public final int r;
        final short[] diep;
        final byte[] bov;
        /** Ponds: columns whose top is water. */
        final boolean[] nat;
        /** The drip points (dx, dz pairs) that get a crystal tip. */
        public final int[] punten;
        public final long zaad;
        public Direction trap, lift, stroom;
        public int trapStappen;
        /** The lift's and stream's middle column and ground height (valid when lift / stroom is set). */
        public int liftX, liftZ, liftGrond, stroomX, stroomZ, stroomGrond;
        int[] trapX, trapZ;
        /** The stack this island belongs to, and its place in the chain (0 = the lowest). */
        public Stapel stapel;
        public int niveau;
        /** Tree spots (dx, dz, kind: 0 small pluizenboom, 1 small guhbloesem, 2 big). */
        public final List<int[]> bomen = new ArrayList<>(4);

        Eiland(long h, int klasse, int vormWens) {
            this.zaad = h;
            this.klasse = klasse;
            this.rots = klasse == ROTSJE;
            double a = BioModel.kans(h, 3), b = BioModel.kans(h, 4);
            int v;
            double rxx, rzz;
            if (rots) {
                v = ROND;
                rxx = 1.7 + 1.5 * a;
                rzz = rxx * (0.75 + 0.5 * b);
            } else {
                double rr = klasse == KLEIN ? 4.2 + 2.3 * a : klasse == MIDDEL ? 7 + 3 * a : 11 + 2 * a;
                if (vormWens >= 0) {
                    v = vormWens;
                } else {
                    double s = BioModel.kans(h, 5);
                    v = s < 0.36 ? ROND : s < 0.56 ? LANG : s < 0.72 ? MAAN : s < 0.88 ? DUBBEL : GAT;
                }
                if (v == LANG) {
                    rxx = rr * 1.4;
                    rzz = Math.max(2.7, rr * 0.55);
                } else if (v == MAAN || v == GAT) {
                    rxx = rzz = Math.max(rr, 7.2);
                } else if (v == DUBBEL) {
                    rr = Math.max(rr, 6);
                    rxx = rr * 1.5;
                    rzz = rr * 1.05;
                } else {
                    rxx = rr;
                    rzz = rr * (0.78 + 0.44 * b);
                }
            }
            vorm = v;
            rx = rxx;
            rz = rzz;
            double hoek = BioModel.kans(h, 6) * Math.PI * 2;
            cos = Math.cos(hoek);
            sin = Math.sin(hoek);
            f1 = BioModel.kans(h, 7) * 6.283;
            f2 = BioModel.kans(h, 8) * 6.283;
            r = (int) (Math.max(rx, rz) * 1.4) + 2;
            int w = 2 * r + 1;
            diep = new short[w * w];
            bov = new byte[w * w];
            nat = new boolean[w * w];
            // the drip points: a few long ones, more and longer under a bigger island
            double klein = Math.min(rx, rz);
            int drups = rots ? 1 : klasse == KLEIN ? 2 + (int) (BioModel.kans(h, 9) * 2) : klasse == MIDDEL ? 3 + (int) (BioModel.kans(h, 9) * 3)
                    : 5 + (int) (BioModel.kans(h, 9) * 2);
            double[] drup = new double[drups * 3];
            for (int i = 0; i < drups; i++) {
                double ha = BioModel.kans(h, 20 + i * 3) * 6.283, d = i == 0 && (vorm == ROND || vorm == LANG) ? 0.1 : 0.15 + BioModel.kans(h, 21 + i * 3) * 0.45;
                double u = Math.cos(ha) * d * rx, vv = Math.sin(ha) * d * rz;
                if (vorm == DUBBEL) {
                    u = (i % 2 == 0 ? 0.58 : -0.6) * rx + Math.cos(ha) * d * 0.4 * rx;
                    vv = Math.sin(ha) * d * 0.4 * rz;
                } else if (vorm == GAT) {
                    u = Math.cos(ha) * 0.66 * rx;
                    vv = Math.sin(ha) * 0.66 * rz;
                } else if (vorm == MAAN) {
                    u = -Math.abs(Math.cos(ha)) * (0.3 + 0.4 * d) * rx;
                    vv = Math.sin(ha) * 0.5 * rz;
                }
                drup[i * 3] = u * cos - vv * sin;
                drup[i * 3 + 1] = u * sin + vv * cos;
                double lang = BioModel.kans(h, 22 + i * 3);
                drup[i * 3 + 2] = rots ? 1 + 1.5 * lang : klasse == KLEIN ? 2.5 + 3.5 * lang : klasse == MIDDEL ? 5 + 7 * lang : 6 + 8 * lang;
                if (i == 0 && klasse >= MIDDEL) {
                    drup[2] += 2;
                }
                // (a narrow island gets short points, or it would become a pillar)
                drup[i * 3 + 2] = Math.min(drup[i * 3 + 2], 1.5 * klein + 1);
            }
            double bak = rots ? 0.9 * klein + 0.4 : klasse == KLEIN ? 0.95 * klein + 1.4 : 1.2 * klein + 2.2;
            // a low hill on some medium and large islands, away from the middle (the middle stays the island's flat top)
            boolean heuvel = klasse >= MIDDEL && (vorm == ROND || vorm == LANG || vorm == GAT) && BioModel.kans(h, 14) < 0.5;
            double hh = 1 + (int) (BioModel.kans(h, 15) * 3), hr = 2 + 1.5 * hh, ha2 = BioModel.kans(h, 16) * 6.283;
            double hd = Math.max(hr + 1.5, 0.5 * klein);
            double hx = Math.cos(ha2) * hd, hz = Math.sin(ha2) * hd;
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    int i = (dx + r) + (dz + r) * w;
                    double f = binnen(dx, dz);
                    if (f <= 0) {
                        diep[i] = -1;
                        continue;
                    }
                    if (f == BOOG) {
                        // the arch of a double island: one block thick in the middle of the span, two at its ends
                        double u = dx * cos + dz * sin;
                        diep[i] = (short) (Math.abs(u + 0.05 * rx) > 0.07 * rx ? 1 : 0);
                        continue;
                    }
                    double d = 0.55 + bak * Math.pow(Math.min(1, f), 0.72) * (1 + 0.12 * Math.sin(dx * 0.9 + f1) * Math.sin(dz * 0.8 + f2));
                    if (f > 0.1) {
                        for (int k = 0; k < drup.length; k += 3) {
                            double qa = dx - drup[k], qb = dz - drup[k + 1];
                            // (a cone that narrows evenly: its last single column is never more than two or three long)
                            double breed = 1.3 + drup[k + 2] * 0.24;
                            double q = 1 - Math.sqrt(qa * qa + qb * qb) / breed;
                            if (q > 0) {
                                d += drup[k + 2] * q;
                            }
                        }
                    }
                    diep[i] = (short) d;
                    if (heuvel && f > 0.3) {
                        double q = 1 - Math.hypot(dx - hx, dz - hz) / hr;
                        if (q > 0) {
                            bov[i] = (byte) Math.round(hh * BioModel.zacht(q));
                        }
                    } else if (rots && rx > 2.5 && Math.abs(dx) + Math.abs(dz) <= 1 && BioModel.kans(h, 17) < 0.6) {
                        bov[i] = 1;
                    }
                }
            }
            List<Integer> p = new ArrayList<>();
            for (int k = 0; k < drup.length; k += 3) {
                int dx = (int) Math.round(drup[k]), dz = (int) Math.round(drup[k + 1]);
                if (is(dx, dz) && diep[(dx + r) + (dz + r) * w] >= 3) {
                    p.add(dx);
                    p.add(dz);
                }
            }
            punten = p.stream().mapToInt(Integer::intValue).toArray();
        }

        private static final double BOOG = 0.0625;

        /** How deep inside the outline a column lies: above 0 inside (1 = the middle), 0 or less outside. */
        public double binnen(double dx, double dz) {
            double uu = dx * cos + dz * sin, v = -dx * sin + dz * cos;
            if (vorm == DUBBEL) {
                double f = Math.max(bol(uu - 0.58 * rx, v, 0.5), bol(uu + 0.6 * rx, v, 0.42));
                // the natural arch between the two halves
                return f <= 0 && Math.abs(v) <= 1.6 && Math.abs(uu) < 0.6 * rx ? BOOG : f;
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

        int index(int dx, int dz) {
            return (dx + r) + (dz + r) * (2 * r + 1);
        }

        /** Is the column (relative to the middle) part of the island? */
        public boolean is(int dx, int dz) {
            return dx >= -r && dx <= r && dz >= -r && dz <= r && diep[index(dx, dz)] >= 0;
        }

        /** The top block of a column (world coordinates), or {@link Kaart#GEEN}. A pond column: its bed. */
        public int boven(int wx, int wz) {
            int dx = wx - x, dz = wz - z;
            return is(dx, dz) ? top + bov[index(dx, dz)] : Kaart.GEEN;
        }

        /** The lowest block of a column (world coordinates), or {@link Kaart#GEEN}. */
        public int onder(int wx, int wz) {
            int dx = wx - x, dz = wz - z;
            return is(dx, dz) ? top - diep[index(dx, dz)] : Kaart.GEEN;
        }

        /** Is the column a pond (water at {@link #top}, its bed one below)? */
        public boolean vijver(int wx, int wz) {
            int dx = wx - x, dz = wz - z;
            return is(dx, dz) && nat[index(dx, dz)];
        }

        /** The outermost column of the island along a side, on the row {@code rij} columns to the right of the middle line; GEEN: none. */
        int reik(Direction d, int rij) {
            Direction p = d.getClockWise();
            for (int a = r; a >= -r; a--) {
                if (is(d.getStepX() * a + p.getStepX() * rij, d.getStepZ() * a + p.getStepZ() * rij)) {
                    return a;
                }
            }
            return GEEN;
        }

        /** {@link #reik} over three rows. */
        int reik3(Direction d, int rij) {
            return Math.max(reik(d, rij - 1), Math.max(reik(d, rij), reik(d, rij + 1)));
        }

        /** How far the island reaches from its middle in a direction (the last column that is island). */
        public int rand(Direction d) {
            return Math.max(0, reik(d, 0));
        }

        /** Makes a column flat (no hill): where a lift, a stream or a stair arrives. */
        void vlak(int dx, int dz) {
            if (is(dx, dz) && bov[index(dx, dz)] > 0) {
                bov[index(dx, dz)] = 0;
            }
        }

        double straal() {
            return Math.max(rx, rz) * 1.3 + 1;
        }

        int zet(int x, int z, int top) {
            this.x = x;
            this.z = z;
            this.top = top;
            return top;
        }
    }

    // ==================================================================================================================
    // the parts of a stack
    // ==================================================================================================================
    /** A stepping stone: 3 x 3 around (x, z), its top block at {@code top}; of rock (terrain) or of cloud (placed). */
    public record Stap(int x, int z, int top, boolean wolk, int hoeken) {
    }

    /** A 3 x 3 lift column around (x, z): pads at {@code voet}, stream from voet + 1 to {@code boven}. kijk: where it puffs you off. */
    public record Kolom(int x, int z, int voet, int boven, Direction kijk, boolean omlaag, boolean los) {
    }

    /** A cushion of cloud: a round patch at height y, wherever nothing else is. */
    public record Kussen(int x, int y, int z, double straal) {
    }

    /** Falling water in one column: flowing out of a pond at {@code boven}, falling down to {@code onder} (the block above the catch pool). */
    public record Val(int x, int z, int boven, int onder) {
    }

    /** Meadow ground that is made level: every column within {@code straal} (Chebyshev) of (x, z) becomes y (only upward unless exact). */
    public record Terp(int x, int z, int straal, int y, boolean exact) {
    }

    /** A box of blocks, corners included. */
    public record Doos(int x0, int y0, int z0, int x1, int y1, int z1) {
        boolean snijdt(Doos o) {
            return x0 <= o.x1 && x1 >= o.x0 && y0 <= o.y1 && y1 >= o.y0 && z0 <= o.z1 && z1 >= o.z0;
        }

        public boolean bevat(int x, int y, int z) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
        }
    }

    /** The pond and stream on the meadow where a waterfall arrives: water columns (x, z, depth) and the water's top block. */
    public static final class Plas {
        public final int water;
        public final int[] kolommen;
        public final int x0, z0, x1, z1;

        Plas(int water, int[] kolommen) {
            this.water = water;
            this.kolommen = kolommen;
            int ax = Integer.MAX_VALUE, az = Integer.MAX_VALUE, bx = Integer.MIN_VALUE, bz = Integer.MIN_VALUE;
            for (int i = 0; i < kolommen.length; i += 3) {
                ax = Math.min(ax, kolommen[i]);
                bx = Math.max(bx, kolommen[i]);
                az = Math.min(az, kolommen[i + 1]);
                bz = Math.max(bz, kolommen[i + 1]);
            }
            x0 = ax;
            z0 = az;
            x1 = bx;
            z1 = bz;
        }

        /** The depth of the water in a column, 0: not water. */
        public int diepte(int x, int z) {
            if (x < x0 || x > x1 || z < z0 || z > z1) {
                return 0;
            }
            for (int i = 0; i < kolommen.length; i += 3) {
                if (kolommen[i] == x && kolommen[i + 1] == z) {
                    return kolommen[i + 2];
                }
            }
            return 0;
        }
    }

    /** A stack of islands with its ways up and down (or one loose island with its own way up: {@link #los}). */
    public static final class Stapel {
        public final int x, z;
        public final boolean los;
        public final List<Eiland> eilanden = new ArrayList<>();
        public final List<Stap> stappen = new ArrayList<>();
        public final List<Kolom> kolommen = new ArrayList<>();
        public final List<Kussen> kussens = new ArrayList<>();
        public final List<Val> vallen = new ArrayList<>();
        public final List<Terp> terpen = new ArrayList<>();
        /** Air that must stay free: above stepping stones and landings, in the lift columns. */
        public final List<Doos> vrij = new ArrayList<>();
        final List<Doos> vast = new ArrayList<>();
        public Plas plas;
        /** How far anything of the stack lies from (x, z). */
        public int straal;

        Stapel(int x, int z, boolean los) {
            this.x = x;
            this.z = z;
            this.los = los;
        }

        /** The highest island. */
        public Eiland hoogste() {
            Eiland h = null;
            for (Eiland e : eilanden) {
                h = h == null || e.top > h.top ? e : h;
            }
            return h;
        }

        /** Must the air at this block stay free (a route passes)? */
        public boolean vrij(int x, int y, int z) {
            for (Doos d : vrij) {
                if (d.bevat(x, y, z)) {
                    return true;
                }
            }
            return false;
        }

        /** Does water fall through this column (at any height)? */
        public boolean valt(int x, int z) {
            for (Val v : vallen) {
                if (v.x == x && v.z == z) {
                    return true;
                }
            }
            return false;
        }

        private int[] merk() {
            return new int[]{eilanden.size(), stappen.size(), kolommen.size(), kussens.size(), terpen.size(), vrij.size(), vast.size()};
        }

        private void terug(int[] m) {
            kap(eilanden, m[0]);
            kap(stappen, m[1]);
            kap(kolommen, m[2]);
            kap(kussens, m[3]);
            kap(terpen, m[4]);
            kap(vrij, m[5]);
            kap(vast, m[6]);
        }

        private static void kap(List<?> l, int n) {
            while (l.size() > n) {
                l.remove(l.size() - 1);
            }
        }
    }

    // ==================================================================================================================
    // building a stack
    // ==================================================================================================================
    static boolean raakt(Eiland e, Doos d) {
        if (d.y1 < e.top - 40 || d.y0 > e.top + 3) {
            return false;
        }
        for (int x = Math.max(d.x0, e.x - e.r); x <= Math.min(d.x1, e.x + e.r); x++) {
            for (int z = Math.max(d.z0, e.z - e.r); z <= Math.min(d.z1, e.z + e.r); z++) {
                int i = e.index(x - e.x, z - e.z);
                if (e.diep[i] >= 0 && e.top + e.bov[i] >= d.y0 && e.top - e.diep[i] <= d.y1) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Two islands above each other need five blocks of air between them. */
    private static boolean teDicht(Eiland a, Eiland b) {
        Eiland laag = a.top <= b.top ? a : b, hoog = laag == a ? b : a;
        for (int x = Math.max(laag.x - laag.r, hoog.x - hoog.r); x <= Math.min(laag.x + laag.r, hoog.x + hoog.r); x++) {
            for (int z = Math.max(laag.z - laag.r, hoog.z - hoog.r); z <= Math.min(laag.z + laag.r, hoog.z + hoog.r); z++) {
                int il = laag.index(x - laag.x, z - laag.z), ih = hoog.index(x - hoog.x, z - hoog.z);
                if (laag.diep[il] >= 0 && hoog.diep[ih] >= 0 && hoog.top - hoog.diep[ih] - (laag.top + laag.bov[il]) < 6) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean inRegio(BioModel m, int x, int z, int marge) {
        return m.eWeide(x, z) >= BINNEN && m.eWeide(x - marge, z) > RAND && m.eWeide(x + marge, z) > RAND && m.eWeide(x, z - marge) > RAND
                && m.eWeide(x, z + marge) > RAND;
    }

    /** Does what was added since the mark fit: inside the region and the stack's room, in free air, nothing in a route's way? */
    private static boolean klopt(BioModel m, Stapel s, Eiland nieuw, int[] merk, int ruim) {
        if (nieuw != null) {
            int st = (int) nieuw.straal();
            if (Math.hypot(nieuw.x - s.x, nieuw.z - s.z) + st > ruim || !inRegio(m, nieuw.x, nieuw.z, st + 2) || Luchtruim.bezet(nieuw.x, nieuw.z, st + 2)) {
                return false;
            }
            for (Doos d : s.vrij) {
                if (raakt(nieuw, d)) {
                    return false;
                }
            }
            for (Doos d : s.vast) {
                if (raakt(nieuw, d)) {
                    return false;
                }
            }
            for (Eiland e : s.eilanden) {
                if (e != nieuw && teDicht(e, nieuw)) {
                    return false;
                }
            }
        }
        for (int i = merk[5]; i < s.vrij.size(); i++) {
            Doos d = s.vrij.get(i);
            for (Eiland e : s.eilanden) {
                if (e != nieuw && raakt(e, d)) {
                    return false;
                }
            }
            for (Doos v : s.vast) {
                if (v.snijdt(d)) {
                    return false;
                }
            }
        }
        for (int i = merk[6]; i < s.vast.size(); i++) {
            Doos v = s.vast.get(i);
            for (Eiland e : s.eilanden) {
                if (e != nieuw && raakt(e, v)) {
                    return false;
                }
            }
            for (Doos d : s.vrij) {
                if (v.snijdt(d)) {
                    return false;
                }
            }
        }
        for (int i = merk[1]; i < s.stappen.size(); i++) {
            Stap p = s.stappen.get(i);
            if (Math.hypot(p.x - s.x, p.z - s.z) + 3 > ruim || m.eWeide(p.x, p.z) < BINNEN || Luchtruim.bezet(p.x, p.z, 3)) {
                return false;
            }
        }
        for (int i = merk[2]; i < s.kolommen.size(); i++) {
            Kolom k = s.kolommen.get(i);
            if (Math.hypot(k.x - s.x, k.z - s.z) + 5 > ruim || !inRegio(m, k.x, k.z, 5) || Luchtruim.bezet(k.x, k.z, 5)) {
                return false;
            }
        }
        return true;
    }

    private static void stap(Stapel s, int x, int z, int top, boolean wolk, long h) {
        s.stappen.add(new Stap(x, z, top, wolk, (int) (BioModel.kans(h, 300 + s.stappen.size()) * 256)));
        s.vast.add(new Doos(x - 1, top - 2, z - 1, x + 1, top, z + 1));
        s.vrij.add(new Doos(x - 1, top + 1, z - 1, x + 1, top + 4, z + 1));
    }

    /** The air a jump from one stepping stone to the next (or to a rim) passes through: the gap between them. */
    private static void sprong(Stapel s, int x, int z, int top, int nx, int nz) {
        int dx = Integer.signum(nx - x), dz = Integer.signum(nz - z);
        int gx = x + 2 * dx, gz = z + 2 * dz;
        s.vrij.add(new Doos(gx - Math.abs(dz), top + 1, gz - Math.abs(dx), gx + Math.abs(dz), top + 5, gz + Math.abs(dx)));
    }

    private static int[] lusPlekken(Direction naar) {
        return switch (naar) {
            case WEST -> new int[]{7, 0, 6};
            case NORTH -> new int[]{1, 0, 2};
            case EAST -> new int[]{3, 2, 4};
            default -> new int[]{5, 4, 6};
        };
    }

    /** The landing of a stair or a lift on an island's rim: a few flat columns with free air above them. */
    private static void landing(Stapel s, Eiland e, Direction kant, int diepte) {
        int ext = e.reik3(kant, 0);
        Direction p = kant.getClockWise();
        for (int i = 0; i <= diepte; i++) {
            for (int q = -1; q <= 1; q++) {
                e.vlak(kant.getStepX() * (ext - i) + p.getStepX() * q, kant.getStepZ() * (ext - i) + p.getStepZ() * q);
            }
        }
        int ax = e.x + kant.getStepX() * ext, az = e.z + kant.getStepZ() * ext, bx = e.x + kant.getStepX() * (ext - diepte), bz = e.z + kant.getStepZ() * (ext - diepte);
        s.vrij.add(new Doos(Math.min(ax, bx) - Math.abs(p.getStepX()), e.top + 1, Math.min(az, bz) - Math.abs(p.getStepZ()),
                Math.max(ax, bx) + Math.abs(p.getStepX()), e.top + 4, Math.max(az, bz) + Math.abs(p.getStepZ())));
    }

    /** Are the columns just inside the rim there (so a landing has ground)? */
    private static boolean rimVast(Eiland e, Direction kant, int diepte) {
        int ext = e.reik3(kant, 0);
        if (ext == GEEN) {
            return false;
        }
        Direction p = kant.getClockWise();
        for (int i = 1; i <= diepte; i++) {
            for (int q = -1; q <= 1; q++) {
                if (!e.is(kant.getStepX() * (ext - i) + p.getStepX() * q, kant.getStepZ() * (ext - i) + p.getStepZ() * q)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * A wenteltrap from island a up to island b (b's shape is known, its place follows): stepping stones, each one block
     * higher than the last, around a square. False (and nothing added) when it does not fit.
     */
    private static boolean wentel(BioModel m, Stapel s, Eiland a, Eiland b, int rijs, Direction d, long h, int poging) {
        int[] merk = s.merk();
        if (!rimVast(a, d, 1)) {
            return false;
        }
        int ext = a.reik3(d, 0);
        int[] keus = lusPlekken(d.getOpposite());
        int i1 = keus[(int) (BioModel.kans(h, 40 + poging) * 3)], draai = BioModel.kans(h, 41 + poging) < 0.5 ? 1 : -1;
        int sx = a.x + d.getStepX() * (ext + 3), sz = a.z + d.getStepZ() * (ext + 3);
        int hx = sx - LUS[i1][0], hz = sz - LUS[i1][1];
        int stijl = (int) (BioModel.kans(h, 42) * 4);
        landing(s, a, d, 1);
        int n = rijs - 1, px = sx, pz = sz, il = i1;
        s.vrij.add(new Doos(sx - d.getStepX() * 2 - Math.abs(d.getStepZ()), a.top + 1, sz - d.getStepZ() * 2 - Math.abs(d.getStepX()),
                sx - d.getStepX() * 2 + Math.abs(d.getStepZ()), a.top + 5, sz - d.getStepZ() * 2 + Math.abs(d.getStepX())));
        for (int k = 1; k <= n; k++) {
            il = Math.floorMod(i1 + draai * (k - 1), 8);
            int x = hx + LUS[il][0], z = hz + LUS[il][1];
            boolean wolk = stijl == 1 ? k % 2 == 0 : stijl == 2 ? k % 3 == 0 : stijl == 3;
            stap(s, x, z, a.top + k, wolk, h);
            if (k > 1) {
                sprong(s, px, pz, a.top + k - 1, x, z);
            }
            px = x;
            pz = z;
        }
        Direction[] uit = UIT[il];
        int eerst = (int) (BioModel.kans(h, 43 + poging) * uit.length);
        for (int t = 0; t < uit.length; t++) {
            Direction e = uit[(eerst + t) % uit.length];
            int extB = b.reik3(e.getOpposite(), 0);
            if (extB == GEEN || !rimVast(b, e.getOpposite(), 1)) {
                continue;
            }
            int[] m2 = s.merk();
            b.zet(px + e.getStepX() * (3 + extB), pz + e.getStepZ() * (3 + extB), a.top + rijs);
            sprong(s, px, pz, a.top + n, px + e.getStepX() * 4, pz + e.getStepZ() * 4);
            landing(s, b, e.getOpposite(), 1);
            if (klopt(m, s, b, merk, STAPEL_RUIM)) {
                return true;
            }
            s.terug(m2);
        }
        s.terug(merk);
        return false;
    }

    /**
     * A lift pair from island a up to island b: a wolkenlift column on a's rim to b's rim, and a wolkenstroom column down
     * beside it on a cushion of cloud. False (and nothing added) when it does not fit.
     */
    private static boolean liftpaar(BioModel m, Stapel s, Eiland a, Eiland b, int rijs, Direction d, long h, int poging) {
        int[] merk = s.merk();
        int extA = a.reik(d, 0), extB = b.reik3(d.getOpposite(), 0);
        if (extA == GEEN || extA < 2 || extB == GEEN || !rimVast(b, d.getOpposite(), 3)) {
            return false;
        }
        Direction p = d.getClockWise();
        int lx = a.x + d.getStepX() * (extA - 1), lz = a.z + d.getStepZ() * (extA - 1);
        b.zet(lx + d.getStepX() * (extB + 2), lz + d.getStepZ() * (extB + 2), a.top + rijs);
        for (int i = -2; i <= 2; i++) {
            for (int q = -2; q <= 2; q++) {
                a.vlak(lx - a.x + i, lz - a.z + q);
            }
        }
        s.kolommen.add(new Kolom(lx, lz, a.top, b.top + 2, d, false, false));
        s.kussens.add(new Kussen(lx, a.top, lz, 2.9));
        s.vrij.add(new Doos(lx - 2, a.top + 1, lz - 2, lx + 2, a.top + 4, lz + 2));
        s.vrij.add(new Doos(lx - 1, a.top + 1, lz - 1, lx + 1, b.top + 5, lz + 1));
        landing(s, b, d.getOpposite(), 3);
        // the stream down, four to a side
        int eerst = BioModel.kans(h, 50 + poging) < 0.5 ? 1 : -1;
        boolean stroom = false;
        for (int t = 0; t < 2 && !stroom; t++) {
            int kant = t == 0 ? eerst : -eerst;
            // (rows are counted to the right of b's side that looks back at a: the other way round than along d)
            int extS = b.reik3(d.getOpposite(), -4 * kant);
            if (extS == GEEN) {
                continue;
            }
            int voor = extB + 2 - extS;
            int sx = lx + p.getStepX() * 4 * kant + d.getStepX() * (voor - 2), sz = lz + p.getStepZ() * 4 * kant + d.getStepZ() * (voor - 2);
            // the pad must lie within a step of the lower island
            boolean bij = false;
            for (int i = -5; i <= 5 && !bij; i++) {
                for (int q = -5; q <= 5 && !bij; q++) {
                    bij = i * i + q * q <= 20 && a.is(sx + i - a.x, sz + q - a.z);
                }
            }
            if (!bij) {
                continue;
            }
            int[] m2 = s.merk();
            for (int i = -2; i <= 2; i++) {
                for (int q = -2; q <= 2; q++) {
                    a.vlak(sx - a.x + i, sz - a.z + q);
                }
            }
            s.kolommen.add(new Kolom(sx, sz, a.top, b.top + 1, d, true, false));
            s.kussens.add(new Kussen(sx, a.top, sz, 4.4));
            s.vrij.add(new Doos(sx - 2, a.top + 1, sz - 2, sx + 2, a.top + 4, sz + 2));
            s.vrij.add(new Doos(sx - 1, a.top + 1, sz - 1, sx + 1, b.top + 4, sz + 1));
            if (klopt(m, s, null, m2, STAPEL_RUIM)) {
                stroom = true;
            } else {
                s.terug(m2);
            }
        }
        // no room beside the lift: a stream from another side of b all the way down to the meadow
        for (int t = 0; t < 12 && !stroom; t++) {
            Direction e = RICHTINGEN[(t + (int) (BioModel.kans(h, 52) * 4)) % 4];
            int rij = t < 4 ? 0 : t < 8 ? 3 : -3, ext = b.reik3(e, rij);
            if (ext == GEEN) {
                continue;
            }
            Direction pe = e.getClockWise();
            int sx = b.x + e.getStepX() * (ext + 2) + pe.getStepX() * rij, sz = b.z + e.getStepZ() * (ext + 2) + pe.getStepZ() * rij, sg = grond(m, sx, sz);
            int[] m2 = s.merk();
            s.kolommen.add(new Kolom(sx, sz, sg, b.top + 1, e, true, false));
            s.vrij.add(new Doos(sx - 1, sg + 1, sz - 1, sx + 1, b.top + 4, sz + 1));
            s.terpen.add(new Terp(sx, sz, 1, sg, true));
            if (klopt(m, s, null, m2, STAPEL_RUIM)) {
                stroom = true;
            } else {
                s.terug(m2);
            }
        }
        // (a lift without a way down is not built)
        if (stroom && klopt(m, s, b, merk, STAPEL_RUIM)) {
            return true;
        }
        s.terug(merk);
        return false;
    }

    /** The wenteltrap from an island down to the meadow (the island's place is known). */
    private static boolean trapNaarWeide(BioModel m, Stapel s, Eiland e, Direction d, long h, int ruim) {
        int[] merk = s.merk();
        if (!rimVast(e, d, 1)) {
            return false;
        }
        int ext = e.reik3(d, 0);
        int[] keus = lusPlekken(d.getOpposite());
        int i1 = keus[(int) (BioModel.kans(h, 60) * 3)], draai = BioModel.kans(h, 61) < 0.5 ? 1 : -1;
        int sx = e.x + d.getStepX() * (ext + 3), sz = e.z + d.getStepZ() * (ext + 3);
        int hx = sx - LUS[i1][0], hz = sz - LUS[i1][1];
        landing(s, e, d, 1);
        s.vrij.add(new Doos(sx - d.getStepX() * 2 - Math.abs(d.getStepZ()), e.top, sz - d.getStepZ() * 2 - Math.abs(d.getStepX()),
                sx - d.getStepX() * 2 + Math.abs(d.getStepZ()), e.top + 4, sz - d.getStepZ() * 2 + Math.abs(d.getStepX())));
        int max = e.top - (WEIDE_Y - 4);
        int[] tx = new int[max + 1], tz = new int[max + 1];
        int stappen = 0, px = sx, pz = sz;
        for (int k = 1; k <= max; k++) {
            int il = Math.floorMod(i1 + draai * (k - 1), 8);
            int x = hx + LUS[il][0], z = hz + LUS[il][1];
            if (e.top - k <= grond(m, x, z)) {
                break;
            }
            stap(s, x, z, e.top - k, false, h);
            if (k > 1) {
                sprong(s, x, z, e.top - k, px, pz);
            }
            tx[k - 1] = x;
            tz[k - 1] = z;
            stappen = k;
            px = x;
            pz = z;
        }
        if (stappen == 0 || !klopt(m, s, null, merk, ruim)) {
            s.terug(merk);
            return false;
        }
        // the foot: the meadow around the lowest stone lies one below it
        s.terpen.add(new Terp(px, pz, 3, e.top - stappen - 1, false));
        e.trap = d;
        e.trapStappen = stappen;
        e.trapX = tx;
        e.trapZ = tz;
        return true;
    }

    /** A lift pair from the meadow to an island (the island's place is known). */
    private static boolean liftVanWeide(BioModel m, Stapel s, Eiland e, Direction kant, long h, int ruim) {
        int[] merk = s.merk();
        int ext = e.reik3(kant, 0);
        if (ext == GEEN || !rimVast(e, kant, 3)) {
            return false;
        }
        Direction p = kant.getClockWise();
        int lx = e.x + kant.getStepX() * (ext + 2), lz = e.z + kant.getStepZ() * (ext + 2), lg = grond(m, lx, lz);
        s.kolommen.add(new Kolom(lx, lz, lg, e.top + 2, kant.getOpposite(), false, s.los));
        s.vrij.add(new Doos(lx - 1, lg + 1, lz - 1, lx + 1, e.top + 5, lz + 1));
        s.terpen.add(new Terp(lx, lz, 1, lg, true));
        landing(s, e, kant, 3);
        if (!klopt(m, s, null, merk, ruim)) {
            s.terug(merk);
            return false;
        }
        e.lift = kant;
        e.liftX = lx;
        e.liftZ = lz;
        e.liftGrond = lg;
        int eerst = BioModel.kans(h, 70) < 0.5 ? 1 : -1;
        for (int t = 0; t < 2; t++) {
            int zij = t == 0 ? eerst : -eerst;
            int extS = e.reik3(kant, 4 * zij);
            if (extS == GEEN) {
                continue;
            }
            int[] m2 = s.merk();
            int sx = e.x + p.getStepX() * 4 * zij + kant.getStepX() * (extS + 2), sz = e.z + p.getStepZ() * 4 * zij + kant.getStepZ() * (extS + 2);
            int sg = grond(m, sx, sz);
            s.kolommen.add(new Kolom(sx, sz, sg, e.top + 1, kant, true, s.los));
            s.vrij.add(new Doos(sx - 1, sg + 1, sz - 1, sx + 1, e.top + 4, sz + 1));
            s.terpen.add(new Terp(sx, sz, 1, sg, true));
            if (klopt(m, s, null, m2, ruim)) {
                e.stroom = kant;
                e.stroomX = sx;
                e.stroomZ = sz;
                e.stroomGrond = sg;
                break;
            }
            s.terug(m2);
        }
        return true;
    }

    /** The way from the meadow to the lowest island: a wenteltrap when it is low, else a lift pair. */
    private static boolean ingang(BioModel m, Stapel s, Eiland e, long h, int ruim) {
        int eerste = (int) (BioModel.kans(h, 10) * 4);
        int hoogte = e.top - grond(m, e.x, e.z);
        for (int t = 0; t < 4; t++) {
            Direction d = RICHTINGEN[(eerste + t) % 4];
            if (hoogte <= TRAP_TOT ? trapNaarWeide(m, s, e, d, h, ruim) : liftVanWeide(m, s, e, d, h, ruim)) {
                return true;
            }
        }
        return false;
    }

    private static int klasse(long h, int n) {
        double k = BioModel.kans(h, n);
        return k < 0.50 ? KLEIN : k < 0.92 ? MIDDEL : GROOT;
    }

    /** The stack of a grid cell (cached in the model); never null, maybe without islands. */
    public static Stapel stapel(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_STAPEL, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return (Stapel) bekend;
        }
        long h = m.hash(cx, cz, 5301);
        int vrijheid = STAPEL_CEL - 2 * STAPEL_RUIM - 4;
        int ax = cx * STAPEL_CEL + STAPEL_RUIM + 2 + (int) (BioModel.kans(h, 1) * vrijheid), az = cz * STAPEL_CEL + STAPEL_RUIM + 2 + (int) (BioModel.kans(h, 2) * vrijheid);
        Stapel s = new Stapel(ax, az, false);
        if (BioModel.kans(h, 0) < STAPEL_KANS && m.eWeide(ax, az) >= BINNEN) {
            bouw(m, s, h);
        }
        if (m.cellen.size() > 60000) {
            m.cellen.clear();
        }
        m.cellen.put(sleutel, s);
        return s;
    }

    private static void bouw(BioModel m, Stapel s, long h) {
        int grond = grond(m, s.x, s.z);
        // the lowest island: mostly low with a wenteltrap from the meadow, sometimes higher with a lift pair
        Eiland a = null;
        for (int poging = 0; poging < 3 && a == null; poging++) {
            long he = BioModel.mix(h + 977L * (poging + 1));
            Eiland e = new Eiland(he, poging == 2 ? KLEIN : poging == 0 && BioModel.kans(he, 1) < 0.6 ? MIDDEL : klasse(he, 1), poging == 0 ? -1 : ROND);
            boolean laag = BioModel.kans(h, 3) < 0.7;
            e.zet(s.x, s.z, grond + (laag ? LAAG + (int) (BioModel.kans(he, 2) * (TRAP_TOT - LAAG + 1)) : 17 + (int) (BioModel.kans(he, 2) * 14)));
            int[] merk = s.merk();
            s.eilanden.add(e);
            if (klopt(m, s, e, merk, STAPEL_RUIM) && ingang(m, s, e, he, STAPEL_RUIM)) {
                a = e;
            } else {
                s.terug(merk);
            }
        }
        if (a == null) {
            return;
        }
        a.stapel = s;
        int niveaus = 4 + (int) (BioModel.kans(h, 4) * 4), draai = BioModel.kans(h, 5) < 0.5 ? 1 : 3;
        int richting = (int) (BioModel.kans(h, 6) * 4);
        boolean vorigeLift = a.lift != null;
        for (int n = 1; n < niveaus; n++) {
            long hn = BioModel.mix(h + 31337L * n);
            boolean lift = BioModel.kans(hn, 0) < (vorigeLift ? 0.3 : 0.65);
            int rijs = lift ? 14 + (int) (BioModel.kans(hn, 1) * 19) : 5 + (int) (BioModel.kans(hn, 1) * 6);
            int ruimte = grond + LAAG + HOOG - a.top;
            if (rijs > ruimte) {
                if (ruimte >= 14) {
                    lift = true;
                    rijs = ruimte;
                } else if (ruimte >= 5) {
                    lift = false;
                    rijs = Math.min(ruimte, 10);
                } else {
                    break;
                }
            }
            Eiland b = null;
            for (int poging = 0; poging < 3 && b == null; poging++) {
                long hb = BioModel.mix(hn + 4099L * (poging + 1));
                Eiland e = new Eiland(hb, poging == 2 ? KLEIN : klasse(hb, 1), poging == 0 ? -1 : ROND);
                for (int t = 0; t < 4 && b == null; t++) {
                    Direction d = RICHTINGEN[(richting + draai * n + t) % 4];
                    int[] merk = s.merk();
                    s.eilanden.add(e);
                    boolean past = lift ? liftpaar(m, s, a, e, rijs, d, hb, t) : wentel(m, s, a, e, rijs, d, hb, t);
                    if (past) {
                        b = e;
                    } else {
                        s.terug(merk);
                    }
                }
                if (b == null && poging == 1) {
                    // the other kind of link, as a last try
                    lift = !lift;
                    rijs = lift ? Math.max(14, Math.min(ruimte, 16)) : Math.min(ruimte, 8);
                    if (rijs > ruimte) {
                        break;
                    }
                }
            }
            if (b == null) {
                break;
            }
            b.stapel = s;
            b.niveau = n;
            vorigeLift = lift;
            a = b;
        }
        // side islands: beside an island of the chain, level with it or one block up or down, a jump of one block away
        int hoofd = s.eilanden.size();
        for (int n = 0; n < hoofd; n++) {
            Eiland van = s.eilanden.get(n);
            for (int k = 0; k < 2; k++) {
                long hz = BioModel.mix(h + 5557L * (n * 2 + k + 1));
                if (BioModel.kans(hz, 0) >= 0.7) {
                    continue;
                }
                Eiland e = new Eiland(hz, BioModel.kans(hz, 1) < 0.75 ? KLEIN : MIDDEL, BioModel.kans(hz, 2) < 0.6 ? ROND : -1);
                int eerste = (int) (BioModel.kans(hz, 3) * 4), verschil = (int) (BioModel.kans(hz, 4) * 3) - 1;
                for (int t = 0; t < 4; t++) {
                    Direction d = RICHTINGEN[(eerste + t) % 4];
                    int[] merk = s.merk();
                    s.eilanden.add(e);
                    if (naast(m, s, van, e, verschil, d)) {
                        e.stapel = s;
                        e.niveau = van.niveau;
                        break;
                    }
                    s.terug(merk);
                }
            }
        }
        water(m, s, h);
        for (Eiland e : s.eilanden) {
            bomen(s, e);
        }
        omvang(s);
    }

    /** Puts island b beside island a, a jump of one block away (their tops at most one block apart). */
    private static boolean naast(BioModel m, Stapel s, Eiland a, Eiland b, int verschil, Direction d) {
        int[] merk = s.merk();
        if (!rimVast(a, d, 1) || !rimVast(b, d.getOpposite(), 1)) {
            return false;
        }
        // the two rims face each other on the same row: there the gap is exactly one block, on the other rows at least one
        // (b's rows are counted the other way round, it looks back at a)
        int afstand = GEEN;
        for (int q = -1; q <= 1; q++) {
            int ra = a.reik(d, q), rb = b.reik(d.getOpposite(), -q);
            if (ra != GEEN && rb != GEEN) {
                afstand = Math.max(afstand, ra + rb + 2);
            }
        }
        if (afstand == GEEN) {
            return false;
        }
        b.zet(a.x + d.getStepX() * afstand, a.z + d.getStepZ() * afstand, a.top + verschil);
        landing(s, a, d, 1);
        landing(s, b, d.getOpposite(), 1);
        Direction p = d.getClockWise();
        for (int q = -1; q <= 1; q++) {
            int ra = a.reik(d, q), rb = b.reik(d.getOpposite(), -q);
            if (ra != GEEN && rb != GEEN) {
                int x0 = a.x + d.getStepX() * (ra + 1) + p.getStepX() * q, z0 = a.z + d.getStepZ() * (ra + 1) + p.getStepZ() * q;
                int x1 = a.x + d.getStepX() * (afstand - rb - 1) + p.getStepX() * q, z1 = a.z + d.getStepZ() * (afstand - rb - 1) + p.getStepZ() * q;
                s.vrij.add(new Doos(Math.min(x0, x1), Math.min(a.top, b.top) + 1, Math.min(z0, z1), Math.max(x0, x1), Math.max(a.top, b.top) + 5, Math.max(z0, z1)));
                if (ra + rb + 2 == afstand) {
                    // the two rim columns that face each other across one block: flat, with room to jump
                    int ax = a.x + d.getStepX() * ra + p.getStepX() * q, az = a.z + d.getStepZ() * ra + p.getStepZ() * q;
                    int bx = a.x + d.getStepX() * (ra + 2) + p.getStepX() * q, bz = a.z + d.getStepZ() * (ra + 2) + p.getStepZ() * q;
                    a.vlak(ax - a.x, az - a.z);
                    b.vlak(bx - b.x, bz - b.z);
                    s.vrij.add(new Doos(ax, a.top + 1, az, ax, a.top + 4, az));
                    s.vrij.add(new Doos(bx, b.top + 1, bz, bx, b.top + 4, bz));
                }
            }
        }
        if (klopt(m, s, b, merk, STAPEL_RUIM)) {
            return true;
        }
        s.terug(merk);
        return false;
    }

    private static void omvang(Stapel s) {
        double r = 0;
        for (Eiland e : s.eilanden) {
            r = Math.max(r, Math.hypot(e.x - s.x, e.z - s.z) + e.straal() + 2);
        }
        for (Stap p : s.stappen) {
            r = Math.max(r, Math.hypot(p.x - s.x, p.z - s.z) + 5);
        }
        for (Kolom k : s.kolommen) {
            r = Math.max(r, Math.hypot(k.x - s.x, k.z - s.z) + 7);
        }
        for (Kussen k : s.kussens) {
            r = Math.max(r, Math.hypot(k.x - s.x, k.z - s.z) + k.straal + 1);
        }
        if (s.plas != null) {
            for (int i = 0; i < s.plas.kolommen.length; i += 3) {
                r = Math.max(r, Math.hypot(s.plas.kolommen[i] - s.x, s.plas.kolommen[i + 1] - s.z) + 3);
            }
        }
        s.straal = (int) Math.ceil(r);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // water: a tiny pond on a high island, a thin fall over its rim, a catch pool below or the pond on the meadow
    // ------------------------------------------------------------------------------------------------------------------
    private static boolean droogVlak(Stapel s, Eiland e, int dx, int dz, int diepte) {
        if (!e.is(dx, dz)) {
            return false;
        }
        int i = e.index(dx, dz);
        if (e.bov[i] != 0 || e.nat[i] || e.diep[i] < diepte) {
            return false;
        }
        int wx = e.x + dx, wz = e.z + dz;
        for (Doos d : s.vrij) {
            if (d.x0 <= wx && d.x1 >= wx && d.z0 <= wz && d.z1 >= wz && d.y0 <= e.top + 3 && d.y1 >= e.top) {
                return false;
            }
        }
        for (Kolom k : s.kolommen) {
            if (Math.abs(k.x - wx) <= 2 && Math.abs(k.z - wz) <= 2) {
                return false;
            }
        }
        return true;
    }

    private static boolean kolomVrij(Stapel s, int x, int z, int y0, int y1) {
        Doos d = new Doos(x, y0, z, x, y1, z);
        for (Doos v : s.vrij) {
            if (v.snijdt(d)) {
                return false;
            }
        }
        for (Doos v : s.vast) {
            if (v.snijdt(d)) {
                return false;
            }
        }
        for (Kussen k : s.kussens) {
            if (k.y >= y0 && k.y <= y1 && Math.hypot(k.x - x, k.z - z) <= k.straal + 1) {
                return false;
            }
        }
        return true;
    }

    private static void nat(Eiland e, int dx, int dz) {
        int i = e.index(dx, dz);
        e.nat[i] = true;
        e.bov[i] = -1;
    }

    private static void water(BioModel m, Stapel s, long h) {
        if (s.eilanden.size() < 2 || BioModel.kans(h, 80) >= WATER_KANS) {
            return;
        }
        for (int n = s.eilanden.size() - 1; n >= 1; n--) {
            Eiland e = s.eilanden.get(n);
            int eerste = (int) (BioModel.kans(h, 81 + n) * 4);
            for (int t = 0; t < 4; t++) {
                Direction d = RICHTINGEN[(eerste + t) % 4];
                Direction p = d.getClockWise();
                for (int rij = -3; rij <= 3; rij++) {
                    int ext = e.reik(d, rij);
                    if (ext == GEEN) {
                        continue;
                    }
                    int ex = d.getStepX() * ext + p.getStepX() * rij, ez = d.getStepZ() * ext + p.getStepZ() * rij;
                    if (bron(m, s, e, ex, ez, d, h)) {
                        return;
                    }
                }
            }
        }
    }

    /** Tries a pond with its spout at the rim column (ex, ez) of e, spilling to side d. */
    private static boolean bron(BioModel m, Stapel s, Eiland e, int ex, int ez, Direction d, long h) {
        Direction p = d.getClockWise();
        int ix = ex - d.getStepX(), iz = ez - d.getStepZ(), jx = ix - d.getStepX(), jz = iz - d.getStepZ();
        // the spout: a rim column with island to both sides, the pond behind it, every pond column walled in
        if (!droogVlak(s, e, ex, ez, 1) || !droogVlak(s, e, ix, iz, 2) || !droogVlak(s, e, jx, jz, 2) || !e.is(ex + p.getStepX(), ez + p.getStepZ())
                || !e.is(ex - p.getStepX(), ez - p.getStepZ()) || e.is(ex + d.getStepX(), ez + d.getStepZ())) {
            return false;
        }
        List<int[]> plas = new ArrayList<>();
        plas.add(new int[]{ex, ez});
        plas.add(new int[]{ix, iz});
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                if (a * a + b * b <= 4 && !(a == 0 && b == 0) && !(jx + a == ix && jz + b == iz) && !(jx + a == ex && jz + b == ez)
                        && droogVlak(s, e, jx + a, jz + b, 2)) {
                    plas.add(new int[]{jx + a, jz + b});
                }
            }
        }
        plas.add(new int[]{jx, jz});
        // every pond column but the spout must be walled in by island on all four sides
        List<int[]> goed = new ArrayList<>();
        for (int[] c : plas) {
            boolean dicht = true;
            for (Direction b : RICHTINGEN) {
                int nx = c[0] + b.getStepX(), nz = c[1] + b.getStepZ();
                if (!(c[0] == ex && c[1] == ez && b == d) && !e.is(nx, nz)) {
                    dicht = false;
                }
            }
            if (dicht || c[0] == ex && c[1] == ez) {
                goed.add(c);
            }
        }
        if (goed.size() < 3) {
            return false;
        }
        // where the water goes: down, from catch pool to catch pool, to the meadow
        List<Val> vallen = new ArrayList<>();
        List<Object[]> vangen = new ArrayList<>();
        int vx = e.x + ex + d.getStepX(), vz = e.z + ez + d.getStepZ(), vy = e.top;
        Plas weide = null;
        boolean klaar = false;
        for (int trap = 0; trap < 5 && !klaar; trap++) {
            Eiland onder = null;
            for (Eiland o : s.eilanden) {
                if (o.top < vy && o.is(vx - o.x, vz - o.z) && (onder == null || o.top > onder.top)) {
                    onder = o;
                }
            }
            int land = onder != null ? onder.top : grond(m, vx, vz) - 1;
            if (!kolomVrij(s, vx, vz, land, vy + 1)) {
                return false;
            }
            for (Eiland o : s.eilanden) {
                // nothing may hang in the fall (an island between the two that the scan above did not take as the landing)
                if (o != onder && o.top < vy && o.top > land && o.is(vx - o.x, vz - o.z)) {
                    return false;
                }
            }
            if (onder == null) {
                weide = plas(m, s, vx, vz, h);
                if (weide == null) {
                    return false;
                }
                vallen.add(new Val(vx, vz, vy, weide.water + 1));
                klaar = true;
                break;
            }
            int ox = vx - onder.x, oz = vz - onder.z;
            if (!droogVlak(s, onder, ox, oz, 1)) {
                return false;
            }
            vallen.add(new Val(vx, vz, vy, onder.top + 1));
            int open = 0;
            Direction uit = null;
            for (Direction b : RICHTINGEN) {
                if (!onder.is(ox + b.getStepX(), oz + b.getStepZ())) {
                    open++;
                    uit = b;
                }
            }
            vangen.add(new Object[]{onder, ox, oz});
            if (open == 0) {
                // a catch pool in the island
                klaar = true;
                break;
            }
            if (open > 1) {
                return false;
            }
            vx += uit.getStepX();
            vz += uit.getStepZ();
            vy = onder.top;
        }
        if (!klaar) {
            return false;
        }
        for (int[] c : goed) {
            nat(e, c[0], c[1]);
        }
        for (Object[] v : vangen) {
            nat((Eiland) v[0], (int) v[1], (int) v[2]);
        }
        s.vallen.addAll(vallen);
        s.plas = weide;
        return true;
    }

    /** The meadow's pond around (x, z) with a short winding stream to a second, smaller pool; null when there is no room. */
    private static Plas plas(BioModel m, Stapel s, int x, int z, long h) {
        if (!inRegio(m, x, z, 12) || Luchtruim.bezet(x, z, 12)) {
            return null;
        }
        java.util.Map<Long, Integer> nat = new java.util.HashMap<>();
        double r = 3.6 + 1.8 * BioModel.kans(h, 90), f = BioModel.kans(h, 91) * 6.283;
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                double a = Math.atan2(dz, dx), rr = r * (1 + 0.16 * Math.sin(2 * a + f) + 0.1 * Math.sin(3 * a - f));
                double q = Math.hypot(dx, dz) / rr;
                if (q < 1) {
                    if (Math.hypot(x + dx - s.x, z + dz - s.z) > STAPEL_RUIM + 3) {
                        return null;
                    }
                    nat.put(pak(x + dx, z + dz), q < 0.55 ? 2 : 1);
                }
            }
        }
        // the stream: away from the stack's middle, winding, two wide, to a small pool
        double hoek = Math.atan2(z - s.z, x - s.x) + (BioModel.kans(h, 92) - 0.5) * 1.2, px = x, pz = z;
        int lang = 14 + (int) (BioModel.kans(h, 93) * 14);
        for (int i = 0; i < lang; i++) {
            hoek += 0.22 * Math.sin(i * 0.55 + f);
            px += Math.cos(hoek);
            pz += Math.sin(hoek);
            int bx = (int) Math.round(px), bz = (int) Math.round(pz);
            if (!inRegio(m, bx, bz, 5) || Luchtruim.bezet(bx, bz, 5) || Math.hypot(bx - s.x, bz - s.z) > STAPEL_RUIM - 1) {
                break;
            }
            nat.putIfAbsent(pak(bx, bz), 1);
            nat.putIfAbsent(pak(bx + (Math.abs(Math.cos(hoek)) < 0.7 ? 1 : 0), bz + (Math.abs(Math.cos(hoek)) < 0.7 ? 0 : 1)), 1);
            if (i == lang - 1) {
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (dx * dx + dz * dz <= 7) {
                            nat.putIfAbsent(pak(bx + dx, bz + dz), 1);
                        }
                    }
                }
            }
        }
        // nothing of the stack may stand in or beside the water
        int laag = Integer.MAX_VALUE;
        for (long k : nat.keySet()) {
            int kx = (int) (k >> 32), kz = (int) k;
            laag = Math.min(laag, grond(m, kx, kz));
            for (Stap p : s.stappen) {
                if (Math.abs(p.x - kx) <= 5 && Math.abs(p.z - kz) <= 5 && p.top < WEIDE_Y + 8) {
                    return null;
                }
            }
            for (Kolom k2 : s.kolommen) {
                if (Math.abs(k2.x - kx) <= 4 && Math.abs(k2.z - kz) <= 4 && k2.voet < WEIDE_Y + 8) {
                    return null;
                }
            }
            for (Eiland e : s.eilanden) {
                if (e.top < WEIDE_Y + 12 && e.is(kx - e.x, kz - e.z)) {
                    return null;
                }
            }
        }
        int[] kol = new int[nat.size() * 3];
        int i = 0;
        List<Long> sleutels = new ArrayList<>(nat.keySet());
        java.util.Collections.sort(sleutels);
        for (long k : sleutels) {
            kol[i++] = (int) (k >> 32);
            kol[i++] = (int) k;
            kol[i++] = nat.get(k);
        }
        return new Plas(laag - 1, kol);
    }

    private static long pak(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    /** Is there water of the model at this block above the meadow (an island pond, a catch pool, a fall)? */
    public static boolean water(BioModel m, int x, int y, int z) {
        for (Stapel s : stapels(m, x, z, x + 1, z + 1)) {
            for (Val v : s.vallen) {
                if (v.x == x && v.z == z && y <= v.boven && y >= v.onder) {
                    return true;
                }
            }
            for (Eiland e : s.eilanden) {
                if (e.top == y && e.vijver(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // trees
    // ------------------------------------------------------------------------------------------------------------------
    private static void bomen(Stapel s, Eiland e) {
        if (e.rots) {
            return;
        }
        int wens = e.klasse == KLEIN ? (int) (BioModel.kans(e.zaad, 110) * 2.4) : e.klasse == MIDDEL ? 2 + (int) (BioModel.kans(e.zaad, 110) * 3) : 4 + (int) (BioModel.kans(e.zaad, 110) * 3);
        boolean groot = e.klasse >= MIDDEL && BioModel.kans(e.zaad, 111) < 0.6;
        for (int poging = 0; poging < wens * 5 && e.bomen.size() < wens; poging++) {
            double a = BioModel.kans(e.zaad, 120 + poging * 2) * 6.283, d = BioModel.kans(e.zaad, 121 + poging * 2);
            int dx = (int) Math.round(Math.cos(a) * d * e.rx * 0.9), dz = (int) Math.round(Math.sin(a) * d * e.rz * 0.9);
            if (!e.is(dx, dz) || e.binnen(dx, dz) < 0.28 || e.nat[e.index(dx, dz)] || e.diep[e.index(dx, dz)] < 2) {
                continue;
            }
            boolean reus = groot && e.bomen.isEmpty() && e.binnen(dx, dz) > 0.45;
            int kruin = reus ? 5 : 2, hoog = reus ? 14 : 7;
            int wx = e.x + dx, wz = e.z + dz, y = e.top + e.bov[e.index(dx, dz)];
            Doos boom = new Doos(wx - kruin, y + 1, wz - kruin, wx + kruin, y + hoog, wz + kruin);
            boolean vrij = true;
            for (Doos v : s.vrij) {
                vrij &= !v.snijdt(boom);
            }
            for (Kolom k : s.kolommen) {
                vrij &= Math.abs(k.x - wx) > kruin + 2 || Math.abs(k.z - wz) > kruin + 2 || k.boven < y || k.voet > y + hoog;
            }
            for (Val v : s.vallen) {
                vrij &= Math.abs(v.x - wx) > kruin || Math.abs(v.z - wz) > kruin || v.boven < y || v.onder > y + hoog;
            }
            for (Eiland o : s.eilanden) {
                vrij &= o == e || o.top <= e.top || !raakt(o, boom);
            }
            for (Doos v : s.vast) {
                vrij &= !v.snijdt(boom);
            }
            for (int[] b : e.bomen) {
                vrij &= Math.abs(b[0] - dx) + Math.abs(b[1] - dz) >= (reus || b[2] == 2 ? 8 : 4);
            }
            // (not beside a pond: its canopy would hang in the fall)
            for (int qa = -1; qa <= 1 && vrij; qa++) {
                for (int qb = -1; qb <= 1 && vrij; qb++) {
                    vrij = !e.is(dx + qa, dz + qb) || !e.nat[e.index(dx + qa, dz + qb)];
                }
            }
            if (vrij) {
                e.bomen.add(new int[]{dx, dz, reus ? 2 : BioModel.kans(e.zaad, 160 + poging) < 0.6 ? 0 : 1});
            }
        }
    }

    // ==================================================================================================================
    // the loose islands and rocks between the stacks
    // ==================================================================================================================
    private static final Eiland GEEN_EILAND = new Eiland(1L, ROTSJE, ROND);

    /**
     * The loose island a grid cell WOULD have (shape and place, nothing checked; cached). Neighbouring cells look at each
     * other's candidates, so the choice between two that are in each other's way is the same from both sides.
     */
    private static Eiland kandidaat(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_KANDIDAAT, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return bekend == GEEN_EILAND ? null : (Eiland) bekend;
        }
        long h = m.hash(cx, cz, 5201);
        Eiland ei = null;
        int x = cx * CEL + (int) (BioModel.kans(h, 1) * CEL), z = cz * CEL + (int) (BioModel.kans(h, 13) * CEL);
        if (BioModel.kans(h, 0) < EILAND_KANS && m.eWeide(x, z) >= BINNEN) {
            ei = new Eiland(h, klasse(h, 18), -1);
            ei.zet(x, z, grond(m, x, z) + LAAG + (int) (HOOG * Math.pow(BioModel.kans(h, 2), 1.5)));
            // (only one that has room beside the stacks counts)
            if (!losVrij(m, ei, 14) || !inRegio(m, x, z, (int) ei.straal() + 2)) {
                ei = null;
            }
        }
        m.cellen.put(sleutel, ei == null ? GEEN_EILAND : ei);
        return ei;
    }

    /** The loose islands of a grid cell (cached in the model). */
    public static Eiland[] cel(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_EILAND, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return (Eiland[]) bekend;
        }
        List<Eiland> uit = new ArrayList<>(4);
        List<Eiland> buren = new ArrayList<>(8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Eiland b = kandidaat(m, cx + dx, cz + dz);
                if (b != null && !(dx == 0 && dz == 0)) {
                    buren.add(b);
                }
            }
        }
        Eiland ei = kandidaat(m, cx, cz);
        if (ei != null) {
            // of two loose islands in each other's way (their stairs and lifts counted), the one with the higher draw stays
            boolean blijft = true;
            for (Eiland b : buren) {
                blijft &= Math.hypot(b.x - ei.x, b.z - ei.z) >= b.straal() + ei.straal() + 22 || BioModel.kans(b.zaad, 19) < BioModel.kans(ei.zaad, 19);
            }
            Stapel s = new Stapel(ei.x, ei.z, true);
            int[] merk = s.merk();
            s.eilanden.add(ei);
            ei.stapel = s;
            if (blijft && klopt(m, s, ei, merk, BEREIK)) {
                ingang(m, s, ei, ei.zaad, BEREIK);
                bomen(s, ei);
                omvang(s);
                uit.add(ei);
            } else {
                ei = null;
            }
        }
        long h = m.hash(cx, cz, 5202);
        int x = cx * CEL + (int) (BioModel.kans(h, 1) * CEL), z = cz * CEL + (int) (BioModel.kans(h, 13) * CEL);
        if (BioModel.kans(h, 0) < ROTS_KANS && m.eWeide(x, z) >= BINNEN) {
            // a little swarm of rocks, out of the way of the islands
            int top = grond(m, x, z) + LAAG + (int) (HOOG * Math.pow(BioModel.kans(h, 2), 1.3));
            int n = 1 + (int) (BioModel.kans(h, 30) * 3);
            List<Eiland> zwerm = new ArrayList<>(3);
            for (int i = 0; i < n; i++) {
                long hr = BioModel.mix(h + 7717L * (i + 1));
                Eiland rots = new Eiland(hr, ROTSJE, ROND);
                rots.zet(x + (i == 0 ? 0 : (int) (BioModel.kans(hr, 31) * 15) - 7), z + (i == 0 ? 0 : (int) (BioModel.kans(hr, 32) * 15) - 7),
                        top + (i == 0 ? 0 : (int) (BioModel.kans(hr, 33) * 13) - 6));
                boolean vrij = rots.top - grond(m, rots.x, rots.z) >= LAAG - 3 && m.eWeide(rots.x, rots.z) >= BINNEN && rotsVrij(m, rots);
                for (Eiland b : buren) {
                    vrij &= Math.hypot(b.x - rots.x, b.z - rots.z) >= b.straal() + rots.straal() + 13;
                }
                vrij &= ei == null || Math.hypot(ei.x - rots.x, ei.z - rots.z) >= ei.straal() + rots.straal() + 13;
                for (Eiland b : zwerm) {
                    vrij &= Math.hypot(b.x - rots.x, b.z - rots.z) >= b.straal() + rots.straal() + 1 || Math.abs(b.top - rots.top) >= 7;
                }
                if (!vrij) {
                    continue;
                }
                Stapel s = new Stapel(rots.x, rots.z, true);
                s.eilanden.add(rots);
                s.straal = 6;
                rots.stapel = s;
                zwerm.add(rots);
                uit.add(rots);
            }
        }
        // (the highest island of a stack that lies in this cell is listed too: a spot for a building on a summit)
        for (int sx = Math.floorDiv(cx * CEL - STAPEL_RUIM, STAPEL_CEL); sx <= Math.floorDiv(cx * CEL + CEL + STAPEL_RUIM, STAPEL_CEL); sx++) {
            for (int sz = Math.floorDiv(cz * CEL - STAPEL_RUIM, STAPEL_CEL); sz <= Math.floorDiv(cz * CEL + CEL + STAPEL_RUIM, STAPEL_CEL); sz++) {
                Stapel s = stapel(m, sx, sz);
                Eiland top = s.eilanden.isEmpty() ? null : s.hoogste();
                if (top != null && Math.floorDiv(top.x, CEL) == cx && Math.floorDiv(top.z, CEL) == cz) {
                    uit.add(top);
                }
            }
        }
        Eiland[] r = uit.toArray(new Eiland[0]);
        if (m.cellen.size() > 60000) {
            m.cellen.clear();
        }
        m.cellen.put(sleutel, r);
        return r;
    }

    /** A rock may float among a stack's islands, as long as it is in nobody's way: no route, no island, no tree, no fall. */
    private static boolean rotsVrij(BioModel m, Eiland e) {
        int st = (int) e.straal() + 2;
        if (Luchtruim.bezet(e.x, e.z, st)) {
            return false;
        }
        Doos d = new Doos(e.x - st, e.top - 9, e.z - st, e.x + st, e.top + 5, e.z + st);
        for (int cx = Math.floorDiv(e.x - STAPEL_RUIM * 2, STAPEL_CEL); cx <= Math.floorDiv(e.x + STAPEL_RUIM * 2, STAPEL_CEL); cx++) {
            for (int cz = Math.floorDiv(e.z - STAPEL_RUIM * 2, STAPEL_CEL); cz <= Math.floorDiv(e.z + STAPEL_RUIM * 2, STAPEL_CEL); cz++) {
                Stapel s = stapel(m, cx, cz);
                if (s.eilanden.isEmpty() || Math.hypot(e.x - s.x, e.z - s.z) > s.straal + st) {
                    continue;
                }
                for (Doos v : s.vrij) {
                    if (v.snijdt(d)) {
                        return false;
                    }
                }
                for (Doos v : s.vast) {
                    if (v.snijdt(d)) {
                        return false;
                    }
                }
                for (Kolom k : s.kolommen) {
                    if (Math.abs(k.x - e.x) <= st + 3 && Math.abs(k.z - e.z) <= st + 3) {
                        return false;
                    }
                }
                for (Val v : s.vallen) {
                    if (Math.abs(v.x - e.x) <= st && Math.abs(v.z - e.z) <= st) {
                        return false;
                    }
                }
                for (Kussen k : s.kussens) {
                    if (Math.hypot(k.x - e.x, k.z - e.z) <= k.straal + st && Math.abs(k.y - e.top) < 10) {
                        return false;
                    }
                }
                for (Eiland o : s.eilanden) {
                    if (raakt(o, new Doos(d.x0, d.y0 - 4, d.z0, d.x1, d.y1 + 6, d.z1))) {
                        return false;
                    }
                    for (int[] b : o.bomen) {
                        if (new Doos(o.x + b[0] - 6, o.top, o.z + b[1] - 6, o.x + b[0] + 6, o.top + 16, o.z + b[1] + 6).snijdt(d)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /** A loose island keeps out of every stack's room. */
    private static boolean losVrij(BioModel m, Eiland e, int bereik) {
        int reik = (int) e.straal() + bereik;
        for (int cx = Math.floorDiv(e.x - STAPEL_RUIM - reik, STAPEL_CEL); cx <= Math.floorDiv(e.x + STAPEL_RUIM + reik, STAPEL_CEL); cx++) {
            for (int cz = Math.floorDiv(e.z - STAPEL_RUIM - reik, STAPEL_CEL); cz <= Math.floorDiv(e.z + STAPEL_RUIM + reik, STAPEL_CEL); cz++) {
                Stapel s = stapel(m, cx, cz);
                if (!s.eilanden.isEmpty() && Math.hypot(e.x - s.x, e.z - s.z) < s.straal + reik) {
                    return false;
                }
            }
        }
        return !Luchtruim.bezet(e.x, e.z, reik);
    }

    /** Every stack (the loose islands' own included) that can reach into the box [x0, x1) x [z0, z1). */
    public static List<Stapel> stapels(BioModel m, int x0, int z0, int x1, int z1) {
        List<Stapel> uit = new ArrayList<>();
        for (int cx = Math.floorDiv(x0 - STAPEL_RUIM * 2, STAPEL_CEL); cx <= Math.floorDiv(x1 + STAPEL_RUIM * 2, STAPEL_CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - STAPEL_RUIM * 2, STAPEL_CEL); cz <= Math.floorDiv(z1 + STAPEL_RUIM * 2, STAPEL_CEL); cz++) {
                Stapel s = stapel(m, cx, cz);
                if (!s.eilanden.isEmpty() && s.x + s.straal >= x0 && s.x - s.straal < x1 && s.z + s.straal >= z0 && s.z - s.straal < z1) {
                    uit.add(s);
                }
            }
        }
        for (int cx = Math.floorDiv(x0 - BEREIK, CEL); cx <= Math.floorDiv(x1 + BEREIK, CEL); cx++) {
            for (int cz = Math.floorDiv(z0 - BEREIK, CEL); cz <= Math.floorDiv(z1 + BEREIK, CEL); cz++) {
                for (Eiland ei : cel(m, cx, cz)) {
                    Stapel s = ei.stapel;
                    if (s.los && s.x + s.straal >= x0 && s.x - s.straal < x1 && s.z + s.straal >= z0 && s.z - s.straal < z1) {
                        uit.add(s);
                    }
                }
            }
        }
        return uit;
    }

    /** Every island that can reach into the box [x0, x1) x [z0, z1). */
    public static List<Eiland> bij(BioModel m, int x0, int z0, int x1, int z1) {
        List<Eiland> uit = new ArrayList<>();
        for (Stapel s : stapels(m, x0, z0, x1, z1)) {
            uit.addAll(s.eilanden);
        }
        return uit;
    }

    // ==================================================================================================================
    // clouds
    // ==================================================================================================================
    /** One blob of a cloud bank. */
    public record Wolk(int x, int y, int z, double rx, double ry, double rz, boolean roze, int bank) {
    }

    /** The cloud blobs of a grid cell (cached in the model): a big bank, sometimes a smaller high one, and a little puff. */
    public static Wolk[] wolken(BioModel m, int cx, int cz) {
        long sleutel = BioModel.sleutel(SOORT_WOLK, cx, cz);
        Object bekend = m.cellen.get(sleutel);
        if (bekend != null) {
            return (Wolk[]) bekend;
        }
        List<Wolk> uit = new ArrayList<>(8);
        for (int welke = 0; welke < 3; welke++) {
            long h = m.hash(cx, cz, 6301 + welke);
            int x = cx * WOLK_CEL + (int) (BioModel.kans(h, 1) * WOLK_CEL), z = cz * WOLK_CEL + (int) (BioModel.kans(h, 2) * WOLK_CEL);
            if (BioModel.kans(h, 0) >= (welke == 0 ? WOLK_KANS : welke == 1 ? 0.55 : 0.8) || m.eWeide(x, z) < BINNEN || Luchtruim.bezet(x, z, 22)) {
                continue;
            }
            double groot = welke == 0 ? 0.85 + 0.6 * BioModel.kans(h, 6) : welke == 1 ? 0.5 + 0.3 * BioModel.kans(h, 6) : 0.3 + 0.2 * BioModel.kans(h, 6);
            int y = grond(m, x, z) + (welke == 0 ? 14 + (int) (88 * BioModel.kans(h, 3)) : welke == 1 ? 60 + (int) (52 * BioModel.kans(h, 3)) : 18 + (int) (90 * BioModel.kans(h, 3)));
            boolean roze = BioModel.kans(h, 4) < ROZE_KANS;
            int n = welke == 0 ? 3 + (int) (BioModel.kans(h, 5) * 4) : welke == 1 ? 2 + (int) (BioModel.kans(h, 5) * 2) : 1 + (int) (BioModel.kans(h, 5) * 2);
            double rx0 = (7 + 4 * BioModel.kans(h, 7)) * groot, rz0 = (5.5 + 3.5 * BioModel.kans(h, 8)) * groot, ry0 = (2.4 + 1.4 * BioModel.kans(h, 9)) * Math.sqrt(groot);
            for (int i = 0; i < n; i++) {
                double a = BioModel.kans(h, 10 + i * 5) * 6.283, d = i == 0 ? 0 : 0.55 + 0.4 * BioModel.kans(h, 11 + i * 5);
                double k = i == 0 ? 1 : 0.5 + 0.3 * BioModel.kans(h, 13 + i * 5);
                uit.add(new Wolk(x + (int) Math.round(Math.cos(a) * d * rx0), y + (i == 0 ? 0 : (int) (BioModel.kans(h, 12 + i * 5) * 3) - 1),
                        z + (int) Math.round(Math.sin(a) * d * rz0), rx0 * k, ry0 * (i == 0 ? 1 : 0.7 + 0.35 * BioModel.kans(h, 14 + i * 5)), rz0 * k,
                        roze || BioModel.kans(h, 15 + i * 5) < 0.08, welke));
            }
        }
        Wolk[] r = uit.toArray(new Wolk[0]);
        m.cellen.put(sleutel, r);
        return r;
    }

    /** How thick the thin cloud sea is at this column, in blocks (0: none; halves are slabs). */
    public static double zee(BioModel m, int x, int z) {
        double v = m.ruis(BioModel.R_RIVIER, x * 0.9 - 9000, z * 0.9 + 9000);
        if (v < 0.02) {
            return 0;
        }
        // (two finer noises break the sheet into drifts with holes between them: billows, thick in their middles, thin at their edges)
        double rand = Math.min(1, (v - 0.02) * 6);
        double dik = rand * (0.7 + 3.4 * m.ruis(BioModel.R_DETAIL, x * 0.8 + 700, z * 0.8 - 700) + 1.6 * m.ruis(BioModel.R_DETAIL, x * 2.1 - 300, z * 2.1 + 300));
        return dik < 0.5 ? 0 : Math.min(3.0, dik);
    }

    /** Is the thin cloud sea at this column (0: no; 1 or 2: that many blocks thick)? */
    public static int wolkenzee(BioModel m, int x, int z) {
        return (int) Math.ceil(zee(m, x, z) - 0.49);
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

    // ==================================================================================================================
    // the chunk map
    // ==================================================================================================================
    /** Fills the Wolkenweide columns of a chunk map; false when the chunk has none. */
    static boolean vul(BioModel m, Kaart k) {
        long t0 = System.nanoTime();
        boolean r = vul0(m, k);
        if (r) {
            TIJD.addAndGet(System.nanoTime() - t0);
            KEER.incrementAndGet();
        }
        return r;
    }

    private static boolean vul0(BioModel m, Kaart k) {
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
        List<Stapel> stapels = stapels(m, x0, z0, x0 + 16, z0 + 16);
        // the meadow: the feet of stairs and lifts, then the pond and its banks
        for (Stapel s : stapels) {
            for (Terp t : s.terpen) {
                for (int x = Math.max(x0, t.x - t.straal); x <= Math.min(x0 + 15, t.x + t.straal); x++) {
                    for (int z = Math.max(z0, t.z - t.straal); z <= Math.min(z0 + 15, t.z + t.straal); z++) {
                        int o = Kaart.index(x, z);
                        if (k.meng[o] >= 1f && (t.exact || k.hoogte[o] < t.y)) {
                            k.hoogte[o] = t.y;
                        }
                    }
                }
            }
        }
        for (Stapel s : stapels) {
            Plas p = s.plas;
            if (p == null || p.x1 + 1 < x0 || p.x0 - 1 > x0 + 15 || p.z1 + 1 < z0 || p.z0 - 1 > z0 + 15) {
                continue;
            }
            for (int i = 0; i < p.kolommen.length; i += 3) {
                int px = p.kolommen[i], pz = p.kolommen[i + 1];
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int x = px + dx, z = pz + dz;
                        if ((x >> 4) != k.cx || (z >> 4) != k.cz) {
                            continue;
                        }
                        int o = Kaart.index(x, z);
                        if (dx == 0 && dz == 0) {
                            k.hoogte[o] = p.water - p.kolommen[i + 2];
                            k.water[o] = p.water;
                        } else if (k.water[o] == Kaart.GEEN && p.diepte(x, z) == 0 && k.hoogte[o] < p.water) {
                            k.hoogte[o] = p.water;
                        }
                    }
                }
            }
        }
        for (Stapel s : stapels) {
            for (Eiland ei : s.eilanden) {
                int w = 2 * ei.r + 1;
                for (int x = Math.max(x0, ei.x - ei.r); x <= Math.min(x0 + 15, ei.x + ei.r); x++) {
                    for (int z = Math.max(z0, ei.z - ei.r); z <= Math.min(z0 + 15, ei.z + ei.r); z++) {
                        int i = (x - ei.x + ei.r) + (z - ei.z + ei.r) * w;
                        if (ei.diep[i] < 0) {
                            continue;
                        }
                        int o = Kaart.index(x, z);
                        if (k.meng[o] <= 0) {
                            continue;
                        }
                        // (a low island floats: its underside stays three blocks off the meadow)
                        int boven = ei.top + ei.bov[i], onder = Math.max(ei.top - ei.diep[i], k.hoogte[o] + 4);
                        if (onder <= boven) {
                            k.span(o, onder, boven);
                        }
                    }
                }
            }
            for (Stap p : s.stappen) {
                if (p.wolk || p.x + 1 < x0 || p.x - 1 > x0 + 15 || p.z + 1 < z0 || p.z - 1 > z0 + 15) {
                    continue;
                }
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int x = p.x + dx, z = p.z + dz;
                        if ((x >> 4) != k.cx || (z >> 4) != k.cz) {
                            continue;
                        }
                        int o = Kaart.index(x, z);
                        if (k.meng[o] > 0) {
                            boolean hoek = dx != 0 && dz != 0;
                            // a pebble: two thick in the middle, thin at its sides, and some of its corners missing
                            if (hoek && (p.hoeken >> ((dx > 0 ? 1 : 0) + (dz > 0 ? 2 : 0)) & 1) == 0) {
                                continue;
                            }
                            int dik = dx == 0 && dz == 0 ? 2 : hoek ? 1 : 1 + (p.hoeken >> (4 + (dx != 0 ? (dx + 1) / 2 : 2 + (dz + 1) / 2)) & 1);
                            k.span(o, Math.max(p.top - dik + 1, k.hoogte[o] + 1), p.top);
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
