package nl.juiced.guhs.feature.einde2.audio;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice audio: "De Muziek van het Guheinde", the credits page of the music (CONTRACT_E2_B 6.3) as a written book:
 * every real piece with its maker and licence, and the pieces the guhs made themselves. The pages are translated
 * ({@code gui.guhs.einde2_audio.boek.<n>}, written by tools/features/einde2_audio.py from the licence ledger
 * tools/features/einde2_audio_licenties.json; its self-check holds {@link #PAGINAS} to the number it writes).
 * <p>
 * Every player gets it once, after the finale of the Guhvatar story (the credits roll has just played the music); the dev
 * command {@code /guhs einde2 audio boek} gives a copy any time.
 */
public final class Muziekboekje {
    /** The pages of the book (tools/features/einde2_audio.py checks this number). */
    public static final int PAGINAS = 16;
    /** Per player: the book was given ({@code GuhQuests.saved}). */
    public static final String GEGEVEN = "guhs_einde2_audio_boek";
    public static final String TAG = "GuhsMuziekboekje";
    private static final String T = "gui.guhs.einde2_audio.boek.";

    private Muziekboekje() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Muziekboekje::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % 100 == 0 && !GuhQuests.saved(p).getBooleanOr(GEGEVEN, false)
                && E2Verhaal.finaleKlaar(p)) {
            geefEenmaal(p);
        }
    }

    /** Gives the book once (the flag stays: lost is lost, the dev command makes a new one). Returns whether it gave one. */
    public static boolean geefEenmaal(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        if (saved.getBooleanOr(GEGEVEN, false)) {
            return false;
        }
        saved.putBoolean(GEGEVEN, true);
        Minigames.give(p, stack());
        p.sendSystemMessage(Component.translatable(T + "gekregen").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** A fresh copy of the book. */
    public static ItemStack stack() {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int i = 0; i < PAGINAS; i++) {
            pages.add(Filterable.passThrough(Component.translatable(T + i)));
        }
        // the title of the content is a plain String (one language): the name is the translated custom name instead
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Muziek"), "", 0, pages, true));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(T + "titel").copy()
                .withStyle(Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(0xF7B6CB))));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable(T + "lore")
                .withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GRAY)))));
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        return stack;
    }
}
