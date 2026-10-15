package nl.juiced.guhs.feature.einde2.h1lucht;

import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * einde2 phase B, slice h1_lucht: the story actors this slice puts in the world for ONE player (the kaasbol, the luchtbal
 * and Aang-guh on his pole, the lemurguhtje of the chase, Zuka and his men at the pier). They live only while their moment
 * lasts: a copy that was saved with its chunk anyway (a server stop in the middle) is not let back in when the chunk loads.
 */
public final class Tijdelijk {
    /** The tag of every such actor. */
    public static final String TAG = "guhs_einde2_h1_lucht_tijdelijk";

    private Tijdelijk() {
    }

    public static void merk(Entity e) {
        e.addTag(TAG);
    }

    public static boolean is(Entity e) {
        return e.entityTags().contains(TAG);
    }

    static void opJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && is(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Takes an actor away (null: nothing). */
    static void weg(Entity e) {
        if (e != null && !e.isRemoved()) {
            e.discard();
        }
    }
}
