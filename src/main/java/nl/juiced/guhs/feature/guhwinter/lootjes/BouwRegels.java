package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.GlowInkSacItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.InkSacItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import nl.juiced.guhs.Guhs;

/**
 * What may stand in a lootjes plot (and so travel in a gift). Design R5: everything decorative may (diamond blocks too,
 * signs with text, lanterns, banners, heads, flowers, candles, carpets, stairs, slabs, walls, doors...), but no spawners,
 * no working machines and no blocks with an inventory. Three layers:
 * <ol>
 *   <li>the block tag {@code guhs:guhwinter_lootje_verboden} (pistons, TNT, barriers, light blocks, structure voids, powered
 *       rails, cauldrons, composters, eggs that hatch, infested blocks, saplings, fire, portals... and the vanilla machines
 *       by name, for the wiki);</li>
 *   <li>every block that gives a redstone signal ({@code isSignalSource}: levers, buttons, pressure plates, redstone,
 *       repeaters, observers, targets, lightning rods; modded ones too);</li>
 *   <li>every block with a block entity, unless that block entity is pure decoration ({@link #decorBe}: signs, hanging signs,
 *       heads, banners, beds, bells): chests, hoppers, furnaces, spawners, command blocks, structure blocks, jigsaws, every
 *       modded Guhs machine... The tag {@code guhs:guhwinter_lootje_mag} lets a modded decoration block through anyway.</li>
 * </ol>
 * Liquids never. Waterlogged blocks lose their water ({@link #schoon}).
 */
public final class BouwRegels {
    public static final TagKey<net.minecraft.world.level.block.Block> VERBODEN = TagKey.create(Registries.BLOCK, Guhs.id("guhwinter_lootje_verboden"));
    public static final TagKey<net.minecraft.world.level.block.Block> MAG = TagKey.create(Registries.BLOCK, Guhs.id("guhwinter_lootje_mag"));

    private static Set<BlockEntityType<?>> decor;

    private BouwRegels() {
    }

    private static Set<BlockEntityType<?>> decor() {
        if (decor == null) {
            decor = Set.of(BlockEntityType.SIGN, BlockEntityType.HANGING_SIGN, BlockEntityType.SKULL, BlockEntityType.BANNER,
                    BlockEntityType.BED, BlockEntityType.BELL);
        }
        return decor;
    }

    /** Is this block entity only looks (its data may travel with a gift)? */
    public static boolean decorBe(BlockEntityType<?> type) {
        return decor().contains(type);
    }

    /** May this block stand in a lootjes plot / a gift? */
    public static boolean magBlok(BlockState s) {
        if (s.isAir()) {
            return true;
        }
        if (s.is(VERBODEN)) {
            return false;
        }
        Block b = s.getBlock();
        if (b instanceof SurpriseBlok) {
            return false;   // (a gift in a gift: no)
        }
        if (s.is(MAG)) {
            return true;
        }
        if (b instanceof LiquidBlock || (!s.getFluidState().isEmpty() && !s.hasProperty(BlockStateProperties.WATERLOGGED))) {
            return false;
        }
        if (s.isSignalSource()) {
            return false;
        }
        if (b instanceof EntityBlock) {
            for (BlockEntityType<?> t : decor()) {
                if (t.isValid(s)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /** The block as it goes into a gift: without water; a forbidden block becomes air. */
    public static BlockState schoon(BlockState s) {
        if (!magBlok(s)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        if (s.hasProperty(BlockStateProperties.WATERLOGGED) && s.getValue(BlockStateProperties.WATERLOGGED)) {
            s = s.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return s;
    }

    /**
     * May a builder use this item on a block in their plot? Blocks (checked again when placed), and the decorating tools:
     * axe (strip), shovel (path), hoe, shears, dyes and (glow) ink on signs, honeycomb (wax). Nothing else: no spawn eggs,
     * buckets, flint and steel, bone meal, minecarts, boats, armour stands, item frames, paintings...
     */
    public static boolean magItemOpBlok(ItemStack stack) {
        var i = stack.getItem();
        return stack.isEmpty() || i instanceof BlockItem || i instanceof AxeItem || i instanceof ShovelItem || i instanceof HoeItem
                || i instanceof ShearsItem || i instanceof DyeItem || i instanceof InkSacItem || i instanceof GlowInkSacItem
                || i instanceof HoneycombItem;
    }
}
