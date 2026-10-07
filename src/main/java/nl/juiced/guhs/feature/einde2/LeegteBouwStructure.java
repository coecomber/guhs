package nl.juiced.guhs.feature.einde2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
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
import nl.juiced.guhs.world.BouwRuimte;

/**
 * einde2: the big places of the Guheinde's outer islands (structure type {@code guhs:leegte_bouw}): the four story
 * structures and the eight side places. One huge build that brings its own floating island, saved in tiles
 * ({@code <templates>/stuk_<i>_<j>}, see tools/features/einde2_kit.py) like the Barbecuether's burcht, but it never turns
 * and never looks at the terrain: it floats at a fixed {@code y} in the high band (y 134-253), above everything the
 * terrain can make. So a spot in the template is a spot in the world by a plain shift ({@link #oorsprong}), and where the
 * one guaranteed copy lies follows from the world seed alone ({@link Plekken}).
 * <ul>
 *   <li>{@code anchor}: the spot of the template that lands on the middle of the start chunk (x and z are 16 k + 8, so
 *       the tiles line up with the chunks);</li>
 *   <li>{@code size}: the size of the whole build (the tiles together);</li>
 *   <li>{@code min_distance}: never nearer to 0,0 than this (the main island);</li>
 *   <li>{@code rand}: blocks kept free around the build's box for other high-band structures (the story structures: room
 *       for the veil of the later story): an empty piece that has no blocks and protects nothing.</li>
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
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, LeegteBouwStructure::new));

    private final Identifier templates;
    private final int tilesX, tilesZ, tileSize;
    private final BlockPos anchor;
    private final Vec3i size;
    private final int y;
    private final int minDistance;
    private final int rand;
    /** Room kept around the build (see BouwRuimte). */
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public LeegteBouwStructure(StructureSettings settings, Identifier templates, int tilesX, int tilesZ, int tileSize, BlockPos anchor, Vec3i size,
                               int y, int minDistance, int rand, int keepClear, Optional<Integer> voorrang) {
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
        if (x * x + z * z < (long) minDistance * minDistance) {
            return Optional.empty();
        }
        BlockPos oorsprong = oorsprong(chunk);
        StructureTemplateManager manager = context.structureTemplateManager();
        List<StructurePiece> pieces = new ArrayList<>();
        for (int i = 0; i < tilesX; i++) {
            for (int j = 0; j < tilesZ; j++) {
                Identifier id = tile(i, j);
                if (manager.get(id).isEmpty()) {
                    continue;   // (an empty tile is left out by the builder)
                }
                pieces.add(new Stuk(manager, id, oorsprong.offset(i * tileSize, 0, j * tileSize)));
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
        return BouwRuimte.claim(context, this, Optional.of(new GenerationStub(at, builder -> pieces.forEach(builder::addPiece))));
    }

    @Override
    public StructureType<?> type() {
        return Einde2.LEEGTE_BOUW.get();
    }

    /** One tile of the build: never turned or mirrored. */
    public static class Stuk extends TemplateStructurePiece {
        public Stuk(StructureTemplateManager manager, Identifier id, BlockPos position) {
            super(Einde2.LEEGTE_STUK.get(), 0, manager, id, id.toString(), settings(), position);
        }

        public Stuk(StructureTemplateManager manager, CompoundTag tag) {
            super(Einde2.LEEGTE_STUK.get(), tag, manager, id -> settings());
        }

        static StructurePlaceSettings settings() {
            return new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE)
                    .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
                    .setFinalizeEntities(true);
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
