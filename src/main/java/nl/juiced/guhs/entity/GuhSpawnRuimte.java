package nl.juiced.guhs.entity;

import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.8.0: room for a guh's real size. Vanilla checks a spawn spot with the default hitbox (0.9 x 0.8), but a wild guh only
 * gets its random size (SCALE, up to 2.1x) in finalizeSpawn, so a big one could end up half in a wall and suffocate.
 * {@link #pasIn} finds a free spot close by for the final hitbox (or shrinks a wild guh until it fits);
 * {@link #bevrijd} is the safety net for a guh found in a wall in its first seconds (spawned, or loaded from a structure).
 */
public final class GuhSpawnRuimte {
    /** How far a guh may be moved to a free spot: sideways and up, in blocks. */
    static final int ZIJ = 2, OP = 2;
    /** Each try a wild guh that doesn't fit anywhere gets this much smaller. */
    static final float KRIMP = 0.85f;

    private GuhSpawnRuimte() {
    }

    /** Whether the guh's current hitbox is free of blocks. */
    public static boolean vrij(CollisionGetter level, GuhEntity guh) {
        return level.noCollision(guh, guh.getBoundingBox());
    }

    /**
     * At spawn (after the random size): free spot as it is, else a free spot with ground under it close by, else (wild guh
     * only) a bit smaller until it fits. Returns whether it fits now.
     */
    public static boolean pasIn(CollisionGetter level, GuhEntity guh, boolean magKrimpen) {
        if (vrij(level, guh)) {
            return true;
        }
        if (verhuis(level, guh)) {
            return true;
        }
        if (!magKrimpen) {
            return false;
        }
        while (guh.getGuhScale() > GuhEntity.MIN_SCALE + 0.001f) {
            guh.setGuhScale(guh.getGuhScale() * KRIMP);
            if (vrij(level, guh) || verhuis(level, guh)) {
                return true;
            }
        }
        return vrij(level, guh);
    }

    /** The safety net: move a stuck guh to free room (a wild one may also shrink). */
    public static boolean bevrijd(CollisionGetter level, GuhEntity guh) {
        return pasIn(level, guh, !guh.isTame());
    }

    /** Tries the spots around the guh, nearest first, that are free and have something to stand on; moves it there. */
    static boolean verhuis(CollisionGetter level, GuhEntity guh) {
        Vec3 start = guh.position();
        AABB box = guh.getBoundingBox();
        for (int afstand = 1; afstand <= ZIJ + OP; afstand++) {
            for (int dy = 0; dy <= OP; dy++) {
                for (int dx = -ZIJ; dx <= ZIJ; dx++) {
                    for (int dz = -ZIJ; dz <= ZIJ; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) + dy != afstand) {
                            continue;
                        }
                        AABB plek = box.move(dx, dy, dz);
                        if (level.noCollision(guh, plek) && !level.noCollision(guh, plek.move(0, -0.2, 0).setMaxY(plek.minY))) {
                            guh.setPos(start.x + dx, start.y + dy, start.z + dz);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
