package nl.juiced.guhs.feature.bio.wereld;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.juiced.guhs.Guhs;
import org.jspecify.annotations.Nullable;

/**
 * biomes3 wereld: THE terrain model of the three biomes, a pure function of the world's noises and the position.
 * <p>
 * Why a model in Java and not density-function JSON: a structure is positioned before any terrain exists, and it has to
 * know where the river, the waterfall, the lake island and the free air are ({@code guhs:bio_plek}). So the shape of the
 * land is worked out here, per column, and everything else reads it:
 * <ul>
 *   <li>{@link BioTerreinFunctie} ({@code guhs:bio_terrein}, wraps the Guhmensie's final density): the solid blocks;</li>
 *   <li>{@link BioRegioFunctie} ({@code guhs:bio_regio}): 1 inside a biome, which moves the multi-noise climate to that
 *       biome's entry (a hard step: no rings of other biomes around ours);</li>
 *   <li>{@link BioVulling} (feature {@code guhs:bio_wereld_vulling}): water, river beds, clouds, lifts;</li>
 *   <li>{@link BioPlekken} / {@link BioPlekStructure}: the spots for buildings.</li>
 * </ul>
 * The regions. One low noise ({@link #R_DAL}, about 1000 blocks) makes a "dal": where it rises above {@link #DAL_VANAF}
 * the land steps down in terraces (the Klaterdal, {@link DalTerrein}) to a lake in the middle (the Bloesemmeertje,
 * {@link MeerTerrein}); so the two always lie together and every river ends in the lake. A second noise
 * ({@link #R_WEIDE}) makes the rare Wolkenweide ({@link WolkTerrein}). Both give way to every older region (deep sea,
 * Knuffeldal, Guhpolder, tundra, Guhwai'i, Bleekwoud): {@link #masker}. Nothing of ours touches a column outside these
 * regions, so the rest of the Guhmensie generates exactly as before.
 * <p>
 * The value everything hangs on is "how far into the region" a column lies: {@link #eDal} / {@link #eWeide} (0 at the
 * edge, rising inward; the region noise above its threshold, cut down near an older region).
 */
public final class BioModel {
    /** The noises, in the order of the {@code ruis} list of the two density functions (tools/features/bio_wereld.py). */
    public static final String[] RUIS = {"klaterdal_regio", "wolkenweide_regio", "klaterdal_rivier", "klaterdal_detail",
            "guhmension_zee", "guhmension_knuffel", "guhmension_polder", "sneeuwguhtoendra", "guhwaii", "bleekwoud"};
    public static final int R_DAL = 0, R_WEIDE = 1, R_RIVIER = 2, R_DETAIL = 3, R_ZEE = 4, R_KNUFFEL = 5, R_POLDER = 6, R_TOENDRA = 7,
            R_GUHWAII = 8, R_BLEEKWOUD = 9;

    // <wereld-plaatsing> (the shares: BioWereldGameTests.bioWereldAandeel; only the kern/placement owner edits these)
    /** The dal region (Klaterdal + Bloesemmeertje) starts where its noise is this high. */
    public static final double DAL_VANAF = 0.39;
    /** The Wolkenweide starts where its noise is this high. */
    public static final double WEIDE_VANAF = 0.62;
    /** The older regions: {noise, the value where our regions must have ended}; all well before their own terrain starts. */
    // biomes3 wereld-meer: MASKERS and MASKER_SCHAAL are package-visible now (MeerTerrein.afstand reads them); values untouched
    static final double[][] MASKERS = {{R_ZEE, 0.30}, {R_KNUFFEL, 0.45}, {R_POLDER, 0.54}, {R_TOENDRA, 0.44}, {R_GUHWAII, 0.34},
            {R_BLEEKWOUD, 0.60}};
    /** How fast our regions fade towards an older one: within 0.08 of its noise below the value above, a dal is squeezed (no lake there). */
    static final double MASKER_SCHAAL = 2.0;
    /** The Wolkenweide keeps this far (in dal noise) from a dal. */
    private static final double WEIDE_DAL_AF = 0.05;
    // </wereld-plaatsing>

