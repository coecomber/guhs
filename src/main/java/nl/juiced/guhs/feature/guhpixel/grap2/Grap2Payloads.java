package nl.juiced.guhs.feature.guhpixel.grap2;

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
 * The Guhmon-gevecht and Boer zoekt Guh over the network:
 * <ul>
 *   <li>{@code guhs:guhmon_scherm} (to the client): the picker ("Scherm" = kies) or the battle screen (gevecht), opened or refreshed;</li>
 *   <li>{@code guhs:guhmon_doe} (to the server): {@link #KIES} a guh (arg = its band id or "leenguh"), a move ({@link #ZET}, arg = 0..3),
 *   {@link #OPNIEUW} after a lost battle, {@link #VERDER} after a won one;</li>
 *   <li>{@code guhs:bzg_scherm} (to the client): the letters (brieven), the framed thank-you letter (brief) or the credits (aftiteling);</li>
 *   <li>{@code guhs:bzg_doe} (to the server): a letter was {@link #GELEZEN} (arg = 0..2), the letters were put away
 *   ({@link #BRIEVEN_DICHT}), the credits are over ({@link #AFTITELING_KLAAR}).</li>
 * </ul>
 * The server decides everything; the client only shows. Client handlers are set by client.Grap2Client.
 */
public final class Grap2Payloads {
    public static final int KIES = 0, ZET = 1, OPNIEUW = 2, VERDER = 3;
    public static final int GELEZEN = 0, BRIEVEN_DICHT = 1, AFTITELING_KLAAR = 2;

    public static volatile Consumer<GuhmonScherm> guhmonOpener = p -> { };
    public static volatile Consumer<BzgScherm> bzgOpener = p -> { };

    public record GuhmonScherm(CompoundTag data) implements CustomPacketPayload {
        public static final Type<GuhmonScherm> TYPE = new Type<>(Guhs.id("guhmon_scherm"));
        public static final StreamCodec<FriendlyByteBuf, GuhmonScherm> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, GuhmonScherm::data,
                GuhmonScherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(GuhmonScherm p, IPayloadContext context) {
            guhmonOpener.accept(p);
        }
    }

    public record GuhmonDoe(int actie, String arg) implements CustomPacketPayload {
        public static final Type<GuhmonDoe> TYPE = new Type<>(Guhs.id("guhmon_doe"));
        public static final StreamCodec<FriendlyByteBuf, GuhmonDoe> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, GuhmonDoe::actie,
                ByteBufCodecs.stringUtf8(64), GuhmonDoe::arg, GuhmonDoe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(GuhmonDoe p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                Guhmon.doe(sp, p.actie(), p.arg());
            }
        }
    }

    public record BzgScherm(CompoundTag data) implements CustomPacketPayload {
        public static final Type<BzgScherm> TYPE = new Type<>(Guhs.id("bzg_scherm"));
        public static final StreamCodec<FriendlyByteBuf, BzgScherm> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, BzgScherm::data,
                BzgScherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(BzgScherm p, IPayloadContext context) {
            bzgOpener.accept(p);
        }
    }

    public record BzgDoe(int actie, int arg) implements CustomPacketPayload {
        public static final Type<BzgDoe> TYPE = new Type<>(Guhs.id("bzg_doe"));
        public static final StreamCodec<FriendlyByteBuf, BzgDoe> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, BzgDoe::actie,
                ByteBufCodecs.VAR_INT, BzgDoe::arg, BzgDoe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(BzgDoe p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                Bzg.doe(sp, p.actie(), p.arg());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(GuhmonScherm.TYPE, GuhmonScherm.STREAM_CODEC, GuhmonScherm::handle);
        registrar.playToClient(BzgScherm.TYPE, BzgScherm.STREAM_CODEC, BzgScherm::handle);
        registrar.playToServer(GuhmonDoe.TYPE, GuhmonDoe.STREAM_CODEC, GuhmonDoe::handle);
        registrar.playToServer(BzgDoe.TYPE, BzgDoe.STREAM_CODEC, BzgDoe::handle);
    }

    private Grap2Payloads() {
    }
}
