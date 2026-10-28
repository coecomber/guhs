package nl.juiced.guhs.feature.hamsterbuis;

import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.droomwolkje.Wolkjes;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The hamsterbuis network (SavedData {@code guhs:hamsterbuis_net} in the overworld): every station (its id, owner, spot in
 * any dimension, name and whether it is open to everyone), and per owner the Hamsterknooppunt (where it stands) and its
 * buffer of vadskracht (VK). A trip costs {@link HamsterbuisFeature#REIS} VK from the owner's buffer; an empty buffer (or
 * no knooppunt) and the owner's whole network stands still. The buffer lives here, not in the block, so it works while the
 * knooppunt's chunk is unloaded (you travel home from far away).
 */
public final class HamsterNet extends SavedData {
    public static final SavedDataType<HamsterNet> TYPE = GuhSavedData.tagType("hamsterbuis_net", HamsterNet::new, HamsterNet::laad,
            n -> n.bewaar(new CompoundTag()));
    public static final int MAX_NAAM = 24;

    /** One station. */
    public static final class Station {
        public final UUID id;
        public final UUID eigenaar;
        public GlobalPos plek;
        public String naam;
        public boolean openbaar;

        Station(UUID id, UUID eigenaar, GlobalPos plek, String naam, boolean openbaar) {
            this.id = id;
            this.eigenaar = eigenaar;
            this.plek = plek;
            this.naam = naam;
            this.openbaar = openbaar;
        }
    }

    private final Map<UUID, Station> stations = new LinkedHashMap<>();
    /** owner -> where their Hamsterknooppunt stands. */
    private final Map<UUID, GlobalPos> knooppunten = new HashMap<>();
    /** owner -> the VK in their knooppunt's buffer. */
    private final Map<UUID, Long> lading = new HashMap<>();

    public HamsterNet() {
    }

