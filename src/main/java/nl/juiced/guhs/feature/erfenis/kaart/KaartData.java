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

import net.minecraft.core.UUIDUtil;
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
 * Also (only for the stand-alone branch, see {@link KaartBronStandaard}): the cells handed out to real players while the
 * core's ErfenisData is not wired yet ({@link #spelers}).
 */
public final class KaartData extends SavedData {
    /** A real player's cell in the stand-alone fallback (the core's ErfenisData replaces this at merge). */
    public record SpelerCel(String naam, int cx, int cz) {
    }

    public static final int MAX_NIEUWS = 40;
    static final SavedDataType<KaartData> TYPE = GuhSavedData.tagType("erfenis_kaart", KaartData::new, KaartData::load, KaartData::save);

    public long seed;
    public boolean gezaaid;
    final Map<UUID, NpcDorp> npcs = new LinkedHashMap<>();
    final List<KaartNieuws> nieuws = new ArrayList<>();
    /** How many growth ticks happened (each one's randomness comes from seed + this, so it is stable). */
    public int groeiTeller;
    /** Dev: grow every few seconds instead of every few minutes. */
    public boolean snel;
    final Map<UUID, SpelerCel> spelers = new LinkedHashMap<>();
    int volgendeSpiraal;

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
        d.volgendeSpiraal = t.getIntOr("VolgendeSpiraal", 0);
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
        for (Tag e : t.getListOrEmpty("Spelers")) {
            if (e instanceof CompoundTag c) {
                c.read("Id", UUIDUtil.CODEC).ifPresent(id -> d.spelers.put(id, new SpelerCel(c.getStringOr("Naam", "?"), c.getIntOr("Cx", 0),
                        c.getIntOr("Cz", 0))));
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
        t.putInt("VolgendeSpiraal", volgendeSpiraal);
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
        ListTag sl = new ListTag();
        for (Map.Entry<UUID, SpelerCel> e : spelers.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.store("Id", UUIDUtil.CODEC, e.getKey());
            c.putString("Naam", e.getValue().naam());
            c.putInt("Cx", e.getValue().cx());
            c.putInt("Cz", e.getValue().cz());
            sl.add(c);
        }
        t.put("Spelers", sl);
        return t;
    }
}
