package nl.juiced.guhs.feature.verhaal.wereld;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.juiced.guhs.feature.elftocht.ElftochtPiek;
import nl.juiced.guhs.feature.onderwater.GuhbubbelStructure;

/**
 * 3.0: WHERE in its region a regio structure goes ({@code guhs:regio_piek} placement + {@code guhs:regio_jigsaw} structure use
 * the very same spot, so /locate, the Superkompas and the guh compasses find it). One spot per cell of {@code cell} chunks,
 * only when that cell holds the peak of its region ({@link Regio#isPiek}):
 * <ul>
 *   <li>{@code piek}: on the region's peak (Nomguh on the top of its tundra, the 626 capsule on the hill of its island);</li>
 *   <li>{@code kust}: from the peak in a direction (a hash of the cell + {@code hoek} degrees) until the value drops under
 *       {@code kust_value}: the last spot still above it (the beach of the island: Lilo's stilt house, the surf beach)
 *       (merge 3.0: the spot stays in its own cell and at least {@link #KUST_MIN} from the peak: when the walk leaves the cell or
 *       stops too close, directions up to 67.5 degrees to either side are tried; a tiny island takes its farthest beach);</li>
 *   <li>{@code zee}: {@code afstand} blocks from the peak of a deep sea in a hashed direction (the next of 8 directions when one
 *       doesn't fit, or leaves its cell): deep water ({@code diep_value}) there and on a ring of {@code ring} blocks, outside the {@code vermijd}
 *       region (the Guhwai'i islands) and at least {@code bubbel_afstand} blocks from every Guhbubbel peak (the kloon-eiland).</li>
 * </ul>
 */
