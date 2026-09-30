package nl.juiced.guhs.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.registry.ModBlockEntities;

/** Remembers how many kaas knabbels the pan can still fry. */
public class FryingPanBlockEntity extends BlockEntity {
    private int charges;

    public FryingPanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRYING_PAN.get(), pos, state);
    }

    public int getCharges() {
        return charges;
    }

    public void setCharges(int charges) {
        this.charges = charges;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        charges = tag.getInt("Charges");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Charges", charges);
    }
}
