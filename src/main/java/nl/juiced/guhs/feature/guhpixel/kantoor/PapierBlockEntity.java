package nl.juiced.guhs.feature.guhpixel.kantoor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What is written on a paper that hangs on the wall: the paper's tag ({@link Papier}), taken from the item's custom data
 * when it is hung up and given back to the item when the block is broken (the loot table's copy_components).
 */
public class PapierBlockEntity extends BlockEntity {
    private CompoundTag papier = new CompoundTag();

    public PapierBlockEntity(BlockPos pos, BlockState state) {
        super(KantoorSlice.PAPIER_BE.get(), pos, state);
    }

    /** A copy of the paper's tag. */
    public CompoundTag papier() {
        return papier.copy();
    }

    public void zet(CompoundTag nieuw) {
        papier = nieuw.copy();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        papier = in.read("Papier", CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("Papier", CompoundTag.CODEC, papier);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        CustomData d = input.get(DataComponents.CUSTOM_DATA);
        papier = d == null ? new CompoundTag() : d.copyTag().getCompoundOrEmpty(Papier.SLEUTEL);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!papier.isEmpty()) {
            CompoundTag wortel = new CompoundTag();
            wortel.put(Papier.SLEUTEL, papier.copy());
            components.set(DataComponents.CUSTOM_DATA, CustomData.of(wortel));
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput out) {
        out.discard("Papier");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
