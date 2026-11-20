package nl.juiced.guhs.feature.bouwhulp.kern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * What a spookbouw builds (deel 2, F8): a block map. Positions are relative and start at 0,0,0 (the low corner of the box);
 * a state may be {@link Blocks#AIR}: "make this spot empty" (a hollow shape inside a hill, a tunnel). Immutable.
 * <p>
 * Placed with an origin, a {@link Rotation} and a {@link Mirror} ({@link #geplaatst}): it turns around the middle of its
 * footprint, so turning a ghost keeps it where it was. Every slice builds its own: a shape (F9), a blueprint (F9), a
 * helper's template (F10).
 */
public final class Ontwerp {
    private final Map<BlockPos, BlockState> blokken;
    private final Vec3i grootte;

    private Ontwerp(Map<BlockPos, BlockState> blokken, Vec3i grootte) {
        this.blokken = blokken;
        this.grootte = grootte;
    }

    /**
     * An ontwerp of these blocks (any positions: they are moved so the box starts at 0,0,0). Spots without a block are
     * left alone when it is built.
     */
    public static Ontwerp van(Map<BlockPos, BlockState> blokken) {
        if (blokken.isEmpty()) {
            return new Ontwerp(Map.of(), Vec3i.ZERO);
        }
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (BlockPos p : blokken.keySet()) {
            x0 = Math.min(x0, p.getX());
            y0 = Math.min(y0, p.getY());
            z0 = Math.min(z0, p.getZ());
            x1 = Math.max(x1, p.getX());
            y1 = Math.max(y1, p.getY());
            z1 = Math.max(z1, p.getZ());
        }
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, BlockState> e : blokken.entrySet()) {
            uit.put(e.getKey().offset(-x0, -y0, -z0).immutable(), e.getValue());
        }
        return new Ontwerp(Collections.unmodifiableMap(uit), new Vec3i(x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1));
    }

    /** The blocks, relative to the low corner. */
    public Map<BlockPos, BlockState> blokken() {
        return blokken;
    }

    /** The size of the box (x, y, z). */
    public Vec3i grootte() {
        return grootte;
    }

    public int aantal() {
        return blokken.size();
    }

    public boolean isLeeg() {
        return blokken.isEmpty();
    }

    /** The longest side of the box (what the levels limit: "vormen ≤ 24"). */
    public int maat() {
        return Math.max(grootte.getX(), Math.max(grootte.getY(), grootte.getZ()));
    }

    /** The middle of the footprint: what a ghost turns around. */
    public BlockPos spil() {
        return new BlockPos((grootte.getX() - 1) / 2, 0, (grootte.getZ() - 1) / 2);
    }

    /** Where a relative spot lands: origin + the spot turned and mirrored around {@link #spil}. */
    public BlockPos plek(BlockPos rel, BlockPos oorsprong, Rotation draai, Mirror spiegel) {
        return StructureTemplate.transform(rel, spiegel, draai, spil()).offset(oorsprong);
    }

    /** The state as it lands (turned and mirrored like the spot). */
    public static BlockState staat(BlockState s, Rotation draai, Mirror spiegel) {
        return s.mirror(spiegel).rotate(draai);
    }

    /**
     * Every block as it lands in the world, in building order: from the bottom up, and per layer the solid blocks before
     * what leans on them (torches, carpets, doors, plants), the empty spots ("dig out") first.
     */
    public List<Map.Entry<BlockPos, BlockState>> geplaatst(BlockPos oorsprong, Rotation draai, Mirror spiegel) {
        List<Map.Entry<BlockPos, BlockState>> uit = new ArrayList<>(blokken.size());
        for (Map.Entry<BlockPos, BlockState> e : blokken.entrySet()) {
            uit.add(Map.entry(plek(e.getKey(), oorsprong, draai, spiegel).immutable(), staat(e.getValue(), draai, spiegel)));
        }
        uit.sort(BOUWVOLGORDE);
        return uit;
    }

    /** Bottom up; empty spots first, then full blocks, then what needs something to stand on. */
    public static final Comparator<Map.Entry<BlockPos, BlockState>> BOUWVOLGORDE = Comparator
            .<Map.Entry<BlockPos, BlockState>>comparingInt(e -> e.getKey().getY())
            .thenComparingInt(e -> e.getValue().isAir() ? 0 : e.getValue().canOcclude() ? 1 : 2)
            .thenComparingLong(e -> e.getKey().asLong());

    /** The items it takes (what {@link Materiaal#kost} says per block; empty spots and free blocks take nothing). */
    public Map<Item, Integer> nodig() {
        Map<Item, Integer> uit = new HashMap<>();
        for (BlockState s : blokken.values()) {
            Materiaal.Kost k = Materiaal.kost(s);
            if (k != null) {
                k.telOp(uit);
            }
        }
        return uit;
    }

    /** A copy with the blocks this function gives instead (null: leave the spot out). */
    public Ontwerp vervang(java.util.function.Function<BlockState, BlockState> wat) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, BlockState> e : blokken.entrySet()) {
            BlockState s = wat.apply(e.getValue());
            if (s != null) {
                uit.put(e.getKey(), s);
            }
        }
        return van(uit);
    }

    // =====================================================================================================================
    // saving: a palette and two int arrays (packed spots, palette indexes)
    // =====================================================================================================================

    public CompoundTag opslaan() {
        CompoundTag t = new CompoundTag();
        Map<BlockState, Integer> index = new LinkedHashMap<>();
        int[] plekken = new int[blokken.size()], staten = new int[blokken.size()];
        int i = 0;
        for (Map.Entry<BlockPos, BlockState> e : blokken.entrySet()) {
            plekken[i] = pak(e.getKey());
            staten[i] = index.computeIfAbsent(e.getValue(), s -> index.size());
            i++;
        }
        ListTag palet = new ListTag();
        for (BlockState s : index.keySet()) {
            palet.add(NbtUtils.writeBlockState(s));
        }
        t.put("Palet", palet);
        t.put("Plekken", new IntArrayTag(plekken));
        t.put("Staten", new IntArrayTag(staten));
        return t;
    }

    public static Ontwerp laad(CompoundTag t) {
        ListTag palet = t.getListOrEmpty("Palet");
        List<BlockState> staten = new ArrayList<>(palet.size());
        for (int i = 0; i < palet.size(); i++) {
            staten.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK, palet.getCompoundOrEmpty(i)));
        }
        int[] plekken = t.getIntArray("Plekken").orElse(new int[0]);
        int[] idx = t.getIntArray("Staten").orElse(new int[0]);
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(plekken.length, idx.length); i++) {
            if (idx[i] >= 0 && idx[i] < staten.size()) {
                uit.put(pak(plekken[i]), staten.get(idx[i]));
            }
        }
        return van(uit);
    }

    /** A relative spot (0..1023 per axis) in one int. */
    static int pak(BlockPos p) {
        return (p.getX() & 1023) << 20 | (p.getY() & 1023) << 10 | p.getZ() & 1023;
    }

    static BlockPos pak(int v) {
        return new BlockPos(v >> 20 & 1023, v >> 10 & 1023, v & 1023);
    }

    // =====================================================================================================================
    // 1.10.10: from a structure .nbt
    // =====================================================================================================================

    /** Ground a structure was saved on (the ground slab {@code zonderFundering} leaves out; guhs: knuffelgras, bleekmos). */
    private static final java.util.Set<Identifier> FUNDERING = java.util.Set.of(Identifier.withDefaultNamespace("dirt"),
            Identifier.withDefaultNamespace("grass_block"), Identifier.withDefaultNamespace("stone"), Identifier.withDefaultNamespace("pink_wool"),
            Identifier.withDefaultNamespace("coarse_dirt"), Identifier.withDefaultNamespace("bedrock"), Identifier.withDefaultNamespace("rooted_dirt"),
            Identifier.withDefaultNamespace("deepslate"), Identifier.fromNamespaceAndPath("guhs", "knuffelgras"),
            Identifier.fromNamespaceAndPath("guhs", "bleekmos"));
    /** Blocks without an item that a structure design builds as something else (the huisje's view window: glass). */
    private static final Map<Identifier, Block> STANDIN = Map.of(Identifier.fromNamespaceAndPath("guhs", "huisje_raam"), Blocks.GLASS);
    /** A bottom layer belongs to the ground slab when at least this part of its footprint is ground. */
    private static final double GROND_DEEL = 0.45;

    /**
     * An ontwerp from a structure file of the mod or a datapack ({@code data/<ns>/structure/<path>.nbt}, read through the
     * server's StructureTemplateManager, so data-fixed like any structure). Left out: air (the build does not dig),
     * structure voids, jigsaws and structure blocks, every block without an item ({@link Materiaal#kost} null: fire, a
     * fluid that is not water) and, with {@code zonderFundering}, whole bottom layers of plain dirt / grass / coarse dirt /
     * stone (the piece of ground a structure was saved with). Block entities' contents (chest loot, signs) are never
     * taken: only the block state. Empty when the file does not exist.
     */
    public static Ontwerp uitStructuur(MinecraftServer server, Identifier id, boolean zonderFundering) {
        return uitStructuur(server, id, zonderFundering, null);
    }

    /** The same; {@code zonderItem} (may be null) gets the blocks that were left out because they have no item. */
    public static Ontwerp uitStructuur(MinecraftServer server, Identifier id, boolean zonderFundering, @javax.annotation.Nullable Map<Block, Integer> zonderItem) {
        java.util.Optional<StructureTemplate> t = server.getStructureManager().get(id);
        if (t.isEmpty()) {
            return van(Map.of());
        }
        CompoundTag tag = t.get().save(new CompoundTag());
        ListTag palet = tag.contains("palette") ? tag.getListOrEmpty("palette") : tag.getListOrEmpty("palettes").getListOrEmpty(0);
        List<BlockState> staten = new ArrayList<>(palet.size());
        for (int i = 0; i < palet.size(); i++) {
            staten.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK, palet.getCompoundOrEmpty(i)));
        }
        ListTag lijst = tag.getListOrEmpty("blocks");
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        for (int i = 0; i < lijst.size(); i++) {
            CompoundTag b = lijst.getCompoundOrEmpty(i);
            int idx = b.getIntOr("state", -1);
            ListTag pos = b.getListOrEmpty("pos");
            if (idx < 0 || idx >= staten.size() || pos.size() < 3) {
                continue;
            }
            BlockState s = staten.get(idx);
            if (s.isAir() || s.is(Blocks.STRUCTURE_VOID) || s.is(Blocks.JIGSAW) || s.is(Blocks.STRUCTURE_BLOCK)) {
                continue;
            }
            if (Materiaal.kost(s) == null && STANDIN.containsKey(BuiltInRegistries.BLOCK.getKey(s.getBlock()))) {
                s = STANDIN.get(BuiltInRegistries.BLOCK.getKey(s.getBlock())).defaultBlockState();   // a block without an item that has a buildable stand-in
            }
            if (Materiaal.kost(s) == null) {
                if (zonderItem != null) {
                    zonderItem.merge(s.getBlock(), 1, Integer::sum);
                }
                continue;
            }
            m.put(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), s);
        }
        if (zonderFundering) {
            Vec3i maat = t.get().getSize();
            zonderFundering(m, maat.getX() * maat.getZ());
        }
        return van(m);
    }

    /**
     * Leaves out the ground slab (the same rule as the blueprint renders, blauwdruk_kandidaten/_bron/kandkit.py): from the
     * lowest layer up, every layer that is for at least {@link #GROND_DEEL} of the footprint ({@code vlak} = x * z of the
     * file) {@link #FUNDERING} ground belongs to the slab; in each column, ground blocks of the slab go from the bottom up
     * to the first real building block. Ground higher up (pink wool walls) stays.
     */
    static void zonderFundering(Map<BlockPos, BlockState> m, int vlak) {
        if (m.isEmpty()) {
            return;
        }
        int laag = m.keySet().stream().mapToInt(BlockPos::getY).min().orElse(0);
        int hoog = m.keySet().stream().mapToInt(BlockPos::getY).max().orElse(0);
        int y = laag;
        while (y <= hoog) {
            int yy = y;
            long grond = m.entrySet().stream().filter(e -> e.getKey().getY() == yy && grond(e.getValue())).count();
            if (grond < GROND_DEEL * vlak) {
                break;
            }
            y++;
        }
        int bovenGrond = y - 1;
        if (bovenGrond < laag) {
            return;
        }
        Map<Long, List<BlockPos>> kolommen = new HashMap<>();
        for (BlockPos p : m.keySet()) {
            if (p.getY() <= bovenGrond) {
                kolommen.computeIfAbsent(((long) p.getX() << 32) ^ (p.getZ() & 0xffffffffL), k -> new ArrayList<>()).add(p);
            }
        }
        for (List<BlockPos> kolom : kolommen.values()) {
            kolom.sort(Comparator.comparingInt(BlockPos::getY));
            for (BlockPos p : kolom) {
                if (!grond(m.get(p))) {
                    break;   // the first real building block: the rest of the column stays
                }
                m.remove(p);
            }
        }
    }

    private static boolean grond(BlockState s) {
        return FUNDERING.contains(BuiltInRegistries.BLOCK.getKey(s.getBlock()));
    }

    /** (for tests and the dev command) a filled box of one block. */
    public static Ontwerp doos(int x, int y, int z, BlockState s) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        for (int a = 0; a < x; a++) {
            for (int b = 0; b < y; b++) {
                for (int c = 0; c < z; c++) {
                    m.put(new BlockPos(a, b, c), s);
                }
            }
        }
        return van(m);
    }
}
