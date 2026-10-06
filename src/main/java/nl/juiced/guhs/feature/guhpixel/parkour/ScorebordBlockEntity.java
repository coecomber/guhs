package nl.juiced.guhs.feature.guhpixel.parkour;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Scorebord of a Guh-parkour: a copy of the scores of the Startpaaltje it is linked to (the nearest one within reach
 * when it is placed; the post sends its rows after every lap), synced to the client for the renderer: per guh its name,
 * laps, best and last lap time.
 */
public class ScorebordBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos paal;
    private ListTag rijen = new ListTag();

    public ScorebordBlockEntity(BlockPos pos, BlockState state) {
        super(ParkourSlice.SCOREBORD_BE.get(), pos, state);
    }

    @Nullable
    public BlockPos paal() {
        return paal;
    }

    /** The rows (Naam, Rondjes, Beste, Laatste; best first). */
    public ListTag rijen() {
        return rijen;
    }

    public void koppel(@Nullable BlockPos nieuw) {
        paal = nieuw == null ? null : nieuw.immutable();
        if (nieuw == null) {
            rijen = new ListTag();
        }
        sync();
    }

    public void zet(ListTag nieuw) {
        rijen = nieuw.copy();
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        CompoundTag t = new CompoundTag();
        if (paal != null) {
            t.putLong("Paal", paal.asLong());
        }
        t.put("Rijen", rijen.copy());
        tag.store("Bord", CompoundTag.CODEC, t);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        CompoundTag t = tag.read("Bord", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        paal = t.contains("Paal") ? BlockPos.of(t.getLongOr("Paal", 0L)) : null;
        rijen = t.getListOrEmpty("Rijen").copy();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
