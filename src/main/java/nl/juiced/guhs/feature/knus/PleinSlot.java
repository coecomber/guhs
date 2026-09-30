package nl.juiced.guhs.feature.knus;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import nl.juiced.guhs.Guhs;

/**
 * The five building slots on the plein of the Knuffeldal town (structure {@code guhs:knuffeldal_stadje}, 2.8): four
 * buildings on the edges (31 x H x 31 templates, one jigsaw at (15, 4, 30) {@code south_up} named
 * {@code guhs:plein_ingang}) and the grijpmachine under the arcade. Phase 1 ships placeholders; each owner replaces the
 * template {@code guhs:knuffeldal_stadje/<id>} (same pool id) from its own generator.
 * <p>
 * The whole town is protected by phase 1 (no breaking or building for non-creative players, {@code Protected.add});
 * slot owners need no protection of their own. Game tests can mark areas with {@link #testStadje} / {@link #testStuk}.
 */
public enum PleinSlot {
    BAKKERIJ, THEEHUIS, KAPPER, CRECHE, GRIJPMACHINE;

    public static final ResourceKey<Structure> STADJE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("knuffeldal_stadje"));

    private static final List<BoundingBox> TEST_STADJES = new CopyOnWriteArrayList<>();
    private static final Map<PleinSlot, List<BoundingBox>> TEST_STUKKEN = new ConcurrentHashMap<>();
    private static final Pattern TEMPLATE = Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)");

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The template of the slot's building: guhs:knuffeldal_stadje/&lt;id&gt;. */
    public Identifier template() {
        return Guhs.id("knuffeldal_stadje/" + id());
    }

    /** The template pool of the slot (one element: {@link #template}). */
    public Identifier pool() {
        return Guhs.id("knuffeldal_stadje/" + id());
    }

    /** The town (structure start) around pos, or null. */
    @Nullable
    public static StructureStart stadje(ServerLevel level, BlockPos pos) {
        Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(STADJE);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
        if (start.isValid()) {
            return start;
        }
        // (not in a piece, but inside the town's box: the streets between the pieces)
        start = level.structureManager().getStructureAt(pos, structure);
        return start.isValid() ? start : null;
    }

    /** The piece of this slot in the town at pos (pos anywhere in that town), or null. */
    @Nullable
    public static BoundingBox stuk(ServerLevel level, BlockPos pos, PleinSlot slot) {
        for (BoundingBox box : TEST_STUKKEN.getOrDefault(slot, List.of())) {
            if (TEST_STADJES.stream().anyMatch(t -> t.isInside(pos)) || box.isInside(pos)) {
                return box;
            }
        }
        StructureStart start = stadje(level, pos);
        if (start == null) {
            return null;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (slot.template().equals(template(piece))) {
                return piece.getBoundingBox();
            }
        }
        return null;
    }

    /** The slot whose building (piece) is at pos, or null. */
    @Nullable
    public static PleinSlot bij(ServerLevel level, BlockPos pos) {
        for (var e : TEST_STUKKEN.entrySet()) {
            for (BoundingBox box : e.getValue()) {
                if (box.isInside(pos)) {
                    return e.getKey();
                }
            }
        }
        StructureStart start = stadje(level, pos);
        if (start == null) {
            return null;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece.getBoundingBox().isInside(pos)) {
                Identifier t = template(piece);
                for (PleinSlot slot : values()) {
                    if (slot.template().equals(t)) {
                        return slot;
                    }
                }
            }
        }
        return null;
    }

    /** Is pos in a piece of a Knuffeldal town (plein, houses, slots)? */
    public static boolean inStadje(ServerLevel level, BlockPos pos) {
        for (BoundingBox box : TEST_STADJES) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(STADJE);
        return structure != null && level.structureManager().getStructureWithPieceAt(pos, structure).isValid();
    }

    /** The template a jigsaw piece was made from (null for other pieces). */
    @Nullable
    public static Identifier template(StructurePiece piece) {
        if (!(piece instanceof PoolElementStructurePiece pool)) {
            return null;
        }
        // (SinglePoolElement keeps its location to itself; its toString is "Single[Left[<location>]]")
        Matcher m = TEMPLATE.matcher(pool.getElement().toString());
        return m.find() ? Identifier.tryParse(m.group(1)) : null;
    }

    // --- game tests -----------------------------------------------------------------------------------------------------

    /** (Tests) this box counts as a Knuffeldal town. */
    public static void testStadje(BoundingBox box) {
        TEST_STADJES.add(box);
    }

    /** (Tests) this box counts as the building of a slot (also inside a test town). */
    public static void testStuk(PleinSlot slot, BoundingBox box) {
        TEST_STUKKEN.computeIfAbsent(slot, s -> new CopyOnWriteArrayList<>()).add(box);
    }

    /** (Tests, server stop) forget the test areas. */
    public static void testWissen() {
        TEST_STADJES.clear();
        TEST_STUKKEN.clear();
    }
}
