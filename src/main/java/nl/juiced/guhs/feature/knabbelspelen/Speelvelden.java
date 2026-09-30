package nl.juiced.guhs.feature.knabbelspelen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.doolhof.Anker;
import nl.juiced.guhs.feature.doolhof.DoolhofBlocks;

/**
 * Where everything of De Knabbelspelen is in its template (tools/features/knabbelspelen_bouw.py uses the same numbers):
 * the circus tent in the middle with Juf Vahoegsakee (the anchor under her), three play fields north of it and three
 * south of it, each {@value #VELD_B} x {@value #VELD_D} with {@value #BANEN} lanes of 5 blocks between low fences. A
 * lane is walked "along" (u: 0 at the start line near the tent, growing away from it) and "sideways" (s: -2..2, +s is
 * the template's east).
 */
public final class Speelvelden {
    public static final int G = 4, W = 96;
    /** The anchor block under Juf Vahoegsakee (template coordinates, facing north in the template). */
    public static final int AX = 47, AY = G - 1, AZ = 48;
    public static final int BANEN = 4, VELD_B = 28, VELD_D = 26;
    static final int ZOEK = 4;

    /** The field of an event: its corner and whether it lies north of the tent (lanes run north) or south. */
    public record Veld(int x0, int z0, boolean noord) {
        /** The template x of lane k's middle block. */
        public int baanX(int k) {
            return x0 + 5 + 6 * k;
        }

        /** The template z of the start line's blocks (u = 0). */
        public int startZ() {
            return noord ? z0 + VELD_D - 2 : z0 + 2;
        }

        public int richting() {
            return noord ? -1 : 1;
        }
    }

    public static Veld veld(Onderdeel o) {
        return switch (o) {
            case KNABBELHAPPEN -> new Veld(3, 3, true);
            case ZAKLOPEN -> new Veld(34, 3, true);
            case BLIKGOOIEN -> new Veld(65, 3, true);
            case EIERLOPEN -> new Veld(3, 67, false);
            case SPIJKERPOEPEN -> new Veld(34, 67, false);
            case GUHGUHTJE_PRIK -> new Veld(65, 67, false);
        };
    }

    // --- lanes ---------------------------------------------------------------------------------------------------------

    /** The block of lane k at (u, s) and height y (template). */
    public static BlockPos blok(Anker a, Onderdeel o, int k, int u, int s, int y) {
        Veld v = veld(o);
        return a.blok(v.baanX(k) + s, y, v.startZ() + v.richting() * u);
    }

    /** A spot of lane k: u along (0 = middle of the start blocks), s sideways, y height (template). */
    public static Vec3 punt(Anker a, Onderdeel o, int k, double u, double s, double y) {
        Veld v = veld(o);
        return a.punt(v.baanX(k) + 0.5 + s, y, v.startZ() + 0.5 + v.richting() * u);
    }

    /** The world yaw of looking along the lane (away from the tent). */
    public static float yaw(Anker a, Onderdeel o) {
        return a.yaw(veld(o).noord ? 180f : 0f);
    }

    /** A world spot as {u, s, y} of lane k. */
    public static double[] baan(Anker a, Onderdeel o, int k, Vec3 world) {
        Veld v = veld(o);
        Vec3 l = a.lokaal(world);
        return new double[] {(l.z - v.startZ() - 0.5) * v.richting(), l.x - v.baanX(k) - 0.5, l.y};
    }

    /** Is this world spot inside the template's square (the whole Knabbelspelen ground)? */
    public static boolean opTerrein(Anker a, Vec3 world, double marge) {
        Vec3 l = a.lokaal(world);
        return l.x >= -marge && l.z >= -marge && l.x <= W + marge && l.z <= W + marge;
    }

    /** Where the scoreboard of an event floats (over its field's entrance, near the tent). */
    public static Vec3 bord(Anker a, Onderdeel o) {
        Veld v = veld(o);
        return a.punt(v.x0 + VELD_B / 2.0, G + 6.5, v.noord ? v.z0 + VELD_D + 0.5 : v.z0 - 0.5);
    }

    // --- the events' fixed spots (u, s) -----------------------------------------------------------------------------------

    /** Knabbelhappen: you stand on your mat here; the beam with the strings hangs over u = HAP_BALK at HAP_Y. */
    public static final int HAP_MAT = 1, HAP_BALK = 4, HAP_Y = G + 7;
    /** Zaklopen: humps at these u, the finish line at ZAK_FINISH. */
    public static final int[] ZAK_HOBBELS = {5, 10, 15};
    public static final int ZAK_FINISH = 20;
    /** Blikgooien: the throwing mat and the table (tins on it, 3-2-1). */
    public static final int BLIK_MAT = 1, BLIK_TAFEL = 9;
    /** Eierlopen: the flags (u) and on which side they stand (s sign), the finish. */
    public static final int[] EI_VLAGGEN = {4, 8, 12, 16};
    public static final int[] EI_KANT = {1, -1, 1, -1};
    public static final int EI_FINISH = 20;
    /** Spijkerpoepen: the three kaasmelk bottles (u, in the floor at height G). */
    public static final int[] FLES_U = {5, 10, 15};
    /** Guhguhtje prik: the mat, the board (u, from s -2..2 and G+1..G+5), the tail spot on it. */
    public static final int PRIK_MAT = 1, PRIK_BORD = 10;
    public static final double PRIK_DOEL_Y = G + 2.5;

    // --- the anchor ------------------------------------------------------------------------------------------------------

    /** The Knabbelspelen of this Juf Vahoegsakee (remembered in her roleData, else found under her); null: none. */
    @Nullable
    public static Anker anker(GuhNpcEntity npc) {
        if (npc.roleData.contains("SpelenAnker")) {
            return Anker.load(npc.roleData.getCompound("SpelenAnker"), AX, AY, AZ);
        }
        Anker a = zoek(npc.level(), npc.blockPosition());
        if (a != null) {
            npc.roleData.put("SpelenAnker", a.save());
        }
        return a;
    }

    @Nullable
    public static Anker zoek(Level level, BlockPos rond) {
        for (BlockPos p : BlockPos.betweenClosed(rond.offset(-ZOEK, -ZOEK, -ZOEK), rond.offset(ZOEK, 1, ZOEK))) {
            BlockState s = level.getBlockState(p);
            if (s.is(KnabbelspelenFeature.ANKER.get())) {
                return Anker.van(p, s.getValue(DoolhofBlocks.AnkerBlock.FACING), AX, AY, AZ);
            }
        }
        return null;
    }

    private Speelvelden() {
    }
}
