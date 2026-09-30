package nl.juiced.guhs.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.registry.ModBlockEntities;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
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
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        charges = tag.getIntOr("Charges", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt("Charges", charges);
    }
}
