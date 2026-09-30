package nl.juiced.guhs.feature.wereldleven;

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
 * The payloads of the wereldleven feature: a note on the xylofoon (client -> server), and the grijpmachine's screen
 * (open, drop the claw, the result, stop).
 */
public final class WereldlevenPayloads {
    /** guhs:wereldleven_noot: the player hit bar {@code noot} (0-7) of the xylofoon at pos. */
    public record Noot(BlockPos pos, int noot) implements CustomPacketPayload {
        public static final Type<Noot> TYPE = new Type<>(Guhs.id("wereldleven_noot"));
        public static final StreamCodec<FriendlyByteBuf, Noot> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Noot::pos, ByteBufCodecs.VAR_INT, Noot::noot, Noot::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Noot p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                Koortje.noot(player, p.pos(), p.noot());
            }
        }
    }

    /** guhs:wereldleven_grijp_open: opens the claw screen of the machine at pos (the plushies inside). */
    public record GrijpOpen(BlockPos pos, CompoundTag prijzen) implements CustomPacketPayload {
        public static final Type<GrijpOpen> TYPE = new Type<>(Guhs.id("wereldleven_grijp_open"));
        public static final StreamCodec<FriendlyByteBuf, GrijpOpen> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, GrijpOpen::pos, ByteBufCodecs.COMPOUND_TAG, GrijpOpen::prijzen, GrijpOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(GrijpOpen p, IPayloadContext context) {
            nl.juiced.guhs.feature.wereldleven.client.WereldlevenClient.grijpOpen(p);
        }
    }

    /** guhs:wereldleven_grijp: the player drops the claw at x/z (0..1 over the floor of the case). */
    public record Grijp(BlockPos pos, float x, float z) implements CustomPacketPayload {
        public static final Type<Grijp> TYPE = new Type<>(Guhs.id("wereldleven_grijp"));
        public static final StreamCodec<FriendlyByteBuf, Grijp> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Grijp::pos, ByteBufCodecs.FLOAT, Grijp::x, ByteBufCodecs.FLOAT, Grijp::z, Grijp::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Grijp p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                Grijpmachine.grijp(player, p.pos(), p.x(), p.z());
            }
        }
    }

    /** guhs:wereldleven_grijp_uitslag: what the claw caught (index -1: nothing under it), and the case afterwards. */
    public record GrijpUitslag(BlockPos pos, int index, boolean gepakt, String knuffel, CompoundTag prijzen) implements CustomPacketPayload {
        public static final Type<GrijpUitslag> TYPE = new Type<>(Guhs.id("wereldleven_grijp_uitslag"));
        public static final StreamCodec<FriendlyByteBuf, GrijpUitslag> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, GrijpUitslag::pos, ByteBufCodecs.VAR_INT, GrijpUitslag::index, ByteBufCodecs.BOOL, GrijpUitslag::gepakt,
                ByteBufCodecs.STRING_UTF8, GrijpUitslag::knuffel, ByteBufCodecs.COMPOUND_TAG, GrijpUitslag::prijzen, GrijpUitslag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(GrijpUitslag p, IPayloadContext context) {
            nl.juiced.guhs.feature.wereldleven.client.WereldlevenClient.grijpUitslag(p);
        }
    }

    /** guhs:wereldleven_grijp_stop: the player closed the claw screen without dropping. */
    public record GrijpStop(BlockPos pos) implements CustomPacketPayload {
        public static final Type<GrijpStop> TYPE = new Type<>(Guhs.id("wereldleven_grijp_stop"));
        public static final StreamCodec<FriendlyByteBuf, GrijpStop> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, GrijpStop::pos, GrijpStop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(GrijpStop p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                Grijpmachine.stop(player, p.pos());
            }
        }
    }

    /** guhs:wereldleven_grijp_opnieuw: "Nog een keer!" in the claw screen (another ticket, another turn). */
    public record GrijpOpnieuw(BlockPos pos) implements CustomPacketPayload {
        public static final Type<GrijpOpnieuw> TYPE = new Type<>(Guhs.id("wereldleven_grijp_opnieuw"));
        public static final StreamCodec<FriendlyByteBuf, GrijpOpnieuw> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, GrijpOpnieuw::pos, GrijpOpnieuw::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(GrijpOpnieuw p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p.pos())) < 8 * 8
                    && player.level().getBlockState(p.pos()).is(WereldlevenFeature.GRIJPMACHINE.get())) {
                Grijpmachine.start(player, p.pos());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(GrijpOpnieuw.TYPE, GrijpOpnieuw.STREAM_CODEC, GrijpOpnieuw::handle);
        registrar.playToServer(Noot.TYPE, Noot.STREAM_CODEC, Noot::handle);
        registrar.playToClient(GrijpOpen.TYPE, GrijpOpen.STREAM_CODEC, GrijpOpen::handle);
        registrar.playToServer(Grijp.TYPE, Grijp.STREAM_CODEC, Grijp::handle);
        registrar.playToClient(GrijpUitslag.TYPE, GrijpUitslag.STREAM_CODEC, GrijpUitslag::handle);
        registrar.playToServer(GrijpStop.TYPE, GrijpStop.STREAM_CODEC, GrijpStop::handle);
    }

    private WereldlevenPayloads() {
    }
}
