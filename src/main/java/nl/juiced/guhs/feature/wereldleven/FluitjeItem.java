package nl.juiced.guhs.feature.wereldleven;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** The guh-fluitje: whistles your last song (Koortje.fluit); every guh around sings along. */
public class FluitjeItem extends Item {
    public static final int COOLDOWN = 20 * 5;

    public FluitjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            Koortje.fluit(sp);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.guh_fluitje.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
