package nl.juiced.guhs.feature.bakkerij;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The feesttaart: Bakker Korstje's three-layer party cake for the Grote Knusfeest (item tag guhs:knus/feesttaart),
 * packed in a pink box with a bow. Not for eating on the way - it goes to Burgemeester Vadsema (and look out for
 * Kruimel-Mika's...).
 */
public class FeesttaartItem extends Item {
    public FeesttaartItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.feesttaart.lore").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.accept(Component.translatable("item.guhs.feesttaart.lore2").withStyle(ChatFormatting.GRAY));
    }
}
