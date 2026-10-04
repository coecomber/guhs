package nl.juiced.guhs.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhSleeEntity;
import nl.juiced.guhs.registry.ModItems;

/**
 * The guh kermis coaster: every lap (over the finish line) gives everyone riding along a kermisbon; the very first
 * lap also gives a prize bag (the "first ride" advancement comes with the first kermisbon). Kermisbonnen buy the kermis outfit from the Kermis-guh.
 */
public final class KermisRides {
    private static final String FIRST = "guhs_kermis_first_lap";

    public static void lap(ServerPlayer player, GuhSleeEntity sled) {
        give(player, new ItemStack(ModItems.KERMISBON.get()));
        CompoundTag data = GuhQuests.saved(player);
        if (!data.getBooleanOr(FIRST, false) && freeSlots(player) < 3) {
            // 1.2.7: the one-time prize bag waits until it fits (it used to fall along the track, for anyone to take)
            player.sendSystemMessage(Component.translatable("quest.guhs.kermis.first_full").withStyle(ChatFormatting.GOLD));
            player.level().playSound(null, sled.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.9f, 1.5f);
        } else if (!data.getBooleanOr(FIRST, false)) {
            data.putBoolean(FIRST, true);
            give(player, new ItemStack(ModItems.GUH_BALLON.get(), 3));
            give(player, new ItemStack(ModItems.KAASHONING.get(), 4));
            give(player, new ItemStack(ModItems.GUH_KRISTAL.get(), 8));
            give(player, new ItemStack(ModItems.KERMISBON.get(), 2));
            player.sendSystemMessage(Component.translatable("quest.guhs.kermis.first").withStyle(ChatFormatting.GOLD));
            player.level().playSound(null, sled.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
        } else {
            player.level().playSound(null, sled.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.9f, 1.5f);
        }
        player.sendOverlayMessage(Component.translatable("quest.guhs.kermis.lap").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Into the pockets; what doesn't fit lies in front of the player, for them alone (1.2.7: was a loose drop). */
    private static void give(ServerPlayer player, ItemStack stack) {
        nl.juiced.guhs.feature.Minigames.give(player, stack);
    }

    static int freeSlots(ServerPlayer player) {
        int free = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty()) {
                free++;
            }
        }
        return free;
    }

    private KermisRides() {
    }
}
