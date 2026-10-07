package nl.juiced.guhs.feature.guhpixel.reisbureau;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
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
 * The Reisbureau over the network:
 * <ul>
 *   <li>{@code guhs:reisbureau_open} (to the client): open / refresh the trip screen with the whole state (Reizen.stand);</li>
 *   <li>{@code guhs:reisbureau_kaart} (to the client): open an ansichtkaart to read;</li>
 *   <li>{@code guhs:reisbureau_actie} (to the server): book, collect, call back early or refresh; the server checks everything.</li>
 * </ul>
 * Client handlers are set by client.ReisbureauClient; the server never calls them.
 */
public final class ReisbureauPayloads {
    public static final int BOEK = 0, OPHALEN = 1, EERDER = 2, VERVERS = 3;

    public static volatile Consumer<Open> opener = p -> { };
    public static volatile Consumer<Kaart> kaartOpener = p -> { };

    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("reisbureau_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            opener.accept(p);
        }
    }

    public record Kaart(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Kaart> TYPE = new Type<>(Guhs.id("reisbureau_kaart"));
        public static final StreamCodec<FriendlyByteBuf, Kaart> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Kaart::data, Kaart::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kaart p, IPayloadContext context) {
            kaartOpener.accept(p);
        }
    }

    /** actie: {@link #BOEK} (bestemming + guh), {@link #OPHALEN}, {@link #EERDER}, {@link #VERVERS}. */
    public record Actie(BlockPos pos, int actie, String bestemming, String guh) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("reisbureau_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Actie::pos,
                ByteBufCodecs.VAR_INT, Actie::actie, ByteBufCodecs.stringUtf8(48), Actie::bestemming, ByteBufCodecs.stringUtf8(48), Actie::guh, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Actie p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                Reizen.opActie(sp, p.pos(), p.actie(), p.bestemming(), p.guh());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToClient(Kaart.TYPE, Kaart.STREAM_CODEC, Kaart::handle);
        registrar.playToServer(Actie.TYPE, Actie.STREAM_CODEC, Actie::handle);
    }

    private ReisbureauPayloads() {
    }
}
