package nl.juiced.guhs.feature.kermisritten2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** What both rides share: the turn of the building, and the one-time clean-up of the props the stamp's design left in the way. */
public abstract class AnkerBlockEntity extends BlockEntity {
    private boolean geveegd;

    protected AnkerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract void rit(ServerLevel level);

    protected abstract void veeg(ServerLevel level);

    protected abstract Direction ontwerp();

    protected Rotation draai() {
        return Anker.draai(ontwerp(), getBlockState().getValue(AnkerBlock.FACING));
    }

    protected final void veegEenmaal(ServerLevel level) {
        if (!geveegd) {
            geveegd = true;
            veeg(level);
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Geveegd", geveegd);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        geveegd = input.getBooleanOr("Geveegd", false);
    }
}
