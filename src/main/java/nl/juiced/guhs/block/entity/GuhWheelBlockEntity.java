package nl.juiced.guhs.block.entity;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.registry.ModBlockEntities;

/**
 * Holds the guh that's running in the wheel (stored as entity data, like bees in a beehive),
 * plus client-side animation state for the spinning wheel.
 */
public class GuhWheelBlockEntity extends BlockEntity {
    @Nullable
    private CompoundTag guhData;

    // client-side only: a copy of the guh for rendering, and the wheel angle
    @Nullable
    private GuhEntity displayGuh;
    private float spin;
    private float spinO;

    public GuhWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GUH_WHEEL.get(), pos, state);
    }

    public boolean hasGuh() {
        return guhData != null;
    }

    @Nullable
    public UUID getGuhOwner() {
        return guhData != null && guhData.hasUUID("Owner") ? guhData.getUUID("Owner") : null;
    }

    /** Puts a (picked-up) guh into the wheel. */
    public void insert(CompoundTag guh) {
        guhData = guh.copy();
        sync();
    }

    /** Takes the guh out again, as picked-up guh data (or null if the wheel was empty). */
    @Nullable
    public CompoundTag takeOut() {
        CompoundTag tag = guhData;
        guhData = null;
        sync();
        return tag;
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- client animation ---

    public static void clientTick(Level level, BlockPos pos, BlockState state, GuhWheelBlockEntity be) {
        be.spinO = be.spin;
        if (be.guhData == null) {
            be.displayGuh = null;
            return;
        }
        be.spin += 9f;
        GuhEntity guh = be.getDisplayGuh();
        if (guh != null) {
            guh.tickCount++; // drives the GeckoLib animation clock
            guh.walkAnimation.update(1.0f, 0.4f); // "moving" -> walk animation
        }
    }

    public float getSpin(float partialTick) {
        return spinO + (spin - spinO) * partialTick;
    }

    @Nullable
    public GuhEntity getDisplayGuh() {
        if (displayGuh == null && guhData != null && level != null) {
            displayGuh = EntityType.create(guhData, level).filter(e -> e instanceof GuhEntity).map(e -> (GuhEntity) e).orElse(null);
            if (displayGuh != null) {
                displayGuh.setInSittingPose(false);
                displayGuh.setRunningInWheel(true);
            }
        }
        return displayGuh;
    }

    // --- saving & syncing ---

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag newData = tag.contains("Guh") ? tag.getCompound("Guh") : null;
        if (newData == null || !newData.equals(guhData)) {
            displayGuh = null;
        }
        guhData = newData;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // always write something: an empty update would be ignored by the client, and the wheel would keep
        // showing a guh that was already taken out
        tag.putBoolean("HasGuh", guhData != null);
        if (guhData != null) {
            tag.put("Guh", guhData);
        }
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
