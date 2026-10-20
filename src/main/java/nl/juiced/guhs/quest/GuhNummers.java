package nl.juiced.guhs.quest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.6.0 (F3b): every tamed guh has a fixed number in its owner's Guhdex: 1, 2, 3... in the order they were tamed, never
 * reused (a guh that goes to the wolkjes keeps its number). Shown in "Mijn guhs" and on the picked-up guh's tooltip.
 * <p>
 * Kept per owner (SavedData {@code guhs:guh_nummers}): band id -> number, and the next number. Guhs that were tamed before
 * numbering existed, or are missing for any other reason (a tamed baby), get numbers the first time anything asks: in the
 * order of the day they became yours ({@code BandData.Rec.sindsDag}), then in band order. A guh that is tamed gets its
 * number right away ({@link #getemd}), so later ones always come after it.
 */
public final class GuhNummers extends SavedData {
    public static final SavedDataType<GuhNummers> TYPE = GuhSavedData.tagType("guh_nummers", GuhNummers::new, GuhNummers::load, GuhNummers::save);

    private final Map<UUID, Map<UUID, Integer>> nummers = new HashMap<>();
    private final Map<UUID, Integer> volgende = new HashMap<>();

    public static GuhNummers get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The number of this owner's guh (0 when it is not one of their band guhs). Fills in missing numbers first. */
    public static int nummer(MinecraftServer server, UUID eigenaar, UUID guh) {
        GuhNummers data = get(server);
        Integer nr = data.nummers.getOrDefault(eigenaar, Map.of()).get(guh);
        if (nr != null) {
            return nr;
        }
        data.vulAan(server, eigenaar);
        return data.nummers.getOrDefault(eigenaar, Map.of()).getOrDefault(guh, 0);
    }

    /** A guh was just tamed: it gets the next number now (after the older guhs got theirs). */
    public static int getemd(MinecraftServer server, UUID eigenaar, UUID guh) {
        GuhNummers data = get(server);
        data.vulAan(server, eigenaar);
        Map<UUID, Integer> eigen = data.nummers.computeIfAbsent(eigenaar, k -> new HashMap<>());
        Integer al = eigen.get(guh);
        if (al != null) {
            return al;
        }
        int nr = data.volgende.getOrDefault(eigenaar, 1);
        eigen.put(guh, nr);
        data.volgende.put(eigenaar, nr + 1);
        data.setDirty();
        return nr;
    }

    /** Numbers for every band guh of this owner that has none yet, in the order they became yours. */
    private void vulAan(MinecraftServer server, UUID eigenaar) {
        Map<UUID, Integer> eigen = nummers.computeIfAbsent(eigenaar, k -> new HashMap<>());
        List<BandData.Rec> zonder = new ArrayList<>();
        for (BandData.Rec r : BandData.get(server).guhsVan(eigenaar)) {
            if (!eigen.containsKey(r.id)) {
                zonder.add(r);
            }
        }
        if (zonder.isEmpty()) {
            return;
        }
        // (a stable sort: same day keeps the band order)
        zonder.sort(Comparator.comparingLong(r -> r.sindsDag < 0 ? Long.MAX_VALUE : r.sindsDag));
        int nr = volgende.getOrDefault(eigenaar, 1);
        for (BandData.Rec r : zonder) {
            eigen.put(r.id, nr++);
        }
        volgende.put(eigenaar, nr);
        setDirty();
    }

    private CompoundTag save() {
        CompoundTag out = new CompoundTag();
        CompoundTag e = new CompoundTag();
        nummers.forEach((eigenaar, m) -> {
            CompoundTag t = new CompoundTag();
            m.forEach((guh, nr) -> t.putInt(guh.toString(), nr));
            CompoundTag o = new CompoundTag();
            o.put("Guhs", t);
            o.putInt("Volgende", volgende.getOrDefault(eigenaar, 1));
            e.put(eigenaar.toString(), o);
        });
        out.put("Eigenaars", e);
        return out;
    }

    private static GuhNummers load(CompoundTag tag) {
        GuhNummers data = new GuhNummers();
        CompoundTag e = tag.getCompoundOrEmpty("Eigenaars");
        for (String key : e.keySet()) {
            try {
                UUID eigenaar = UUID.fromString(key);
                CompoundTag o = e.getCompoundOrEmpty(key);
                Map<UUID, Integer> m = new HashMap<>();
                CompoundTag t = o.getCompoundOrEmpty("Guhs");
                for (String g : t.keySet()) {
                    m.put(UUID.fromString(g), t.getIntOr(g, 0));
                }
                data.nummers.put(eigenaar, m);
                data.volgende.put(eigenaar, o.getIntOr("Volgende", 1));
            } catch (IllegalArgumentException ignored) {
                // (not a UUID: skip it)
            }
        }
        return data;
    }
}
