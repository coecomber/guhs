package nl.juiced.guhs.feature.guhriobeloning;

import java.util.function.Consumer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** The payloads of guhrio-beloning (server -> client): {@link Film}, the little film of a trip through a green pipe. */
public final class GuhrioBeloningPayloads {
    /** Set by the client init (GuhrioBeloningClient): what to do with a film on the client. */
    public static Consumer<Film> filmOntvanger = f -> {
    };

    /**
     * A player (entity id) slides down into a pipe ({@link #IN}) or rises out of one ({@link #UIT}), in this many ticks.
     * Sent to that player and to everybody near them.
     */
    public record Film(int speler, int fase, int ticks) implements CustomPacketPayload {
        public static final int IN = 0, UIT = 1;
        public static final Type<Film> TYPE = new Type<>(Guhs.id("guhriobeloning_film"));
        public static final StreamCodec<FriendlyByteBuf, Film> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Film::speler, ByteBufCodecs.VAR_INT, Film::fase, ByteBufCodecs.VAR_INT, Film::ticks, Film::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Film f, IPayloadContext context) {
            context.enqueueWork(() -> filmOntvanger.accept(f));
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Film.TYPE, Film.STREAM_CODEC, Film::handle);
    }

    private GuhrioBeloningPayloads() {
    }
}
