package nl.juiced.guhs.feature.vadsrijk.kern.mob;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;

/**
 * Mob spawning in the dimension is code (no natural spawns): a {@link Spawner} keeps up to {@code max} of one mob type alive
 * in a circle, re-filling one every {@code respawnTicks} while a player is within 64 blocks. Elites roll with {@code eliteKans}
 * and get 1-2 random affixes.
 */
public final class Spawners {
    /**
     * A spawner. Open air (default): the ground from the top (heightmap) in a circle. Cave/interior mode ({@link #grot}): the
     * first standing spot searching down from {@code maxY} to {@code minY} (solid floor, 2 free blocks, no fluid), and every
     * distance check is flat (2D) within that y-range, so a cave spawner under a hill or a room inside a building works.
     */
    public record Spawner(String id, BlockPos centrum, int straal, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int max, int respawnTicks,
            int minLevel, int maxLevel, Element element, double eliteKans, boolean grot, int minY, int maxY) {
        /** Open air (the original 10-argument form). */
        public Spawner(String id, BlockPos centrum, int straal, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int max, int respawnTicks,
                int minLevel, int maxLevel, Element element, double eliteKans) {
            this(id, centrum, straal, type, max, respawnTicks, minLevel, maxLevel, element, eliteKans, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        /** The same spawner in cave/interior mode between these heights. */
        public Spawner grot(int minY, int maxY) {
            return new Spawner(id, centrum, straal, type, max, respawnTicks, minLevel, maxLevel, element, eliteKans, true, minY, maxY);
        }

        /** The box mobs of this spawner are counted in. */
        public AABB gebied() {
            int r = straal + 16;
            if (grot) {
                return new AABB(centrum.getX() - r, minY - 4, centrum.getZ() - r, centrum.getX() + r + 1, maxY + 4, centrum.getZ() + r + 1);
            }
            return new AABB(centrum).inflate(r, 32, r);
        }
    }

    private static final Map<String, Spawner> ALLE = new LinkedHashMap<>();
    private static final Map<String, Long> VOLGENDE = new LinkedHashMap<>();

    private Spawners() {
    }

    public static synchronized void registreer(Spawner s) {
        ALLE.put(s.id(), s);
    }

    @javax.annotation.Nullable
    public static Spawner van(String id) {
        return ALLE.get(id);
    }

    public static java.util.Collection<Spawner> alle() {
        return java.util.Collections.unmodifiableCollection(ALLE.values());
    }

    /** How many (not yet plofd) mobs of this spawner are alive in its area now. */
    public static int levend(ServerLevel sl, Spawner s) {
        return sl.getEntitiesOfClass(VadsrijkMob.class, s.gebied(), m -> s.id().equals(m.spawner) && !m.geplofd()).size();
    }

    /** Is a (non-spectator) player near: within 64 blocks, flat and inside the y-range for a cave spawner. */
    static boolean spelerInBuurt(ServerLevel sl, Spawner s) {
        if (!s.grot()) {
            return sl.getNearestPlayer(s.centrum().getX(), s.centrum().getY(), s.centrum().getZ(), 64, false) != null;
        }
        for (var p : sl.players()) {
            double dx = p.getX() - s.centrum().getX(), dz = p.getZ() - s.centrum().getZ();
            if (!p.isSpectator() && dx * dx + dz * dz <= 64 * 64 && p.getY() >= s.minY() - 8 && p.getY() <= s.maxY() + 8) {
                return true;
            }
        }
        return false;
    }

    /** Every second (kern). */
    public static void tick(ServerLevel sl) {
        long nu = sl.getGameTime();
        for (Spawner s : ALLE.values()) {
            if (!sl.isLoaded(s.centrum()) || !spelerInBuurt(sl, s)) {
                continue;
            }
            int levend = levend(sl, s);
            if (levend >= s.max()) {
                VOLGENDE.remove(s.id());
                continue;
            }
            long volgende = VOLGENDE.computeIfAbsent(s.id(), k -> nu + s.respawnTicks());
            if (nu < volgende && levend > 0) {
                continue;
            }
            VOLGENDE.put(s.id(), nu + s.respawnTicks());
            spawn(sl, s);
        }
    }

    public static VadsrijkMob spawn(ServerLevel sl, Spawner s) {
        RandomSource r = sl.getRandom();
        for (int poging = 0; poging < 8; poging++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * s.straal();
            int x = s.centrum().getX() + (int) Math.round(Math.cos(a) * d), z = s.centrum().getZ() + (int) Math.round(Math.sin(a) * d);
            if (!sl.isLoaded(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = s.grot() ? vloer(sl, x, z, s.minY(), s.maxY()) : sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y == Integer.MIN_VALUE || y <= sl.getMinY() + 1 || !sl.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                continue;
            }
            VadsrijkMob m = s.type().get().create(sl, EntitySpawnReason.SPAWNER);
            if (m == null) {
                return null;
            }
            boolean elite = r.nextDouble() < s.eliteKans();
            Set<Affix> af = EnumSet.noneOf(Affix.class);
            if (elite) {
                Affix[] alle = Affix.values();
                af.add(alle[r.nextInt(alle.length)]);
                if (r.nextBoolean()) {
                    af.add(alle[r.nextInt(alle.length)]);
                }
            }
            int lvl = s.minLevel() + (s.maxLevel() > s.minLevel() ? r.nextInt(s.maxLevel() - s.minLevel() + 1) : 0);
            m.snapTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360, 0);
            m.stelIn(lvl, s.element(), elite, af);
            m.spawner = s.id();
            sl.addFreshEntity(m);
            return m;
        }
        return null;
    }

    /** The first standing spot searching down from maxY to minY (solid floor, 2 free blocks, no fluid), or MIN_VALUE. */
    static int vloer(ServerLevel sl, int x, int z, int minY, int maxY) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int boven = Math.min(maxY, sl.getMaxY() - 2), onder = Math.max(minY, sl.getMinY() + 1);
        for (int y = boven; y >= onder; y--) {
            m.set(x, y - 1, z);
            if (sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() || !sl.getFluidState(m).isEmpty()) {
                continue;
            }
            m.set(x, y, z);
            boolean vrij1 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() && sl.getFluidState(m).isEmpty();
            m.set(x, y + 1, z);
            boolean vrij2 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty();
            if (vrij1 && vrij2) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
