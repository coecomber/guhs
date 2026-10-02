package nl.juiced.guhs.compat.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.menu.BankGuhMenu;
import nl.juiced.guhs.network.BankJeiPayload;
import nl.juiced.guhs.registry.ModMenuTypes;
import nl.juiced.guhs.storage.BankContents;

/**
 * 1.2.5: JEI support for the Bank Guh. JEI's "+" on a crafting recipe fills the Bank Guh's crafting grid with items from the
 * bank first and from your inventory second (shift: as many as fit). Only loaded when JEI is installed.
 */
@JeiPlugin
public class GuhsJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Guhs.id("jei");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new BankTransfer(registration.getTransferHelper()), RecipeTypes.CRAFTING);
    }

    /** 1.2.5: the Guhoven bakes the furnace's smelting recipes (on guh power): JEI lists it next to the furnace. */
    @Override
    public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(RecipeTypes.SMELTING, nl.juiced.guhs.feature.guhoven.GuhovenFeature.GUH_OVEN.get());
    }

    static final class BankTransfer implements IRecipeTransferHandler<BankGuhMenu, RecipeHolder<CraftingRecipe>> {
        private final IRecipeTransferHandlerHelper helper;

        BankTransfer(IRecipeTransferHandlerHelper helper) {
            this.helper = helper;
        }

        @Override
        public Class<? extends BankGuhMenu> getContainerClass() {
            return BankGuhMenu.class;
        }

        @Override
        public Optional<MenuType<BankGuhMenu>> getMenuType() {
            return Optional.of(ModMenuTypes.BANK_GUH.get());
        }

        @Override
        public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        @Override
        public IRecipeTransferError transferRecipe(BankGuhMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots,
                                                   Player player, boolean maxTransfer, boolean doTransfer) {
            List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
            // what we have: the bank (client copy), the inventory and what's in the grid now (it goes back to the bank)
            Map<String, Long> pool = new HashMap<>();
            BankContents bank = menu.getClientContents();
            if (bank != null) {
                for (BankContents.Entry e : bank.entries()) {
                    pool.merge(key(e.item()), e.count(), Long::sum);
                }
            }
            for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
                if (!s.isEmpty()) {
                    pool.merge(key(s), (long) s.getCount(), Long::sum);
                }
            }
            for (int i = BankGuhMenu.GRID_START; i < BankGuhMenu.GRID_END; i++) {
                ItemStack s = menu.getSlot(i).getItem();
                if (!s.isEmpty()) {
                    pool.merge(key(s), (long) s.getCount(), Long::sum);
                }
            }
            List<List<ItemStack>> keuzes = new ArrayList<>();
            List<IRecipeSlotView> missing = new ArrayList<>();
            for (IRecipeSlotView view : inputs) {
                List<ItemStack> kandidaten = view.getItemStacks().filter(s -> !s.isEmpty()).map(s -> s.copyWithCount(1)).limit(64).toList();
                keuzes.add(kandidaten);
                if (kandidaten.isEmpty()) {
                    continue;
                }
                boolean gevonden = false;
                for (ItemStack k : kandidaten) {
                    Long n = pool.get(key(k));
                    if (n != null && n > 0) {
                        pool.put(key(k), n - 1);
                        gevonden = true;
                        break;
                    }
                }
                if (!gevonden) {
                    missing.add(view);
                }
            }
            if (!missing.isEmpty()) {
                return helper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), missing);
            }
            if (keuzes.size() > 9) {
                return helper.createInternalError();
            }
            while (keuzes.size() < 9) {
                keuzes.add(List.of());
            }
            if (doTransfer) {
                ClientPacketDistributor.sendToServer(new BankJeiPayload(menu.containerId, keuzes, maxTransfer));
            }
            return null;
        }

        private static String key(ItemStack stack) {
            return stack.getItem() + "|" + stack.getComponentsPatch();
        }
    }
}
