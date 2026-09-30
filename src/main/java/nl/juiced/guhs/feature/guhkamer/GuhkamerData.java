package nl.juiced.guhs.feature.guhkamer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import net.minecraft.core.UUIDUtil;
/**
 * Every player's Guhkamer (SavedData {@value #NAAM}, overworld storage): how big it is built, where its door in the maag
 * is, and its guests ("gasten"): the guhs that stay there. A guest is either in the world (in the room, while someone is
 * there) or kept here as its saved entity data (while the room is empty), so it can always be called with the Guhbel.
 */
public class GuhkamerData extends SavedData {
    public static final String NAAM = "guhs_guhkamer";

    /** One player's room. */
    public static final class Kamer {
        public final UUID eigenaar;
        /** The built size (0 = not built yet). */
        public int breedte, hoogte;
        /** The door in the maag (lower half), or null. */
        @Nullable
        public BlockPos maagDeur;
        public final Map<UUID, Gast> gasten = new LinkedHashMap<>();

        Kamer(UUID eigenaar) {
            this.eigenaar = eigenaar;
        }

        /** Guests that are in the world right now (not kept as data). */
        public boolean iemandBuiten() {
            return gasten.values().stream().anyMatch(g -> !g.opgeslagen());
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
            t.putInt("Breedte", breedte);
            t.putInt("Hoogte", hoogte);
            if (maagDeur != null) {
                t.putLong("MaagDeur", maagDeur.asLong());
            }
            ListTag list = new ListTag();
            gasten.values().forEach(g -> list.add(g.save()));
            t.put("Gasten", list);
            return t;
        }

        static Kamer load(CompoundTag t) {
            Kamer k = new Kamer(t.read("Eigenaar", UUIDUtil.CODEC).orElseThrow());
            k.breedte = t.getIntOr("Breedte", 0);
            k.hoogte = t.getIntOr("Hoogte", 0);
            if (t.contains("MaagDeur")) {
                k.maagDeur = BlockPos.of(t.getLongOr("MaagDeur", 0L));
            }
            ListTag list = t.getListOrEmpty("Gasten");
            for (int i = 0; i < list.size(); i++) {
                Gast g = Gast.load(list.getCompoundOrEmpty(i));
                k.gasten.put(g.id, g);
            }
            return k;
        }
    }

    /** A guh staying in the room. */
    public static final class Gast {
        public final UUID id;
        public String naam;
        /** Its saved entity data while the room is empty; empty while it is in the world. */
        public CompoundTag data = new CompoundTag();
        /** Where it was in the room (to put it back there), or null. */
        @Nullable
        public Vec3 plek;
        public CompoundTag looks = new CompoundTag();
        public long sinds;

        public Gast(UUID id, String naam) {
            this.id = id;
            this.naam = naam;
        }

        public boolean opgeslagen() {
            return !data.isEmpty();
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.store("Id", UUIDUtil.CODEC, id);
            t.putString("Naam", naam);
            if (!data.isEmpty()) {
                t.put("Data", data);
            }
            if (plek != null) {
                t.putDouble("X", plek.x);
                t.putDouble("Y", plek.y);
                t.putDouble("Z", plek.z);
            }
            t.put("Looks", looks);
            t.putLong("Sinds", sinds);
            return t;
        }

        static Gast load(CompoundTag t) {
            Gast g = new Gast(t.read("Id", UUIDUtil.CODEC).orElseThrow(), t.getStringOr("Naam", ""));
            g.data = t.getCompoundOrEmpty("Data");
            if (t.contains("X")) {
                g.plek = new Vec3(t.getDoubleOr("X", 0.0), t.getDoubleOr("Y", 0.0), t.getDoubleOr("Z", 0.0));
            }
            g.looks = t.getCompoundOrEmpty("Looks");
            g.sinds = t.getLongOr("Sinds", 0L);
            return g;
        }
    }

    private final Map<UUID, Kamer> kamers = new LinkedHashMap<>();

    public static GuhkamerData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(GuhkamerData::new, GuhkamerData::load, null), NAAM);
    }

    /** This player's room (made when needed). */
    public Kamer kamer(UUID eigenaar) {
        return kamers.computeIfAbsent(eigenaar, Kamer::new);
    }

    @Nullable
    public Kamer vind(UUID eigenaar) {
        return kamers.get(eigenaar);
    }

    public Collection<Kamer> alle() {
        return kamers.values();
    }

    /** The room a guest stays in, or null. */
    @Nullable
    public Kamer vanGast(UUID id) {
        for (Kamer k : kamers.values()) {
            if (k.gasten.containsKey(id)) {
                return k;
            }
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        kamers.values().forEach(k -> list.add(k.save()));
        tag.put("Kamers", list);
        return tag;
    }

    public static GuhkamerData load(CompoundTag tag, HolderLookup.Provider registries) {
        GuhkamerData d = new GuhkamerData();
        ListTag list = tag.getListOrEmpty("Kamers");
        for (int i = 0; i < list.size(); i++) {
            Kamer k = Kamer.load(list.getCompoundOrEmpty(i));
            d.kamers.put(k.eigenaar, k);
        }
        return d;
    }
}
