package nl.juiced.guhs.feature.barbecuether;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The Grillguh's secret recipe (the reward of his questline). With it, Aanmaakblokjes can be crafted: it stays in the
 * crafting grid, like a smithing template that never runs out.
 */
public class GrillReceptItem extends Item {
    public GrillReceptItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.grillguh_recept.lore").withStyle(ChatFormatting.GRAY));
    }
}
