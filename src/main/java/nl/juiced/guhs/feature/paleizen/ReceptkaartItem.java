package nl.juiced.guhs.feature.paleizen;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Tolwachter's building plan of the bridge (bbq2, a recipe card: CONTRACT_130 10a D5): the recipes of the
 * Mika-brugplanken and the touwleuning need it, and it stays in the crafting grid, so one is enough for ever (he gives a new
 * one when it got lost).
 */
public class ReceptkaartItem extends Item {
    public ReceptkaartItem(Properties properties) {
        super(properties);
    }

    /** Stays in the crafting grid (like the Timmerguh's bouwboekje). */
    @Override
    public net.minecraft.world.item.ItemStackTemplate getCraftingRemainder(net.minecraft.world.item.ItemInstance instance) {
        return new net.minecraft.world.item.ItemStackTemplate(instance.typeHolder(), 1, instance instanceof ItemStack stack
                ? stack.getComponentsPatch() : net.minecraft.core.component.DataComponentPatch.EMPTY);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore2").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
