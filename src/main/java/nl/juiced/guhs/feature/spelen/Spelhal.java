package nl.juiced.guhs.feature.spelen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * What the five cave-minigame halls of 1.7.0 share (Schatgraven, Glimwormvangen, Lichtjespuzzel, the Wisselgrot, the
 * Kegelgrot): which spots of their buildings are protected. Each building is three jigsaw pieces: the hall (the start
 * piece), a shaft of nothing and the entrance hut. The hall's template is the hall itself plus {@link #RAND} blocks of room
 * on every side (the world's own rock and the extra entrance tunnels: tools/features/spelhal_bouw.py), so only the start
 * piece's box minus {@link #RAND} sideways is protected (the same box whichever way the piece was turned), and the hut;
 * the shaft is only nothing (the rock between the hall and the hut stays the world's). So a player who digs in from a cave
 * reaches the hall's wall, and the entrance tunnels can be walked and dug.
 */
public final class Spelhal {
    /** The room around the hall in its template (tools/features/spelhal_bouw.py RAND, checked there). */
    public static final int RAND = 8;

    /** Is this spot part of one of these buildings (the hall with its walls, or the hut)? */
    public static boolean beschermd(ServerLevel level, BlockPos pos, List<ResourceKey<Structure>> structuren) {
        if (!level.structureManager().hasAnyStructureAt(pos)) {
            return false;
        }
        var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (ResourceKey<Structure> key : structuren) {
            Structure structure = reg.getValue(key);
            if (structure == null) {
                continue;
            }
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
            if (start.isValid() && binnen(dozen(start.getPieces()), pos)) {
                return true;
            }
        }
        return false;
    }

    /** The protected boxes of a building's pieces (in their order: the first is the hall, the shaft is skipped). */
    public static List<BoundingBox> dozen(List<StructurePiece> pieces) {
        List<BoundingBox> uit = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            BoundingBox b = pieces.get(i).getBoundingBox();
            if (i == 0) {
                uit.add(hal(b));
            } else if (b.getXSpan() > 1 || b.getZSpan() > 1) {
                uit.add(b);
            }
        }
        return uit;
    }

    /** The hall in the start piece's box: RAND less on every side (not up or down). */
    public static BoundingBox hal(BoundingBox piece) {
        return new BoundingBox(piece.minX() + RAND, piece.minY(), piece.minZ() + RAND, piece.maxX() - RAND, piece.maxY(), piece.maxZ() - RAND);
    }

    public static boolean binnen(List<BoundingBox> dozen, BlockPos pos) {
        for (BoundingBox b : dozen) {
            if (b.isInside(pos)) {
                return true;
            }
        }
        return false;
    }

    private Spelhal() {
    }
}
