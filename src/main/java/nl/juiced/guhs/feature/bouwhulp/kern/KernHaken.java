package nl.juiced.guhs.feature.bouwhulp.kern;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** What {@code BouwhulpFeature} hangs on the game's events for the kern: the building tick, the party flags. */
public final class KernHaken {
    /** Every level tick on the server: build, take away flags whose time is over. */
    public static void tick(ServerLevel level) {
        Spoken.van(level).tick(level);
        Feestje.tick(level);
    }

    /** A party flag that outlived a restart (it must not come back). */
    public static boolean oudeVlag(Entity e) {
        return Feestje.oudeVlag(e);
    }

    private KernHaken() {
    }
}
