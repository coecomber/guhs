package nl.juiced.guhs.feature.bedden;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.piep.PieppiepmuisjeEntity;
import nl.juiced.guhs.feature.piep.PoepschillyEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The little sleeping places ("vakjes") of the critter and family beds: which critter sleeps where. A bed is one block (the
 * luciferdoosje, the schelpbedje, the mandje's Blub place) or a multi-block {@link FamilieBedBlock}; its vakjes are given in the
 * bed's own frame (blocks, x to the right, z from the headboard to the front, drawn facing north) and turned with its FACING
 * around its anchor (the front-left block, where it was placed). Who takes a vakje is kept in the sleeper's persistent data
 * ({@link #VAKJE}), so two critters never share one.
 */
public final class Vakjes {
    /** Who may sleep in a vakje. Schilly and Poepschilly share theirs (a wet corner or a shell). */
    public enum Soort { GUH, BLUB, MUIS, SCHILLY }

    /** A vakje: where the sleeper lies in the bed's frame (blocks; y = its feet), and for whom. */
    public record Vakje(double u, double v, double w, Soort soort) {
    }

    /** A bed's size in blocks (x, y, z in its frame) and its vakjes. */
    public record Def(int sx, int sy, int sz, List<Vakje> vakjes) {
    }

    /** A found vakje: its key (anchor + index) and where in the world the sleeper lies. */
    public record Plek(String sleutel, Vec3 pos, float yaw) {
    }

    /** Persistent data: the key of the vakje this critter sleeps in (or walks to). */
    public static final String VAKJE = "guhs_bedden_vakje";

    private static final Map<Block, Def> DEFS = new HashMap<>();

    private Vakjes() {
    }

    public static void zet(Block block, Def def) {
        DEFS.put(block, def);
    }

    public static Def def(Block block) {
        return DEFS.get(block);
    }

    /** Which kind of sleeper is this (null: none of ours, or a guh-like that isn't a plain guh)? */
    public static Soort soort(Entity e) {
        if (e instanceof BlubGuhEntity) {
            return Soort.BLUB;
        }
        if (e instanceof PieppiepmuisjeEntity) {
            return Soort.MUIS;
        }
        if (e instanceof PoepschillyEntity) {
            return Soort.SCHILLY;
        }
        if (e instanceof GuhEntity && e.getType() == ModEntities.GUH.get()) {
            return Soort.GUH;
        }
        return null;
    }

    /** Turns a horizontal offset (a: to the right, b: to the front) from the north frame to the bed's facing. */
    static double[] draai(Direction f, double a, double b) {
        return switch (f) {
            case EAST -> new double[] {-b, a};
            case SOUTH -> new double[] {-a, -b};
            case WEST -> new double[] {b, -a};
            default -> new double[] {a, b};
        };
    }

    /** The world block of the bed's cell (lx, ly, lz) when its anchor (cell 0, 0, sz-1) is at {@code anker}. */
    public static BlockPos cel(BlockPos anker, Direction f, int sz, int lx, int ly, int lz) {
        double[] d = draai(f, lx, lz - (sz - 1));
        return anker.offset((int) Math.round(d[0]), ly, (int) Math.round(d[1]));
    }

    /** The world position of a point (u, v, w) in the bed's frame. */
    public static Vec3 punt(BlockPos anker, Direction f, int sz, double u, double v, double w) {
        int lx = (int) Math.floor(u), lz = (int) Math.floor(w);
        BlockPos c = cel(anker, f, sz, lx, 0, lz);
        double[] o = draai(f, u - lx - 0.5, w - lz - 0.5);
        return new Vec3(c.getX() + 0.5 + o[0], anker.getY() + v, c.getZ() + 0.5 + o[1]);
    }

    /** The anchor of the bed this block belongs to (itself for a one-block bed), or null if it isn't the anchor's cell. */
    static BlockPos anker(BlockState s, BlockPos pos, Def def, boolean alleenAnker) {
        if (s.getBlock() instanceof FamilieBedBlock fb) {
            int[] l = fb.lokaal(s);
            if (alleenAnker && !(l[0] == 0 && l[1] == 0 && l[2] == def.sz() - 1)) {
                return null;
            }
            return fb.anker(s, pos);
        }
        return pos;
    }

    /** The nearest free vakje for this critter within {@code bereik} blocks. */
    public static Optional<Plek> zoek(ServerLevel level, Mob mob, Soort soort, int bereik) {
        BlockPos hier = mob.blockPosition();
        Plek best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(hier.offset(-bereik, -3, -bereik), hier.offset(bereik, 3, bereik))) {
            BlockState s = level.getBlockState(p);
            Def def = DEFS.get(s.getBlock());
            if (def == null) {
                continue;
            }
            BlockPos anker = anker(s, p, def, true);
            if (anker == null) {
                continue;
            }
            Direction f = s.getValue(HorizontalDirectionalBlock.FACING);
            for (int i = 0; i < def.vakjes().size(); i++) {
                Vakje v = def.vakjes().get(i);
                if (v.soort() != soort) {
                    continue;
                }
                String sleutel = anker.asLong() + ":" + i;
                Vec3 pos = punt(anker, f, def.sz(), v.u(), v.v(), v.w());
                double d = pos.distanceToSqr(mob.position());
                if (d < bestD && !bezet(level, mob, sleutel, pos)) {
                    best = new Plek(sleutel, pos, f.toYRot());
                    bestD = d;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** Is the vakje taken (another critter sleeps in it, or walks to it)? */
    static boolean bezet(ServerLevel level, Mob mob, String sleutel, Vec3 pos) {
        return !level.getEntitiesOfClass(Mob.class, new AABB(pos, pos).inflate(24),
                m -> m != mob && sleutel.equals(m.getPersistentData().getStringOr(VAKJE, ""))).isEmpty();
    }

    /** Is the block at the vakje's key still a bed of ours (the bed may have been broken)? */
    static boolean bestaat(ServerLevel level, String sleutel) {
        int dp = sleutel.indexOf(':');
        if (dp < 0) {
            return false;
        }
        BlockPos anker = BlockPos.of(Long.parseLong(sleutel.substring(0, dp)));
        return DEFS.containsKey(level.getBlockState(anker).getBlock());
    }
}
