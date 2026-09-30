package nl.juiced.guhs.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.registry.ModBlockEntities;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * Lets the whole rail piece be drawn by a block entity renderer. A piece can be a finish line (the guh kermis): a sled
 * passing it has done a lap.
 */
public class SleeRailBlockEntity extends BlockEntity {
    private boolean finish;

    public SleeRailBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLEE_RAIL.get(), pos, state);
    }

    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        finish = tag.getBooleanOr("Finish", false);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (finish) {
            tag.putBoolean("Finish", true);
        }
    }
}
