package nl.juiced.guhs.feature.bleekwoud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WrittenBookContent;
import nl.juiced.guhs.world.VoorIedereen;

/**
 * The diary of the Houthakkerguh, on the lectern of het Houthakkershutje: a written book whose pages are translated
 * (book.guhs.bleekwoud.dagboek.&lt;n&gt;, so every player reads it in their own language). The book on the lectern is for
 * whoever takes it first; everybody else gets their own copy by clicking the lectern (1.2.7's rule: nothing in a
 * structure is for the first player only).
 */
public final class Dagboek {
    /** Pages (tools/features/bleekwoud.py checks that it writes this many). */
    public static final int BLADZIJDEN = 7;
    public static final String TAG = "GuhsBleekwoudDagboek";

    /** A fresh copy of the diary. */
    public static ItemStack stack() {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int i = 0; i < BLADZIJDEN; i++) {
            pages.add(Filterable.passThrough(Component.translatable("book.guhs.bleekwoud.dagboek." + i)));
        }
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Houthakkersdagboek"), "", 0, pages, true));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("book.guhs.bleekwoud.dagboek.title")
                .withStyle(Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(0xD8D2CC))));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("book.guhs.bleekwoud.dagboek.door")
                .withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GRAY)))));
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        return stack;
    }

    public static boolean is(ItemStack stack) {
        if (!stack.is(Items.WRITTEN_BOOK)) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(TAG, false);
    }

    public static boolean heeft(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (is(stack)) {
                return true;
            }
        }
        return is(player.getOffhandItem());
    }

    /** A copy for this player (not when they carry one already). Every player, every time: nobody can miss it. */
    public static boolean geef(ServerPlayer player) {
        VoorIedereen.shown(player, "guhmension/bleekwoud_dagboek");
        if (heeft(player)) {
            return false;
        }
        player.getInventory().placeItemBackInInventory(stack());
        player.sendSystemMessage(Component.translatable("gui.guhs.bleekwoud.dagboek_kopie").withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        return true;
    }

    private Dagboek() {
    }
}
