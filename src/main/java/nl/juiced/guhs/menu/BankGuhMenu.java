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
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.BankVolPayload;
import nl.juiced.guhs.network.BankContentsPayload;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModMenuTypes;
import nl.juiced.guhs.storage.BankContents;
import nl.juiced.guhs.storage.BankStorage;

/**
 * Bank Guh screen contents: a 3x3 crafting grid + result, and the player inventory as normal slots.
 * The storage itself isn't made of slots: the server sends the item list with {@link BankContentsPayload}
 * and the screen asks for items with {@link nl.juiced.guhs.network.BankActionPayload}.
 * <p>
 * bbq2: the stomach holds at most {@link BankStorage#CAP} of one kind of item (until the bank is upgraded), so every way
 * in goes through {@link #stop}, which gives back what did not fit: that remainder always stays where it was (the cursor,
 * the inventory slot, the crafting grid) or goes to the player, never away. A refused item is told to the screen
 * ({@link BankVolPayload}).
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
    /** Client side: the latest item list sent by the server, and whether the bank has the upgrade (no cap). */
    private BankContents clientContents = BankContents.EMPTY;
    private boolean clientUpgraded;
    /** Client side: the item the bank refused last (it is full of it), and a counter so the screen sees a new refusal. */
    private ItemStack clientVol = ItemStack.EMPTY;
    private int clientVolTeller;
    /** Server side: the first kind of item refused during the action that is being handled. */
    private ItemStack geweigerd = ItemStack.EMPTY;
    /** Server side: a kind of item reached the cap during the action that is being handled. */
    private boolean vol;

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

    public void setClientContents(BankContents contents, boolean upgraded) {
        this.clientContents = contents;
        this.clientUpgraded = upgraded;
    }

    /** Client side: does the open bank have the upgrade (no cap)? */
    public boolean isClientUpgraded() {
        return clientUpgraded;
    }

    public void setClientVol(ItemStack item) {
        this.clientVol = item;
        this.clientVolTeller++;
    }

    /** Client side: what the bank refused last because it is full of it (empty: nothing yet). */
    public ItemStack getClientVol() {
        return clientVol;
    }

    public int getClientVolTeller() {
        return clientVolTeller;
    }

    // --- sync the stomach to the client whenever it changed ---

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (bank != null && player instanceof ServerPlayer serverPlayer && bank.getStorage().version() != sentVersion) {
            sentVersion = bank.getStorage().version();
            ModNetworking.sendTo(serverPlayer, new BankContentsPayload(containerId, bank.getStorage().snapshot(), bank.getStorage().isUpgraded()));
        }
    }

    // --- the one way into the stomach ---

    /**
     * Puts a stack into the stomach and returns what did NOT fit (the cap): the caller keeps that. Remembers the first
     * kind that was refused, so {@link #meld} can tell the screen.
     */
    private ItemStack stop(ItemStack stack) {
        if (bank == null || stack.isEmpty()) {
            return stack;
        }
        ItemStack rest = bank.getStorage().insert(stack);
        gestopt(stack, !rest.isEmpty());
        return rest;
    }

    private void gestopt(ItemStack soort, boolean rest) {
        if (rest && geweigerd.isEmpty()) {
            geweigerd = soort.copyWithCount(1);
        }
        if (!bank.getStorage().isUpgraded() && bank.getStorage().isFull(soort)) {
            vol = true;
        }
    }

    /** After an action: tell the screen what was refused, and count the "my bank is full" quest. */
    private void meld() {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!geweigerd.isEmpty()) {
                ModNetworking.sendTo(serverPlayer, new BankVolPayload(containerId, geweigerd));
            }
            if (vol) {
                BankFeature.vol(serverPlayer);
            }
        }
        geweigerd = ItemStack.EMPTY;
        vol = false;
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
                    bank.getStorage().restore(taken); // whatever didn't fit goes back (it came out a moment ago: no cap)
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
                        slot.set(stop(slot.getItem()));       // (what the bank is full of stays in the inventory)
                    }
                }
            }
            case CLEAR_GRID -> {
                for (int i = GRID_START; i < GRID_END; i++) {
                    Slot slot = slots.get(i);
                    if (slot.hasItem() && !Features.isLoaned(slot.getItem())) {
                        ItemStack rest = stop(slot.getItem());
                        if (!rest.isEmpty()) {
                            player.getInventory().add(rest);  // the bank is full of it: to the inventory (shrinks rest)
                        }
                        slot.set(rest.isEmpty() ? ItemStack.EMPTY : rest);   // (no room there either: it stays in the grid)
                    }
                }
            }
        }
        meld();
        broadcastChanges();
    }

    /**
     * 1.2.5: JEI's "+": put a recipe in the crafting grid. Per grid slot the items that may go there; each comes from the bank
     * first, then from the player's inventory (what was in the grid goes back to the bank first). max: as many as fit.
     */
    public void vulGrid(List<List<ItemStack>> keuzes, boolean max) {
        if (bank == null) {
            return;
        }
        handleAction(Action.CLEAR_GRID, ItemStack.EMPTY, 0, false);
        for (int i = 0; i < 9; i++) {
            if (!craftGrid.getItem(i).isEmpty()) {
                return;                               // (the old grid fits nowhere: bank and inventory are full of it)
            }
        }
        ItemStack[] gekozen = new ItemStack[9];
        for (int i = 0; i < 9 && i < keuzes.size(); i++) {
            for (ItemStack kandidaat : keuzes.get(i)) {
                if (!kandidaat.isEmpty() && beschikbaar(kandidaat) > 0) {
                    gekozen[i] = kandidaat.copyWithCount(1);
                    break;
                }
            }
        }
        int rondes = max ? 64 : 1;
        for (int r = 0; r < rondes; r++) {
            // check the whole round first, so the grid stays even
            java.util.Map<String, Integer> nodig = new java.util.HashMap<>();
            boolean kan = false;
            for (int i = 0; i < 9; i++) {
                if (gekozen[i] == null) {
                    continue;
                }
                ItemStack in = craftGrid.getItem(i);
                if (!in.isEmpty() && in.getCount() >= in.getMaxStackSize()) {
                    kan = false;
                    nodig = null;
                    break;
                }
                kan = true;
                nodig.merge(sleutel(gekozen[i]), 1, Integer::sum);
            }
            if (!kan || nodig == null) {
                break;
            }
            boolean genoeg = true;
            for (int i = 0; i < 9; i++) {
                if (gekozen[i] != null && beschikbaar(gekozen[i]) < nodig.get(sleutel(gekozen[i]))) {
                    genoeg = false;
                }
            }
            if (!genoeg) {
                break;
            }
            for (int i = 0; i < 9; i++) {
                if (gekozen[i] != null) {
                    ItemStack een = neem(gekozen[i]);
                    ItemStack in = craftGrid.getItem(i);
                    if (in.isEmpty()) {
                        craftGrid.setItem(i, een);
                    } else {
                        in.grow(een.getCount());
                    }
                }
            }
        }
        slotsChanged(craftGrid);
        broadcastChanges();
    }

    private static String sleutel(ItemStack stack) {
        return stack.getItem() + "|" + stack.getComponentsPatch();
    }

    /** How many of this item the bank plus the player's inventory have. */
    private long beschikbaar(ItemStack like) {
        long n = bank.getStorage().count(like);
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (ItemStack.isSameItemSameComponents(s, like) && !Features.isLoaned(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** One of this item: from the bank first, else from the player's inventory. */
    private ItemStack neem(ItemStack like) {
        ItemStack uitBank = bank.getStorage().extract(like, 1);
        if (!uitBank.isEmpty()) {
            return uitBank;
        }
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (ItemStack.isSameItemSameComponents(s, like) && !Features.isLoaned(s)) {
                return s.split(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private void depositCarried(int button) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || bank == null || Features.isLoaned(carried)) {
            return;                                   // (a loaned golf club, rod... isn't yours to keep: not in the stomach)
        }
        if (button == 1) {
            boolean erin = bank.getStorage().insert(carried, 1) > 0;
            gestopt(carried, !erin);
            if (erin) {
                carried.shrink(1);
            }
        } else {
            setCarried(stop(carried));                // (what the bank is full of stays on the cursor)
        }
    }

    // --- crafting ---

    @Override
    public void slotsChanged(Container container) {
        if (container == craftGrid && player.level() instanceof Level level && !level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            CraftingInput input = craftGrid.asCraftInput();
            ItemStack result = ItemStack.EMPTY;
            Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (recipe.isPresent() && craftResult.setRecipeUsed(serverPlayer, recipe.get())) {
                ItemStack crafted = recipe.get().value().assemble(input);
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
            stack.getItem().onCraftedBy(stack, player);
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
            ItemStack rest = stop(stack);
            if (!rest.isEmpty() && index >= GRID_START && index < GRID_END) {
                moveItemStackTo(rest, INV_START, INV_END, false);   // out of the grid: what the bank is full of goes to the inventory
            }
            slot.set(rest.isEmpty() ? ItemStack.EMPTY : rest);   // (else it stays where it was)
            meld();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // leftovers in the crafting grid go back into the stomach (or to the player if the bank is gone)
        if (bank != null && !bank.isRemoved()) {
            for (int i = 0; i < craftGrid.getContainerSize(); i++) {
                ItemStack stack = craftGrid.removeItemNoUpdate(i);
                if (!Features.isLoaned(stack)) {
                    stack = bank.getStorage().insert(stack);
                }
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack);   // loaned, or the bank is full of it: back to the player
                }
            }
        } else if (!player.level().isClientSide()) {
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
