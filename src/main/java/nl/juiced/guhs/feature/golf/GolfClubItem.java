package nl.juiced.guhs.feature.golf;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
/**
 * The guh golf club, lent by the Golfguh for one round. Hold right-click (near your ball) to charge the swing: the power
 * bar fills up to 100% (and stays there), the ball goes the way you look, and letting go hits it. It can't be dropped or
 * stored and it disappears as soon as you're not playing any more.
 */
public class GolfClubItem extends Item {
    /** Ticks of holding from 0 to full power. */
    public static final int CYCLE = 26;

    public GolfClubItem(Properties properties) {
        super(properties);
    }

    /** The power (0..1) after holding for this many ticks: it charges up to full and stays full, never quite 0. */
    public static float power(int ticks) {
        return Math.max(0.03f, Math.min(1f, ticks / (float) CYCLE));
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
        return ItemUseAnimation.SPEAR;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            GolfGame.swing(player, power(getUseDuration(stack, entity) - timeLeft));
        }
        return false;
    }

    /** Only golfers keep their club. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot equipSlot) {
        if (!(entity instanceof Player player) || !GolfGame.isGolfing(player)) {
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
        tooltip.accept(Component.translatable("item.guhs.guhgolfclub.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.guhgolfclub.loan").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
