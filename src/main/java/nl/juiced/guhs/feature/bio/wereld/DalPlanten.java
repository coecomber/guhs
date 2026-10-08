package nl.juiced.guhs.feature.bio.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * biomes3 wereld, the Klaterdal: what grows there, placed with the terrain (by {@link DalVulling}) because every choice
 * needs the terrain model: how far the water is, whether the ground is plain terrace, where the rock is.
 * <p>
 * Lush but calm (biomes3 fix-dal; the first sketch had too much tall grass, the first finished valley was bare): still no
 * grass at all. Broad soft patches of pale moss ({@link DalBlokken#MOS}) on the pink ground; flowers in DRIFTS of one kind
 * near the water and in the groves; reeds in tufts on the bank; petals and lily pads on still water, most on the koi
 * pools. Along the river's LEFT bank a path of flush stepping stones and a little sand links the natural rock steps beside
 * the cascades, so a walker is led from the rim to the lake. Guhbloesem trees ({@link BloesemBoom}, small to large, now
 * and then a giant) stand in GROVES with open lawn between; near the water they lean towards it, but no crown reaches over
 * it, and the river's right bank keeps its clear building strip. Esdoorn accents ({@link #esdoorn}) stand in ones and twos
 * by a fall and by a koi pool; crooked little trees ({@link #bonsai}) on boulders and on the ledges of the rock, leaning
 * out; guh-bamboe in real groves against a rock face. Nothing is planted in or on a building
 * ({@link BouwRuimte#inBuilding}). Everything is a pure function of the world seed and the position.
 */
public final class DalPlanten {
    // <dal-planten>
    /** One try for a guhbloesem per cell of this size; the chance in a grove, at a grove's edge, on the open lawn. */
    public static final int BOOM_CEL = 6;
    public static final double BOOM_BOS = 0.6, BOOM_RAND = 0.2, BOOM_GAZON = 0.09;
    /** The grove noise above this is a grove, above the second its edge. */
    public static final double BOS_VANAF = -0.06, BOS_RAND = -0.2;
    /** A trunk stands at least this far from the water on the left bank (beyond the path), a crown's edge this far. */
    public static final int STAM_AF = 7, KROON_AF = 1;
    /** On the right bank the trunk stays out of the building strip, and the crown's edge this far from the water. */
    public static final int STAM_RECHTS = 11, KROON_RECHTS = 6;
    /** Esdoorn accents: one try per cell near a fall, and the chance beside a koi pool. */
    public static final int ESDOORN_CEL = 10;
    public static final double ESDOORN_VAL = 0.8, ESDOORN_POEL = 0.6;
    /** One try for a bamboo grove per cell (it must stand against a rock face). */
    public static final int BAMBOE_CEL = 14;
    public static final double BAMBOE_KANS = 0.7;
    /** The moss noise above this is a moss patch (broad soft patches, about a quarter of the ground). */
    public static final double MOS_VANAF = 0.4;
    /** Flower drifts: the drift noise above this, near water or in a grove; how full a drift is. */
    public static final double BLOEM_VANAF = 0.2, BLOEM_VOL = 0.55;
    /** The path on the left bank: this far from the water. */
    public static final double PAD_VAN = 3.8, PAD_TOT = 5.7;
    /** How many boulders carry a crooked little tree; how many ledge columns do, near a fall and elsewhere. */
    public static final double BONSAI_KANS = 0.6, BONSAI_VAL = 0.09, BONSAI_RICHEL = 0.012;
    // </dal-planten>

    static final String[] BLOEMEN = {"roze_guhbloem", "knabbelroos", "roze_hibiscus", "guhoortjes"};
    /** The window of model columns around a chunk the plants look at. */
    private static final int RAND = 12, N = 16 + 2 * RAND;

    private static boolean grond(BlockState s) {
        return s.is(Blocks.PINK_WOOL) || s.is(DalBlokken.MOS.get());
    }

    /** Plain dry terrace ground of the Klaterdal biome at full strength? */
    private static boolean vlak(BioModel m, int x, int z) {
        return m.terras(x, z) >= 0 && m.droog(x, z) && m.vlag(x, z) == 0 && m.soort(x, z) == Kaart.DAL;
    }

    /** What the model says around a chunk: heights, and the distance in blocks to the nearest water and to the nearest fall. */
    private static final class Buurt {
        final int x0, z0;
        final int[] h = new int[N * N];
        final float[] water = new float[N * N], val = new float[N * N];

        Buurt(BioModel m, Kaart k) {
            x0 = (k.cx << 4) - RAND;
            z0 = (k.cz << 4) - RAND;
            for (int j = 0; j < N; j++) {
                for (int i = 0; i < N; i++) {
                    int idx = i + j * N, x = x0 + i, z = z0 + j;
                    Kaart kk = m.kaart(x >> 4, z >> 4);
                    water[idx] = val[idx] = 99;
                    if (kk.leeg) {
                        h[idx] = Kaart.GEEN;
                        continue;
                    }
                    int o = Kaart.index(x, z);
                    h[idx] = kk.hoogte[o];
                    if (kk.meng[o] >= 1f && (kk.water[o] != Kaart.GEEN || (kk.vlag[o] & Kaart.LIP) != 0)) {
                        water[idx] = 0;
                        if ((kk.vlag[o] & Kaart.VAL) != 0) {
                            val[idx] = 0;
                        }
                    }
                }
            }
            afstand(water);
            afstand(val);
        }

        /** A chamfer distance transform (1 straight, 1.4 diagonal). */
        private static void afstand(float[] d) {
            for (int j = 0; j < N; j++) {
                for (int i = 0; i < N; i++) {
                    int idx = i + j * N;
                    float v = d[idx];
                    if (i > 0) {
                        v = Math.min(v, d[idx - 1] + 1);
                    }
                    if (j > 0) {
                        v = Math.min(v, d[idx - N] + 1);
                        if (i > 0) {
                            v = Math.min(v, d[idx - N - 1] + 1.4f);
                        }
                        if (i < N - 1) {
                            v = Math.min(v, d[idx - N + 1] + 1.4f);
                        }
                    }
                    d[idx] = v;
                }
            }
            for (int j = N - 1; j >= 0; j--) {
                for (int i = N - 1; i >= 0; i--) {
                    int idx = i + j * N;
                    float v = d[idx];
                    if (i < N - 1) {
                        v = Math.min(v, d[idx + 1] + 1);
                    }
                    if (j < N - 1) {
                        v = Math.min(v, d[idx + N] + 1);
                        if (i < N - 1) {
                            v = Math.min(v, d[idx + N + 1] + 1.4f);
                        }
                        if (i > 0) {
                            v = Math.min(v, d[idx + N - 1] + 1.4f);
                        }
                    }
                    d[idx] = v;
                }
            }
        }

        int idx(int x, int z) {
            int i = Math.max(0, Math.min(N - 1, x - x0)), j = Math.max(0, Math.min(N - 1, z - z0));
            return i + j * N;
        }

        /** Blocks to the nearest water (12 or more: far). */
        float water(int x, int z) {
            return water[idx(x, z)];
        }

        float val(int x, int z) {
            return val[idx(x, z)];
        }

        int h(int x, int z) {
            return h[idx(x, z)];
        }

        /** The step (-1, 0, 1 each way) towards the nearest water from a column, or {0, 0}. */
        int[] naarWater(int x, int z) {
            float best = water(x, z);
            int[] uit = {0, 0};
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                float v = water(x + d[0] * 3, z + d[1] * 3);
                if (v < best - 1.5f) {
                    best = v;
                    uit = d;
                }
            }
            return uit;
        }
    }

    /** {is this the river's left bank, blocks to the river's middle line} from the river noise (three samples). */
    private static double[] rivier(BioModel m, int x, int z) {
        double r = m.ruis(BioModel.R_RIVIER, x, z);
        double gx = (m.ruis(BioModel.R_RIVIER, x + 1, z) - m.ruis(BioModel.R_RIVIER, x - 1, z)) / 2, gz = (m.ruis(BioModel.R_RIVIER, x, z + 1) - m.ruis(BioModel.R_RIVIER, x, z - 1)) / 2;
        double g = Math.sqrt(gx * gx + gz * gz);
        return new double[]{r < 0 ? 1 : 0, g <= 0.0012 ? 99 : Math.abs(r) / g};
    }

    /**
     * May a tree with this crown stand here? The trunk on plain level ground, far enough from the water for its side of
     * the river, the crown's edge clear of the water, and no building.
     */
    private static boolean boomMag(WorldGenLevel level, BioModel m, Buurt b, int x, int z, double kx, double kz, double straal) {
        int h = m.hoogte(x, z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (m.hoogte(x + dx, z + dz) != h || m.vlag(x + dx, z + dz) != 0 || !m.droog(x + dx, z + dz)) {
                    return false;
                }
            }
        }
        float stam = b.water(x, z), kroon = b.water((int) Math.round(kx), (int) Math.round(kz));
        if (stam < STAM_AF || kroon < straal + KROON_AF) {
            return false;
        }
        if (stam < STAM_RECHTS + 6) {
            double[] r = rivier(m, x, z);
            // (the right bank of the RIVER; a brook or a pool far from the river's line has no building strip)
            if (r[0] == 0 && r[1] < stam + 9 && (stam < STAM_RECHTS || kroon < straal + KROON_RECHTS)) {
                return false;
            }
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        return grond(level.getBlockState(p.set(x, h, z))) && !inGebouw(level, x, h + 1, z, (int) Math.ceil(straal));
    }

    static int vul(WorldGenLevel level, BioModel m, Kaart k) {
        int x0 = k.cx << 4, z0 = k.cz << 4, gezet = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean gebouwen = !level.getChunk(k.cx, k.cz).getAllReferences().isEmpty();
        BlockState mos = DalBlokken.MOS.get().defaultBlockState(), riet = DalBlokken.RIET.get().defaultBlockState();
        BlockState blaadjes = Bio.blok("drijvende_bloesemblaadjes", Blocks.PINK_PETALS).defaultBlockState();
        BlockState lelie = Bio.blok("guh_waterlelie", Blocks.LILY_PAD).defaultBlockState();
        BlockState steen = DalVulling.rots(), zand = DalVulling.bedding();
        BlockState[] bloemen = new BlockState[BLOEMEN.length];
        for (int i = 0; i < bloemen.length; i++) {
            bloemen[i] = Bio.blok(BLOEMEN[i], Blocks.PINK_TULIP).defaultBlockState();
        }
        var poelen = DalTerrein.poelen(m, k.cx, k.cz);
        Buurt b = new Buurt(m, k);
        // --- per column: moss, the path, flowers, reeds, petals --------------------------------------------------------------------
        for (int o = 0; o < 256; o++) {
            if (k.terras[o] < 0 || k.meng[o] < 1f) {
                continue;
            }
            int x = x0 + (o & 15), z = z0 + (o >> 4), h = k.hoogte[o], w = k.water[o];
            long hash = m.hash(x, z, 6101);
            if (w != Kaart.GEEN) {
                // petals and lily pads on still water: not at a fall, and not where a fall lands
                if ((k.vlag[o] & Kaart.VAL) != 0) {
                    continue;
                }
                boolean stil = true;
                for (int dx = -2; dx <= 2 && stil; dx++) {
                    for (int dz = -2; dz <= 2 && stil; dz++) {
                        int bw = m.water(x + dx, z + dz);
                        stil = bw == Kaart.GEEN || bw == w;
                    }
                }
                if (!stil) {
                    continue;
                }
                boolean poel = false;
                for (DalTerrein.Poel pl : poelen) {
                    poel |= pl.rand(x - pl.x(), z - pl.z()) > 0;
                }
                double kans = poel ? 0.30 : m.ruis(BioModel.R_DETAIL, x * 0.8 + 500, z * 0.22 - 500) > 0.26 ? 0.32 : k.terras[o] == 0 ? 0.04 : 0.025;
                double worp = BioModel.kans(hash, 0);
                if (worp < kans && level.isEmptyBlock(p.set(x, w + 1, z)) && !(gebouwen && BouwRuimte.inBuilding(level, p))) {
                    level.setBlock(p, DalVulling.met(blaadjes, "dichtheid", String.valueOf(1 + (int) (BioModel.kans(hash, 1) * (poel ? 3 : 2)))), 2);
                    gezet++;
                } else if (poel && worp > 0.90 && level.isEmptyBlock(p.set(x, w + 1, z)) && !(gebouwen && BouwRuimte.inBuilding(level, p))) {
                    level.setBlock(p, lelie, 2);
                    gezet++;
                }
                continue;
            }
            if (k.vlag[o] != 0 || !grond(level.getBlockState(p.set(x, h, z))) || gebouwen && BouwRuimte.inBuilding(level, p)) {
                continue;
            }
            boolean oever = false, rots = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int bx = x + d.getStepX(), bz = z + d.getStepZ();
                oever |= m.water(bx, bz) == h - 1;
                rots |= m.water(bx, bz) == Kaart.GEEN && (m.vlag(bx, bz) & DalTerrein.VORM) != 0 && m.hoogte(bx, bz) > h;
            }
            float nat = b.water(x, z);
            boolean dal = k.soort[o] == Kaart.DAL;
            // the path along the left bank: flush stepping stones and a little sand, the ground between them
            if (dal && nat >= PAD_VAN && nat < PAD_TOT) {
                double[] r = rivier(m, x, z);
                if (r[0] == 1 && r[1] < nat + 12) {
                    double worp = BioModel.kans(hash, 7);
                    // (stones lie in twos and threes: a slow hash along the bank decides where)
                    boolean groep = BioModel.kans(m.hash(x >> 1, z >> 1, 6102), 0) < 0.62;
                    if (groep && worp < 0.72) {
                        level.setBlock(p, steen, 2);
                        gezet++;
                        continue;
                    }
                    if (worp > 0.80) {
                        level.setBlock(p, zand, 2);
                        gezet++;
                        continue;
                    }
                }
            }
            // moss: broad soft patches, more of it against rock, along the water and in the groves
            double grof = m.ruis(BioModel.R_DETAIL, x * 0.5 + 900, z * 0.5 + 300), fijn = m.ruis(BioModel.R_DETAIL, x * 1.1 + 900, z * 1.1 + 300);
            double mosRuis = grof + 0.4 * fijn;
            boolean opMos = mosRuis > MOS_VANAF || rots && mosRuis > -0.2 || oever && mosRuis > 0.0 || nat < 3.5 && mosRuis > 0.18 || mosRuis > MOS_VANAF - 0.15 && bos(m, x, z) > BOS_VANAF + 0.2;
            if (opMos) {
                level.setBlock(p, mos, 2);
                gezet++;
            }
            if (!level.isEmptyBlock(p.set(x, h + 1, z))) {
                continue;
            }
            // reeds: tufts on the bank
            if (oever && m.ruis(BioModel.R_DETAIL, x * 0.4 + 123, z * 0.4 + 77) > 0.18 && BioModel.kans(hash, 3) < 0.7) {
                level.setBlock(p, riet, 2);
                gezet++;
                continue;
            }
            // flowers: drifts of one kind near the water and in the groves
            if (dal && (nat < 7 || bos(m, x, z) > BOS_VANAF)) {
                double drift = m.ruis(BioModel.R_DETAIL, x * 0.5 + 2100, z * 0.5 - 1300);
                if (drift > BLOEM_VANAF && BioModel.kans(hash, 4) < BLOEM_VOL * Math.min(1.0, (drift - BLOEM_VANAF) / 0.12 + 0.35) || BioModel.kans(hash, 4) < 0.012) {
                    int soort = (int) ((m.ruis(BioModel.R_DETAIL, x * 0.06 + 5100, z * 0.06 + 3300) + 1) * 3.5) & 3;
                    level.setBlock(p, bloemen[soort], 2);
                    gezet++;
                }
            }
        }
        // --- guhbloesem trees: groves with open lawn between ------------------------------------------------------------------------
        for (int cx = Math.floorDiv(x0, BOOM_CEL); cx <= Math.floorDiv(x0 + 15, BOOM_CEL); cx++) {
            for (int cz = Math.floorDiv(z0, BOOM_CEL); cz <= Math.floorDiv(z0 + 15, BOOM_CEL); cz++) {
                long hash = m.hash(cx, cz, 6301);
                int x = cx * BOOM_CEL + (int) (BioModel.kans(hash, 1) * BOOM_CEL), z = cz * BOOM_CEL + (int) (BioModel.kans(hash, 2) * BOOM_CEL);
                if ((x >> 4) != k.cx || (z >> 4) != k.cz || !vlak(m, x, z)) {
                    continue;
                }
                double bos = bos(m, x, z), worp = BioModel.kans(hash, 0);
                if (worp >= (bos > BOS_VANAF ? BOOM_BOS : bos > BOS_RAND ? BOOM_RAND : BOOM_GAZON)) {
                    continue;
                }
                // sizes: a grove mixes small, middle and large (its heart now and then a giant); a tree alone is middle or large
                double w2 = BioModel.kans(hash, 5);
                int maat = bos > BOS_VANAF ? (w2 < 0.25 ? 0 : w2 < 0.65 ? 1 : w2 < 0.965 || bos < 0.25 ? 2 : 3) : w2 < 0.45 ? 1 : 2;
                // near the water it leans towards it
                int[] leun = b.water(x, z) < 15 ? b.naarWater(x, z) : new int[]{0, 0};
                MeerTerrein.Boom boom = new MeerTerrein.Boom(x, z, maat, leun[0], leun[1], false, hash);
                double[] kroon = BloesemBoom.kroon(boom);
                if (!boomMag(level, m, b, x, z, kroon[0], kroon[1], kroon[2])) {
                    if (maat == 0 || leun[0] == 0 && leun[1] == 0) {
                        continue;
                    }
                    // (no room for it: a small upright one may still fit)
                    boom = new MeerTerrein.Boom(x, z, 0, 0, 0, false, hash);
                    kroon = BloesemBoom.kroon(boom);
                    if (!boomMag(level, m, b, x, z, kroon[0], kroon[1], kroon[2])) {
                        continue;
                    }
                }
                gezet += BloesemBoom.bouw(level, boom, m.hoogte(x, z) + 1);
            }
        }
        // --- esdoorn accents: by a fall, and beside a koi pool ----------------------------------------------------------------------
        for (int cx = Math.floorDiv(x0, ESDOORN_CEL); cx <= Math.floorDiv(x0 + 15, ESDOORN_CEL); cx++) {
            for (int cz = Math.floorDiv(z0, ESDOORN_CEL); cz <= Math.floorDiv(z0 + 15, ESDOORN_CEL); cz++) {
                long hash = m.hash(cx, cz, 6601);
                int x = cx * ESDOORN_CEL + (int) (BioModel.kans(hash, 1) * ESDOORN_CEL), z = cz * ESDOORN_CEL + (int) (BioModel.kans(hash, 2) * ESDOORN_CEL);
                if ((x >> 4) != k.cx || (z >> 4) != k.cz || BioModel.kans(hash, 0) >= ESDOORN_VAL || b.val(x, z) > 13.5f || !vlak(m, x, z)
                        || !boomMag(level, m, b, x, z, x, z, 3.6)) {
                    continue;
                }
                gezet += esdoorn(level, RandomSource.create(hash), x, m.hoogte(x, z) + 1, z);
            }
        }
        for (DalTerrein.Poel pl : poelen) {
            long hash = m.hash((long) Math.floor(pl.x()), (long) Math.floor(pl.z()), 6602);
            if (BioModel.kans(hash, 0) >= ESDOORN_POEL) {
                continue;
            }
            // (a few tries around the pool: the first place the model allows; every chunk makes the same tries)
            for (int i = 0; i < 6; i++) {
                double hoek = (BioModel.kans(hash, 1) + i / 6.0) * 6.283, ver = pl.straal() * 1.25 + 5.5 + 2 * BioModel.kans(hash, 2 + i);
                int x = (int) Math.round(pl.x() + Math.cos(hoek) * ver), z = (int) Math.round(pl.z() + Math.sin(hoek) * ver);
                if (!vlak(m, x, z) || !boomMagModel(m, x, z)) {
                    continue;
                }
                if ((x >> 4) == k.cx && (z >> 4) == k.cz && boomMag(level, m, b, x, z, x, z, 3.6)) {
                    gezet += esdoorn(level, RandomSource.create(hash), x, m.hoogte(x, z) + 1, z);
                }
                break;
            }
        }
        // --- crooked little trees: on boulders, and on ledges of the rock (most near a fall), leaning out ---------------------------
        for (DalTerrein.Kei kei : DalTerrein.keien(m, k.cx, k.cz)) {
            if ((kei.x() >> 4) != k.cx || (kei.z() >> 4) != k.cz || kei.hoog() < 2 || kei.straal() < 1.8) {
                continue;
            }
            long hash = m.hash(kei.x(), kei.z(), 6401);
            int h = m.hoogte(kei.x(), kei.z());
            if (BioModel.kans(hash, 0) < BONSAI_KANS && (m.vlag(kei.x(), kei.z()) & DalTerrein.VORM) != 0 && m.water(kei.x(), kei.z()) == Kaart.GEEN
                    && h > DalTerrein.HOOGTE[kei.terras()] && !inGebouw(level, kei.x(), h + 1, kei.z(), 2)) {
                gezet += bonsai(level, RandomSource.create(hash), kei.x(), h + 1, kei.z(), omlaag(m, kei.x(), kei.z(), h));
            }
        }
        for (int o = 0; o < 256; o++) {
            if (k.terras[o] < 0 || k.meng[o] < 1f || k.water[o] != Kaart.GEEN || (k.vlag[o] & (DalTerrein.VORM | DalTerrein.TREDE | Kaart.LIP)) != DalTerrein.VORM) {
                continue;
            }
            int x = x0 + (o & 15), z = z0 + (o >> 4), h = k.hoogte[o];
            if (h < DalTerrein.HOOGTE[k.terras[o]] + 2) {
                continue;
            }
            long hash = m.hash(x, z, 6402);
            if (BioModel.kans(hash, 0) >= (b.val(x, z) < 8 ? BONSAI_VAL : BONSAI_RICHEL)) {
                continue;
            }
            // (the top of a ledge: no neighbour higher, and a drop on one side to lean over)
            Direction naar = omlaag(m, x, z, h);
            boolean top = naar != null;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                top &= m.hoogte(x + d.getStepX(), z + d.getStepZ()) <= h || m.water(x + d.getStepX(), z + d.getStepZ()) != Kaart.GEEN;
            }
            if (top && !inGebouw(level, x, h + 1, z, 2)) {
                gezet += bonsai(level, RandomSource.create(hash), x, h + 1, z, naar);
            }
        }
        // --- groves of guh-bamboe against a rock face ---------------------------------------------------------------------------------
        for (int cx = Math.floorDiv(x0, BAMBOE_CEL); cx <= Math.floorDiv(x0 + 15, BAMBOE_CEL); cx++) {
            for (int cz = Math.floorDiv(z0, BAMBOE_CEL); cz <= Math.floorDiv(z0 + 15, BAMBOE_CEL); cz++) {
                long hash = m.hash(cx, cz, 6501);
                int x = cx * BAMBOE_CEL + 2 + (int) (BioModel.kans(hash, 1) * (BAMBOE_CEL - 4)), z = cz * BAMBOE_CEL + 2 + (int) (BioModel.kans(hash, 2) * (BAMBOE_CEL - 4));
                if ((x >> 4) != k.cx || (z >> 4) != k.cz || BioModel.kans(hash, 0) >= BAMBOE_KANS || !vlak(m, x, z) || b.water(x, z) < 6) {
                    continue;
                }
                int h = m.hoogte(x, z);
                boolean wand = false;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    for (int a = 2; a <= 5 && !wand; a++) {
                        wand = b.h(x + d.getStepX() * a, z + d.getStepZ() * a) >= h + 4;
                    }
                }
                if (!wand || b.water(x, z) < STAM_RECHTS && rivier(m, x, z)[0] == 0 || inGebouw(level, x, h + 1, z, 5)) {
                    continue;
                }
                gezet += bamboe(level, m, b, hash, x, z, 3.6 + 2.4 * BioModel.kans(hash, 3));
            }
        }
        return gezet;
    }

    /** The grove noise: high where the guhbloesem trees stand together. */
    private static double bos(BioModel m, int x, int z) {
        return m.ruis(BioModel.R_DETAIL, x * 0.4 + 4000, z * 0.4 - 2500);
    }

    /** The model's part of {@link #boomMag} for an esdoorn beside a pool (so neighbouring chunks agree which try is taken). */
    private static boolean boomMagModel(BioModel m, int x, int z) {
        int h = m.hoogte(x, z);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (m.water(x + dx, z + dz) != Kaart.GEEN) {
                    return false;
                }
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && (m.hoogte(x + dx, z + dz) != h || m.vlag(x + dx, z + dz) != 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The side on which the ground (or water) lies lowest under a column at height h, at least two down; or null. */
    private static Direction omlaag(BioModel m, int x, int z, int h) {
        Direction uit = null;
        int laagst = h - 1;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int a = 1; a <= 2; a++) {
                int bx = x + d.getStepX() * a, bz = z + d.getStepZ() * a;
                int peil = m.water(bx, bz) != Kaart.GEEN ? m.water(bx, bz) : m.hoogte(bx, bz);
                if (m.meng(bx, bz) >= 1f && peil < laagst) {
                    laagst = peil;
                    uit = d;
                }
            }
        }
        return uit;
    }

    private static boolean inGebouw(WorldGenLevel level, int x, int y, int z, int r) {
        for (int dx = -r; dx <= r; dx += r) {
            for (int dz = -r; dz <= r; dz += r) {
                BlockPos pos = new BlockPos(x + dx, y, z + dz);
                if (BouwRuimte.inBuilding(level, pos) || BouwRuimte.inBuilding(level, pos.above(4))) {
                    return true;
                }
            }
        }
        return false;
    }

    static BlockState blad(BlockState s) {
        return s.hasProperty(LeavesBlock.PERSISTENT) ? s.setValue(LeavesBlock.PERSISTENT, true) : s;
    }

    private static boolean zet(WorldGenLevel level, BlockPos.MutableBlockPos p, BlockState s) {
        if (level.isEmptyBlock(p) || level.getBlockState(p).canBeReplaced()) {
            level.setBlock(p, s, 2);
            return true;
        }
        return false;
    }

    /**
     * An esdoorn accent tree with its foot at (x, y, z): a short trunk and a rounded, slightly flattened crown. The crown
     * is one colour; the other colour only comes as a soft cloud in its sunny top (never block by block: that looked like
     * a hard mosaic in the sketch).
     */
    static int esdoorn(WorldGenLevel level, RandomSource rand, int x, int y, int z) {
        BlockState stam = Bio.blok("esdoorn_stam", Blocks.CHERRY_LOG).defaultBlockState();
        boolean rood = rand.nextFloat() < 0.6f;
        BlockState een = blad(Bio.blok(rood ? "esdoorn_bladeren_rood" : "esdoorn_bladeren_oranje", Blocks.CHERRY_LEAVES).defaultBlockState());
        BlockState ander = blad(Bio.blok(rood ? "esdoorn_bladeren_oranje" : "esdoorn_bladeren_rood", Blocks.CHERRY_LEAVES).defaultBlockState());
        int hoog = 4 + rand.nextInt(2), gezet = 0;
        double straal = 2.9 + rand.nextFloat() * 0.7, wolkX = (rand.nextFloat() - 0.5) * 3, wolkZ = (rand.nextFloat() - 0.5) * 3, fase = rand.nextFloat() * 6.283f;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = (int) Math.ceil(straal);
        int cy = y + hoog;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -2; dy <= 3; dy++) {
                    double hoek = Math.atan2(dz, dx);
                    double rand2 = straal * (1 + 0.10 * Math.sin(3 * hoek + fase) + 0.06 * Math.sin(5 * hoek - fase));
                    double q = (dx * dx + dz * dz) / (rand2 * rand2) + (dy > 0 ? dy * dy / 6.2 : dy * dy / 2.4);
                    if (q > 1.0 || q > 0.82 && rand.nextFloat() < 0.35f) {
                        continue;
                    }
                    // the cloud of the other colour: a soft ball high in the crown, a little to one side
                    double w = (dx - wolkX) * (dx - wolkX) + (dz - wolkZ) * (dz - wolkZ) + (dy - 2.2) * (dy - 2.2) * 1.6;
                    if (zet(level, p.set(x + dx, cy + dy, z + dz), w < 3.4 ? ander : een)) {
                        gezet++;
                    }
                }
            }
        }
        for (int dy = 0; dy < hoog + 1; dy++) {
            level.setBlock(p.set(x, y + dy, z), stam, 2);
            gezet++;
        }
        return gezet;
    }

    /**
     * A crooked little tree on a rock, its foot at (x, y, z): two blocks of trunk, a jog to the side, and two or three flat
     * pads of leaves at different heights (a cloud-pruned garden tree). Most are pale green, some blossom pink, a few red.
     */
    static int bonsai(WorldGenLevel level, RandomSource rand, int x, int y, int z, Direction naar) {
        BlockState stam = Bio.blok("esdoorn_stam", Blocks.CHERRY_LOG).defaultBlockState();
        float soort = rand.nextFloat();
        BlockState loof = blad(soort < 0.62f ? DalBlokken.BONSAIBLAD.get().defaultBlockState()
                : soort < 0.87f ? Bio.blok("guhbloesem_leaves", Blocks.CHERRY_LEAVES).defaultBlockState()
                : Bio.blok("esdoorn_bladeren_rood", Blocks.CHERRY_LEAVES).defaultBlockState());
        Direction zij = Direction.Plane.HORIZONTAL.getRandomDirection(rand);
        if (naar != null) {
            // (biomes3 fix-dal: it leans out over the drop)
            zij = naar;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        if (!level.isEmptyBlock(p.set(x, y, z)) || !level.isEmptyBlock(p.set(x, y + 3, z))) {
            return 0;
        }
        int gezet = 0, hoog = 1 + rand.nextInt(2);
        for (int dy = 0; dy < hoog; dy++) {
            level.setBlock(p.set(x, y + dy, z), stam, 2);
        }
        int tx = x + zij.getStepX(), tz = z + zij.getStepZ(), ty = y + hoog;
        BlockState dwars = stam.hasProperty(RotatedPillarBlock.AXIS) ? stam.setValue(RotatedPillarBlock.AXIS, zij.getAxis()) : stam;
        level.setBlock(p.set(x, ty, z), dwars, 2);
        level.setBlock(p.set(tx, ty, tz), dwars, 2);
        level.setBlock(p.set(tx, ty + 1, tz), stam, 2);
        gezet += hoog + 3;
        // the top pad over the end of the jog, a smaller one on the other side lower down, sometimes a third at the tip
        gezet += kussen(level, rand, tx, ty + 2, tz, loof, true);
        gezet += kussen(level, rand, x - zij.getStepX(), ty + 1, z - zij.getStepZ(), loof, false);
        if (rand.nextFloat() < 0.5f) {
            gezet += kussen(level, rand, tx + zij.getStepX() * 2, ty, tz + zij.getStepZ() * 2, loof, false);
        }
        return gezet;
    }

    private static int kussen(WorldGenLevel level, RandomSource rand, int x, int y, int z, BlockState loof, boolean groot) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int gezet = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean hoek = dx != 0 && dz != 0;
                if (hoek && (!groot || rand.nextFloat() < 0.45f)) {
                    continue;
                }
                if (zet(level, p.set(x + dx, y, z + dz), loof)) {
                    gezet++;
                }
            }
        }
        if (groot && zet(level, p.set(x, y + 1, z), loof)) {
            gezet++;
        }
        return gezet;
    }

    /** A grove of guh-bamboe around (x, z): stalks on most of the plain ground within the radius (thinner at its edge), on moss. */
    private static int bamboe(WorldGenLevel level, BioModel m, Buurt b, long hash, int x, int z, double straal) {
        BlockState stengel = Bio.blok("guh_bamboe", Blocks.BAMBOO).defaultBlockState();
        stengel = DalVulling.met(stengel, "stage", "0");
        BlockState mos = DalBlokken.MOS.get().defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = (int) Math.ceil(straal), gezet = 0;
        double f1 = BioModel.kans(hash, 8) * 6.283, f2 = BioModel.kans(hash, 9) * 6.283;
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double hoek = Math.atan2(dz, dx), rand = straal * (1 + 0.18 * Math.sin(2 * hoek + f1) + 0.12 * Math.sin(3 * hoek + f2));
                double q = (dx * dx + dz * dz) / (rand * rand);
                int px = x + dx, pz = z + dz;
                if (q > 1 || !vlak(m, px, pz) || b.water(px, pz) < 4.5) {
                    continue;
                }
                int h = m.hoogte(px, pz);
                if (!grond(level.getBlockState(p.set(px, h, pz))) || BouwRuimte.inBuilding(level, p)) {
                    continue;
                }
                level.setBlock(p, mos, 2);
                long kh = m.hash(px, pz, 6502);
                if (BioModel.kans(kh, 0) >= 0.66 - 0.3 * q || !level.isEmptyBlock(p.set(px, h + 1, pz))) {
                    continue;
                }
                int hoog = Math.max(4, (int) Math.round((6 + BioModel.kans(kh, 1) * 4.5) * (1 - 0.3 * q)));
                for (int i = 0; i < hoog; i++) {
                    if (!level.isEmptyBlock(p.set(px, h + 1 + i, pz))) {
                        hoog = i;
                        break;
                    }
                }
                for (int i = 0; i < hoog; i++) {
                    String loof = i == hoog - 1 ? (hoog == 1 ? "small" : "large") : i == hoog - 2 ? "small" : "none";
                    level.setBlock(p.set(px, h + 1 + i, pz), DalVulling.met(stengel, "leaves", loof), 2);
                    gezet++;
                }
            }
        }
        return gezet;
    }

    private DalPlanten() {
    }
}
