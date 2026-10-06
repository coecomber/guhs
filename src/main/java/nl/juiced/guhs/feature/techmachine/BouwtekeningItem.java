package nl.juiced.guhs.feature.techmachine;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * De Bouwtekening: a blue sheet of paper. Empty it is just that; on a Tekentafel a crafting recipe is drawn on it (the
 * data component {@code guhs:bouwtekening}, {@link Bouwtekening}), and then its name and tooltip say what it makes and
 * what that takes. A Knutselmachine crafts what its drawing shows.
 */
public class BouwtekeningItem extends Item {
    public BouwtekeningItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Bouwtekening tekening = Bouwtekeningen.lees(stack);
        if (tekening == null) {
            return Component.translatable("item.guhs.bouwtekening.leeg");
        }
        return Component.translatable("item.guhs.bouwtekening.van", tekening.resultaat().getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        Bouwtekening tekening = Bouwtekeningen.lees(stack);
        if (tekening == null) {
            tooltip.accept(Component.translatable("item.guhs.bouwtekening.leeg.lore").withStyle(ChatFormatting.GRAY));
            return;
        }
        ItemStack maakt = tekening.resultaat();
        tooltip.accept(Component.translatable("item.guhs.bouwtekening.maakt", maakt.getCount(), maakt.getHoverName()).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("item.guhs.bouwtekening.nodig").withStyle(ChatFormatting.GRAY));
        for (ItemStack wat : tekening.ingredienten()) {
            tooltip.accept(Component.translatable("item.guhs.bouwtekening.regel", wat.getCount(), wat.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }
}
