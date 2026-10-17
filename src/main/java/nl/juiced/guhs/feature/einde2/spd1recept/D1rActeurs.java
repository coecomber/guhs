package nl.juiced.guhs.feature.einde2.spd1recept;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.ring.Zicht;

/**
 * einde2 "Sponsguh", slice sp_d1_recept: this slice's characters in the world that exist for ONE player
 * ({@link Zicht#alleenVoor}, CONTRACT_E2_C 2.6): the band of the Bellenkom, Octo on his conductor's spot, Sponsguh at the
 * microphone and Octoliam on his stand while that player drums (step 10). Each has a name per player, so asking twice gives the
 * same one; they stand still on their mark ({@code zetStil}) and are never saved for good: whatever stands in the world with
 * {@link #TAG} after a restart is not ours any more and goes ({@link #opBinnenkomst}). Logging out takes them along.
 */
final class D1rActeurs {
    /** The entity tag of every actor of this class. */
    static final String TAG = "guhs_einde2_sp_d1_recept_acteur";

    /** player -> name -> the actor. */
    private static final Map<UUID, Map<String, Entity>> ALLE = new ConcurrentHashMap<>();

    private D1rActeurs() {
    }

    /** This player's actor of that name (null: none, or it is gone). */
    @Nullable
    static Entity van(ServerPlayer p, String naam) {
        Map<String, Entity> m = ALLE.get(p.getUUID());
        Entity e = m == null ? null : m.get(naam);
        if (e != null && (e.isRemoved() || !e.isAlive())) {
            m.remove(naam);
            return null;
        }
        return e;
    }

    /**
     * This player's cast member of that name: the one already there, or a new one of this cast at this spot (only this player
     * sees it), standing still and looking that way. soort: the look of a character with several (-1: its own pick).
     */
    @Nullable
    static SponsguhWezen wezen(ServerPlayer p, String naam, Cast cast, ServerLevel level, Vec3 waar, float yaw, int soort) {
        Entity al = van(p, naam);
        if (al instanceof SponsguhWezen w && al.level() == level) {
            return w;
        }
        weg(p, naam);
        SponsguhWezen w = E2SponsguhFeature.type(cast).create(level, EntitySpawnReason.TRIGGERED);
        if (w == null) {
            return null;
        }
        if (soort >= 0) {
            w.zetSoort(soort);                                     // (before it is placed: its little clarinet toot plays at 0, 0, 0)
        }
        w.snapTo(waar.x, waar.y, waar.z, yaw, 0f);
        w.setYHeadRot(yaw);
        w.setYBodyRot(yaw);
        w.setInvulnerable(true);
        w.zetStil(true);
        w.addTag(TAG);
        Zicht.alleenVoor(w, p.getUUID());
        ALLE.computeIfAbsent(p.getUUID(), k -> new ConcurrentHashMap<>()).put(naam, w);   // (before it joins: see opBinnenkomst)
        level.addFreshEntity(w);
        return w;
    }

    /** Takes this player's actor of that name away. */
    static void weg(ServerPlayer p, String naam) {
        Map<String, Entity> m = ALLE.get(p.getUUID());
        Entity e = m == null ? null : m.remove(naam);
        if (e != null && !e.isRemoved()) {
            e.discard();
        }
    }

    /** Takes every actor of this player away. */
    static void wegAlle(ServerPlayer p) {
        Map<String, Entity> m = ALLE.remove(p.getUUID());
        if (m != null) {
            m.values().forEach(e -> {
                if (!e.isRemoved()) {
                    e.discard();
                }
            });
        }
    }

    /** Has this player any actor now? */
    static boolean heeft(ServerPlayer p) {
        Map<String, Entity> m = ALLE.get(p.getUUID());
        return m != null && !m.isEmpty();
    }

    /** Every actor of this player now. */
    static List<Entity> alle(ServerPlayer p) {
        Map<String, Entity> m = ALLE.get(p.getUUID());
        return m == null ? List.of() : new ArrayList<>(m.values());
    }

    /** Plays an animation on every actor of this player whose name starts with prefix. */
    static void speel(ServerPlayer p, String prefix, String animatie) {
        Map<String, Entity> m = ALLE.get(p.getUUID());
        if (m == null) {
            return;
        }
        m.forEach((naam, e) -> {
            if (naam.startsWith(prefix) && e instanceof SponsguhWezen w && !e.isRemoved()) {
                w.zetAnimatie(animatie);
            }
        });
    }

    /** An actor of a session that ended (a restart, a chunk saved with it): never ours, so it goes. */
    static void opBinnenkomst(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (event.getLevel().isClientSide() || !e.entityTags().contains(TAG)) {
            return;
        }
        for (Map<String, Entity> m : ALLE.values()) {
            if (m.containsValue(e)) {
                return;
            }
        }
        event.setCanceled(true);
    }

    static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            wegAlle(p);
        }
    }
}
