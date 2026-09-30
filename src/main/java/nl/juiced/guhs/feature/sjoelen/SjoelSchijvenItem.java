package nl.juiced.guhs.feature.sjoelen;

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
 * Opoe Njegschuif's sjoelschijven, lent for one turn (the stack shows how many you have left). Stand at the head of the
 * sjoelbak, hold right-click: the power bar goes up and down (Opoe's two little marks show the good part), look where
 * the puck should go and let go to slide it. It can't be dropped or stored and it is gone as soon as you stop playing.
 */
public class SjoelSchijvenItem extends Item {
    /** Ticks for the power bar to go from 0 up to 100% and back down again. */
    public static final int CYCLE = 50;
    /** Opoe's marks on the power bar: between these, a straight puck ends up behind the gates. */
    public static final float GOED_VAN = 0.60f, GOED_TOT = 0.92f;

    public SjoelSchijvenItem(Properties properties) {
        super(properties);
    }

    /** The power (0..1) after holding for this many ticks: up and down, up and down. */
    public static float power(int ticks) {
        int t = Math.floorMod(ticks, CYCLE);
        float half = CYCLE / 2f;
        float p = t <= half ? t / half : (CYCLE - t) / half;
        return Math.max(0.02f, Math.min(1f, p));
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
            SjoelGame.slide(player, power(getUseDuration(stack, entity) - timeLeft));
            return true;
        }
        return false;
    }

    /** Only players at the sjoelbak keep their pucks. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot equipSlot) {
        if (!level.isClientSide() && (!(entity instanceof Player player) || !SjoelGame.isPlaying(player))) {
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
        tooltip.accept(Component.translatable("item.guhs.sjoelen_schijven.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.sjoelen_schijven.loan").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
