package nl.juiced.guhs.feature.guhpixel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.storage.Nbt;

/**
 * The safe of a minigame: {@link #bewaar} takes the player's whole inventory (hotbar, main, armour, offhand, each with its
 * slot number; the stack on the cursor and what lies in the 2 x 2 crafting grid; the selected hotbar slot) and leaves the
 * inventory EMPTY for the game items; {@link #herstel} first throws every game item away and then puts everything back
 * exactly where it was.
 * <p>
 * The snapshot lives in {@code GuhQuests.saved(p)["guhs_guhpixel_kluis"]}: in the player's own file, the same file as the
 * inventory, so one write holds both and a crash can never leave "game items but no snapshot". It survives death too
 * (PlayerPersisted). A snapshot found at login without a running game is restored at once ({@link Sessies}).
 */
public final class Kluis {
    public static final String SLEUTEL = "guhs_guhpixel_kluis";

    public static boolean heeft(ServerPlayer p) {
        return GuhQuests.saved(p).getCompound(SLEUTEL).isPresent();
    }

    /** Stores the inventory and empties it. False (and nothing changes) when a snapshot already exists. */
    public static boolean bewaar(ServerPlayer p) {
        if (heeft(p)) {
            return false;
        }
        CompoundTag kluis = new CompoundTag();
        ListTag extra = new ListTag();
        // the cursor and the crafting grid first (closing the screen would push them into the inventory or onto the floor)
        ItemStack cursor = p.containerMenu.getCarried();
        if (!cursor.isEmpty()) {
            extra.add(Nbt.saveStack(p.registryAccess(), cursor));
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
        CraftingContainer raster = p.inventoryMenu.getCraftSlots();
        for (int i = 0; i < raster.getContainerSize(); i++) {
            ItemStack s = raster.removeItemNoUpdate(i);
            if (!s.isEmpty()) {
                extra.add(Nbt.saveStack(p.registryAccess(), s));
            }
        }
        p.closeContainer();
        Inventory inv = p.getInventory();
        ListTag slots = new ListTag();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag e = new CompoundTag();
                e.putInt("Slot", slot);
                e.put("Item", Nbt.saveStack(p.registryAccess(), stack));
                slots.add(e);
            }
        }
        kluis.put("Slots", slots);
        kluis.put("Extra", extra);
        kluis.putInt("Gekozen", inv.getSelectedSlot());
        kluis.putLong("Sinds", Klok.nu());
        GuhQuests.saved(p).put(SLEUTEL, kluis);
        inv.clearContent();
        p.inventoryMenu.broadcastChanges();
        return true;
    }

    /** Throws the game items away and gives the stored inventory back. False when there is no snapshot. */
    public static boolean herstel(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag kluis = saved.getCompound(SLEUTEL).orElse(null);
        if (kluis == null) {
            return false;
        }
        p.containerMenu.setCarried(ItemStack.EMPTY);
        CraftingContainer raster = p.inventoryMenu.getCraftSlots();
        for (int i = 0; i < raster.getContainerSize(); i++) {
            raster.removeItemNoUpdate(i);
        }
        p.closeContainer();
        Inventory inv = p.getInventory();
        inv.clearContent();
        ListTag slots = kluis.getListOrEmpty("Slots");
        for (int i = 0; i < slots.size(); i++) {
            CompoundTag e = slots.getCompoundOrEmpty(i);
            int slot = e.getIntOr("Slot", -1);
            ItemStack stack = Nbt.parseStack(p.registryAccess(), e.getCompoundOrEmpty("Item"));
            if (slot >= 0 && slot < inv.getContainerSize() && !stack.isEmpty()) {
                inv.setItem(slot, stack);
            }
        }
        int gekozen = kluis.getIntOr("Gekozen", 0);
        if (gekozen >= 0 && gekozen < Inventory.SELECTION_SIZE) {
            inv.setSelectedSlot(gekozen);
            if (p.connection != null) {
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket(gekozen));
            }
        }
        // the snapshot goes first: whatever happens next, nothing can be given twice
        saved.remove(SLEUTEL);
        ListTag extra = kluis.getListOrEmpty("Extra");
        for (int i = 0; i < extra.size(); i++) {
            ItemStack stack = Nbt.parseStack(p.registryAccess(), extra.getCompoundOrEmpty(i));
            if (!stack.isEmpty()) {
                Minigames.give(p, stack);   // (into the pockets; when they are full it lies at the player's feet, theirs only)
            }
        }
        p.inventoryMenu.broadcastChanges();
        return true;
    }

    /** (Dev) what the snapshot holds, as a short text. */
    public static String toon(ServerPlayer p) {
        CompoundTag kluis = GuhQuests.saved(p).getCompound(SLEUTEL).orElse(null);
        if (kluis == null) {
            return "geen kluis";
        }
        return kluis.getListOrEmpty("Slots").size() + " slots, " + kluis.getListOrEmpty("Extra").size() + " extra, gekozen "
                + kluis.getIntOr("Gekozen", 0);
    }

    private Kluis() {
    }
}