    /** Below this y the Guhmensie's own underground stays (caves, the Gatenkaasgrotten). */
    public static final int ONDER = 36;
    /** The biomes start here (the cave biomes below stay what they were). */
    public static final int BIOME_ONDER = 40;
    /** Columns around a chunk the terrain passes look at. */
    static final int MARGE = 4;

    private static final Map<NormalNoise, BioModel> MODELLEN = new WeakHashMap<>();

    final NormalNoise[] ruis;
    /** A seed of this world for everything that is hashed (islands, clouds): taken from the noises, so it follows the world seed. */
    public final long zaad;
    private final ThreadLocal<Kaart[]> kaarten = ThreadLocal.withInitial(() -> new Kaart[CACHE]);
    private static final int CACHE = 256;
    // biomes3 merge: behind the per-thread cache one cache for all threads. World generation asks for a chunk's map from
    // many worker threads (structure starts, biomes, density, the surface, each feature), and each used to work it out
    // again: 25-35 times per generated chunk. A Kaart is never changed after bouw() returns and bouw() is a pure function
    // of the model and the chunk, so sharing is safe; two threads that miss at the same moment both build the same map
    // and one of them wins (no lock is held while building: bouw() asks for neighbouring maps itself).
    // Bound: at most GEDEELD_MAX maps (about 8 KB each when not empty), then the whole cache is dropped.
    // GUHS_BIO_KAART_CACHE=0 in the environment switches it off (to measure).
    private static final boolean GEDEELD = !"0".equals(System.getenv("GUHS_BIO_KAART_CACHE"));
    private static final int GEDEELD_MAX = 2048;
    private final ConcurrentHashMap<Long, Kaart> gedeeld = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.LongAdder GEBOUWD = new java.util.concurrent.atomic.LongAdder(),
            GEDEELD_RAAK = new java.util.concurrent.atomic.LongAdder(), BOUW_NS = new java.util.concurrent.atomic.LongAdder();

    /** How many chunk maps were worked out and how many came from the shared cache since the last call (resets the count). */
    public static String teller() {
        long n = GEBOUWD.sumThenReset(), raak = GEDEELD_RAAK.sumThenReset(), ns = BOUW_NS.sumThenReset();
        return String.format(java.util.Locale.ROOT, "chunk maps: %d worked out in %.0f ms, %d taken from the shared cache (%s)", n, ns / 1e6, raak, GEDEELD ? "on" : "OFF");
    }
    /** Per-cell results of the biome classes (lake islands, floating islands, clouds), keyed by {@link #sleutel}. */
    final Map<Long, Object> cellen = new ConcurrentHashMap<>();

    private BioModel(NormalNoise[] ruis) {
        this.ruis = ruis;
        this.zaad = Double.doubleToLongBits(ruis[R_DAL].getValue(12345.5, 0, -54321.5)) ^ Double.doubleToLongBits(ruis[R_WEIDE].getValue(-777.5, 0, 999.5));
    }

    /** The model of a world (dimension) by its random state. */
    public static BioModel van(RandomState random) {
        NormalNoise[] ruis = new NormalNoise[RUIS.length];
        for (int i = 0; i < ruis.length; i++) {
            ruis[i] = random.getOrCreateNoise(ResourceKey.create(Registries.NOISE, Guhs.id(RUIS[i])));
        }
        return van(ruis);
    }

    /** The model of wired noise holders (a density function after {@code mapAll}); null while they are not wired. */
    @Nullable
    static BioModel van(List<DensityFunction.NoiseHolder> holders) {
        if (holders.size() != RUIS.length) {
            throw new IllegalStateException("biomes3: the ruis list must hold " + RUIS.length + " noises: " + String.join(", ", RUIS));
        }
        NormalNoise[] ruis = new NormalNoise[RUIS.length];
        for (int i = 0; i < ruis.length; i++) {
            ruis[i] = holders.get(i).noise();
            if (ruis[i] == null) {
                return null;
            }
        }
        return van(ruis);
    }

