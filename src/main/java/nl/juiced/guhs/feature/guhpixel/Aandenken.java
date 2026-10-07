package nl.juiced.guhs.feature.guhpixel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.registry.ModItems;

/**
 * Keepsakes of the joke games: an outfit is an unlock (no item; all the player's guhs can wear it), a decoration is a
 * real item that is never lost ({@link Minigames#give}).
 */
public final class Aandenken {
    /** Unlocks these clothes for the player and tells them; false when they knew all of them. */
    public static boolean kleding(ServerPlayer p, GuhClothes... stukken) {
        boolean nieuw = false;
        for (GuhClothes c : stukken) {
            if (KledingUnlocks.ontgrendel(p, c)) {
                nieuw = true;
                p.sendSystemMessage(Component.translatable("gui.guhs.guhpixel.aandenken.kleding",
                        new ItemStack(ModItems.clothingItem(c)).getHoverName().copy().withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        return nieuw;
    }

    /** Gives a keepsake item (into the pockets, else at the player's feet for them only). */
    public static boolean item(ServerPlayer p, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        p.sendSystemMessage(Component.translatable("gui.guhs.guhpixel.aandenken.item", stack.getHoverName().copy().withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        Minigames.give(p, stack);
        return true;
    }

    /** Gives another one only when the player carries none (the NPC option "kwijt, njeg"). */
    public static boolean opnieuw(ServerPlayer p, Item item) {
        if (p.getInventory().hasAnyMatching(s -> s.is(item))) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.guhpixel.aandenken.heb_je_al").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        Minigames.give(p, new ItemStack(item));
        return true;
    }

    private Aandenken() {
    }
}
