package nl.juiced.guhs.feature.guhpixel.lobby;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

/**
 * Floating text that only ONE player sees (their own stats on the lobby boards): a text display that exists only as
 * packets for that player. The server makes a text display that is never added to the world, and sends its "add entity"
 * and "entity data" packets to that one player; a changed text is a new data packet for the same entity id. No client
 * code, and every player reads their own numbers at the same spot. (Shared texts are plain text displays in the world:
 * {@code Scorebord.show}.)
 */
public final class Zweeftekst {
    /** What one player sees at one spot: the entity id their client knows, and the text it shows. */
    private record Zicht(int id, Component tekst, Vec3 pos) {
    }

    private static final Map<UUID, Map<String, Zicht>> ZICHT = new ConcurrentHashMap<>();

    /** Shows (or updates) the text {@code sleutel} for this player at pos. Returns true when a packet was sent. */
    public static boolean toon(ServerPlayer p, String sleutel, Vec3 pos, Component tekst) {
        Map<String, Zicht> mijn = ZICHT.computeIfAbsent(p.getUUID(), u -> new HashMap<>());
        Zicht oud = mijn.get(sleutel);
        if (oud != null && oud.tekst().equals(tekst) && oud.pos().equals(pos)) {
            return false;
        }
        if (!(p.level() instanceof ServerLevel level) || p.connection == null) {
            return false;
        }
        Entity e = maak(level, pos, tekst);
        if (e == null) {
            return false;
        }
        boolean nieuw = oud == null || !oud.pos().equals(pos);
        if (oud != null && nieuw) {
            p.connection.send(new ClientboundRemoveEntitiesPacket(oud.id()));
        }
        int id = nieuw ? e.getId() : oud.id();
        if (nieuw) {
            p.connection.send(new ClientboundAddEntityPacket(id, e.getUUID(), pos.x, pos.y, pos.z, 0f, 0f, e.getType(), 0, Vec3.ZERO, 0d));
        }
        List<SynchedEntityData.DataValue<?>> waarden = e.getEntityData().getNonDefaultValues();
        if (waarden != null) {
            p.connection.send(new ClientboundSetEntityDataPacket(id, waarden));
        }
        mijn.put(sleutel, new Zicht(id, tekst, pos));
        return true;
    }

    /** A text display that is NOT in the world (only its data is used). */
    @Nullable
    private static Entity maak(ServerLevel level, Vec3 pos, Component tekst) {
        Tag encoded = ComponentSerialization.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), tekst).getOrThrow();
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:text_display");
        tag.put("text", encoded);
        tag.putString("billboard", "vertical");
        tag.putString("alignment", "center");
        tag.putInt("line_width", 260);
        tag.putInt("background", 0x90301028);
        return EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.LOAD, x -> {
            x.snapTo(pos.x, pos.y, pos.z, 0, 0);
            return x;
        });
    }

    /** What this player sees at {@code sleutel} now (null: nothing). */
    @Nullable
    public static Component zichtbaar(ServerPlayer p, String sleutel) {
        Zicht z = ZICHT.getOrDefault(p.getUUID(), Map.of()).get(sleutel);
        return z == null ? null : z.tekst();
    }

    public static boolean ziet(ServerPlayer p) {
        Map<String, Zicht> mijn = ZICHT.get(p.getUUID());
        return mijn != null && !mijn.isEmpty();
    }

    /** Takes every personal text of this player away again (they walked off, into a game...). */
    public static void weg(ServerPlayer p) {
        Map<String, Zicht> mijn = ZICHT.remove(p.getUUID());
        if (mijn != null && !mijn.isEmpty() && p.connection != null) {
            p.connection.send(new ClientboundRemoveEntitiesPacket(mijn.values().stream().mapToInt(Zicht::id).toArray()));
        }
    }

    /** The client forgot everything by itself (another dimension, logged out, respawned): only our notes go. */
    public static void vergeet(ServerPlayer p) {
        ZICHT.remove(p.getUUID());
    }

    private Zweeftekst() {
    }
}
