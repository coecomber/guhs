package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/** Every player's Sherlock progress ({@link Voortgang}), saved with the overworld ({@code data/guhs/erfenis_sherlock.dat}). */
public final class SherlockData extends SavedData {
    static final SavedDataType<SherlockData> TYPE = GuhSavedData.tagType("erfenis_sherlock", SherlockData::new, SherlockData::load, SherlockData::save);

    private final Map<UUID, Voortgang> spelers = new HashMap<>();

    public static SherlockData get(MinecraftServer server) {
        return GuhSavedData.get(server.overworld(), TYPE, "guhs_erfenis_sherlock");
    }

    public static Voortgang van(MinecraftServer server, UUID speler) {
        return get(server).spelers.computeIfAbsent(speler, k -> new Voortgang());
    }

    /** This player's progress if they have any (null: never started; nothing is made). */
    @javax.annotation.Nullable
    public static Voortgang als(MinecraftServer server, UUID speler) {
        return get(server).spelers.get(speler);
    }

    public static void veranderd(MinecraftServer server) {
        get(server).setDirty();
    }

    /** (dev reset) forgets this player's progress. */
    public void vergeet(UUID speler) {
        spelers.remove(speler);
        setDirty();
    }

    private static SherlockData load(CompoundTag t) {
        SherlockData d = new SherlockData();
        CompoundTag s = t.getCompoundOrEmpty("Spelers");
        for (String k : s.keySet()) {
            try {
                d.spelers.put(UUID.fromString(k), Voortgang.load(s.getCompoundOrEmpty(k)));
            } catch (IllegalArgumentException e) {
                // (not a uuid: skip)
            }
        }
        return d;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        CompoundTag s = new CompoundTag();
        spelers.forEach((id, v) -> s.put(id.toString(), v.save()));
        t.put("Spelers", s);
        return t;
    }
}