    private static BioModel van(NormalNoise[] ruis) {
        synchronized (MODELLEN) {
            BioModel m = MODELLEN.get(ruis[0]);
            if (m == null) {
                m = new BioModel(ruis);
                MODELLEN.put(ruis[0], m);
            }
            return m;
        }
    }

    /** Forget everything that was worked out (a new server start: the reserved air of the structures may have changed). */
    public static void vergeet() {
        synchronized (MODELLEN) {
            MODELLEN.clear();
        }
    }

    // --- noises and hashes ----------------------------------------------------------------------------------------------------
    public double ruis(int welke, double x, double z) {
        return ruis[welke].getValue(x, 0, z);
    }

    static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    /** A hash of (a, b) with a salt, for this world. */
    public long hash(long a, long b, long zout) {
        return mix(mix(zaad ^ zout * 0x9E3779B97F4A7C15L) + a * 0x632BE59BD9B4E019L + mix(b + 0x2545F4914F6CDD1DL));
    }

    /** A number in [0, 1) from a hash and a draw index. */
    public static double kans(long hash, int n) {
        return (mix(hash + n * 0x9E3779B97F4A7C15L) >>> 11) * 0x1.0p-53;
    }

    static long sleutel(int soort, int a, int b) {
        return ((long) soort << 58) ^ ((long) a & 0x1FFFFFFFL) << 29 ^ ((long) b & 0x1FFFFFFFL);
    }

    public static double zacht(double t) {
        t = t <= 0 ? 0 : t >= 1 ? 1 : t;
        return t * t * (3 - 2 * t);
    }

    // --- the regions --------------------------------------------------------------------------------------------------------
    /** How far the older regions are: positive = room, 0 or less = too close (already scaled to dal noise). */
    public double masker(double x, double z) {
        double m = 10;
        for (double[] k : MASKERS) {
            m = Math.min(m, (k[1] - ruis[(int) k[0]].getValue(x, 0, z)) * MASKER_SCHAAL);
            if (m <= 0) {
                break;
            }
        }
        return m;
    }

    /** How far into a dal (x, z) lies: at most 0 outside, rising inward (0.03: past the rim; see {@link DalTerrein}). */
    public double eDal(double x, double z) {
        double r = ruis[R_DAL].getValue(x, 0, z) - DAL_VANAF;
        return r <= 0 ? r : Math.min(r, masker(x, z));
    }

    /** How far into a Wolkenweide (x, z) lies: at most 0 outside, rising inward. */
    public double eWeide(double x, double z) {
        double r = ruis[R_WEIDE].getValue(x, 0, z) - WEIDE_VANAF;
        if (r <= 0) {
            return r;
        }
        r = Math.min(r, -WEIDE_DAL_AF - (ruis[R_DAL].getValue(x, 0, z) - DAL_VANAF));
        return r <= 0 ? r : Math.min(r, masker(x, z));
    }

    // --- the chunk maps -------------------------------------------------------------------------------------------------------
    /** What we know about chunk (cx, cz); cached per thread. */
    public Kaart kaart(int cx, int cz) {
        Kaart[] cache = kaarten.get();
        int slot = (cx * 31 + cz) & (CACHE - 1);
        Kaart k = cache[slot];
        if (k != null && k.cx == cx && k.cz == cz) {
            return k;
        }
        if (!GEDEELD) {
            return cache[slot] = getimed(cx, cz);
        }
        long sleutel = (long) cx << 32 | cz & 0xFFFFFFFFL;
        k = gedeeld.get(sleutel);
        if (k == null) {
            k = getimed(cx, cz);
            // (only maps that took a terrain pass are shared: "nothing of ours near here" costs 25 noise samples to find out
            // again, and those chunks are nearly all of the world: they would push the real maps out)
            if (k.duur) {
                if (gedeeld.size() >= GEDEELD_MAX) {
                    gedeeld.clear();
                }
                gedeeld.put(sleutel, k);
            }
        } else {
            GEDEELD_RAAK.increment();
        }
        return cache[slot] = k;
    }

