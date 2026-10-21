package nl.juiced.guhs.feature.bouwhulp.vormen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A material (design 2.4: "één blok of een mix, zoals bij de mengkwast"): one to four blocks, the first the most. Which
 * block a spot gets depends only on the spot, so a shape looks the same in the preview and when built. Immutable.
 */
public final class Mix {
    public static final int MAX = 4;
    /** The shares by number of blocks (design 2.4: 70/20/10). */
    private static final int[][] DELEN = {{100}, {70, 30}, {70, 20, 10}, {55, 25, 12, 8}};
    public static final Mix LEEG = new Mix(List.of());

    private final List<Block> blokken;

    private Mix(List<Block> blokken) {
        this.blokken = List.copyOf(blokken);
    }

    public static Mix van(Block... b) {
        List<Block> l = new ArrayList<>();
        for (Block x : b) {
            if (x != null && x != Blocks.AIR && l.size() < MAX && !l.contains(x)) {
                l.add(x);
            }
        }
        return new Mix(l);
    }

    public List<Block> blokken() {
        return blokken;
    }

    public boolean isLeeg() {
        return blokken.isEmpty();
    }

    public int aantal() {
        return blokken.size();
    }

    /** The first block (or the fallback when empty). */
    public Block eerste(Block anders) {
        return blokken.isEmpty() ? anders : blokken.get(0);
    }

    /** The block of this spot: the first in about 70 % of the spots, the rest by their share. */
    public Block kies(BlockPos p, Block anders) {
        if (blokken.size() <= 1) {
            return eerste(anders);
        }
        int[] delen = DELEN[blokken.size() - 1];
        int r = Math.floorMod(hash(p.getX(), p.getY(), p.getZ()), 100);
        for (int i = 0; i < delen.length; i++) {
            r -= delen[i];
            if (r < 0) {
                return blokken.get(i);
            }
        }
        return blokken.get(0);
    }

    /** The block for a place along 0..1 (a gradient: the blocks in their order). */
    public Block verloop(double t, BlockPos p, Block anders) {
        if (blokken.size() <= 1) {
            return eerste(anders);
        }
        // a little dithering at the borders, so the bands don't look like steps
        double d = t * (blokken.size() - 1) + (Math.floorMod(hash(p.getX(), p.getY(), p.getZ()), 100) / 100.0 - 0.5) * 0.6;
        int i = (int) Math.round(Math.max(0, Math.min(blokken.size() - 1, d)));
        return blokken.get(i);
    }

    /** With this block added at the end (or, when it is in already, moved to the front); at most four. */
    public Mix met(Block b) {
        List<Block> l = new ArrayList<>(blokken);
        if (l.remove(b)) {
            l.add(0, b);
        } else {
            if (l.size() >= MAX) {
                l.remove(l.size() - 1);
            }
            l.add(b);
        }
        return new Mix(l);
    }

    public static int hash(int x, int y, int z) {
        int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return h ^ h >>> 15;
    }

    // =====================================================================================================================

    public ListTag opslaan() {
        ListTag l = new ListTag();
        blokken.forEach(b -> l.add(StringTag.valueOf(BuiltInRegistries.BLOCK.getKey(b).toString())));
        return l;
    }

    public static Mix laad(ListTag l) {
        List<Block> b = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            Block x = blok(l.getStringOr(i, ""));
            if (x != null && !b.contains(x) && b.size() < MAX) {
                b.add(x);
            }
        }
        return new Mix(b);
    }

    @Nullable
    public static Block blok(String id) {
        Identifier i = Identifier.tryParse(id);
        if (i == null) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getOptional(i).filter(b -> b != Blocks.AIR).orElse(null);
    }

    /** The block an item places (null: not a block, or one a shape cannot be made of). */
    @Nullable
    public static Block uitItem(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem bi) {
            Block b = bi.getBlock();
            BlockState s = b.defaultBlockState();
            if (b == Blocks.AIR || s.hasBlockEntity()) {
                return null;
            }
            return b;
        }
        return null;
    }
}
