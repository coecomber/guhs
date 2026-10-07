package nl.juiced.guhs.feature.guhpixel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The per-player saved data of everything guhpixel (SavedData {@code guhs:px_spelers}, kept with the overworld): one
 * mutable tag per player per slice ({@link #deel}), readable and writable for offline players too, plus one shared tag
 * ({@link #algemeen}: the clock offset, the lobby version). Call {@link #vuil} after every change.
 */
public final class PxData extends SavedData {
    public static final SavedDataType<PxData> TYPE = GuhSavedData.tagType("px_spelers", PxData::new, PxData::load, PxData::save);

    private final Map<UUID, CompoundTag> spelers = new HashMap<>();
    private CompoundTag algemeen = new CompoundTag();

    public static PxData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The tag of this slice for this player (created on demand; change it and call {@link #vuil}). */
    public static CompoundTag deel(ServerPlayer p, String slice) {
        return deel(p.level().getServer(), p.getUUID(), slice);
    }

    /** The same, also for a player who is offline. */
    public static CompoundTag deel(MinecraftServer server, UUID speler, String slice) {
        PxData data = get(server);
        CompoundTag alles = data.spelers.computeIfAbsent(speler, u -> new CompoundTag());
        if (alles.getCompound(slice).isEmpty()) {
            alles.put(slice, new CompoundTag());
            data.setDirty();
        }
        return alles.getCompoundOrEmpty(slice);
    }

    /** A sub tag of a tag, created (and attached) on demand. */
    public static CompoundTag sub(CompoundTag tag, String key) {
        if (tag.getCompound(key).isEmpty()) {
            tag.put(key, new CompoundTag());
        }
        return tag.getCompoundOrEmpty(key);
    }

    /** Shared, not per player. */
    public static CompoundTag algemeen(MinecraftServer server) {
        return get(server).algemeen;
    }

    public static void vuil(MinecraftServer server) {
        get(server).setDirty();
    }

    private CompoundTag save() {
        CompoundTag out = new CompoundTag();
        CompoundTag s = new CompoundTag();
        spelers.forEach((id, tag) -> s.put(id.toString(), tag));
        out.put("Spelers", s);
        out.put("Algemeen", algemeen);
        return out;
    }

    private static PxData load(CompoundTag tag) {
        PxData data = new PxData();
        CompoundTag s = tag.getCompoundOrEmpty("Spelers");
        for (String key : s.keySet()) {
            try {
                data.spelers.put(UUID.fromString(key), s.getCompoundOrEmpty(key));
            } catch (IllegalArgumentException ignored) {
                // (not a UUID: skip it)
            }
        }
        data.algemeen = tag.getCompoundOrEmpty("Algemeen");
        return data;
    }
}
