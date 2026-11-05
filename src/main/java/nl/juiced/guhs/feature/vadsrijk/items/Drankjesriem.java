package nl.juiced.guhs.feature.vadsrijk.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.storage.Nbt;

/**
 * T-ITEMS: the drankjesriem (R4): 3 quick slots for potions, no stats. Drink with the keys Y / I / O (client), shown left of the
 * hotbar. Kept in the player data ({@code SpelerData.extra("t_items")}, key {@code riem}: like the jewellery it never touches
 * the live inventory, so the inventory swap can't lose it; per character once t-karakter re-keys SpelerData). Only drinkable
 * items fit (a {@code consumable} with the drink animation: every Vadsrijk potion). Drinking from the belt is instant but uses
 * the same shared potion cooldown as the hotbar ({@code use_cooldown} component). Only in the Vadsrijk.
 * <p>Putting one in: click a belt slot with a potion on the cursor, or shift-click a potion in the inventory. Taking one out:
 * click the slot with an empty cursor.
 */
public final class Drankjesriem {
    public static final int VAKKEN = 3;
    static final String SLICE = "t_items";

    private Drankjesriem() {
    }

    private static CompoundTag riem(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        return d.extra(SLICE).getCompound("riem").orElseGet(() -> {
            CompoundTag t = new CompoundTag();
            d.extra(SLICE).put("riem", t);
            return t;
        });
    }

    public static ItemStack get(ServerPlayer p, int i) {
        return riem(p).getCompound("v" + i).map(t -> Nbt.parseStack(p.registryAccess(), t)).orElse(ItemStack.EMPTY);
    }

    public static void zet(ServerPlayer p, int i, ItemStack s) {
        CompoundTag r = riem(p);
        if (s.isEmpty()) {
            r.remove("v" + i);
        } else {
            r.put("v" + i, Nbt.saveStack(p.registryAccess(), s));
        }
    }

    public static List<ItemStack> alle(ServerPlayer p) {
        List<ItemStack> l = new ArrayList<>();
        for (int i = 0; i < VAKKEN; i++) {
            l.add(get(p, i));
        }
        return l;
    }

    /** Fits in the belt: something you drink. */
    public static boolean isDrankje(ItemStack s) {
        Consumable c = s.isEmpty() ? null : s.get(DataComponents.CONSUMABLE);
        return c != null && c.animation() == ItemUseAnimation.DRINK;
    }

    /** Puts (as much as fits of) {@code s} into the belt: first onto the same potion, then into an empty slot. Returns the rest. */
    public static ItemStack leg(ServerPlayer p, ItemStack s) {
        if (!isDrankje(s)) {
            return s;
        }
        ItemStack rest = s.copy();
        for (int i = 0; i < VAKKEN && !rest.isEmpty(); i++) {
            ItemStack v = get(p, i);
            if (!v.isEmpty() && ItemStack.isSameItemSameComponents(v, rest) && v.getCount() < v.getMaxStackSize()) {
                int n = Math.min(rest.getCount(), v.getMaxStackSize() - v.getCount());
                v.grow(n);
                rest.shrink(n);
                zet(p, i, v);
            }
        }
        for (int i = 0; i < VAKKEN && !rest.isEmpty(); i++) {
            if (get(p, i).isEmpty()) {
                zet(p, i, rest.copy());
                rest = ItemStack.EMPTY;
            }
        }
        return rest;
    }

    /** Shift-click in the inventory (0..35): that potion stack into the belt. */
    public static boolean legUitInventaris(ServerPlayer p, int index) {
        Inventory inv = p.getInventory();
        if (index < 0 || index >= Inventory.INVENTORY_SIZE || !isDrankje(inv.getItem(index))) {
            return false;
        }
        ItemStack voor = inv.getItem(index);
        ItemStack rest = leg(p, voor);
        if (rest.getCount() == voor.getCount()) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.items.riem.vol").withStyle(ChatFormatting.GRAY));
            return false;
        }
        inv.setItem(index, rest);
        inv.setChanged();
        stuur(p);
        return true;
    }

    /** A click on belt slot {@code i}: with a potion on the cursor it goes in (swapping a different potion), else it comes out. */
    public static boolean klik(ServerPlayer p, int i) {
        if (i < 0 || i >= VAKKEN) {
            return false;
        }
        ItemStack hand = p.containerMenu.getCarried();
        ItemStack v = get(p, i);
        if (!hand.isEmpty()) {
            if (!isDrankje(hand)) {
                return false;
            }
            if (!v.isEmpty() && ItemStack.isSameItemSameComponents(v, hand)) {
                int n = Math.min(hand.getCount(), v.getMaxStackSize() - v.getCount());
                v.grow(n);
                hand.shrink(n);
                zet(p, i, v);
                p.containerMenu.setCarried(hand.isEmpty() ? ItemStack.EMPTY : hand);
            } else {
                zet(p, i, hand.copy());
                p.containerMenu.setCarried(v);
            }
        } else if (!v.isEmpty()) {
            ItemStack k = v.copy();
            p.getInventory().add(k);
            zet(p, i, k);
            if (k.getCount() == v.getCount()) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.kern.sieraad.vol").withStyle(ChatFormatting.GRAY));
            }
        } else {
            return false;
        }
        p.containerMenu.broadcastChanges();
        stuur(p);
        return true;
    }

    /** Drink from slot {@code i} (the keys): instant, the shared potion cooldown counts. */
    public static boolean drink(ServerPlayer p, int i) {
        if (!Vadsrijk.in(p) || p.isSpectator()) {
            return false;
        }
        return drinkZonderCheck(p, i);
    }

    static boolean drinkZonderCheck(ServerPlayer p, int i) {
        if (i < 0 || i >= VAKKEN) {
            return false;
        }
        ItemStack v = get(p, i);
        if (v.isEmpty()) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.items.riem.leeg", i + 1).withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (p.getCooldowns().isOnCooldown(v)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.items.riem.wacht").withStyle(ChatFormatting.GRAY));
            return false;
        }
        int voor = v.getCount();
        ItemStack rest = v.finishUsingItem(p.level(), p);
        if (rest != v && !rest.isEmpty()) {   // something left over (a bottle): to the inventory
            if (!p.getInventory().add(rest)) {
                p.drop(rest, false);
            }
        }
        if (v.getCount() == voor && !p.hasInfiniteMaterials()) {
            v.shrink(1);   // (an item that didn't use itself up)
        }
        zet(p, i, v.isEmpty() ? ItemStack.EMPTY : v);
        stuur(p);
        return true;
    }

    public static void stuur(ServerPlayer p) {
        ItemsPayloads.stuurRiem(p, Vadsrijk.in(p) ? alle(p) : List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
    }
}
