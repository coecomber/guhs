package nl.juiced.guhs.feature.einde2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.event.level.LevelEvent;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * einde2: the big places of the Guheinde's outer islands (structure type {@code guhs:leegte_bouw}): the four story
 * structures, the eight side places and the two places of the Sponsguh story. One huge build that brings its own floating
 * island, saved in tiles ({@code <templates>/stuk_<i>_<j>}, see tools/features/einde2_kit.py) like the Barbecuether's
 * burcht, but it never turns and never adapts to the terrain: it floats at a fixed {@code y} in the high band (y 134-253),
 * above everything the terrain can make. So a spot in the template is a spot in the world by a plain shift
 * ({@link #oorsprong}), and where the one guaranteed copy lies follows from the world seed alone ({@link Plekken}).
 * The tiles keep the block states of their templates (nothing looks at its neighbours while a copy appears chunk by
 * chunk), so a ladder, a bed or a door across a tile seam stays whole: the template holds the final states.
 * <ul>
 *   <li>{@code anchor}: the spot of the template that lands on the middle of the start chunk (x and z are 16 k + 8, so
 *       the tiles line up with the chunks);</li>
 *   <li>{@code size}: the size of the whole build (the tiles together);</li>
 *   <li>{@code min_distance}: never nearer to 0,0 than this (the main island);</li>
 *   <li>{@code rand}: blocks kept free around the build's box for other high-band structures (the story structures: room
 *       for the veil of the later story): an empty piece that has no blocks and protects nothing.</li>
 * </ul>
 * The deep band (y 4-117, the Sponsguh pair) lies where the terrain is, so three optional fields find and keep a spot
 * without any:
 * <ul>
 *   <li>{@code leeg} [x, z]: this box from the build's origin must be free of terrain ({@link #terrein}); the later rounds of
 *       the guaranteed search let terrain stand in a few columns, so the one copy always exists. (The outer islands are
 *       about 43 % land in small pieces: a box of 320 x 160 usually has a spot without any, one of 480 x 224 never has and
 *       is found by the relaxed rounds.);</li>
 *   <li>{@code ruim}: the tiles empty their own box before they place their template (rock of such a relaxed spot never
 *       sticks into town);</li>
 *   <li>{@code bij} {structuur, dx, dz}: this build stands at that offset from the origin of the ONE guaranteed copy of
 *       another build and nowhere else ({@link #bijChunk}). Its set makes every chunk a candidate; it claims no room of
 *       its own (two builds that touch would make the lower one give way).</li>
 * </ul>
 */
public class LeegteBouwStructure extends Structure implements BouwRuimte.Ruimte {
    public static final MapCodec<LeegteBouwStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Identifier.CODEC.fieldOf("templates").forGetter(s -> s.templates),
            Codec.INT.fieldOf("tiles_x").forGetter(s -> s.tilesX),
            Codec.INT.fieldOf("tiles_z").forGetter(s -> s.tilesZ),
            Codec.INT.fieldOf("tile_size").forGetter(s -> s.tileSize),
            BlockPos.CODEC.fieldOf("anchor").forGetter(s -> s.anchor),
            Vec3i.CODEC.fieldOf("size").forGetter(s -> s.size),
            Codec.INT.fieldOf("y").forGetter(s -> s.y),
            Codec.INT.fieldOf("min_distance").forGetter(s -> s.minDistance),
            Codec.INT.optionalFieldOf("rand", 0).forGetter(s -> s.rand),
            Codec.INT.listOf(2, 2).optionalFieldOf("leeg").forGetter(s -> s.leeg),
            Codec.BOOL.optionalFieldOf("ruim", false).forGetter(s -> s.ruim),
            Bij.CODEC.optionalFieldOf("bij").forGetter(s -> s.bij),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, LeegteBouwStructure::new));

    /** This build stands at (dx, dz) from the origin of the one guaranteed copy of {@code structuur}. */
    public record Bij(Identifier structuur, int dx, int dz) {
        public static final Codec<Bij> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("structuur").forGetter(Bij::structuur),
                Codec.INT.fieldOf("dx").forGetter(Bij::dx),
                Codec.INT.fieldOf("dz").forGetter(Bij::dz)
        ).apply(i, Bij::new));
    }

    /**
     * How far apart the columns are that {@link #terrein} looks at. (One column costs a whole noise column, about 1.5 ms; a
     * relaxed search round looks at tens of columns per spot and tries hundreds of spots, at every server start.)
     */
    public static final int LEEG_STAP = 32;

    // (per level: the structure state, to find the guaranteed copy a "bij" build stands next to; as BouwRuimte keeps it)
    private static final Map<RandomState, ChunkGeneratorStructureState> STATES = Collections.synchronizedMap(new WeakHashMap<>());

    /** Registers the structure state of a world (at level load, and the game tests' Guheinde: {@link Einde2Test}). */
    public static void onthoud(RandomState random, ChunkGeneratorStructureState state) {
        STATES.put(random, state);
    }

    static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            onthoud(level.getChunkSource().randomState(), level.getChunkSource().getGeneratorState());
        }
    }

    private final Identifier templates;
    private final int tilesX, tilesZ, tileSize;
    private final BlockPos anchor;
    private final Vec3i size;
    private final int y;
    private final int minDistance;
    private final int rand;
    private final Optional<List<Integer>> leeg;
    private final boolean ruim;
    private final Optional<Bij> bij;
    /** Room kept around the build (see BouwRuimte). */
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public LeegteBouwStructure(StructureSettings settings, Identifier templates, int tilesX, int tilesZ, int tileSize, BlockPos anchor, Vec3i size,
                               int y, int minDistance, int rand, Optional<List<Integer>> leeg, boolean ruim, Optional<Bij> bij, int keepClear,
                               Optional<Integer> voorrang) {
        super(settings);
        this.templates = templates;
        this.tilesX = tilesX;
        this.tilesZ = tilesZ;
        this.tileSize = tileSize;
        this.anchor = anchor;
        this.size = size;
        this.y = y;
        this.minDistance = minDistance;
        this.rand = rand;
        this.leeg = leeg;
        this.ruim = ruim;
        this.bij = bij;
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

    public Identifier tile(int i, int j) {
        return templates.withPath(templates.getPath() + "/stuk_" + i + "_" + j);
    }

    public BlockPos anchor() {
        return anchor;
    }

    /** The world y of template y 0. */
    public int y() {
        return y;
    }

    public Vec3i size() {
        return size;
    }

    public int tilesX() {
        return tilesX;
    }

    public int tilesZ() {
        return tilesZ;
    }

    public int rand() {
        return rand;
    }

    /** The box (x, z blocks from the origin) that must be free of terrain; empty: this build asks nothing of the terrain. */
    public Optional<List<Integer>> leeg() {
        return leeg;
    }

    /** Do the tiles empty their box before they place their template? */
    public boolean ruim() {
        return ruim;
    }

    /** The build this one stands next to; empty: it has a spot of its own. */
    public Optional<Bij> bij() {
        return bij;
    }

    /**
     * For a {@code bij} build: its start chunk when the guaranteed copy of its neighbour starts in {@code buurStart} (a pure
     * function; null: this build has no {@code bij}).
     */
    @Nullable
    public ChunkPos bijChunk(LeegteBouwStructure buur, ChunkPos buurStart) {
        if (bij.isEmpty()) {
            return null;
        }
        BlockPos o = buur.oorsprong(buurStart);
        return new ChunkPos((o.getX() + bij.get().dx() + anchor.getX()) >> 4, (o.getZ() + bij.get().dz() + anchor.getZ()) >> 4);
    }

    /** How many columns {@link #terrein} looks at. */
    public int leegKolommen() {
        return leeg.map(l -> kolommen(l.get(0), l.get(1))).orElse(0);
    }

    /** How many of those columns may hold terrain in a search round with this flatness factor (1: none, 2: 5 %, more: 15 %). */
    public int leegMag(int factor) {
        return leegMag(leegKolommen(), factor);
    }

    /** The columns sampled in a box of x by z blocks. */
    public static int kolommen(int lx, int lz) {
        return stappen(lx).length * stappen(lz).length;
    }

    public static int leegMag(int kolommen, int factor) {
        return factor <= 1 ? 0 : kolommen * (factor == 2 ? 5 : 15) / 100;
    }

    /**
     * How many sampled columns of the {@code leeg} box of a build with this origin hold terrain (every {@link #LEEG_STAP}th
     * block and the last one, the corners and the middle first); stops counting above {@code genoeg}. A column without
     * terrain answers the bottom of the world.
     */
    public int terrein(ChunkGenerator generator, RandomState random, LevelHeightAccessor height, BlockPos oorsprong, int genoeg) {
        return leeg.map(l -> terrein(generator, random, height, oorsprong, l.get(0), l.get(1), genoeg)).orElse(0);
    }

    /** The same for any box of x by z blocks (the game tests sample the void with it). */
    public static int terrein(ChunkGenerator generator, RandomState random, LevelHeightAccessor height, BlockPos oorsprong, int lx, int lz, int genoeg) {
        int[] xs = stappen(lx), zs = stappen(lz);
        int n = 0;
        for (int[] eerst : new int[][]{{0, 0}, {xs.length - 1, 0}, {0, zs.length - 1}, {xs.length - 1, zs.length - 1}, {xs.length / 2, zs.length / 2}}) {
            if (kolom(generator, random, height, oorsprong.getX() + xs[eerst[0]], oorsprong.getZ() + zs[eerst[1]]) && ++n > genoeg) {
                return n;
            }
        }
        for (int i = 0; i < xs.length; i++) {
            for (int j = 0; j < zs.length; j++) {
                boolean hoek = (i == 0 || i == xs.length - 1) && (j == 0 || j == zs.length - 1);
                if (hoek || (i == xs.length / 2 && j == zs.length / 2)) {
                    continue;   // (counted above)
                }
                if (kolom(generator, random, height, oorsprong.getX() + xs[i], oorsprong.getZ() + zs[j]) && ++n > genoeg) {
                    return n;
                }
            }
        }
        return n;
    }

    private static boolean kolom(ChunkGenerator generator, RandomState random, LevelHeightAccessor height, int x, int z) {
        return generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random) > height.getMinY();
    }

    /** 0, 32, 64 .. and the last block of a side of this length. */
    private static int[] stappen(int lengte) {
        int n = (lengte - 1) / LEEG_STAP + 1;
        int[] out = new int[(lengte - 1) % LEEG_STAP == 0 ? n : n + 1];
        for (int k = 0; k < n; k++) {
            out[k] = k * LEEG_STAP;
        }
        out[out.length - 1] = lengte - 1;
        return out;
    }

    /** The world position of template 0,0,0 for a start in this chunk (a pure function: no world needed). */
    public BlockPos oorsprong(ChunkPos start) {
        return new BlockPos(start.getMiddleBlockX() - anchor.getX(), y, start.getMiddleBlockZ() - anchor.getZ());
    }

    /** The box of the whole build for a start in this chunk (without the rand). */
    public BoundingBox doos(ChunkPos start) {
        BlockPos o = oorsprong(start);
        return new BoundingBox(o.getX(), o.getY(), o.getZ(), o.getX() + size.getX() - 1, o.getY() + size.getY() - 1, o.getZ() + size.getZ() - 1);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        long x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        if (bij.isPresent()) {
            // (first of all: its set makes every chunk a candidate, and only the one next to its neighbour is it)
            ChunkPos mijn = Plekken.startChunk(context.registryAccess(), STATES.get(context.randomState()), context.seed(), this);
            if (!chunk.equals(mijn)) {
                return Optional.empty();
            }
        } else if (x * x + z * z < (long) minDistance * minDistance) {
            return Optional.empty();
        }
        BlockPos oorsprong = oorsprong(chunk);
        if (leeg.isPresent()) {
            int mag = leegMag(BouwRuimte.vlakFactor(context, this));
            if (terrein(context.chunkGenerator(), context.randomState(), context.heightAccessor(), oorsprong, mag) > mag) {
                return Optional.empty();
            }
        }
        StructureTemplateManager manager = context.structureTemplateManager();
        List<StructurePiece> pieces = new ArrayList<>();
        for (int i = 0; i < tilesX; i++) {
            for (int j = 0; j < tilesZ; j++) {
                Identifier id = tile(i, j);
                if (manager.get(id).isEmpty()) {
                    continue;   // (an empty tile is left out by the builder)
                }
                pieces.add(new Stuk(manager, id, oorsprong.offset(i * tileSize, 0, j * tileSize), ruim));
            }
        }
        if (pieces.isEmpty()) {
            return Optional.empty();
        }
        if (rand > 0) {
            pieces.add(new Rand(doos(chunk).inflatedBy(rand, 0, rand)));   // (after the tiles: the first piece is a real one)
        }
        // (the stub's position is the anchor: its biome decides, and columns have one biome from bottom to top)
        BlockPos at = new BlockPos((int) x, y + anchor.getY(), (int) z);
        Optional<GenerationStub> stub = Optional.of(new GenerationStub(at, builder -> pieces.forEach(builder::addPiece)));
        // (a bij build asks for no room: its neighbour's leeg box kept the spot free, and the two touch on purpose)
        return bij.isPresent() ? stub : BouwRuimte.claim(context, this, stub);
    }

    @Override
    public StructureType<?> type() {
        return Einde2.LEEGTE_BOUW.get();
    }

    /**
     * One tile of the build: never turned or mirrored, and placed with the block states of its template as they are
     * (known shape: no block looks at its neighbours, so nothing breaks off at a tile or chunk seam).
     */
    public static class Stuk extends TemplateStructurePiece {
        private static final String RUIM = "Ruim";
        /** Empty the tile's box (whatever terrain stands in it) before the template is placed. */
        private final boolean ruim;

        public Stuk(StructureTemplateManager manager, Identifier id, BlockPos position, boolean ruim) {
            super(Einde2.LEEGTE_STUK.get(), 0, manager, id, id.toString(), settings(), position);
            this.ruim = ruim;
        }

        public Stuk(StructureTemplateManager manager, CompoundTag tag) {
            super(Einde2.LEEGTE_STUK.get(), tag, manager, id -> settings());
            this.ruim = tag.getBooleanOr(RUIM, false);
        }

        static StructurePlaceSettings settings() {
            return new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE)
                    .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
                    .setFinalizeEntities(true).setKnownShape(true);
        }

        public boolean ruim() {
            return ruim;
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            if (ruim) {
                tag.putBoolean(RUIM, true);
            }
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB,
                                ChunkPos chunkPos, BlockPos referencePos) {
            if (ruim) {
                maakLeeg(level, getBoundingBox(), chunkBB);
            }
            super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
        }

        /**
         * Empties the part of this box that lies in this chunk: in every column that has anything above the bottom of the
         * box, everything from that bottom up to the column's top (not above the top of the box) becomes air. A column of
         * void costs one heightmap lookup.
         */
        static void maakLeeg(WorldGenLevel level, BoundingBox doos, BoundingBox chunkBB) {
            // (a finished chunk only keeps its final heightmaps: a copy put down later, by a test or a command, reads those)
            Heightmap.Types top = level instanceof ServerLevel ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = Math.max(doos.minX(), chunkBB.minX()); x <= Math.min(doos.maxX(), chunkBB.maxX()); x++) {
                for (int z = Math.max(doos.minZ(), chunkBB.minZ()); z <= Math.min(doos.maxZ(), chunkBB.maxZ()); z++) {
                    int tot = Math.min(level.getHeight(top, x, z) - 1, doos.maxY());
                    for (int y = doos.minY(); y <= tot; y++) {
                        level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        @Override
        protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        }
    }

    /**
     * The empty rim around a story structure: only a box, so {@link BouwRuimte} keeps other high-band structures out of
     * it. It places nothing; protection and discovery skip it.
     */
    public static class Rand extends StructurePiece {
        public Rand(BoundingBox box) {
            super(Einde2.LEEGTE_RAND.get(), 0, box);
        }

        public Rand(CompoundTag tag) {
            super(Einde2.LEEGTE_RAND.get(), tag);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB,
                                ChunkPos chunkPos, BlockPos referencePos) {
        }
    }
}
