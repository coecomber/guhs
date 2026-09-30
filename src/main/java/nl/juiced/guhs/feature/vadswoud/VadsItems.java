package nl.juiced.guhs.feature.vadswoud;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** The Vadswoud's items with a line of lore (lang key: the item's own key + ".lore"). */
public final class VadsItems {
    /** Knabbelbessen: food, and planted they grow into a knabbelbessen bush (like sweet berries). */
    public static class Bessen extends BlockItem {
        public Bessen(Block bush, Properties properties) {
            super(bush, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** A block item with lore. */
    public static class LoreBlock extends BlockItem {
        public LoreBlock(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private VadsItems() {
    }
}
