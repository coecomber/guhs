package nl.juiced.guhs.feature.band;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import net.minecraft.core.UUIDUtil;
/**
 * Everything the 2.10 band keeps per owner and per guh (SavedData {@code guhs_band} in the overworld): the hearts, the
 * favourites, the dagboekje (stats, eerste keren, wist-je-datjes), where it is ({@link GuhVolger}), a looks snapshot for
 * the Guhdex, and the friendships between guhs ({@link Vriendjes}). Keyed by owner, then band id ({@link Band#id}): an
 * owner may be offline and a guh may be an item in a chest, the data is always here.
 */
public final class BandData extends SavedData {
    public static final String NAAM = "guhs_band";
    /** Wist-je-datjes kept per guh (newest first). */
    public static final int WIST_MAX = 40;

    public record Eerste(String id, long dag) {
    }

    public record WistJeDat(String key, List<String> args, long dag) {
    }

    /** One guh (or maatje) of one owner. */
    public static final class Rec {
        public final UUID id;
        /** A band guh (true) or a maatje (muisje, Schilly, Poepschilly: only tracked for "waar is hij"). */
        public boolean guh = true;
        /** The maatje kind ("pieppiepmuisje", "schilly", "poepschilly"), "guh" for guhs. */
        public String soort = "guh";
        public String naam = "";
        /** Looks for the Guhdex preview: variant, personality, clothes ids per slot, hair colour, scale, baby. */
        public CompoundTag looks = new CompoundTag();
        public int hartjes;
        /** Highest level already reached (ordinal of BandNiveau). */
        public int niveau;
        /** Levels reached while the owner was offline: announced at the next login. */
        public final List<Integer> teMelden = new ArrayList<>();
        /** The day the per-day caps belong to, and what each reason already gave that day. */
        public long dag = -1;
        public final int[] vandaag = new int[Reden.values().length];
        public final EnumMap<FavorietSoort, String> fav = new EnumMap<>(FavorietSoort.class);
        public final EnumSet<FavorietSoort> ontdekt = EnumSet.noneOf(FavorietSoort.class);
        public final EnumMap<DagboekStat, Long> stats = new EnumMap<>(DagboekStat.class);
        public final List<Eerste> eerste = new ArrayList<>();
        public final List<WistJeDat> wist = new ArrayList<>();
        public Plek plek = Plek.ONBEKEND;
        /** The last day counted in DAGEN_SAMEN. */
        public long samenDag = -1;
        /** The day it became yours. */
        public long sindsDag = -1;
        /** 3.0: the guh died ("In de wolkjes... njeg"): see Wolkjes. */
        public boolean dood;
        /** 3.0: the day it went to the wolkjes (Band.dag). */
        public long doodDag = -1;
        /** 3.0: its whole entity NBT at the moment it died (saveWithoutId + "id"), so the Knuffelhart can bring it back. */
        public CompoundTag lichaam = new CompoundTag();

        Rec(UUID id) {
            this.id = id;
        }

        public BandNiveau niveau() {
            return BandNiveau.van(hartjes);
        }

        public long stat(DagboekStat s) {
            return stats.getOrDefault(s, 0L);
        }

