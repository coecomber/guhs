package nl.juiced.guhs.feature.mewtwo;

import java.util.function.Consumer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The kloon-eiland's payloads (server -> client): {@link Stand} (your questline: the tank you see, the spots that still
 * sparkle) and {@link X2} (a Guhtwo eats: the funny x2 double chomp).
 */
public final class MewtwoPayloads {
    /** Set by the client init (MewtwoClient): what to do with them on the client. */
    public static Consumer<Stand> standOntvanger = s -> {
    };
    public static Consumer<X2> x2Ontvanger = x -> {
    };

    public record Stand(int stap, int notities, int onderdelen, int ingebouwd) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("mewtwo_stand"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Stand::stap, ByteBufCodecs.VAR_INT, Stand::notities, ByteBufCodecs.VAR_INT, Stand::onderdelen,
                ByteBufCodecs.VAR_INT, Stand::ingebouwd, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand s, IPayloadContext context) {
            context.enqueueWork(() -> standOntvanger.accept(s));
        }
    }

    public record X2(int guh) implements CustomPacketPayload {
        public static final Type<X2> TYPE = new Type<>(Guhs.id("mewtwo_x2"));
        public static final StreamCodec<FriendlyByteBuf, X2> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, X2::guh, X2::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(X2 x, IPayloadContext context) {
            context.enqueueWork(() -> x2Ontvanger.accept(x));
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
        registrar.playToClient(X2.TYPE, X2.STREAM_CODEC, X2::handle);
    }

    private MewtwoPayloads() {
    }
}
