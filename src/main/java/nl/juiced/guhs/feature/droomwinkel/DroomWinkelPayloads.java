package nl.juiced.guhs.feature.droomwinkel;

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

/** The droomwinkeltje's two messages: the screen's contents (server → client) and a buy (client → server). */
public final class DroomWinkelPayloads {
    /** (Client) opens or refreshes the shop screen; set by client.DroomWinkelClient. */
    public static volatile Consumer<Open> opener = p -> { };

    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("droomwinkel_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            opener.accept(p);
        }
    }

    public record Koop(String id) implements CustomPacketPayload {
        public static final Type<Koop> TYPE = new Type<>(Guhs.id("droomwinkel_koop"));
        public static final StreamCodec<FriendlyByteBuf, Koop> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(96), Koop::id, Koop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Koop p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> DroomWinkel.opKoop(sp, p.id()));
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Koop.TYPE, Koop.STREAM_CODEC, Koop::handle);
    }

    private DroomWinkelPayloads() {
    }
}
