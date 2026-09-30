package nl.juiced.guhs.feature.speelgoed;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The swinging of a wip or schommel: an angle that swings like a pendulum, {@code hoek(t) = A(t) * sin(omega * t)},
 * where the amplitude A dies down slowly after the last push ({@code A(t) = amp * exp(-(t - sinds) / verval)}). The same
 * formula runs on server and client (synced amp and sinds), so the seats and the renderer agree without packets per tick.
 */
public class ToestelBlockEntity extends BlockEntity {
    private float amp;
    private long sinds;

    public ToestelBlockEntity(BlockPos pos, BlockState state) {
        super(SpeelgoedFeature.TOESTEL_BE.get(), pos, state);
    }

    @Nullable
    private Schommelend soort() {
        return getBlockState().getBlock() instanceof Schommelend s ? s : null;
    }

    /** The amplitude now (radians). */
    public float amplitude(float tijd) {
        Schommelend s = soort();
        if (s == null || amp <= 0) {
            return 0;
        }
        return amp * (float) Math.exp(-Math.max(0, tijd - sinds) / s.verval());
    }

    /** The angle now (radians). tijd = game time + partial tick. */
    public float hoek(float tijd) {
        Schommelend s = soort();
        if (s == null) {
            return 0;
        }
        float fase = (worldPosition.getX() * 7 + worldPosition.getZ() * 13) % 64;   // (not every swing in step)
        return amplitude(tijd) * Mth.sin((tijd + fase) * s.omega());
    }

    /** A push: more swing (up to the maximum). Returns the new amplitude. */
    public float duw(float extra) {
        Schommelend s = soort();
        if (level == null || s == null) {
            return 0;
        }
        long nu = level.getGameTime();
        amp = Math.min(s.maxHoek(), amplitude(nu) + extra);
        sinds = nu;
        sync();
        return amp;
    }

    /** Keeps it swinging at least this much (a guh swinging its legs). */
    public void minstens(float basis) {
        if (level == null) {
            return;
        }
        long nu = level.getGameTime();
        if (amplitude(nu) < basis - 0.03f) {
            amp = basis;
            sinds = nu;
            sync();
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putFloat("Amp", amp);
        tag.putLong("Sinds", sinds);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        amp = tag.getFloatOr("Amp", 0.0F);
        sinds = tag.getLongOr("Sinds", 0L);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    /** 1.1.0 (onRemove is gone): the toy was broken or replaced: remove its parts, riders off. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide() && state.getBlock() instanceof ToestelBlock toestel) {
            toestel.verwijderd(level, pos, state);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** A toy that swings (wip, schommel). */
    public interface Schommelend {
        /** Radians per tick. */
        float omega();

        /** Ticks for the swing to die down to 1/e. */
        float verval();

        float maxHoek();
    }
}
