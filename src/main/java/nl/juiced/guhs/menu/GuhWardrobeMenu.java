package nl.juiced.guhs.menu;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.item.GuhClothingItem;
import nl.juiced.guhs.registry.ModMenuTypes;

/**
 * The guh's wardrobe (2.9). Clothes are unlocks now, so they are not item slots any more: the screen
 * ({@code client.screen.GuhWardrobeScreen}) picks them from your unlocks and sends them with the payload
 * {@code guhs:kleding_kleed} (feature.kleding.KledingKast). What is left as a real container: the armour slot, the 18
 * backpack slots (only usable while the guh wears the backpack; they belong to that guh) and your own inventory. The
 * screen shows these on its second tab ("Rugzak &amp; harnas"): on the clothes tab the slots are switched off
 * ({@link #setToonVakjes}, client side only).
 * <p>
 * Slot indices: 0 armour, 1-18 backpack, 19-45 inventory, 46-54 hotbar.
 */
public class GuhWardrobeMenu extends AbstractContainerMenu {
    /** The screen is this big; the slot area (176 wide, like a chest) sits in its middle. */
    public static final int WIDTH = 332, HEIGHT = 250;
    public static final int OFFSET_X = (WIDTH - 176) / 2;
    public static final int ARMOR_X = OFFSET_X + 152, ARMOR_Y = 46;
    public static final int PACK_X = OFFSET_X + 8, PACK_Y = 98, PACK_COLS = 9, PACK_ROWS = 2;
    public static final int INV_X = OFFSET_X + 8, INV_Y = 152;
    public static final int ARMOR_SLOT = 0, PACK_START = 1, INV_START = PACK_START + GuhEntity.BACKPACK_SIZE,
            HOTBAR_START = INV_START + 27, INV_END = HOTBAR_START + 9;

    /** The index of a clothing slot in the wardrobe (its place in GuhClothes.Slot.kleding(); -1 for HAAR). */
    public static int index(GuhClothes.Slot part) {
        return GuhClothes.Slot.kleding().indexOf(part);
    }

    private final int guhId;
    @Nullable
    private final GuhEntity guh;
    private final Player player;
    private final Container pack;
    /** Client side: whether the screen shows the slots (the "Rugzak & harnas" tab). The server never hides them. */
    private boolean toonVakjes = true;

    /** Server side. */
    public GuhWardrobeMenu(int containerId, Inventory inventory, GuhEntity guh) {
        this(containerId, inventory, guh.getId(), guh, guh.getBackpack(), new ArmorContainer(guh));
    }

    /** Client side: the server sends the guh's entity id. */
    public GuhWardrobeMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readVarInt(), null, new SimpleContainer(GuhEntity.BACKPACK_SIZE), new SimpleContainer(1));
    }

    private GuhWardrobeMenu(int containerId, Inventory inventory, int guhId, @Nullable GuhEntity guh, Container pack, Container armor) {
        super(ModMenuTypes.GUH_WARDROBE.get(), containerId);
        this.guhId = guhId;
        this.guh = guh;
        this.player = inventory.player;
        this.pack = pack;
        addSlot(new Slot(armor, 0, ARMOR_X, ARMOR_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof nl.juiced.guhs.item.GuhArmorItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return toonVakjes;
            }
        });
        for (int row = 0; row < PACK_ROWS; row++) {
            for (int col = 0; col < PACK_COLS; col++) {
                addSlot(new Slot(pack, col + row * PACK_COLS, PACK_X + col * 18, PACK_Y + row * 18) {
                    @Override
                    public boolean isActive() {
                        return toonVakjes && hasBackpack();
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return hasBackpack() && !(stack.getItem() instanceof GuhClothingItem c && c.getClothes() == GuhClothes.GUH_BACKPACK)
                                && !nl.juiced.guhs.feature.Features.isLoaned(stack);     // (loaned things go back, they aren't yours to pack)
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18) {
                    @Override
                    public boolean isActive() {
                        return toonVakjes;
                    }
                });
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INV_X + col * 18, INV_Y + 58) {
                @Override
                public boolean isActive() {
                    return toonVakjes;
                }
            });
        }
    }

    public int getGuhId() {
        return guhId;
    }

    /** The guh (server: the real one; client: looked up by its id). */
    @Nullable
    public GuhEntity guh() {
        if (guh != null) {
            return guh;
        }
        return player.level().getEntity(guhId) instanceof GuhEntity g ? g : null;
    }

    /** (Client) show or hide the slots: the screen's clothes tab hides them. */
    public void setToonVakjes(boolean toon) {
        this.toonVakjes = toon;
    }

    public boolean toonVakjes() {
        return toonVakjes;
    }

    /** Whether the guh wears the backpack (both sides: the client reads the synced entity data). */
    public boolean hasBackpack() {
        GuhEntity g = guh();
        return g != null && g.hasBackpack();
    }

    /** Whether the backpack's 18 slots are empty (both sides: the client reads the synced slots). */
    public boolean backpackIsEmpty() {
        return pack.isEmpty();
    }

    /** The guh's armour (its body armour slot) as a one-slot container. */
    static class ArmorContainer extends SimpleContainer {
        private final GuhEntity guh;

        ArmorContainer(GuhEntity guh) {
            super(1);
            this.guh = guh;
        }

        @Override
        public ItemStack getItem(int index) {
            return guh.getBodyArmorItem();
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack armor = guh.getBodyArmorItem().copy();
            guh.setBodyArmorItem(ItemStack.EMPTY);
            return armor;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return removeItem(index, 1);
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            guh.setBodyArmorItem(stack.copyWithCount(Math.min(1, stack.getCount())));
        }

        @Override
        public boolean isEmpty() {
            return guh.getBodyArmorItem().isEmpty();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < INV_START) {
            // armour / backpack -> inventory
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof nl.juiced.guhs.item.GuhArmorItem && !this.slots.get(ARMOR_SLOT).hasItem()) {
            if (!moveItemStackTo(stack, ARMOR_SLOT, ARMOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!hasBackpack() || !this.slots.get(PACK_START).mayPlace(stack) || !moveItemStackTo(stack, PACK_START, INV_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return guh == null || (guh.isAlive() && guh.isOwnedBy(player) && player.distanceToSqr(guh) < 12 * 12);
    }
}
