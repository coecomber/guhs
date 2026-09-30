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

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
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
        return guhData != null && guhData.read("Owner", UUIDUtil.CODEC).isPresent() ? guhData.read("Owner", UUIDUtil.CODEC).orElseThrow() : null;
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
            guh.walkAnimation.update(1.0f, 0.4f, guh.isBaby() ? 3.0f : 1.0f); // "moving" -> walk animation
        }
    }

    public float getSpin(float partialTick) {
        return spinO + (spin - spinO) * partialTick;
    }

    @Nullable
    public GuhEntity getDisplayGuh() {
        if (displayGuh == null && guhData != null && level != null) {
            displayGuh = EntityType.create(nl.juiced.guhs.storage.Nbt.input(level.registryAccess(), guhData), level, net.minecraft.world.entity.EntitySpawnReason.LOAD).filter(e -> e instanceof GuhEntity).map(e -> (GuhEntity) e).orElse(null);
            if (displayGuh != null) {
                displayGuh.setInSittingPose(false);
                displayGuh.setRunningInWheel(true);
            }
        }
        return displayGuh;
    }

    // --- saving & syncing ---

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        CompoundTag newData = tag.read("Guh", CompoundTag.CODEC).orElse(null);
        if (newData == null || !newData.equals(guhData)) {
            displayGuh = null;
        }
        guhData = newData;
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        // always write something: an empty update would be ignored by the client, and the wheel would keep
        // showing a guh that was already taken out
        tag.putBoolean("HasGuh", guhData != null);
        if (guhData != null) {
            tag.store("Guh", CompoundTag.CODEC, guhData);
        }
    }

    /**
     * Breaking the wheel (however it goes) drops the guh that was in it (as a picked-up guh item) and removes the
     * wheel's other parts. 26.1: was GuhWheelBlock#onRemove.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) {
            return;
        }
        CompoundTag guh = takeOut();
        if (guh != null) {
            net.minecraft.world.item.ItemStack item = nl.juiced.guhs.item.PickedUpGuhItem.of(guh);
            nl.juiced.guhs.feature.band.GuhVolger.item(item, nl.juiced.guhs.feature.band.PlekSoort.ITEM_GROND, level.dimension(), pos, "",
                    level.getGameTime());   // 2.10: "waar is mijn guh": the wheel broke, it lies on the ground
            net.minecraft.world.level.block.Block.popResource(level, pos, item);
        }
        if (state.hasProperty(nl.juiced.guhs.block.GuhWheelBlock.FACING)) {
            for (BlockPos part : nl.juiced.guhs.block.GuhWheelBlock.partPositions(pos, state.getValue(nl.juiced.guhs.block.GuhWheelBlock.FACING))) {
                if (level.getBlockState(part).is(nl.juiced.guhs.registry.ModBlocks.GUH_WHEEL_PART.get())) {
                    level.setBlock(part, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
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
