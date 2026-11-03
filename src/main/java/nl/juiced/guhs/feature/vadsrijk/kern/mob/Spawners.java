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
    public record Spawner(String id, BlockPos centrum, int straal, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int max, int respawnTicks,
            int minLevel, int maxLevel, Element element, double eliteKans) {
    }

    private static final Map<String, Spawner> ALLE = new LinkedHashMap<>();
    private static final Map<String, Long> VOLGENDE = new LinkedHashMap<>();

    private Spawners() {
    }

    public static synchronized void registreer(Spawner s) {
        ALLE.put(s.id(), s);
    }

    /** Every second (kern). */
    public static void tick(ServerLevel sl) {
        long nu = sl.getGameTime();
        for (Spawner s : ALLE.values()) {
            if (!sl.isLoaded(s.centrum()) || sl.getNearestPlayer(s.centrum().getX(), s.centrum().getY(), s.centrum().getZ(), 64, false) == null) {
                continue;
            }
            int r = s.straal() + 16;
            int levend = sl.getEntitiesOfClass(VadsrijkMob.class, new AABB(s.centrum()).inflate(r, 32, r), m -> s.id().equals(m.spawner) && !m.geplofd()).size();
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
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= sl.getMinY() + 1 || !sl.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
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
}
