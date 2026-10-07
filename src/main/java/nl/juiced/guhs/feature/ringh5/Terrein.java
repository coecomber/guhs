package nl.juiced.guhs.feature.ringh5;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * bbq2 (ring-h5): one copy of the Zwarte Roosterpoort in a world: where the block (0, 0, 0) of the whole build is and how the
 * copy is turned. Everything of chapter 5 works in the coordinates of the build ({@link Plekken}) and asks a Terrein where
 * that is in the world ({@link #wereld}, {@link #midden}, {@link #punt}) or the other way round ({@link #lokaal}): so the
 * same code runs on every turned copy, on the hand-made copy of the dev command and on the little stand-ins of the game
 * tests ({@link #test}).
 */
public record Terrein(ResourceKey<Level> dim, BlockPos nul, Rotation draai) {
    /** How far around a spot a copy is looked for (blocks, per axis). */
    public static final int ZOEK = 20;

    /** (not saved) the copies found so far: dimension + start chunk -> its Terrein. */
    private static final Map<String, Terrein> GEVONDEN = new ConcurrentHashMap<>();
    /** (tests, the dev command) stand-ins that count as copies. */
    private static final List<Terrein> TEST = new CopyOnWriteArrayList<>();

    /** The world block of a block of the build. */
    public BlockPos wereld(BlockPos lokaal) {
        return nul.offset(StructureTemplate.transform(lokaal, Mirror.NONE, draai, BlockPos.ZERO));
    }

    /** The world position of a point of the build (a point, not a block: (x + 0.5, y, z + 0.5) is the middle of a block's floor). */
    public Vec3 punt(Vec3 lokaal) {
        double dx = lokaal.x - 0.5, dz = lokaal.z - 0.5;
        double rx, rz;
        switch (draai) {
            case CLOCKWISE_90 -> {
                rx = -dz;
                rz = dx;
            }
            case CLOCKWISE_180 -> {
                rx = -dx;
                rz = -dz;
            }
            case COUNTERCLOCKWISE_90 -> {
                rx = dz;
                rz = -dx;
            }
            default -> {
                rx = dx;
                rz = dz;
            }
        }
        return new Vec3(nul.getX() + rx + 0.5, nul.getY() + lokaal.y, nul.getZ() + rz + 0.5);
    }

    /** Where you stand in a cell of the build: the middle of its floor, in the world. */
    public Vec3 midden(BlockPos lokaal) {
        return punt(new Vec3(lokaal.getX() + 0.5, lokaal.getY(), lokaal.getZ() + 0.5));
    }

    /** The point of the build a world position lies at. */
    public Vec3 lokaal(Vec3 wereld) {
        double dx = wereld.x - nul.getX() - 0.5, dz = wereld.z - nul.getZ() - 0.5;
        double rx, rz;
        switch (draai) {
            case CLOCKWISE_90 -> {
                rx = dz;
                rz = -dx;
            }
            case CLOCKWISE_180 -> {
                rx = -dx;
                rz = -dz;
            }
            case COUNTERCLOCKWISE_90 -> {
                rx = -dz;
                rz = dx;
            }
            default -> {
                rx = dx;
                rz = dz;
            }
        }
        return new Vec3(rx + 0.5, wereld.y - nul.getY(), rz + 0.5);
    }

    /** Is this world position inside a box of the build (two corners, both inclusive; see {@link Plekken})? */
    public boolean in(List<BlockPos> doos, Vec3 wereld) {
        Vec3 l = lokaal(wereld);
        BlockPos a = doos.get(0), b = doos.get(1);
        return l.x >= a.getX() && l.x < b.getX() + 1 && l.y >= a.getY() - 0.01 && l.y < b.getY() + 1 && l.z >= a.getZ() && l.z < b.getZ() + 1;
    }

    /** A box of the build as a box in the world. */
    public AABB doos(List<BlockPos> doos) {
        return AABB.encapsulatingFullBlocks(wereld(doos.get(0)), wereld(doos.get(1)));
    }

    /** A direction of the build (a yaw: 0 = +z, 90 = -x) in the world. */
    public float yaw(float lokaal) {
        return switch (draai) {
            case CLOCKWISE_90 -> lokaal + 90f;
            case CLOCKWISE_180 -> lokaal + 180f;
            case COUNTERCLOCKWISE_90 -> lokaal - 90f;
            default -> lokaal;
        };
    }

    /** The world positions (where you stand) of a row of cells of the build. */
    public List<Vec3> route(List<BlockPos> cellen) {
        return cellen.stream().map(this::midden).toList();
    }

    public boolean isIn(Level level) {
        return level.dimension() == dim;
    }

    /** Does this world position lie in this copy (its whole box, a little wider)? */
    public boolean bevat(Vec3 wereld) {
        Vec3 l = lokaal(wereld);
        Vec3i m = Plekken.MAAT;
        return l.x >= -8 && l.x <= m.getX() + 8 && l.z >= -8 && l.z <= m.getZ() + 8 && l.y >= -8 && l.y <= m.getY() + 8;
    }

    // --- finding the copy --------------------------------------------------------------------------------------------------

    /** The Terrein of a real copy (a structure start of guhs:zwarte_roosterpoort); null when it has no pieces. */
    @Nullable
    public static Terrein van(ServerLevel level, StructureStart start) {
        BlockPos nul = Kopieen.wereld(start, null, BlockPos.ZERO);
        return nul == null ? null : new Terrein(level.dimension(), nul, Kopieen.draai(start, null));
    }

    /** The copy this spot lies in or next to (loaded chunks only); null: none here. */
    @Nullable
    public static Terrein bij(ServerLevel level, BlockPos pos) {
        Vec3 hier = Vec3.atCenterOf(pos);
        for (Terrein t : TEST) {
            if (t.isIn(level) && t.bevat(hier)) {
                return t;
            }
        }
        for (Terrein t : GEVONDEN.values()) {
            if (t.isIn(level) && t.bevat(hier)) {
                return t;
            }
        }
        Structure structuur = Kopieen.structuur(level, RingH5Feature.STRUCTUUR);
        if (structuur == null) {
            return null;
        }
        for (StructureStart start : Kopieen.bij(level, structuur, pos, ZOEK)) {
            Terrein t = van(level, start);
            if (t != null) {
                GEVONDEN.put(level.dimension().identifier() + "|" + start.getChunkPos().pack(), t);
                if (t.bevat(hier)) {
                    return t;
                }
            }
        }
        return null;
    }

    /** Does this spot lie in a copy that was found already (no looking: for what is asked very often)? */
    public static boolean kent(Level level, Vec3 plek) {
        for (Terrein t : TEST) {
            if (t.isIn(level) && t.bevat(plek)) {
                return true;
            }
        }
        for (Terrein t : GEVONDEN.values()) {
            if (t.isIn(level) && t.bevat(plek)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The nearest copy with a piece within {@code afstand} blocks of this spot (loaded chunks only), whether or not the
     * spot lies in it: for the op commands.
     */
    @Nullable
    public static Terrein zoek(ServerLevel level, BlockPos pos, int afstand) {
        Terrein hier = bij(level, pos);
        if (hier != null) {
            return hier;
        }
        Vec3 plek = Vec3.atCenterOf(pos);
        List<Terrein> kandidaten = new java.util.ArrayList<>(TEST);
        Structure structuur = Kopieen.structuur(level, RingH5Feature.STRUCTUUR);
        if (structuur != null) {
            for (StructureStart start : Kopieen.bij(level, structuur, pos, afstand)) {
                Terrein t = van(level, start);
                if (t != null) {
                    GEVONDEN.put(level.dimension().identifier() + "|" + start.getChunkPos().pack(), t);
                    kandidaten.add(t);
                }
            }
        }
        Terrein beste = null;
        double dichtst = (afstand + 96.0) * (afstand + 96.0);
        for (Terrein t : kandidaten) {
            double d = t.midden(Plekken.ANKER).distanceToSqr(plek);
            if (t.isIn(level) && d < dichtst) {
                dichtst = d;
                beste = t;
            }
        }
        return beste;
    }

    /** (Tests, the dev command) this Terrein counts as a copy from now on. */
    public static void test(Terrein t) {
        TEST.add(t);
    }

    /** (Tests) forget the stand-ins. */
    public static void testWissen() {
        TEST.clear();
    }

    static void wisAlles() {
        GEVONDEN.clear();
        TEST.clear();
    }
}
