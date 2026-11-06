package nl.juiced.guhs.feature.vadsrijk.geluid;

import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.juiced.guhs.Guhs;

/**
 * T-GELUID: one small packet. The server says "play this" (a stinger after a quest / level / boss / new area, or a dev preview);
 * the client plays it with ITS volume sliders. The client sets the receiver ({@code geluid.client.GeluidClient}).
 * <pre>
 *   soort = "stinger" | "gebied" | "baas" | "bed" | "stop" | "nu" | "info"     naam = the track (or "")
 * </pre>
 */
public final class GeluidPayloads {
    public record Speel(String soort, String naam) implements CustomPacketPayload {
        public static final Type<Speel> TYPE = new Type<>(Guhs.id("vr_geluid_speel"));
        public static final StreamCodec<ByteBuf, Speel> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Speel::soort,
                ByteBufCodecs.STRING_UTF8, Speel::naam, Speel::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static volatile Consumer<Speel> speel = s -> {
    };

    private GeluidPayloads() {
    }
}
