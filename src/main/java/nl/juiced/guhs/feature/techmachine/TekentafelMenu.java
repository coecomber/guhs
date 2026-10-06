package nl.juiced.guhs.feature.techmachine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadskracht.Snoet;

/**
 * The Tekentafel's menu: a crafting grid of 3 x 3 ({@link #ROOSTER}..), a slot for sheets ({@link #VEL}: Bouwtekeningen,
 * empty or drawn on) and the result ({@link #RESULTAAT}). While the grid holds a recipe and the sheet slot a sheet, the
 * result shows that sheet with the recipe drawn on it; taking it uses up ONE sheet and nothing of the grid (you only
 * show the table what you mean). A sheet that was drawn on before is drawn over. Shift-click draws on every sheet. What
 * lies on the table goes back to the player when the screen closes.
 */
public class TekentafelMenu extends AbstractContainerMenu {
    public static final int RESULTAAT = 0, ROOSTER = 1, VEL = 10, INVENTARIS = 11;
    public static final int ROOSTER_X = 30, ROOSTER_Y = 17, VEL_X = 93, VEL_Y = 55, RESULTAAT_X = 124, RESULTAAT_Y = 35;

    public final BlockPos pos;
    private final ContainerLevelAccess toegang;
    private final Player speler;
    private final CraftingContainer rooster = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultaat = new ResultContainer();
    private final SimpleContainer vellen = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    /** Client side. */
    public TekentafelMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, ContainerLevelAccess.NULL, buf.readBlockPos());
    }

    public TekentafelMenu(int id, Inventory inventory, ContainerLevelAccess toegang, BlockPos pos) {
        super(TechmachineFeature.TEKENTAFEL_MENU.get(), id);
        this.toegang = toegang;
        this.pos = pos;
        this.speler = inventory.player;
        addSlot(new Slot(resultaat, 0, RESULTAAT_X, RESULTAAT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                getekend(player);
                super.onTake(player, stack);
            }
        });
        for (int rij = 0; rij < 3; rij++) {
            for (int kolom = 0; kolom < 3; kolom++) {
                addSlot(new Slot(rooster, kolom + rij * 3, ROOSTER_X + kolom * 18, ROOSTER_Y + rij * 18));
            }
        }
        addSlot(new Slot(vellen, 0, VEL_X, VEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(TechmachineFeature.BOUWTEKENING.get());
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        toegang.execute((level, p) -> TekentafelBlock.zetSnoet(level, p, Snoet.WERKT));
    }

    /** One drawing was taken: one sheet is used up (which draws the next one right away, if there is another sheet). */
    private void getekend(Player player) {
        vellen.removeItem(0, 1);
        toegang.execute((level, p) -> level.playSound(null, p, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.2f));
        if (player instanceof ServerPlayer sp) {
            TechBlockEntity.beloon(sp, "tekening");
        }
    }

    /** The recipe on the grid or the sheets changed: what would the next drawing be? (Server: the client knows no recipes.) */
    @Override
    public void slotsChanged(Container container) {
        toegang.execute((level, p) -> {
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            List<ItemStack> cellen = new ArrayList<>(9);
            for (int i = 0; i < 9; i++) {
                cellen.add(rooster.getItem(i));
            }
            Bouwtekening tekening = Bouwtekeningen.maak(server, cellen);
            boolean vel = vellen.getItem(0).is(TechmachineFeature.BOUWTEKENING.get());
            resultaat.setItem(0, tekening != null && vel ? Bouwtekeningen.metTekening(tekening) : ItemStack.EMPTY);
            TekentafelBlock.zetSnoet(level, p, tekening != null ? Snoet.VOL : Snoet.WERKT);
            broadcastChanges();
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack kopie = stack.copy();
        if (index == RESULTAAT) {
            if (!moveItemStackTo(stack, INVENTARIS, INVENTARIS + 36, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, kopie);
        } else if (index >= INVENTARIS) {
            // a sheet goes to the sheet slot; anything else between the inventory and the hotbar
            boolean verplaatst = stack.is(TechmachineFeature.BOUWTEKENING.get()) && moveItemStackTo(stack, VEL, VEL + 1, false);
            if (!verplaatst) {
                verplaatst = index < INVENTARIS + 27 ? moveItemStackTo(stack, INVENTARIS + 27, INVENTARIS + 36, false)
                        : moveItemStackTo(stack, INVENTARIS, INVENTARIS + 27, false);
            }
            if (!verplaatst) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INVENTARIS, INVENTARIS + 36, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == kopie.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        if (index == RESULTAAT) {
            player.drop(stack, false);
        }
        return kopie;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != resultaat && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        toegang.execute((level, p) -> {
            clearContainer(player, rooster);
            clearContainer(player, vellen);
            if (level instanceof ServerLevel server && !TekentafelBlock.nogIemand(server, p, player)) {
                TekentafelBlock.zetSnoet(level, p, Snoet.SLAAPT);
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(toegang, player, TechmachineFeature.TEKENTAFEL.get());
    }
}
