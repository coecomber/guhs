package nl.juiced.guhs.feature.park.plek;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.park.ParkFeature;
import nl.juiced.guhs.feature.park.ParkPlekjes;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * 1.4.2 (park-plekjes b): the post-step of the park's layout (called once from {@code ParkStructure.layout}, after a
 * complete layout is drawn; the Plekjes API point 3):
 * <ol>
 *   <li>the plekjes of {@link #UNIEK} stand at most once in a park: a second copy (and a third...) becomes the little flower
 *       bed {@link #VULSEL} (smaller than every one of them, so it fits where they stood);</li>
 *   <li>in {@value #KANS} of 4 parks (from the layout's own random: the same every time) the plekje on the HIGHEST spot of
 *       the park (the highest ground of all plekjes whose footprint can hold the tower, {@value #TOREN_MAAT} x
 *       {@value #TOREN_MAAT}) becomes the {@link #TOREN uitkijktoren}.</li>
 * </ol>
 * A swap keeps the plekje's entrance where it was (the walk's side connector), its rotation, height and junctions; the new
 * piece lies inside the old piece's footprint (checked), so nothing else of the park moves, the required pieces stay
 * (ParkStructure.compleet) and the reach stays within the jigsaw limit. Only plekjes (rigid pieces of the park with the
 * entrance jigsaw {@link ParkPlekjes#INGANG}) are ever swapped.
 */
public final class Uitkijktoren {
    public static final String TOREN = "knabbelpark/uitkijktoren", VULSEL = "knabbelpark/plek_b_vulsel";
    /** The plekjes that stand at most once in a park (park_plek_b.PLEKJES). */
    public static final List<String> UNIEK = List.of("knabbelpark/duivenvoer_oma", "knabbelpark/poppenkast", "knabbelpark/wensboom",
            "knabbelpark/mika_bankje");
    /** The tower's footprint (park_plek_b.TOREN_MAAT); in how many of 4 parks it stands. */
    public static final int TOREN_MAAT = 9, KANS = 3;

    private Uitkijktoren() {
    }

    /** What {@link #schik} did (for the game tests and /guhs parkb toren). */
    public record Schikking(int vervangen, @Nullable BoundingBox toren, int hoogste) {
    }

    /** The post-step on a complete layout: once-per-park plekjes, the tower on the highest plekje spot. */
    public static Schikking schik(StructurePiecesBuilder builder, StructureTemplateManager mgr, RandomSource random) {
        List<StructurePiece> pieces = new ArrayList<>(builder.build().pieces());
        int vervangen = 0;
        Set<String> gezien = new HashSet<>();
        for (int i = 0; i < pieces.size(); i++) {
            Identifier id = PleinSlot.template(pieces.get(i));
            if (id == null || !UNIEK.contains(id.getPath()) || !plekje(pieces.get(i), mgr)) {
                continue;
            }
            if (!gezien.add(id.getPath())) {
                StructurePiece nieuw = wissel((PoolElementStructurePiece) pieces.get(i), VULSEL, mgr);
                if (nieuw != pieces.get(i)) {
                    pieces.set(i, nieuw);
                    vervangen++;
                }
            }
        }
        BoundingBox toren = null;
        int hoogste = Integer.MIN_VALUE, best = -1;
        for (int i = 0; i < pieces.size(); i++) {
            StructurePiece piece = pieces.get(i);
            if (plekje(piece, mgr) && past(piece, mgr) && piece.getBoundingBox().minY() > hoogste) {
                hoogste = piece.getBoundingBox().minY();
                best = i;
            }
        }
        if (best >= 0 && random.nextInt(4) < KANS) {
            StructurePiece nieuw = wissel((PoolElementStructurePiece) pieces.get(best), TOREN, mgr);
            if (nieuw != pieces.get(best)) {
                pieces.set(best, nieuw);
                toren = nieuw.getBoundingBox();
            }
        }
        if (vervangen > 0 || toren != null) {
            builder.clear();
            pieces.forEach(builder::addPiece);
        }
        return new Schikking(vervangen, toren, hoogste);
    }

    /** Is this piece a plekje: a rigid piece of the park with the plekje entrance? */
    static boolean plekje(StructurePiece piece, StructureTemplateManager mgr) {
        if (!(piece instanceof PoolElementStructurePiece pool) || pool.getElement().getProjection() != StructureTemplatePool.Projection.RIGID) {
            return false;
        }
        Identifier id = PleinSlot.template(piece);
        return id != null && id.getPath().startsWith("knabbelpark/") && ingang(pool.getElement(), mgr, pool.getRotation()) != null;
    }

    /** Can the tower stand on this plekje's spot (its template is at least as wide and deep)? */
    static boolean past(StructurePiece piece, StructureTemplateManager mgr) {
        Identifier id = PleinSlot.template(piece);
        if (id == null || id.getPath().equals(TOREN)) {
            return id != null;
        }
        Vec3i maat = mgr.getOrCreate(id).getSize();
        return maat.getX() >= TOREN_MAAT && maat.getZ() >= TOREN_MAAT;
    }

    /** The position of the plekje entrance of this element (turned, template at 0,0,0), or null. */
    @Nullable
    static BlockPos ingang(StructurePoolElement element, StructureTemplateManager mgr, Rotation rotation) {
        for (StructureTemplate.JigsawBlockInfo j : element.getShuffledJigsawBlocks(mgr, BlockPos.ZERO, rotation, RandomSource.create(0L))) {
            if (j.name().toString().equals(ParkPlekjes.INGANG)) {
                return j.info().pos();
            }
        }
        return null;
    }

    /**
     * This plekje's piece with another plekje template in its place: the same entrance spot, rotation, height and junctions.
     * The old piece itself when the new one would not fit in its footprint.
     */
    static StructurePiece wissel(PoolElementStructurePiece oud, String template, StructureTemplateManager mgr) {
        StructurePoolElement element = StructurePoolElement.single("guhs:" + template).apply(StructureTemplatePool.Projection.RIGID);
        Rotation rot = oud.getRotation();
        BlockPos was = ingang(oud.getElement(), mgr, rot), wordt = ingang(element, mgr, rot);
        if (was == null || wordt == null) {
            return oud;
        }
        BlockPos pos = oud.getPosition().offset(was).subtract(wordt);
        BoundingBox box = element.getBoundingBox(mgr, pos, rot);
        BoundingBox b = oud.getBoundingBox();
        if (box.minX() < b.minX() || box.maxX() > b.maxX() || box.minZ() < b.minZ() || box.maxZ() > b.maxZ()) {
            return oud;
        }
        PoolElementStructurePiece nieuw = new PoolElementStructurePiece(mgr, element, pos, oud.getGroundLevelDelta(), rot, box,
                LiquidSettings.IGNORE_WATERLOGGING);
        oud.getJunctions().forEach(nieuw::addJunction);
        return nieuw;
    }

    /** Is this block part of an uitkijktoren of a generated park? */
    public static boolean opToren(ServerLevel level, BlockPos pos) {
        StructureStart start = Bezetting.start(level, ParkFeature.STRUCTUUR, pos);
        String stuk = start == null ? null : Kopieen.stukBij(start, pos);
        return stuk != null && stuk.equals(TOREN);
    }
}
