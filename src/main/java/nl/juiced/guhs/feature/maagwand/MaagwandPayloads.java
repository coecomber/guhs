package nl.juiced.guhs.feature.maagwand;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** The client's copy of its maagwand. */
public final class MaagwandPayloads {
    public record Sync(int laag, int teller) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(Guhs.id("maagwand_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Sync::laag,
                ByteBufCodecs.VAR_INT, Sync::teller, Sync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Sync p, IPayloadContext context) {
            Maagwand.klantLaag = p.laag();
            Maagwand.klantTeller = p.teller();
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Sync.TYPE, Sync.STREAM_CODEC, Sync::handle);
    }

    private MaagwandPayloads() {
    }
}
