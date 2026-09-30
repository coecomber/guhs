package nl.juiced.guhs.feature.barbecuether;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import nl.juiced.guhs.Guhs;

/**
 * The barbecue pit (barbecueput): one structure for two dimensions, like vanilla's ruined portals.
 * <ul>
 *   <li>In the Guhmensie (open sky) it goes on the surface, only where the ground is flat enough.</li>
 *   <li>In the Barbecuether (a ceiling) the column in the middle is searched from high to low for a cave floor with
 *       enough room above it (never in the frying-sauce sea); the four corners must have floor at about the same height.</li>
 * </ul>
 * The template is placed with its centre jigsaw (start_jigsaw_name) on that spot, one layer into the ground.
 */
public class BarbecuePutStructure extends Structure implements nl.juiced.guhs.world.BouwRuimte.Ruimte {
    public static final MapCodec<BarbecuePutStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            ResourceLocation.CODEC.fieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
            Codec.INT.fieldOf("check_radius").forGetter(s -> s.checkRadius),
            Codec.INT.fieldOf("max_height_difference").forGetter(s -> s.maxHeightDifference),
            Codec.INT.optionalFieldOf("headroom", 10).forGetter(s -> s.headroom),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, BarbecuePutStructure::new));

    /** Biomes that have a ceiling over them (the Barbecuether's). */
    public static final TagKey<Biome> CAVE_BIOMES = TagKey.create(Registries.BIOME, Guhs.id("is_barbecuether"));

    private final Holder<StructureTemplatePool> startPool;
    private final ResourceLocation startJigsawName;
    private final int checkRadius;
    private final int maxHeightDifference;
    private final int headroom;
    /** Room kept around the build (see BouwRuimte). */
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public BarbecuePutStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, ResourceLocation startJigsawName,
                                int checkRadius, int maxHeightDifference, int headroom, int keepClear, Optional<Integer> voorrang) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.checkRadius = checkRadius;
        this.maxHeightDifference = maxHeightDifference;
        this.headroom = headroom;
        this.keepClear = keepClear;
        this.voorrang = voorrang;
    }

    @Override
    public int keepClear() {
        return keepClear;
    }

    @Override
    public int voorrang() {
        return voorrang.orElse(keepClear);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        var biome = context.biomeSource().getNoiseBiome(x >> 2, 64 >> 2, z >> 2, context.randomState().sampler());
        boolean cave = biome.is(CAVE_BIOMES);
        int y;
        if (cave) {
            y = caveFloor(context, x, z);
            if (y == Integer.MIN_VALUE) {
                return Optional.empty();
            }
            for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
                int other = floorNear(context, x + c[0] * checkRadius / 2, z + c[1] * checkRadius / 2, y);
                if (other == Integer.MIN_VALUE) {
                    return Optional.empty();
                }
            }
        } else {
            int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    int h = context.chunkGenerator().getBaseHeight(x + i * checkRadius / 2, z + j * checkRadius / 2,
                            Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                    low = Math.min(low, h);
                    high = Math.max(high, h);
                    if (high - low > maxHeightDifference) {
                        return Optional.empty();
                    }
                }
            }
            y = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            // (no pits on water or kaassaus: the surface block must be solid)
            NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
            BlockState ground = column.getBlock(y - 1);
            if (!ground.getFluidState().isEmpty()) {
                return Optional.empty();
            }
        }
        // the centre jigsaw lands on (x, y, z); without heightmap projection the piece sinks one layer, so the template's
        // ground layer (the jigsaw's layer) ends up in the top block of the floor
        // (max depth 1, not 0: with 0 vanilla's jigsaw placement never adds even the start piece)
        return nl.juiced.guhs.world.BouwRuimte.claim(context, this, JigsawPlacement.addPieces(context, startPool, Optional.of(startJigsawName), 1, new BlockPos(x, y, z), false,
                Optional.empty(), 64, PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING));
    }

    /** The highest cave floor (first free y above solid ground) in this column with room above and no fluid, or MIN_VALUE. */
    private int caveFloor(GenerationContext context, int x, int z) {
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        int seaLevel = context.chunkGenerator().getSeaLevel();
        int top = context.heightAccessor().getMaxBuildHeight() - 12;
        for (int y = top; y > Math.max(seaLevel + 1, context.heightAccessor().getMinBuildHeight() + 6); y--) {
            if (isFloor(column, y) && roomAbove(column, y, headroom)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** A floor within 3 blocks of y in this column (the corners of the pit). */
    private int floorNear(GenerationContext context, int x, int z, int y) {
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        for (int dy = 0; dy <= maxHeightDifference / 2; dy++) {
            for (int s : new int[]{y + dy, y - dy}) {
                if (isFloor(column, s) && roomAbove(column, s, 4)) {
                    return s;
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isFloor(NoiseColumn column, int y) {
        BlockState below = column.getBlock(y - 1);
        return column.getBlock(y).isAir() && !below.isAir() && below.getFluidState().isEmpty();
    }

    private static boolean roomAbove(NoiseColumn column, int y, int room) {
        for (int i = 0; i < room; i++) {
            if (!column.getBlock(y + i).isAir()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StructureType<?> type() {
        return BarbecuetherFeature.BARBECUEPUT_TYPE.get();
    }
}
