package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * An item with a little text under its name (lang &lt;item&gt;.lore, and &lt;item&gt;.loan for a loaned one): the
 * schelpjesmunt and Lilo's loaned surfplankje.
 */
public class LoreItem extends Item {
    private final boolean geleend;

    public LoreItem(Properties properties, boolean geleend) {
        super(properties);
        this.geleend = geleend;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        if (geleend) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".loan").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }
    }
}