public record RegioPlek(Regio regio, int cell, Soort soort, double hoek, int afstand, double kustValue, int ring, double diepValue,
                        Optional<Regio> vermijd, int bubbelAfstand) {
    public enum Soort implements StringRepresentable {
        PIEK("piek"), KUST("kust"), ZEE("zee");

        public static final Codec<Soort> CODEC = StringRepresentable.fromEnum(Soort::values);
        private final String naam;

        Soort(String naam) {
            this.naam = naam;
        }

        @Override
        public String getSerializedName() {
            return naam;
        }
    }

    public static final Codec<RegioPlek> CODEC = RecordCodecBuilder.create(i -> i.group(
            Regio.CODEC.fieldOf("regio").forGetter(RegioPlek::regio),
            Codec.intRange(2, 64).fieldOf("cell").forGetter(RegioPlek::cell),
            Soort.CODEC.fieldOf("soort").forGetter(RegioPlek::soort),
            Codec.DOUBLE.optionalFieldOf("hoek", 0.0).forGetter(RegioPlek::hoek),
            Codec.INT.optionalFieldOf("afstand", 0).forGetter(RegioPlek::afstand),
            Codec.DOUBLE.optionalFieldOf("kust_value", 0.0).forGetter(RegioPlek::kustValue),
            Codec.INT.optionalFieldOf("ring", 0).forGetter(RegioPlek::ring),
            Codec.DOUBLE.optionalFieldOf("diep_value", 0.47).forGetter(RegioPlek::diepValue),
            Regio.CODEC.optionalFieldOf("vermijd").forGetter(RegioPlek::vermijd),
            Codec.INT.optionalFieldOf("bubbel_afstand", 120).forGetter(RegioPlek::bubbelAfstand)
    ).apply(i, RegioPlek::new));

    /**
     * The Guhbubbel's placement (tools/features/diepzee.py): cells of 4 chunks, 8 cells around, peak >= 0.51, one sea >= 0.427
     * (1.0.0: 0.44 / 0.357 + diepzee.ZEE_KRIMP 0.07, the smaller seas).
     */
    public static final int BUBBEL_CEL = 4, BUBBEL_BUURT = 8;
    public static final double BUBBEL_MIN = 0.51, BUBBEL_ZEE = 0.427;
    /** How far the kust walk goes at most (blocks), in steps of 4. */
    public static final int KUST_MAX = 400;
    /** (merge 3.0) A kust spot at least this far from the peak when it can (the peak holds the capsule). */
    public static final int KUST_MIN = 56;
    /**
     * (merge 3.0) The directions tried for a kust spot (degrees from the cell's own): at most 67.5 to either side, so two kust
     * structures of one island whose hoek differs by 180 (Lilo's stilt house, the surf beach) never take the same beach.
     */
    private static final double[] RICHTINGEN = {0, 22.5, -22.5, 45, -45, 67.5, -67.5};

    /** A spot: x/z, and the cell's peak it belongs to. */
    public record Plek(int x, int z, Regio.Piek piek) {
    }

    private record Sleutel(long seed, RegioPlek plek, int cx, int cz) {
    }

    private static final Map<Sleutel, Optional<Plek>> PLEKKEN = new ConcurrentHashMap<>();

    /** The spot of cell (cx, cz), or empty when this cell has none (not the peak of its region, or nothing fits). */
    public Optional<Plek> plek(long seed, int cx, int cz) {
        Sleutel key = new Sleutel(seed, this, cx, cz);
        Optional<Plek> known = PLEKKEN.get(key);
        if (known != null) {
            return known;
        }
        Optional<Plek> p = zoek(seed, cx, cz);
        if (PLEKKEN.size() > 20000) {
            PLEKKEN.clear();
        }
        PLEKKEN.put(key, p);
        return p;
    }

    /** The spot for the cell holding chunk (chunkX, chunkZ). */
    public Optional<Plek> plekVoorChunk(long seed, int chunkX, int chunkZ) {
        return plek(seed, Math.floorDiv(chunkX, cell), Math.floorDiv(chunkZ, cell));
    }

    /** The direction (radians) of cell (cx, cz): a hash of the seed and the cell, plus hoek. */
    public double richting(long seed, int cx, int cz) {
        long h = seed * 0x9E3779B97F4A7C15L + cx * 0xC2B2AE3D27D4EB4FL + cz * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        double a = (h >>> 11) * 0x1.0p-53 * Math.PI * 2;
        return a + Math.toRadians(hoek);
    }

    private Optional<Plek> zoek(long seed, int cx, int cz) {
        if (!regio.isPiek(seed, cell, cx, cz)) {
            return Optional.empty();
        }
        Regio.Piek piek = regio.piek(seed, cell, cx, cz);
        double a = richting(seed, cx, cz);
        switch (soort) {
            case PIEK:
                return Optional.of(new Plek(piek.x(), piek.z(), piek));
            case KUST: {
                // (merge 3.0) the spot must lie in this cell (the placement only starts a structure in a chunk of its own cell)
                // and away from the peak (the capsule there); try the neighbouring directions, else the farthest one that fits
                Plek reserve = null;
                double reserveD = -1;
                for (double k : RICHTINGEN) {
                    Plek p = kustLoop(seed, piek, a + Math.toRadians(k));
                    if (p == null || !inCel(p.x(), p.z(), cx, cz)) {
                        continue;
                    }
                    double d = Math.hypot(p.x() - piek.x(), p.z() - piek.z());
                    if (d >= KUST_MIN) {
                        return Optional.of(p);
                    }
                    if (d > reserveD) {
                        reserve = p;
                        reserveD = d;
                    }
                }
                return Optional.ofNullable(reserve);
            }
            case ZEE:
            default: {
                for (int k = 0; k < 8; k++) {
                    double b = a + Math.PI / 4 * k;
                    int x = piek.x() + (int) Math.round(Math.cos(b) * afstand), z = piek.z() + (int) Math.round(Math.sin(b) * afstand);
                    if (inCel(x, z, cx, cz) && zeePast(seed, x, z)) {   // (merge 3.0: in this cell, see KUST)
                        return Optional.of(new Plek(x, z, piek));
                    }
                }
                return Optional.empty();
            }
        }
    }

    /** (merge 3.0) The kust walk in direction b: the last spot still above kust_value (null: none within KUST_MAX). */
    @Nullable
    private Plek kustLoop(long seed, Regio.Piek piek, double b) {
        double dx = Math.cos(b), dz = Math.sin(b);
        int lx = piek.x(), lz = piek.z();
        for (int d = 4; d <= KUST_MAX; d += 4) {
            int x = piek.x() + (int) Math.round(dx * d), z = piek.z() + (int) Math.round(dz * d);
            if (regio.waarde(seed, x, z) < kustValue) {
                return new Plek(lx, lz, piek);
            }
            lx = x;
            lz = z;
        }
        return null;
    }

    /** (merge 3.0) Whether block (x, z) lies in cell (cx, cz). */
    private boolean inCel(int x, int z, int cx, int cz) {
        return Math.floorDiv(x >> 4, cell) == cx && Math.floorDiv(z >> 4, cell) == cz;
    }

    /** Deep water at (x, z) and on the ring, outside the islands, far enough from every Guhbubbel. */
    public boolean zeePast(long seed, int x, int z) {
        if (!diep(seed, x, z)) {
            return false;
        }
        for (int k = 0; k < 8 && ring > 0; k++) {
            double b = Math.PI / 4 * k;
            if (!diep(seed, x + Math.cos(b) * ring, z + Math.sin(b) * ring)) {
                return false;
            }
        }
        NormalNoise zee = ElftochtPiek.noise(seed, regio.noise());
        int bx = Math.floorDiv(x, BUBBEL_CEL * 16), bz = Math.floorDiv(z, BUBBEL_CEL * 16);
        int r = bubbelAfstand / (BUBBEL_CEL * 16) + 1;
        for (int i = -r; i <= r; i++) {
            for (int j = -r; j <= r; j++) {
                GuhbubbelStructure.Peak p = GuhbubbelStructure.peak(seed, zee, BUBBEL_CEL, bx + i, bz + j);
                if (p.value() >= BUBBEL_MIN && Math.hypot(p.x() - x, p.z() - z) < bubbelAfstand
                        && GuhbubbelStructure.highestOfItsSea(seed, zee, BUBBEL_CEL, BUBBEL_BUURT, BUBBEL_ZEE, bx + i, bz + j)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean diep(long seed, double x, double z) {
        if (regio.ruw(seed, x, z) < diepValue) {
            return false;
        }
        return vermijd.isEmpty() || vermijd.get().waarde(seed, x, z) < vermijd.get().dalValue();
    }

    static void vergeet() {
        PLEKKEN.clear();
    }
}
