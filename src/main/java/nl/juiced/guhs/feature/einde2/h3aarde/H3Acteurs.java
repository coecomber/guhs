package nl.juiced.guhs.feature.einde2.h3aarde;

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
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The chapter's own actors in the world that exist for ONE player ({@link Zicht#alleenVoor}): the Dai Li-Mika's that
 * follow you, the guards of the sneak through the base, Miroh under the tree, Toph at the camp, the koolman who just moved
 * his stall, the team at your theeset, Miroh in his reopened tea house. Each has a name per player ("dailing_1", "toph"...),
 * so asking for it twice gives the same one; they are never saved for good: whatever stands in the world with the tag
 * {@link #TAG} after a restart is not ours any more and goes ({@link #opBinnenkomst}).
 */
public final class H3Acteurs {
    /** The entity tag of every actor of this class. */
    public static final String TAG = "guhs_einde2_h3_aarde_acteur";

    /** player -> name -> the actor. */
    private static final Map<UUID, Map<String, Entity>> ALLE = new ConcurrentHashMap<>();

    private H3Acteurs() {
    }

    /** This player's actor of that name (null: none, or it is gone). */
    @Nullable
    public static Entity van(ServerPlayer p, String naam) {
        Map<String, Entity> m = ALLE.get(p.getUUID());
        Entity e = m == null ? null : m.get(naam);
        if (e != null && (e.isRemoved() || !e.isAlive())) {
            m.remove(naam);
            return null;
        }
        return e;
    }

    /**
     * This player's character of that name: the one already there, or a new GuhNpcEntity of this kind at this spot (only this
     * player sees it). plek = the role spot ({@code NpcRollen.PLEK}) or null; stil = it does not turn its head by itself
     * (the guards: their yaw is the game).
     */
    @Nullable
    public static GuhNpcEntity npc(ServerPlayer p, String naam, GuhNpcEntity.Kind kind, ServerLevel level, Vec3 waar, float yaw, @Nullable String plek, boolean stil) {
        Entity al = van(p, naam);
        if (al instanceof GuhNpcEntity npc && al.level() == level) {
            return npc;
        }
        weg(p, naam);
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (npc == null) {
            return null;
        }
        npc.setKind(kind);
        npc.snapTo(waar.x, waar.y, waar.z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setInvulnerable(true);
        npc.setNoAi(stil);
        if (plek != null) {
            npc.roleData.putString(NpcRollen.PLEK, plek);
        }
        npc.addTag(TAG);
        Zicht.alleenVoor(npc, p.getUUID());
        ALLE.computeIfAbsent(p.getUUID(), k -> new ConcurrentHashMap<>()).put(naam, npc);   // (before it joins: see opBinnenkomst)
        level.addFreshEntity(npc);
        return npc;
    }

    /** Any other entity as an actor of this player (made by the caller, not yet in the world). */
    public static Entity zet(ServerPlayer p, String naam, Entity e) {
        weg(p, naam);
        e.addTag(TAG);
        Zicht.alleenVoor(e, p.getUUID());
        ALLE.computeIfAbsent(p.getUUID(), k -> new ConcurrentHashMap<>()).put(naam, e);
        e.level().addFreshEntity(e);
        return e;
    }

    /** Takes this player's actor of that name away. */
    public static void weg(ServerPlayer p, String naam) {
        weg(p.getUUID(), naam);
    }

    private static void weg(UUID speler, String naam) {
        Map<String, Entity> m = ALLE.get(speler);
        Entity e = m == null ? null : m.remove(naam);
        if (e != null && !e.isRemoved()) {
            e.discard();
        }
    }

    /** Takes every actor of this player whose name starts with prefix away. */
    public static void wegAlle(ServerPlayer p, String prefix) {
        Map<String, Entity> m = ALLE.get(p.getUUID());
        if (m == null) {
            return;
        }
        for (String naam : new ArrayList<>(m.keySet())) {
            if (naam.startsWith(prefix)) {
                weg(p.getUUID(), naam);
            }
        }
    }

    /** The names of this player's actors now. */
    public static List<String> namen(ServerPlayer p) {
        Map<String, Entity> m = ALLE.get(p.getUUID());
        return m == null ? List.of() : List.copyOf(m.keySet());
    }

    /** An actor of a session that ended (a restart, a chunk that was saved with it): never ours, so it goes. */
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

    /** A player who logs out takes their actors along. */
    static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        Map<String, Entity> m = ALLE.remove(event.getEntity().getUUID());
        if (m != null) {
            m.values().forEach(e -> {
                if (!e.isRemoved()) {
                    e.discard();
                }
            });
        }
    }
}
