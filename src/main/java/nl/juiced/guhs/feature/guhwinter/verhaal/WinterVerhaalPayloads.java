package nl.juiced.guhs.feature.guhwinter.verhaal;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The winter story over the network: {@code guhwinter_prikbord} (to the client: the cards on the prikbord, whether you may
 * write one, your own text, and whether to open the writing card at once) and {@code guhwinter_voornemen} (to the server:
 * your resolution; the server cleans and checks it, see {@link Voornemens#opslaan}).
 */
public final class WinterVerhaalPayloads {
    /** Client: opens the prikbord (or the writing card); set by client.WinterVerhaalClient. */
    public static volatile Consumer<Prikbord> opener = p -> { };

    public record Prikbord(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Prikbord> TYPE = new Type<>(Guhs.id("guhwinter_prikbord"));
        public static final StreamCodec<FriendlyByteBuf, Prikbord> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Prikbord::data,
                Prikbord::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Voornemen(String tekst) implements CustomPacketPayload {
        public static final Type<Voornemen> TYPE = new Type<>(Guhs.id("guhwinter_voornemen"));
        public static final StreamCodec<FriendlyByteBuf, Voornemen> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(512), Voornemen::tekst,
                Voornemen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Prikbord.TYPE, Prikbord.STREAM_CODEC, (p, c) -> opener.accept(p));
        registrar.playToServer(Voornemen.TYPE, Voornemen.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                Voornemens.opslaan(sp, p.tekst());
            }
        });
    }

    private WinterVerhaalPayloads() {
    }
}
