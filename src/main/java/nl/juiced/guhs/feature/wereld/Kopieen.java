package nl.juiced.guhs.feature.wereld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.spiesburcht.BurchtStructure;

/**
 * bbq2: the copies (structure starts) of a guhs structure around a spot, and where the blocks of their templates are in the
 * world. Shared by {@link Bezetting} (NPCs and props at copies that exist already) and {@link Bescherming} (the pieces of a
 * copy can't be broken). Only loaded chunks are looked at: a chunk knows which starts reach into it (its structure
 * references); a start that was found is kept, so its start chunk is read once.
 * <p>
 * Three kinds of pieces are understood: jigsaw pieces (a template per piece: the surface buildings and the barbecueput), the
 * tiles of a {@code guhs:burcht} (one big build: coordinates count in the whole build) and plain template pieces.
 */
public final class Kopieen {
    /** (not saved) per level: structure -> start chunk -> its start. */
    private static final Map<ServerLevel, Map<Structure, Map<Long, StructureStart>>> STARTS = Collections.synchronizedMap(new WeakHashMap<>());
    /** (tests: the test server has none of our dimensions) starts that count as standing in this level. */
    private static final Map<ServerLevel, List<StructureStart>> TEST = Collections.synchronizedMap(new WeakHashMap<>());

    private Kopieen() {
    }

    /** The structure {@code guhs:<naam>} of this level's registries (null: there is none). */
    @Nullable
    public static Structure structuur(ServerLevel level, String naam) {
        return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(ResourceKey.create(Registries.STRUCTURE, Guhs.id(naam)));
    }

