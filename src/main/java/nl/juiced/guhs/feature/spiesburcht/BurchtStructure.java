package nl.juiced.guhs.feature.spiesburcht;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * The big buildings of the Barbecuether (guhs:spiesburcht and guhs:mika_grillpaleis): one huge build, saved in tiles
 * ({@code <templates>/stuk_<i>_<j>}, see tools/features/spiesburcht_burcht.py) so every chunk only handles the blocks
 * that are really in it. All tiles turn together around the build's anchor.
 * <ul>
 *   <li>{@code "brug"} (the fortress): its bridge deck goes at the height between min_y and max_y where the most
 *       sample points around it are open cave (the build carves its own corridors through the rest, like a nether
 *       fortress; its pillars reach down into the sauce);</li>
 *   <li>{@code "paleis"} (the bastion): its ground floor is at min_y, just above the frying-sauce sea, but only where
 *       the middle is open (cave or sea), not deep inside the rock.</li>
 * </ul>
 */
public class BurchtStructure extends Structure implements nl.juiced.guhs.world.BouwRuimte.Ruimte {
    public static final MapCodec<BurchtStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            ResourceLocation.CODEC.fieldOf("templates").forGetter(s -> s.templates),
            Codec.INT.fieldOf("tiles_x").forGetter(s -> s.tilesX),
            Codec.INT.fieldOf("tiles_z").forGetter(s -> s.tilesZ),
            Codec.INT.fieldOf("tile_size").forGetter(s -> s.tileSize),
            BlockPos.CODEC.fieldOf("anchor").forGetter(s -> s.anchor),
            Codec.STRING.fieldOf("placement").forGetter(s -> s.placement),
            Codec.INT.fieldOf("min_y").forGetter(s -> s.minY),
            Codec.INT.fieldOf("max_y").forGetter(s -> s.maxY),
            Codec.INT.optionalFieldOf("reach", 32).forGetter(s -> s.reach),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, BurchtStructure::new));

    private final ResourceLocation templates;
    private final int tilesX, tilesZ, tileSize;
    private final BlockPos anchor;
    private final String placement;
    private final int minY, maxY;
    /** How far from the anchor the sample points for the height lie. */
    private final int reach;
    /** Room kept around the build (see BouwRuimte). */
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public BurchtStructure(StructureSettings settings, ResourceLocation templates, int tilesX, int tilesZ, int tileSize, BlockPos anchor,
                           String placement, int minY, int maxY, int reach, int keepClear, Optional<Integer> voorrang) {
        super(settings);
        this.templates = templates;
        this.tilesX = tilesX;
        this.tilesZ = tilesZ;
        this.tileSize = tileSize;
        this.anchor = anchor;
        this.placement = placement;
        this.minY = minY;
        this.maxY = maxY;
        this.reach = reach;
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

    public ResourceLocation tile(int i, int j) {
        return templates.withPath(templates.getPath() + "/stuk_" + i + "_" + j);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        int y = placement.equals("paleis") ? palaceHeight(context, x, z) : bridgeHeight(context, x, z);
        if (y == Integer.MIN_VALUE) {
            return Optional.empty();
        }
        Rotation rotation = Rotation.getRandom(context.random());
        BlockPos at = new BlockPos(x, y, z);
        StructureTemplateManager manager = context.structureTemplateManager();
        List<Piece> pieces = new ArrayList<>();
        for (int i = 0; i < tilesX; i++) {
            for (int j = 0; j < tilesZ; j++) {
                ResourceLocation id = tile(i, j);
                if (manager.get(id).isEmpty()) {
                    continue;
                }
                BlockPos offset = new BlockPos(i * tileSize, 0, j * tileSize);
                BlockPos pivot = anchor.subtract(offset);
                pieces.add(new Piece(manager, id, at.subtract(anchor).offset(offset), rotation, pivot));
            }
        }
        if (pieces.isEmpty()) {
            return Optional.empty();
        }
        return nl.juiced.guhs.world.BouwRuimte.claim(context, this, Optional.of(new GenerationStub(at, builder -> pieces.forEach(builder::addPiece))));
    }

    /** The fortress deck: the height (min_y..max_y) with the most open air at the sample points. */
    private int bridgeHeight(GenerationContext context, int x, int z) {
        List<NoiseColumn> columns = columns(context, x, z);
        int best = Integer.MIN_VALUE, bestScore = 2;
        for (int y = maxY; y >= minY; y -= 2) {
            int score = 0;
            for (int c = 0; c < columns.size(); c++) {
                if (open(columns.get(c), y + 1, 3)) {
                    score += c == 0 ? 2 : 1;             // (the middle counts double)
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = y;
            }
        }
        return best;
    }

    /** The palace floor: at min_y, if the middle and at least two of the four sides are open there (air or sauce). */
    private int palaceHeight(GenerationContext context, int x, int z) {
        List<NoiseColumn> columns = columns(context, x, z);
        if (!open(columns.get(0), minY + 1, 4)) {
            return Integer.MIN_VALUE;
        }
        int sides = 0;
        for (int c = 1; c < columns.size(); c++) {
            sides += open(columns.get(c), minY + 2, 2) ? 1 : 0;
        }
        return sides >= 2 ? minY : Integer.MIN_VALUE;
    }

    private List<NoiseColumn> columns(GenerationContext context, int x, int z) {
        List<NoiseColumn> list = new ArrayList<>();
        for (int[] d : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            list.add(context.chunkGenerator().getBaseColumn(x + d[0] * reach, z + d[1] * reach, context.heightAccessor(), context.randomState()));
        }
        return list;
    }

    /** `height` blocks from y up that are air or fluid (the sauce sea counts as open: the palace rises out of it). */
    private static boolean open(NoiseColumn column, int y, int height) {
        for (int i = 0; i < height; i++) {
            BlockState state = column.getBlock(y + i);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StructureType<?> type() {
        return SpiesburchtFeature.BURCHT_TYPE.get();
    }

    /** One tile of the build. */
    public static class Piece extends TemplateStructurePiece {
        public Piece(StructureTemplateManager manager, ResourceLocation id, BlockPos position, Rotation rotation, BlockPos pivot) {
            super(SpiesburchtFeature.BURCHT_PIECE.get(), 0, manager, id, id.toString(), settings(rotation, pivot), position);
        }

        public Piece(StructureTemplateManager manager, CompoundTag tag) {
            super(SpiesburchtFeature.BURCHT_PIECE.get(), tag, manager,
                    id -> settings(Rotation.valueOf(tag.getString("Rot")), BlockPos.of(tag.getLong("Pivot"))));
        }

        static StructurePlaceSettings settings(Rotation rotation, BlockPos pivot) {
            return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(pivot).setMirror(Mirror.NONE)
                    .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
                    .setFinalizeEntities(true);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("Rot", placeSettings.getRotation().name());
            tag.putLong("Pivot", placeSettings.getRotationPivot().asLong());
        }

        @Override
        protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        }
    }
}
