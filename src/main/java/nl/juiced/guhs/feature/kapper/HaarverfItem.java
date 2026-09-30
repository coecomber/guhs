package nl.juiced.guhs.feature.kapper;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A bottle of hair dye: right-click your own tamed guh (with a hairstyle) to dye its hair (see {@link KapperHaar}). */
public class HaarverfItem extends Item {
    public final Haarverf verf;

    public HaarverfItem(Haarverf verf, Properties properties) {
        super(properties);
        this.verf = verf;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.haarverf.lore").withStyle(ChatFormatting.GRAY));
    }
}
