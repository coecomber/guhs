package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The server-wide Vadsrijk market (veilinghuis), SavedData {@code guhs:vr_economie_markt} (with the overworld): the listings
 * (each holds the item as a tag: it left the seller's bag when it was listed), the proceeds waiting for each seller (collected at
 * the marktmeester, also after selling while offline) and how many things each seller sold (title Marktkoopman).
 * Only {@link Markt} changes it, always on the server thread.
 */
public final class MarktData extends SavedData {
    public static final SavedDataType<MarktData> TYPE = GuhSavedData.tagType("vr_economie_markt", MarktData::new, MarktData::load, MarktData::save);

    /** One listing. {@code slot}/{@code klasse}/{@code zeldzaamheid}/{@code level} are the filters ("" / -1 for non-gear). */
    public record Aanbod(long id, UUID verkoper, String naam, CompoundTag item, long prijs, String slot, String klasse, int zeldzaamheid, int level,
            String zoek, long tijd) {
    }

    long volgende = 1;
    final List<Aanbod> aanbod = new ArrayList<>();
    final Map<UUID, Long> opbrengst = new HashMap<>();
    final Map<UUID, Integer> verkocht = new HashMap<>();

    public static MarktData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    @Nullable
    public Aanbod aanbod(long id) {
        for (Aanbod a : aanbod) {
            if (a.id() == id) {
                return a;
            }
        }
        return null;
    }

    public List<Aanbod> alle() {
        return List.copyOf(aanbod);
    }

    public long opbrengst(UUID id) {
        return opbrengst.getOrDefault(id, 0L);
    }

    public int verkocht(UUID id) {
        return verkocht.getOrDefault(id, 0);
    }

    public int aantalVan(UUID id) {
        int n = 0;
        for (Aanbod a : aanbod) {
            if (a.verkoper().equals(id)) {
                n++;
            }
        }
        return n;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("Volgende", volgende);
        ListTag l = new ListTag();
        for (Aanbod a : aanbod) {
            CompoundTag c = new CompoundTag();
            c.putLong("Id", a.id());
            c.store("Verkoper", UUIDUtil.CODEC, a.verkoper());
            c.putString("Naam", a.naam());
            c.put("Item", a.item());
            c.putLong("Prijs", a.prijs());
            c.putString("Slot", a.slot());
            c.putString("Klasse", a.klasse());
            c.putInt("Zeldzaamheid", a.zeldzaamheid());
            c.putInt("Level", a.level());
            c.putString("Zoek", a.zoek());
            c.putLong("Tijd", a.tijd());
            l.add(c);
        }
        t.put("Aanbod", l);
        ListTag o = new ListTag();
        opbrengst.forEach((id, n) -> {
            CompoundTag c = new CompoundTag();
            c.store("Speler", UUIDUtil.CODEC, id);
            c.putLong("Munten", n);
            c.putInt("Verkocht", verkocht.getOrDefault(id, 0));
            o.add(c);
        });
        verkocht.forEach((id, n) -> {
            if (!opbrengst.containsKey(id)) {
                CompoundTag c = new CompoundTag();
                c.store("Speler", UUIDUtil.CODEC, id);
                c.putLong("Munten", 0);
                c.putInt("Verkocht", n);
                o.add(c);
            }
        });
        t.put("Spelers", o);
        return t;
    }

    private static MarktData load(CompoundTag t) {
        MarktData m = new MarktData();
        m.volgende = t.getLongOr("Volgende", 1);
        ListTag l = t.getListOrEmpty("Aanbod");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompoundOrEmpty(i);
            UUID v = c.read("Verkoper", UUIDUtil.CODEC).orElse(null);
            if (v == null) {
                continue;
            }
            m.aanbod.add(new Aanbod(c.getLongOr("Id", 0), v, c.getStringOr("Naam", "?"), c.getCompoundOrEmpty("Item"), c.getLongOr("Prijs", 1),
                    c.getStringOr("Slot", ""), c.getStringOr("Klasse", ""), c.getIntOr("Zeldzaamheid", -1), c.getIntOr("Level", -1),
                    c.getStringOr("Zoek", ""), c.getLongOr("Tijd", 0)));
        }
        ListTag o = t.getListOrEmpty("Spelers");
        for (int i = 0; i < o.size(); i++) {
            CompoundTag c = o.getCompoundOrEmpty(i);
            UUID id = c.read("Speler", UUIDUtil.CODEC).orElse(null);
            if (id == null) {
                continue;
            }
            long n = c.getLongOr("Munten", 0);
            if (n > 0) {
                m.opbrengst.put(id, n);
            }
            int k = c.getIntOr("Verkocht", 0);
            if (k > 0) {
                m.verkocht.put(id, k);
            }
        }
        return m;
    }
}
