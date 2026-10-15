package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * biomes3 fix-plaatsing: "one per region". A {@code guhs:bio_plek} structure with the field {@code per_regio} does not
 * start wherever its structure set happens to try a chunk with a fitting spot (which gave a valley anything from no
 * weebhuisje to six): each region ({@link BioRegio}: one valley with its lake, one Wolkenweide) gets it at most once,
 * at a spot that is chosen HERE, from the terrain model alone, the same in every run of a world and whatever is
 * generated first:
 * <ul>
 *   <li>the region takes part with chance {@code per_regio} (1: every region that has a fitting spot);</li>
 *   <li>the spot is looked for all over the region, in an order drawn from the region's own number: random places on
 *       the terraces for a valley building, around the shore for a lake-shore building, on the large islands for an
 *       island building, on the meadow for the Wolkenweide. The first chunk with a spot of the kind is THE place;</li>
 *   <li>with {@code twee_vanaf}, a region whose lake (or meadow) has at least that mean radius gets a second one, on the
 *       far side of the first.</li>
 * </ul>
 * The structure set still decides which chunk STARTS the structure (its random-spread grid: {@code locate} and the
 * Superkompas walk that grid), but the building stands on the chosen spot, which may lie in another chunk of the same
 * grid cell: {@link #inCel}.
 * <p>
 * Buildings in the air ({@code lucht}) of one Wolkenweide are chosen together ({@link #lucht}), the widest first, so
 * they never stand in each other's air; that choice asks no chunk map (the meadow's ground is a noise), because the
 * chunk maps of the Wolkenweide themselves ask where the buildings are ({@link Luchtruim}).
 */
public final class RegioKeuze {
    /** How many chunks are tried at most for one building in one region. */
    private static final int POGINGEN = 140;
    /** A building in the air chooses among this many places that fit. */
    private static final int KEUS = 14;
    private static final int SOORT_PLEK = 1, SOORT_LUCHT = 2;
    private static final List<BioPlekken.Plek> GEEN = List.of();

    /** A chosen building in the air: its anchor. */
    public record Gekozen(Luchtruim.Bouwsel bouwsel, int x, int y, int z) {
    }

    /** Is this spot kind one of the Wolkenweide? */
    static boolean weide(BioPlekken.Soort soort) {
        return soort == BioPlekken.Soort.WEIDE || soort == BioPlekken.Soort.LUCHT || soort == BioPlekken.Soort.ZWEEFEILAND;
    }

    /**
     * Is the meadow whole (no blend into other land, no rim) at sixteen points around this column, on rings of
     * {@code straal} and half of it? From the region values alone: no chunk map is asked.
     */
    public static boolean heleWeide(BioModel m, int x, int z, int straal) {
        if (m.eWeide(x, z) < WolkTerrein.RAND) {
            return false;
        }
        for (int ring = 1; ring <= 2; ring++) {
            double r = straal * ring / 2.0;
            for (int i = 0; i < 8; i++) {
                if (m.eWeide(x + (int) Math.round(Math.cos(i * Math.PI / 4) * r), z + (int) Math.round(Math.sin(i * Math.PI / 4) * r)) < WolkTerrein.RAND) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The buildings in the air of one Wolkenweide (cached in the model). */
    @SuppressWarnings("unchecked")
    public static List<Gekozen> lucht(BioModel m, BioRegio g) {
        long sleutel = BioModel.sleutel(SOORT_LUCHT, g.cx, g.cz);
        Object bekend = m.keuzes.get(sleutel);
        if (bekend != null) {
            return (List<Gekozen>) bekend;
        }
        List<Gekozen> uit = new ArrayList<>();
        for (Luchtruim.Bouwsel b : Luchtruim.perRegio()) {
            BioPlekStructure s = b.structuur();
            long h = m.hash(g.cx, g.cz, 7700L + s.zout());
            if (BioModel.kans(h, 0) >= s.perRegio().orElse(0.0)) {
                continue;
            }
            // of the first places that fit, the one furthest from where the natural stacks of islands stand: a building in a
            // gap between the stacks costs the sky little, one on a stack's spot costs the stack
            int beste = -1, besteX = 0, besteZ = 0, passend = 0;
            for (int poging = 0; poging < POGINGEN && passend < KEUS; poging++) {
                // (anywhere within 0.85 of the outline, evenly over its area; the middle of that chunk)
                double hoek = BioModel.kans(h, 10 + 2 * poging) * 2 * Math.PI, deel = 0.85 * Math.sqrt(BioModel.kans(h, 11 + 2 * poging));
                double[] rand = g.rand(hoek, 0);
                int x = ((int) Math.floor(g.x + (rand[0] - g.x) * deel) >> 4 << 4) + 8, z = ((int) Math.floor(g.z + (rand[1] - g.z) * deel) >> 4 << 4) + 8;
                if (m.regio(x, z, true) != g || !heleWeide(m, x, z, s.ruimte())) {
                    continue;
                }
                boolean vrij = true;
                for (Gekozen k : uit) {
                    vrij &= Math.hypot(k.x - x, k.z - z) >= k.bouwsel.structuur().ruimte() + s.ruimte() + Luchtruim.MARGE + 3;
                }
                if (!vrij) {
                    continue;
                }
                passend++;
                int ver = 200, c = WolkTerrein.STAPEL_CEL;
                for (int ax = -1; ax <= 1; ax++) {
                    for (int az = -1; az <= 1; az++) {
                        int[] st = WolkTerrein.stapelMidden(m, Math.floorDiv(x, c) + ax, Math.floorDiv(z, c) + az);
                        if (st != null) {
                            ver = Math.min(ver, (int) Math.hypot(st[0] - x, st[1] - z));
                        }
                    }
                }
                if (ver > beste) {
                    beste = ver;
                    besteX = x;
                    besteZ = z;
                }
            }
            if (beste >= 0) {
                uit.add(new Gekozen(b, besteX, WolkTerrein.grond(m, besteX, besteZ) + s.hoogte(), besteZ));
            }
        }
        List<Gekozen> vast = List.copyOf(uit);
        m.bewaarKeuze(sleutel, vast);
        return vast;
    }

    /** The spots of a one-per-region structure in a region (none, one, or two with {@code twee_vanaf}); cached in the model. */
    @SuppressWarnings("unchecked")
    public static List<BioPlekken.Plek> plekken(BioModel m, BioRegio g, BioPlekStructure s) {
        if (s.perRegio().isEmpty() || weide(s.soort()) != g.weide) {
            return GEEN;
        }
        if (s.soort() == BioPlekken.Soort.LUCHT) {
            List<BioPlekken.Plek> uit = new ArrayList<>(1);
            for (Gekozen k : lucht(m, g)) {
                if (k.bouwsel.structuur() == s) {
                    uit.add(new BioPlekken.Plek(k.x, k.y, k.z, BioPlekken.willekeurig(m, k.x >> 4, k.z >> 4)));
                }
            }
            return uit;
        }
        long sleutel = BioModel.sleutel(SOORT_PLEK, g.cx, g.cz) ^ BioModel.mix(s.zout() * 0x9E3779B97F4A7C15L + 12345);
        Object bekend = m.keuzes.get(sleutel);
        if (bekend != null) {
            return (List<BioPlekken.Plek>) bekend;
        }
        List<BioPlekken.Plek> uit = GEEN;
        long h = m.hash(g.cx, g.cz, 7700L + s.zout());
        if (BioModel.kans(h, 0) < s.perRegio().get()) {
            uit = new ArrayList<>(2);
            double begin = BioModel.kans(h, 1) * 2 * Math.PI;
            int aantal = s.tweeVanaf() > 0 && g.straal >= s.tweeVanaf() ? 2 : 1;
            for (int nr = 0; nr < aantal; nr++) {
                BioPlekken.Plek p = zoek(m, g, s, h + 977L * nr, begin + nr * Math.PI, uit);
                if (p != null) {
                    uit.add(p);
                }
            }
            uit = List.copyOf(uit);
        }
        m.bewaarKeuze(sleutel, uit);
        return uit;
    }

    /** One spot of the structure's kind in the region, looked for in an order that hangs on h and the start direction; null when there is none. */
    private static BioPlekken.Plek zoek(BioModel m, BioRegio g, BioPlekStructure s, long h, double begin, List<BioPlekken.Plek> al) {
        java.util.Set<Long> gehad = new java.util.HashSet<>();
        BioPlekken.Soort soort = s.soort();
        switch (soort) {
            case MEER_EILAND, MEER_BOOM -> {
                // the large islands of this lake, in a drawn order; the spot is within a chunk of an island's middle or tree
                int reik = (int) (g.buiten - g.breed * 0.8);
                List<MeerTerrein.Eiland> groot = new ArrayList<>();
                for (MeerTerrein.Eiland ei : MeerTerrein.bij(m, (int) g.x - reik, (int) g.z - reik, (int) g.x + reik, (int) g.z + reik)) {
                    if (ei.groot() && m.regio(ei.x(), ei.z(), false) == g) {
                        groot.add(ei);
                    }
                }
                groot.sort(java.util.Comparator.comparingLong((MeerTerrein.Eiland ei) -> BioModel.mix(h ^ MeerTerrein.pak(ei.x(), ei.z()))));
                for (MeerTerrein.Eiland ei : groot) {
                    for (int[] om : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}}) {
                        for (int welke = 0; welke < 2; welke++) {
                            BioPlekken.Plek p = probeer(m, g, s, ((welke == 0 ? ei.boomX() : ei.x()) >> 4) + om[0], ((welke == 0 ? ei.boomZ() : ei.z()) >> 4) + om[1], gehad, al);
                            if (p != null) {
                                return p;
                            }
                        }
                    }
                }
                return null;
            }
            case WATERVAL, ROTS -> {
                // a fall is where the river or a brook crosses a terrace edge: walk rings through the valley, keep the points
                // close to a water line AND to an edge (for the tall fall: the edge that is one), and only there ask a chunk map
                int stappen = (int) Math.ceil(2 * Math.PI * (g.straal * BioRegio.REK + g.breed) / 10);
                DalTerrein.Vorm v = new DalTerrein.Vorm();
                for (int i = 0; i < stappen && gehad.size() < POGINGEN; i++) {
                    double hoek = begin + i * 2 * Math.PI / stappen;
                    for (double deel = 0.08; deel < 0.9; deel += 0.09) {
                        double[] punt = g.inDal(hoek, deel);
                        int x = (int) Math.floor(punt[0]), z = (int) Math.floor(punt[1]);
                        if (DalTerrein.bijWater(m, x, z) > 16) {
                            continue;
                        }
                        double e = m.eDal(x, z);
                        if (e < DalTerrein.RAND) {
                            continue;
                        }
                        DalTerrein.vorm(m, x, z, e, DalTerrein.steilte(m, x, z), v);
                        if (!v.kern || Math.abs(v.sn) > 14 || soort == BioPlekken.Soort.ROTS && !v.hoog) {
                            continue;
                        }
                        for (int[] om : new int[][]{{0, 0}, {8, 0}, {-8, 0}, {0, 8}, {0, -8}}) {
                            BioPlekken.Plek p = probeer(m, g, s, (x + om[0]) >> 4, (z + om[1]) >> 4, gehad, al);
                            if (p != null) {
                                return p;
                            }
                        }
                    }
                }
                return null;
            }
            case MEER_OEVER, MONDING -> {
                // once round the lake from the start direction, a chunk at a time, on three lines: the shore and the
                // river mouths lie within some twenty blocks of the lake's outline
                int stappen = (int) Math.ceil(2 * Math.PI * g.straal * BioRegio.REK / 12);
                double[] lijnen = soort == BioPlekken.Soort.MONDING ? new double[]{8, 22, -6} : new double[]{-4, -16, 8};
                for (int i = 0; i < stappen; i++) {
                    double hoek = begin + i * 2 * Math.PI / stappen;
                    for (double lijn : lijnen) {
                        double[] punt = g.rand(hoek, lijn);
                        BioPlekken.Plek p = probeer(m, g, s, (int) Math.floor(punt[0]) >> 4, (int) Math.floor(punt[1]) >> 4, gehad, al);
                        if (p != null) {
                            return p;
                        }
                    }
                    if (gehad.size() > 3 * POGINGEN) {
                        break;
                    }
                }
                return null;
            }
            default -> {
                // random places: over the terraces of the valley, or over the meadow
                for (int poging = 0; poging < 3 * POGINGEN && gehad.size() < POGINGEN; poging++) {
                    double hoek = BioModel.kans(h, 10 + 2 * poging) * 2 * Math.PI, deel = BioModel.kans(h, 11 + 2 * poging);
                    double[] punt;
                    if (g.weide) {
                        punt = g.rand(hoek, 0);
                        deel = 0.9 * Math.sqrt(deel);
                        punt[0] = g.x + (punt[0] - g.x) * deel;
                        punt[1] = g.z + (punt[1] - g.z) * deel;
                    } else {
                        punt = g.inDal(hoek, 0.04 + 0.78 * deel);
                    }
                    BioPlekken.Plek p = probeer(m, g, s, (int) Math.floor(punt[0]) >> 4, (int) Math.floor(punt[1]) >> 4, gehad, al);
                    if (p != null) {
                        return p;
                    }
                }
                return null;
            }
        }
    }

    /** The spot of the structure's kind in this chunk, if the chunk is new to this search, lies in the region and is far enough from the spots already taken. */
    private static BioPlekken.Plek probeer(BioModel m, BioRegio g, BioPlekStructure s, int cx, int cz, java.util.Set<Long> gehad, List<BioPlekken.Plek> al) {
        if (!gehad.add((long) cx << 32 | cz & 0xFFFFFFFFL) || m.regio((cx << 4) + 8, (cz << 4) + 8, false) != g && m.regio((cx << 4) + 8, (cz << 4) + 8, true) != g) {
            return null;
        }
        Optional<BioPlekken.Plek> p = BioPlekken.zoek(m, s.soort(), cx, cz, s.hoogte(), s.vlak());
        if (p.isEmpty()) {
            return null;
        }
        for (BioPlekken.Plek a : al) {
            if (Math.hypot(a.x() - p.get().x(), a.z() - p.get().z()) < Math.max(110, g.straal)) {
                return null;
            }
        }
        return p.get();
    }

    /**
     * The chosen spot of this structure that lies in the grid cell of {@code spacing} chunks holding chunk (cx, cz), or
     * nothing. Cheap where no region is near: no chunk map, no noise.
     */
    public static Optional<BioPlekken.Plek> inCel(BioModel m, BioPlekStructure s, int cx, int cz, int spacing) {
        int c0x = Math.floorDiv(cx, spacing) * spacing, c0z = Math.floorDiv(cz, spacing) * spacing;
        int x0 = c0x << 4, z0 = c0z << 4, x1 = x0 + spacing * 16 - 1, z1 = z0 + spacing * 16 - 1;
        boolean weide = weide(s.soort());
        for (BioRegio g : BioRegio.bij(m, x0, z0, x1, z1)) {
            if (g.weide != weide) {
                continue;
            }
            for (BioPlekken.Plek p : plekken(m, g, s)) {
                if (p.x() >= x0 && p.x() <= x1 && p.z() >= z0 && p.z() <= z1) {
                    return Optional.of(p);
                }
            }
        }
        return Optional.empty();
    }

    private RegioKeuze() {
    }
}
