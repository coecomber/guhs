package nl.juiced.guhs.feature.snuffelsteiger;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The steigerhuisje as a place (structure {@code guhs:steigerhuisje}; template and geometry:
 * tools/features/snuffel_steiger_bouw.py, whose numbers the module's self-check compares with the constants here).
 * <p>
 * Everything in this package counts in ONE system of coordinates: the template's own blocks, with {@code +z} out to sea,
 * and {@link #G} the layer of the quay and the pier's planks (world y 64). A {@link #punt} is such a position as a
 * cutscene wants it: relative to {@link #VOET}, the first plank of the pier (the anchor block of every scene), with y 0 =
 * standing on the deck. {@link Oord} is one copy in the world: where that anchor is and how the copy is turned.
 */
public final class Steiger {
    public static final String STRUCTUUR = "steigerhuisje";
    /** Template y of the ground and the deck; the world y of that layer (the sea's top water block is two lower). */
    public static final int G = 30, DEK_Y = 64;
    /** Template: the first plank of the pier (what the structure type puts on the water's edge), and where you stand on it. */
    public static final BlockPos ANKER = new BlockPos(10, G, 14), VOET = new BlockPos(10, G + 1, 14);
    /** Template: the size of the whole build (x, z) and the last row of the quay. */
    public static final int SX = 23, SZ = 34, PLOT_Z = 13;

    /** Template positions (x, y above the deck, z) and yaws of who lives here, and of the boat. */
    public static final Vec3 PUP = new Vec3(4.5, 0.5625, 5.85), BUUR = new Vec3(5.5, 0, 6.5), KAPITEIN = new Vec3(11.5, 0, 29.5),
            BOOT = new Vec3(14.6, -2.45, 28.5);
    public static final float PUP_YAW = 180f, BUUR_YAW = 90f, KAPITEIN_YAW = 170f, BOOT_YAW = 0f;
    /** How high over the keel you stand in the boat, and where its two places are (along the boat: + = the bow). */
    public static final double BOOT_DEK = 0.4375, BOOT_BOEG = 0.9, BOOT_ROER = -1.3;
    /** The sea's surface, in blocks above the deck (negative: below). */
    public static final double WATER = -2.125;

    private Steiger() {
    }

    /** A template position (y above the deck) as a cutscene position: relative to the anchor block {@link #VOET}. */
    public static Vec3 punt(double x, double y, double z) {
        return new Vec3(x - VOET.getX(), y, z - VOET.getZ());
    }

    public static Vec3 punt(Vec3 template) {
        return punt(template.x, template.y, template.z);
    }

    /** A copy in the world: the anchor block of the scenes (the first plank of the pier, where you stand), its turn, its box. */
    public record Oord(BlockPos anker, Rotation draai, BoundingBox doos) {
        /** A template position (y above the deck) in the world. */
        public Vec3 wereld(Vec3 template) {
            return Cutscene.wereld(anker, draai, punt(template));
        }

        public Vec3 wereld(double x, double y, double z) {
            return Cutscene.wereld(anker, draai, punt(x, y, z));
        }

        public float yaw(float template) {
            return Cutscene.wereldYaw(draai, template);
        }

        /** On the plot or the pier (the build's own box, a little wider and from the water up to its roof)? */
        public boolean op(Vec3 plek) {
            return plek.x >= doos.minX() - 1 && plek.x <= doos.maxX() + 2 && plek.z >= doos.minZ() - 1 && plek.z <= doos.maxZ() + 2
                    && plek.y >= anker.getY() - 4 && plek.y <= anker.getY() + 12;
        }
    }

    /** Does the dock's story play in this level (the Guhmensie; the game tests: wherever a test copy stands)? */
    public static boolean inWereld(ServerLevel level) {
        return level.dimension() == ModDimensions.GUHMENSION || SteigerVerhaal.OVERAL;
    }

    /** The copy at this spot (a piece within reach of {@link Bezetting#start}), or null. */
    @Nullable
    public static Oord oord(ServerLevel level, BlockPos bij) {
        if (!inWereld(level)) {
            return null;
        }
        StructureStart start = Bezetting.start(level, STRUCTUUR, bij);
        if (start == null || start.getPieces().isEmpty()) {
            return null;
        }
        BlockPos anker = Kopieen.wereld(start, null, VOET);
        if (anker == null) {
            return null;
        }
        return new Oord(anker, Kopieen.draai(start, null), start.getPieces().get(0).getBoundingBox());
    }

    /** The copy this player stands on (null: none). */
    @Nullable
    public static Oord oordVan(ServerPlayer p) {
        Oord o = oord(p.level(), p.blockPosition());
        return o != null && o.op(p.position()) ? o : null;
    }
}
