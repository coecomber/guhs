package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import nl.juiced.guhs.Guhs;

/**
 * (fix round 3, i-ui) The rest of the mod stays outside the Vadsrijk: makelaar Guhrrit, the Postguh, the Sherlock cast,
 * followers like the Libelkonijnguh / Sponsguh / Gerrit, called guhs and any other Guhs entity that is not part of the
 * Vadsrijk never appear in {@code guhs:vadsrijk}.
 * <ul>
 *   <li>a non-player entity on its way INTO the Vadsrijk (vanilla teleport / portal) stays where it is (the travel is
 *       cancelled, so nothing is lost);</li>
 *   <li>a Guhs entity that would join the Vadsrijk level (spawned there, or loaded from an old save) is refused.</li>
 * </ul>
 * Allowed: players, every vanilla / other-mod entity, Guhs entity types with an id starting with {@code vr_}, classes in the
 * vadsrijk package, and the ids in {@link #TOEGESTAAN} (an ambient guh fish from the Sfeer). A new Vadsrijk entity from the
 * shared mod: {@code Buitenstaanders.TOEGESTAAN.add("my_id")}. Features that move their own followers can also ask
 * {@link Vadsrijk#in} first and simply wait (cleaner than being refused).
 */
public final class Buitenstaanders {
    /** Guhs entity type paths (no "vr_") that may still be in the Vadsrijk. */
    public static final Set<String> TOEGESTAAN = ConcurrentHashMap.newKeySet();

    static {
        TOEGESTAAN.add("guh_vis");   // (wezens/Sfeer: ambient fish at the ponds)
    }

    private Buitenstaanders() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((EntityTravelToDimensionEvent e) -> {
            if (weigert(e.getDimension(), e.getEntity())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide() && weigert(e.getLevel().dimension(), e.getEntity())) {
                e.setCanceled(true);
            }
        });
    }

    /** Is this entity refused in this dimension (= the Vadsrijk and it does not belong there)? */
    public static boolean weigert(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, Entity e) {
        return dim == Vadsrijk.DIM && !hoortErbij(e);
    }

    /** May this entity be in the Vadsrijk? */
    public static boolean hoortErbij(Entity e) {
        if (e instanceof Player || e.getClass().getName().startsWith("nl.juiced.guhs.feature.vadsrijk.")) {
            return true;
        }
        return hoortErbij(e.getType());
    }

    public static boolean hoortErbij(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id == null || !Guhs.MODID.equals(id.getNamespace()) || id.getPath().startsWith("vr_") || TOEGESTAAN.contains(id.getPath());
    }
}