    /**
     * The copies of this structure that have a piece within {@code afstand} blocks (per axis) of this spot. Looks at the
     * loaded chunks around the spot only (server thread).
     */
    public static List<StructureStart> bij(ServerLevel level, Structure structuur, BlockPos bij, int afstand) {
        List<StructureStart> uit = new ArrayList<>();
        int x0 = (bij.getX() - afstand) >> 4, x1 = (bij.getX() + afstand) >> 4, z0 = (bij.getZ() - afstand) >> 4, z1 = (bij.getZ() + afstand) >> 4;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                LongSet refs = chunk.getAllReferences().get(structuur);
                if (refs == null || refs.isEmpty()) {
                    continue;
                }
                for (long ref : refs) {
                    StructureStart start = start(level, structuur, ref);
                    if (start != null && !uit.contains(start) && dichtbij(start, bij, afstand)) {
                        uit.add(start);
                    }
                }
            }
        }
        for (StructureStart start : TEST.getOrDefault(level, List.of())) {
            if (start.getStructure() == structuur && !uit.contains(start) && dichtbij(start, bij, afstand)) {
                uit.add(start);
            }
        }
        return uit;
    }

    /** The start of this structure in this start chunk (kept once found; null: none). */
    @Nullable
    private static StructureStart start(ServerLevel level, Structure structuur, long startChunk) {
        Map<Long, StructureStart> known = STARTS.computeIfAbsent(level, l -> new ConcurrentHashMap<>()).computeIfAbsent(structuur, s -> new ConcurrentHashMap<>());
        StructureStart start = known.get(startChunk);
        if (start == null) {
            start = level.getChunk(ChunkPos.getX(startChunk), ChunkPos.getZ(startChunk), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structuur);
            if (start == null || !start.isValid()) {
                return null;
            }
            known.put(startChunk, start);
        }
        return start;
    }

    private static boolean dichtbij(StructureStart start, BlockPos bij, int afstand) {
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox b = piece.getBoundingBox();
            if (bij.getX() >= b.minX() - afstand && bij.getX() <= b.maxX() + afstand && bij.getZ() >= b.minZ() - afstand && bij.getZ() <= b.maxZ() + afstand
                    && bij.getY() >= b.minY() - afstand && bij.getY() <= b.maxY() + afstand) {
                return true;
            }
        }
        return false;
    }

    /** The template id of a piece (null: a piece without a template). */
    @Nullable
    public static Identifier template(StructurePiece piece) {
        if (piece instanceof BurchtStructure.Piece) {
            return null;   // (tiles: asked by coordinates of the whole build, not by name)
        }
        return PleinSlot.template(piece);
    }

    /**
     * The world position of a block of a copy's template. {@code stuk}: part of the template name of the piece that is
     * meant (null: the start piece); for a {@code guhs:burcht} (one build in tiles) {@code stuk} is ignored and
     * {@code lokaal} counts in the coordinates of the whole build. Null when the copy has no such piece (e.g. a small
     * barbecueput when the big one is asked for).
     */
    @Nullable
    public static BlockPos wereld(StructureStart start, @Nullable String stuk, BlockPos lokaal) {
        if (start.getStructure() instanceof BurchtStructure burcht) {
            for (StructurePiece piece : start.getPieces()) {
                if (piece instanceof TemplateStructurePiece tile) {
                    // (every tile turns around the build's anchor: world = generation point + turned (lokaal - anchor); the
                    // generation point = this tile's template position + its pivot)
                    BlockPos at = tile.templatePosition().offset(tile.placeSettings().getRotationPivot());
                    return at.offset(StructureTemplate.transform(lokaal.subtract(burcht.anchor()), Mirror.NONE, tile.placeSettings().getRotation(), BlockPos.ZERO));
                }
            }
            return null;
        }
        StructurePiece piece = stuk(start, stuk);
        if (piece instanceof PoolElementStructurePiece pool) {
            return pool.getPosition().offset(StructureTemplate.transform(lokaal, Mirror.NONE, pool.getRotation(), BlockPos.ZERO));
        }
        if (piece instanceof TemplateStructurePiece template) {
            return StructureTemplate.calculateRelativePosition(template.placeSettings(), lokaal).offset(template.templatePosition());
        }
        return null;
    }

    /**
     * The other way round: the template coordinates ({@link #wereld}'s {@code lokaal}) of a world position, for the piece
     * {@code stuk} of this copy (a guhs:burcht: of the whole build). Null when the copy has no such piece. For finding the
     * numbers to register: stand at the spot in a real copy and ask /guhs wereld kopie &lt;structuur&gt;.
     */
    @Nullable
    public static BlockPos lokaal(StructureStart start, @Nullable String stuk, BlockPos wereld) {
        BlockPos nul = wereld(start, stuk, BlockPos.ZERO);
        if (nul == null) {
            return null;
        }
        Rotation terug = switch (draai(start, stuk)) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            case CLOCKWISE_180 -> Rotation.CLOCKWISE_180;
            default -> Rotation.NONE;
        };
        return StructureTemplate.transform(wereld.subtract(nul), Mirror.NONE, terug, BlockPos.ZERO);
    }

    /**
     * The jigsaw piece of this copy this world position lies in: its template name (the {@code stuk} to register with), or
     * null: no piece there, or a copy that is one build (a guhs:burcht: coordinates count in the whole build, no stuk).
     */
    @Nullable
    public static String stukBij(StructureStart start, BlockPos wereld) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece.getBoundingBox().isInside(wereld)) {
                Identifier id = template(piece);
                if (id != null) {
                    return id.getPath();
                }
            }
        }
        return null;
    }

    /** How the piece that {@link #wereld} uses is turned (NONE when there is no such piece). */
    public static Rotation draai(StructureStart start, @Nullable String stuk) {
        if (start.getStructure() instanceof BurchtStructure) {
            for (StructurePiece piece : start.getPieces()) {
                if (piece instanceof TemplateStructurePiece tile) {
                    return tile.placeSettings().getRotation();
                }
            }
            return Rotation.NONE;
        }
        StructurePiece piece = stuk(start, stuk);
        if (piece instanceof PoolElementStructurePiece pool) {
            return pool.getRotation();
        }
        return piece instanceof TemplateStructurePiece template ? template.placeSettings().getRotation() : Rotation.NONE;
    }

    /** The piece whose template name contains {@code stuk} (null: the first piece). */
    @Nullable
    private static StructurePiece stuk(StructureStart start, @Nullable String stuk) {
        for (StructurePiece piece : start.getPieces()) {
            if (stuk == null) {
                return piece;
            }
            Identifier id = template(piece);
            if (id != null && id.getPath().contains(stuk)) {
                return piece;
            }
        }
        return null;
    }

    // --- game tests ------------------------------------------------------------------------------------------------------

    /** (Tests) this start counts as a copy standing in this level. */
    public static void test(ServerLevel level, StructureStart start) {
        TEST.computeIfAbsent(level, l -> new CopyOnWriteArrayList<>()).add(start);
        Bescherming.wis(level);
    }

    /** (Tests) forget the test copies of this level. */
    public static void testWissen(ServerLevel level) {
        TEST.remove(level);
        Bescherming.wis(level);
    }

    /** (Tests) the test copies of this level. */
    static List<StructureStart> test(ServerLevel level) {
        return TEST.getOrDefault(level, List.of());
    }
}
