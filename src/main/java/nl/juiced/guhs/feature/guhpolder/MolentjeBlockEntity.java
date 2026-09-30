package nl.juiced.guhs.feature.guhpolder;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.knus.KnusTags;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The guh-molentje's inside: a hopper of knabbelgraan and a little sack of knabbelmeel (at most {@link #MAX} each). One
 * graan becomes one meel every {@link #maalTijd} ticks: {@link #MAAL_TICKS} in calm weather, faster when it snows or
 * rains on the molentje (the wind!), fastest in a thunderstorm. The client turns the sails at the matching speed.
 */
public class MolentjeBlockEntity extends BlockEntity {
    public static final int MAX = 64;
    /** Ticks per knabbelmeel: calm, snow/rain, storm. */
    public static final int[] MAAL_TICKS = {80, 50, 30};
    /** Degrees per tick the sails turn: calm, snow/rain, storm. */
    public static final float[] DRAAI = {2.5f, 6f, 11f};

    private ItemStack graan = ItemStack.EMPTY;
    private ItemStack meel = ItemStack.EMPTY;
    private int voortgang;
    /** (client) the sails' angle, now and a tick ago. */
    private float hoek, oHoek;

    public MolentjeBlockEntity(BlockPos pos, BlockState state) {
        super(GuhpolderFeature.GUH_MOLENTJE_BE.get(), pos, state);
        // (every molentje starts at its own angle, so a row of them doesn't turn in lockstep)
        hoek = oHoek = Math.floorMod(pos.hashCode(), 90);
    }

    // --- the weather ----------------------------------------------------------------------------------------------------------

    /** 0 = calm, 1 = snow or rain on the molentje, 2 = thunderstorm. */
    public static int weer(Level level, BlockPos pos) {
        if (!level.isRaining() || !level.canSeeSky(pos.above())) {
            return 0;
        }
        return level.isThundering() ? 2 : 1;
    }

    public static int maalTijd(int weer) {
        return MAAL_TICKS[Math.max(0, Math.min(2, weer))];
    }

    // --- contents -------------------------------------------------------------------------------------------------------------

    public ItemStack graan() {
        return graan;
    }

    public ItemStack meel() {
        return meel;
    }

    public int voortgang() {
        return voortgang;
    }

    /** Pours (part of) a stack of knabbelgraan in; returns how many went in. */
    public int stort(ItemStack stack) {
        if (!stack.is(KnusTags.KNABBELGRAAN)) {
            return 0;
        }
        if (!graan.isEmpty() && !ItemStack.isSameItemSameComponents(graan, stack)) {
            return 0;
        }
        int n = Math.min(stack.getCount(), MAX - graan.getCount());
        if (n <= 0) {
            return 0;
        }
        if (graan.isEmpty()) {
            graan = stack.copyWithCount(n);
        } else {
            graan.grow(n);
        }
        stack.shrink(n);
        changed();
        return n;
    }

    /** Takes all the knabbelmeel out. */
    public ItemStack neemMeel() {
        ItemStack out = meel;
        meel = ItemStack.EMPTY;
        if (!out.isEmpty()) {
            changed();
        }
        return out;
    }

    /** Takes the knabbelgraan back out. */
    public ItemStack neemGraan() {
        ItemStack out = graan;
        graan = ItemStack.EMPTY;
        voortgang = 0;
        if (!out.isEmpty()) {
            changed();
        }
        return out;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // --- ticking --------------------------------------------------------------------------------------------------------------

    void serverTick() {
        if (graan.isEmpty() || meel.getCount() >= MAX || !(level instanceof ServerLevel server)) {
            voortgang = 0;
            return;
        }
        if (++voortgang >= maalTijd(weer(level, worldPosition))) {
            maal(server);
        }
    }

    /** One knabbelgraan becomes one knabbelmeel (with a puff of flour). Public for the tests. */
    public void maal(ServerLevel server) {
        if (graan.isEmpty() || meel.getCount() >= MAX) {
            return;
        }
        voortgang = 0;
        graan.shrink(1);
        if (graan.isEmpty()) {
            graan = ItemStack.EMPTY;
        }
        if (meel.isEmpty()) {
            meel = new ItemStack(GuhpolderFeature.KNABBELMEEL.get());
        } else {
            meel.grow(1);
        }
        server.sendParticles(BakkerijFeature.MEELSTOFJE.get(), worldPosition.getX() + 0.5, worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5,
                4, 0.25, 0.1, 0.25, 0.01);
        server.playSound(null, worldPosition, GuhpolderFeature.MOLENTJE_MAAL.get(), SoundSource.BLOCKS, 0.35f, 0.9f + server.getRandom().nextFloat() * 0.2f);
        changed();
    }

    void clientTick() {
        oHoek = hoek;
        hoek = (hoek + DRAAI[weer(level, worldPosition)]) % 360f;
        if (!graan.isEmpty() && level.getRandom().nextInt(14) == 0) {
            level.addParticle(BakkerijFeature.MEELSTOFJE.get(), worldPosition.getX() + 0.3 + level.getRandom().nextDouble() * 0.4,
                    worldPosition.getY() + 0.15, worldPosition.getZ() + 0.3 + level.getRandom().nextDouble() * 0.4, 0, 0.01, 0);
        }
    }

    /** (client) The sails' angle in degrees. */
    public float hoek(float partialTick) {
        float d = hoek - oHoek;
        if (d < 0) {
            d += 360f;
        }
        return oHoek + d * partialTick;
    }

    /** Broken: the graan and the meel fall out (1.0.0: MolentjeBlock#onRemove). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), graan.copy());
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), meel.copy());
        }
    }

    // --- saving and syncing ---------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        graan = tag.read("Graan", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        meel = tag.read("Meel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        voortgang = tag.getIntOr("Voortgang", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (!graan.isEmpty()) {
            tag.store("Graan", ItemStack.CODEC, graan);
        }
        if (!meel.isEmpty()) {
            tag.store("Meel", ItemStack.CODEC, meel);
        }
        tag.putInt("Voortgang", voortgang);
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

    // --- hoppers ----------------------------------------------------------------------------------------------------------------

    /**
     * Slot 0 = the graan (in only, not from below), slot 1 = the meel (out only).
     * <p>1.1.0: NeoForge 26.1 item capability = a transactional {@link ResourceHandler}; the journal puts graan and meel
     * back when a hopper's transaction is aborted, and sends the update once it is committed.
     */
    public ResourceHandler<ItemResource> handler(@Nullable Direction side) {
        return new ResourceHandler<>() {
            @Override
            public int size() {
                return 2;
            }

            @Override
            public ItemResource getResource(int index) {
                return ItemResource.of(index == 0 ? graan : index == 1 ? meel : ItemStack.EMPTY);
            }

            @Override
            public long getAmountAsLong(int index) {
                return index == 0 ? graan.getCount() : index == 1 ? meel.getCount() : 0;
            }

            @Override
            public long getCapacityAsLong(int index, ItemResource resource) {
                return MAX;
            }

            @Override
            public boolean isValid(int index, ItemResource resource) {
                return index == 0 && resource.test(s -> s.is(KnusTags.KNABBELGRAAN));
            }

            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (index != 0 || side == Direction.DOWN || resource.isEmpty() || amount <= 0 || !isValid(index, resource)) {
                    return 0;
                }
                if (!graan.isEmpty() && !resource.matches(graan)) {
                    return 0;
                }
                int n = Math.min(amount, MAX - graan.getCount());
                if (n <= 0) {
                    return 0;
                }
                journal.updateSnapshots(transaction);
                if (graan.isEmpty()) {
                    graan = resource.toStack(n);
                } else {
                    graan.grow(n);
                }
                return n;
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (index != 1 || meel.isEmpty() || amount <= 0 || !resource.matches(meel)) {
                    return 0;
                }
                int n = Math.min(amount, meel.getCount());
                journal.updateSnapshots(transaction);
                meel.shrink(n);
                if (meel.isEmpty()) {
                    meel = ItemStack.EMPTY;
                }
                return n;
            }
        };
    }

    private final Journal journal = new Journal();

    private final class Journal extends SnapshotJournal<ItemStack[]> {
        @Override
        protected ItemStack[] createSnapshot() {
            return new ItemStack[] {graan.copy(), meel.copy()};
        }

        @Override
        protected void revertToSnapshot(ItemStack[] snapshot) {
            graan = snapshot[0];
            meel = snapshot[1];
        }

        @Override
        protected void onRootCommit(ItemStack[] originalState) {
            changed();
        }
    }
}
