package nl.juiced.guhs.feature.techbuis;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * The block entity of a sensor ({@link SensorBlock}): a guh machine without slots that measures something every tick it has
 * vadskracht ({@link #meet}) and says what it found with {@link #zet}: the redstone signal (on or off: the block state, so
 * redstone hears it) and a strength 0..15 for a comparator. Without vadskracht the signal goes off. The face (the machine's
 * "full" face: surprised) shows the signal.
 */
public abstract class SensorBlockEntity extends MachineBlockEntity {
    private boolean signaal;
    private int sterkte;

    protected SensorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, VadsGetallen.SENSOR, 0);
    }

    /** Measure (called every server tick while the sensor has vadskracht) and call {@link #zet}. */
    protected abstract void meet(ServerLevel level, long nu);

    /** A player used the sensor: change its setting (sluipt: the other setting, or backwards). */
    public abstract void klik(ServerPlayer player, boolean sluipt);

    public boolean signaal() {
        return signaal;
    }

    /** 0..15, for a comparator. */
    public int sterkte() {
        return sterkte;
    }

    @Override
    protected final boolean kanWerken() {
        return true;
    }

    /** The surprised face = the signal is on. */
    @Override
    protected final boolean isVol() {
        return signaal;
    }

    @Override
    protected final void werk() {
        if (level instanceof ServerLevel server) {
            meet(server, server.getGameTime());
        }
    }

    /** After the machine's tick: without vadskracht there is no signal. */
    final void naTick() {
        if (!heeftKracht() && (signaal || sterkte != 0)) {
            zet(false, 0);
        }
    }

    protected final void zet(boolean aan, int kracht) {
        if (level == null || level.isClientSide()) {
            return;
        }
        kracht = Mth.clamp(kracht, 0, 15);
        boolean anders = aan != signaal;
        int oud = sterkte;
        signaal = aan;
        sterkte = kracht;
        BlockState state = getBlockState();
        if (state.hasProperty(SensorBlock.SIGNAAL) && state.getValue(SensorBlock.SIGNAAL) != aan) {
            level.setBlock(worldPosition, state.setValue(SensorBlock.SIGNAAL, aan), Block.UPDATE_ALL);
        }
        if (anders || oud != kracht) {
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    /** The little click of a setting that changes. */
    protected final void tik() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.4f, 1.4f);
        }
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putBoolean("Signaal", signaal);
        uit.putInt("Sterkte", sterkte);
    }

    @Override
    protected void laden(ValueInput in) {
        signaal = in.getBooleanOr("Signaal", false);
        sterkte = in.getIntOr("Sterkte", 0);
    }
}
