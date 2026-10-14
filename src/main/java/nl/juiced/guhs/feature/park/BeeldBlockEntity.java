package nl.juiced.guhs.feature.park;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.taal.Tekst;

/**
 * 1.4.2 (Het Knabbelpark): the block entity of a statue ({@link ParkBlokken.Beeld}, {@link ParkBlokken.EigenBeeld}). It
 * moves nothing (no animation controllers: the guh model stands in its rest pose, a statue); the client draws the guh on
 * top of the plinth. A statue of your own guh keeps the guh's variant and name (also on the item: custom data
 * {@link #VARIANT} / {@link #NAAM}).
 */
public class BeeldBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final String VARIANT = "guhs_beeld_variant", NAAM = "guhs_beeld_naam";
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    /** EigenBeeld: the variant of the guh it shows (the plain guh until it took a look). */
    private GuhVariant variant = GuhVariant.NORMAL;
    private Component naam = Component.empty();

    public BeeldBlockEntity(BlockPos pos, BlockState state) {
        super(ParkFeature.BEELD_BE.get(), pos, state);
    }

    public GuhVariant variant() {
        return variant;
    }

    public Component naam() {
        return naam;
    }

    public void zet(GuhVariant variant, Component naam) {
        this.variant = variant;
        this.naam = naam;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Takes the look that is on the item (EigenBeeld placed). */
    public void vanItem(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            zet(GuhVariant.byId(tag.getStringOr(VARIANT, "normal")), Tekst.get(tag, NAAM));
        }
    }

    /** Puts its look on the item (broken, or picked with the middle mouse button). */
    public void naarItem(ItemStack stack) {
        schrijf(stack, variant, naam);
    }

    /** The look on an EigenBeeld item. */
    public static void schrijf(ItemStack stack, GuhVariant variant, Component naam) {
        CompoundTag tag = stack.has(DataComponents.CUSTOM_DATA) ? stack.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
        tag.putString(VARIANT, variant.id());
        Tekst.put(tag, NAAM, naam);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        variant = GuhVariant.byId(in.getStringOr(VARIANT, "normal"));
        naam = Tekst.get(in, NAAM);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString(VARIANT, variant.id());
        Tekst.put(out, NAAM, naam);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // (a statue: no animation, the model's rest pose)
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
