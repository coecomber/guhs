package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.feature.guhwinter.GuhwinterData;

/**
 * The lootjes of this world ({@code guhs:guhwinter_lootjes}, with the overworld): who signed up (first come, first served,
 * max {@link Lootjes#MAX}), the draw (giver -&gt; receiver, giver -&gt; plot), the givers' drawings, the captured gifts, the
 * sacks still waiting for their receiver, the letters waiting for a login, and a backup copy of every builder's stashed
 * survival things (the real stash lives in the player's own save: {@link Bouwkist}).
 */
public final class LootjesData extends SavedData {
    public static final SavedDataType<LootjesData> TYPE = GuhwinterData.type("lootjes", LootjesData::new, LootjesData::laad, LootjesData::opslaan);

    /** A name in Het Grote Boek; {@code nep} = a dev test name (no real player). */
    public record Deelnemer(UUID id, String naam, boolean nep) {
    }

    final List<Deelnemer> ingeschreven = new ArrayList<>();
    /** The draw happened (2 Dec); {@code mislukt}: there were fewer than two names, so no draw. */
    boolean getrokken, mislukt;
    /** The plots were captured (5 Dec 18:00); the sacks were handed out (5 Dec 19:00). */
    boolean gevangen, bezorgd;
    final Map<UUID, UUID> lootje = new LinkedHashMap<>();
    final Map<UUID, Integer> veld = new LinkedHashMap<>();
    final Map<UUID, UUID> tekening = new HashMap<>();
    final Map<UUID, CompoundTag> cadeau = new HashMap<>();
    final Set<UUID> teBezorgen = new LinkedHashSet<>();
    final Set<UUID> klaar = new HashSet<>();
    final Map<UUID, List<String>> brieven = new HashMap<>();
    final Map<UUID, CompoundTag> kist = new HashMap<>();

    public static LootjesData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // --- reading -----------------------------------------------------------------------------------------------------------

    @Nullable
    public Deelnemer deelnemer(UUID id) {
        for (Deelnemer d : ingeschreven) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }

    public boolean isIngeschreven(UUID id) {
        return deelnemer(id) != null;
    }

    public List<Deelnemer> ingeschreven() {
        return List.copyOf(ingeschreven);
    }

    /** The name of a participant (or "?"). */
    public String naam(@Nullable UUID id) {
        Deelnemer d = id == null ? null : deelnemer(id);
        return d == null ? "?" : d.naam();
    }

    @Nullable
    public UUID lootjeVan(UUID gever) {
        return lootje.get(gever);
    }

