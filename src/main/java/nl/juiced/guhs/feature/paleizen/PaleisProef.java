package nl.juiced.guhs.feature.paleizen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import nl.juiced.guhs.feature.spiesburcht.BurchtStructure;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * bbq2 (paleizen): a try-out copy of one of the three palaces, for the game tests (the test server has no
 * Guhbarbecuether) and for the dev command {@code /guhs paleizen bouw}. The copy is a structure start made by hand, with the
 * very pieces worldgen makes (the tiles of a guhs:burcht, the one jigsaw piece of the stal), handed to
 * {@link Kopieen#test}: the questlines, the protection and the inhabitants then treat it as a real copy until the server
 * stops. Not used in a released game.
 */
public final class PaleisProef {
    private static final int TEGEL = 32;

    /**
     * A copy of {@code guhs:<structuur>} placed so that the template block {@code lokaal} is the world block {@code wereld}
     * (not turned). {@code blokken}: also put its blocks and inhabitants in the world (false: only the copy, for a test that
     * places the few blocks it needs itself).
     */
    public static StructureStart bouw(ServerLevel level, String structuur, BlockPos lokaal, BlockPos wereld, boolean blokken) {
        Structure structure = Kopieen.structuur(level, structuur);
        if (structure == null) {
            throw new IllegalArgumentException("no structure guhs:" + structuur);
        }
        StructureTemplateManager manager = level.getStructureManager();
        BlockPos nul = wereld.subtract(lokaal);                       // (the world position of template block 0, 0, 0)
        List<StructurePiece> pieces = new ArrayList<>();
        if (structure instanceof BurchtStructure burcht) {
            BlockPos anchor = burcht.anchor();
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    Identifier id = burcht.tile(i, j);
                    if (manager.get(id).isEmpty()) {
                        continue;
                    }
                    BlockPos offset = new BlockPos(i * TEGEL, 0, j * TEGEL);
                    pieces.add(new BurchtStructure.Piece(manager, id, nul.offset(offset), Rotation.NONE, anchor.subtract(offset)));
                }
            }
        } else {
            StructurePoolElement element = StructurePoolElement.single("guhs:" + structuur).apply(StructureTemplatePool.Projection.RIGID);
            BoundingBox box = element.getBoundingBox(manager, nul, Rotation.NONE);
            pieces.add(new PoolElementStructurePiece(manager, element, nul, 0, Rotation.NONE, box, LiquidSettings.IGNORE_WATERLOGGING));
        }
        if (pieces.isEmpty()) {
            throw new IllegalStateException("guhs:" + structuur + " has no templates");
        }
        StructureStart start = new StructureStart(structure, ChunkPos.containing(wereld), 0, new PiecesContainer(pieces));
        if (blokken) {
            for (StructurePiece piece : pieces) {
                BoundingBox box = piece.getBoundingBox();
                if (piece instanceof PoolElementStructurePiece pool) {
                    pool.place(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box, nul, false);
                } else {
                    piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box,
                            ChunkPos.containing(wereld), nul);
                }
            }
        }
        Kopieen.test(level, start);
        return start;
    }

    /** The dev command: the building in front of the player, its way in at their feet. */
    public static StructureStart bouw(ServerLevel level, String structuur, BlockPos bij, boolean blokken) {
        BlockPos ingang = switch (structuur) {
            case PaleisPlekken.BRUGPALEIS -> new BlockPos(2, 26, 15);        // (the foot of the west stair)
            case PaleisPlekken.WOONBLOKKEN -> new BlockPos(31, 13, 53);      // (the landing outside the gate)
            default -> new BlockPos(40, 4, 12);                              // (the yard in front of the barn door)
        };
        return bouw(level, structuur, ingang, bij, blokken);
    }

    private PaleisProef() {
    }
}
