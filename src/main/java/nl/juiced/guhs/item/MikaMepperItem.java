package nl.juiced.guhs.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import nl.juiced.guhs.entity.MikaEntity;

/** The Mika-jager's swatter: iron sword stats and it never breaks, but it hits Mikas (Big Mika too) five times as hard. */
public class MikaMepperItem extends SwordItem {
    public MikaMepperItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        return target instanceof MikaEntity ? damage * 4f : 0f;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.mika_mepper.lore").withStyle(ChatFormatting.GRAY));
    }
}
