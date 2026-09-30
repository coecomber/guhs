package nl.juiced.guhs.feature.samen;

import java.util.function.Consumer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The messages of samen (2.10). Server to client: which hartjes emotes you unlocked ({@code guhs:samen_emotes}, sent at
 * login and on every unlock) and the big bff-knuffel heart between you and your guh ({@code guhs:samen_bff}).
 */
public final class SamenPayloads {
    /** Client handler of the bff-knuffel (set by client.SamenClient; the dedicated server never gets it). */
    public static volatile Consumer<Bff> bffOntvanger = p -> {
    };

    /** The unlocked hartjes emotes (bits of Emote ordinals). */
    public record Emotes(int bits) implements CustomPacketPayload {
        public static final Type<Emotes> TYPE = new Type<>(Guhs.id("samen_emotes"));
        public static final StreamCodec<FriendlyByteBuf, Emotes> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Emotes::bits, Emotes::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Emotes p, IPayloadContext context) {
            SamenBeloning.Client.zet(p.bits());
        }
    }

    /** A bff-knuffel between a guh and a player: the client draws the big heart above them both for a while. */
    public record Bff(int guh, int speler) implements CustomPacketPayload {
        public static final Type<Bff> TYPE = new Type<>(Guhs.id("samen_bff"));
        public static final StreamCodec<FriendlyByteBuf, Bff> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Bff::guh, ByteBufCodecs.VAR_INT, Bff::speler, Bff::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Bff p, IPayloadContext context) {
            bffOntvanger.accept(p);
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Emotes.TYPE, Emotes.STREAM_CODEC, Emotes::handle);
        registrar.playToClient(Bff.TYPE, Bff.STREAM_CODEC, Bff::handle);
    }

    private SamenPayloads() {
    }
}
