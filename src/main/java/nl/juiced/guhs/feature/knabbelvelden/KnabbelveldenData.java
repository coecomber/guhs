package nl.juiced.guhs.feature.knabbelvelden;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.feature.guhpixel.GuhOpslag;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The guhs in De Eeuwige Knabbelvelden, per owner (SavedData {@code guhs:knabbelvelden}): the stored guh
 * ({@link GuhOpslag#bewaar}: id, owner, name, looks, the whole entity), the day it went and its Guhdex number; and every
 * owner's own plot (an index, handed out in order, never reused). A stored guh is never also an entity or an item: the
 * record goes only after the item was made ({@link Pensioen#terug}).
 */
public final class KnabbelveldenData extends SavedData {
    public static final SavedDataType<KnabbelveldenData> TYPE = GuhSavedData.tagType("knabbelvelden", KnabbelveldenData::new,
            KnabbelveldenData::load, KnabbelveldenData::save);

    /** One retired guh: its stored self, since which day, its Guhdex number. */
    public record Rust(CompoundTag opslag, long sindsDag, int nr) {
        public UUID id() {
            return GuhOpslag.id(opslag);
        }

        /** 1.6.2: a maatje (pieppiepmuisje, Schilly, landdiertje...) is kept as its own pick-up item ({@link Vadsjes}). */
        public boolean isMaatje() {
            return opslag.contains(Vadsjes.ITEM);
        }

        /** "guh", or the maatje's kind (its entity id, like the Guhdex tab "Mijn andere vadsjes"). */
        public String soort() {
            return isMaatje() ? opslag.getStringOr(Vadsjes.SOORT, "") : "guh";
        }
    }

    private final Map<UUID, Map<UUID, Rust>> guhs = new HashMap<>();
    private final Map<UUID, Integer> plots = new HashMap<>();
    private int volgendePlot;

    public static KnabbelveldenData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** This owner's guhs in the Knabbelvelden, by number. */
    public List<Rust> van(UUID eigenaar) {
        List<Rust> uit = new ArrayList<>(guhs.getOrDefault(eigenaar, Map.of()).values());
        uit.sort(Comparator.comparingInt(Rust::nr));
        return uit;
    }

    @Nullable
    public Rust vind(UUID eigenaar, UUID id) {
        return guhs.getOrDefault(eigenaar, Map.of()).get(id);
    }

    /** Is this guh (any owner) in the Knabbelvelden? */
    public boolean bevat(UUID id) {
        for (Map<UUID, Rust> m : guhs.values()) {
            if (m.containsKey(id)) {
                return true;
            }
        }
        return false;
    }

    /** Every owner with at least one guh here. */
    public List<UUID> eigenaars() {
        List<UUID> uit = new ArrayList<>();
        guhs.forEach((e, m) -> {
            if (!m.isEmpty()) {
                uit.add(e);
            }
        });
        return uit;
    }

    void zet(UUID eigenaar, Rust rust) {
        guhs.computeIfAbsent(eigenaar, k -> new LinkedHashMap<>()).put(rust.id(), rust);
        setDirty();
    }

    @Nullable
    Rust haalWeg(UUID eigenaar, UUID id) {
        Map<UUID, Rust> m = guhs.get(eigenaar);
        Rust r = m == null ? null : m.remove(id);
        if (r != null) {
            setDirty();
        }
        return r;
    }

    /** This owner's plot index (a new one the first time). */
    public int plot(UUID eigenaar) {
        Integer p = plots.get(eigenaar);
        if (p == null) {
            p = volgendePlot++;
            plots.put(eigenaar, p);
            setDirty();
        }
        return p;
    }

    /** Whose plot this index is (null: nobody's yet). */
    @Nullable
    public UUID vanPlot(int plot) {
        for (Map.Entry<UUID, Integer> e : plots.entrySet()) {
            if (e.getValue() == plot) {
                return e.getKey();
            }
        }
        return null;
    }

    private CompoundTag save() {
        CompoundTag out = new CompoundTag();
        CompoundTag e = new CompoundTag();
        guhs.forEach((eigenaar, m) -> {
            ListTag lijst = new ListTag();
            for (Rust r : m.values()) {
                CompoundTag t = new CompoundTag();
                t.put("Opslag", r.opslag());
                t.putLong("Sinds", r.sindsDag());
                t.putInt("Nr", r.nr());
                lijst.add(t);
            }
            e.put(eigenaar.toString(), lijst);
        });
        out.put("Guhs", e);
        CompoundTag p = new CompoundTag();
        plots.forEach((eigenaar, n) -> p.putInt(eigenaar.toString(), n));
        out.put("Plots", p);
        out.putInt("Volgende", volgendePlot);
        return out;
    }

    private static KnabbelveldenData load(CompoundTag tag) {
        KnabbelveldenData d = new KnabbelveldenData();
        CompoundTag e = tag.getCompoundOrEmpty("Guhs");
        for (String key : e.keySet()) {
            try {
                UUID eigenaar = UUID.fromString(key);
                Map<UUID, Rust> m = new LinkedHashMap<>();
                for (Tag raw : e.getListOrEmpty(key)) {
                    CompoundTag t = (CompoundTag) raw;
                    Rust r = new Rust(t.getCompoundOrEmpty("Opslag"), t.getLongOr("Sinds", 0L), t.getIntOr("Nr", 0));
                    if (r.id() != null) {
                        m.put(r.id(), r);
                    }
                }
                d.guhs.put(eigenaar, m);
            } catch (IllegalArgumentException ignored) {
                // (not a UUID: skip it)
            }
        }
        CompoundTag p = tag.getCompoundOrEmpty("Plots");
        for (String key : p.keySet()) {
            try {
                d.plots.put(UUID.fromString(key), p.getIntOr(key, 0));
            } catch (IllegalArgumentException ignored) {
                // (not a UUID)
            }
        }
        d.volgendePlot = tag.getIntOr("Volgende", d.plots.size());
        return d;
    }
}
