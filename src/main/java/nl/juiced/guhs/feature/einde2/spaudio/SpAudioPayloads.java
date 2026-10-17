package nl.juiced.guhs.feature.einde2.spaudio;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * einde2 "Sponsguh", slice sp_audio: the three messages from the server to one player's client.
 * <ul>
 *   <li>{@code guhs:einde2_sp_audio_plek}: the music area the player is in ({@code bikinibodem}, {@code schelpenstad},
 *       {@code loopgraaf} or "" for none), sent only when it changes;</li>
 *   <li>{@code guhs:einde2_sp_audio_moment}: start a moment (a full name, {@code moment.plan_z}; for a song {@code lied} is
 *       true and the client starts it on its scene's tick), or end the one that plays ("" with the fade-out in ticks);</li>
 *   <li>{@code guhs:einde2_sp_audio_deuntje}: start a minigame tune (a full name, {@code deuntje.bakken}; {@code herhaal} =
 *       loop it), or stop the tune ("").</li>
 * </ul>
 * The handlers only put the news in the fields below; the client (client/SpAudioSpeler) picks it up on its next tick, so this
 * common class never touches client code (the same pattern as feature/einde2/audio).
 */
public final class SpAudioPayloads {
    public static volatile String clientPlek = "";
    public static volatile Moment clientMoment;
    public static volatile Deuntje clientDeuntje;

    public record Plek(String gebied) implements CustomPacketPayload {
        public static final Type<Plek> TYPE = new Type<>(Guhs.id("einde2_sp_audio_plek"));
        public static final StreamCodec<ByteBuf, Plek> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16), Plek::gebied, Plek::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Plek p, IPayloadContext context) {
            clientPlek = p.gebied();
        }
    }

    public record Moment(String naam, int fade, boolean lied) implements CustomPacketPayload {
        public static final Type<Moment> TYPE = new Type<>(Guhs.id("einde2_sp_audio_moment"));
        public static final StreamCodec<ByteBuf, Moment> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(64), Moment::naam,
                ByteBufCodecs.VAR_INT, Moment::fade, ByteBufCodecs.BOOL, Moment::lied, Moment::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Moment p, IPayloadContext context) {
            clientMoment = p;
        }
    }

    public record Deuntje(String naam, boolean herhaal) implements CustomPacketPayload {
        public static final Type<Deuntje> TYPE = new Type<>(Guhs.id("einde2_sp_audio_deuntje"));
        public static final StreamCodec<ByteBuf, Deuntje> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(64), Deuntje::naam,
                ByteBufCodecs.BOOL, Deuntje::herhaal, Deuntje::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Deuntje p, IPayloadContext context) {
            clientDeuntje = p;
        }
    }

    /** Client: leaving a world or server. */
    public static void wisClient() {
        clientPlek = "";
        clientMoment = null;
        clientDeuntje = null;
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Plek.TYPE, Plek.STREAM_CODEC, Plek::handle);
        registrar.playToClient(Moment.TYPE, Moment.STREAM_CODEC, Moment::handle);
        registrar.playToClient(Deuntje.TYPE, Deuntje.STREAM_CODEC, Deuntje::handle);
    }

    private SpAudioPayloads() {
    }
}
