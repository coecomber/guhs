package nl.juiced.guhs.feature.einde2.zwevers;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** einde2, slice zwevers: the one message of the slice, the player's own {@link Walviszicht} flag for its client. */
public final class E2ZweversPayloads {
    public record Zicht(boolean ziet) implements CustomPacketPayload {
        public static final Type<Zicht> TYPE = new Type<>(Guhs.id("einde2_zwevers_walviszicht"));
        public static final StreamCodec<FriendlyByteBuf, Zicht> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Zicht::ziet, Zicht::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Zicht payload, IPayloadContext context) {
            Walviszicht.lokaal(payload.ziet());
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Zicht.TYPE, Zicht.STREAM_CODEC, Zicht::handle);
    }

    private E2ZweversPayloads() {
    }
}
