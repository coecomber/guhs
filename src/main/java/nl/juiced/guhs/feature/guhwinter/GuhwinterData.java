package nl.juiced.guhs.feature.guhwinter;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.10.9 Guhwinter: the event's shared SavedData {@code guhs:guhwinter} (with the overworld).
 * <ul>
 *   <li>kern: the dev date override (as an offset to the real clock, so it keeps running), the moments that have fired
 *       ({@link Guhwinter#opMoment}), per player the last phase they were told about;</li>
 *   <li>for every slice: {@link #sectie} — a CompoundTag of its own that is saved with this file (call {@link #opslaan} after
 *       changing it), or {@link #type} for a SavedData class of its own named {@code guhs:guhwinter_<naam>}.</li>
 * </ul>
 */
public final class GuhwinterData extends SavedData {
    public static final SavedDataType<GuhwinterData> TYPE = GuhSavedData.tagType("guhwinter", GuhwinterData::new, GuhwinterData::load, GuhwinterData::save);

    /** Dev override: added to the real clock (ms); null = the real date. */
    @Nullable
    Long verschuiving;
    /** The listener ids of {@link Guhwinter#opMoment} that have fired (in firing order). */
    final Set<String> gevuurd = new LinkedHashSet<>();
    /** Per player: the ordinal of the last phase they got the "new phase" message of. */
    final Map<UUID, Integer> gezien = new HashMap<>();
    /** The slices' own tags ({@link #sectie}). */
    final CompoundTag secties = new CompoundTag();

    public static GuhwinterData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * A slice's own part of this file (made when missing). Change it in place and call {@link #opslaan}; it is written with
     * the world. Use a name per slice ("lootjes", "verhaal", ...).
     */
    public static CompoundTag sectie(MinecraftServer server, String naam) {
        GuhwinterData d = get(server);
        if (d.secties.get(naam) instanceof CompoundTag t) {
            return t;
        }
        CompoundTag t = new CompoundTag();
        d.secties.put(naam, t);
        d.setDirty();
        return t;
    }

    /** After changing a {@link #sectie}: write it with the next save. */
    public static void opslaan(MinecraftServer server) {
        get(server).setDirty();
    }

    /** A SavedDataType {@code guhs:guhwinter_<naam>} for a slice that wants a class of its own (tag based, like GuhSavedData.tagType). */
    public static <T extends SavedData> SavedDataType<T> type(String naam, Supplier<T> nieuw, Function<CompoundTag, T> laad, Function<T, CompoundTag> bewaar) {
        return GuhSavedData.tagType("guhwinter_" + naam, nieuw, laad, bewaar);
    }

    /** (kern) the ordinal of the last phase this player got the "new phase" message of (-1: none yet). */
    public int gezien(UUID speler) {
        return gezien.getOrDefault(speler, -1);
    }

    /** (kern) remembers that this player got the message of this phase. */
    public void zetGezien(UUID speler, GuhwinterFase fase) {
        if (gezien(speler) < fase.ordinal()) {
            gezien.put(speler, fase.ordinal());
            setDirty();
        }
    }

    /** Has this listener of {@link Guhwinter#opMoment} fired (in this world)? */
    public boolean gevuurd(String id) {
        return gevuurd.contains(id);
    }

    // --- saving ----------------------------------------------------------------------------------------------------------

    static CompoundTag save(GuhwinterData d) {
        CompoundTag t = new CompoundTag();
        if (d.verschuiving != null) {
            t.putLong("Verschuiving", d.verschuiving);
        }
        ListTag g = new ListTag();
        d.gevuurd.forEach(id -> g.add(StringTag.valueOf(id)));
        t.put("Gevuurd", g);
        CompoundTag z = new CompoundTag();
        d.gezien.forEach((u, f) -> z.putInt(u.toString(), f));
        t.put("Gezien", z);
        t.put("Secties", d.secties.copy());
        return t;
    }

    static GuhwinterData load(CompoundTag t) {
        GuhwinterData d = new GuhwinterData();
        d.verschuiving = t.getLong("Verschuiving").orElse(null);
        for (Tag x : t.getListOrEmpty("Gevuurd")) {
            x.asString().ifPresent(d.gevuurd::add);
        }
        CompoundTag z = t.getCompoundOrEmpty("Gezien");
        for (String k : z.keySet()) {
            try {
                d.gezien.put(UUID.fromString(k), z.getIntOr(k, 0));
            } catch (IllegalArgumentException ignored) {
                // (not a player id)
            }
        }
        d.secties.merge(t.getCompoundOrEmpty("Secties"));
        return d;
    }
}
