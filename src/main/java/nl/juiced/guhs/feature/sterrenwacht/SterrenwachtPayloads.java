package nl.juiced.guhs.feature.sterrenwacht;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The telescope: guhs:sterrenwacht_open (server -> client: which constellation is in the sky, a seed for the other
 * stars, new for you or not, how many you found tonight), guhs:sterrenwacht_klaar (client -> server: the lines you
 * connected) and guhs:sterrenwacht_stop (client -> server: you closed the screen).
 */
public final class SterrenwachtPayloads {

    public record Open(BlockPos telescoop, int beeld, int seed, boolean nieuw, int vannacht) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("sterrenwacht_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Open::telescoop, ByteBufCodecs.VAR_INT, Open::beeld, ByteBufCodecs.INT, Open::seed,
                ByteBufCodecs.BOOL, Open::nieuw, ByteBufCodecs.VAR_INT, Open::vannacht, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.sterrenwacht.client.SterrenwachtClient.open(payload);
        }
    }

    /** The lines connected (flat list: a0, b0, a1, b1, ...) and how many wrong tries. */
    public record Klaar(int beeld, List<Integer> lijnen, int fouten) implements CustomPacketPayload {
        public static final Type<Klaar> TYPE = new Type<>(Guhs.id("sterrenwacht_klaar"));
        public static final StreamCodec<FriendlyByteBuf, Klaar> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Klaar::beeld, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), Klaar::lijnen,
                ByteBufCodecs.VAR_INT, Klaar::fouten, Klaar::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Klaar payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                Sterrenkijken.klaar(player, payload.beeld(), payload.lijnen());
            }
        }
    }

    public record Stop() implements CustomPacketPayload {
        public static final Type<Stop> TYPE = new Type<>(Guhs.id("sterrenwacht_stop"));
        public static final StreamCodec<FriendlyByteBuf, Stop> STREAM_CODEC = StreamCodec.unit(new Stop());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stop payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                Sterrenkijken.stop(player);
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Klaar.TYPE, Klaar.STREAM_CODEC, Klaar::handle);
        registrar.playToServer(Stop.TYPE, Stop.STREAM_CODEC, Stop::handle);
    }

    private SterrenwachtPayloads() {
    }
}
