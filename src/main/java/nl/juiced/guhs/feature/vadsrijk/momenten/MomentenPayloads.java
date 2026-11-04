package nl.juiced.guhs.feature.vadsrijk.momenten;

import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** The momenten network: the recolour wave ({@link Golf}). */
public final class MomentenPayloads {
    /** Client hooks (set by MomentenClient). */
    public static Consumer<GolfKomt> komtOntvanger = g -> {
    };
    public static Consumer<GolfStart> startOntvanger = g -> {
    };

    private MomentenPayloads() {
    }

    /** "Your wave is coming": keep the scars grey (the flag is already set) until {@link GolfStart}, at most {@code ticks}. */
    public record GolfKomt(int ticks) implements CustomPacketPayload {
        public static final Type<GolfKomt> TYPE = new Type<>(Guhs.id("vr_momenten_golf_komt"));
        public static final StreamCodec<ByteBuf, GolfKomt> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(GolfKomt::new, GolfKomt::ticks);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The wave starts now (from the temple at tx/tz; the player's distance; the zone edge). */
    public record GolfStart(double tx, double tz, double afstand, double rMax) implements CustomPacketPayload {
        public static final Type<GolfStart> TYPE = new Type<>(Guhs.id("vr_momenten_golf_start"));
        public static final StreamCodec<ByteBuf, GolfStart> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.DOUBLE, GolfStart::tx,
                ByteBufCodecs.DOUBLE, GolfStart::tz, ByteBufCodecs.DOUBLE, GolfStart::afstand, ByteBufCodecs.DOUBLE, GolfStart::rMax, GolfStart::new);

        public GolfBaan baan() {
            return new GolfBaan(tx, tz, afstand, rMax);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void registreer(PayloadRegistrar r) {
        r.playToClient(GolfKomt.TYPE, GolfKomt.STREAM_CODEC, (g, c) -> komtOntvanger.accept(g));
        r.playToClient(GolfStart.TYPE, GolfStart.STREAM_CODEC, (g, c) -> startOntvanger.accept(g));
    }
}
