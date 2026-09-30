package nl.juiced.guhs.feature.knuffeldal;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import nl.juiced.guhs.feature.onderwater.GuhbubbelStructure;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * The Knuffeldal town's own structure type (2.8): exactly one town in the middle of every Knuffeldal.
 * <p>
 * The Knuffeldal comes from one noise ({@code knuffel_noise}, also in the Guhmension's noise router: the higher, the
 * more Knuffeldal; the biome starts around {@code dal_value}, the terrain is flat from a bit higher up). Like the
 * Guhbubbel ({@link GuhbubbelStructure}, whose peak search this reuses): the world is cut into cells of cell_chunks x
 * cell_chunks chunks (the structure set's spacing), in its cell the noise is sampled every 8 blocks and the highest point
 * is the cell's peak; the town goes on that peak only if it is high enough ({@code min_value}), no higher peak within
 * {@code neighbourhood} cells lies in the same dal (the noise stays at least dal_value along the line between them), and
 * the noise is at least {@code flat_value} on a ring of {@code flat_radius} around it (so the whole plein and its
 * houses stand on the flat middle of the dal). The plein's centre jigsaw ({@code start_jigsaw_name}, at ground level)
 * lands on the top block of the ground at the peak; the other pieces (the four building slots, the grijpmachine, the
 * four corners with the houses) hang on it with jigsaws ({@code size} deep).
 * <p>
 * The start chunk is at most cell_chunks - 1 chunks from the peak and the town reaches ~56 blocks from its middle,
 * so every piece stays within the 8 chunks structure references reach.
 */
public class KnuffeldalStadjeStructure extends Structure implements BouwRuimte.Ruimte {
    public static final MapCodec<KnuffeldalStadjeStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            ResourceLocation.CODEC.fieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
            ResourceKey.codec(Registries.NOISE).fieldOf("knuffel_noise").forGetter(s -> s.knuffelNoise),
            Codec.intRange(1, 5).fieldOf("cell_chunks").forGetter(s -> s.cellChunks),
            Codec.intRange(1, 12).fieldOf("neighbourhood").forGetter(s -> s.neighbourhood),
            Codec.DOUBLE.fieldOf("min_value").forGetter(s -> s.minValue),
            Codec.DOUBLE.fieldOf("dal_value").forGetter(s -> s.dalValue),
            Codec.DOUBLE.fieldOf("flat_value").forGetter(s -> s.flatValue),
            Codec.intRange(0, 128).fieldOf("flat_radius").forGetter(s -> s.flatRadius),
            Codec.intRange(1, 7).fieldOf("size").forGetter(s -> s.size),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistance),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, KnuffeldalStadjeStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final ResourceLocation startJigsawName;
    private final ResourceKey<NormalNoise.NoiseParameters> knuffelNoise;
    private final int cellChunks;
    private final int neighbourhood;
    private final double minValue;
    private final double dalValue;
    private final double flatValue;
    private final int flatRadius;
    private final int size;
    private final int maxDistance;
    /** Room kept around the town (see BouwRuimte): nothing else grows into it. */
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public KnuffeldalStadjeStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, ResourceLocation startJigsawName,
                                     ResourceKey<NormalNoise.NoiseParameters> knuffelNoise, int cellChunks, int neighbourhood, double minValue,
                                     double dalValue, double flatValue, int flatRadius, int size, int maxDistance, int keepClear,
                                     Optional<Integer> voorrang) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.knuffelNoise = knuffelNoise;
        this.cellChunks = cellChunks;
        this.neighbourhood = neighbourhood;
        this.minValue = minValue;
        this.dalValue = dalValue;
        this.flatValue = flatValue;
        this.flatRadius = flatRadius;
        this.size = size;
        this.maxDistance = maxDistance;
        this.keepClear = keepClear;
        this.voorrang = voorrang;
    }

    /** Is the town of cell (cx, cz) here: the cell's peak is high enough, the highest of its dal, with a flat ring around? */
    public boolean plek(long seed, NormalNoise noise, int cx, int cz) {
        GuhbubbelStructure.Peak peak = GuhbubbelStructure.peak(seed, noise, cellChunks, cx, cz);
        if (peak.value() < minValue || !GuhbubbelStructure.highestOfItsSea(seed, noise, cellChunks, neighbourhood, dalValue, cx, cz)) {
            return false;
        }
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            if (noise.getValue(peak.x() + Math.cos(a) * flatRadius, 0, peak.z() + Math.sin(a) * flatRadius) < flatValue) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        NormalNoise noise = context.randomState().getOrCreateNoise(knuffelNoise);
        int cx = Math.floorDiv(chunk.x, cellChunks), cz = Math.floorDiv(chunk.z, cellChunks);
        if (!plek(context.seed(), noise, cx, cz)) {
            return Optional.empty();
        }
        GuhbubbelStructure.Peak peak = GuhbubbelStructure.peak(context.seed(), noise, cellChunks, cx, cz);
        int surface = context.chunkGenerator().getBaseHeight(peak.x(), peak.z(), Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
                context.randomState());
        // the plein's ground (template y 4, where the anchor is) on the top block of the ground: vanilla moves an unprojected
        // start piece down by its ground level delta (2.10: the real ground, guhs:grond_single_pool_element, so the land meets
        // the plein instead of sinking into a moat), so the anchor is asked that much higher minus one
        return BouwRuimte.claim(context, this, JigsawPlacement.addPieces(context, startPool, Optional.of(startJigsawName), size,
                new BlockPos(peak.x(), nl.juiced.guhs.world.grond.Grond.startY(startPool, surface), peak.z()), false, Optional.empty(), maxDistance, PoolAliasLookup.EMPTY, DimensionPadding.ZERO,
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

    public int cellChunks() {
        return cellChunks;
    }

    public ResourceKey<NormalNoise.NoiseParameters> knuffelNoise() {
        return knuffelNoise;
    }

    public int neighbourhood() {
        return neighbourhood;
    }

    public double minValue() {
        return minValue;
    }

    public double dalValue() {
        return dalValue;
    }

    @Override
    public StructureType<?> type() {
        return KnuffeldalFeature.STADJE_TYPE.get();
    }
}
