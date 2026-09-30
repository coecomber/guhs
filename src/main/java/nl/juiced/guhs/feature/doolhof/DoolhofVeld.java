package nl.juiced.guhs.feature.doolhof;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * The hedge field of Het Guhdoolhof in its template (tools/features/doolhof_bouw.py uses the same numbers): a square of
 * {@value #F} x {@value #F} blocks at ({@value #FX}, {@value #FZ}) whose lines every {@value #P} blocks are the hedge
 * walls (posts where they cross), with 2-wide paths in the cells between them; the lookout tower stands on the cells
 * {@link DoolhofKaart#TOREN_VAN}..{@link DoolhofKaart#TOREN_TOT}. The border (with the exit gate in the north and the
 * closed entrance under the bridge in the south) belongs to the template; everything inside is rebuilt for every game.
 */
public final class DoolhofVeld {
    /** Floor height, cell pitch, field size and corner (template coordinates). */
    public static final int G = 4, P = 3, F = DoolhofKaart.N * P + 1, FX = 19, FZ = 14;
    /** The anchor block under Meneer Vadskronkel (template coordinates; it faces north in the template). */
    public static final int AX = 36, AY = G - 1, AZ = 84;
    /** Hedges from here up to HEG_TOT (inclusive); the little lanterns stand on posts one higher. */
    public static final int HEG_VAN = G + 1, HEG_TOT = G + 4, LAMP_Y = G + 5;
    /** The tower's blocks (field coordinates a, b: inclusive). */
    public static final int TOREN_A = DoolhofKaart.TOREN_VAN * P, TOREN_B = (DoolhofKaart.TOREN_TOT + 1) * P;
    /** The bridge from the plaza to the tower runs over the posts a = 27 and 30 south of the tower. */
    public static final int BRUG_A1 = 27, BRUG_A2 = 30;
    /** How far around the NPC the anchor is searched. */
    static final int ZOEK = 4;

    private DoolhofVeld() {
    }

    // --- where is it -------------------------------------------------------------------------------------------------

    /** The maze of this Meneer Vadskronkel (remembered in his roleData, else found under him); null: none. */
    @Nullable
    public static Anker anker(GuhNpcEntity npc) {
        if (npc.roleData.contains("DoolhofAnker")) {
            return Anker.load(npc.roleData.getCompoundOrEmpty("DoolhofAnker"), AX, AY, AZ);
        }
        Anker a = zoek(npc.level(), npc.blockPosition());
        if (a != null) {
            npc.roleData.put("DoolhofAnker", a.save());
        }
        return a;
    }

    @Nullable
    public static Anker zoek(Level level, BlockPos rond) {
        for (BlockPos p : BlockPos.betweenClosed(rond.offset(-ZOEK, -ZOEK, -ZOEK), rond.offset(ZOEK, 1, ZOEK))) {
            BlockState s = level.getBlockState(p);
            if (s.is(DoolhofFeature.ANKER.get())) {
                return Anker.van(p, s.getValue(DoolhofBlocks.AnkerBlock.FACING), AX, AY, AZ);
            }
        }
        return null;
    }

    // --- cells ---------------------------------------------------------------------------------------------------------

    /** The middle of a cell (a spot between its 2x2 path blocks) at height y (template). */
    public static Vec3 cel(Anker a, int cx, int cz, double y) {
        return a.punt(FX + P * cx + 2.0, y, FZ + P * cz + 2.0);
    }

    /** The cell of a world spot, or null outside the maze's cells. */
    @Nullable
    public static int[] celVan(Anker a, Vec3 world) {
        Vec3 l = a.lokaal(world);
        int bx = (int) Math.floor(l.x) - FX, bz = (int) Math.floor(l.z) - FZ;
        if (bx < 0 || bz < 0 || bx >= F || bz >= F) {
            return null;
        }
        int cx = Math.min(DoolhofKaart.N - 1, bx / P), cz = Math.min(DoolhofKaart.N - 1, bz / P);
        return new int[] {cx, cz};
    }

    /** Is this spot beyond the exit gate (north of the field, in front of the gate)? */
    public static boolean bijUitgang(Anker a, Vec3 world) {
        Vec3 l = a.lokaal(world);
        double midden = FX + P * DoolhofKaart.MIDDEN + 2.0;
        return l.z < FZ - 0.2 && l.z > FZ - 6 && Math.abs(l.x - midden) < 3.5;
    }

    /** Is this spot on (or above) the hedge field? */
    public static boolean inVeld(Anker a, Vec3 world, double marge) {
        Vec3 l = a.lokaal(world);
        return l.x >= FX - marge && l.x <= FX + F + marge && l.z >= FZ - marge && l.z <= FZ + F + marge;
    }

    /** The field (plus the space above it) as a world box. */
    public static AABB veld(Anker a) {
        BlockPos p1 = a.blok(FX, G, FZ), p2 = a.blok(FX + F - 1, G + 8, FZ + F - 1);
        return new AABB(Vec3.atLowerCornerOf(p1), Vec3.atLowerCornerOf(p2)).expandTowards(1, 1, 1).inflate(0.5);
    }

    /** The template height of a world y. */
    public static double hoogte(Anker a, double y) {
        return a.lokaal(new Vec3(a.pos().getX(), y, a.pos().getZ())).y;
    }

    // --- the plan: which block goes where ------------------------------------------------------------------------------

    public record Blok(BlockPos pos, BlockState state) {
    }

    /**
     * Every block of the field's inside for this maze, row by row from north to south (so the game can grow the hedges
     * a few rows per tick). Border, tower and bridge stay as the template made them.
     */
    public static List<List<Blok>> plan(Anker a, DoolhofKaart k) {
        BlockState heg = DoolhofFeature.HEG.get().defaultBlockState();
        BlockState lucht = Blocks.AIR.defaultBlockState();
        BlockState lamp = DoolhofFeature.LANTAARN.get().defaultBlockState();
        List<List<Blok>> rijen = new ArrayList<>();
        for (int b = 1; b <= F - 2; b++) {
            List<Blok> rij = new ArrayList<>();
            for (int bx = 1; bx <= F - 2; bx++) {
                if (toren(bx, b)) {
                    continue;
                }
                boolean dicht = dicht(k, bx, b);
                for (int y = HEG_VAN; y <= HEG_TOT; y++) {
                    rij.add(new Blok(a.blok(FX + bx, y, FZ + b), dicht ? heg : lucht));
                }
                if (bx % P == 0 && b % P == 0 && !brugpaal(bx, b)) {
                    rij.add(new Blok(a.blok(FX + bx, LAMP_Y, FZ + b), lampje(bx, b) ? lamp : lucht));
                }
            }
            rijen.add(rij);
        }
        // the dead ends get a trimmed guh face in their back wall (so you know you've been there, and it's cute)
        for (int[] c : k.doodlopend) {
            Direction open = null;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (k.doorgang(c[0], c[1], d.getStepX(), d.getStepZ()) || (c[0] == k.uitX && c[1] == k.uitZ && d == Direction.NORTH)) {
                    open = d;
                }
            }
            if (open == null) {
                continue;
            }
            Direction achter = open.getOpposite();
            int bx = switch (achter) {
                case WEST -> P * c[0];
                case EAST -> P * c[0] + P;
                default -> P * c[0] + 1;
            };
            int bz = switch (achter) {
                case NORTH -> P * c[1];
                case SOUTH -> P * c[1] + P;
                default -> P * c[1] + 1;
            };
            if (bx <= 0 || bz <= 0 || bx >= F - 1 || bz >= F - 1 || toren(bx, bz)) {
                continue;
            }
            BlockState gezicht = DoolhofFeature.HEG_GEZICHT.get().defaultBlockState()
                    .setValue(DoolhofBlocks.HegGezicht.FACING, a.richting(open));
            int rij = bz - 1;
            rijen.get(rij).add(new Blok(a.blok(FX + bx, G + 2, FZ + bz), gezicht));
        }
        return rijen;
    }

    /** Is this block (field coordinates) hedge in this maze? */
    public static boolean dicht(DoolhofKaart k, int bx, int b) {
        boolean wa = bx % P == 0, wb = b % P == 0;
        if (wa && wb) {
            return true;                                          // a post: always hedge
        }
        int cx = bx / P, cz = b / P;
        if (!wa && !wb) {
            return !(k.actief[cx][cz] || k.gang[cx][cz]);         // a cell: open when it's in the maze (or the corridor)
        }
        if (wa) {                                                 // a wall between (cx - 1, cz) and (cx, cz)
            return !k.doorgang(cx - 1, cz, 1, 0);
        }
        // a wall between (cx, cz - 1) and (cx, cz)
        int zx = cx, noord = cz - 1;
        if (k.doorgang(zx, noord, 0, 1)) {
            return false;
        }
        boolean gangNoord = DoolhofKaart.binnen(zx, noord) && k.gang[zx][noord];
        boolean gangZuid = DoolhofKaart.binnen(zx, cz) && k.gang[zx][cz];
        boolean uitgang = zx == k.uitX && cz == k.uitZ;
        return !(gangNoord && (gangZuid || uitgang));
    }

    static boolean toren(int bx, int b) {
        return bx >= TOREN_A && bx <= TOREN_B && b >= TOREN_A && b <= TOREN_B;
    }

    /** The posts the bridge stands on (its pillars): no lantern there. */
    public static boolean brugpaal(int bx, int b) {
        return (bx == BRUG_A1 || bx == BRUG_A2) && b > TOREN_B;
    }

    /** Lanterns on some posts (the same ones in the template). */
    public static boolean lampje(int bx, int b) {
        int pa = bx / P, pb = b / P;
        return (pa * 7 + pb * 3) % 5 == 0;
    }
}