        public boolean heeftEerste(String id) {
            return eerste.stream().anyMatch(e -> e.id().equals(id));
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.store("Id", UUIDUtil.CODEC, id);
            t.putBoolean("Guh", guh);
            t.putString("Soort", soort);
            t.putString("Naam", naam);
            t.put("Looks", looks.copy());
            t.putInt("Hartjes", hartjes);
            t.putInt("Niveau", niveau);
            t.putIntArray("TeMelden", teMelden.stream().mapToInt(Integer::intValue).toArray());
            t.putLong("Dag", dag);
            t.putIntArray("Vandaag", vandaag);
            CompoundTag f = new CompoundTag();
            fav.forEach((k, v) -> f.putString(k.id(), v));
            t.put("Fav", f);
            ListTag o = new ListTag();
            ontdekt.forEach(s -> o.add(StringTag.valueOf(s.id())));
            t.put("Ontdekt", o);
            CompoundTag st = new CompoundTag();
            stats.forEach((k, v) -> st.putLong(k.id(), v));
            t.put("Stats", st);
            ListTag e = new ListTag();
            for (Eerste x : eerste) {
                CompoundTag c = new CompoundTag();
                c.putString("Id", x.id());
                c.putLong("Dag", x.dag());
                e.add(c);
            }
            t.put("Eerste", e);
            ListTag w = new ListTag();
            for (WistJeDat x : wist) {
                CompoundTag c = new CompoundTag();
                c.putString("Key", x.key());
                ListTag args = new ListTag();
                x.args().forEach(a -> args.add(StringTag.valueOf(a)));
                c.put("Args", args);
                c.putLong("Dag", x.dag());
                w.add(c);
            }
            t.put("Wist", w);
            t.put("Plek", plek.save());
            t.putLong("SamenDag", samenDag);
            t.putLong("SindsDag", sindsDag);
            if (dood) {
                t.putBoolean("Dood", true);
                t.putLong("DoodDag", doodDag);
            }
            if (!lichaam.isEmpty()) {
                t.put("Lichaam", lichaam.copy());
            }
            return t;
        }

        static Rec load(CompoundTag t) {
            Rec r = new Rec(t.read("Id", UUIDUtil.CODEC).orElseThrow());
            r.guh = !t.contains("Guh") || t.getBooleanOr("Guh", false);
            r.soort = t.getStringOr("Soort", "").isEmpty() ? "guh" : t.getStringOr("Soort", "");
            r.naam = t.getStringOr("Naam", "");
            r.looks = t.getCompoundOrEmpty("Looks");
            r.hartjes = Math.max(0, t.getIntOr("Hartjes", 0));
            r.niveau = t.getIntOr("Niveau", 0);
            for (int i : t.getIntArray("TeMelden").orElse(new int[0])) {
                r.teMelden.add(i);
            }
            r.dag = t.contains("Dag") ? t.getLongOr("Dag", 0L) : -1;
            int[] v = t.getIntArray("Vandaag").orElse(new int[0]);
            System.arraycopy(v, 0, r.vandaag, 0, Math.min(v.length, r.vandaag.length));
            CompoundTag f = t.getCompoundOrEmpty("Fav");
            for (String k : f.keySet()) {
                FavorietSoort s = FavorietSoort.byId(k);
                if (s != null) {
                    r.fav.put(s, f.getStringOr(k, ""));
                }
            }
            ListTag o = t.getListOrEmpty("Ontdekt");
            for (int i = 0; i < o.size(); i++) {
                FavorietSoort s = FavorietSoort.byId(o.getStringOr(i, ""));
                if (s != null) {
                    r.ontdekt.add(s);
                }
            }
            CompoundTag st = t.getCompoundOrEmpty("Stats");
            for (String k : st.keySet()) {
                DagboekStat s = DagboekStat.byId(k);
                if (s != null) {
                    r.stats.put(s, st.getLongOr(k, 0L));
                }
            }
            ListTag e = t.getListOrEmpty("Eerste");
            for (int i = 0; i < e.size(); i++) {
                r.eerste.add(new Eerste(e.getCompoundOrEmpty(i).getStringOr("Id", ""), e.getCompoundOrEmpty(i).getLongOr("Dag", 0L)));
            }
            ListTag w = t.getListOrEmpty("Wist");
            for (int i = 0; i < w.size(); i++) {
                CompoundTag c = w.getCompoundOrEmpty(i);
                List<String> args = new ArrayList<>();
                ListTag a = c.getListOrEmpty("Args");
                for (int j = 0; j < a.size(); j++) {
                    args.add(a.getStringOr(j, ""));
                }
                r.wist.add(new WistJeDat(c.getStringOr("Key", ""), args, c.getLongOr("Dag", 0L)));
            }
            r.plek = Plek.load(t.getCompoundOrEmpty("Plek"));
            r.samenDag = t.contains("SamenDag") ? t.getLongOr("SamenDag", 0L) : -1;
            r.sindsDag = t.contains("SindsDag") ? t.getLongOr("SindsDag", 0L) : -1;
            r.dood = t.getBooleanOr("Dood", false);
            r.doodDag = t.contains("DoodDag") ? t.getLongOr("DoodDag", 0L) : -1;
            r.lichaam = t.getCompoundOrEmpty("Lichaam");
            return r;
        }
    }

