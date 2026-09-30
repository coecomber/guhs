package nl.juiced.guhs.feature.hemel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
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
 * The Knuffelhart's messages: guhs:hemel_open (server -> client: the revive screen, opened or updated, see {@link Hemel#data}),
 * guhs:hemel_terug (client -> server: bring this guh back) and guhs:hemel_status (server -> client: does the Knuffelhart beat
 * for you? The heart's renderer and music follow it).
 */
public final class HemelPayloads {
    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("hemel_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.hemel.client.HemelClient.open(payload.data());
        }
    }

    public record Terug(long hart, java.util.UUID guh) implements CustomPacketPayload {
        public static final Type<Terug> TYPE = new Type<>(Guhs.id("hemel_terug"));
        public static final StreamCodec<FriendlyByteBuf, Terug> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, Terug::hart,
                UUIDUtil.STREAM_CODEC, Terug::guh, Terug::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Terug payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer p) {
                Hemel.kies(p, BlockPos.of(payload.hart()), payload.guh());
            }
        }
    }

    public record Status(boolean klopt) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(Guhs.id("hemel_status"));
        public static final StreamCodec<FriendlyByteBuf, Status> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Status::klopt, Status::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Status payload, IPayloadContext context) {
            nl.juiced.guhs.feature.hemel.client.HemelClient.status(payload.klopt());
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Terug.TYPE, Terug.STREAM_CODEC, Terug::handle);
        registrar.playToClient(Status.TYPE, Status.STREAM_CODEC, Status::handle);
    }

    /** Tells the player's client whether the Knuffelhart beats for them. */
    public static void status(ServerPlayer p) {
        ModNetworking.sendTo(p, new Status(HemelQuest.klopt(p)));
    }

    private HemelPayloads() {
    }
}
