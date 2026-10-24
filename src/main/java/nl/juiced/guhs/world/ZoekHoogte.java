package nl.juiced.guhs.world;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import nl.juiced.guhs.feature.onderwol.GrottenFunctie;
import org.jspecify.annotations.Nullable;

/**
 * 1.7.0: a quick surface height for the flatness and biome checks (the guaranteed searches, {@link FlatJigsawStructure}).
 * {@code getBaseHeight} builds a NoiseChunk per column and fills every interpolator over the whole height, the caves'
 * noise too: the searches spent nearly all their time there. This probe evaluates the final density without the caves
 * ({@link GrottenFunctie} becomes solid) block by block, from the top down in steps of a cell, and remembers each column.
 * <p>
 * Not the exact height of the world: the terrain without caves and without the interpolation (a block or so off on a
 * slope), but always the same answer for the same seed and spot, which is all a filter needs. Only where the final
 * density has a {@link GrottenFunctie} (the Guhmension); other dimensions keep {@code getBaseHeight}.
 */
public final class ZoekHoogte {
    private static final int STAP = 4;
    private static final int MAX_KOLOMMEN = 1 << 20;
    private static final Map<RandomState, Kolommen> PER_STAAT = Collections.synchronizedMap(new WeakHashMap<>());

    private ZoekHoogte() {
    }

    private record Kolommen(@Nullable DensityFunction dichtheid, Map<Long, Integer> hoogtes) {
    }

    /** The height of the surface (the first air above the top solid block, as WORLD_SURFACE_WG) at x, z. */
    public static int hoogte(ChunkGenerator generator, RandomState random, LevelHeightAccessor height, int x, int z) {
        Kolommen k = generator instanceof NoiseBasedChunkGenerator ? PER_STAAT.computeIfAbsent(random, ZoekHoogte::kolommen) : null;
        if (k == null || k.dichtheid == null) {
            return generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
        }
        NoiseBasedChunkGenerator noise = (NoiseBasedChunkGenerator) generator;
        long key = (long) x << 32 | (z & 0xffffffffL);
        Integer bekend = k.hoogtes.get(key);
        if (bekend != null) {
            return bekend;
        }
        NoiseSettings settings = noise.generatorSettings().value().noiseSettings().clampToHeightAccessor(height);
        int h = zoek(k.dichtheid, x, z, settings.minY(), settings.minY() + settings.height() - 1);
        if (k.hoogtes.size() > MAX_KOLOMMEN) {
            k.hoogtes.clear();
        }
        k.hoogtes.put(key, h);
        return h;
    }

    /** Only a final density with caves (the Guhmension): elsewhere the exact height, so nothing changes for the other dimensions. */
    private static Kolommen kolommen(RandomState random) {
        boolean[] grotten = {false};
        DensityFunction zonder = random.router().finalDensity().mapAll(f -> {
            if (f instanceof GrottenFunctie) {
                grotten[0] = true;
                return DensityFunctions.constant(1.0);
            }
            return f;
        });
        return new Kolommen(grotten[0] ? zonder : null, new ConcurrentHashMap<>());
    }

    private static int zoek(DensityFunction dichtheid, int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y -= STAP) {
            if (vast(dichtheid, x, y, z)) {
                // (the cell above was air: the top block lies in y .. y + STAP - 1)
                for (int t = Math.min(y + STAP - 1, maxY); t > y; t--) {
                    if (vast(dichtheid, x, t, z)) {
                        return t + 1;
                    }
                }
                return y + 1;
            }
        }
        return minY;
    }

    private static boolean vast(DensityFunction dichtheid, int x, int y, int z) {
        return dichtheid.compute(new DensityFunction.SinglePointContext(x, y, z)) > 0.0;
    }
}
