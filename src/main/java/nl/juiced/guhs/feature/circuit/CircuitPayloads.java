package nl.juiced.guhs.feature.circuit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;

/** The Guh-Circuit's messages: Coach Vahoegvroem's screen (open / a button with the chosen track and level). */
public final class CircuitPayloads {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
    }

    /** Sends to a player, if their client knows the message (fake players in tests don't). */
    static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (!(player instanceof net.neoforged.neoforge.common.util.FakePlayer) && player.connection != null && player.connection.hasChannel(payload)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** Server -> client: open Coach Vahoegvroem's screen. */
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("circuit_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.circuit.client.CircuitClient.open(payload);
        }
    }

    /** Client -> server: a button (CircuitRole.START / GHOST / GOUD / SHOP), with the chosen track and level. */
    public record Action(int npcId, int action, String baan, int niveau) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("circuit_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, ByteBufCodecs.STRING_UTF8, Action::baan,
                ByteBufCodecs.VAR_INT, Action::niveau, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(payload.npcId()) instanceof GuhNpcEntity npc) {
                CircuitRole.action(npc, player, payload.action(), payload.baan(), payload.niveau());
            }
        }
    }

    private CircuitPayloads() {
    }
}
