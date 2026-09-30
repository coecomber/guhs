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
import net.neoforged.neoforge.items.IItemHandler;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.knus.KnusTags;

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
        if (level != null && !level.isClientSide) {
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
        server.playSound(null, worldPosition, GuhpolderFeature.MOLENTJE_MAAL.get(), SoundSource.BLOCKS, 0.35f, 0.9f + server.random.nextFloat() * 0.2f);
        changed();
    }

    void clientTick() {
        oHoek = hoek;
        hoek = (hoek + DRAAI[weer(level, worldPosition)]) % 360f;
        if (!graan.isEmpty() && level.random.nextInt(14) == 0) {
            level.addParticle(BakkerijFeature.MEELSTOFJE.get(), worldPosition.getX() + 0.3 + level.random.nextDouble() * 0.4,
                    worldPosition.getY() + 0.15, worldPosition.getZ() + 0.3 + level.random.nextDouble() * 0.4, 0, 0.01, 0);
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

    // --- saving and syncing ---------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        graan = ItemStack.parseOptional(registries, tag.getCompound("Graan"));
        meel = ItemStack.parseOptional(registries, tag.getCompound("Meel"));
        voortgang = tag.getInt("Voortgang");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!graan.isEmpty()) {
            tag.put("Graan", graan.save(registries));
        }
        if (!meel.isEmpty()) {
            tag.put("Meel", meel.save(registries));
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

    /** Slot 0 = the graan (in only, not from below), slot 1 = the meel (out only). */
    public IItemHandler handler(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 2;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot == 0 ? graan : slot == 1 ? meel : ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot != 0 || side == Direction.DOWN || !isItemValid(slot, stack)) {
                    return stack;
                }
                if (!graan.isEmpty() && !ItemStack.isSameItemSameComponents(graan, stack)) {
                    return stack;
                }
                int n = Math.min(stack.getCount(), MAX - graan.getCount());
                if (n <= 0) {
                    return stack;
                }
                if (!simulate) {
                    stort(stack.copyWithCount(n));
                }
                return stack.copyWithCount(stack.getCount() - n);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != 1 || meel.isEmpty() || amount <= 0) {
                    return ItemStack.EMPTY;
                }
                int n = Math.min(amount, meel.getCount());
                ItemStack out = meel.copyWithCount(n);
                if (!simulate) {
                    meel.shrink(n);
                    if (meel.isEmpty()) {
                        meel = ItemStack.EMPTY;
                    }
                    changed();
                }
                return out;
            }

            @Override
            public int getSlotLimit(int slot) {
                return MAX;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && stack.is(KnusTags.KNABBELGRAAN);
            }
        };
    }
}
