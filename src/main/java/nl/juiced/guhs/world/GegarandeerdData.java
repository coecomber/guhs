package nl.juiced.guhs.world;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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
 * <p>
 * 1.6.5: EVERY guaranteed set of the dimension saves its spot here now, also the plain ones (whose spot used to be worked
 * out again at every start): a later version that changes the terrain (1.7.0: the Guhmension) would move those spots
 * (compasses pointing at nothing, a second copy in new chunks). {@code AllesBewaard} ({@link #allesBewaard}) is set once
 * the search thread has looked for every guaranteed set of this dimension in the level's own world and every set that
 * found a spot has it saved here; the sets that found NO spot then are listed in {@code GeenPlek} ({@link #geenPlek}), so
 * they don't block the marker for ever (no copy of them stands anywhere, so nothing can move). That list only shrinks: a
 * set of it that gets a spot later (an alleen_nieuw set in new terrain, or an operator's /guhs bouwcheck plek) leaves it.
 * A set that is neither saved nor listed was not known when the marker was set (added by a later version).
 * <p>
 * NBT: {@code {Sets:{"guhs:<set>":{X,Z,Vlak}}, AllesBewaard:1b, GeenPlek:["guhs:<set>", ...]}} (the last two only once marked).
 */
public class GegarandeerdData extends SavedData {
    public static final SavedDataType<GegarandeerdData> TYPE = GuhSavedData.tagType("gegarandeerd", GegarandeerdData::new, GegarandeerdData::load,
            GegarandeerdData::save);

    private final Map<String, GegarandeerdPlacement.Plek> plekken = new ConcurrentHashMap<>();
    /** 1.6.5: every guaranteed set of this dimension has been searched once and the found spots are saved (see the class doc). */
    private volatile boolean allesBewaard;
    /** 1.6.5: the sets that had no spot when {@link #allesBewaard} was set (only shrinks). */
    private final Set<String> geenPlek = ConcurrentHashMap.newKeySet();

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
            geenPlek.remove(set);
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

    /**
     * 1.6.5: has every guaranteed set of this dimension been searched once in this world, with every spot that was found
     * saved here? (1.7.0 checks this before it trusts that no spot can move with the new terrain.)
     */
    public boolean allesBewaard() {
        return allesBewaard;
    }

    /** 1.6.5: the sets that had no spot when {@link #allesBewaard} was set, minus the ones that got one since (sorted). */
    public Set<String> geenPlek() {
        return new TreeSet<>(geenPlek);
    }

    /**
     * 1.6.5 (the search thread, after a complete search of the level's own world): sets the marker, with the sets that
     * found no spot. Only the first time: the list of a marked world only shrinks ({@link #zet}). Returns whether it was
     * set now.
     */
    public boolean markeerAllesBewaard(java.util.Collection<String> zonderPlek) {
        synchronized (geenPlek) {
            if (allesBewaard) {
                return false;
            }
            for (String set : zonderPlek) {
                if (!plekken.containsKey(set)) {
                    geenPlek.add(set);
                }
            }
            allesBewaard = true;
        }
        setDirty();
        return true;
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
        if (allesBewaard) {
            tag.putBoolean("AllesBewaard", true);
            ListTag geen = new ListTag();
            new TreeSet<>(geenPlek).forEach(set -> geen.add(StringTag.valueOf(set)));
            tag.put("GeenPlek", geen);
        }
        return tag;
    }

    private static GegarandeerdData load(CompoundTag tag) {
        GegarandeerdData data = new GegarandeerdData();
        CompoundTag sets = tag.getCompoundOrEmpty("Sets");
        for (String set : sets.keySet()) {
            CompoundTag p = sets.getCompoundOrEmpty(set);
            data.plekken.put(set, new GegarandeerdPlacement.Plek(new ChunkPos(p.getIntOr("X", 0), p.getIntOr("Z", 0)), Math.max(1, p.getIntOr("Vlak", 1))));
        }
        data.allesBewaard = tag.getBooleanOr("AllesBewaard", false);
        for (Tag t : tag.getListOrEmpty("GeenPlek")) {
            t.asString().filter(set -> !data.plekken.containsKey(set)).ifPresent(data.geenPlek::add);
        }
        return data;
    }
}
