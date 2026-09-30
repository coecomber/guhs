package nl.juiced.guhs.feature.spelen;

import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** The network messages of the shared 2.9 framework (package spelen): guhs:spelgroepen_data. */
public final class SpelenPayloads {
    /** Server -> client: the groups of the Minigames tab this player has visited (ids, see {@link SpelGroepen}). */
    public record SpelgroepenData(List<String> bezocht) implements CustomPacketPayload {
        public static final Type<SpelgroepenData> TYPE = new Type<>(Guhs.id("spelgroepen_data"));
        public static final StreamCodec<FriendlyByteBuf, SpelgroepenData> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(SpelgroepenData::new, SpelgroepenData::bezocht).cast();

        public SpelgroepenData {
            bezocht = List.copyOf(bezocht);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SpelgroepenData payload, IPayloadContext context) {
            SpelGroepen.Client.set(payload.bezocht());
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(SpelgroepenData.TYPE, SpelgroepenData.STREAM_CODEC, SpelgroepenData::handle);
    }

    private SpelenPayloads() {
    }
}
