package nl.juiced.guhs.feature.blub.knuffel;

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
import nl.juiced.guhs.feature.blub.BlubFeature;

/**
 * 1.8.0: a Blub-knuffel's block entity: which Blub it is a plush of ({@value #KEY}: meter, soort, tint, size, clothes, name;
 * see BlubGuhEntity#knuffelData). It travels on the item as {@code minecraft:custom_data} {@value #KEY} (the loot table
 * copies it back when the knuffel is broken). The client draws that Blub, small, on the block (client.BlubKnuffelRenderer).
 */
public class BlubKnuffelBlockEntity extends BlockEntity {
    public static final String KEY = "BlubKnuffel";

    private CompoundTag blub = new CompoundTag();

    public BlubKnuffelBlockEntity(BlockPos pos, BlockState state) {
        super(BlubFeature.KNUFFEL_BE.get(), pos, state);
    }

    /** The Blub it is a plush of (empty: a plain blue Blub). */
    public CompoundTag blub() {
        return blub;
    }

    public void zetBlub(CompoundTag blub) {
        this.blub = blub.copy();
        setChanged();
    }

    /** An item stack of a knuffel of this Blub. */
    public static void schrijf(net.minecraft.world.item.ItemStack stack, CompoundTag blub) {
        CompoundTag t = new CompoundTag();
        t.put(KEY, blub.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    /** The Blub on an item stack (empty: none). */
    public static CompoundTag lees(net.minecraft.world.item.ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag().getCompoundOrEmpty(KEY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(KEY, CompoundTag.CODEC, blub);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        blub = input.read(KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        CustomData data = components.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            blub = data.copyTag().getCompoundOrEmpty(KEY);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!blub.isEmpty()) {
            CompoundTag t = new CompoundTag();
            t.put(KEY, blub.copy());
            components.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(KEY);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
