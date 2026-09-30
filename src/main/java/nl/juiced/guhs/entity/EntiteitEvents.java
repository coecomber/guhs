package nl.juiced.guhs.entity;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import nl.juiced.guhs.feature.bakkerij.BakkerijKlant;
import nl.juiced.guhs.feature.baltoslee.SleeEntity;

/**
 * The mod's own entity-event ids ({@code level.broadcastEntityEvent(entity, id)}) and the vanilla ids they must avoid
 * (1.21.1). Vanilla's {@code ClientPacketListener.handleEntityEvent} handles {@link #CLIENT_LISTENER} itself BEFORE the
 * entity gets it and casts the entity (21 Guardian, 35 totem, 63 Sniffer): a custom id there crashes every client (the
 * 3.0 sled race sent 63 for a rest stop: "SleeEntity cannot be cast to Sniffer"). The superclass ids would be eaten by (or
 * trigger) vanilla effects. Deliberate reuse of a vanilla effect (6 smoke / 7 hearts on a TamableAnimal, 18 love hearts on
 * an Animal) is not a custom id and is not listed here.
 */
public final class EntiteitEvents {
    private EntiteitEvents() {
    }

    /** Handled by ClientPacketListener before the entity sees them (with a cast!). */
    public static final Set<Integer> CLIENT_LISTENER = Set.of(21, 35, 63);

    /** A custom event of one entity class. */
    public record Eigen(Class<? extends Entity> soort, int id, String naam) {
    }

    /** Every custom id of the mod (keep in sync when adding one; the gametest checks it). */
    public static final List<Eigen> EIGEN = List.of(
            new Eigen(SleeEntity.class, SleeEntity.EV_BEDOLVEN, "slee bedolven"),
            new Eigen(SleeEntity.class, SleeEntity.EV_ONTWEKEN, "slee ontweken"),
            new Eigen(SleeEntity.class, SleeEntity.EV_PLOF, "slee plof"),
            new Eigen(SleeEntity.class, SleeEntity.EV_RUST, "slee rust"),
            new Eigen(SleeEntity.class, SleeEntity.EV_KEER, "slee keer"),
            new Eigen(BakkerijKlant.class, BakkerijKlant.EV_NEE, "bakkerijklant nee"));

    /** The ids vanilla already uses for an entity of this class (the listener's plus those of its superclasses). */
    public static Set<Integer> gereserveerd(Class<? extends Entity> soort) {
        Set<Integer> ids = new TreeSet<>(CLIENT_LISTENER);
        ids.add(53);                                                        // Entity: honey slide
        if (LivingEntity.class.isAssignableFrom(soort)) {
            ids.addAll(List.of(2, 3, 29, 30, 46, 47, 48, 49, 50, 51, 52, 54, 55, 60, 65)); // 2: old hurt (kept out, to be safe)
        }
        if (Mob.class.isAssignableFrom(soort)) {
            ids.add(20);                                                    // spawn poof
        }
        if (Animal.class.isAssignableFrom(soort)) {
            ids.add(18);                                                    // in love
        }
        if (TamableAnimal.class.isAssignableFrom(soort)) {
            ids.addAll(List.of(6, 7));                                      // taming smoke / hearts
        }
        return ids;
    }

    /** The custom ids that collide with a vanilla one (empty = fine). */
    public static List<Eigen> botsingen() {
        return EIGEN.stream().filter(e -> e.id() < 0 || e.id() > Byte.MAX_VALUE || gereserveerd(e.soort()).contains(e.id())).toList();
    }
}
