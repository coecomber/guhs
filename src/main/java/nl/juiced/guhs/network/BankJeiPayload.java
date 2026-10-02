package nl.juiced.guhs.network;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.menu.BankGuhMenu;

/**
 * 1.2.5, client -> server: JEI's "+" in the Bank Guh. Per crafting grid slot (9) the items that may go there (empty list:
 * nothing); the server takes them from the bank first, then from the player's inventory. max: fill whole stacks (shift).
 */
public record BankJeiPayload(int containerId, List<List<ItemStack>> slots, boolean max) implements CustomPacketPayload {
    public static final Type<BankJeiPayload> TYPE = new Type<>(Guhs.id("bank_jei"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BankJeiPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BankJeiPayload::containerId,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(64)).apply(ByteBufCodecs.list(9)), BankJeiPayload::slots,
            ByteBufCodecs.BOOL, BankJeiPayload::max,
            BankJeiPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BankJeiPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof BankGuhMenu menu && menu.containerId == payload.containerId()
                && menu.stillValid(context.player())) {
            menu.vulGrid(payload.slots(), payload.max());
        }
    }
}
