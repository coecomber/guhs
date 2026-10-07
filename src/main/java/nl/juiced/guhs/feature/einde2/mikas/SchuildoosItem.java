package nl.juiced.guhs.feature.einde2.mikas;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * The Schuildoos in your pockets: it carries what was in the block (the container component; the game lists it in the
 * tooltip by itself). It does not fit inside another Schuildoos, a shulker box or a bundle.
 */
public class SchuildoosItem extends BlockItem {
    public SchuildoosItem(Block block, Properties properties) {
        super(block, properties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("block.guhs.schuildoos.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
