package nl.juiced.guhs.feature.kermiskraam;

import java.util.function.Consumer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** A game stall's two messages: start a round (server → client) and how it went (client → server). */
public final class KraamPayloads {
    /** (Client) opens the game screen; set by client.KraamClient. */
    public static volatile Consumer<Open> opener = p -> { };

    public record Open(String kraam, long seed) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("kermiskraam_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(32), Open::kraam,
                ByteBufCodecs.LONG, Open::seed, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            opener.accept(p);
        }
    }

    public record Klaar(long seed, int score) implements CustomPacketPayload {
        public static final Type<Klaar> TYPE = new Type<>(Guhs.id("kermiskraam_klaar"));
        public static final StreamCodec<FriendlyByteBuf, Klaar> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.LONG, Klaar::seed,
                ByteBufCodecs.VAR_INT, Klaar::score, Klaar::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Klaar p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> KraamSpel.klaar(sp, p.seed(), p.score()));
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Klaar.TYPE, Klaar.STREAM_CODEC, Klaar::handle);
    }

    private KraamPayloads() {
    }
}
