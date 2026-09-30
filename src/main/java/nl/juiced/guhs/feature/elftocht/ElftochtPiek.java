package nl.juiced.guhs.feature.elftocht;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Where the Elf-Guhjestocht goes: on the highest point of the Guhpolder noise, one per polder.
 * <p>
 * The world is cut into cells of {@code cell} x {@code cell} chunks. In its cell the polder noise is sampled every
 * {@link #STEP} blocks (then finer around the best one): that's the cell's peak. The tour goes on a peak only if it is
 * high enough ({@code minValue}: the middle of a real polder) and no neighbouring cell has a higher peak in the same
 * polder (the noise stays at least {@code dalValue} on the straight line between them). The start chunk of the
 * structure is the chunk of the peak, so the whole ~256 x 256 tour (its anchor in the middle) stays within the eight
 * chunks around its start that structure pieces may reach.
 * <p>
 * The noise is made exactly like {@code RandomState#getOrCreateNoise} makes it (Xoroshiro from the level seed, forked
 * by the noise's id), so the placement (which only gets the seed) and the terrain agree. The Guhpolder's router uses
 * the raw noise ({@code xz_scale 1, y_scale 0}).
 */
public final class ElftochtPiek {
    /** Sample distance for the coarse search of a cell. */
    public static final int STEP = 16;

    public record Peak(int x, int z, double value) {
    }

    private record NoiseKey(long seed, Identifier id) {
    }

    private record CellKey(long seed, Identifier id, int size, int cx, int cz) {
    }

    private static final Map<NoiseKey, NormalNoise> NOISES = new ConcurrentHashMap<>();
    private static final Map<CellKey, Peak> PEAKS = new ConcurrentHashMap<>();

    /** The polder noise of this world seed, made like the terrain makes it. */
    public static NormalNoise noise(long seed, Holder<NormalNoise.NoiseParameters> params) {
        Identifier id = params.unwrapKey().orElseThrow().identifier();
        return NOISES.computeIfAbsent(new NoiseKey(seed, id), k -> NormalNoise.create(
                WorldgenRandom.Algorithm.XOROSHIRO.newInstance(seed).forkPositional().fromHashOf(id), params.value()));
    }

    /** The highest point of the noise in cell (cx, cz) of size x size chunks. */
    public static Peak peak(long seed, Holder<NormalNoise.NoiseParameters> params, int size, int cx, int cz) {
        Identifier id = params.unwrapKey().orElseThrow().identifier();
        CellKey key = new CellKey(seed, id, size, cx, cz);
        Peak known = PEAKS.get(key);
        if (known != null) {
            return known;
        }
        NormalNoise noise = noise(seed, params);
        int x0 = cx * size * 16, z0 = cz * size * 16, n = size * 16 / STEP;
        Peak best = null;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int x = x0 + STEP / 2 + i * STEP, z = z0 + STEP / 2 + j * STEP;
                double v = noise.getValue(x, 0, z);
                if (best == null || v > best.value) {
                    best = new Peak(x, z, v);
                }
            }
        }
        // finer around the best sample (still inside the cell)
        Peak fine = best;
        for (int dx = -STEP; dx <= STEP; dx += 4) {
            for (int dz = -STEP; dz <= STEP; dz += 4) {
                int x = best.x + dx, z = best.z + dz;
                if (x < x0 || z < z0 || x >= x0 + size * 16 || z >= z0 + size * 16) {
                    continue;
                }
                double v = noise.getValue(x, 0, z);
                if (v > fine.value) {
                    fine = new Peak(x, z, v);
                }
            }
        }
        if (PEAKS.size() > 20000) {
            PEAKS.clear();
        }
        PEAKS.put(key, fine);
        return fine;
    }

    /** Is the peak of cell (cx, cz) the one of its polder: high enough and not beaten by a neighbour in the same polder? */
    public static boolean isPolderPiek(long seed, Holder<NormalNoise.NoiseParameters> params, int size, int cx, int cz,
                                       double minValue, double dalValue) {
        Peak own = peak(seed, params, size, cx, cz);
        if (own.value < minValue) {
            return false;
        }
        NormalNoise noise = noise(seed, params);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                Peak other = peak(seed, params, size, cx + dx, cz + dz);
                boolean higher = other.value > own.value || other.value == own.value && (dx < 0 || dx == 0 && dz < 0);
                if (higher && samePolder(noise, own, other, dalValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Do two peaks lie in the same polder (the noise stays at least dalValue on the line between them)? */
    public static boolean samePolder(NormalNoise noise, Peak a, Peak b, double dalValue) {
        double d = Math.hypot(b.x - a.x, b.z - a.z);
        int n = Math.max(1, (int) (d / 12));
        for (int i = 1; i < n; i++) {
            double t = i / (double) n;
            if (noise.getValue(a.x + (b.x - a.x) * t, 0, a.z + (b.z - a.z) * t) < dalValue) {
                return false;
            }
        }
        return true;
    }

    /** Is the noise at least flatValue on a ring of this radius around the peak (so the tour lies in the polder)? */
    public static boolean vlakRond(long seed, Holder<NormalNoise.NoiseParameters> params, Peak peak, int radius, double flatValue) {
        if (radius <= 0) {
            return true;
        }
        NormalNoise noise = noise(seed, params);
        for (int k = 0; k < 16; k++) {
            double a = Math.PI * 2 * k / 16;
            if (noise.getValue(peak.x + Math.cos(a) * radius, 0, peak.z + Math.sin(a) * radius) < flatValue) {
                return false;
            }
        }
        return true;
    }

    // =================================================================================================================
    // the best spot for the tour near the peak (elftocht-integratie): where the 256 x 256 square lies on dead-flat polder
    // =================================================================================================================

    /**
     * What dead-flat polder ground is, from the seed alone (the Guhpolder's router: its plain is flat where the polder
     * noise is at least {@code flatValue}, the deep sea's noise at most {@code zeeMax} and the Knuffeldal's at most
     * {@code knuffelMax}; all raw noises, xz_scale 1). {@code minVlak}: how many of the {@link #GRID} x {@link #GRID}
     * points over the square must be flat for a tour (0: no spot search, the tour goes on the peak itself).
     */
    public record Vlak(double flatValue, java.util.Optional<Holder<NormalNoise.NoiseParameters>> zee, double zeeMax,
                       java.util.Optional<Holder<NormalNoise.NoiseParameters>> knuffel, double knuffelMax, int minVlak) {
        public static final com.mojang.serialization.Codec<Vlak> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                com.mojang.serialization.Codec.DOUBLE.fieldOf("flat_value").forGetter(Vlak::flatValue),
                NormalNoise.NoiseParameters.CODEC.optionalFieldOf("sea_noise").forGetter(Vlak::zee),
                com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("sea_max", 0.28).forGetter(Vlak::zeeMax),
                NormalNoise.NoiseParameters.CODEC.optionalFieldOf("knuffel_noise").forGetter(Vlak::knuffel),
                com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("knuffel_max", 0.40).forGetter(Vlak::knuffelMax),
                com.mojang.serialization.Codec.intRange(0, GRID * GRID).optionalFieldOf("min_flat", 0).forGetter(Vlak::minVlak)
        ).apply(i, Vlak::new));
        /** No spot search: the tour on the peak (the old behaviour). */
        public static final Vlak GEEN = new Vlak(0, java.util.Optional.empty(), 1, java.util.Optional.empty(), 1, 0);
    }

    /** The sample grid over the tour's square: GRID x GRID points, {@link #SAMPLE} blocks apart (+-120: the corners too). */
    public static final int GRID = 7, SAMPLE = 40;
    /** The spot search: candidates every {@link #ZOEK_STAP} blocks up to {@link #ZOEK} blocks from the peak (inside its cell). */
    public static final int ZOEK = 96, ZOEK_STAP = 16;

    /** The tour's middle: its x/z and how many grid points of its square are dead-flat polder. */
    public record Spot(int x, int z, int vlak) {
    }

    private record SpotKey(long seed, Identifier id, int size, int cx, int cz, Vlak vlak) {
    }

    private static final Map<SpotKey, Spot> SPOTS = new ConcurrentHashMap<>();

    /**
     * The best spot for the tour in cell (cx, cz): near the cell's peak (within {@link #ZOEK} blocks, inside the cell), where
     * most of the square is dead-flat polder (ties: the one nearest the peak). Without a spot search (minVlak 0): the peak.
     */
    public static Spot spot(long seed, Holder<NormalNoise.NoiseParameters> params, Vlak v, int size, int cx, int cz) {
        Peak peak = peak(seed, params, size, cx, cz);
        if (v.minVlak() <= 0) {
            return new Spot(peak.x(), peak.z(), GRID * GRID);
        }
        SpotKey key = new SpotKey(seed, params.unwrapKey().orElseThrow().identifier(), size, cx, cz, v);
        Spot known = SPOTS.get(key);
        if (known != null) {
            return known;
        }
        NormalNoise polder = noise(seed, params);
        NormalNoise zee = v.zee().map(h -> noise(seed, h)).orElse(null);
        NormalNoise knuffel = v.knuffel().map(h -> noise(seed, h)).orElse(null);
        // a lattice every 8 blocks covering every candidate's square (candidates and samples are multiples of 8 apart)
        int reach = ZOEK + SAMPLE * (GRID / 2);
        int n = reach / 8 * 2 + 1;
        boolean[][] vlak = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double x = peak.x() - reach + i * 8, z = peak.z() - reach + j * 8;
                vlak[i][j] = polder.getValue(x, 0, z) >= v.flatValue()
                        && (zee == null || zee.getValue(x, 0, z) <= v.zeeMax())
                        && (knuffel == null || knuffel.getValue(x, 0, z) <= v.knuffelMax());
            }
        }
        int x0 = cx * size * 16, z0 = cz * size * 16;
        Spot best = null;
        long bestD = Long.MAX_VALUE;
        for (int dx = -ZOEK; dx <= ZOEK; dx += ZOEK_STAP) {
            for (int dz = -ZOEK; dz <= ZOEK; dz += ZOEK_STAP) {
                int x = peak.x() + dx, z = peak.z() + dz;
                if (x < x0 || z < z0 || x >= x0 + size * 16 || z >= z0 + size * 16) {
                    continue;
                }
                int count = 0;
                for (int a = -(GRID / 2); a <= GRID / 2; a++) {
                    for (int b = -(GRID / 2); b <= GRID / 2; b++) {
                        if (vlak[(reach + dx + a * SAMPLE) / 8][(reach + dz + b * SAMPLE) / 8]) {
                            count++;
                        }
                    }
                }
                long d = (long) dx * dx + (long) dz * dz;
                if (best == null || count > best.vlak() || count == best.vlak() && d < bestD) {
                    best = new Spot(x, z, count);
                    bestD = d;
                }
            }
        }
        if (SPOTS.size() > 20000) {
            SPOTS.clear();
        }
        SPOTS.put(key, best);
        return best;
    }

    /** (Tests) forget the cached peaks and noises. */
    public static void vergeet() {
        PEAKS.clear();
        SPOTS.clear();
        NOISES.clear();
    }

    private ElftochtPiek() {
    }
}
