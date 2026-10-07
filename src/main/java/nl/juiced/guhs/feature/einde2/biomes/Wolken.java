package nl.juiced.guhs.feature.einde2.biomes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;

/**
 * einde2, slice biomes: who the kruimelwolk carries. Everybody sinks slowly through a {@code guhs:kruimelwolk}; whoever
 * one of the registered rules names stands on it (the Kruimelguh registers itself).
 * Skeleton stub: the list and the question are real, the cloud block that asks it comes with the biomes slice.
 * <p>
 * hook fase B: the Guhvatar's air power registers the players who learnt it here.
 */
public final class Wolken {
    private static final List<Predicate<Entity>> DRAGERS = new CopyOnWriteArrayList<>();

    /** Whoever this rule names walks on the kruimelwolken. */
    public static void looptErop(Predicate<Entity> wie) {
        DRAGERS.add(wie);
    }

    /** Does a kruimelwolk carry this entity? */
    public static boolean draagt(Entity e) {
        for (Predicate<Entity> wie : DRAGERS) {
            if (wie.test(e)) {
                return true;
            }
        }
        return false;
    }

    private Wolken() {
    }
}
