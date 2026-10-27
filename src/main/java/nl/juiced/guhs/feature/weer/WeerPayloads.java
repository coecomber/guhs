package nl.juiced.guhs.feature.weer;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0, the Weer vadser's payloads: the zones of a dimension (server to client), its menu (server to client) and a chosen
 * weather (client to server).
 */
public final class WeerPayloads {
    /** Set by the client (WeerClient): what to do with these payloads there. */
    public static Consumer<Zones> zonesOntvanger = p -> {
    };
    public static Consumer<Open> menuOpener = p -> {
    };

    private WeerPayloads() {
    }

    /** Server to client: every zone of the dimension (centres as BlockPos longs, weathers as ordinals). */
    public record Zones(List<Long> plekken, List<Integer> weertjes) implements CustomPacketPayload {
        public static final Type<Zones> TYPE = new Type<>(Guhs.id("weer_zones"));
        public static final StreamCodec<FriendlyByteBuf, Zones> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(256)), Zones::plekken,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), Zones::weertjes, Zones::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Zones p, IPayloadContext context) {
            zonesOntvanger.accept(p);
        }
    }

    /** Server to client: open the menu of the Weer vadser at pos (its weather, does it have vadskracht, the unlocked ones). */
    public record Open(BlockPos pos, int weertje, boolean kracht, List<String> ontgrendeld) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("weer_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Open::pos, ByteBufCodecs.VAR_INT, Open::weertje, ByteBufCodecs.BOOL, Open::kracht,
                ByteBufCodecs.stringUtf8(32).apply(ByteBufCodecs.list(32)), Open::ontgrendeld, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            menuOpener.accept(p);
        }
    }

    /** Client to server: this Weer vadser gets this weather. */
    public record Kies(BlockPos pos, int weertje) implements CustomPacketPayload {
        public static final Type<Kies> TYPE = new Type<>(Guhs.id("weer_kies"));
        public static final StreamCodec<FriendlyByteBuf, Kies> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Kies::pos, ByteBufCodecs.VAR_INT, Kies::weertje, Kies::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kies p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp && sp.distanceToSqr(p.pos().getCenter()) < 12 * 12
                    && sp.level().getBlockEntity(p.pos()) instanceof WeerVadserBlockEntity be) {
                be.kies(sp, Weertje.byIndex(p.weertje()));
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Zones.TYPE, Zones.STREAM_CODEC, Zones::handle);
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Kies.TYPE, Kies.STREAM_CODEC, Kies::handle);
    }
}
