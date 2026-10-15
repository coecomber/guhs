package nl.juiced.guhs.feature.fossielmijn;

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
 * bbq2 (fossiel-mijn): the one message of the Fossiel-opgraving. Server -> client: what this player did at the dig
 * ({@link Stand}: the bones on the stand, the spots of bottenzand brushed empty), so the client can draw the stand and the
 * sand the way they are for this very player (client.TekenRenderer).
 */
public final class FossielmijnPayloads {
    private FossielmijnPayloads() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
    }

    /** Sends to a player, if their client knows the message (mock players in tests don't). */
    static void send(ServerPlayer player, CustomPacketPayload payload) {
        ModNetworking.sendTo(player, payload);
    }

    /** data: "Rek" (int: the mask of the bones on the stand), "Gekwast" (long[]: the brushed spots). */
    public record Stand(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("fossielmijn_stand"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Stand::data, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand payload, IPayloadContext context) {
            nl.juiced.guhs.feature.fossielmijn.client.FossielmijnClient.zet(payload.data());
        }
    }
}
