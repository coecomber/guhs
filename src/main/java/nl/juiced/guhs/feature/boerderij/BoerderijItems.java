package nl.juiced.guhs.feature.boerderij;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** The farm's items with a line of lore (lang key: the item's own key + ".lore"). */
public final class BoerderijItems {
    /** An item with lore. */
    public static class Lore extends Item {
        public Lore(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** A block item with lore. */
    public static class LoreBlock extends BlockItem {
        public LoreBlock(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Kaasmelk: you drink it (the bottle stays), a creamy moment of vahoegheid (a little saturation and regeneration). */
    public static class Kaasmelk extends Lore {
        public Kaasmelk(Properties properties) {
            super(properties);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public SoundEvent getDrinkingSound() {
            return SoundEvents.GENERIC_DRINK;
        }

        @Override
        public SoundEvent getEatingSound() {
            return SoundEvents.GENERIC_DRINK;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!level.isClientSide) {
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
            }
            return super.finishUsingItem(stack, level, entity);
        }
    }

    private BoerderijItems() {
    }
}
