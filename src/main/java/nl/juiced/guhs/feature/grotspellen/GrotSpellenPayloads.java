package nl.juiced.guhs.feature.grotspellen;

import java.util.function.Consumer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * Server to client: {@link Potje}, the glimworm jar on a player's back and how bright it is (-1: no jar). Sent to the
 * player and everyone who sees them when it changes and every few seconds during a game; the client forgets a jar it
 * hasn't heard of for a while (client.GrotSpellenClient).
 */
public final class GrotSpellenPayloads {
    /** Set by the client init: what to do with a jar update. */
    public static Consumer<Potje> potjeOntvanger = p -> {
    };

    public record Potje(int speler, int stand) implements CustomPacketPayload {
        public static final Type<Potje> TYPE = new Type<>(Guhs.id("grotspel_potje"));
        public static final StreamCodec<FriendlyByteBuf, Potje> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Potje::speler, ByteBufCodecs.INT, Potje::stand, Potje::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Potje p, IPayloadContext context) {
            context.enqueueWork(() -> potjeOntvanger.accept(p));
        }
    }

    /** Tells everyone around (and the player) about this player's jar. */
    public static void stuur(ServerPlayer p, int stand) {
        if (p.connection != null) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new Potje(p.getId(), stand));
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Potje.TYPE, Potje.STREAM_CODEC, Potje::handle);
    }

    private GrotSpellenPayloads() {
    }
}
