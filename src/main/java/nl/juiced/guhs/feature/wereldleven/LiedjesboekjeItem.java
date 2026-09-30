package nl.juiced.guhs.feature.wereldleven;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** The liedjesboekje: the six songs of the guh-xylofoon, with their bars (the xylofoon screen without a xylofoon). */
public class LiedjesboekjeItem extends Item {
    public LiedjesboekjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            WereldlevenFeature.openXylofoon.accept(null);
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.wereldleven_liedjesboekje.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
