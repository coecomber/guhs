package nl.juiced.guhs.feature.safari.kern;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Op Safari! step 2 (S0): the per-player saved data of everything safari (SavedData {@code guhs:safari_spelers}, kept with
 * the overworld, like guhpixel's PxData): one mutable tag per player, readable and writable for offline players too.
 * Slices never touch it directly: they go through {@link SafariSpeler} and {@link SafariMunten}, which call {@link #vuil}.
 */
public final class SafariData extends SavedData {
    public static final SavedDataType<SafariData> TYPE = GuhSavedData.tagType("safari_spelers", SafariData::new, SafariData::load, SafariData::save);

    private final Map<UUID, CompoundTag> spelers = new HashMap<>();

    public static SafariData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** This player's tag (created on demand; change it and call {@link #vuil}). */
    static CompoundTag speler(MinecraftServer server, UUID speler) {
        SafariData data = get(server);
        CompoundTag tag = data.spelers.get(speler);
        if (tag == null) {
            tag = new CompoundTag();
            data.spelers.put(speler, tag);
            data.setDirty();
        }
        return tag;
    }

    /** A sub tag, created (and attached) on demand. */
    static CompoundTag sub(CompoundTag tag, String key) {
        if (tag.getCompound(key).isEmpty()) {
            tag.put(key, new CompoundTag());
        }
        return tag.getCompoundOrEmpty(key);
    }

    static void vuil(MinecraftServer server) {
        get(server).setDirty();
    }

    /** (Dev, tests) forgets everything of this player. */
    static void wis(MinecraftServer server, UUID speler) {
        get(server).spelers.remove(speler);
        vuil(server);
    }

    private CompoundTag save() {
        CompoundTag out = new CompoundTag();
        CompoundTag s = new CompoundTag();
        spelers.forEach((id, tag) -> s.put(id.toString(), tag));
        out.put("Spelers", s);
        return out;
    }

    private static SafariData load(CompoundTag tag) {
        SafariData data = new SafariData();
        CompoundTag s = tag.getCompoundOrEmpty("Spelers");
        for (String key : s.keySet()) {
            try {
                data.spelers.put(UUID.fromString(key), s.getCompoundOrEmpty(key));
            } catch (IllegalArgumentException ignored) {
                // (not a UUID: skip it)
            }
        }
        return data;
    }
}
