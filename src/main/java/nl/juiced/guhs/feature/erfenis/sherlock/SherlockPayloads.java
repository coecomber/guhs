package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The Detectivebureau's screen: the server sends the whole state ({@link Bord}: the Speurboekje and maybe one board, see
 * Sherlock.staat) and the client only sends intents ({@link BordActie}: start, bord, boekje, pin, los, wijs).
 */
public final class SherlockPayloads {
    /** Client hook (set by SherlockClient). */
    public static Consumer<Bord> bordOntvanger = b -> {
    };

    public record Bord(CompoundTag staat) implements CustomPacketPayload {
        public static final Type<Bord> TYPE = new Type<>(Guhs.id("sherlock_bord"));
        public static final StreamCodec<FriendlyByteBuf, Bord> STREAM_CODEC = StreamCodec.ofMember(Bord::write, Bord::new);

        private Bord(FriendlyByteBuf buf) {
            this(buf.readNbt());
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeNbt(staat);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BordActie(String soort, String a, String b) implements CustomPacketPayload {
        public static final Type<BordActie> TYPE = new Type<>(Guhs.id("sherlock_bord_actie"));
        public static final StreamCodec<FriendlyByteBuf, BordActie> STREAM_CODEC = StreamCodec.ofMember(BordActie::write, BordActie::new);

        private BordActie(FriendlyByteBuf buf) {
            this(buf.readUtf(16), buf.readUtf(64), buf.readUtf(64));
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(soort, 16);
            buf.writeUtf(a, 64);
            buf.writeUtf(b, 64);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Bord.TYPE, Bord.STREAM_CODEC, (b, c) -> bordOntvanger.accept(b));
        registrar.playToServer(BordActie.TYPE, BordActie.STREAM_CODEC, SherlockPayloads::opActie);
    }

    private static void opActie(BordActie a, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer p) {
            Sherlock.bordActie(p, a.soort(), a.a(), a.b());
        }
    }

    private SherlockPayloads() {
    }
}
