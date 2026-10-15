package nl.juiced.guhs.feature.vadskracht;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The block entity of a guh machine: a consumer of vadskracht with an inventory, an owner and a face. A subclass only says
 * what the machine does:
 * <pre>
 * public class MaalBlockEntity extends MachineBlockEntity {
 *     public MaalBlockEntity(BlockPos pos, BlockState state) { super(MY_BE.get(), pos, state, VadsGetallen.MOLEN, 2); }
 *     protected boolean kanWerken() { return !Kisten.neem(vakken(), s -> true, 1, true).isEmpty() ... }
 *     protected boolean isVol()     { return the output slot is full; }
 *     protected void werk()         { one tick of work; }
 * }
 * </pre>
 * Every server tick: while the machine has vadskracht ({@link #heeftKracht}), is switched on ({@link #aan}) and
 * {@link #kanWerken}, {@link #werk} runs. The face ({@link MachineBlock#SNOET}) follows by itself: asleep without
 * vadskracht (or switched off), surprised when {@link #isVol}, happy otherwise. {@link #bezig} (synced) is true while it
 * really works, for animations. The demand is NOMINAL: the machine asks its {@code vraag} whenever it is switched on, also
 * when it has nothing to do.
 * <p>
 * The inventory ({@link #vakken}) is saved and dropped when the machine is removed; {@link #handler} is what pipes and
 * hoppers get (in only into {@link #isInvoer} slots, out only of {@link #isUitvoer} slots).
 */
public abstract class MachineBlockEntity extends BlockEntity implements VadsVerbruiker {
    /** {@link #bezig} stays true this many ticks after the last tick of work (so an animation does not stutter). */
    private static final int NALOOP = 10;

    private final int vraag;
    private final Vakken vakken;
    @Nullable
    private UUID eigenaar;
    private boolean kracht;
    private boolean bezig;
    private int naloop;
    private boolean wasAan = true;
    private boolean gemeld;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int vraag, int vakken) {
        super(type, pos, state);
        this.vraag = vraag;
        this.vakken = new Vakken(vakken);
    }

    // =====================================================================================================================
    // what a machine fills in
    // =====================================================================================================================

    /** Is there something to do right now (something in, room for what comes out)? */
    protected abstract boolean kanWerken();

    /** Is the output blocked (then the face is surprised)? */
    protected abstract boolean isVol();

    /** One server tick of work (only called while {@link #heeftKracht}, {@link #aan} and {@link #kanWerken}). */
    protected abstract void werk();

    /** Extra data (the items, the owner and the face are handled). */
    protected void opslaan(ValueOutput uit) {
    }

    protected void laden(ValueInput in) {
    }

    /** Switched on? (default: always; override for an off switch. Call {@link #meld} when the answer changes.) */
    protected boolean aan() {
        return true;
    }

    /** May a pipe or hopper put something into this slot? Default: the first half of the slots. */
    protected boolean isInvoer(int vak) {
        return vak < (vakken.size() + 1) / 2;
    }

    /** May a pipe or hopper take from this slot? Default: the second half of the slots. */
    protected boolean isUitvoer(int vak) {
        return vak >= (vakken.size() + 1) / 2;
    }

    /** Does this item belong in this slot at all (for pipes, hoppers and {@link Kisten#stop} on {@link #handler})? */
    protected boolean past(int vak, ItemResource wat) {
        return true;
    }

    /** Called every client tick (animations, particles). */
    protected void clientTick() {
    }

    /** Called when the inventory changed (after {@link #setChanged}). */
    protected void vakkenVeranderd() {
    }

    // =====================================================================================================================
    // state
    // =====================================================================================================================

    /** Does the machine's net run (the answer of the net's last evaluation)? */
    public final boolean heeftKracht() {
        return kracht;
    }

    public final Snoet snoet() {
        return getBlockState().hasProperty(MachineBlock.SNOET) ? getBlockState().getValue(MachineBlock.SNOET) : Snoet.SLAAPT;
    }

    /** Is it really working right now (synced to the client: for animations)? */
    public final boolean bezig() {
        return bezig;
    }

    /** The inventory (saved, marks the block entity dirty, updates comparators). Slots 0..n-1. */
    public final ItemStacksResourceHandler vakken() {
        return vakken;
    }

    /** Who placed it (null: placed by a structure or a command). For {@code Bescherming.magWijzigen}. */
    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    public void zetEigenaar(@Nullable UUID wie) {
        eigenaar = wie;
        setChanged();
    }

    /** Tell the net that what this machine asks changed (it was switched on or off). */
    protected final void meld() {
        VadsKracht.veranderd(level, worldPosition);
    }

    // =====================================================================================================================
    // vadskracht
    // =====================================================================================================================

    @Override
    public BlockPos vadsPlek() {
        return worldPosition;
    }

    @Override
    public int vadsVraag() {
        return aan() ? vraag : 0;
    }

    @Override
    public void vadsStroom(boolean aan) {
        kracht = aan;
    }

    // =====================================================================================================================
    // ticking
    // =====================================================================================================================

    final void serverTick() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (!gemeld) {
            gemeld = true;   // (a machine put there without a block update still finds its net)
            meld();
        }
        boolean isAan = aan();
        if (isAan != wasAan) {
            wasAan = isAan;
            meld();
        }
        boolean werkt = kracht && isAan && kanWerken();
        if (werkt) {
            werk();
            naloop = NALOOP;
        } else if (naloop > 0) {
            naloop--;
        }
        boolean nu = naloop > 0;
        if (nu != bezig) {
            bezig = nu;
            sync();
        }
        BlockState state = getBlockState();
        if (state.hasProperty(MachineBlock.SNOET)) {
            Snoet snoet = Snoet.van(kracht && isAan, isVol());
            if (state.getValue(MachineBlock.SNOET) != snoet) {
                level.setBlock(worldPosition, state.setValue(MachineBlock.SNOET, snoet), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Sends the block entity's data to the clients that see it. */
    protected final void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // =====================================================================================================================
    // removal, saving, syncing
    // =====================================================================================================================

    /** Removed (however): the items fall out, and the parts of a machine bigger than one block go with it. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) {
            return;
        }
        for (ItemStack stack : vakken.copyToList()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        }
        if (state.getBlock() instanceof MachineBlock machine) {
            Meerblok.ruimOp(level, pos, state, machine.deel());
        }
    }

    @Override
    protected final void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("Vakken").ifPresent(vakken::deserialize);   // (absent in what a client gets: getUpdateTag)
        eigenaar = in.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        bezig = in.getBooleanOr("Bezig", false);
        laden(in);
    }

    @Override
    protected final void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        vakken.serialize(uit.child("Vakken"));
        uit.storeNullable("Eigenaar", UUIDUtil.CODEC, eigenaar);
        uit.putBoolean("Bezig", bezig);
        opslaan(uit);
    }

    /**
     * What the clients get: the saved data WITHOUT the items. Nothing on a client looks into a machine's slots (a screen
     * gets them through its menu, a renderer draws from {@link #bezig} and the block state), and a machine is synced every
     * time it starts or stops working: nine stacks with their components (a Neerzetter full of filled shulker boxes) to
     * everybody around, each time, is a lot of nothing. A subclass that needs more removes or adds keys on top of this.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.remove("Vakken");
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // =====================================================================================================================
    // the inventory
    // =====================================================================================================================

    private final class Vakken extends ItemStacksResourceHandler {
        Vakken(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
            vakkenVeranderd();
        }
    }

    /**
     * What pipes, hoppers and chore guhs get (register it for {@code Capabilities.Item.BLOCK}, see
     * {@link VadskrachtFeature#machineCapabilities}): in only into the {@link #isInvoer} slots (and only what {@link #past}),
     * out only of the {@link #isUitvoer} slots. The machine itself works on {@link #vakken} directly.
     */
    public ResourceHandler<ItemResource> handler(@Nullable Direction kant) {
        return new ResourceHandler<>() {
            @Override
            public int size() {
                return vakken.size();
            }

            @Override
            public ItemResource getResource(int index) {
                return vakken.getResource(index);
            }

            @Override
            public long getAmountAsLong(int index) {
                return vakken.getAmountAsLong(index);
            }

            @Override
            public long getCapacityAsLong(int index, ItemResource resource) {
                return vakken.getCapacityAsLong(index, resource);
            }

            @Override
            public boolean isValid(int index, ItemResource resource) {
                return isInvoer(index) && past(index, resource);
            }

            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return isInvoer(index) && past(index, resource) ? vakken.insert(index, resource, amount, transaction) : 0;
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return isUitvoer(index) ? vakken.extract(index, resource, amount, transaction) : 0;
            }
        };
    }
}
