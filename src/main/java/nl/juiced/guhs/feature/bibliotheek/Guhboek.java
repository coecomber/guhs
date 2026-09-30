package nl.juiced.guhs.feature.bibliotheek;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * The guh lore books of the guh library: vanilla written books whose pages are translated (book.guhs.bieb.&lt;id&gt;.&lt;page&gt;,
 * written by tools/features/bibliotheek.py, which checks that this list matches its own). A book knows which one it is by
 * its custom data (GuhsBoek). Every book has two quiz questions (gui.guhs.bieb.kwis.&lt;id&gt;.&lt;n&gt;), the first answer is right.
 * The last one, the secret book, is only found on the book stand in the secret room.
 */
public enum Guhboek {
    EERSTE_GUH("eerste_guh", 4, 2, 0xF08CB4),
    MOEDER_VADSIG("moeder_vadsig", 5, 2, 0xD65A8C),
    KAASKNABBELS("kaasknabbels", 4, 2, 0xFACD46),
    GUHMENSIE("guhmensie", 4, 2, 0xEE8DAD),
    MIKA_OORLOG("mika_oorlog", 4, 2, 0x462D46),
    KONINGSKROON("koningskroon", 4, 2, 0x7828A0),
    VAHOEG("vahoeg", 4, 2, 0xFF78BE),
    ROZE_MAAN("roze_maan", 4, 2, 0xBE96E6),
    GOUDEN_GUH("gouden_guh", 4, 2, 0xF5C43C),
    GUHMAAG("guhmaag", 4, 2, 0xE66E78),
    SLEE_KERMIS("slee_kermis", 4, 2, 0x6EBEE6),
    GEHEIM("geheim", 5, 2, 0x281E3C),
    /** (after GEHEIM, so the book bits in old saves stay the same) Found in the Knabbelkelder and given by the Koningguh. */
    GUHEINDE("guheinde", 5, 2, 0xE8B83C),
    // --- 2.7 ---
    /** Found in the Mika-voorraadschuur of the Stille Voorraadkelder (tools/features/gatenkaas.py), not in the library. */
    VOORRAADKELDER("voorraadkelder", 5, 2, 0x8A5A2B);

    public static final String TAG = "GuhsBoek";
    /** Quiz questions per book, and answers per question. */
    public static final int QUESTIONS = 2, ANSWERS = 3;

    public final String id;
    public final int pages;
    public final int questions;
    /** Colour of the book's spine in the Bibliothecaris' screen. */
    public final int colour;

    Guhboek(String id, int pages, int questions, int colour) {
        this.id = id;
        this.pages = pages;
        this.questions = questions;
        this.colour = colour;
    }

    public boolean secret() {
        return this == GEHEIM;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public Component title() {
        return Component.translatable("book.guhs.bieb." + id + ".title");
    }

    /** A fresh copy of this book: a written book by "De Bibliothecaris" with the translated pages. */
    public ItemStack stack() {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> list = new ArrayList<>();
        for (int i = 0; i < pages; i++) {
            list.add(Filterable.passThrough(Component.translatable("book.guhs.bieb." + id + "." + i)));
        }
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Guhboek " + (ordinal() + 1)),
                "De Bibliothecaris", 0, list, true));
        stack.set(DataComponents.CUSTOM_NAME, title().copy().withStyle(Style.EMPTY.withItalic(false)
                .withColor(secret() ? TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE) : TextColor.fromRgb(0xF7B6CB))));
        stack.set(DataComponents.LORE, new ItemLore(List.of(secret()
                ? Component.translatable("item.guhs.bieb_boek.lore_secret").withStyle(s -> s.withItalic(false).withColor(ChatFormatting.DARK_PURPLE))
                : Component.translatable("item.guhs.bieb_boek.lore", ordinal() + 1, values().length)
                        .withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GRAY)))));
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG, id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, secret());   // (written books glitter: only the secret one does here)
        return stack;
    }

    /** Which guh book this stack is (a written book with our custom data), or null. */
    @Nullable
    public static Guhboek of(ItemStack stack) {
        if (!stack.is(Items.WRITTEN_BOOK)) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        String id = data.copyTag().getStringOr(TAG, "");
        return byId(id);
    }

    @Nullable
    public static Guhboek byId(String id) {
        for (Guhboek book : values()) {
            if (book.id.equals(id.toLowerCase(Locale.ROOT))) {
                return book;
            }
        }
        return null;
    }

    /** All quiz questions: book * QUESTIONS + n. */
    public static int questionCount() {
        return values().length * QUESTIONS;
    }

    public static Guhboek bookOfQuestion(int question) {
        return values()[question / QUESTIONS];
    }

    public static String questionKey(int question) {
        return "gui.guhs.bieb.kwis." + bookOfQuestion(question).id + "." + (question % QUESTIONS);
    }

    public static String answerKey(int question, int answer) {
        return questionKey(question) + "." + answer;
    }
}
