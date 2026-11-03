package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The arenas' respawn timers, saved with the overworld ({@code data/guhs/vadsrijk_arenas.dat}): per arena id the game time
 * at which its boss comes back (absent = no timer running, the boss may spawn). So a restart doesn't bring a just-beaten boss
 * back at once. (The boss itself is saved with its chunk; parties are not saved.)
 */
public final class ArenaData extends SavedData {
    static final SavedDataType<ArenaData> TYPE = GuhSavedData.tagType("vadsrijk_arenas", ArenaData::new, ArenaData::load, ArenaData::save);

    private final Map<String, Long> respawnOp = new HashMap<>();

    public static ArenaData get(MinecraftServer server) {
        return GuhSavedData.get(server.overworld(), TYPE, "guhs_vadsrijk_arenas");
    }

    /** The saved respawn time of this arena, or -1. */
    public long respawnOp(String arena) {
        return respawnOp.getOrDefault(arena, -1L);
    }

    public void zet(String arena, long tijd) {
        Long oud = tijd < 0 ? respawnOp.remove(arena) : respawnOp.put(arena, tijd);
        if (oud == null || oud != tijd) {
            setDirty();
        }
    }

    private static ArenaData load(CompoundTag t) {
        ArenaData d = new ArenaData();
        for (String k : t.keySet()) {
            d.respawnOp.put(k, t.getLongOr(k, -1L));
        }
        return d;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        respawnOp.forEach(t::putLong);
        return t;
    }
}
