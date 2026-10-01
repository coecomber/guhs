package nl.juiced.guhs.feature.guhwaii;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** Guhwai'i's payloads: guhs:guhwaii_scan (server -> client: open the vadsigheid-scanner's big screen for this guh). */
public final class GuhwaiiPayloads {
    /** The scanner measured a guh: its entity id (for the picture), its name and its kind (variant name). */
    /** 1.2.0: name and kind as Components (resolved in the player's own language). */
    public record Scan(int guhId, net.minecraft.network.chat.Component naam, net.minecraft.network.chat.Component soort) implements CustomPacketPayload {
        public static final Type<Scan> TYPE = new Type<>(Guhs.id("guhwaii_scan"));
        public static final StreamCodec<FriendlyByteBuf, Scan> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Scan::guhId, net.minecraft.network.chat.ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC, Scan::naam,
                net.minecraft.network.chat.ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC, Scan::soort, Scan::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Scan payload, IPayloadContext context) {
            context.enqueueWork(() -> nl.juiced.guhs.feature.guhwaii.client.GuhwaiiClient.scan(payload));
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Scan.TYPE, Scan.STREAM_CODEC, Scan::handle);
    }

    private GuhwaiiPayloads() {
    }
}
