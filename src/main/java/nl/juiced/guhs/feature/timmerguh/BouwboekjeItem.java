package nl.juiced.guhs.feature.timmerguh;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The Timmerguh's bouwboekje (the reward of "Samen een huisje bouwen"): with it the three guhhuisjes can be crafted. It
 * stays in the crafting grid (like the sleebouwersboek), so one is enough forever.
 */
public class BouwboekjeItem extends Item {
    public BouwboekjeItem(Properties properties) {
        super(properties);
    }

    /** Stays in the crafting grid (1.0.0: hasCraftingRemainingItem + copyWithCount(1)). */
    @Override
    public net.minecraft.world.item.ItemStackTemplate getCraftingRemainder(net.minecraft.world.item.ItemInstance instance) {
        return new net.minecraft.world.item.ItemStackTemplate(instance.typeHolder(), 1, instance instanceof ItemStack stack
                ? stack.getComponentsPatch() : net.minecraft.core.component.DataComponentPatch.EMPTY);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.timmerguh_bouwboekje.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.timmerguh_bouwboekje.lore2").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