    private Kaart getimed(int cx, int cz) {
        long t0 = System.nanoTime();
        Kaart k = bouw(cx, cz);
        if (k.duur) {
            // (a map that asks for its neighbours' maps counts their time too: the total is an upper bound)
            GEBOUWD.increment();
            BOUW_NS.add(System.nanoTime() - t0);
        }
        return k;
    }

    private Kaart bouw(int cx, int cz) {
        int x0 = (cx << 4) - MARGE, z0 = (cz << 4) - MARGE;
        double stap = (16 + 2 * MARGE - 1) / 4.0;
        boolean dal = false, weide = false;
        for (int i = 0; i <= 4 && !(dal && weide); i++) {
            for (int j = 0; j <= 4; j++) {
                double x = x0 + i * stap, z = z0 + j * stap;
                if (!dal && ruis[R_DAL].getValue(x, 0, z) > DAL_VANAF - 0.04) {
                    dal = true;
                }
                if (!weide && ruis[R_WEIDE].getValue(x, 0, z) > WEIDE_VANAF - 0.07) {
                    weide = true;
                }
            }
        }
        if (!dal && !weide) {
            return Kaart.leeg(cx, cz);
        }
        Kaart k = new Kaart(cx, cz);
        boolean iets = false;
        if (dal) {
            iets = DalTerrein.vul(this, k);
        }
        if (weide) {
            iets |= WolkTerrein.vul(this, k);
        }
        if (!iets) {
            k = Kaart.leeg(cx, cz);
        }
        k.duur = true;
        return k;
    }

    // --- single columns (structure spots, tests, commands) -------------------------------------------------------------------
    public byte soort(int x, int z) {
        Kaart k = kaart(x >> 4, z >> 4);
        return k.leeg ? Kaart.BUITEN : k.soort[Kaart.index(x, z)];
    }

    public float meng(int x, int z) {
        Kaart k = kaart(x >> 4, z >> 4);
        return k.leeg ? 0f : k.meng[Kaart.index(x, z)];
    }

    /** Our top solid block (only meaningful where {@link #meng} is 1). */
    public int hoogte(int x, int z) {
        Kaart k = kaart(x >> 4, z >> 4);
        return k.leeg ? Kaart.GEEN : k.hoogte[Kaart.index(x, z)];
    }

    public int water(int x, int z) {
        Kaart k = kaart(x >> 4, z >> 4);
        return k.leeg ? Kaart.GEEN : k.water[Kaart.index(x, z)];
    }

    public int terras(int x, int z) {
        Kaart k = kaart(x >> 4, z >> 4);
        return k.leeg ? -1 : k.terras[Kaart.index(x, z)];
    }

    public int vlag(int x, int z) {
        Kaart k = kaart(x >> 4, z >> 4);
        return k.leeg ? 0 : k.vlag[Kaart.index(x, z)];
    }

    /** Dry ground of ours at full strength (no water, no lip, not the rim)? */
    public boolean droog(int x, int z) {
        Kaart k = kaart(x >> 4, z >> 4);
        if (k.leeg) {
            return false;
        }
        int i = Kaart.index(x, z);
        return k.meng[i] >= 1f && k.water[i] == Kaart.GEEN && (k.vlag[i] & Kaart.LIP) == 0;
    }

    /** Is the air of the Wolkenweide free of our islands in this column between two heights (both included)? */
    public boolean luchtVrij(int x, int z, int yOnder, int yBoven) {
        Kaart k = kaart(x >> 4, z >> 4);
        if (k.leeg) {
            return true;
        }
        int[] sp = k.spans[Kaart.index(x, z)];
        if (sp != null) {
            for (int s = 0; s < sp.length; s += 2) {
                if (sp[s] <= yBoven && sp[s + 1] >= yOnder) {
                    return false;
                }
            }
        }
        return true;
    }
}
