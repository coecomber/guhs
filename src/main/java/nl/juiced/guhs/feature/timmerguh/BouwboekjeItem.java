package nl.juiced.guhs.feature.timmerguh;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The Timmerguh's bouwboekje (the reward of "Samen een huisje bouwen"): with it the three guhhuisjes can be crafted. It
 * stays in the crafting grid (like the sleebouwersboek), so one is enough forever.
 */
public class BouwboekjeItem extends Item {
    public BouwboekjeItem(Properties properties) {
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
        tooltip.add(Component.translatable("item.guhs.timmerguh_bouwboekje.lore").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.guhs.timmerguh_bouwboekje.lore2").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