    private final Map<UUID, Map<UUID, Rec>> eigenaars = new LinkedHashMap<>();
    /** Friendship points per pair of band ids ("a|b", sorted). */
    final Map<String, Integer> vriendjes = new HashMap<>();

    public static BandData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(BandData::new, BandData::load, null), NAAM);
    }

    /** The Guhdex "Mijn guhs" data of this owner: one CompoundTag per band guh (see {@link MijnGuhs#snapshot}). */
    public static ListTag snapshot(net.minecraft.server.level.ServerPlayer player) {
        return MijnGuhs.snapshot(player, null).getListOrEmpty("Guhs");
    }

    /** The record of this guh of this owner (made when it doesn't exist yet). */
    public Rec rec(UUID eigenaar, UUID id) {
        Map<UUID, Rec> van = eigenaars.computeIfAbsent(eigenaar, k -> new LinkedHashMap<>());
        Rec r = van.get(id);
        if (r == null) {
            r = new Rec(id);
            van.put(id, r);
            setDirty();
        }
        return r;
    }

    @Nullable
    public Rec vind(UUID eigenaar, UUID id) {
        Map<UUID, Rec> van = eigenaars.get(eigenaar);
        return van == null ? null : van.get(id);
    }

    /** Somebody's record of this band id (for a guh whose owner we don't know here). */
    @Nullable
    public Rec vindOveral(UUID id) {
        for (Map<UUID, Rec> van : eigenaars.values()) {
            Rec r = van.get(id);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /** The owner of this band id, or null. */
    @Nullable
    public UUID eigenaarVan(UUID id) {
        for (Map.Entry<UUID, Map<UUID, Rec>> e : eigenaars.entrySet()) {
            if (e.getValue().containsKey(id)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** All records of an owner (guhs and maatjes), oldest first. */
    public List<Rec> van(UUID eigenaar) {
        Map<UUID, Rec> van = eigenaars.get(eigenaar);
        return van == null ? List.of() : new ArrayList<>(van.values());
    }

    /** Only the band guhs of an owner. */
    public List<Rec> guhsVan(UUID eigenaar) {
        return van(eigenaar).stream().filter(r -> r.guh).toList();
    }

    // --- saving -------------------------------------------------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Map<UUID, Rec>> e : eigenaars.entrySet()) {
            CompoundTag o = new CompoundTag();
            o.store("Eigenaar", UUIDUtil.CODEC, e.getKey());
            ListTag recs = new ListTag();
            e.getValue().values().forEach(r -> recs.add(r.save()));
            o.put("Guhs", recs);
            list.add(o);
        }
        tag.put("Eigenaars", list);
        CompoundTag v = new CompoundTag();
        vriendjes.forEach(v::putInt);
        tag.put("Vriendjes", v);
        return tag;
    }

    public static BandData load(CompoundTag tag, HolderLookup.Provider registries) {
        BandData d = new BandData();
        ListTag list = tag.getListOrEmpty("Eigenaars");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag o = list.getCompoundOrEmpty(i);
            Map<UUID, Rec> van = new LinkedHashMap<>();
            ListTag recs = o.getListOrEmpty("Guhs");
            for (int j = 0; j < recs.size(); j++) {
                Rec r = Rec.load(recs.getCompoundOrEmpty(j));
                van.put(r.id, r);
            }
            d.eigenaars.put(o.read("Eigenaar", UUIDUtil.CODEC).orElseThrow(), van);
        }
        CompoundTag v = tag.getCompoundOrEmpty("Vriendjes");
        for (String k : v.keySet()) {
            d.vriendjes.put(k, v.getIntOr(k, 0));
        }
        return d;
    }
}
