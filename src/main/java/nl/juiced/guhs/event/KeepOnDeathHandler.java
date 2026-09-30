package nl.juiced.guhs.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.Guhs;

/**
 * Vahoege Vads gear stays with you when you die: just before the death drops, items in the tag
 * {@code guhs:keep_on_death} are taken out of the inventory (remembering their slot) and given back, in the same
 * slot, when you respawn.
 */
public final class KeepOnDeathHandler {
    public static final TagKey<Item> KEEP_ON_DEATH = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Guhs.MODID, "keep_on_death"));
    private static final String KEY = "guhs_kept_items";

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        Inventory inventory = player.getInventory();
        ListTag kept = new ListTag();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(KEEP_ON_DEATH)) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", slot);
                entry.put("Item", stack.save(player.registryAccess()));
                kept.add(entry);
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        if (!kept.isEmpty()) {
            player.getPersistentData().put(KEY, kept);
        }
    }

    public static void onRespawnCopy(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        CompoundTag old = event.getOriginal().getPersistentData();
        if (!old.contains(KEY)) {
            return;
        }
        Player player = event.getEntity();
        ListTag kept = old.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < kept.size(); i++) {
            CompoundTag entry = kept.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(player.registryAccess(), entry.getCompound("Item"));
            int slot = entry.getInt("Slot");
            if (player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().setItem(slot, stack);
            } else {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
        old.remove(KEY);
    }

    private KeepOnDeathHandler() {
    }
}
