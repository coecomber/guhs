package nl.juiced.guhs.feature.guhpixel.kantoor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Every guh that is "at work" on a Guhkantoor (SavedData {@code guhs:px_kantoor}, kept with the overworld). The guh itself
 * is in here as data (GuhOpslag), so it can never be lost with a block, a chunk or a crash; the desk's block entity only
 * holds a copy of its looks to draw it. One record per guh: whose it is, at which desk, since when, and from which moment
 * the next loonstrookje counts.
 */
public final class KantoorData extends SavedData {
    public static final SavedDataType<KantoorData> TYPE = GuhSavedData.tagType("px_kantoor", KantoorData::new, KantoorData::load, KantoorData::save);

    public static final class Werk {
        public final UUID guh;
        public final UUID eigenaar;
        /** The stored guh (GuhOpslag). */
        public final CompoundTag opslag;
        public ResourceKey<Level> dim;
        public BlockPos pos;
        /** Klok.nu() stamps: clocked in; the start of the shift that is not paid yet; hours counted up to here. */
        public long sinds, loonVanaf, geteldTot;

        public Werk(UUID guh, UUID eigenaar, CompoundTag opslag, ResourceKey<Level> dim, BlockPos pos, long nu) {
            this.guh = guh;
            this.eigenaar = eigenaar;
            this.opslag = opslag;
            this.dim = dim;
            this.pos = pos.immutable();
            this.sinds = this.loonVanaf = this.geteldTot = nu;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.store("Guh", UUIDUtil.CODEC, guh);
            t.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
            t.put("Opslag", opslag);
            t.putString("Dim", dim.identifier().toString());
            t.putLong("Pos", pos.asLong());
            t.putLong("Sinds", sinds);
            t.putLong("LoonVanaf", loonVanaf);
            t.putLong("GeteldTot", geteldTot);
            return t;
        }

        @Nullable
        static Werk load(CompoundTag t) {
            UUID guh = t.read("Guh", UUIDUtil.CODEC).orElse(null), eigenaar = t.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
            Identifier dim = Identifier.tryParse(t.getStringOr("Dim", "minecraft:overworld"));
            if (guh == null || eigenaar == null) {
                return null;
            }
            Werk w = new Werk(guh, eigenaar, t.getCompoundOrEmpty("Opslag"),
                    dim == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(t.getLongOr("Pos", 0L)), 0L);
            w.sinds = t.getLongOr("Sinds", 0L);
            w.loonVanaf = t.getLongOr("LoonVanaf", w.sinds);
            w.geteldTot = t.getLongOr("GeteldTot", w.sinds);
            return w;
        }
    }

    private final Map<UUID, Werk> werk = new LinkedHashMap<>();

    public static KantoorData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    @Nullable
    public Werk van(UUID guh) {
        return werk.get(guh);
    }

    /** The guh that works at this desk (null: nobody). */
    @Nullable
    public Werk op(ResourceKey<Level> dim, BlockPos pos) {
        for (Werk w : werk.values()) {
            if (w.dim == dim && w.pos.equals(pos)) {
                return w;
            }
        }
        return null;
    }

    public List<Werk> vanEigenaar(UUID eigenaar) {
        List<Werk> uit = new ArrayList<>();
        for (Werk w : werk.values()) {
            if (w.eigenaar.equals(eigenaar)) {
                uit.add(w);
            }
        }
        return uit;
    }

    public List<Werk> alle() {
        return new ArrayList<>(werk.values());
    }

    public void zet(Werk w) {
        werk.put(w.guh, w);
        setDirty();
    }

    public void weg(UUID guh) {
        if (werk.remove(guh) != null) {
            setDirty();
        }
    }

    private CompoundTag save() {
        CompoundTag out = new CompoundTag();
        ListTag lijst = new ListTag();
        for (Werk w : werk.values()) {
            lijst.add(w.save());
        }
        out.put("Werk", lijst);
        return out;
    }

    private static KantoorData load(CompoundTag tag) {
        KantoorData data = new KantoorData();
        for (Tag raw : tag.getListOrEmpty("Werk")) {
            if (raw instanceof CompoundTag t) {
                Werk w = Werk.load(t);
                if (w != null && !w.opslag.isEmpty()) {
                    data.werk.put(w.guh, w);
                }
            }
        }
        return data;
    }
}
