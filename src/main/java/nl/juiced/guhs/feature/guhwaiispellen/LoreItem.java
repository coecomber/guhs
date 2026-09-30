package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        if (geleend) {
            tooltip.add(Component.translatable(getDescriptionId() + ".loan").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }
    }
}
