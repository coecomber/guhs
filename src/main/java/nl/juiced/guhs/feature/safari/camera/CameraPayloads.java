package nl.juiced.guhs.feature.safari.camera;

import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** The camera's packets (S1): the result of a photo (server to client) and "print this photo" (client to server). */
public final class CameraPayloads {
    /** (Client) what to do with a photo result: the flash and the little photo card (set by the client side). */
    public static volatile Consumer<Uitslag> ontvanger = u -> { };

    /** A photo was taken: {@code foto} the photo tag (empty: nothing in the picture), the previous best stars of the
     *  species, the coins it paid, how many of today's missions it finished. */
    public record Uitslag(CompoundTag foto, int vorige, int munten, int missies) implements CustomPacketPayload {
        public static final Type<Uitslag> TYPE = new Type<>(Guhs.id("safari_foto"));
        public static final StreamCodec<FriendlyByteBuf, Uitslag> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Uitslag::foto, ByteBufCodecs.VAR_INT, Uitslag::vorige, ByteBufCodecs.VAR_INT, Uitslag::munten,
                ByteBufCodecs.VAR_INT, Uitslag::missies, Uitslag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Uitslag p, IPayloadContext context) {
            ontvanger.accept(p);
        }
    }

    /** G1: "schilderij:" before the species = print it as a painting ({@link SafariFotoItem#schilderij}). */
    public static final String SCHILDERIJ = "schilderij:";

    /** Print the dex photo of this species as a photo card (costs a sheet of paper). */
    public record Afdrukken(String soort) implements CustomPacketPayload {
        public static final Type<Afdrukken> TYPE = new Type<>(Guhs.id("safari_afdrukken"));
        public static final StreamCodec<ByteBuf, Afdrukken> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Afdrukken::new, Afdrukken::soort);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Afdrukken p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                if (p.soort().startsWith(SCHILDERIJ)) {
                    SafariFotoItem.schilderij(sp, p.soort().substring(SCHILDERIJ.length()));
                } else {
                    SafariFotoItem.afdrukken(sp, p.soort());
                }
            }
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(Uitslag.TYPE, Uitslag.STREAM_CODEC, Uitslag::handle);
        registrar.playToServer(Afdrukken.TYPE, Afdrukken.STREAM_CODEC, Afdrukken::handle);
    }

    private CameraPayloads() {
    }
}
