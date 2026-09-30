package nl.juiced.guhs.feature.beauty;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** The network messages of the beauty contest: the Showguh's screens (server to client) and the buttons (client to server). */
public final class BeautyPayloads {
    /**
     * Server to client: open (or close) a screen. data.Mode is "lobby" (the Showguh's screen), "dress" (the loaner
     * wardrobe while dressing the model) or "close".
     */
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("beauty_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.beauty.client.BeautyClient.open(payload);
        }
    }

    /** Client to server: a button in one of the screens (see the action numbers in {@link BeautyShow}). */
    public record Action(int npcId, int action, int value) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("beauty_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, ByteBufCodecs.VAR_INT, Action::value, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player
                    && player.level().getEntity(p.npcId()) instanceof nl.juiced.guhs.entity.GuhNpcEntity npc) {
                BeautyShow.action(npc, player, p.action(), p.value());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
    }

    private BeautyPayloads() {
    }
}
