package nl.juiced.guhs.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.menu.BankGuhMenu;
import nl.juiced.guhs.registry.ModBlockEntities;
import nl.juiced.guhs.registry.ModDataComponents;
import nl.juiced.guhs.storage.BankContents;
import nl.juiced.guhs.storage.BankStorage;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** The Bank Guh: a sitting guh that keeps an infinite amount of items in its stomach. */
public class BankGuhBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh_sitting.idle");

    private final BankStorage storage = new BankStorage(this::setChanged);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public BankGuhBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BANK_GUH.get(), pos, state);
    }

    public BankStorage getStorage() {
        return storage;
    }

    // --- menu ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.guhs.bank_guh");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BankGuhMenu(containerId, inventory, this);
    }

    // --- saving (the storage is also copied onto the item when the block is broken) ---

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Stomach")) {
            BankContents.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("Stomach"))
                    .resultOrPartial(err -> { })
                    .ifPresent(storage::load);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        BankContents.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), storage.snapshot())
                .ifSuccess(t -> tag.put("Stomach", t));
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        storage.load(input.getOrDefault(ModDataComponents.BANK_CONTENTS.get(), BankContents.EMPTY));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        BankContents contents = storage.snapshot();
        if (!contents.isEmpty()) {
            components.set(ModDataComponents.BANK_CONTENTS.get(), contents);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Stomach");
    }

    // --- GeckoLib ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
