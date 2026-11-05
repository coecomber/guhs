package nl.juiced.guhs.feature.vadsrijk.zone4.wereld;

import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;

/**
 * The Kringstroom hook for other slices (t-z4boot): {@link #EIGEN} says which vehicles handle the current themselves (the
 * sailboat), so {@link Z4WereldRegistry} does not push them twice. The strength at a point is {@link Z4WereldPlekken#stroming}.
 */
public final class Z4Stroming {
    /** Vehicles that do their own current handling (set by the sailboat slice). */
    public static Predicate<Entity> EIGEN = e -> false;

    private Z4Stroming() {
    }

    static boolean eigenStroming(Entity e) {
        return EIGEN.test(e);
    }
}
