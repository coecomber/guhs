package nl.juiced.guhs.feature.bouwhulp.blauwdruk;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;

/**
 * A blueprint (design 2.6): an ontwerp with a name, its maker and a small picture ({@link Voorbeeld}). Own blueprints live
 * with their player ({@code BouwSpelers}), published ones in the server's {@link Bibliotheek} (with thumbs up), the ready
 * ones in the {@link Catalogus}.
 */
public final class Blauwdruk {
    public final String id;
    String naam;
    @Nullable
    public final UUID maker;
    public final String makerNaam;
    public final long datum;
    public final Ontwerp ontwerp;
    public final int[] voorbeeld;
    final Set<UUID> duimpjes = new HashSet<>();

    public Blauwdruk(String id, String naam, @Nullable UUID maker, String makerNaam, long datum, Ontwerp ontwerp, int[] voorbeeld) {
        this.id = id;
        this.naam = naam;
        this.maker = maker;
        this.makerNaam = makerNaam;
        this.datum = datum;
        this.ontwerp = ontwerp;
        this.voorbeeld = voorbeeld;
    }

    public static Blauwdruk nieuw(String naam, @Nullable UUID maker, String makerNaam, Ontwerp ontwerp) {
        return new Blauwdruk(UUID.randomUUID().toString(), naam, maker, makerNaam, System.currentTimeMillis(), ontwerp, Voorbeeld.maak(ontwerp));
    }

    public String naam() {
        return naam;
    }

    public int duimpjes() {
        return duimpjes.size();
    }

    public boolean heeftDuimpje(UUID u) {
        return duimpjes.contains(u);
    }

    public CompoundTag opslaan() {
        CompoundTag t = new CompoundTag();
        t.putString("Id", id);
        t.putString("Naam", naam);
        if (maker != null) {
            t.store("Maker", UUIDUtil.CODEC, maker);
        }
        t.putString("MakerNaam", makerNaam);
        t.putLong("Datum", datum);
        t.put("Ontwerp", ontwerp.opslaan());
        t.put("Voorbeeld", new IntArrayTag(voorbeeld));
        ListTag d = new ListTag();
        duimpjes.forEach(u -> d.add(net.minecraft.nbt.StringTag.valueOf(u.toString())));
        t.put("Duimpjes", d);
        return t;
    }

    public static Blauwdruk laad(CompoundTag t) {
        Ontwerp o = Ontwerp.laad(t.getCompoundOrEmpty("Ontwerp"));
        int[] v = t.getIntArray("Voorbeeld").orElse(null);
        Blauwdruk b = new Blauwdruk(t.getStringOr("Id", UUID.randomUUID().toString()), t.getStringOr("Naam", "?"), t.read("Maker", UUIDUtil.CODEC).orElse(null),
                t.getStringOr("MakerNaam", "?"), t.getLongOr("Datum", 0L), o, v != null && v.length == Voorbeeld.N * Voorbeeld.N ? v : Voorbeeld.maak(o));
        ListTag d = t.getListOrEmpty("Duimpjes");
        for (int i = 0; i < d.size(); i++) {
            try {
                b.duimpjes.add(UUID.fromString(d.getStringOr(i, "")));
            } catch (IllegalArgumentException ignored) {
                // a broken entry: skip it
            }
        }
        return b;
    }
}
