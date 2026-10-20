package nl.juiced.guhs.feature.safari.lobby;

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

/** The giftshop's two messages: the screen's contents (server → client) and a buy (client → server). */
public final class LobbyPayloads {
    /** (Client) opens or refreshes the giftshop screen; set by client.LobbyClient. */
    public static volatile Consumer<WinkelOpen> winkelOpener = p -> { };

    public record WinkelOpen(CompoundTag data) implements CustomPacketPayload {
        public static final Type<WinkelOpen> TYPE = new Type<>(Guhs.id("safari_winkel"));
        public static final StreamCodec<FriendlyByteBuf, WinkelOpen> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, WinkelOpen::data,
                WinkelOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(WinkelOpen p, IPayloadContext context) {
            winkelOpener.accept(p);
        }
    }

    public record Koop(String id) implements CustomPacketPayload {
        public static final Type<Koop> TYPE = new Type<>(Guhs.id("safari_koop"));
        public static final StreamCodec<FriendlyByteBuf, Koop> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(96), Koop::id, Koop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Koop p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> SafariWinkel.opKoop(sp, p.id()));
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(WinkelOpen.TYPE, WinkelOpen.STREAM_CODEC, WinkelOpen::handle);
        registrar.playToServer(Koop.TYPE, Koop.STREAM_CODEC, Koop::handle);
    }

    private LobbyPayloads() {
    }
}
