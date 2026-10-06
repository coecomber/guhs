package nl.juiced.guhs.feature.ringh3;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** bbq2 (ring-h3): the one message of the chapter: "your bridge is broken / whole again" ({@link Brug}). */
public final class RingH3Payloads {
    private RingH3Payloads() {
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(BrugKapot.TYPE, BrugKapot.STREAM_CODEC, BrugKapot::handle);
    }

    /** nul: the world position of the template block (0, 0, 0) of that copy; draai: its Rotation ordinal. */
    public record BrugKapot(BlockPos nul, int draai, boolean kapot) implements CustomPacketPayload {
        public static final Type<BrugKapot> TYPE = new Type<>(Guhs.id("ringh3_brug"));
        public static final StreamCodec<FriendlyByteBuf, BrugKapot> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, BrugKapot::nul,
                ByteBufCodecs.VAR_INT, BrugKapot::draai, ByteBufCodecs.BOOL, BrugKapot::kapot, BrugKapot::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(BrugKapot payload, IPayloadContext context) {
            nl.juiced.guhs.feature.ringh3.client.BrugBreuk.vanServer(payload.nul(), payload.draai(), payload.kapot());
        }
    }
}
