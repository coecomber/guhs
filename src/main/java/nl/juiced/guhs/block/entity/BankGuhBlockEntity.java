package nl.juiced.guhs.block.entity;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.block.BankGuhBlock;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.bank.BankAdressen;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.menu.BankGuhMenu;
import nl.juiced.guhs.registry.ModBlockEntities;
import nl.juiced.guhs.registry.ModDataComponents;
import nl.juiced.guhs.storage.BankContents;
import nl.juiced.guhs.storage.BankHandler;
import nl.juiced.guhs.storage.BankStorage;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The Bank Guh: a sitting guh that keeps items in its stomach ({@link BankStorage}): of every kind at most
 * {@link BankStorage#CAP}, or without a limit once it got the upgrade (bbq2).
 * <p>
 * Three things travel with the bank when it is broken and placed again (saved here, copied onto the item by the loot
 * table, read back from the item): the stomach, the upgrade, and the bank's id. The id is what a Hapluikje is linked to;
 * {@link BankAdressen} keeps where the bank with that id stands right now.
 */
public class BankGuhBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh_sitting.idle");

    private final BankStorage storage = new BankStorage(this::veranderd);
    /** What pipes, hoppers and Hapluikjes get (loaned things never go in). */
    private final BankHandler handler = new BankHandler(storage, soort -> soort.test(Features::isLoaned));
    /** This bank's id; given the first time the bank stands in a world (banks from before bbq2 get one then). */
    @Nullable
    private UUID bankId;
    /** Set when the chunk unloads: the {@link #setRemoved} that follows is then no removal of the block. */
    private boolean ontladen;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public BankGuhBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BANK_GUH.get(), pos, state);
    }

    public BankStorage getStorage() {
        return storage;
    }

    /** The item capability: in up to the cap, out only when upgraded. The same object every time. */
    public BankHandler handler() {
        return handler;
    }

    public boolean isUpgraded() {
        return storage.isUpgraded();
    }

    /** The stomach or the upgrade changed: save, and let the block state show the upgrade (the client lets it sparkle). */
    private void veranderd() {
        setChanged();
        toonUpgrade();
    }

    private void toonUpgrade() {
        BlockState state = getBlockState();
        if (level != null && !level.isClientSide() && !isRemoved() && state.hasProperty(BankGuhBlock.OPGEVOERD)
                && state.getValue(BankGuhBlock.OPGEVOERD) != storage.isUpgraded()) {
            level.setBlock(worldPosition, state.setValue(BankGuhBlock.OPGEVOERD, storage.isUpgraded()), Block.UPDATE_CLIENTS);
        }
    }

    // --- the id and the address book ---

    /** This bank's id (never null on the server once the bank is in a world). */
    public UUID bankId() {
        if (bankId == null) {
            bankId = UUID.randomUUID();
            setChanged();
        }
        return bankId;
    }

    /** Placed, or its chunk loaded (NeoForge calls this at the start of the next tick, after the item's components are in). */
    @Override
    public void onLoad() {
        super.onLoad();
        meld();
    }

    /**
     * Writes this bank's address in the book ({@link BankAdressen}); gives it an id first when it has none. Called when
     * the bank appears in a world and whenever something is about to use the id (a Banksleutel).
     */
    public void meld() {
        if (!(level instanceof ServerLevel server) || isRemoved()) {
            return;
        }
        BankAdressen boek = BankAdressen.van(server.getServer());
        GlobalPos hier = GlobalPos.of(server.dimension(), worldPosition);
        if (bankId != null) {
            // a copy of a bank that already stands somewhere (creative pick block with data, /clone): the newcomer gets
            // an id of its own, so the luikjes of the first one keep pointing at the first one
            GlobalPos ander = boek.plek(bankId);
            if (ander != null && !ander.equals(hier)) {
                ServerLevel daar = server.getServer().getLevel(ander.dimension());
                if (daar != null && daar.isLoaded(ander.pos()) && daar.getBlockEntity(ander.pos()) instanceof BankGuhBlockEntity eerste
                        && eerste != this && bankId.equals(eerste.bankId)) {
                    bankId = UUID.randomUUID();
                    setChanged();
                }
            }
        }
        boek.zet(bankId(), hier);
        toonUpgrade();
    }

    /**
     * The bank's chunk unloads: the address book remembers what it holds, so a Hapluikje can say what still fits without
     * loading this chunk again ({@link BankAdressen.Schaduw}).
     */
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        ontladen = true;
        if (level instanceof ServerLevel server && bankId != null) {
            BankAdressen.van(server.getServer()).onthoud(bankId, GlobalPos.of(server.dimension(), worldPosition), storage);
        }
    }

    /** Removed while its chunk stays loaded (broken or replaced, also without side effects): a shadow of it would lie. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!ontladen && level instanceof ServerLevel server && bankId != null) {
            BankAdressen.van(server.getServer()).vergeet(bankId);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        ontladen = false;
    }

    /** The block is really gone (broken, replaced): this bank stands nowhere until it is placed again. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && bankId != null) {
            BankAdressen.van(server.getServer()).wis(bankId, GlobalPos.of(server.dimension(), pos));
        }
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

    // --- saving (the stomach, the upgrade and the id are also copied onto the item when the block is broken) ---

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        tag.read("Stomach", BankContents.CODEC).ifPresent(storage::load);
        storage.setUpgraded(tag.getBooleanOr("Opgevoerd", false));
        bankId = tag.read("BankId", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.store("Stomach", BankContents.CODEC, storage.snapshot());
        tag.putBoolean("Opgevoerd", storage.isUpgraded());
        tag.storeNullable("BankId", UUIDUtil.CODEC, bankId);
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter input) {
        super.applyImplicitComponents(input);
        storage.load(input.getOrDefault(ModDataComponents.BANK_CONTENTS.get(), BankContents.EMPTY));
        storage.setUpgraded(input.getOrDefault(BankFeature.BANK_OPGEVOERD.get(), false));
        UUID id = input.get(BankFeature.BANK_ID.get());
        if (id != null) {
            bankId = id;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        BankContents contents = storage.snapshot();
        if (!contents.isEmpty()) {
            components.set(ModDataComponents.BANK_CONTENTS.get(), contents);
        }
        if (storage.isUpgraded()) {
            components.set(BankFeature.BANK_OPGEVOERD.get(), true);
        }
        if (bankId != null) {
            components.set(BankFeature.BANK_ID.get(), bankId);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput tag) {
        tag.discard("Stomach");
        tag.discard("Opgevoerd");
        tag.discard("BankId");
    }

    // --- GeckoLib ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
