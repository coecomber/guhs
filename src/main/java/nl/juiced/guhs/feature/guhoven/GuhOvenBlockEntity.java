package nl.juiced.guhs.feature.guhoven;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Guhoven's block entity: a vanilla furnace (RecipeType.SMELTING) without fuel. Every server tick, while the oven gets guh
 * power ({@link GuhovenFeature#guhKracht}) and has something it can bake, its burn timer is topped up to one tick, and then the
 * vanilla furnace tick runs (cooking, XP, results). Without guh power the timer runs out at once and the cooking progress cools
 * down like a furnace without fuel. Nothing is ever burned as fuel: the fuel slot accepts nothing, and anything that still ends
 * up in it (a command, an old save) pops out on top of the oven.
 */
public class GuhOvenBlockEntity extends AbstractFurnaceBlockEntity {
    private static final int[] SLOTS_FOR_DOWN = {SLOT_RESULT};
    private static final int[] SLOTS_FOR_UP_AND_SIDES = {SLOT_INPUT};

    public GuhOvenBlockEntity(BlockPos pos, BlockState state) {
        super(GuhovenFeature.GUH_OVEN_BE.get(), pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.guhs.guh_oven");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new GuhOvenMenu(containerId, inventory, this, this.dataAccess);
    }

    /** The fuel slot takes nothing (not from a hopper, not from a pipe, not from the screen). */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != SLOT_FUEL && super.canPlaceItem(slot, stack);
    }

    /** Hoppers on top and on the sides fill the input slot (a furnace's sides would fill the fuel slot), the bottom takes results. */
    @Override
    public int[] getSlotsForFace(Direction direction) {
        return direction == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_UP_AND_SIDES;
    }

    /** Is the oven baking right now (its burn timer runs)? */
    public boolean bakt() {
        return this.dataAccess.get(DATA_LIT_TIME) > 0;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GuhOvenBlockEntity oven) {
        ItemStack fuel = oven.items.get(SLOT_FUEL);
        if (!fuel.isEmpty()) {
            oven.items.set(SLOT_FUEL, ItemStack.EMPTY);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, fuel);
            oven.setChanged();
        }
        if (GuhovenFeature.guhKracht(level, pos) && oven.kanBakken(level)) {
            // one tick of "fire": the vanilla tick counts it down to 0... next tick we top it up again, as long as the guh runs
            if (oven.dataAccess.get(DATA_LIT_TIME) < 2) {
                oven.dataAccess.set(DATA_LIT_TIME, 2);
                oven.dataAccess.set(DATA_LIT_DURATION, 1);
            }
        }
        AbstractFurnaceBlockEntity.serverTick(level, pos, state, oven);
        // the vanilla tick only flips LIT when the timer itself changes between lit and unlit; our top-up lights it from nothing
        BlockState now = level.getBlockState(pos);
        boolean lit = oven.bakt();
        if (now.is(GuhovenFeature.GUH_OVEN.get()) && now.getValue(AbstractFurnaceBlock.LIT) != lit) {
            level.setBlock(pos, now.setValue(AbstractFurnaceBlock.LIT, lit), 3);
        }
    }

    /**
     * Is there something in the input slot that smelts, and does its result fit in the result slot? (like the furnace's canBurn)
     * Also fills in a missing total cooking time.
     */
    private boolean kanBakken(ServerLevel level) {
        ItemStack input = this.items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return level.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, level).map(recipe -> {
            ItemStack result = recipe.value().assemble(recipeInput);
            if (result.isEmpty()) {
                return false;
            }
            if (this.dataAccess.get(DATA_COOKING_TOTAL_TIME) <= 0) {
                // (filled without setItem, e.g. /data or a structure: the vanilla furnace would never finish; we know the time)
                this.dataAccess.set(DATA_COOKING_TOTAL_TIME, recipe.value().cookingTime());
            }
            ItemStack out = this.items.get(SLOT_RESULT);
            if (out.isEmpty()) {
                return true;
            }
            return ItemStack.isSameItemSameComponents(out, result)
                    && out.getCount() + result.getCount() <= Math.min(getMaxStackSize(), result.getMaxStackSize());
        }).orElse(false);
    }
}
