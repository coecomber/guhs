package nl.juiced.guhs.feature.meppen;

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

/** The Mepguh's screen: opened by the server (with the game and your records), buttons go back to the server. */
public final class MepPayloads {

    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("mika_mep_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.meppen.client.MeppenClient.open(payload);
        }
    }

    public record Action(int npcId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("mika_mep_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(p.npcId()) instanceof GuhNpcEntity npc
                    && npc.getKind() == GuhNpcEntity.Kind.MEPGUH) {
                MepGame.action(npc, player, p.action());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
    }

    private MepPayloads() {
    }
}
