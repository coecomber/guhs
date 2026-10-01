package nl.juiced.guhs.feature.weerder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Every Wilde-guhweerder of one dimension (1.2.0; SavedData {@code guhs:wilde_guhweerders} per dimension): its position and
 * area radius, plus a chunk index (rebuilt from that list, never saved) so the spawn check is a single map lookup: which
 * weerders reach the chunk of this spawn spot? A weerder's area is a cylinder: within {@code straal} blocks horizontally of
 * the block, from {@code straal} blocks below it to {@code straal} blocks above it. The index is kept by the block itself
 * (placed, radius changed, removed) and healed when its block entity loads, so weerders in unloaded chunks still keep wild
 * guhs out of the loaded chunks around them.
 */
public final class WeerderIndex extends SavedData {
    static final SavedDataType<WeerderIndex> TYPE = GuhSavedData.tagType("wilde_guhweerders", WeerderIndex::new, WeerderIndex::load,
            i -> i.save(new CompoundTag()));

    /** Weerder position (BlockPos#asLong) -> its area radius. */
    private final Map<Long, Integer> weerders = new HashMap<>();
    /** ChunkPos#asLong -> the weerders whose area reaches into that chunk (positions). */
    private final Long2ObjectOpenHashMap<long[]> perChunk = new Long2ObjectOpenHashMap<>();

    public WeerderIndex() {
    }

    static WeerderIndex get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // =====================================================================================================================

    /** Is this spot inside the area of a Wilde-guhweerder in this level (no wild guhs spawn here)? Cheap: one map lookup. */
    public static boolean beschermd(ServerLevel level, BlockPos pos) {
        return get(level).binnen(pos.getX(), pos.getY(), pos.getZ());
    }

    boolean binnen(int x, int y, int z) {
        if (weerders.isEmpty()) {
            return false;
        }
        long[] hier = perChunk.get(ChunkPos.pack(x >> 4, z >> 4));
        if (hier == null) {
            return false;
        }
        for (long p : hier) {
            Integer straal = weerders.get(p);
            if (straal == null) {
                continue;
            }
            int dx = BlockPos.getX(p) - x, dy = BlockPos.getY(p) - y, dz = BlockPos.getZ(p) - z;
            if (Math.abs(dy) <= straal && (long) dx * dx + (long) dz * dz <= (long) straal * straal) {
                return true;
            }
        }
        return false;
    }

    /** A weerder at pos with this radius (new or changed). */
    static void zet(ServerLevel level, BlockPos pos, int straal) {
        WeerderIndex i = get(level);
        Integer oud = i.weerders.put(pos.asLong(), straal);
        if (oud == null || oud != straal) {
            i.herbouw();
            i.setDirty();
        }
    }

    /** The weerder at pos is gone. */
    static void weg(ServerLevel level, BlockPos pos) {
        WeerderIndex i = get(level);
        if (i.weerders.remove(pos.asLong()) != null) {
            i.herbouw();
            i.setDirty();
        }
    }

    /** (tests) the radius registered at pos, or -1. */
    static int straalOp(ServerLevel level, BlockPos pos) {
        Integer s = get(level).weerders.get(pos.asLong());
        return s == null ? -1 : s;
    }

    private void herbouw() {
        Map<Long, List<Long>> lijsten = new HashMap<>();
        for (Map.Entry<Long, Integer> e : weerders.entrySet()) {
            long p = e.getKey();
            int r = e.getValue(), x = BlockPos.getX(p), z = BlockPos.getZ(p);
            for (int cx = (x - r) >> 4; cx <= (x + r) >> 4; cx++) {
                for (int cz = (z - r) >> 4; cz <= (z + r) >> 4; cz++) {
                    lijsten.computeIfAbsent(ChunkPos.pack(cx, cz), k -> new ArrayList<>()).add(p);
                }
            }
        }
        perChunk.clear();
        lijsten.forEach((k, v) -> perChunk.put((long) k, v.stream().mapToLong(Long::longValue).toArray()));
    }

    // =====================================================================================================================

    private static WeerderIndex load(CompoundTag tag) {
        WeerderIndex i = new WeerderIndex();
        ListTag list = tag.getListOrEmpty("Weerders");
        for (int n = 0; n < list.size(); n++) {
            CompoundTag c = list.getCompoundOrEmpty(n);
            i.weerders.put(c.getLongOr("Pos", 0L), Math.max(1, c.getIntOr("Straal", WeerderFeature.STANDAARD)));
        }
        i.herbouw();
        return i;
    }

    private CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        weerders.forEach((p, r) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", p);
            c.putInt("Straal", r);
            list.add(c);
        });
        tag.put("Weerders", list);
        return tag;
    }
}
