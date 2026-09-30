package nl.juiced.guhs.feature.guhwaiispellen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;

/**
 * 3.0: the surf beach of Guhwai'i (guhwaii-spellen): where the waves are. {@link #open} (for Lilo-guh's hint lines at home,
 * CONTRACT_30 §4.10) says whether a surf beach is near; {@link #zoek} finds, around the surf Lilo-guh, the way to the open
 * water: the direction with the longest stretch of water from the beach out (the waves roll in along it), and its first
 * water block at the beach (the {@link Spot}'s origin). The template is placed unrotated, so this is found in the world.
 */
public final class Surfplek {
    public static final ResourceKey<Structure> SURFSTRAND = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guhwaii_surfstrand"));
    /** How far a surf beach may be for {@link #open}. */
    public static final int OPEN_AFSTAND = 300;
    /** The water must start within this distance of Lilo-guh and run at least MIN_RUN blocks out to sea. */
    public static final int ZOEK_START = 32, ZOEK_TOT = 110, MIN_RUN = 44, RICHTINGEN = 48;

    /**
     * A surf spot: the first water block at the beach (its top: the water surface is at y + {@link #WATER}) and the
     * direction out to sea (radians: 0 = +x, pi/2 = +z). u = blocks out to sea, v = blocks along the beach (+v = the sea
     * direction turned a quarter to the left, seen from above).
     */
    public record Spot(BlockPos origin, double hoek) {
        /** The world spot of (u, v) at this height above the calm water. */
        public Vec3 wereld(double u, double v, double hoogte) {
            double c = Math.cos(hoek), s = Math.sin(hoek);
            return new Vec3(origin.getX() + 0.5 + c * u - s * v, origin.getY() + WATER + hoogte, origin.getZ() + 0.5 + s * u + c * v);
        }

        /** The Minecraft yaw of a direction (x, z). */
        public static float yaw(double dx, double dz) {
            return (float) Math.toDegrees(Math.atan2(-dx, dz));
        }

        /** The yaw of the board of this ride: along the wave while riding (plus the spin in the air), to the beach otherwise. */
        public float yaw(SurfSim sim) {
            double c = Math.cos(hoek), s = Math.sin(hoek);
            float y = switch (sim.fase()) {
                case RIJDEN, LUCHT -> yaw(-s * sim.dir(), c * sim.dir());
                default -> yaw(-c, -s);
            };
            return sim.fase() == SurfSim.Fase.LUCHT ? y - (float) sim.spin() : y;
        }

        /** The yaw looking at the beach (the surfer's view). */
        public float strandYaw() {
            return yaw(-Math.cos(hoek), -Math.sin(hoek));
        }
    }

    /** The water surface lies this high above the top water block's floor. */
    public static final double WATER = 0.88;

    /** Is there a surf beach (guhwaii_surfstrand) within 300 blocks of p? (for a hint line) */
    public static boolean open(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) {
            return false;
        }
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(SURFSTRAND);
        if (holder.isEmpty()) {
            return false;
        }
        try {
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder.get()), p.blockPosition(), 2, false);
            return found != null && found.getFirst().distSqr(p.blockPosition().atY(found.getFirst().getY())) <= (double) OPEN_AFSTAND * OPEN_AFSTAND;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The surf spot around this place (the surf Lilo-guh), or null when there's no open water near. */
    @Nullable
    public static Spot zoek(Level level, BlockPos bij) {
        return zoek(level, bij, ZOEK_START, MIN_RUN);
    }

    /** (Also for the tests, with a smaller pool.) */
    @Nullable
    public static Spot zoek(Level level, BlockPos bij, int start, int minRun) {
        Spot best = null;
        int bestScore = 0;
        for (int i = 0; i < RICHTINGEN; i++) {
            double a = Math.PI * 2 * i / RICHTINGEN;
            double c = Math.cos(a), s = Math.sin(a);
            BlockPos eerste = null;
            int run = 0;
            for (int r = 1; r <= ZOEK_TOT; r++) {
                int x = bij.getX() + (int) Math.round(c * r), z = bij.getZ() + (int) Math.round(s * r);
                BlockPos w = water(level, x, z, bij.getY());
                if (w == null) {
                    if (eerste != null) {
                        break;
                    }
                    if (r > start) {
                        break;
                    }
                    continue;
                }
                if (eerste == null) {
                    eerste = w;
                }
                run++;
            }
            if (eerste == null || run < minRun) {
                continue;
            }
            // the width: water to both sides halfway out
            int breed = 0;
            for (int side = -1; side <= 1; side += 2) {
                for (int d = 1; d <= 20; d++) {
                    double u = Math.min(run, 30), v = side * d;
                    int x = eerste.getX() + (int) Math.round(c * u - s * v), z = eerste.getZ() + (int) Math.round(s * u + c * v);
                    if (water(level, x, z, bij.getY()) == null) {
                        break;
                    }
                    breed++;
                }
            }
            int score = Math.min(run, 80) * 2 + breed;
            if (score > bestScore) {
                bestScore = score;
                best = new Spot(eerste, a);
            }
        }
        return best;
    }

    /** The top water block of the column (x, z) near height y (the water must be open to the sky), or null. */
    @Nullable
    private static BlockPos water(Level level, int x, int z, int y) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y + 2, z);
        for (int k = 0; k < 16; k++) {
            var state = level.getBlockState(p);
            if (!state.isAir()) {
                return state.getFluidState().is(FluidTags.WATER) && state.getFluidState().isSource() ? p.immutable() : null;
            }
            p.move(0, -1, 0);
        }
        return null;
    }

    private Surfplek() {
    }
}
