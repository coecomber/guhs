package nl.juiced.guhs.feature.wereldleven;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** A kaasijsje (IJscoguh Tingeling): eat it yourself, or right-click a guh with it (Kaasijsjes). */
public class KaasijsjeItem extends Item {
    public final Kaasijsjes.Smaak smaak;

    public KaasijsjeItem(Kaasijsjes.Smaak smaak, Properties properties) {
        super(properties);
        this.smaak = smaak;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            Kaasijsjes.eet(entity, smaak);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs." + smaak.itemId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (smaak.seizoen != null) {
            tooltip.accept(Component.translatable("gui.guhs.wereldleven.seizoensijsje", smaak.seizoen.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.accept(Component.translatable("gui.guhs.wereldleven.ijsje_tip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
