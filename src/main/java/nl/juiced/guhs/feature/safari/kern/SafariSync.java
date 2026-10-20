package nl.juiced.guhs.feature.safari.kern;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;

/**
 * Op Safari! step 2 (S0): the client's copy of this player's safari state ({@code guhs:safari_stand}, server → client).
 * It is the whole per-player tag of {@link SafariData} (dex with photo data, munten, geheimpjes, badges, vlaggen, quest
 * states, today's counters, board cooldowns): small, and every UI slice reads what it needs from {@link #stand}.
 * Sent on login, on dimension change and after every change ({@link SafariSpeler}, {@link SafariMunten}).
 * <p>
 * Client side: {@link #stand} holds the last tag; {@link #ontvanger} lets a client class react (refresh a screen, HUD).
 * Keys inside the tag: see {@link SafariSpeler} / {@link SafariMunten} (Dex.&lt;soort&gt;.{Gezien,Sterren,Foto},
 * Munten, MuntenTotaal, Geheimpjes, Badges, Vlaggen, Quests, Dag, Bord).
 */
public final class SafariSync {
    /** (Client) the last received state; an empty tag before the first sync. */
    public static volatile CompoundTag stand = new CompoundTag();
    /** (Client) extra reaction to a new state (after {@link #stand} is set). */
    public static volatile Consumer<CompoundTag> ontvanger = t -> { };

    public record Stand(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("safari_stand"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Stand::data, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand p, IPayloadContext context) {
            stand = p.data();
            ontvanger.accept(p.data());
        }
    }

    /** Sends this player its whole safari state. */
    public static void stuur(ServerPlayer p) {
        ModNetworking.sendTo(p, new Stand(SafariSpeler.tag(p.level().getServer(), p.getUUID()).copy()));
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
    }

    static void bijInloggen(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            stuur(p);
        }
    }

    static void bijWereldWissel(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            stuur(p);
        }
    }

    private SafariSync() {
    }
}
