package nl.juiced.guhs.feature.bank;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.menu.BankGuhMenu;

/**
 * Server -> client: something did not go into the open Bank Guh because the bank is full of it (the cap). The screen
 * shows it for a moment ("Vol! Daar past niks meer van bij"), so a refused click is never silent.
 */
public record BankVolPayload(int containerId, ItemStack item) implements CustomPacketPayload {
    public static final Type<BankVolPayload> TYPE = new Type<>(Guhs.id("bank_vol"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BankVolPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BankVolPayload::containerId,
            ItemStack.OPTIONAL_STREAM_CODEC, BankVolPayload::item,
            BankVolPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BankVolPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof BankGuhMenu menu && menu.containerId == payload.containerId()) {
            menu.setClientVol(payload.item());
        }
    }
}
