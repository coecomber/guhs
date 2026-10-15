package nl.juiced.guhs.feature.park;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * Structure type {@code guhs:knabbelpark} (1.4.2): Het Knabbelpark, a jigsaw park that grows like a vanilla village.
 * <p>
 * The pieces (tools/features/park_bouw.py): the start piece is the fountain plaza (rigid, with the one Reisguh in the
 * middle); three short lanes ({@code terrain_matching}) lead from it to the three REQUIRED pieces (rigid, each at its own
 * height): the beeldentuin, the hertenkooi and the parkwachter's hut. From the plaza and from the far side of every
 * required piece, random park walks grow ({@code terrain_matching}: they follow the hills, with hedges, flowers, lanterns,
 * benches and litter bins of their own), and along the walks random plekjes (rigid) hang on side connectors: see
 * {@link ParkPlekjes} for the pool / connector API.
 * <p>
 * What this type adds to a plain jigsaw:
 * <ul>
 *   <li>only on dry, gentle land: the surface on a 5x5 grid ({@code check_radius}) differs at most
 *       {@code max_height_difference}, and no sample point is under water;</li>
 *   <li>the REQUIRED pieces ({@code verplicht}, template ids) are always there: the required lanes go first (higher
 *       selection and placement priority on the plaza's jigsaws) so nothing random can take their room, and when a layout
 *       still misses one, another layout is drawn (up to {@code pogingen}, from the chunk seed: the same every time); a
 *       chunk where no layout has them all gets no park;</li>
 *   <li>(park-plekjes b) on a complete layout, {@code plek.Uitkijktoren#schik}: the once-per-park plekjes stay once, and the
 *       uitkijktoren takes the place of the plekje on the highest spot (only plekjes are swapped, inside their footprint);</li>
 *   <li>the boxes of the walks are grown to the real height of the land under them (a terrain-matching piece keeps the
 *       box of its template at the height of its joint, but its blocks follow the hills): so protection
 *       ({@code feature.wereld.Bescherming}), {@link BouwRuimte} and the "no plants on buildings" filter know where the
 *       walks really are;</li>
 *   <li>{@link #afterPlace}: the walks are made walkable ({@link ParkPaden#effen}: a step of 2 or more between two path
 *       blocks becomes a ramp of 1-block steps).</li>
 * </ul>
 * {@code keep_clear} / {@code voorrang} as for every guhs structure; tools/make_v2.py bouwruimte() fills in keep_clear
 * through {@code RUIMTE_HOOKS["guhs:knabbelpark"]}.
 */
public class ParkStructure extends Structure implements BouwRuimte.Ruimte {
    public static final MapCodec<ParkStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Codec.intRange(1, 20).fieldOf("size").forGetter(s -> s.size),
            Codec.intRange(16, 116).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistance),
            Codec.intRange(0, 128).optionalFieldOf("check_radius", 40).forGetter(s -> s.checkRadius),
            Codec.intRange(0, 128).optionalFieldOf("max_height_difference", 14).forGetter(s -> s.maxHeightDifference),
            Identifier.CODEC.listOf().optionalFieldOf("verplicht", List.of()).forGetter(s -> s.verplicht),
            Codec.intRange(1, 32).optionalFieldOf("pogingen", 8).forGetter(s -> s.pogingen),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, ParkStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final int size;
    private final int maxDistance;
    private final int checkRadius;
    private final int maxHeightDifference;
    private final List<Identifier> verplicht;
    private final int pogingen;
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public ParkStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size, int maxDistance, int checkRadius,
                         int maxHeightDifference, List<Identifier> verplicht, int pogingen, int keepClear, Optional<Integer> voorrang) {
        super(settings);
        this.startPool = startPool;
        this.size = size;
        this.maxDistance = maxDistance;
        this.checkRadius = checkRadius;
        this.maxHeightDifference = maxHeightDifference;
        this.verplicht = List.copyOf(verplicht);
        this.pogingen = pogingen;
        this.keepClear = keepClear;
        this.voorrang = voorrang;
    }

    public List<Identifier> verplicht() {
        return verplicht;
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
    public StructureType<?> type() {
        return ParkFeature.KNABBELPARK_TYPE.get();
    }

    // --- where ---------------------------------------------------------------------------------------------------------

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        if (!droogEnGlooiend(context.chunkGenerator(), context.randomState(), context.heightAccessor(), chunk,
                checkRadius > 0 ? BouwRuimte.vlakFactor(context, this) : 1)) {
            return Optional.empty();
        }
        Optional<Layout> layout = layout(context);
        return layout.isEmpty() ? Optional.empty() : BouwRuimte.claim(context, this, Optional.of(layout.get().stub()));
    }

    /** A layout: the stub (pieces built, boxes grown) and the attempt it took (0 = the first draw). */
    public record Layout(GenerationStub stub, int poging) {
    }

    /**
     * The first layout (of {@code pogingen}, each from its own random of the chunk seed) that has every required piece, or
     * empty. Public for the game tests and the worldgen check: it does what generation does, without BouwRuimte.
     */
    public Optional<Layout> layout(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        for (int poging = 0; poging < pogingen; poging++) {
            WorldgenRandom random;
            if (poging == 0) {
                random = context.random();
            } else {
                random = new WorldgenRandom(new LegacyRandomSource(0L));
                random.setLargeFeatureSeed(context.seed() + 0x5DEECE66DL * poging, chunk.x(), chunk.z());
            }
            GenerationContext ctx = new GenerationContext(context.registryAccess(), context.chunkGenerator(), context.biomeSource(),
                    context.randomState(), context.structureTemplateManager(), random, context.seed(), chunk, context.heightAccessor(),
                    context.validBiome());
            Optional<GenerationStub> stub = JigsawPlacement.addPieces(ctx, startPool, Optional.empty(), size,
                    new BlockPos(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ()), false, Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
                    new JigsawStructure.MaxDistance(maxDistance, maxDistance + 32), PoolAliasLookup.EMPTY, DimensionPadding.ZERO,
                    LiquidSettings.IGNORE_WATERLOGGING);
            if (stub.isEmpty()) {
                continue;
            }
            StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
            if (builder.isEmpty() || !compleet(builder.build())) {
                continue;
            }
            // park-plekjes b: once-per-park plekjes, the uitkijktoren on the highest plekje spot (plekjes only, inside their footprint)
            nl.juiced.guhs.feature.park.plek.Uitkijktoren.schik(builder, context.structureTemplateManager(), random);
            groeiPaden(builder.build(), context.chunkGenerator(), context.heightAccessor(), context.randomState());
            return Optional.of(new Layout(new GenerationStub(stub.get().position(), Either.right(builder)), poging));
        }
        return Optional.empty();
    }

    /** Does this set of pieces hold every required template exactly once? */
    public boolean compleet(PiecesContainer pieces) {
        Map<Identifier, Integer> n = tel(pieces);
        for (Identifier id : verplicht) {
            if (n.getOrDefault(id, 0) != 1) {
                return false;
            }
        }
        return true;
    }

    /** How many pieces of every template. */
    public static Map<Identifier, Integer> tel(PiecesContainer pieces) {
        Map<Identifier, Integer> n = new HashMap<>();
        for (StructurePiece piece : pieces.pieces()) {
            Identifier id = PleinSlot.template(piece);
            if (id != null) {
                n.merge(id, 1, Integer::sum);
            }
        }
        return n;
    }

    /** Is the land around this chunk dry (no water at the sample points) and gentle enough? */
    public boolean droogEnGlooiend(ChunkGenerator generator, RandomState random, LevelHeightAccessor height, ChunkPos chunk, int factor) {
        if (checkRadius <= 0) {
            return true;
        }
        int x0 = chunk.getMiddleBlockX(), z0 = chunk.getMiddleBlockZ(), max = maxHeightDifference * Math.max(1, factor);
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                int x = x0 + i * checkRadius / 2, z = z0 + j * checkRadius / 2;
                int top = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
                if (top != generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, random)) {
                    return false;   // (water there: no park in a lake)
                }
                low = Math.min(low, top);
                high = Math.max(high, top);
                if (high - low > max) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The terrain-matching pieces (the walks) get a box as high as the land under them: from 2 below the lowest surface to
     * 5 above the highest, sampled on their corners, edges and middle.
     */
    static void groeiPaden(PiecesContainer pieces, ChunkGenerator generator, LevelHeightAccessor height, RandomState random) {
        for (StructurePiece piece : pieces.pieces()) {
            if (!(piece instanceof PoolElementStructurePiece pool) || pool.getElement().getProjection() != StructureTemplatePool.Projection.TERRAIN_MATCHING) {
                continue;
            }
            BoundingBox box = piece.getBoundingBox();
            int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
            int[] xs = {box.minX(), (box.minX() + box.maxX()) / 2, box.maxX()};
            int[] zs = {box.minZ(), (box.minZ() + box.maxZ()) / 2, box.maxZ()};
            for (int x : xs) {
                for (int z : zs) {
                    int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random) - 1;
                    lo = Math.min(lo, y);
                    hi = Math.max(hi, y);
                }
            }
            box.encapsulate(new BlockPos(box.minX(), lo - 2, box.minZ()));
            box.encapsulate(new BlockPos(box.maxX(), hi + 5, box.maxZ()));
        }
    }

    // --- after the blocks are in -----------------------------------------------------------------------------------------

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                           BoundingBox chunkBox, ChunkPos chunkPos, PiecesContainer pieces) {
        List<BoundingBox> paden = new ArrayList<>(), vast = new ArrayList<>();
        for (StructurePiece piece : pieces.pieces()) {
            if (piece instanceof PoolElementStructurePiece pool && piece.getBoundingBox().intersects(chunkBox.inflatedBy(1))) {
                (pool.getElement().getProjection() == StructureTemplatePool.Projection.TERRAIN_MATCHING ? paden : vast).add(piece.getBoundingBox());
            }
        }
        ParkPaden.effen(level, chunkBox, paden, vast);
    }
}
