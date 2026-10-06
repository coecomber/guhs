package nl.juiced.guhs.feature.guhpixel.kantoor;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
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
 * The Guhkantoor over the network: {@code guhs:guhkantoor_stand} (to the client: open or refresh the Prikklok screen with
 * {@link Kantoor#stand}) and {@code guhs:guhkantoor_actie} (to the server: refresh, clock in, clock out, collect; checked
 * in {@link Kantoor#actie}). The client handler is set by client.KantoorClient; the server never calls it.
 */
public final class KantoorPayloads {
    public static volatile Consumer<Stand> standOntvanger = p -> { };

    public record Stand(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("guhkantoor_stand"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Stand::data, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand p, IPayloadContext context) {
            standOntvanger.accept(p);
        }
    }

    public record Actie(BlockPos klok, int actie, int bureau, UUID guh) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("guhkantoor_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Actie::klok,
                ByteBufCodecs.VAR_INT, Actie::actie, ByteBufCodecs.VAR_INT, Actie::bureau, UUIDUtil.STREAM_CODEC, Actie::guh, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Actie p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                Kantoor.actie(sp, p.klok(), p.actie(), p.bureau(), p.guh());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
        registrar.playToServer(Actie.TYPE, Actie.STREAM_CODEC, Actie::handle);
    }

    private KantoorPayloads() {
    }
}
