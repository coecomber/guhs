package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * De Erfenis, wereldkaart: the living NPC villages, the news feed and the growth clock, saved with the overworld
 * ({@code data/guhs/erfenis_kaart.dat}). The villages are made by {@link KaartLeven} from {@link #seed} (from the world
 * seed: the same world gets the same map) and then saved, so they are stable across restarts.
 * <p>
 * The real players' bases are not here: they live in the core's ErfenisData (see {@link KaartBronErfenis}).
 */
public final class KaartData extends SavedData {
    public static final int MAX_NIEUWS = 40;
    static final SavedDataType<KaartData> TYPE = GuhSavedData.tagType("erfenis_kaart", KaartData::new, KaartData::load, KaartData::save);

    /** 2 = the 1.9 rule (villages follow players); 0/1 = a POC map (40-ish random villages), migrated by KaartLeven. */
    public static final int VERSIE = 2;

    public long seed;
    public boolean gezaaid;
    public int versie = VERSIE;
    /** The first real player (gets three villages: one lower, one equal, one higher); null until there is one. */
    @Nullable
    public UUID eerste;
    final Map<UUID, NpcDorp> npcs = new LinkedHashMap<>();
    final List<KaartNieuws> nieuws = new ArrayList<>();
    /** How many growth ticks happened (each one's randomness comes from seed + this, so it is stable). */
    public int groeiTeller;
    /** Dev: grow every few seconds instead of every few minutes. */
    public boolean snel;

    public static KaartData get(MinecraftServer server) {
        KaartData d = GuhSavedData.get(server.overworld(), TYPE, "guhs_erfenis_kaart");
        if (!d.gezaaid && d.seed == 0) {
            d.seed = server.overworld().getSeed() ^ 0x6B61617274L;   // "kaart"
            d.setDirty();
        }
        return d;
    }

    public Collection<NpcDorp> npcs() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    @Nullable
    public NpcDorp npc(UUID id) {
        return npcs.get(id);
    }

    public List<KaartNieuws> nieuws() {
        return Collections.unmodifiableList(nieuws);
    }

    /** Every cell an NPC village stands on. */
    public Set<Long> npcCellen() {
        Set<Long> s = new HashSet<>();
        for (NpcDorp d : npcs.values()) {
            s.add(sleutel(d.cx, d.cz));
        }
        return s;
    }

    void voegToe(NpcDorp d) {
        npcs.put(d.id, d);
        setDirty();
    }

    void verwijder(UUID id) {
        if (npcs.remove(id) != null) {
            setDirty();
        }
    }

    /** The NPC villages that follow this player. */
    public List<NpcDorp> volgers(UUID speler) {
        List<NpcDorp> l = new ArrayList<>();
        for (NpcDorp d : npcs.values()) {
            if (speler.equals(d.volgt)) {
                l.add(d);
            }
        }
        return l;
    }

    /** Adds a news line (newest first; the oldest fall off). Also usable by the core (a player's stadhuis went up). */
    public void meld(KaartNieuws n) {
        nieuws.add(0, n);
        while (nieuws.size() > MAX_NIEUWS) {
            nieuws.remove(nieuws.size() - 1);
        }
        setDirty();
    }

    public static long sleutel(int cx, int cz) {
        return (long) cx << 32 | (cz & 0xFFFFFFFFL);
    }

    // =====================================================================================================================

    public static KaartData load(CompoundTag t) {
        KaartData d = new KaartData();
        d.seed = t.getLongOr("Seed", 0L);
        d.gezaaid = t.getBooleanOr("Gezaaid", false);
        d.groeiTeller = t.getIntOr("GroeiTeller", 0);
        d.snel = t.getBooleanOr("Snel", false);
        d.versie = t.getIntOr("Versie", 0);
        d.eerste = t.read("Eerste", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        for (Tag e : t.getListOrEmpty("Npcs")) {
            if (e instanceof CompoundTag c) {
                NpcDorp n = NpcDorp.load(c);
                if (n != null) {
                    d.npcs.put(n.id, n);
                }
            }
        }
        for (Tag e : t.getListOrEmpty("Nieuws")) {
            if (e instanceof CompoundTag c) {
                d.nieuws.add(KaartNieuws.load(c));
            }
        }
        return d;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("Seed", seed);
        t.putBoolean("Gezaaid", gezaaid);
        t.putInt("GroeiTeller", groeiTeller);
        t.putBoolean("Snel", snel);
        t.putInt("Versie", versie);
        if (eerste != null) {
            t.store("Eerste", net.minecraft.core.UUIDUtil.CODEC, eerste);
        }
        ListTag l = new ListTag();
        for (NpcDorp n : npcs.values()) {
            l.add(n.save());
        }
        t.put("Npcs", l);
        ListTag nl = new ListTag();
        for (KaartNieuws n : nieuws) {
            nl.add(n.save());
        }
        t.put("Nieuws", nl);
        return t;
    }
}
