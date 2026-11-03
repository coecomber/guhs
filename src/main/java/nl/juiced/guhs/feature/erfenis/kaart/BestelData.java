package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * De Erfenis, bestellingen + Handje helpen: per real player their order carts, the daily star order, how many orders they
 * delivered, and their helping-hand day (who they helped today, stamps). Saved with the overworld
 * ({@code data/guhs/erfenis_bestellingen.dat}).
 */
public final class BestelData extends SavedData {
    static final SavedDataType<BestelData> TYPE = GuhSavedData.tagType("erfenis_bestellingen", BestelData::new, BestelData::load, BestelData::save);

    /** One player's state. */
    public static final class Speler {
        public final List<Bestelling> karren = new ArrayList<>();
        @Nullable
        public Bestelling ster;
        /** The day (epoch day) the last star order was made (-1: never). */
        public long sterDag = -1;
        /** Orders delivered in total (Hoeve goals, FTB, the moment bestelling_geleverd). */
        public int geleverd;
        /** A counter for the randomness (every new order takes the next number). */
        public int teller;
        // Handje helpen (as a helper)
        public int stempels;
        public long hulpDag = -1;
        public int hulpBeloond;
        public final List<UUID> geholpen = new ArrayList<>();
        // (as the helped one) "X hielp je een handje!" lines waiting for the next login
        public final List<String> berichten = new ArrayList<>();

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            ListTag k = new ListTag();
            for (Bestelling b : karren) {
                k.add(b.save());
            }
            t.put("Karren", k);
            if (ster != null) {
                t.put("Ster", ster.save());
            }
            t.putLong("SterDag", sterDag);
            t.putInt("Geleverd", geleverd);
            t.putInt("Teller", teller);
            t.putInt("Stempels", stempels);
            t.putLong("HulpDag", hulpDag);
            t.putInt("HulpBeloond", hulpBeloond);
            ListTag g = new ListTag();
            for (UUID u : geholpen) {
                CompoundTag c = new CompoundTag();
                c.store("Id", UUIDUtil.CODEC, u);
                g.add(c);
            }
            t.put("Geholpen", g);
            ListTag b = new ListTag();
            for (String s : berichten) {
                b.add(StringTag.valueOf(s));
            }
            t.put("Berichten", b);
            return t;
        }

        static Speler load(CompoundTag t) {
            Speler s = new Speler();
            for (Tag e : t.getListOrEmpty("Karren")) {
                if (e instanceof CompoundTag c) {
                    s.karren.add(Bestelling.load(c));
                }
            }
            t.getCompound("Ster").ifPresent(c -> s.ster = Bestelling.load(c));
            s.sterDag = t.getLongOr("SterDag", -1L);
            s.geleverd = t.getIntOr("Geleverd", 0);
            s.teller = t.getIntOr("Teller", 0);
            s.stempels = t.getIntOr("Stempels", 0);
            s.hulpDag = t.getLongOr("HulpDag", -1L);
            s.hulpBeloond = t.getIntOr("HulpBeloond", 0);
            for (Tag e : t.getListOrEmpty("Geholpen")) {
                if (e instanceof CompoundTag c) {
                    c.read("Id", UUIDUtil.CODEC).ifPresent(s.geholpen::add);
                }
            }
            for (Tag e : t.getListOrEmpty("Berichten")) {
                e.asString().ifPresent(s.berichten::add);
            }
            return s;
        }
    }

    private final Map<UUID, Speler> spelers = new HashMap<>();
    /** Dev: carts drive and rest 10 seconds instead of minutes. */
    public boolean snel;

    public static BestelData get(MinecraftServer server) {
        return GuhSavedData.get(server.overworld(), TYPE, "guhs_erfenis_bestellingen");
    }

    public Speler van(UUID speler) {
        return spelers.computeIfAbsent(speler, k -> new Speler());
    }

    @Nullable
    public Speler bestaand(UUID speler) {
        return spelers.get(speler);
    }

    // =====================================================================================================================

    public static BestelData load(CompoundTag t) {
        BestelData d = new BestelData();
        d.snel = t.getBooleanOr("Snel", false);
        for (Tag e : t.getListOrEmpty("Spelers")) {
            if (e instanceof CompoundTag c) {
                c.read("Id", UUIDUtil.CODEC).ifPresent(id -> d.spelers.put(id, Speler.load(c)));
            }
        }
        return d;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Snel", snel);
        ListTag l = new ListTag();
        for (Map.Entry<UUID, Speler> e : spelers.entrySet()) {
            CompoundTag c = e.getValue().save();
            c.store("Id", UUIDUtil.CODEC, e.getKey());
            l.add(c);
        }
        t.put("Spelers", l);
        return t;
    }
}
