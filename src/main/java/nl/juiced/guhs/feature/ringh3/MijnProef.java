package nl.juiced.guhs.feature.ringh3;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * bbq2 (ring-h3): a copy of the mine made by hand, for the game tests and the dev command {@code /guhs ringh3 bouw} (the test
 * server has no Guhbarbecuether, and a dev world may not have walked to the real one yet). {@link #kopie} only says "a copy
 * stands here, turned like this" (the frame every position of {@link Plekken} is mapped through); {@link #bouw} also places
 * the whole template. Both make {@link Mijn} look in every dimension ({@link Mijn#OVERAL}).
 */
public final class MijnProef {
    private MijnProef() {
    }

    /**
     * A copy whose template block (0, 0, 0) is at {@code nul}, turned {@code draai}; {@code doos} = the box that counts as the
     * building (null: the template's own box). Registered with {@link Kopieen#test}: forget it with {@link #weg}.
     */
    @Nullable
    public static StructureStart kopie(ServerLevel level, BlockPos nul, Rotation draai, @Nullable BoundingBox doos) {
        Structure structure = Kopieen.structuur(level, Mijn.STRUCTUUR);
        if (structure == null) {
            return null;
        }
        StructurePoolElement element = StructurePoolElement.single("guhs:" + Mijn.STRUCTUUR).apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = doos != null ? doos : element.getBoundingBox(level.getStructureManager(), nul, draai);
        PoolElementStructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, nul, 0, draai, box,
                LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(structure, ChunkPos.containing(nul), 0, new PiecesContainer(List.of(piece)));
        Kopieen.test(level, start);
        Mijn.OVERAL = true;
        Mijn.vergeetAlles();
        return start;
    }

    /** Places the whole template with its block (0, 0, 0) at {@code nul} and registers the copy. */
    @Nullable
    public static StructureStart bouw(ServerLevel level, BlockPos nul) {
        StructureTemplate template = level.getStructureManager().get(Guhs.id(Mijn.STRUCTUUR)).orElse(null);
        if (template == null) {
            return null;
        }
        template.placeInWorld(level, nul, nul, new StructurePlaceSettings().setKnownShape(true), level.getRandom(), Block.UPDATE_CLIENTS);
        return kopie(level, nul, Rotation.NONE, null);
    }

    /** Forgets every hand-made copy of this level. */
    public static void weg(ServerLevel level) {
        Kopieen.testWissen(level);
        Mijn.OVERAL = false;
        Mijn.vergeetAlles();
    }
}
