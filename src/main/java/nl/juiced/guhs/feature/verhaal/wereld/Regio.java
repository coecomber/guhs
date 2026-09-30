package nl.juiced.guhs.feature.verhaal.wereld;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.juiced.guhs.feature.elftocht.ElftochtPiek;

/**
 * 3.0 (Guhverhalen): a region of the Guhmension made by one low noise of its own (the Sneeuwguhtoendra, the islands of
 * Guhwai'i, or the deep seas for the kloon-eiland), with masks against the other regions: the same numbers as the terms of
 * tools/features/verhaal_wereld.py, but from the seed alone (a structure placement only gets the seed). The noises are made
 * exactly like {@code RandomState} makes them ({@link ElftochtPiek#noise}).
 * <ul>
 *   <li>{@link #waarde}: the region noise at a spot, or {@link #GEEN} when a mask says "not here" (a sea, a Knuffeldal...);</li>
 *   <li>{@link #piek}: the highest point of a cell of {@code cell} x {@code cell} chunks;</li>
 *   <li>{@link #isPiek}: that peak is high enough ({@code min_value}) and the highest of its region (no neighbouring cell has a
 *       higher peak that lies in the same region: the value stays at least {@code dal_value} on the line between).</li>
 * </ul>
 */
public record Regio(Holder<NormalNoise.NoiseParameters> noise, List<Masker> maskers, double minValue, double dalValue, int buurt) {
    /**
     * A mask: this other noise must lie between min and max (else: not in the region). With {@code ring} (radii): the highest
     * value of the spot and of 8 points on each ring must stay under max (the router's shifted_noise samples: e.g. the
     * Guhwai'i islands keep away from the deep middle of a sea, where the Guhbubbel is).
     */
    public record Masker(Holder<NormalNoise.NoiseParameters> noise, double min, double max, List<Integer> ring) {
        public static final Codec<Masker> CODEC = RecordCodecBuilder.create(i -> i.group(
                NormalNoise.NoiseParameters.CODEC.fieldOf("noise").forGetter(Masker::noise),
                Codec.DOUBLE.optionalFieldOf("min", -10.0).forGetter(Masker::min),
                Codec.DOUBLE.optionalFieldOf("max", 10.0).forGetter(Masker::max),
                Codec.INT.listOf().optionalFieldOf("ring", List.of()).forGetter(Masker::ring)
        ).apply(i, Masker::new));

        boolean past(long seed, double x, double z) {
            NormalNoise n = ElftochtPiek.noise(seed, noise);
            double v = n.getValue(x, 0, z);
            if (v < min) {
                return false;
            }
            for (int r : ring) {
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI / 4 * k;
                    v = Math.max(v, n.getValue(x + Math.cos(a) * r, 0, z + Math.sin(a) * r));
                }
            }
            return v <= max;
        }
    }

    public static final Codec<Regio> CODEC = RecordCodecBuilder.create(i -> i.group(
            NormalNoise.NoiseParameters.CODEC.fieldOf("noise").forGetter(Regio::noise),
            Masker.CODEC.listOf().optionalFieldOf("masks", List.of()).forGetter(Regio::maskers),
            Codec.DOUBLE.fieldOf("min_value").forGetter(Regio::minValue),
            Codec.DOUBLE.fieldOf("dal_value").forGetter(Regio::dalValue),
            Codec.intRange(1, 6).optionalFieldOf("buurt", 1).forGetter(Regio::buurt)
    ).apply(i, Regio::new));

    /** "Not in this region" (a mask said no). */
    public static final double GEEN = -10;
    /** Sample distance of the coarse peak search. */
    public static final int STAP = 16;

    public record Piek(int x, int z, double waarde) {
    }

    private record CelSleutel(long seed, Regio regio, int cell, int cx, int cz) {
    }

    private static final Map<CelSleutel, Piek> PIEKEN = new ConcurrentHashMap<>();

    /** The region noise at (x, z), or {@link #GEEN} where a mask says no. */
    public double waarde(long seed, double x, double z) {
        for (Masker m : maskers) {
            if (!m.past(seed, x, z)) {
                return GEEN;
            }
        }
        return ElftochtPiek.noise(seed, noise).getValue(x, 0, z);
    }

    /** The raw region noise (no masks). */
    public double ruw(long seed, double x, double z) {
        return ElftochtPiek.noise(seed, noise).getValue(x, 0, z);
    }

    /** The highest point of the region in cell (cx, cz) of cell x cell chunks (masked spots never win). */
    public Piek piek(long seed, int cell, int cx, int cz) {
        CelSleutel key = new CelSleutel(seed, this, cell, cx, cz);
        Piek known = PIEKEN.get(key);
        if (known != null) {
            return known;
        }
        int x0 = cx * cell * 16, z0 = cz * cell * 16, n = cell * 16 / STAP;
        Piek best = null;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int x = x0 + STAP / 2 + i * STAP, z = z0 + STAP / 2 + j * STAP;
                double v = waarde(seed, x, z);
                if (best == null || v > best.waarde) {
                    best = new Piek(x, z, v);
                }
            }
        }
        Piek fijn = best;
        for (int dx = -STAP; dx <= STAP; dx += 4) {
            for (int dz = -STAP; dz <= STAP; dz += 4) {
                int x = best.x + dx, z = best.z + dz;
                if (x < x0 || z < z0 || x >= x0 + cell * 16 || z >= z0 + cell * 16) {
                    continue;
                }
                double v = waarde(seed, x, z);
                if (v > fijn.waarde) {
                    fijn = new Piek(x, z, v);
                }
            }
        }
        if (PIEKEN.size() > 20000) {
            PIEKEN.clear();
        }
        PIEKEN.put(key, fijn);
        return fijn;
    }

    /**
     * Is the peak of cell (cx, cz) the peak of its region: high enough, and no peak of a cell within {@code buurt} cells in the
     * same region beats it?
     */
    public boolean isPiek(long seed, int cell, int cx, int cz) {
        Piek eigen = piek(seed, cell, cx, cz);
        if (eigen.waarde < minValue) {
            return false;
        }
        for (int dx = -buurt; dx <= buurt; dx++) {
            for (int dz = -buurt; dz <= buurt; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                Piek ander = piek(seed, cell, cx + dx, cz + dz);
                boolean hoger = ander.waarde > eigen.waarde || ander.waarde == eigen.waarde && (dx < 0 || dx == 0 && dz < 0);
                if (hoger && zelfde(seed, eigen, ander)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Do two peaks lie in the same region (the value stays at least dal_value on the straight line between them)? */
    public boolean zelfde(long seed, Piek a, Piek b) {
        double d = Math.hypot(b.x - a.x, b.z - a.z);
        int n = Math.max(1, (int) (d / 12));
        for (int i = 1; i < n; i++) {
            double t = i / (double) n;
            if (waarde(seed, a.x + (b.x - a.x) * t, a.z + (b.z - a.z) * t) < dalValue) {
                return false;
            }
        }
        return true;
    }

    /** (Tests) forget the cached peaks. */
    public static void vergeet() {
        PIEKEN.clear();
        RegioPlek.vergeet();
    }
}
