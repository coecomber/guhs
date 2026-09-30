package nl.juiced.guhs.feature.gids;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The Guhdex tab "Verhalen": server to client {@code guhs:gids_verhalen} (every questline's {@link VerhaalStand} for this
 * player; sent when the Guhdex opens and when the tab asks), client to server {@code guhs:gids_verhalen_vraag}.
 */
public final class VerhalenPayloads {
    /** Client handler (set by client.GidsClient). */
    public static volatile Consumer<Data> ontvanger = p -> {
    };

    public record Data(List<VerhaalStand> verhalen) implements CustomPacketPayload {
        public static final Type<Data> TYPE = new Type<>(Guhs.id("gids_verhalen"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                VerhaalStand.STREAM_CODEC.apply(ByteBufCodecs.list(64)), Data::verhalen, Data::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Data p, IPayloadContext context) {
            ontvanger.accept(p);
        }
    }

    public record Vraag() implements CustomPacketPayload {
        public static final Type<Vraag> TYPE = new Type<>(Guhs.id("gids_verhalen_vraag"));
        public static final StreamCodec<FriendlyByteBuf, Vraag> STREAM_CODEC = StreamCodec.unit(new Vraag());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Vraag p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                sync(sp);
            }
        }
    }

    /** Sends this player's story progress. */
    public static void sync(ServerPlayer player) {
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new Data(VerhalenVoortgang.alle(player)));
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Data.TYPE, Data.STREAM_CODEC, Data::handle);
        registrar.playToServer(Vraag.TYPE, Vraag.STREAM_CODEC, Vraag::handle);
    }

    private VerhalenPayloads() {
    }
}
