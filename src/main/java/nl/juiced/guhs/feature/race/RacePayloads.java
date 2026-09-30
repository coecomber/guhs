package nl.juiced.guhs.feature.race;

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

/** The guhrace's messages: the Raceguh's screen (open / a button), and the race panel of the racer. */
public final class RacePayloads {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
        registrar.playToClient(Hud.TYPE, Hud.STREAM_CODEC, Hud::handle);
    }

    /** Sends to a player, if their client knows the message (fake players in tests don't). */
    static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (!(player instanceof net.neoforged.neoforge.common.util.FakePlayer) && player.connection != null && player.connection.hasChannel(payload)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** Server -> client: open the Raceguh's screen (records, whether the track is free...). */
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("race_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.race.client.RaceClient.open(payload);
        }
    }

    /** Client -> server: a button on the Raceguh's screen (RaceRole.START, GHOST, SHOP). */
    public record Action(int npcId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("race_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(payload.npcId()) instanceof GuhNpcEntity npc) {
                RaceRole.action(npc, player, payload.action());
            }
        }
    }

    /** Server -> client: the race panel (lap, next checkpoint, times; the client keeps the clock running). */
    public record Hud(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Hud> TYPE = new Type<>(Guhs.id("race_hud"));
        public static final StreamCodec<FriendlyByteBuf, Hud> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Hud::data, Hud::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Hud payload, IPayloadContext context) {
            nl.juiced.guhs.feature.race.client.RaceClient.hud(payload.data());
        }
    }

    private RacePayloads() {
    }
}
