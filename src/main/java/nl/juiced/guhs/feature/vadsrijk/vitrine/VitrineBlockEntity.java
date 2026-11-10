package nl.juiced.guhs.feature.vadsrijk.vitrine;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;

/**
 * Y-VITRINE: what a vitrine knows: its owner (uuid) and the last snapshot of the owner's Vadsrijk character ({@link Vitrine#foto}),
 * plus the zone gate it was last sent with ({@code open}). Saved with the chunk (= the offline cache) and synced to the client
 * (vanilla block entity update). The server re-reads the owner every {@link Vitrine#VERVERS} ticks while he is online (staggered
 * per position); a change is sent, nothing else is.
 */
public class VitrineBlockEntity extends BlockEntity {
    /** The loaded vitrines on the server (for the dev "ververs" command). */
    static final Set<VitrineBlockEntity> GELADEN = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));

    @Nullable
    private UUID eigenaar;
    private CompoundTag foto = new CompoundTag();
    /** The zone gate this vitrine was last synced with (the client hides closed zones by it). */
    private int open = 6;

    public VitrineBlockEntity(BlockPos pos, BlockState state) {
        super(Vitrine.BE.get(), pos, state);
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    /** The snapshot (never null; empty = no owner yet). */
    public CompoundTag foto() {
        return foto;
    }

    public int open() {
        return open;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide()) {
            GELADEN.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        GELADEN.remove(this);
    }

    /** The placer becomes the owner; the first snapshot right away. */
    public void zetEigenaar(ServerPlayer p) {
        eigenaar = p.getUUID();
        foto = Vitrine.foto(p);
        open = ZonePoort.open();
        sync();
    }

    /**
     * Re-reads the owner when he is online (force: also when nothing changed, then it is still re-sent). Returns true when the
     * snapshot or the gate changed.
     */
    public boolean ververs(boolean force) {
        if (!(level instanceof ServerLevel sl)) {
            return false;
        }
        boolean anders = false;
        ServerPlayer p = Vitrine.online(sl.getServer(), eigenaar);
        if (p != null) {
            CompoundTag nieuw = Vitrine.foto(p);
            if (!nieuw.equals(foto)) {
                foto = nieuw;
                anders = true;
            }
        }
        int o = ZonePoort.open();
        if (o != open) {
            open = o;
            anders = true;
        }
        if (anders || force) {
            sync();
        }
        return anders;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VitrineBlockEntity be) {
        if ((level.getGameTime() + (pos.asLong() & 0x7F)) % Vitrine.VERVERS == 0) {
            be.ververs(false);
        }
    }

    /** A click: the summary in the chat (the owner also refreshes it first). */
    public void klik(ServerPlayer p) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        if (eigenaar == null) {
            zetEigenaar(p);   // (a vitrine without owner, e.g. from /setblock: the first one to click it claims it)
        } else {
            ververs(false);
        }
        boolean online = Vitrine.online(sl.getServer(), eigenaar) != null;
        for (Component c : Vitrine.samenvatting(foto, online, open)) {
            p.sendSystemMessage(c);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (eigenaar != null) {
            out.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }
        out.store("Foto", CompoundTag.CODEC, foto.copy());
        out.putInt("Open", open);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        eigenaar = in.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        foto = in.read("Foto", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        open = in.getIntOr("Open", 6);
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
}
