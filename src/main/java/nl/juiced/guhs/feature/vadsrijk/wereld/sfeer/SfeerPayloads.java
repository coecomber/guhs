package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer;

import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.juiced.guhs.Guhs;

/**
 * The few packets of the atmosphere (everything else follows the day clock, {@link SfeerKlok}). The client sets the receivers
 * ({@code sfeer.client.SfeerClient}), so the server never loads client classes.
 */
public final class SfeerPayloads {
    /** This player's flags: beat the Opperzwam (the light pillar from the dome). */
    public record Vlaggen(boolean opperzwam) implements CustomPacketPayload {
        public static final Type<Vlaggen> TYPE = new Type<>(Guhs.id("vr_wereld_sfeer_vlaggen"));
        public static final StreamCodec<ByteBuf, Vlaggen> STREAM_CODEC = ByteBufCodecs.BOOL.map(Vlaggen::new, Vlaggen::opperzwam);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Dev: force a moment now ("golf", "storm", "bui", "noorderlicht", "stop"). */
    public record Forceer(String wat) implements CustomPacketPayload {
        public static final Type<Forceer> TYPE = new Type<>(Guhs.id("vr_wereld_sfeer_forceer"));
        public static final StreamCodec<ByteBuf, Forceer> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Forceer::new, Forceer::wat);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** A dragon pass starts: the flyover entity and how long it lasts (the client does shadow, wind, lanterns, grey flowers). */
    public record DraakPas(int draak, int ticks) implements CustomPacketPayload {
        public static final Type<DraakPas> TYPE = new Type<>(Guhs.id("vr_wereld_sfeer_draakpas"));
        public static final StreamCodec<ByteBuf, DraakPas> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, DraakPas::draak,
                ByteBufCodecs.VAR_INT, DraakPas::ticks, DraakPas::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static volatile Consumer<Vlaggen> vlaggen = v -> {
    };
    public static volatile Consumer<Forceer> forceer = v -> {
    };
    public static volatile Consumer<DraakPas> draakPas = v -> {
    };

    private SfeerPayloads() {
    }
}
