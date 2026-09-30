package nl.juiced.guhs.feature.kapper;

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
 * Kapper Krulletje's screens: guhs:kapper_open (server -> client: the talking screen), guhs:kapper_knip (server ->
 * client: open / update / close the knip screen with the show's state, see KappersShow.data) and guhs:kapper_action
 * (client -> server: a button, KappersShow.START ... VERF + n).
 */
public final class KapperPayloads {
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("kapper_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.kapper.client.KapperClient.open(payload);
        }
    }

    /** The knip screen: "Open" (true: open it), "Sluit" (true: close it), otherwise update it when it's open. */
    public record Knip(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Knip> TYPE = new Type<>(Guhs.id("kapper_knip"));
        public static final StreamCodec<FriendlyByteBuf, Knip> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Knip::npcId, ByteBufCodecs.COMPOUND_TAG, Knip::data, Knip::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Knip payload, IPayloadContext context) {
            nl.juiced.guhs.feature.kapper.client.KapperClient.knip(payload);
        }
    }

    public record Action(int npcId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("kapper_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(p.npcId()) instanceof GuhNpcEntity npc) {
                KappersShow.action(npc, player, p.action());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToClient(Knip.TYPE, Knip.STREAM_CODEC, Knip::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
    }

    private KapperPayloads() {
    }
}
