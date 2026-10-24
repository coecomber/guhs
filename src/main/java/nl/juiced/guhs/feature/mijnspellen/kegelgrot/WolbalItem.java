package nl.juiced.guhs.feature.mijnspellen.kegelgrot;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * De Guhde's pink wool ball, lent for one game. Stand on the approach of your lane, look where it should go and hold
 * right-click: the longer, the harder ({@link #kracht}, full after {@link #VOL} ticks); let go to bowl. Standing left or
 * right of the middle gives it a little hook towards the pins; sneaking bowls it straight. It can't be dropped or stored
 * and it is gone as soon as you stop.
 */
public class WolbalItem extends Item {
    /** Ticks of holding for full power. */
    public static final int VOL = 40;

    public WolbalItem(Properties properties) {
        super(properties);
    }

    /** The power (0..1) after holding this long: up to full, then it stays full. */
    public static float kracht(int ticks) {
        return Math.max(0.05f, Math.min(1f, ticks / (float) VOL));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME.heldItemTransformedTo(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            KegelGame.gooi(player, kracht(getUseDuration(stack, entity) - timeLeft));
            return true;
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player) || !KegelGame.isPlaying(player)) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.discard();
        }
        return true;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.kegelgrot_wolbal_leen.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.kegelgrot_wolbal_leen.leen").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
