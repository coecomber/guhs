package nl.juiced.guhs.feature.techquest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;

/**
 * bbq2 (tech-quests): the one message of the slice. Server -> client: how far this player's own Grote Knabbelmachine is
 * ({@link Stand}), so the client can draw the machine on the bordes of the Oude Guhrad-centrale the way it is for this
 * very player (client.KnabbelmachineRenderer).
 */
public final class TechquestPayloads {
    private TechquestPayloads() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
    }

    /** Sends to a player, if their client knows the message (mock players in tests don't). */
    static void send(ServerPlayer player, CustomPacketPayload payload) {
        ModNetworking.sendTo(player, payload);
    }

    /** data: "Fase" (int 0..6: the step of the questline knabbelmachine), "Knabbel" (boolean: today's knabbel lies ready). */
    public record Stand(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("techquest_stand"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Stand::data, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand payload, IPayloadContext context) {
            nl.juiced.guhs.feature.techquest.client.TechquestClient.zet(payload.data());
        }
    }
}
