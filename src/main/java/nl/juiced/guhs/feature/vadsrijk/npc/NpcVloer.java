package nl.juiced.guhs.feature.vadsrijk.npc;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.10.3 (npc-vloer, "in zone 2 I see NPCs in the floor and under the ground"): ONE floor rule for everything that puts a guh,
 * a nep-speler or an escort copy somewhere in the Vadsrijk.
 * <ul>
 *   <li>{@link #staanPlek}: where an NPC of a spot stands (the exact spot, the free floor beside it, the column search). "Lit" is
 *   now the time-INDEPENDENT raw light (sky light without the night darkening, or block light): the map's hollowed terrain has
 *   sealed pitch-dark pockets, and the old test ({@code getMaxLocalRawBrightness}, darkened at night) also called every roofed
 *   floor "dark" after sunset, so at night the search failed and the NPC was put INSIDE its blocked spot (hovering, no gravity).
 *   When nothing lit is found and the spot is blocked, it now goes UP to the first free floor (never inside solid blocks).</li>
 *   <li>{@link #vast} / {@link #bevrijd}: an entity whose middle is inside blocks goes up to the first free floor above it.</li>
 *   <li>{@link #onderDeKaart}: the map is a shell over an empty void down to the bedrock (y 0); whatever stands on that bedrock
 *   fell through a gap and is "under the ground".</li>
 *   <li>{@link #vrijBij}: a free spot next to a player for followers that teleport along (was: a random offset at the player's
 *   y, which on a slope is inside the hill).</li>
 * </ul>
 * The search works on a {@link Wereld} so the game test can run the very same code on the map tiles.
 */
public final class NpcVloer {
    /** How far up {@link #bevrijd} / the blocked-spot fallback look for a free floor. */
    public static final int OMHOOG = 24;

    private NpcVloer() {
    }

    /** The blocks the search reads: the live level, or the map tiles in the game test. */
    public interface Wereld {
        /** The block here has a collision shape. */
        boolean botst(BlockPos p);

        /** Not a sealed dark pocket (raw light > 0, independent of the time of day). */
        boolean licht(BlockPos p);
    }

    public static Wereld van(ServerLevel sl) {
        return new Wereld() {
            @Override
            public boolean botst(BlockPos p) {
                return !sl.getBlockState(p).getCollisionShape(sl, p).isEmpty();
            }

            @Override
            public boolean licht(BlockPos p) {
                return sl.getRawBrightness(p, 0) > 0;
            }
        };
    }

    /** Solid floor below, feet and head free. */
    public static boolean staan(Wereld w, BlockPos b) {
        return w.botst(b.below()) && !w.botst(b) && !w.botst(b.above());
    }

    /** Feet or head inside a block. */
    public static boolean geblokt(Wereld w, BlockPos b) {
        return w.botst(b) || w.botst(b.above());
    }

    /**
     * Where an NPC of this spot stands. The exact spot first; when that cell is blocked (counter, wall, ground) the nearest free
     * LIT floor at the same y within 2 (ring 1, then ring 2: inside the stall/house, never the dark pocket under it); then the
     * column: down 1..4 first, then the nearest lit floor up/down within 12; then (blocked spot only) the first free floor above
     * it within {@link #OMHOOG}. Without any of those the spot itself (an open spot without a floor: the caller lets it hover).
     */
    public static BlockPos staanPlek(Wereld w, BlockPos pos) {
        if (staan(w, pos)) {
            return pos;
        }
        boolean geblokt = geblokt(w, pos);
        for (int r = 1; geblokt && r <= 2; r++) {
            BlockPos beste = null;
            double bd = Double.MAX_VALUE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos b = pos.offset(dx, 0, dz);
                    double dd = dx * dx + dz * dz;
                    if (dd < bd && staan(w, b) && w.licht(b)) {
                        beste = b;
                        bd = dd;
                    }
                }
            }
            if (beste != null) {
                return beste;
            }
        }
        BlockPos k = kolom(w, pos);
        if (k != null) {
            return k;
        }
        if (geblokt) {
            BlockPos op = omhoog(w, pos, OMHOOG);
            if (op != null) {
                return op;
            }
        }
        return pos;
    }

    /** The column search of {@link #staanPlek} (exact spot, down 1..4 lit, up/down 12 lit), or null. */
    @Nullable
    public static BlockPos kolom(Wereld w, BlockPos pos) {
        if (staan(w, pos)) {
            return pos;
        }
        for (int d = 1; d <= 4; d++) {   // (fix round 2: down first, the floor of the room the spot is in)
            BlockPos b = pos.below(d);
            if (staan(w, b) && w.licht(b)) {
                return b;
            }
        }
        for (int d = 1; d <= 12; d++) {
            for (int s : new int[] {-1, 1}) {
                if (s < 0 && d <= 4) {
                    continue;
                }
                BlockPos b = pos.above(s * d);
                if (staan(w, b) && w.licht(b)) {
                    return b;
                }
            }
        }
        return null;
    }

    /** The first free floor at or above pos within max blocks (light does not matter: out of the rock first), or null. */
    @Nullable
    public static BlockPos omhoog(Wereld w, BlockPos pos, int max) {
        for (int d = 0; d <= max; d++) {
            BlockPos b = pos.above(d);
            if (staan(w, b)) {
                return b;
            }
        }
        return null;
    }

    // ---- entities ---------------------------------------------------------------------------------------------------------------

    /** Is the MIDDLE of this entity inside blocks (a thin column through its centre, so a wall beside a wide guh does not count)? */
    public static boolean vast(Entity e) {
        double h = Math.max(0.3, e.getBbHeight());
        AABB kern = new AABB(e.getX() - 0.15, e.getY() + 0.05, e.getZ() - 0.15, e.getX() + 0.15, e.getY() + h - 0.05, e.getZ() + 0.15);
        for (VoxelShape s : e.level().getBlockCollisions(null, kern)) {
            if (!s.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Standing on the bedrock under the map (the void below the terrain shell): fell through a gap. */
    public static boolean onderDeKaart(Entity e) {
        return e.getY() < e.level().getMinY() + 3;
    }

    /** An entity stuck inside blocks goes up to the first free floor within {@link #OMHOOG} above it. True = moved. */
    public static boolean bevrijd(Entity e) {
        if (!(e.level() instanceof ServerLevel sl) || !vast(e)) {
            return false;
        }
        BlockPos op = omhoog(van(sl), e.blockPosition(), OMHOOG);
        if (op == null) {
            return false;
        }
        e.snapTo(op.getX() + 0.5, op.getY(), op.getZ() + 0.5, e.getYRot(), e.getXRot());
        e.setDeltaMovement(Vec3.ZERO);
        e.resetFallDistance();
        return true;
    }

    /**
     * Where a follower that teleports along goes: {@code voorkeur} (a spot beside the player) when a guh can stand there (at that
     * cell or one up/down: slopes), else the player's own position (always free: the player stands there).
     */
    public static Vec3 vrijBij(ServerLevel sl, Vec3 speler, Vec3 voorkeur) {
        Wereld w = van(sl);
        BlockPos b = BlockPos.containing(voorkeur);
        for (int dy : new int[] {0, 1, -1}) {
            BlockPos c = b.above(dy);
            if (staan(w, c)) {
                return new Vec3(voorkeur.x, c.getY(), voorkeur.z);
            }
        }
        return speler;
    }
}
