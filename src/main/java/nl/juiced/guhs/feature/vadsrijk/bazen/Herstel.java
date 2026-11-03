package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Temporary block changes that always come back (the brief: "never permanently change the world"): the Opperzwam's broken
 * ceiling, opened temple doors, puzzle gates. {@link #zet} remembers the ORIGINAL state of a position the first time it is
 * changed under a key; {@link #terug} puts all of a key back. The originals are saved (SavedData {@code guhs:vr_bazen_herstel}
 * in the level) so a crash mid-fight is repaired at the next server start ({@link #allesTerug}).
 */
public final class Herstel extends SavedData {
    public static final SavedDataType<Herstel> TYPE = GuhSavedData.tagType("vr_bazen_herstel", Herstel::new, Herstel::load, Herstel::save);

    /** key -> (pos -> original state). */
    private final Map<String, Map<BlockPos, BlockState>> sets = new LinkedHashMap<>();

    public static Herstel van(ServerLevel sl) {
        return sl.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Sets a block, remembering what was there first (only the first change per key + position counts). */
    public static void zet(ServerLevel sl, String key, BlockPos pos, BlockState nieuw) {
        Herstel h = van(sl);
        BlockState oud = sl.getBlockState(pos);
        if (oud == nieuw) {
            return;
        }
        Map<BlockPos, BlockState> m = h.sets.computeIfAbsent(key, k -> new LinkedHashMap<>());
        m.putIfAbsent(pos.immutable(), oud);
        h.setDirty();
        sl.setBlock(pos, nieuw, 2 | 16);
    }

    public static boolean heeft(ServerLevel sl, String key) {
        Map<BlockPos, BlockState> m = van(sl).sets.get(key);
        return m != null && !m.isEmpty();
    }

    /** Everything changed under this key back as it was. Returns how many blocks. */
    public static int terug(ServerLevel sl, String key) {
        Herstel h = van(sl);
        Map<BlockPos, BlockState> m = h.sets.remove(key);
        if (m == null) {
            return 0;
        }
        for (Map.Entry<BlockPos, BlockState> e : m.entrySet()) {
            sl.setBlock(e.getKey(), e.getValue(), 2 | 16);
        }
        h.setDirty();
        return m.size();
    }

    public static List<String> sleutels(ServerLevel sl) {
        return new ArrayList<>(van(sl).sets.keySet());
    }

    /** Server start / dev command: everything back. */
    public static int allesTerug(ServerLevel sl) {
        int n = 0;
        for (String k : sleutels(sl)) {
            n += terug(sl, k);
        }
        return n;
    }

    public static void allesTerug(MinecraftServer server) {
        ServerLevel sl = nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.level(server);
        if (sl != null) {
            allesTerug(sl);
        }
    }

    private Herstel() {
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Map.Entry<String, Map<BlockPos, BlockState>> s : sets.entrySet()) {
            for (Map.Entry<BlockPos, BlockState> e : s.getValue().entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putString("k", s.getKey());
                c.putInt("x", e.getKey().getX());
                c.putInt("y", e.getKey().getY());
                c.putInt("z", e.getKey().getZ());
                c.put("s", NbtUtils.writeBlockState(e.getValue()));
                l.add(c);
            }
        }
        t.put("blokken", l);
        return t;
    }

    private static Herstel load(CompoundTag t) {
        Herstel h = new Herstel();
        ListTag l = t.getListOrEmpty("blokken");
        var blokken = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompoundOrEmpty(i);
            BlockState s = NbtUtils.readBlockState(blokken, c.getCompoundOrEmpty("s"));
            h.sets.computeIfAbsent(c.getStringOr("k", "?"), k -> new LinkedHashMap<>())
                    .put(new BlockPos(c.getIntOr("x", 0), c.getIntOr("y", 0), c.getIntOr("z", 0)), s);
        }
        return h;
    }
}
