package nl.juiced.guhs.feature.katapult;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Kapitein Floepguh's pluisballen, lent for one fort at a time (the stack shows how many you have left). Stand at the
 * Knabbelkatapult, look where the ball should fly, hold right-click to pull the elastic back (the longer, the harder, up
 * to 100%) and let go: FLOEP! On makkelijk the dotted aiming line shows where it will go.
 */
public class PluisballenItem extends Item {
    /** Ticks of pulling from 0 to full power. */
    public static final int CYCLE = 30;
    /** Custom data flag: this player may see the aiming line (makkelijk). */
    public static final String RICHTLIJN = "Richtlijn";

    public PluisballenItem(Properties properties) {
        super(properties);
    }

    /** The power (0..1) after pulling this many ticks: up to full, and it stays full. */
    public static float power(int ticks) {
        return Math.max(0.05f, Math.min(1f, ticks / (float) CYCLE));
    }

    public static boolean richtlijn(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(RICHTLIJN);
    }

    public static ItemStack stack(int count, boolean richtlijn) {
        ItemStack stack = new ItemStack(KatapultFeature.PLUISBALLEN.get(), Math.max(1, count));
        if (richtlijn) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean(RICHTLIJN, true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            KatapultGame.fire(player, power(getUseDuration(stack, entity) - timeLeft));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && (!(entity instanceof Player player) || !KatapultGame.isPlaying(player))) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide) {
            entity.discard();
        }
        return true;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.katapult_pluisballen.lore").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.guhs.katapult_pluisballen.loan").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
