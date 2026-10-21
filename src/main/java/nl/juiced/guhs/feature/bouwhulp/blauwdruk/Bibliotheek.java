package nl.juiced.guhs.feature.bouwhulp.blauwdruk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The server's blueprint library (design 2.6): anybody publishes freely from a Sauskeet (level 3), with their name on it,
 * at most {@link #MAX_PER_SPELER} each; everybody can build them and give a thumbs up; only the maker or an op removes
 * one. The size is checked when publishing (the publishing keet's level).
 */
public final class Bibliotheek extends SavedData {
    public static final SavedDataType<Bibliotheek> TYPE = GuhSavedData.tagType("bouwhulp_bibliotheek", Bibliotheek::new, Bibliotheek::laad, Bibliotheek::opslaan);
    public static final int MAX_PER_SPELER = 10;

    public enum Uitkomst { OK, VOL, AL, LEEG }

    private final Map<String, Blauwdruk> ontwerpen = new LinkedHashMap<>();

    public Bibliotheek() {
    }

    public static Bibliotheek van(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Most thumbs first, then newest. */
    public List<Blauwdruk> lijst() {
        List<Blauwdruk> l = new ArrayList<>(ontwerpen.values());
        l.sort(Comparator.comparingInt(Blauwdruk::duimpjes).reversed().thenComparing(Comparator.comparingLong((Blauwdruk b) -> b.datum).reversed()));
        return l;
    }

    @Nullable
    public Blauwdruk get(String id) {
        return ontwerpen.get(id);
    }

    public int vanSpeler(UUID u) {
        return (int) ontwerpen.values().stream().filter(b -> u.equals(b.maker)).count();
    }

    /** Publishes a copy of an own blueprint (a new id; the same picture). */
    public Uitkomst publiceer(Blauwdruk eigen, UUID maker, String makerNaam) {
        if (eigen.ontwerp.isLeeg()) {
            return Uitkomst.LEEG;
        }
        if (vanSpeler(maker) >= MAX_PER_SPELER) {
            return Uitkomst.VOL;
        }
        for (Blauwdruk b : ontwerpen.values()) {
            if (maker.equals(b.maker) && b.naam.equals(eigen.naam) && b.ontwerp.aantal() == eigen.ontwerp.aantal()) {
                return Uitkomst.AL;
            }
        }
        Blauwdruk b = new Blauwdruk(UUID.randomUUID().toString(), eigen.naam, maker, makerNaam, System.currentTimeMillis(), eigen.ontwerp, eigen.voorbeeld);
        ontwerpen.put(b.id, b);
        setDirty();
        return Uitkomst.OK;
    }

    /** Thumbs up on or off; true = now on. */
    public boolean duimpje(String id, UUID u) {
        Blauwdruk b = ontwerpen.get(id);
        if (b == null) {
            return false;
        }
        boolean aan = !b.duimpjes.remove(u);
        if (aan) {
            b.duimpjes.add(u);
        }
        setDirty();
        return aan;
    }

    /** Removes it, when the maker (or an op) asks. */
    public boolean verwijder(String id, UUID wie, boolean op) {
        Blauwdruk b = ontwerpen.get(id);
        if (b == null || !(op || wie.equals(b.maker))) {
            return false;
        }
        ontwerpen.remove(id);
        setDirty();
        return true;
    }

    private CompoundTag opslaan() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        ontwerpen.values().forEach(b -> l.add(b.opslaan()));
        t.put("Ontwerpen", l);
        return t;
    }

    private static Bibliotheek laad(CompoundTag t) {
        Bibliotheek d = new Bibliotheek();
        ListTag l = t.getListOrEmpty("Ontwerpen");
        for (int i = 0; i < l.size(); i++) {
            Blauwdruk b = Blauwdruk.laad(l.getCompoundOrEmpty(i));
            d.ontwerpen.put(b.id, b);
        }
        return d;
    }
}
