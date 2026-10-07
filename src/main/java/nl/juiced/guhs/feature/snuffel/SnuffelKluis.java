package nl.juiced.guhs.feature.snuffel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.storage.Nbt;

/**
 * The safe that holds a player's own things while they are a dog on Het Snuffeleiland (the method of Guhpixel's
 * {@code Kluis}, with its own keys so the two never touch): {@link #bewaar} takes the whole inventory (hotbar, main, armour,
 * offhand, each with its slot number; the stack on the cursor and what lies in the 2 x 2 crafting grid; the selected
 * hotbar slot) and leaves the inventory EMPTY; {@link #herstel} puts everything back exactly where it was.
 * <p>
 * The snapshot lives in {@code GuhQuests.saved(p)["guhs_snuffel_kluis"]}: in the player's own file, the same file as the
 * inventory, so one write holds both and a crash can never leave "a dog's pockets but no snapshot". It survives death
 * (PlayerPersisted). Nothing is ever dropped: what does not fit when it comes back, and what the island GIVES a dog to take
 * home ({@link #post}), waits in {@code guhs_snuffel_post} until there is a free slot ({@link #geefPost}).
 */
public final class SnuffelKluis {
    public static final String SLEUTEL = "guhs_snuffel_kluis";
    /** Stacks that wait for a free slot at home: gifts of the island, and what did not fit at {@link #herstel}. */
    public static final String POST = "guhs_snuffel_post";

    private SnuffelKluis() {
    }

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
        GuhQuests.saved(p).put(SLEUTEL, kluis);
        inv.clearContent();
        p.inventoryMenu.broadcastChanges();
        return true;
    }

    /**
     * Gives the stored inventory back, every stack in its own slot. Whatever the player carries NOW (a dog's pockets: only
     * the memory card, which is simply dropped from existence) is not thrown away but goes to the {@link #post}. False when
     * there is no snapshot.
     */
    public static boolean herstel(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag kluis = saved.getCompound(SLEUTEL).orElse(null);
        if (kluis == null) {
            return false;
        }
        ItemStack cursor = p.containerMenu.getCarried();
        if (!cursor.isEmpty()) {
            post(p, cursor.copy());
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
        CraftingContainer raster = p.inventoryMenu.getCraftSlots();
        for (int i = 0; i < raster.getContainerSize(); i++) {
            post(p, raster.removeItemNoUpdate(i));
        }
        p.closeContainer();
        Inventory inv = p.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            post(p, inv.getItem(slot).copy());
        }
        inv.clearContent();
        ListTag slots = kluis.getListOrEmpty("Slots");
        for (int i = 0; i < slots.size(); i++) {
            CompoundTag e = slots.getCompoundOrEmpty(i);
            int slot = e.getIntOr("Slot", -1);
            ItemStack stack = Nbt.parseStack(p.registryAccess(), e.getCompoundOrEmpty("Item"));
            if (stack.isEmpty()) {
                continue;
            }
            if (slot >= 0 && slot < inv.getContainerSize()) {
                inv.setItem(slot, stack);
            } else {
                post(p, stack);
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
            post(p, Nbt.parseStack(p.registryAccess(), extra.getCompoundOrEmpty(i)));
        }
        p.inventoryMenu.broadcastChanges();
        return true;
    }

    /** Something to take home: it waits until the player is no dog and has a free slot. Never dropped, never lost. */
    public static void post(ServerPlayer p, ItemStack stack) {
        if (stack.isEmpty() || stack.is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get())) {
            return;
        }
        CompoundTag saved = GuhQuests.saved(p);
        ListTag post = saved.getListOrEmpty(POST).copy();
        post.add(Nbt.saveStack(p.registryAccess(), stack));
        saved.put(POST, post);
    }

    public static boolean heeftPost(ServerPlayer p) {
        return GuhQuests.saved(p).contains(POST);
    }

    /** How many stacks wait. */
    public static int postAantal(ServerPlayer p) {
        return GuhQuests.saved(p).getListOrEmpty(POST).size();
    }

    /**
     * Hands over what waits as far as it fits (called at once when the inventory came back and about once a second after
     * that). Does nothing for a dog (the snapshot still holds the real inventory) or a dead player. Returns true when
     * something was handed over.
     */
    public static boolean geefPost(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        if (!saved.contains(POST) || heeft(p) || !p.isAlive()) {
            return false;
        }
        ListTag post = saved.getListOrEmpty(POST);
        ListTag over = new ListTag();
        boolean iets = false;
        for (int i = 0; i < post.size(); i++) {
            ItemStack stack = Nbt.parseStack(p.registryAccess(), post.getCompoundOrEmpty(i));
            if (stack.isEmpty()) {
                continue;
            }
            int voor = stack.getCount();
            p.getInventory().add(stack);
            iets |= stack.getCount() != voor;
            if (!stack.isEmpty()) {
                over.add(Nbt.saveStack(p.registryAccess(), stack));
            }
        }
        if (over.isEmpty()) {
            saved.remove(POST);
        } else {
            saved.put(POST, over);
        }
        if (iets) {
            p.inventoryMenu.broadcastChanges();
        }
        return iets;
    }
}
