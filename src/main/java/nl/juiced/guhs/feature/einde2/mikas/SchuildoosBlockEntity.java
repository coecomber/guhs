package nl.juiced.guhs.feature.einde2.mikas;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What is inside a {@link SchuildoosBlock}: 27 slots. Breaking the block does NOT spill them ({@link #preRemoveSideEffects}
 * does nothing): the contents travel on the item (the container component, see BaseContainerBlockEntity).
 */
public class SchuildoosBlockEntity extends RandomizableContainerBlockEntity {
    public static final int SLOTS = 27;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final ContainerOpenersCounter kijkers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            geluid(E2MikasFeature.DOOS_OPEN.get());
            zetOpen(state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            geluid(E2MikasFeature.DOOS_DICHT.get());
            zetOpen(state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof SchuildoosMenu menu && menu.container() == SchuildoosBlockEntity.this;
        }
    };

    public SchuildoosBlockEntity(BlockPos pos, BlockState state) {
        super(E2MikasFeature.SCHUILDOOS_BE.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.guhs.schuildoos");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new SchuildoosMenu(id, inventory, this);
    }

    /** (a Schuildoos never goes into a Schuildoos: hoppers ask this) */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.canFitInsideContainerItems();
    }

    /** (the contents stay in the box: they go along on the item) */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, items, false);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        if (!tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, items);
        }
    }

    // --- the lid lifts while somebody looks inside --------------------------------------------------------------------------

    @Override
    public void startOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator()) {
            kijkers.incrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState(), user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator()) {
            kijkers.decrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return kijkers.getEntitiesWithContainerOpen(getLevel(), getBlockPos());
    }

    public void recheckOpen() {
        if (!this.remove) {
            kijkers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
        }
    }

    private void zetOpen(BlockState state, boolean open) {
        if (state.getBlock() instanceof SchuildoosBlock) {
            this.level.setBlock(getBlockPos(), state.setValue(SchuildoosBlock.OPEN, open), 3);
        }
    }

    private void geluid(SoundEvent event) {
        this.level.playSound(null, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.8, this.worldPosition.getZ() + 0.5, event,
                SoundSource.BLOCKS, 0.5f, this.level.getRandom().nextFloat() * 0.1f + 0.9f);
    }
}
