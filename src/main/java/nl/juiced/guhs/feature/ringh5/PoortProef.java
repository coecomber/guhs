package nl.juiced.guhs.feature.ringh5;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import nl.juiced.guhs.feature.spiesburcht.BurchtStructure;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * bbq2 (ring-h5): a try-out copy of the Zwarte Roosterpoort, for the game tests (the test server has no Guhbarbecuether)
 * and the dev command {@code /guhs ringh5 bouw}: a structure start made by hand with the very tiles worldgen places, handed
 * to {@link Kopieen#test}, so the chapter, the sluier's protection and the inhabitants treat it as a real copy until the
 * server stops (the pattern of feature/paleizen/PaleisProef). Not used in a released game.
 */
public final class PoortProef {
    private static final int TEGEL = 32;

    /**
     * A copy placed so that the build block {@code lokaal} is the world block {@code wereld}, not turned. {@code blokken}:
     * also put its blocks in the world (false: only the copy, for a test that places the few blocks it needs itself).
     */
    public static StructureStart bouw(ServerLevel level, BlockPos lokaal, BlockPos wereld, boolean blokken) {
        Structure structure = Kopieen.structuur(level, RingH5Feature.STRUCTUUR);
        if (!(structure instanceof BurchtStructure burcht)) {
            throw new IllegalArgumentException("no burcht guhs:" + RingH5Feature.STRUCTUUR);
        }
        StructureTemplateManager manager = level.getStructureManager();
        BlockPos nul = wereld.subtract(lokaal);
        BlockPos anchor = burcht.anchor();
        List<StructurePiece> pieces = new ArrayList<>();
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
        if (pieces.isEmpty()) {
            throw new IllegalStateException("guhs:" + RingH5Feature.STRUCTUUR + " has no tiles");
        }
        StructureStart start = new StructureStart(structure, ChunkPos.containing(wereld), 0, new PiecesContainer(pieces));
        if (blokken) {
            for (StructurePiece piece : pieces) {
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), piece.getBoundingBox(),
                        ChunkPos.containing(wereld), nul);
            }
        }
        Kopieen.test(level, start);
        return start;
    }

    private PoortProef() {
    }
}
