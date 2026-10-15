package nl.juiced.guhs.feature.einde2.zij;

import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * einde2 zij: entities that exist for one player for a while (the strayed bison of the Luchttempel). They carry the tag
 * {@link #TAG}; such an entity that comes back from disk (the chunk was saved while it stood there) is refused at once,
 * so nothing of one player's quest ever stays in the world.
 */
public final class ZijActeurs {
    public static final String TAG = "guhs_einde2_zij_tijdelijk";

    private ZijActeurs() {
    }

    /** Marks an entity as temporary (call before adding it to the level). */
    public static void tijdelijk(Entity e) {
        e.addTag(TAG);
    }

    static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().entityTags().contains(TAG)) {
            event.setCanceled(true);
        }
    }
}
