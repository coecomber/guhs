package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.feature.ring.Zicht;

/**
 * einde2 phase B, slice geest_spel: the creatures that exist for ONE player (CONTRACT_E2_B 2.4, 4.3): a lost lantern only
 * that player still has to bring home, the angry Hei Bai-guh of a player who has not calmed him yet, a sighting, the hooded
 * Wan-guh far away. Each is made with {@link Zicht#alleenVoor}, so two players on the same spot each have their own; it
 * is never saved (one that comes back from disk is thrown away: the slice makes it again when it is wanted) and it goes when
 * its player leaves the Geestenwereld or logs out ({@link #weg(UUID)}).
 */
public final class GeestActeurs {
    /** The entity tag of every actor of this slice (and its key in the persistent data: owner + name). */
    public static final String TAG = "guhs_geestspel_acteur", NAAM = "guhs_geestspel_naam";

    /** player -> (actor name -> entity). */
    private static final Map<UUID, Map<String, Entity>> ACTEURS = new ConcurrentHashMap<>();

    private GeestActeurs() {
    }

    /** This player's actor of that name (null: none, or it is gone). */
    @Nullable
    public static Entity van(ServerPlayer p, String naam) {
        Map<String, Entity> m = ACTEURS.get(p.getUUID());
        Entity e = m == null ? null : m.get(naam);
        if (e != null && (e.isRemoved() || e.level() != p.level())) {
            m.remove(naam);
            return null;
        }
        return e;
    }

    public static boolean heeft(ServerPlayer p, String naam) {
        return van(p, naam) != null;
    }

    /**
     * Makes the actor {@code naam} of this player when it is not there yet: entity {@code type} (an id: "guhs:lantaarngeest")
     * at {@code plek}, looking {@code yaw}; {@code nbt} adds to its save tag (Soort, Stil, Houding, Kind ...). It cannot be
     * hurt, never despawns and only this player sees it. Null: the type does not exist (yet).
     */
    @Nullable
    public static Entity zorg(ServerPlayer p, String naam, String type, Vec3 plek, float yaw, @Nullable Consumer<CompoundTag> nbt) {
        Entity al = van(p, naam);
        if (al != null) {
            return al;
        }
        Identifier id = Identifier.tryParse(type);
        if (id == null || !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        if (nbt != null) {
            nbt.accept(tag);
        }
        ServerLevel level = p.level();
        Entity e = EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.TRIGGERED, x -> x);
        if (e == null) {
            return null;
        }
        e.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        e.setYHeadRot(yaw);
        e.setInvulnerable(true);
        e.addTag(TAG);
        e.getPersistentData().putString(NAAM, p.getStringUUID() + "|" + naam);
        if (e instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setYBodyRot(yaw);
        }
        Zicht.alleenVoor(e, p.getUUID());
        if (!level.addFreshEntity(e)) {
            return null;
        }
        ACTEURS.computeIfAbsent(p.getUUID(), u -> new ConcurrentHashMap<>()).put(naam, e);
        return e;
    }

    /** Takes this actor away (with a little puff when {@code poef}). */
    public static void weg(ServerPlayer p, String naam, boolean poef) {
        Map<String, Entity> m = ACTEURS.get(p.getUUID());
        Entity e = m == null ? null : m.remove(naam);
        if (e != null && !e.isRemoved()) {
            if (poef && e.level() instanceof ServerLevel level) {
                level.sendParticles(p, ParticleTypes.END_ROD, true, false, e.getX(), e.getY() + 0.8, e.getZ(), 16, 0.3, 0.4, 0.3, 0.03);
            }
            e.discard();
        }
    }

    /** Every actor of this player whose name starts with this prefix. */
    public static void wegMet(ServerPlayer p, String prefix) {
        Map<String, Entity> m = ACTEURS.get(p.getUUID());
        if (m == null) {
            return;
        }
        for (String naam : Map.copyOf(m).keySet()) {
            if (naam.startsWith(prefix)) {
                weg(p, naam, false);
            }
        }
    }

    /** All actors of this player. */
    public static Map<String, Entity> alle(ServerPlayer p) {
        Map<String, Entity> m = ACTEURS.get(p.getUUID());
        return m == null ? Map.of() : new HashMap<>(m);
    }

    /** Every actor of this player goes (they left, or logged out). */
    public static void weg(UUID speler) {
        Map<String, Entity> m = ACTEURS.remove(speler);
        if (m != null) {
            m.values().forEach(e -> {
                if (!e.isRemoved()) {
                    e.discard();
                }
            });
        }
    }

    /** The name (without owner) of one of our actors, or null when the entity is none. */
    @Nullable
    public static String naam(Entity e) {
        if (!e.entityTags().contains(TAG)) {
            return null;
        }
        String s = e.getPersistentData().getStringOr(NAAM, "");
        int bar = s.indexOf('|');
        return bar < 0 ? null : s.substring(bar + 1);
    }

    /** The owner of one of our actors, or null. */
    @Nullable
    public static UUID eigenaar(Entity e) {
        return e.entityTags().contains(TAG) ? Zicht.eigenaar(e) : null;
    }

    /** An actor coming back from disk (a server restart) is never wanted: the slice makes it again when it should be there. */
    static void opLaden(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().entityTags().contains(TAG)) {
            event.setCanceled(true);
        }
    }
}
