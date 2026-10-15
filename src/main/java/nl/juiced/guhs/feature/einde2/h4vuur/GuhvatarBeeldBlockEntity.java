package nl.juiced.guhs.feature.einde2.h4vuur;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * einde2 phase B, slice h4_vuur: the Guhvatar statue's block entity: whose statue it is (the {@code minecraft:profile} of the
 * item it was placed from, as a player head keeps its owner). The client draws that player's skin on it (client.BeeldRenderer).
 */
public class GuhvatarBeeldBlockEntity extends BlockEntity {
    @Nullable
    private ResolvableProfile eigenaar;

    public GuhvatarBeeldBlockEntity(BlockPos pos, BlockState state) {
        super(E2H4VuurFeature.BEELD_BE.get(), pos, state);
    }

    @Nullable
    public ResolvableProfile eigenaar() {
        return eigenaar;
    }

    public void zetEigenaar(@Nullable ResolvableProfile profiel) {
        this.eigenaar = profiel;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("profile", ResolvableProfile.CODEC, eigenaar);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        eigenaar = input.read("profile", ResolvableProfile.CODEC).orElse(null);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        eigenaar = components.get(DataComponents.PROFILE);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.PROFILE, eigenaar);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("profile");
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
