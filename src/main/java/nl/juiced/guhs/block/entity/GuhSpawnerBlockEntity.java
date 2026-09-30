package nl.juiced.guhs.block.entity;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.registry.ModBlockEntities;
import nl.juiced.guhs.registry.ModEntities;

/** A vanilla-style mob spawner that always spawns guhs. */
public class GuhSpawnerBlockEntity extends BlockEntity {
    private final BaseSpawner spawner = new BaseSpawner() {
        @Override
        public void broadcastEvent(Level level, BlockPos pos, int eventId) {
            level.blockEvent(pos, getBlockState().getBlock(), eventId, 0);
        }

        @Override
        public void setNextSpawnData(@Nullable Level level, BlockPos pos, SpawnData spawnData) {
            super.setNextSpawnData(level, pos, spawnData);
            if (level != null) {
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 4);
            }
        }

        @Override
        public Either<BlockEntity, Entity> getOwner() {
            return Either.left(GuhSpawnerBlockEntity.this);
        }
    };

    public GuhSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GUH_SPAWNER.get(), pos, state);
        spawner.setEntityId(ModEntities.GUH.get(), null, RandomSource.create(), pos);
    }

    public BaseSpawner getSpawner() {
        return spawner;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, GuhSpawnerBlockEntity be) {
        be.spawner.clientTick(level, pos);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GuhSpawnerBlockEntity be) {
        be.spawner.serverTick((ServerLevel) level, pos);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spawner.load(level, worldPosition, tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        spawner.save(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove("SpawnPotentials");
        return tag;
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        return spawner.onEventTriggered(level, id) || super.triggerEvent(id, type);
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }
}
