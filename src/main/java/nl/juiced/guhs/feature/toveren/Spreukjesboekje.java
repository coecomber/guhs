package nl.juiced.guhs.feature.toveren;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import nl.juiced.guhs.Guhs;

/**
 * Het Spreukjesboekje: the guh enchanted book.
 * <p>
 * <b>Decision:</b> it IS a vanilla {@code minecraft:enchanted_book}, dressed up through two item components: its own
 * name ({@code item_name} = "Spreukjesboekje") and its own model ({@code item_model} = {@code guhs:spreukjesboekje}, a
 * fluffy pink book with guh ears). Everything that knows enchanted books keeps working without one line of glue: the
 * anvil (vanilla only reads stored enchantments from the enchanted_book item itself; an own item would need its own
 * anvil logic), the grindstone, book + book merging, the enchantment tooltip and glint, villagers and loot tables.
 * The look travels with the stack (an anvil copies the left book, components and all).
 * <p>
 * The altar dresses a book when it gets a guh enchantment ({@link #kleedAan}). The villager trades and the loot of the
 * cave structures make one with {@link #maak}, or in data with a {@code minecraft:set_components} loot function:
 * <pre>{"function": "minecraft:set_components", "components": {"minecraft:item_model": "guhs:spreukjesboekje",
 *   "minecraft:item_name": {"translate": "item.guhs.spreukjesboekje"}}}</pre>
 * after a {@code minecraft:enchant_randomly} with {@code "options": "#guhs:spreukjes/boekje"} (the book-only spells) or
 * {@code "#guhs:spreukjes/alle"} (every guh spell).
 */
public final class Spreukjesboekje {
    public static final String NAAM = "item.guhs.spreukjesboekje";

    private Spreukjesboekje() {
    }

    /** A Spreukjesboekje holding this one enchantment. */
    public static ItemStack maak(Holder<Enchantment> enchantment, int level) {
        ItemStack boek = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        m.set(enchantment, level);
        boek.set(DataComponents.STORED_ENCHANTMENTS, m.toImmutable());
        kleed(boek);
        return boek;
    }

    public static ItemStack maak(EnchantmentInstance instance) {
        return maak(instance.enchantment(), instance.level());
    }

    /** Gives an enchanted book the Spreukjesboekje's name and look. */
    public static void kleed(ItemStack boek) {
        boek.set(DataComponents.ITEM_NAME, Component.translatable(NAAM));
        boek.set(DataComponents.ITEM_MODEL, Guhs.id("spreukjesboekje"));
    }

    /** Dresses an enchanted book that holds a guh enchantment (anything else is left alone). */
    public static void kleedAan(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_BOOK) && heeftGuhSpreuk(stack)) {
            kleed(stack);
        }
    }

    public static boolean isSpreukjesboekje(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK) && Guhs.id("spreukjesboekje").equals(stack.get(DataComponents.ITEM_MODEL));
    }

    static boolean heeftGuhSpreuk(ItemStack stack) {
        for (Holder<Enchantment> h : stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).keySet()) {
            if (h.is(Spreuken.ALLE)) {
                return true;
            }
        }
        return false;
    }
}
