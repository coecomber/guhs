package nl.juiced.guhs.menu;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.network.BankContentsPayload;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModMenuTypes;
import nl.juiced.guhs.storage.BankContents;

/**
 * Bank Guh screen contents: a 3x3 crafting grid + result, and the player inventory as normal slots.
 * The (infinite) storage itself isn't made of slots: the server sends the item list with {@link BankContentsPayload}
 * and the screen asks for items with {@link nl.juiced.guhs.network.BankActionPayload}.
 * <p>
 * Slot indices: 0 = craft result, 1-9 = craft grid, 10-36 = inventory, 37-45 = hotbar.
 */
public class BankGuhMenu extends AbstractContainerMenu {
    // layout (shared with BankGuhScreen)
    public static final int GRID_X = 8, GRID_Y = 32, GRID_COLS = 9, GRID_ROWS = 4;
    public static final int CRAFT_X = 9, CRAFT_Y = 113;
    public static final int RESULT_X = 103, RESULT_Y = 131;
    public static final int INV_X = 9, INV_Y = 179;
    public static final int RESULT_SLOT = 0, GRID_START = 1, GRID_END = 10, INV_START = 10, HOTBAR_START = 37, INV_END = 46;

    public enum Action { TAKE, DEPOSIT_CARRIED, DEPOSIT_INVENTORY, CLEAR_GRID }

    private final Player player;
    private final BlockPos pos;
    @Nullable
    private final BankGuhBlockEntity bank; // server side only
    private final CraftingContainer craftGrid = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer craftResult = new ResultContainer();
    private int sentVersion = -1;
    /** Client side: the latest item list sent by the server. */
    private BankContents clientContents = BankContents.EMPTY;

    /** Server side. */
    public BankGuhMenu(int containerId, Inventory inventory, BankGuhBlockEntity bank) {
        this(containerId, inventory, bank.getBlockPos(), bank);
    }

    /** Client side (the server sends the block position when opening). */
    public BankGuhMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), null);
    }

    private BankGuhMenu(int containerId, Inventory inventory, BlockPos pos, @Nullable BankGuhBlockEntity bank) {
        super(ModMenuTypes.BANK_GUH.get(), containerId);
        this.player = inventory.player;
        this.pos = pos;
        this.bank = bank;

        addSlot(new ResultSlot(player, craftGrid, craftResult, 0, RESULT_X, RESULT_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftGrid, col + row * 3, CRAFT_X + col * 18, CRAFT_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !Features.isLoaned(stack);     // (it would end up in the stomach)
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INV_X + col * 18, INV_Y + 58));
        }
    }

    public BankContents getClientContents() {
        return clientContents;
    }

    public void setClientContents(BankContents contents) {
        this.clientContents = contents;
    }

    // --- sync the stomach to the client whenever it changed ---

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (bank != null && player instanceof ServerPlayer serverPlayer && bank.getStorage().version() != sentVersion) {
            sentVersion = bank.getStorage().version();
            PacketDistributor.sendToPlayer(serverPlayer, new BankContentsPayload(containerId, bank.getStorage().snapshot()));
        }
    }

    // --- actions from the screen (server side) ---

    public void handleAction(Action action, ItemStack item, int button, boolean shift) {
        if (bank == null) {
            return;
        }
        switch (action) {
            case TAKE -> {
                if (!getCarried().isEmpty()) {
                    depositCarried(button);
                    break;
                }
                int max = item.getMaxStackSize();
                int amount = button == 1 ? Math.max(1, (int) ((Math.min(bank.getStorage().count(item), max) + 1) / 2)) : max;
                ItemStack taken = bank.getStorage().extract(item, amount);
                if (shift) {
                    player.getInventory().add(taken);
                    bank.getStorage().insert(taken); // whatever didn't fit goes back
                } else {
                    setCarried(taken);
                }
            }
            case DEPOSIT_CARRIED -> depositCarried(button);
            case DEPOSIT_INVENTORY -> {
                // main inventory only (not the hotbar), so your tools stay in hand
                for (int i = INV_START; i < HOTBAR_START; i++) {
                    Slot slot = slots.get(i);
                    if (slot.hasItem() && !Features.isLoaned(slot.getItem())) {
                        bank.getStorage().insert(slot.getItem());
                        slot.set(ItemStack.EMPTY);
                    }
                }
            }
            case CLEAR_GRID -> {
                for (int i = GRID_START; i < GRID_END; i++) {
                    Slot slot = slots.get(i);
                    if (slot.hasItem() && !Features.isLoaned(slot.getItem())) {
                        bank.getStorage().insert(slot.getItem());
                        slot.set(ItemStack.EMPTY);
                    }
                }
            }
        }
        broadcastChanges();
    }

    private void depositCarried(int button) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || bank == null || Features.isLoaned(carried)) {
            return;                                   // (a loaned golf club, rod... isn't yours to keep: not in the stomach)
        }
        if (button == 1) {
            bank.getStorage().insert(carried, 1);
            carried.shrink(1);
        } else {
            bank.getStorage().insert(carried);
            setCarried(ItemStack.EMPTY);
        }
    }

    // --- crafting ---

    @Override
    public void slotsChanged(Container container) {
        if (container == craftGrid && player.level() instanceof Level level && !level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CraftingInput input = craftGrid.asCraftInput();
            ItemStack result = ItemStack.EMPTY;
            Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (recipe.isPresent() && craftResult.setRecipeUsed(level, serverPlayer, recipe.get())) {
                ItemStack crafted = recipe.get().value().assemble(input, level.registryAccess());
                if (crafted.isItemEnabled(level.enabledFeatures())) {
                    result = crafted;
                }
            }
            craftResult.setItem(0, result);
            setRemoteSlot(0, result);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != craftResult && super.canTakeItemForPickAll(stack, slot);
    }

    // --- shift-clicking: inventory/grid -> stomach, result -> inventory ---

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (index == RESULT_SLOT) {
            ItemStack copy = stack.copy();
            stack.getItem().onCraftedBy(stack, player.level(), player);
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            return copy; // keep crafting while shift is held
        }
        if (Features.isLoaned(stack)) {
            return ItemStack.EMPTY;                   // (loaned things stay with you)
        }
        if (bank != null) {
            bank.getStorage().insert(stack);
        }
        slot.set(ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // leftovers in the crafting grid go back into the stomach (or to the player if the bank is gone)
        if (bank != null && !bank.isRemoved()) {
            for (int i = 0; i < craftGrid.getContainerSize(); i++) {
                if (Features.isLoaned(craftGrid.getItem(i))) {
                    player.getInventory().placeItemBackInInventory(craftGrid.removeItemNoUpdate(i));
                } else {
                    bank.getStorage().insert(craftGrid.removeItemNoUpdate(i));
                }
            }
        } else if (!player.level().isClientSide) {
            clearContainer(player, craftGrid);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(ModBlocks.BANK_GUH.get())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    /** Helper for the screen: what the grid should show (all entries; filtering/sorting happens in the screen). */
    public List<BankContents.Entry> entries() {
        return clientContents.entries();
    }
}
