package nl.juiced.guhs.feature.einde2.trap5;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Where every Thuispoort stands (SavedData {@code guhs:einde2_trap5_machines_thuispoorten} in the overworld): its id, its
 * owner, its spot in any dimension, its name and its icon (an item id), and per owner the players they let in (DESIGN 6:
 * "alleen jij en wie je toelaat"). The network is personal and has no size limit; what a jump costs grows with the number of
 * poorten of the owner ({@link #kosten}).
 */
public final class Thuispoorten extends SavedData {
    public static final SavedDataType<Thuispoorten> TYPE = GuhSavedData.tagType("einde2_trap5_machines_thuispoorten", Thuispoorten::new,
            Thuispoorten::laad, t -> t.bewaar(new CompoundTag()));
    public static final int MAX_NAAM = 24;

    /** One poort of the network. */
    public static final class Poort {
        public final UUID id;
        public final UUID eigenaar;
        public GlobalPos plek;
        public String naam;
        public String icoon;

        Poort(UUID id, UUID eigenaar, GlobalPos plek, String naam, String icoon) {
            this.id = id;
            this.eigenaar = eigenaar;
            this.plek = plek;
            this.naam = naam;
            this.icoon = icoon;
        }
    }

    private final Map<UUID, Poort> poorten = new LinkedHashMap<>();
    /** owner -> (a player they let in -> that player's name, for the screen). */
    private final Map<UUID, Map<UUID, String>> toegelaten = new LinkedHashMap<>();

    public Thuispoorten() {
    }

    public static Thuispoorten van(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    @Nullable
    public Poort poort(UUID id) {
        return poorten.get(id);
    }

    /** Puts a poort in (or moves it) and returns it. */
    public Poort zet(UUID id, UUID eigenaar, GlobalPos plek, String naam, String icoon) {
        Poort p = poorten.get(id);
        if (p == null) {
            p = new Poort(id, eigenaar, plek, naam, icoon);
            poorten.put(id, p);
        } else {
            p.plek = plek;
        }
        setDirty();
        return p;
    }

    public void wis(UUID id) {
        if (poorten.remove(id) != null) {
            setDirty();
        }
    }

    public void hernoem(UUID id, String naam) {
        Poort p = poorten.get(id);
        if (p != null) {
            p.naam = naam.length() > MAX_NAAM ? naam.substring(0, MAX_NAAM) : naam;
            setDirty();
        }
    }

    public void icoon(UUID id, String icoon) {
        Poort p = poorten.get(id);
        if (p != null) {
            p.icoon = icoon;
            setDirty();
        }
    }

    /** The poorten of this owner. */
    public List<Poort> van(UUID eigenaar) {
        List<Poort> uit = new ArrayList<>();
        for (Poort p : poorten.values()) {
            if (p.eigenaar.equals(eigenaar)) {
                uit.add(p);
            }
        }
        return uit;
    }

    /** May this player use poorten of this owner (the owner himself, or let in)? */
    public boolean mag(UUID eigenaar, UUID speler) {
        return eigenaar.equals(speler) || toegelaten.getOrDefault(eigenaar, Map.of()).containsKey(speler);
    }

    /** Every poort this player may jump to. */
    public List<Poort> bereikbaar(UUID speler) {
        List<Poort> uit = new ArrayList<>();
        for (Poort p : poorten.values()) {
            if (mag(p.eigenaar, speler)) {
                uit.add(p);
            }
        }
        return uit;
    }

    public Map<UUID, String> toegelaten(UUID eigenaar) {
        return toegelaten.getOrDefault(eigenaar, Map.of());
    }

    /** Lets this player in (true) or out (false). */
    public void laatToe(UUID eigenaar, UUID speler, String naam, boolean toe) {
        Map<UUID, String> m = toegelaten.computeIfAbsent(eigenaar, k -> new LinkedHashMap<>());
        if (toe) {
            m.put(speler, naam);
        } else {
            m.remove(speler);
        }
        setDirty();
    }

    /** What a jump with a poort of this owner costs (VK): the base plus a bit for every other poort of the owner. */
    public int kosten(UUID eigenaar) {
        return T5Getallen.THUIS_BASIS + T5Getallen.THUIS_PER_POORT * Math.max(0, van(eigenaar).size() - 1);
    }

    // =====================================================================================================================
    // saving
    // =====================================================================================================================

    private static Thuispoorten laad(CompoundTag tag) {
        Thuispoorten t = new Thuispoorten();
        ListTag lijst = tag.getListOrEmpty("Poorten");
        for (int i = 0; i < lijst.size(); i++) {
            CompoundTag c = lijst.getCompoundOrEmpty(i);
            UUID id = uuid(c.getStringOr("Id", ""));
            UUID eigenaar = uuid(c.getStringOr("Eigenaar", ""));
            Identifier dim = Identifier.tryParse(c.getStringOr("Dim", ""));
            if (id == null || eigenaar == null || dim == null) {
                continue;
            }
            GlobalPos plek = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(c.getLongOr("Pos", 0L)));
            t.poorten.put(id, new Poort(id, eigenaar, plek, c.getStringOr("Naam", ""), c.getStringOr("Icoon", "guhs:thuispoort")));
        }
        CompoundTag toe = tag.getCompoundOrEmpty("Toegelaten");
        for (String e : toe.keySet()) {
            UUID eigenaar = uuid(e);
            if (eigenaar == null) {
                continue;
            }
            CompoundTag m = toe.getCompoundOrEmpty(e);
            Map<UUID, String> namen = new LinkedHashMap<>();
            for (String s : m.keySet()) {
                UUID speler = uuid(s);
                if (speler != null) {
                    namen.put(speler, m.getStringOr(s, ""));
                }
            }
            t.toegelaten.put(eigenaar, namen);
        }
        return t;
    }

    private CompoundTag bewaar(CompoundTag tag) {
        ListTag lijst = new ListTag();
        for (Poort p : poorten.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", p.id.toString());
            c.putString("Eigenaar", p.eigenaar.toString());
            c.putString("Dim", p.plek.dimension().identifier().toString());
            c.putLong("Pos", p.plek.pos().asLong());
            c.putString("Naam", p.naam);
            c.putString("Icoon", p.icoon);
            lijst.add(c);
        }
        tag.put("Poorten", lijst);
        CompoundTag toe = new CompoundTag();
        for (Map.Entry<UUID, Map<UUID, String>> e : toegelaten.entrySet()) {
            CompoundTag m = new CompoundTag();
            e.getValue().forEach((u, n) -> m.putString(u.toString(), n));
            toe.put(e.getKey().toString(), m);
        }
        tag.put("Toegelaten", toe);
        return tag;
    }

    @Nullable
    private static UUID uuid(String s) {
        try {
            return s.isEmpty() ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
