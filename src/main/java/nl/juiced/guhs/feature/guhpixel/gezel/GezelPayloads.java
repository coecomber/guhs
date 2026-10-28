package nl.juiced.guhs.feature.guhpixel.gezel;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The two board games over the network:
 * <ul>
 *   <li>{@code guhs:gezel_scherm} (to the client): the whole state of your board game (BordSpel#staat), opened or refreshed;</li>
 *   <li>{@code guhs:gezel_doe} (to the server): what you did ({@link BordSpel} KIES_BOT, ZOEK, ANNULEER, ZET, VRAAG, RAAD,
 *   OPNIEUW, SLUIT), for which game, with one number.</li>
 * </ul>
 * The server decides everything; the client only shows. The client handler is set by client.GezelClient.
 */
public final class GezelPayloads {
    public static volatile Consumer<Scherm> opener = p -> { };

    public record Scherm(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("gezel_scherm"));
        public static final StreamCodec<FriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Scherm::data,
                Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Scherm p, IPayloadContext context) {
            opener.accept(p);
        }
    }

    public record Doe(String spel, int actie, int arg) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("gezel_doe"));
        public static final StreamCodec<FriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16), Doe::spel,
                ByteBufCodecs.VAR_INT, Doe::actie, ByteBufCodecs.VAR_INT, Doe::arg, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Doe p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                BordSpel.doe(sp, p.spel(), p.actie(), p.arg());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, Scherm::handle);
        registrar.playToServer(Doe.TYPE, Doe.STREAM_CODEC, Doe::handle);
    }

    private GezelPayloads() {
    }
}
