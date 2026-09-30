package nl.juiced.guhs.feature.knabbelspelen;

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

/**
 * Juf Vahoegsakee's screen (guhs:knabbelspelen_open, guhs:knabbelspelen_action) and the blindfold of Guhguhtje prik
 * (guhs:knabbelspelen_blinddoek, server -> client: on / off).
 */
public final class KnabbelspelenPayloads {
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("knabbelspelen_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.knabbelspelen.client.KnabbelspelenClient.open(payload);
        }
    }

    public record Action(int npcId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("knabbelspelen_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(payload.npcId()) instanceof GuhNpcEntity npc) {
                Wedstrijd.action(npc, player, payload.action());
            }
        }
    }

    public record Blinddoek(boolean aan) implements CustomPacketPayload {
        public static final Type<Blinddoek> TYPE = new Type<>(Guhs.id("knabbelspelen_blinddoek"));
        public static final StreamCodec<FriendlyByteBuf, Blinddoek> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Blinddoek::aan, Blinddoek::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Blinddoek payload, IPayloadContext context) {
            nl.juiced.guhs.feature.knabbelspelen.client.KnabbelspelenClient.blinddoek(payload.aan());
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
        registrar.playToClient(Blinddoek.TYPE, Blinddoek.STREAM_CODEC, Blinddoek::handle);
    }

    private KnabbelspelenPayloads() {
    }
}
