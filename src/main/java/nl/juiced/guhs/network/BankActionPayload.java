package nl.juiced.guhs.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.menu.BankGuhMenu;

/** Client -> server: a click in the Bank Guh screen (take an item, put the carried item in, deposit all, ...). */
public record BankActionPayload(int containerId, int action, ItemStack item, int button, boolean shift) implements CustomPacketPayload {
    public static final Type<BankActionPayload> TYPE = new Type<>(Guhs.id("bank_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BankActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BankActionPayload::containerId,
            ByteBufCodecs.VAR_INT, BankActionPayload::action,
            ItemStack.OPTIONAL_STREAM_CODEC, BankActionPayload::item,
            ByteBufCodecs.VAR_INT, BankActionPayload::button,
            ByteBufCodecs.BOOL, BankActionPayload::shift,
            BankActionPayload::new);

    public BankActionPayload(int containerId, BankGuhMenu.Action action, ItemStack item, int button, boolean shift) {
        this(containerId, action.ordinal(), item, button, shift);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BankActionPayload payload, IPayloadContext context) {
        BankGuhMenu.Action[] actions = BankGuhMenu.Action.values();
        if (payload.action() < 0 || payload.action() >= actions.length) {
            return;
        }
        if (context.player().containerMenu instanceof BankGuhMenu menu && menu.containerId == payload.containerId()
                && menu.stillValid(context.player())) {
            menu.handleAction(actions[payload.action()], payload.item(), payload.button(), payload.shift());
        }
    }
}
