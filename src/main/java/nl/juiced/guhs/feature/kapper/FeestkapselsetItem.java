package nl.juiced.guhs.feature.kapper;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** The feestkapselset (Knusfeest task FEESTKAPSELS, tag guhs:knus/feestkapsels): bring it to Burgemeester Vadsema. */
public class FeestkapselsetItem extends Item {
    public FeestkapselsetItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.feestkapselset.lore").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
