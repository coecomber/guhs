package nl.juiced.guhs.feature.kapper;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Krulletje's kappersschaar (lent, tag guhs:loaned: it stays with you). Right-click a customer in the kappersshow to
 * open the knip screen; on your own tamed guh it tells what hairstyle it has, and shift + right-click cuts it off.
 */
public class KappersschaarItem extends Item {
    public KappersschaarItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.kappersschaar.lore").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.guhs.kappersschaar.lore2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
