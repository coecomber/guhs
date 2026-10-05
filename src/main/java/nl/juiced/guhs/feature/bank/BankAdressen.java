package nl.juiced.guhs.feature.bank;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * bbq2: where every placed Bank Guh stands (SavedData {@code guhs:bank_adressen} in the overworld): bank id -> dimension
 * + position. A Hapluikje is linked to a bank's ID, not to a spot, so the bank may be picked up and put down somewhere
 * else, in any dimension, and its luikjes find it again; while it is in somebody's pocket it has no address and the
 * luikjes refuse.
 * <p>
 * A Bank Guh writes its address when it is placed or its chunk loads ({@link BankGuhBlockEntity#onLoad}) and wipes it
 * when the block is removed. The book may still be wrong (a chunk rolled back, a block replaced without side effects):
 * {@link #zoek} always looks at the block itself and tidies up a stale line.
 */
public final class BankAdressen extends SavedData {
    private static final Codec<BankAdressen> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC)
            .xmap(BankAdressen::new, a -> a.adressen);
    public static final SavedDataType<BankAdressen> TYPE = GuhSavedData.type("bank_adressen", BankAdressen::new, CODEC);

    private final Map<UUID, GlobalPos> adressen = new HashMap<>();

    public BankAdressen() {
    }

    private BankAdressen(Map<UUID, GlobalPos> adressen) {
        this.adressen.putAll(adressen);
    }

    public static BankAdressen van(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The address in the book (not checked), or null: this bank is not placed anywhere. */
    @Nullable
    public GlobalPos plek(UUID bank) {
        return adressen.get(bank);
    }

    public void zet(UUID bank, GlobalPos plek) {
        if (!plek.equals(adressen.put(bank, plek))) {
            setDirty();
        }
    }

    /** Wipes the address, but only when the book still says this spot (the bank may stand somewhere else already). */
    public void wis(UUID bank, GlobalPos plek) {
        if (adressen.remove(bank, plek)) {
            setDirty();
        }
    }

    public int aantal() {
        return adressen.size();
    }

    /**
     * The placed Bank Guh with this id, or null when it stands nowhere. {@code laad}: load its chunk when it is not loaded
     * (and keep it loaded for a little while with a {@link BankFeature#HAPLUIKJE_TICKET}, so a busy luikje does not
     * load it again and again); without it an unloaded bank gives null.
     */
    @Nullable
    public static BankGuhBlockEntity zoek(MinecraftServer server, UUID bank, boolean laad) {
        BankAdressen boek = van(server);
        GlobalPos plek = boek.plek(bank);
        if (plek == null) {
            return null;
        }
        ServerLevel level = server.getLevel(plek.dimension());
        if (level == null) {
            return null;   // (a dimension that is not there now: the address stays, the dimension may come back)
        }
        BlockPos pos = plek.pos();
        if (!level.isLoaded(pos)) {
            if (!laad) {
                return null;
            }
            level.getChunkSource().addTicketWithRadius(BankFeature.HAPLUIKJE_TICKET.get(), ChunkPos.containing(pos), 0);
            level.getChunk(pos);   // (loads it now: the bank was placed there, so the chunk exists on disk)
        }
        if (level.getBlockEntity(pos) instanceof BankGuhBlockEntity be && bank.equals(be.bankId())) {
            return be;
        }
        boek.wis(bank, plek);      // (a stale line: there is no such bank there any more)
        return null;
    }

    /** Keeps the chunk of a bank that is in use loaded a little longer (a ticket of the same kind replaces the old one). */
    public static void houdWakker(BankGuhBlockEntity bank) {
        if (bank.getLevel() instanceof ServerLevel level) {
            level.getChunkSource().addTicketWithRadius(BankFeature.HAPLUIKJE_TICKET.get(), ChunkPos.containing(bank.getBlockPos()), 0);
        }
    }
}
