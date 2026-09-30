package nl.juiced.guhs.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import nl.juiced.guhs.entity.MikaEntity;

/**
 * The Mika-jager's swatter: iron sword stats and it never breaks, but it hits Mikas (Big Mika too) five times as hard.
 * 26.1: SwordItem is gone - the sword stats come from {@code Item.Properties#sword(ToolMaterial.IRON, 3, -2.4)} (ModItems).
 */
public class MikaMepperItem extends Item {
    public MikaMepperItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        return target instanceof MikaEntity ? damage * 4f : 0f;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.mika_mepper.lore").withStyle(ChatFormatting.GRAY));
    }
}
