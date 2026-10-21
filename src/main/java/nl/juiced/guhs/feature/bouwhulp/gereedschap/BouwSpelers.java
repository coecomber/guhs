package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Blauwdruk;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Per player (server wide, kept with the overworld): the settings of every tool ({@link Gereedschap}, one tag each; the
 * keet's Vormen/Daken tabs share the vormstaf's) and the player's own blueprints (design 2.6, at most {@link #MAX_EIGEN}).
 */
public final class BouwSpelers extends SavedData {
    public static final SavedDataType<BouwSpelers> TYPE = GuhSavedData.tagType("bouwhulp_spelers", BouwSpelers::new, BouwSpelers::laad, BouwSpelers::opslaan);
    /** Own blueprints per player (an own choice: BOUWHULP_KEUZES.md). */
    public static final int MAX_EIGEN = 30;

    private final Map<UUID, CompoundTag> instellingen = new HashMap<>();
    private final Map<UUID, List<Blauwdruk>> eigen = new HashMap<>();

    public BouwSpelers() {
    }

    public static BouwSpelers van(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The settings of one tool (the stored tag itself: call {@link #veranderd} after changing it). */
    public CompoundTag instelling(UUID speler, Gereedschap g) {
        CompoundTag alles = instellingen.computeIfAbsent(speler, u -> new CompoundTag());
        if (!alles.contains(g.id())) {
            alles.put(g.id(), new CompoundTag());
        }
        return alles.getCompoundOrEmpty(g.id());
    }

    public void veranderd() {
        setDirty();
    }

    public List<Blauwdruk> eigen(UUID speler) {
        return eigen.computeIfAbsent(speler, u -> new ArrayList<>());
    }

    @Nullable
    public Blauwdruk eigen(UUID speler, String id) {
        for (Blauwdruk b : eigen(speler)) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }

    /** Any player's own blueprint by its id (a printed blueprint item may be passed on). */
    @Nullable
    public Blauwdruk zoek(String id) {
        for (List<Blauwdruk> l : eigen.values()) {
            for (Blauwdruk b : l) {
                if (b.id.equals(id)) {
                    return b;
                }
            }
        }
        return null;
    }

    public boolean bewaar(UUID speler, Blauwdruk b) {
        List<Blauwdruk> l = eigen(speler);
        if (l.size() >= MAX_EIGEN) {
            return false;
        }
        l.add(0, b);
        setDirty();
        return true;
    }

    public boolean weg(UUID speler, String id) {
        boolean weg = eigen(speler).removeIf(b -> b.id.equals(id));
        if (weg) {
            setDirty();
        }
        return weg;
    }

    // =====================================================================================================================

    private CompoundTag opslaan() {
        CompoundTag t = new CompoundTag();
        CompoundTag i = new CompoundTag();
        instellingen.forEach((u, c) -> i.put(u.toString(), c));
        t.put("Instellingen", i);
        CompoundTag e = new CompoundTag();
        eigen.forEach((u, l) -> {
            ListTag lt = new ListTag();
            l.forEach(b -> lt.add(b.opslaan()));
            e.put(u.toString(), lt);
        });
        t.put("Eigen", e);
        return t;
    }

    private static BouwSpelers laad(CompoundTag t) {
        BouwSpelers d = new BouwSpelers();
        CompoundTag i = t.getCompoundOrEmpty("Instellingen");
        for (String k : i.keySet()) {
            try {
                d.instellingen.put(UUID.fromString(k), i.getCompoundOrEmpty(k));
            } catch (IllegalArgumentException ignored) {
                // not a player: skip
            }
        }
        CompoundTag e = t.getCompoundOrEmpty("Eigen");
        for (String k : e.keySet()) {
            try {
                UUID u = UUID.fromString(k);
                ListTag lt = e.getListOrEmpty(k);
                List<Blauwdruk> l = new ArrayList<>();
                for (int n = 0; n < lt.size(); n++) {
                    l.add(Blauwdruk.laad(lt.getCompoundOrEmpty(n)));
                }
                d.eigen.put(u, l);
            } catch (IllegalArgumentException ignored) {
                // not a player: skip
            }
        }
        return d;
    }
}
