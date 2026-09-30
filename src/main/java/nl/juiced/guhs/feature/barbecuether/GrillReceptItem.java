package nl.juiced.guhs.feature.barbecuether;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The Grillguh's secret recipe (the reward of his questline). With it, Aanmaakblokjes can be crafted: it stays in the
 * crafting grid, like a smithing template that never runs out.
 */
public class GrillReceptItem extends Item {
    public GrillReceptItem(Properties properties) {
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
        tooltip.accept(Component.translatable("item.guhs.grillguh_recept.lore").withStyle(ChatFormatting.GRAY));
    }
}