    public static HamsterNet van(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // --- stations ---

    @Nullable
    public Station station(UUID id) {
        return stations.get(id);
    }

    public Station zet(UUID id, UUID eigenaar, GlobalPos plek, String naam) {
        Station s = stations.get(id);
        if (s == null) {
            s = new Station(id, eigenaar, plek, naam, false);
            stations.put(id, s);
        } else {
            s.plek = plek;
        }
        setDirty();
        return s;
    }

    public void wis(UUID id) {
        if (stations.remove(id) != null) {
            setDirty();
        }
    }

    public void hernoem(UUID id, String naam) {
        Station s = stations.get(id);
        if (s != null) {
            s.naam = naam.length() > MAX_NAAM ? naam.substring(0, MAX_NAAM) : naam;
            setDirty();
        }
    }

    public void zetOpenbaar(UUID id, boolean open) {
        Station s = stations.get(id);
        if (s != null) {
            s.openbaar = open;
            setDirty();
        }
    }

    public int aantal(UUID eigenaar) {
        int n = 0;
        for (Station s : stations.values()) {
            if (s.eigenaar.equals(eigenaar)) {
                n++;
            }
        }
        return n;
    }

    /**
     * May this player use this station: the owner, anyone when it is open (openbaar), and the owner's friends of their
     * droomwolkje (the one friends list the mod has, 1.8.0).
     */
    public static boolean mag(MinecraftServer server, Station s, UUID speler) {
        if (s.eigenaar.equals(speler) || s.openbaar) {
            return true;
        }
        ServerLevel droom = server.getLevel(DromenFeature.DIM);
        if (droom == null) {
            return false;
        }
        Wolkjes.Wolkje w = Wolkjes.stand(droom).van(s.eigenaar);
        return w != null && w.vrienden().contains(speler);
    }

    /** Every station this player may travel to (in any dimension). */
    public List<Station> bereikbaar(MinecraftServer server, UUID speler) {
        List<Station> uit = new ArrayList<>();
        for (Station s : stations.values()) {
            if (mag(server, s, speler)) {
                uit.add(s);
            }
        }
        return uit;
    }

    // --- the knooppunt and its buffer ---

    @Nullable
    public GlobalPos knooppunt(UUID eigenaar) {
        return knooppunten.get(eigenaar);
    }

    public void zetKnooppunt(UUID eigenaar, @Nullable GlobalPos plek) {
        if (plek == null) {
            knooppunten.remove(eigenaar);
        } else {
            knooppunten.put(eigenaar, plek);
        }
        setDirty();
    }

    public long lading(UUID eigenaar) {
        return lading.getOrDefault(eigenaar, 0L);
    }

    /** Puts VK in (what fits) and returns how much went in. */
    public long laad(UUID eigenaar, long vk) {
        long nu = lading(eigenaar);
        long erin = Math.max(0, Math.min(vk, HamsterbuisFeature.BUFFER - nu));
        if (erin > 0) {
            lading.put(eigenaar, nu + erin);
            setDirty();
        }
        return erin;
    }

    /** Does this owner's network run (a knooppunt, and enough VK for one trip)? */
    public boolean draait(UUID eigenaar) {
        return knooppunten.containsKey(eigenaar) && lading(eigenaar) >= HamsterbuisFeature.REIS;
    }

    /** Takes one trip's VK; false when the network stands still. */
    public boolean betaal(UUID eigenaar) {
        if (!draait(eigenaar)) {
            return false;
        }
        lading.put(eigenaar, lading(eigenaar) - HamsterbuisFeature.REIS);
        setDirty();
        return true;
    }

    /** (tests and commands) sets the buffer. */
    public void zetLading(UUID eigenaar, long vk) {
        lading.put(eigenaar, Math.max(0, Math.min(HamsterbuisFeature.BUFFER, vk)));
        setDirty();
    }

    // =====================================================================================================================
    // saving
    // =====================================================================================================================

    private static HamsterNet laad(CompoundTag tag) {
        HamsterNet n = new HamsterNet();
        ListTag lijst = tag.getListOrEmpty("Stations");
        for (int i = 0; i < lijst.size(); i++) {
            CompoundTag c = lijst.getCompoundOrEmpty(i);
            UUID id = uuid(c.getStringOr("Id", ""));
            UUID eigenaar = uuid(c.getStringOr("Eigenaar", ""));
            GlobalPos plek = plek(c);
            if (id != null && eigenaar != null && plek != null) {
                n.stations.put(id, new Station(id, eigenaar, plek, c.getStringOr("Naam", ""), c.getBooleanOr("Openbaar", false)));
            }
        }
        ListTag kp = tag.getListOrEmpty("Knooppunten");
        for (int i = 0; i < kp.size(); i++) {
            CompoundTag c = kp.getCompoundOrEmpty(i);
            UUID eigenaar = uuid(c.getStringOr("Eigenaar", ""));
            if (eigenaar == null) {
                continue;
            }
            GlobalPos plek = plek(c);
            if (plek != null) {
                n.knooppunten.put(eigenaar, plek);
            }
            n.lading.put(eigenaar, Math.max(0, c.getLongOr("Lading", 0L)));
        }
        return n;
    }

    private CompoundTag bewaar(CompoundTag tag) {
        ListTag lijst = new ListTag();
        for (Station s : stations.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", s.id.toString());
            c.putString("Eigenaar", s.eigenaar.toString());
            zetPlek(c, s.plek);
            c.putString("Naam", s.naam);
            c.putBoolean("Openbaar", s.openbaar);
            lijst.add(c);
        }
        tag.put("Stations", lijst);
        ListTag kp = new ListTag();
        java.util.Set<UUID> eigenaars = new java.util.HashSet<>(knooppunten.keySet());
        eigenaars.addAll(lading.keySet());
        for (UUID e : eigenaars) {
            CompoundTag c = new CompoundTag();
            c.putString("Eigenaar", e.toString());
            GlobalPos plek = knooppunten.get(e);
            if (plek != null) {
                zetPlek(c, plek);
            }
            c.putLong("Lading", lading(e));
            kp.add(c);
        }
        tag.put("Knooppunten", kp);
        return tag;
    }

    private static void zetPlek(CompoundTag c, GlobalPos plek) {
        c.putString("Dim", plek.dimension().identifier().toString());
        c.putLong("Pos", plek.pos().asLong());
    }

    @Nullable
    private static GlobalPos plek(CompoundTag c) {
        Identifier dim = Identifier.tryParse(c.getStringOr("Dim", ""));
        if (dim == null || !c.contains("Pos")) {
            return null;
        }
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(c.getLongOr("Pos", 0L)));
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
