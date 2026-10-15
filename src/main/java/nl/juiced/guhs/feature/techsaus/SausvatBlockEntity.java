package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import nl.juiced.guhs.feature.vadskracht.SausTank;
import nl.juiced.guhs.feature.vadskracht.Sauzen;

/**
 * The tank of a {@link SausvatBlock}: {@link SausGetallen#VAT} mB of one of the sauces of the Guh-technologie. The tank
 * itself is what hoses and pipes get (in and out, every side). Whenever the content changes the block shows it
 * ({@link SausvatBlock#SAUS}, {@link SausvatBlock#NIVEAU}). The sauce travels with the item when the vat is broken
 * ({@link TechsausFeature#INHOUD}, loot table function copy_components).
 */
public class SausvatBlockEntity extends BlockEntity {
    private final SausTank tank = new SausTank(SausGetallen.VAT, Sauzen::isTechniek, this::veranderd);

    public SausvatBlockEntity(BlockPos pos, BlockState state) {
        super(TechsausFeature.SAUSVAT_BE.get(), pos, state);
    }

    public SausTank tank() {
        return tank;
    }

    private void veranderd() {
        setChanged();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.getBlock() instanceof SausvatBlock) {
            BlockState nu = state.setValue(SausvatBlock.SAUS, SausvatBlock.Saus.van(tank.saus()))
                    .setValue(SausvatBlock.NIVEAU, SausvatBlock.niveau(tank.inhoud(), tank.max()));
            if (nu != state) {
                level.setBlock(worldPosition, nu, Block.UPDATE_CLIENTS);
            }
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    /** A right-click with something that holds fluid: tap a bucket out of the vat, or pour it in. */
    public void emmer(ServerPlayer player, InteractionHand hand) {
        int eerst = tank.inhoud();
        FluidResource inHand = FluidResource.of(FluidUtil.getFirstStackContained(player.getItemInHand(hand)));
        if (!FluidUtil.interactWithFluidHandler(player, hand, worldPosition, tank, null)) {
            String waarom;
            if (inHand.isEmpty()) {
                waarom = tank.isLeeg() ? "vat.leeg" : "vat.te_weinig";             // an empty bucket, and less than a bucket to tap
            } else if (!Sauzen.isTechniek(inHand)) {
                waarom = "vat.geen_saus";
            } else {
                waarom = !tank.isLeeg() && !tank.saus().equals(inHand) ? "vat.andere_saus" : "vat.vol";
            }
            player.sendOverlayMessage(Component.translatable(SausTekst.K + waarom, SausTekst.naam(tank.saus())).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (tank.inhoud() < eerst) {
            SausBeloning.geef(player, SausBeloning.GETAPT);
        }
        status(player);
    }

    /** "Sausvat: 3,0/16,0 emmers kaassaus" above the hotbar. */
    public void status(ServerPlayer player) {
        player.sendOverlayMessage(SausTekst.tank(tank, FluidResource.EMPTY).withStyle(ChatFormatting.YELLOW));
    }

    // --- saving (the sauce is also copied onto the item when the block is broken) ---

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        tank.laden(in, "Tank");
    }

    @Override
    protected void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        tank.opslaan(uit, "Tank");
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        SimpleFluidContent inhoud = input.getOrDefault(TechsausFeature.INHOUD.get(), SimpleFluidContent.EMPTY);
        if (!inhoud.isEmpty()) {
            tank.zet(FluidResource.of(inhoud.copy()), inhoud.getAmount());
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isLeeg()) {
            components.set(TechsausFeature.INHOUD.get(), SimpleFluidContent.copyOf(tank.saus().toStack(tank.inhoud())));
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput tag) {
        tag.discard("Tank");
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
