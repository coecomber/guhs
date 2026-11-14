package nl.juiced.guhs.feature.vadsrijk.kern.mob;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * 1.10.1 (bug "in de druipgrotten spawnen super veel mobs net buiten de grot, net eronder of erboven"): the walkable space of a
 * cave spawner. The map's rock is hollowed (sealed, pitch-dark air pockets inside the hills), and a cave spawner used the first
 * standing spot of a column between its heights, so mobs appeared in those pockets just above/below the tunnel, or on the
 * hill outside. Now a cave spawner's stand spot must lie in the air that is CONNECTED to the spawner's centre (a flood fill
 * through free blocks, inside the spawner's cylinder + {@link #RAND} and its y-range), and for the spawners marked
 * {@link #ONDERDAK} also under a roof (no sky). Mobs of such a spawner that stand near it but outside that space (left over
 * from before) are removed so the spawner refills inside the cave.
 * <p>Falls back to the old rule (no check) when the centre is not in air or the space has too few standing spots (a misplaced
 * centre), so no spawner stops working because of this.
 */
public final class GrotRuimte {
    /** The space reaches this far past the spawn circle (leashed mobs stroll a bit; they must count as inside). */
    public static final int RAND = 10;
    /** ... and this far above/below its y-range (tunnels slope; a mob hanging from the ceiling is inside too). */
    public static final int RAND_Y = 8;
    /** Recomputed after this many ticks (blocks hardly change). */
    public static final long GELDIG = 20L * 300;
    /** Flood fill budget (cells). */
    public static final int MAX_CELLEN = 60000;
    /** Spawner id prefixes whose spots must be under a roof (real underground caves). */
    public static final Set<String> ONDERDAK = new CopyOnWriteArraySet<>();

    /** A computed space: the free cells + how many standing spots it has. */
    public record Ruimte(LongOpenHashSet cellen, int staan, long tot) {
        public boolean bevat(BlockPos b) {
            return cellen.contains(b.asLong());
        }
    }

    private static final Map<String, Ruimte> CACHE = new ConcurrentHashMap<>();
    /** Spawners whose centre gave no usable space (the old rule), until this game time. */
    private static final Map<String, Long> GEEN = new ConcurrentHashMap<>();

    private GrotRuimte() {
    }

    public static boolean onderdak(String spawnerId) {
        for (String p : ONDERDAK) {
            if (spawnerId.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /** Forget every computed space (tests, the dev command). */
    public static void vergeet() {
        CACHE.clear();
        GEEN.clear();
    }

    /** The connected space of this cave spawner, or null = no check (not a cave spawner, misplaced centre, chunks not loaded). */
    /** The last {@link #van} gave null because chunks around the spawner were not loaded yet (then: do not spawn now). */
    public static boolean ongeladen;

    @Nullable
    public static Ruimte van(ServerLevel sl, Spawners.Spawner s) {
        ongeladen = false;
        if (!s.grot()) {
            return null;
        }
        long nu = sl.getGameTime();
        // (spawners with the same shape share it: the camps of one knot)
        String k = s.centrum().asLong() + "/" + s.straal() + "/" + s.minY() + "/" + s.maxY() + "/" + s.max();
        Ruimte r = CACHE.get(k);
        if (r != null && nu < r.tot()) {
            return r;
        }
        Long geen = GEEN.get(k);
        if (geen != null && nu < geen) {
            return null;
        }
        Ruimte n = bereken(sl, s.centrum(), s.straal() + RAND, s.minY() - RAND_Y, s.maxY() + RAND_Y, nu);
        if (n == null) {
            ongeladen = true;
            return null;   // (chunks not loaded: try again next time, no cache)
        }
        if (n.staan() < Math.max(3, s.max())) {
            GEEN.put(k, nu + GELDIG);
            CACHE.remove(k);
            return null;
        }
        CACHE.put(k, n);
        return n;
    }

    /**
     * The free cells connected to the start (the centre, or the first free cell below/above it in its column) inside the
     * cylinder (flat radius {@code r} around the centre, {@code minY..maxY}). Null when a needed chunk is not loaded. An empty
     * space when the centre column has no free cell.
     */
    @Nullable
    public static Ruimte bereken(ServerLevel sl, BlockPos centrum, int r, int minY, int maxY, long nu) {
        BlockPos start = null;
        for (int d = 0; d <= Math.max(maxY - minY, 0) && start == null; d++) {
            for (int s : new int[] {-1, 1}) {
                BlockPos b = centrum.offset(0, s * d, 0);
                if (b.getY() < minY || b.getY() > maxY) {
                    continue;
                }
                if (!sl.isLoaded(b)) {
                    return null;
                }
                if (vrij(sl, b)) {
                    start = b;
                    break;
                }
            }
        }
        LongOpenHashSet cellen = new LongOpenHashSet();
        if (start == null) {
            return new Ruimte(cellen, 0, nu + GELDIG);
        }
        int staan = 0;
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        rij.add(start);
        cellen.add(start.asLong());
        long r2 = (long) r * r;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        while (!rij.isEmpty() && cellen.size() < MAX_CELLEN) {
            BlockPos c = rij.poll();
            m.set(c.getX(), c.getY() - 1, c.getZ());
            if (!vrij(sl, m) && vrij(sl, c.above()) && sl.getFluidState(c).isEmpty()) {
                staan++;
            }
            for (int[] o : new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}}) {
                BlockPos b = c.offset(o[0], o[1], o[2]);
                if (b.getY() < minY || b.getY() > maxY || cellen.contains(b.asLong())) {
                    continue;
                }
                long dx = b.getX() - centrum.getX(), dz = b.getZ() - centrum.getZ();
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                if (!sl.isLoaded(b)) {
                    return null;
                }
                if (vrij(sl, b)) {
                    cellen.add(b.asLong());
                    rij.add(b);
                }
            }
        }
        return new Ruimte(cellen, staan, nu + GELDIG);
    }

    static boolean vrij(ServerLevel sl, BlockPos b) {
        return sl.getBlockState(b).getCollisionShape(sl, b).isEmpty();
    }

    /** May a mob of this spawner stand at {@code plek} (stand cell)? True when there is no check for it. */
    public static boolean magStaan(ServerLevel sl, Spawners.Spawner s, @Nullable Ruimte r, BlockPos plek) {
        if (r != null && !r.bevat(plek)) {
            return false;
        }
        return !(r != null && onderdak(s.id()) && openHemel(sl, plek));
    }

    /** Nothing that blocks movement above this cell (the heightmap; not the sky light, which lags behind block changes). */
    public static boolean openHemel(ServerLevel sl, BlockPos plek) {
        return plek.getY() >= sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, plek.getX(), plek.getZ());
    }

    /**
     * A mob of this spawner stands near it (within the space's cylinder and y-range + 6) but not in the space: a left-over in a
     * pocket of the rock (or outside on the hill). The caller removes it when it is not fighting.
     */
    public static boolean verdwaald(Spawners.Spawner s, Ruimte r, BlockPos b) {
        long dx = b.getX() - s.centrum().getX(), dz = b.getZ() - s.centrum().getZ();
        int rr = s.straal() + RAND - 2;
        if (dx * dx + dz * dz > (long) rr * rr || b.getY() < s.minY() - 6 || b.getY() > s.maxY() + 6) {
            return false;   // (far from it: chasing someone, or somewhere else entirely)
        }
        return !r.bevat(b) && !r.bevat(b.above());
    }
}
