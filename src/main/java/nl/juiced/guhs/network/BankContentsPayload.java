package nl.juiced.guhs.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.menu.BankGuhMenu;
import nl.juiced.guhs.storage.BankContents;

/** Server -> client: the current contents of the open Bank Guh. */
public record BankContentsPayload(int containerId, BankContents contents) implements CustomPacketPayload {
    public static final Type<BankContentsPayload> TYPE = new Type<>(Guhs.id("bank_contents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BankContentsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BankContentsPayload::containerId,
            BankContents.STREAM_CODEC, BankContentsPayload::contents,
            BankContentsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BankContentsPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof BankGuhMenu menu && menu.containerId == payload.containerId()) {
            menu.setClientContents(payload.contents());
        }
    }
}
