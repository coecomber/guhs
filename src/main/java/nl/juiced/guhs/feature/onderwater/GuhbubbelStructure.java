package nl.juiced.guhs.feature.onderwater;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The Guhbubbel's own structure type: exactly one bubble in the middle of every Diepe Guhzee.
 * <p>
 * The deep sea comes from one noise (sea_noise, also in the Guhmension's noise router: the higher, the deeper; where it
 * is at least sea_value there is water). The world is cut into cells of cell_chunks x cell_chunks chunks (the same as
 * the structure set's random_spread spacing, so every cell has one start chunk). In its cell the noise is sampled every
 * 8 blocks and the highest point is the cell's peak; the bubble goes on that peak only if it is high enough (the deep
 * middle of a sea, min_value) and no higher peak within `neighbourhood` cells lies in the same sea (the noise stays at
 * least sea_value all the way along the straight line between them). So a sea gets one bubble, on its deepest spot,
 * and two seas side by side each get their own. Finally the sea floor is checked (the ocean floor heightmap): in the
 * middle at least centre_depth under the water, and on two rings around it (check_radius and 3/4 of it) at least
 * min_depth, so the whole template is under water. The template is placed with its centre jigsaw (start_jigsaw_name,
 * which marks the water surface) one block above water_level: the water surface (its top water block) lands on
 * water_level - 1, level with the sea's.
 * <p>
 * The start chunk is at most cell_chunks - 1 chunks from the peak, so the piece (at most 4 chunks around the peak)
 * stays within the 8 chunks structure references reach.
 */
