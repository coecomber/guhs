package nl.juiced.guhs.feature.blub;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A piece of Blub-kleding (1.8.0 extension): right-click and it is used up; the piece hangs in the Blub-Kledingkast of all
 * your Blubs for good ({@link BlubKledingUnlocks}). Already unlocked: nothing is used up (give it to a friend).
 */
public class BlubKledingItem extends Item {
    private final String stuk;

    public BlubKledingItem(String stuk, Properties properties) {
        super(properties);
        this.stuk = stuk;
    }

    public String stuk() {
        return stuk;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlubKleding.Stuk s = BlubKleding.stuk(stuk);
        if (s != null && player instanceof ServerPlayer sp && BlubKledingUnlocks.ontgrendel(sp, s)) {
            stack.consume(1, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.blubkleding_" + stuk + ".lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.blubkleding.gebruik").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
