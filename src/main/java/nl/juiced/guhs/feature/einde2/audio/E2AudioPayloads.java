package nl.juiced.guhs.feature.einde2.audio;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice audio: the two messages from the server to a player's client (CONTRACT_E2_B 6.2).
 * <ul>
 *   <li>{@code guhs:einde2_audio_plek}: the music area the player is in now ({@code lucht}, {@code water}, {@code aarde},
 *       {@code vuur}, {@code geest} or "" for none), sent only when it changes;</li>
 *   <li>{@code guhs:einde2_audio_moment}: start an epic moment (a full name of 6.1, {@code moment.golf}), or end the one that
 *       plays ("" with the fade-out in ticks).</li>
 * </ul>
 * The handlers only put the news in the fields below; the client (client/AudioSpeler) picks it up on its next tick, so this
 * common class never touches client code.
 */
public final class E2AudioPayloads {
    /** Client: the area the server told last. */
    public static volatile String clientPlek = "";
    /** Client: a moment message not yet picked up (null: nothing new). */
    public static volatile Moment clientMoment;

    public record Plek(String gebied) implements CustomPacketPayload {
        public static final Type<Plek> TYPE = new Type<>(Guhs.id("einde2_audio_plek"));
        public static final StreamCodec<ByteBuf, Plek> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16), Plek::gebied, Plek::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Plek p, IPayloadContext context) {
            clientPlek = p.gebied();
        }
    }

    /** {@code naam} = a full name of 6.1 to start, or "" to stop; {@code fade} = the fade-out in ticks when stopping. */
    public record Moment(String naam, int fade) implements CustomPacketPayload {
        public static final Type<Moment> TYPE = new Type<>(Guhs.id("einde2_audio_moment"));
        public static final StreamCodec<ByteBuf, Moment> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(64), Moment::naam,
                ByteBufCodecs.VAR_INT, Moment::fade, Moment::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Moment p, IPayloadContext context) {
            clientMoment = p;
        }
    }

    /** Client: leaving a world or server (the next one tells its own area). */
    public static void wisClient() {
        clientPlek = "";
        clientMoment = null;
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Plek.TYPE, Plek.STREAM_CODEC, Plek::handle);
        registrar.playToClient(Moment.TYPE, Moment.STREAM_CODEC, Moment::handle);
    }

    private E2AudioPayloads() {
    }
}
