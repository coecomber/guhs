package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.feature.vadsrijk.karakter.Karakter;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * U-GILDEN: every Knuffelbond of the server, server-wide SavedData {@code guhs:vr_gilden} (overworld data folder). Membership is per
 * CHARACTER ({@link #sleutel}); the index character -> bond is rebuilt from the member lists on load (one source of truth).
 * The bank items live inside (raw until first use; saved with the server's registries, see {@link Bond#bank}).
 */
public final class Bonden extends SavedData {
    public static final SavedDataType<Bonden> TYPE = GuhSavedData.tagType("vr_gilden", Bonden::new, Bonden::load, Bonden::save);
    /** For saving the bank items (registries); set at server start. */
    @Nullable
    static volatile MinecraftServer server;

    private int volgende = 1;
    private final Map<Integer, Bond> bonden = new LinkedHashMap<>();
    /** character key -> bond id */
    private final Map<String, Integer> index = new HashMap<>();
    /** plot cell -> bond id (a freed cell is reused by the next new bond) */
    private final Map<Integer, Integer> kavels = new HashMap<>();

    public static Bonden get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The character key of this player's ACTIVE character ("uuid/slot"). */
    public static String sleutel(Player p) {
        return Karakter.sleutel(p).tekst();
    }

    /** The bond of this player's active character (null: none). */
    @Nullable
    public static Bond van(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        return s == null ? null : get(s).vanSleutel(sleutel(p));
    }

    @Nullable
    public Bond vanSleutel(String sleutel) {
        Integer id = index.get(sleutel);
        return id == null ? null : bonden.get(id);
    }

    @Nullable
    public Bond bond(int id) {
        return bonden.get(id);
    }

    public Collection<Bond> alle() {
        return bonden.values();
    }

    @Nullable
    public Bond metNaam(String naam) {
        for (Bond b : bonden.values()) {
            if (b.naam.equalsIgnoreCase(naam.trim()) || b.tag.equalsIgnoreCase(naam.trim())) {
                return b;
            }
        }
        return null;
    }

    public boolean naamBezet(String naam) {
        for (Bond b : bonden.values()) {
            if (b.naam.equalsIgnoreCase(naam.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean tagBezet(String tag) {
        for (Bond b : bonden.values()) {
            if (b.tag.equalsIgnoreCase(tag.trim())) {
                return true;
            }
        }
        return false;
    }

    /** The ranking: most bond xp first, then the oldest. */
    public List<Bond> ranglijst() {
        List<Bond> l = new ArrayList<>(bonden.values());
        l.sort(Comparator.comparingLong((Bond b) -> b.xp).reversed().thenComparingLong(b -> b.gemaakt));
        return l;
    }

    // ---- changes (only BondActies / tests) -----------------------------------------------------------------------------------

    Bond nieuw(String naam, String tag, long nu) {
        Bond b = new Bond(volgende++, naam.trim(), tag.trim().toUpperCase(Locale.ROOT));
        b.gemaakt = nu;
        bonden.put(b.id, b);
        setDirty();
        return b;
    }

    void zetLid(Bond b, String sleutel, Bond.Lid l) {
        b.leden.put(sleutel, l);
        index.put(sleutel, b.id);
        setDirty();
    }

    void haalWeg(Bond b, String sleutel) {
        b.leden.remove(sleutel);
        index.remove(sleutel);
        setDirty();
    }

    void hefOp(Bond b) {
        for (String k : b.leden.keySet()) {
            index.remove(k);
        }
        b.leden.clear();
        if (b.kavel >= 0) {
            kavels.remove(b.kavel);
        }
        bonden.remove(b.id);
        setDirty();
    }

    /** The lowest free plot cell; claims it for this bond. */
    int neemKavel(Bond b) {
        if (b.kavel >= 0) {
            return b.kavel;
        }
        int c = 0;
        while (kavels.containsKey(c)) {
            c++;
        }
        kavels.put(c, b.id);
        b.kavel = c;
        setDirty();
        return c;
    }

    /** Which bond owns this cell (null: free). */
    @Nullable
    public Bond opKavel(int cel) {
        Integer id = kavels.get(cel);
        return id == null ? null : bonden.get(id);
    }

    public boolean kavelBezet(int cel) {
        return kavels.containsKey(cel);
    }

    /** Cells that were ever stamped (bezet or freed): the valley's ground stays. */
    public int hoogsteKavel() {
        int m = -1;
        for (int c : kavels.keySet()) {
            m = Math.max(m, c);
        }
        return Math.max(m, gestempeld - 1);
    }

    /** how many cells have their ground stamped (0..n-1); the square is stamped */
    int gestempeld;
    boolean plein;

    /** Dev/tests: forget everything. */
    public void wisAlles() {
        bonden.clear();
        index.clear();
        kavels.clear();
        volgende = 1;
        gestempeld = 0;
        plein = false;
        setDirty();
    }

    // ---- saving --------------------------------------------------------------------------------------------------------------

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Volgende", volgende);
        t.putInt("Gestempeld", gestempeld);
        t.putBoolean("Plein", plein);
        MinecraftServer s = server;
        ListTag l = new ListTag();
        for (Bond b : bonden.values()) {
            l.add(b.save(s == null ? null : s.registryAccess()));
        }
        t.put("Bonden", l);
        return t;
    }

    private static Bonden load(CompoundTag t) {
        Bonden d = new Bonden();
        d.volgende = t.getIntOr("Volgende", 1);
        d.gestempeld = t.getIntOr("Gestempeld", 0);
        d.plein = t.getBooleanOr("Plein", false);
        ListTag l = t.getListOrEmpty("Bonden");
        for (int i = 0; i < l.size(); i++) {
            Bond b = Bond.load(l.getCompoundOrEmpty(i));
            d.bonden.put(b.id, b);
            d.volgende = Math.max(d.volgende, b.id + 1);
            for (String k : b.leden.keySet()) {
                d.index.put(k, b.id);
            }
            if (b.kavel >= 0) {
                d.kavels.put(b.kavel, b.id);
            }
        }
        return d;
    }
}
