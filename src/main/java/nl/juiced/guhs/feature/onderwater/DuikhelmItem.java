package nl.juiced.guhs.feature.onderwater;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
/**
 * The duikhelm: a brass diving helmet with a big round window (sold by the Zeemeerguh). Worn with your head under
 * water you can breathe there (and see a little better); out of the water it wears off after a few seconds.
 */
public class DuikhelmItem extends Item {   // 26.1: the helmet part comes from Item.Properties#humanoidArmor (OnderwaterFeature)
    /** Water breathing lasts this long after your last check under water; night vision a bit longer (so it never flickers). */
    public static final int BREATH_TICKS = 200, SIGHT_TICKS = 260;

    public DuikhelmItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot equipSlot) {
        if (!level.isClientSide() && entity instanceof Player player && entity.tickCount % 20 == 0) {
            divingTick(player, stack);
        }
    }

    /** Worn (on the head, this very stack) with your eyes under water: water breathing and night vision. */
    public static boolean divingTick(Player player, ItemStack stack) {
        if (player.getItemBySlot(EquipmentSlot.HEAD) != stack || !OnderwaterFeature.underWater(player)) {
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, BREATH_TICKS, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, SIGHT_TICKS, 0, true, false, true));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.duikhelm.lore").withStyle(ChatFormatting.GRAY));
    }
}
