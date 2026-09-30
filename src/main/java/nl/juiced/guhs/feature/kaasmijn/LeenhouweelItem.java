package nl.juiced.guhs.feature.kaasmijn;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Mijnguh's loaner pickaxe: an unbreakable iron pickaxe that only works inside a kaasmijn. The moment it is
 * outside one (in your inventory, or lying on the ground) it goes back to the Mijnguh, however it got there.
 * It can't be enchanted.
 */
public class LeenhouweelItem extends PickaxeItem {
    public LeenhouweelItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player && (selected || player.tickCount % 20 == 0)
                && !KaasmijnProtection.inMine(level, player.blockPosition())) {     // (in hand: checked every tick, no mining outside)
            takeBack(player);
        }
    }

    /** Takes every loaner pickaxe out of the player's inventory; returns how many. */
    public static int takeBack(ServerPlayer player) {
        int taken = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof LeenhouweelItem) {
                taken += inventory.getItem(i).getCount();
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        if (player.containerMenu.getCarried().getItem() instanceof LeenhouweelItem) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            taken++;
        }
        if (taken > 0) {
            player.sendSystemMessage(Component.translatable("quest.guhs.kaasmijn.back").withStyle(ChatFormatting.GOLD));
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 0.7f);
        }
        return taken;
    }

    /** How many the player has (anywhere in the inventory, or on the mouse cursor). */
    public static int count(ServerPlayer player) {
        int n = player.containerMenu.getCarried().getItem() instanceof LeenhouweelItem ? 1 : 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof LeenhouweelItem) {
                n += inventory.getItem(i).getCount();
            }
        }
        return n;
    }

    /** Dropped outside a mine (or carried out of it by a hopper, water...): it vanishes back to the Mijnguh. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide && entity.tickCount % 20 == 0 && !KaasmijnProtection.inMine(entity.level(), entity.blockPosition())) {
            entity.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }
}