    /** Who drew this receiver (the maker of their gift), or null. */
    @Nullable
    public UUID geverVoor(UUID ontvanger) {
        for (var e : lootje.entrySet()) {
            if (e.getValue().equals(ontvanger)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** The plot of this giver (0..7), or -1. */
    public int veldVan(UUID gever) {
        return veld.getOrDefault(gever, -1);
    }

    /** The giver who builds on this plot, or null. */
    @Nullable
    public UUID eigenaarVanVeld(int i) {
        for (var e : veld.entrySet()) {
            if (e.getValue() == i) {
                return e.getKey();
            }
        }
        return null;
    }

    // --- writing -----------------------------------------------------------------------------------------------------------

    void brief(UUID speler, String soort) {
        brieven.computeIfAbsent(speler, k -> new ArrayList<>()).add(soort);
        setDirty();
    }

    void backup(UUID speler, @Nullable CompoundTag k) {
        if (k == null) {
            kist.remove(speler);
        } else {
            kist.put(speler, k.copy());
        }
        setDirty();
    }

    /** Everything back to empty (dev reset). */
    void wis() {
        ingeschreven.clear();
        getrokken = mislukt = gevangen = bezorgd = false;
        lootje.clear();
        veld.clear();
        tekening.clear();
        cadeau.clear();
        teBezorgen.clear();
        klaar.clear();
        brieven.clear();
        setDirty();
    }

    // --- saving ------------------------------------------------------------------------------------------------------------

    static CompoundTag opslaan(LootjesData d) {
        CompoundTag t = new CompoundTag();
        ListTag in = new ListTag();
        for (Deelnemer x : d.ingeschreven) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", x.id().toString());
            c.putString("Naam", x.naam());
            c.putBoolean("Nep", x.nep());
            in.add(c);
        }
        t.put("Ingeschreven", in);
        t.putBoolean("Getrokken", d.getrokken);
        t.putBoolean("Mislukt", d.mislukt);
        t.putBoolean("Gevangen", d.gevangen);
        t.putBoolean("Bezorgd", d.bezorgd);
        t.put("Lootje", uuidMap(d.lootje));
        CompoundTag v = new CompoundTag();
        d.veld.forEach((u, i) -> v.putInt(u.toString(), i));
        t.put("Veld", v);
        t.put("Tekening", uuidMap(d.tekening));
        CompoundTag c = new CompoundTag();
        d.cadeau.forEach((u, tag) -> c.put(u.toString(), tag.copy()));
        t.put("Cadeau", c);
        t.put("TeBezorgen", uuidLijst(d.teBezorgen));
        t.put("Klaar", uuidLijst(d.klaar));
        CompoundTag b = new CompoundTag();
        d.brieven.forEach((u, l) -> {
            ListTag lt = new ListTag();
            l.forEach(s -> lt.add(StringTag.valueOf(s)));
            b.put(u.toString(), lt);
        });
        t.put("Brieven", b);
        CompoundTag k = new CompoundTag();
        d.kist.forEach((u, tag) -> k.put(u.toString(), tag.copy()));
        t.put("Kist", k);
        return t;
    }

    static LootjesData laad(CompoundTag t) {
        LootjesData d = new LootjesData();
        for (Tag x : t.getListOrEmpty("Ingeschreven")) {
            if (x instanceof CompoundTag c) {
                UUID u = uuid(c.getStringOr("Id", ""));
                if (u != null) {
                    d.ingeschreven.add(new Deelnemer(u, c.getStringOr("Naam", "?"), c.getBooleanOr("Nep", false)));
                }
            }
        }
        d.getrokken = t.getBooleanOr("Getrokken", false);
        d.mislukt = t.getBooleanOr("Mislukt", false);
        d.gevangen = t.getBooleanOr("Gevangen", false);
        d.bezorgd = t.getBooleanOr("Bezorgd", false);
        leesUuidMap(t.getCompoundOrEmpty("Lootje"), d.lootje);
        CompoundTag v = t.getCompoundOrEmpty("Veld");
        for (String k : v.keySet()) {
            UUID u = uuid(k);
            if (u != null) {
                d.veld.put(u, v.getIntOr(k, -1));
            }
        }
        leesUuidMap(t.getCompoundOrEmpty("Tekening"), d.tekening);
        CompoundTag c = t.getCompoundOrEmpty("Cadeau");
        for (String k : c.keySet()) {
            UUID u = uuid(k);
            if (u != null) {
                d.cadeau.put(u, c.getCompoundOrEmpty(k));
            }
        }
        leesUuidLijst(t.getListOrEmpty("TeBezorgen"), d.teBezorgen);
        leesUuidLijst(t.getListOrEmpty("Klaar"), d.klaar);
        CompoundTag b = t.getCompoundOrEmpty("Brieven");
        for (String k : b.keySet()) {
            UUID u = uuid(k);
            if (u != null) {
                List<String> l = new ArrayList<>();
                for (Tag x : b.getListOrEmpty(k)) {
                    x.asString().ifPresent(l::add);
                }
                d.brieven.put(u, l);
            }
        }
        CompoundTag k = t.getCompoundOrEmpty("Kist");
        for (String s : k.keySet()) {
            UUID u = uuid(s);
            if (u != null) {
                d.kist.put(u, k.getCompoundOrEmpty(s));
            }
        }
        return d;
    }

    private static CompoundTag uuidMap(Map<UUID, UUID> m) {
        CompoundTag t = new CompoundTag();
        m.forEach((a, b) -> t.putString(a.toString(), b.toString()));
        return t;
    }

    private static void leesUuidMap(CompoundTag t, Map<UUID, UUID> m) {
        for (String k : t.keySet()) {
            UUID a = uuid(k), b = uuid(t.getStringOr(k, ""));
            if (a != null && b != null) {
                m.put(a, b);
            }
        }
    }

    private static ListTag uuidLijst(Set<UUID> s) {
        ListTag l = new ListTag();
        s.forEach(u -> l.add(StringTag.valueOf(u.toString())));
        return l;
    }

    private static void leesUuidLijst(ListTag l, Set<UUID> s) {
        for (Tag x : l) {
            x.asString().map(LootjesData::uuid).ifPresent(s::add);
        }
    }

    @Nullable
    static UUID uuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
