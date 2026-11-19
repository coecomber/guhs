package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a lootjes plot held at 5 Dec 18:00: the 7x7x7 block states, plus the data of the few block entities that are pure
 * decoration (sign text, a head's skin, a banner's pattern: {@link BouwRegels#decorBe}). Never an inventory.
 * <p>Cell index = {@code (y * 7 + z) * 7 + x}, x/y/z = 0..6 from the plot's min corner. A gift is stored the "front"
 * (the side the plot's gate was on) to the SOUTH (+z): {@link Lootjes} turns each plot that way when it captures.
 * <p>NBT: {@code P} = palette (block states), {@code B} = one palette index per cell (byte array, or int array for more than
 * 256 different states), {@code E} = list of {I: cell, T: block entity tag (with its "id")}, {@code N} = how many blocks.
 */
public final class Inhoud {
    public static final int N = 7, V = N * N * N;
    private static final BlockState LUCHT = Blocks.AIR.defaultBlockState();

    private final BlockState[] blokken = new BlockState[V];
    private final Map<Integer, CompoundTag> be = new TreeMap<>();

    public Inhoud() {
        Arrays.fill(blokken, LUCHT);
    }

    public static int index(int x, int y, int z) {
        return (y * N + z) * N + x;
    }

    public static int x(int i) {
        return i % N;
    }

    public static int z(int i) {
        return (i / N) % N;
    }

    public static int y(int i) {
        return i / (N * N);
    }

    public BlockState get(int i) {
        return blokken[i];
    }

    public BlockState get(int x, int y, int z) {
        return blokken[index(x, y, z)];
    }

    /** Puts a block in a cell ({@code beTag}: its decorative block-entity data with "id", or null). */
    public void zet(int i, BlockState s, @Nullable CompoundTag beTag) {
        blokken[i] = s == null ? LUCHT : s;
        if (beTag == null || s == null || s.isAir()) {
            be.remove(i);
        } else {
            be.put(i, beTag);
        }
    }

    @Nullable
    public CompoundTag be(int i) {
        return be.get(i);
    }

    public Map<Integer, CompoundTag> beAlle() {
        return Collections.unmodifiableMap(be);
    }

    /** How many cells hold a block. */
    public int aantal() {
        int n = 0;
        for (BlockState s : blokken) {
            if (!s.isAir()) {
                n++;
            }
        }
        return n;
    }

    public boolean leeg() {
        return aantal() == 0;
    }

    /** The first cell with a block (the gift's "kern" stands there when placed), or -1. */
    public int eerste() {
        for (int i = 0; i < V; i++) {
            if (!blokken[i].isAir()) {
                return i;
            }
        }
        return -1;
    }

    // --- turning -----------------------------------------------------------------------------------------------------------

    /** Where cell (x, z) lands when the 7x7 footprint turns by {@code r} (around its middle). */
    public static int[] draai(int x, int z, Rotation r) {
        return switch (r) {
            case NONE -> new int[]{x, z};
            case CLOCKWISE_90 -> new int[]{N - 1 - z, x};
            case CLOCKWISE_180 -> new int[]{N - 1 - x, N - 1 - z};
            case COUNTERCLOCKWISE_90 -> new int[]{z, N - 1 - x};
        };
    }

    /** A turned copy: the cells move and every block state turns along (stairs, signs, heads...). */
    public Inhoud gedraaid(Rotation r) {
        Inhoud uit = new Inhoud();
        for (int i = 0; i < V; i++) {
            if (blokken[i].isAir()) {
                continue;
            }
            int[] p = draai(x(i), z(i), r);
            int j = index(p[0], y(i), p[1]);
            uit.blokken[j] = r == Rotation.NONE ? blokken[i] : blokken[i].rotate(r);
            CompoundTag t = be.get(i);
            if (t != null) {
                uit.be.put(j, t);
            }
        }
        return uit;
    }

    // --- capturing ---------------------------------------------------------------------------------------------------------

    /**
     * Reads the 7x7x7 room with its min corner at {@code min}: every block that may be in a gift ({@link BouwRegels#magBlok},
     * water taken out of it), and the data of decorative block entities. Anything else becomes air (an inventory never travels).
     */
    public static Inhoud vang(ServerLevel level, BlockPos min) {
        Inhoud in = new Inhoud();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < V; i++) {
            p.set(min.getX() + x(i), min.getY() + y(i), min.getZ() + z(i));
            BlockState s = BouwRegels.schoon(level.getBlockState(p));
            if (s.isAir()) {
                continue;
            }
            CompoundTag tag = null;
            BlockEntity b = level.getBlockEntity(p);
            if (b != null) {
                if (!BouwRegels.decorBe(b.getType())) {
                    continue;   // (a block entity that isn't pure decoration: left out)
                }
                tag = beTag(level, b);
            }
            in.zet(i, s, tag);
        }
        return in;
    }

    /** A decorative block entity's data, ready for BlockEntity.loadStatic (with "id", without position or mod data). */
    static CompoundTag beTag(ServerLevel level, BlockEntity b) {
        CompoundTag t = b.saveWithoutMetadata(level.registryAccess());
        t.remove("NeoForgeData");
        t.remove("neoforge:attachments");
        var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(b.getType());
        if (id != null) {
            t.putString("id", id.toString());
        }
        return t;
    }

    // --- saving ------------------------------------------------------------------------------------------------------------

    public CompoundTag opslaan() {
        List<BlockState> palet = new ArrayList<>();
        Map<BlockState, Integer> nummer = new HashMap<>();
        int[] cel = new int[V];
        for (int i = 0; i < V; i++) {
            cel[i] = nummer.computeIfAbsent(blokken[i], s -> {
                palet.add(s);
                return palet.size() - 1;
            });
        }
        CompoundTag t = new CompoundTag();
        ListTag p = new ListTag();
        for (BlockState s : palet) {
            p.add(BlockState.CODEC.encodeStart(NbtOps.INSTANCE, s).result().orElseGet(CompoundTag::new));
        }
        t.put("P", p);
        if (palet.size() <= 256) {
            byte[] b = new byte[V];
            for (int i = 0; i < V; i++) {
                b[i] = (byte) cel[i];
            }
            t.put("B", new ByteArrayTag(b));
        } else {
            t.put("B", new IntArrayTag(cel));
        }
        ListTag e = new ListTag();
        be.forEach((i, tag) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("I", i);
            c.put("T", tag.copy());
            e.add(c);
        });
        t.put("E", e);
        t.putInt("N", aantal());
        return t;
    }

    /** Reads {@link #opslaan}; unknown blocks (a mod that is gone) become air. */
    public static Inhoud laad(CompoundTag t) {
        Inhoud in = new Inhoud();
        ListTag p = t.getListOrEmpty("P");
        BlockState[] palet = new BlockState[p.size()];
        for (int i = 0; i < p.size(); i++) {
            palet[i] = BlockState.CODEC.parse(NbtOps.INSTANCE, p.get(i)).result().orElse(LUCHT);
        }
        Tag b = t.get("B");
        for (int i = 0; i < V; i++) {
            int k = b instanceof ByteArrayTag ba && i < ba.getAsByteArray().length ? ba.getAsByteArray()[i] & 0xFF
                    : b instanceof IntArrayTag ia && i < ia.getAsIntArray().length ? ia.getAsIntArray()[i] : 0;
            in.blokken[i] = k >= 0 && k < palet.length ? palet[k] : LUCHT;
        }
        for (Tag x : t.getListOrEmpty("E")) {
            if (x instanceof CompoundTag c) {
                int i = c.getIntOr("I", -1);
                CompoundTag tag = c.getCompoundOrEmpty("T");
                if (i >= 0 && i < V && !in.blokken[i].isAir() && !tag.isEmpty()) {
                    in.be.put(i, tag);
                }
            }
        }
        return in;
    }

    /** The number of blocks in a saved gift without reading it all. */
    public static int aantal(CompoundTag t) {
        return t.getIntOr("N", 0);
    }

    /** Same blocks and the same block-entity data? */
    public boolean zelfde(Inhoud o) {
        return o != null && Arrays.equals(blokken, o.blokken) && be.equals(o.be);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Inhoud i && zelfde(i);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(blokken), be);
    }
}
