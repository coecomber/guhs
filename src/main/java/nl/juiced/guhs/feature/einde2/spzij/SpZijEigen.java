package nl.juiced.guhs.feature.einde2.spzij;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.storage.Nbt;

/**
 * einde2 "Sponsguh", slice sp_zij: the entities of ONE player (CONTRACT_E2_C 2.6: {@code Zicht.alleenVoor}): Sandy-guh's
 * own copy for a player who has not done the karate yet, the ghost captain on the deck, the rocket parts, the ghost lanterns
 * and Zeemeerman's belt lying round town. Each is known by (player, name); asking for one that stands returns it, one that
 * is no longer wanted is taken away ({@link #weg}). They are never kept: a world that brings one back from disk throws it
 * away ({@link #opJoin}), and a player who leaves loses theirs ({@link #vergeet}).
 */
final class SpZijEigen {
    /** player -> name -> the entity's UUID. */
    private static final Map<UUID, Map<String, UUID>> VAN = new ConcurrentHashMap<>();

    private SpZijEigen() {
    }

    /** The entity of this player by this name, when it still stands in this level. */
    @Nullable
    static Entity get(ServerLevel level, ServerPlayer p, String naam) {
        Map<String, UUID> m = VAN.get(p.getUUID());
        UUID id = m == null ? null : m.get(naam);
        if (id == null) {
            return null;
        }
        Entity e = level.getEntity(id);
        if (e == null || !e.isAlive()) {
            m.remove(naam);
            return null;
        }
        return e;
    }

    /** Is something by this name there for the player? */
    static boolean heeft(ServerLevel level, ServerPlayer p, String naam) {
        return get(level, p, naam) != null;
    }

    /** Takes this player's entity of this name away (when it stands). */
    static void weg(ServerLevel level, ServerPlayer p, String naam) {
        Entity e = get(level, p, naam);
        if (e != null) {
            e.discard();
        }
        Map<String, UUID> m = VAN.get(p.getUUID());
        if (m != null) {
            m.remove(naam);
        }
    }

    /** Takes every entity of this player whose name starts with this away. */
    static void wegAlles(ServerLevel level, ServerPlayer p, String voor) {
        Map<String, UUID> m = VAN.get(p.getUUID());
        if (m == null) {
            return;
        }
        for (String naam : m.keySet()) {
            if (naam.startsWith(voor)) {
                weg(level, p, naam);
            }
        }
    }

    /** A player left: everything of theirs goes (the server keeps running). */
    static void vergeet(ServerPlayer p) {
        Map<String, UUID> m = VAN.remove(p.getUUID());
        if (m == null || !(p.level() instanceof ServerLevel level)) {
            return;
        }
        for (UUID id : m.values()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
    }

    private static <T extends Entity> T zet(ServerLevel level, ServerPlayer p, String naam, T e) {
        e.addTag(SpZij.EIGEN);
        Zicht.alleenVoor(e, p.getUUID());
        level.addFreshEntity(e);
        VAN.computeIfAbsent(p.getUUID(), u -> new ConcurrentHashMap<>()).put(naam, e.getUUID());
        return e;
    }

    /** A character of the cast for this player only, standing still on its mark and looking this way. */
    @Nullable
    static SponsguhWezen wezen(ServerLevel level, ServerPlayer p, String naam, Cast cast, Vec3 plek, float yaw) {
        SponsguhWezen al = get(level, p, naam) instanceof SponsguhWezen w ? w : null;
        if (al != null) {
            return al;
        }
        SponsguhWezen w = E2SponsguhFeature.type(cast).create(level, EntitySpawnReason.EVENT);
        if (w == null) {
            return null;
        }
        w.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        w.setYHeadRot(yaw);
        w.setYBodyRot(yaw);
        w.zetStil(true);
        return zet(level, p, naam, w);
    }

    /** A thing lying in the town for this player only (an item display at hip height, turning to face them, lit by itself). */
    @Nullable
    static Entity ding(ServerLevel level, ServerPlayer p, String naam, String item, Vec3 plek, float schaal) {
        Entity al = get(level, p, naam);
        if (al != null) {
            return al;
        }
        Entity d = EntityType.ITEM_DISPLAY.create(level, EntitySpawnReason.EVENT);
        if (d == null) {
            return null;
        }
        Nbt.load(d, dingData(item, schaal));
        d.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
        return zet(level, p, naam, d);
    }

    /** The entity data of such a thing (the item, ground view, turning towards whoever looks, full light). */
    static CompoundTag dingData(String item, float schaal) {
        CompoundTag tag = new CompoundTag();
        CompoundTag stapel = new CompoundTag();
        stapel.putString("id", item);
        stapel.putInt("count", 1);
        tag.put("item", stapel);
        tag.putString("item_display", "ground");
        tag.putString("billboard", "vertical");
        CompoundTag vorm = new CompoundTag();
        vorm.put("translation", floats(0f, 0f, 0f));
        vorm.put("left_rotation", floats(0f, 0f, 0f, 1f));
        vorm.put("scale", floats(schaal, schaal, schaal));
        vorm.put("right_rotation", floats(0f, 0f, 0f, 1f));
        tag.put("transformation", vorm);
        CompoundTag licht = new CompoundTag();
        licht.putInt("block", 15);
        licht.putInt("sky", 15);
        tag.put("brightness", licht);
        return tag;
    }

    static ListTag floats(float... f) {
        ListTag l = new ListTag();
        for (float v : f) {
            l.add(FloatTag.valueOf(v));
        }
        return l;
    }

    /** An entity of ours that a world brings back from disk is not wanted: they are made fresh for whoever is near. */
    static void opJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().entityTags().contains(SpZij.EIGEN)) {
            event.setCanceled(true);
        }
    }
}