public class GuhbubbelStructure extends Structure implements nl.juiced.guhs.world.BouwRuimte.Ruimte {
    public static final MapCodec<GuhbubbelStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Identifier.CODEC.fieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
            ResourceKey.codec(Registries.NOISE).fieldOf("sea_noise").forGetter(s -> s.seaNoise),
            Codec.intRange(1, 5).fieldOf("cell_chunks").forGetter(s -> s.cellChunks),
            Codec.intRange(1, 10).fieldOf("neighbourhood").forGetter(s -> s.neighbourhood),
            Codec.DOUBLE.fieldOf("min_value").forGetter(s -> s.minValue),
            Codec.DOUBLE.fieldOf("sea_value").forGetter(s -> s.seaValue),
            Codec.INT.fieldOf("water_level").forGetter(s -> s.waterLevel),
            Codec.INT.fieldOf("centre_depth").forGetter(s -> s.centreDepth),
            Codec.INT.fieldOf("min_depth").forGetter(s -> s.minDepth),
            Codec.INT.fieldOf("check_radius").forGetter(s -> s.checkRadius),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, GuhbubbelStructure::new));

    /** Samples per cell side are taken every STEP blocks. */
    public static final int STEP = 8;

    private final Holder<StructureTemplatePool> startPool;
    private final Identifier startJigsawName;
    private final ResourceKey<NormalNoise.NoiseParameters> seaNoise;
    private final int cellChunks;
    private final int neighbourhood;
    private final double minValue;
    private final double seaValue;
    private final int waterLevel;
    private final int centreDepth;
    private final int minDepth;
    private final int checkRadius;
    /** Room kept around the dome (see BouwRuimte): nothing else grows into it. */
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public GuhbubbelStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, Identifier startJigsawName,
                              ResourceKey<NormalNoise.NoiseParameters> seaNoise, int cellChunks, int neighbourhood, double minValue,
                              double seaValue, int waterLevel, int centreDepth, int minDepth, int checkRadius, int keepClear,
                              Optional<Integer> voorrang) {
        super(settings);
        this.keepClear = keepClear;
        this.voorrang = voorrang;
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.seaNoise = seaNoise;
        this.cellChunks = cellChunks;
        this.neighbourhood = neighbourhood;
        this.minValue = minValue;
        this.seaValue = seaValue;
        this.waterLevel = waterLevel;
        this.centreDepth = centreDepth;
        this.minDepth = minDepth;
        this.checkRadius = checkRadius;
    }

    /** The highest sampled point of a cell. */
    public record Peak(int x, int z, double value) {
    }

    private record CellKey(long seed, NormalNoise noise, int size, int cx, int cz) {
    }

    /** Recent cell peaks (worldgen asks from several threads, and every cell is asked about by its neighbours). */
    private static final Map<CellKey, Peak> PEAKS = new ConcurrentHashMap<>();

    /** The highest point of the sea noise in cell (cx, cz) of size x size chunks, sampled every STEP blocks. */
    public static Peak peak(long seed, NormalNoise noise, int size, int cx, int cz) {
        CellKey key = new CellKey(seed, noise, size, cx, cz);
        Peak known = PEAKS.get(key);
        if (known != null) {
            return known;
        }
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
        if (PEAKS.size() > 50000) {
            PEAKS.clear();
        }
        PEAKS.put(key, best);
        return best;
    }

    /**
     * Whether the peak of cell (cx, cz) is the highest of its sea: no cell within `around` cells has a higher peak (ties:
     * the lowest cell wins) that is in the same sea, i.e. with the noise at least seaValue all along the line between.
     */
    public static boolean highestOfItsSea(long seed, NormalNoise noise, int size, int around, double seaValue, int cx, int cz) {
        Peak own = peak(seed, noise, size, cx, cz);
        for (int dx = -around; dx <= around; dx++) {
            for (int dz = -around; dz <= around; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                Peak other = peak(seed, noise, size, cx + dx, cz + dz);
                boolean higher = other.value > own.value || other.value == own.value && (dx < 0 || dx == 0 && dz < 0);
                if (higher && sameSea(noise, own, other, seaValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Is the noise at least seaValue all along the straight line from a to b (sampled every STEP blocks)? */
    public static boolean sameSea(NormalNoise noise, Peak a, Peak b, double seaValue) {
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(b.x - a.x, b.z - a.z) / STEP));
        for (int k = 1; k < steps; k++) {
            double t = (double) k / steps;
            if (noise.getValue(a.x + (b.x - a.x) * t, 0, a.z + (b.z - a.z) * t) < seaValue) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        NormalNoise noise = context.randomState().getOrCreateNoise(seaNoise);
        int cx = Math.floorDiv(chunk.x(), cellChunks), cz = Math.floorDiv(chunk.z(), cellChunks);
        Peak peak = peak(context.seed(), noise, cellChunks, cx, cz);
        if (peak.value < minValue || !highestOfItsSea(context.seed(), noise, cellChunks, neighbourhood, seaValue, cx, cz)) {
            return Optional.empty();
        }
        // deep water all over the template: the middle and two rings around it
        if (!deepEnough(context, peak.x, peak.z, centreDepth)) {
            return Optional.empty();
        }
        for (int ring = 3; ring <= 4; ring++) {
            int r = checkRadius * ring / 4;
            for (int k = 0; k < 8; k++) {
                double a = Math.PI / 4 * k + (ring == 4 ? Math.PI / 8 : 0);
                if (!deepEnough(context, peak.x + (int) Math.round(Math.cos(a) * r), peak.z + (int) Math.round(Math.sin(a) * r), minDepth)) {
                    return Optional.empty();
                }
            }
        }
        // (max depth 1, not 0: with 0 vanilla's jigsaw placement never adds even the start piece)
        return nl.juiced.guhs.world.BouwRuimte.claim(context, this, JigsawPlacement.addPieces(context, startPool, Optional.of(startJigsawName), 1,
                new BlockPos(peak.x, waterLevel + 1, peak.z), false, Optional.empty(), 80, PoolAliasLookup.EMPTY, DimensionPadding.ZERO,
                LiquidSettings.IGNORE_WATERLOGGING));
    }

    @Override
    public int keepClear() {
        return keepClear;
    }

    @Override
    public int voorrang() {
        return voorrang.orElse(keepClear);
    }

    /** Is the sea floor here (the noise terrain; the ocean floor heightmap is the first free block above it) `depth` under the water level? */
    private boolean deepEnough(GenerationContext context, int x, int z, int depth) {
        int floor = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        return floor <= waterLevel - depth;
    }

    public int cellChunks() {
        return cellChunks;
    }

    public int waterLevel() {
        return waterLevel;
    }

    public ResourceKey<NormalNoise.NoiseParameters> seaNoise() {
        return seaNoise;
    }

    public int neighbourhood() {
        return neighbourhood;
    }

    public double minValue() {
        return minValue;
    }

    public double seaValue() {
        return seaValue;
    }

    @Override
    public StructureType<?> type() {
        return OnderwaterFeature.GUHBUBBEL_TYPE.get();
    }
}
