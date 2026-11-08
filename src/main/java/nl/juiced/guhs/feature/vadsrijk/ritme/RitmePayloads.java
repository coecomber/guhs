package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * U-LEVEN-RITME packets. The weather follows the day clock ({@link WeerKlok}) on every client, so there is only the dev packet
 * that forces a weather moment and the forced night/day (the client's lights and fireflies follow the server's forced schedule).
 */
public final class RitmePayloads {
    /** Dev: force a weather moment now ("bui", "regenboog", "gloed", "mist", "stop") or the schedule ("nacht", "dag", "auto"). */
    public record Forceer(String wat) implements CustomPacketPayload {
        public static final Type<Forceer> TYPE = new Type<>(Guhs.id("vr_uritme_forceer"));
        public static final StreamCodec<ByteBuf, Forceer> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Forceer::new, Forceer::wat);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client ({@code ritme.client.RitmeClient}), so the server never loads client classes. */
    public static volatile Consumer<Forceer> forceer = v -> {
    };

    private RitmePayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Forceer.TYPE, Forceer.STREAM_CODEC, (v, c) -> forceer.accept(v));
    }
}
