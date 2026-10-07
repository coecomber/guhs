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
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.storage.BankStorage;
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
 * <p>
 * <b>A bank whose chunk is not loaded is never loaded just to ANSWER a question.</b> When a bank's chunk unloads, the
 * book keeps a {@link Schaduw} of it in memory (how much room it has for every kind; nothing changes in a chunk that is
 * not loaded, so that stays exact), and a Hapluikje answers "what fits" from that. Only a real, committed delivery or a
 * player's click loads the chunk ({@link #zoek} with {@code laad}). A bank the book has no shadow of (after a server
 * start) is asked for in the background ({@link #wek}) and the luikje refuses until it is there. So whatever only polls
 * a luikje (a Richtingstuk every few ticks, a hopper holding what the bank is full of, a Haltepaaltje that asks what
 * fits, the chore scan) loads nothing and keeps nothing loaded.
 */
public final class BankAdressen extends SavedData {
    private static final Codec<BankAdressen> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC)
            .xmap(BankAdressen::new, a -> a.adressen);
    public static final SavedDataType<BankAdressen> TYPE = GuhSavedData.type("bank_adressen", BankAdressen::new, CODEC);

    private final Map<UUID, GlobalPos> adressen = new HashMap<>();
    /** Banks whose chunk unloaded since the server started: what they held then. In memory only, never saved. */
    private final Map<UUID, Schaduw> schaduwen = new HashMap<>();

    /**
     * What a bank held at the moment its chunk unloaded: enough to say how many more of a kind fit. Exact for as long as
     * the chunk stays unloaded; thrown away as soon as the bank is back ({@link #zet}) or gone ({@link #wis}).
     */
    public static final class Schaduw {
        private final Map<ItemResource, Long> aantallen;
        private final boolean opgevoerd;

        private Schaduw(Map<ItemResource, Long> aantallen, boolean opgevoerd) {
            this.aantallen = aantallen;
            this.opgevoerd = opgevoerd;
        }

        /** How many more of this kind the bank takes (the rule of {@link BankStorage#room}). */
        public long ruimte(ItemResource soort) {
            Long heeft = aantallen.get(soort);
            long vrij = (opgevoerd ? Long.MAX_VALUE : BankStorage.CAP) - (heeft == null ? 0 : heeft);
            return Math.max(0, vrij);
        }
    }

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
        schaduwen.remove(bank);   // (the bank itself is there again: ask it, not its shadow)
        if (!plek.equals(adressen.put(bank, plek))) {
            setDirty();
        }
    }

    /** Wipes the address, but only when the book still says this spot (the bank may stand somewhere else already). */
    public void wis(UUID bank, GlobalPos plek) {
        if (adressen.remove(bank, plek)) {
            schaduwen.remove(bank);
            setDirty();
        }
    }

    public int aantal() {
        return adressen.size();
    }

    // =====================================================================================================================
    // the shadow of a bank whose chunk is not loaded
    // =====================================================================================================================

    /** The chunk of this bank unloads: remember what it holds (only when the book says the bank stands right there). */
    public void onthoud(UUID bank, GlobalPos plek, BankStorage maag) {
        if (plek.equals(adressen.get(bank))) {
            schaduwen.put(bank, new Schaduw(maag.aantallen(), maag.isUpgraded()));
        }
    }

    /**
     * Forgets the shadow of this bank: the block there was removed while its chunk was loaded, so what the book
     * remembers is no longer true. (A server start forgets all of them: they are not saved.)
     */
    public void vergeet(UUID bank) {
        schaduwen.remove(bank);
    }

    /** What the bank held when its chunk unloaded, or null: not known (ask the bank itself, or {@link #wek} it). */
    @Nullable
    public Schaduw schaduw(UUID bank) {
        return schaduwen.get(bank);
    }

    /** Is the chunk of this spot there right now, completely? Never waits for a chunk that is still on its way. */
    public static boolean geladen(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    /**
     * Asks for the chunk of this bank in the background (a {@link BankFeature#HAPLUIKJE_TICKET}; nothing waits for it):
     * some ticks later {@link #zoek} finds the bank without loading. Returns false when the bank stands nowhere.
     */
    public static boolean wek(MinecraftServer server, UUID bank) {
        GlobalPos plek = van(server).plek(bank);
        ServerLevel level = plek == null ? null : server.getLevel(plek.dimension());
        if (level == null) {
            return false;
        }
        level.getChunkSource().addTicketWithRadius(BankFeature.HAPLUIKJE_TICKET.get(), ChunkPos.containing(plek.pos()), 0);
        return true;
    }

    // =====================================================================================================================
    // the bank itself
    // =====================================================================================================================

    /**
     * The placed Bank Guh with this id, or null when it stands nowhere. {@code laad}: load its chunk NOW when it is not
     * loaded (and keep it loaded for a little while with a {@link BankFeature#HAPLUIKJE_TICKET}, so a busy luikje does
     * not load it again and again): only for a real delivery or a player's click. Without it an unloaded bank gives null
     * and nothing is loaded.
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
        if (!geladen(level, pos)) {
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
