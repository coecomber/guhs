package nl.juiced.guhs.feature.ringsausuman;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;

/**
 * bbq2 (ring-sausuman): the geometry of the Toren van Sausuman (structure {@code guhs:sausuman_toren}, one template of the
 * same name: tools/features/ring_sausuman_bouw.py). The template coordinates here are the PLEKKEN of that file; the module's
 * self-check stops the build when the two differ.
 */
public final class Toren {
    public static final String STRUCTUUR = "sausuman_toren";
    /** The Bezetting id of Sausuman (also the tag of the NPC in the template). */
    public static final String SAUSUMAN = "ringsausuman_sausuman";
    /** The template's ground layer (feet stand on G + 1). */
    public static final int G = 3;

    /** Sausuman, in the hall next to his machine. */
    public static final BlockPos NPC = new BlockPos(11, 4, 10);
    /** He looks at whoever comes in (south-east). */
    public static final float NPC_YAW = -45.0f;
    /** The Ringenbakker's face: the anchor of the baking scene; in the template it looks south. */
    public static final BlockPos BAKKER = new BlockPos(14, 5, 8);
    /** The three stations (deeg, saus, kaas), one on each floor above the hall. */
    public static final List<BlockPos> VOORRADEN = List.of(new BlockPos(14, 11, 8), new BlockPos(14, 16, 17), new BlockPos(14, 21, 9));
    /** The Mika-rad that all of the hall's machines hang on. */
    public static final BlockPos MIKARAD = new BlockPos(9, 4, 13);
    /** The Pannantir on its pedestal in the study. */
    public static final BlockPos PANNANTIR = new BlockPos(14, 22, 13);
    /** Just outside the door. */
    public static final BlockPos DEUR = new BlockPos(14, 4, 20);
    /** The Rustvuurtje in the yard. */
    public static final BlockPos RUSTVUUR = new BlockPos(20, 4, 25);

    /**
     * How a copy of the tower is turned, read from its Ringenbakker: in the template the machine looks south, so its facing
     * in the world is the template's rotation. The baking scene is written against the unturned template and anchored on
     * the machine with this rotation: it fits every copy (and a machine somebody placed by hand).
     */
    public static Rotation draai(Direction bakkerKijkt) {
        return switch (bakkerKijkt) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** (dev, tests) the tower's template with its corner here, unturned; false: no such template. */
    public static boolean plaats(ServerLevel level, BlockPos hoek) {
        Optional<StructureTemplate> template = level.getStructureManager().get(Guhs.id(STRUCTUUR));
        if (template.isEmpty()) {
            return false;
        }
        template.get().placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), 2);
        return true;
    }

    private Toren() {
    }
}
