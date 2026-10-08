package nl.juiced.guhs.feature.bio.kompas;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * biomes3: looks for the nearest spot of a biome by asking the biome source (no chunk is loaded or made), in steps: it
 * walks square rings of columns around the start, {@code stap} blocks apart, and looks at a few heights per column
 * (the Guhmensie has biomes that only exist deep down). {@link #stap(int)} does a bounded amount of work, so the search
 * can run in slices or on another thread (the biome source and its sampler are what worldgen threads use too); a search
 * is used by one thread at a time.
 * <p>
 * After the first hit it finishes the rings in which a nearer one can still lie (a ring is a square, the distance is
 * round), so the answer is the nearest column of the grid.
 */
public final class BiomeZoeker {
    private final BiomeSource bron;
    private final Climate.Sampler sampler;
    private final Predicate<Holder<Biome>> doel;
    private final int vx, vz, stap, ringen;
    private final int[] hoogtes;
    private int ring, index, eind;
    private long besteAfstand = Long.MAX_VALUE;
    @Nullable
    private BlockPos beste;
    private boolean klaar;
    private long monsters;
    private int afgekeurd;
    /** Is a spot the biome source names really (going to be) that biome? See {@link Opgeslagen}. */
    private Predicate<BlockPos> echt = p -> true;

    /**
     * @param van     where the holder stands
     * @param straal  how far to look (blocks, a circle)
     * @param stap    the grid (blocks between two columns)
     * @param minY    the lowest and
     * @param maxY    the highest block height of the dimension
     */
    public BiomeZoeker(BiomeSource bron, Climate.Sampler sampler, Predicate<Holder<Biome>> doel, BlockPos van, int straal, int stap, int minY, int maxY) {
        this.bron = bron;
        this.sampler = sampler;
        this.doel = doel;
        this.vx = van.getX();
        this.vz = van.getZ();
        this.stap = Math.max(4, stap);
        this.ringen = Math.max(0, straal / this.stap);
        this.eind = ringen;
        // every 32 blocks of height, the ones nearest to the holder first
        int n = Math.max(1, (maxY - minY + 1) / 32);
        Integer[] ys = new Integer[n];
        for (int i = 0; i < n; i++) {
            ys[i] = minY + 16 + i * 32;
        }
        java.util.Arrays.sort(ys, java.util.Comparator.comparingInt(y -> Math.abs(y - van.getY())));
        hoogtes = new int[n];
        for (int i = 0; i < n; i++) {
            hoogtes[i] = ys[i];
        }
    }

    /** The search for a biome in a real dimension. A biome its biome source does not have at all: done at once, nothing found. */
    public static BiomeZoeker voor(ServerLevel level, ResourceKey<Biome> biome, BlockPos van, int straal, int stap) {
        BiomeSource bron = level.getChunkSource().getGenerator().getBiomeSource();
        BiomeZoeker z = new BiomeZoeker(bron, level.getChunkSource().randomState().sampler(), h -> h.is(biome), van, straal, stap, level.getMinY(),
                level.getMaxY()).metControle(Opgeslagen.controle(level, biome.identifier().toString()));
        if (bron.possibleBiomes().stream().noneMatch(h -> h.is(biome))) {
            z.klaar = true;
        }
        return z;
    }

    /** A spot that matches only counts when this says so (the chunk there was not saved earlier as another biome). */
    public BiomeZoeker metControle(Predicate<BlockPos> echt) {
        this.echt = echt;
        return this;
    }

    /** Looks at about {@code budget} more spots; true when the search is over. */
    public boolean stap(int budget) {
        long tot = monsters + Math.max(1, budget);
        while (!klaar && monsters < tot) {
            if (ring > eind) {
                klaar = true;
                break;
            }
            int rand = ring == 0 ? 1 : 8 * ring;
            if (index >= rand) {
                ring++;
                index = 0;
                continue;
            }
            int dx, dz;
            if (ring == 0) {
                dx = 0;
                dz = 0;
            } else {
                int zijde = index / (2 * ring), o = index % (2 * ring);
                switch (zijde) {
                    case 0 -> {
                        dx = -ring + o;
                        dz = -ring;
                    }
                    case 1 -> {
                        dx = ring;
                        dz = -ring + o;
                    }
                    case 2 -> {
                        dx = ring - o;
                        dz = ring;
                    }
                    default -> {
                        dx = -ring;
                        dz = ring - o;
                    }
                }
            }
            index++;
            long afstand = (long) dx * dx + (long) dz * dz;
            if (afstand > (long) ringen * ringen || afstand >= besteAfstand) {
                continue;   // outside the circle, or no nearer than what is found
            }
            int x = vx + dx * stap, z = vz + dz * stap;
            for (int y : hoogtes) {
                monsters++;
                if (doel.test(bron.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler))) {
                    BlockPos plek = new BlockPos(x, y, z);
                    if (!echt.test(plek)) {
                        afgekeurd++;   // an older chunk that is another biome: on to the next column
                        break;
                    }
                    if (beste == null) {
                        // a nearer one can only lie in the rings up to the circle through this ring's corner
                        eind = Math.min(ringen, (int) Math.ceil(ring * 1.4143));
                    }
                    beste = plek;
                    besteAfstand = afstand;
                    break;
                }
            }
        }
        if (ring > eind) {
            klaar = true;
        }
        return klaar;
    }

    /** Runs the whole search at once (dev commands, the self test, tests). */
    public BiomeZoeker helemaal() {
        while (!stap(1 << 16)) {
            // on
        }
        return this;
    }

    public boolean klaar() {
        return klaar;
    }

    /** The nearest spot found (so far), or null. */
    @Nullable
    public BlockPos gevonden() {
        return beste;
    }

    /** How many matching spots were skipped because their saved chunk is another biome. */
    public int afgekeurd() {
        return afgekeurd;
    }

    /** How many spots were looked at. */
    public long monsters() {
        return monsters;
    }
}
