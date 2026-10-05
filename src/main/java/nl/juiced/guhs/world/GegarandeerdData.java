package nl.juiced.guhs.world;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * bbq2: where the guaranteed copies of the {@code "alleen_nieuw"} sets of a dimension stand (SavedData {@code guhs:gegarandeerd},
 * one per dimension): structure set id -> start chunk and flatness factor. A spot is written the first time its search finds
 * one and is the answer for ever after: once the building stands, its own chunks exist, so a new search would pick another
 * spot and make a second copy (and a later version may search differently).
 * <p>
 * The search runs on its own thread or on a worldgen thread, so this is loaded on the server thread when the level loads
 * ({@link GegarandeerdPlacement#laad}) and the map is a concurrent one.
 */
public class GegarandeerdData extends SavedData {
    public static final SavedDataType<GegarandeerdData> TYPE = GuhSavedData.tagType("gegarandeerd", GegarandeerdData::new, GegarandeerdData::load,
            GegarandeerdData::save);

    private final Map<String, GegarandeerdPlacement.Plek> plekken = new ConcurrentHashMap<>();

    public static GegarandeerdData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** The saved spot of this set (null: never found yet). */
    @Nullable
    public GegarandeerdPlacement.Plek plek(String set) {
        return plekken.get(set);
    }

    /** Remembers the spot of this set (the first one stays: a set never moves). Returns the spot that counts. */
    public GegarandeerdPlacement.Plek zet(String set, GegarandeerdPlacement.Plek plek) {
        GegarandeerdPlacement.Plek eerder = plekken.putIfAbsent(set, plek);
        if (eerder == null) {
            setDirty();
            return plek;
        }
        return eerder;
    }

    /** (tests, dev) forgets the spot of this set. */
    public void vergeet(String set) {
        if (plekken.remove(set) != null) {
            setDirty();
        }
    }

    /** Everything that is saved: set id -> spot. */
    public Map<String, GegarandeerdPlacement.Plek> alles() {
        return new TreeMap<>(plekken);
    }

    private CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag sets = new CompoundTag();
        plekken.forEach((set, plek) -> {
            CompoundTag p = new CompoundTag();
            p.putInt("X", plek.chunk().x());
            p.putInt("Z", plek.chunk().z());
            p.putInt("Vlak", plek.vlak());
            sets.put(set, p);
        });
        tag.put("Sets", sets);
        return tag;
    }

    private static GegarandeerdData load(CompoundTag tag) {
        GegarandeerdData data = new GegarandeerdData();
        CompoundTag sets = tag.getCompoundOrEmpty("Sets");
        for (String set : sets.keySet()) {
            CompoundTag p = sets.getCompoundOrEmpty(set);
            data.plekken.put(set, new GegarandeerdPlacement.Plek(new ChunkPos(p.getIntOr("X", 0), p.getIntOr("Z", 0)), Math.max(1, p.getIntOr("Vlak", 1))));
        }
        return data;
    }
}
