package nl.juiced.guhs.feature.guhkamer;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * De Guhbel: a little golden bell with a pink bow and guh ears. Ring it (right-click) and its screen opens: send one of
 * your guhs nearby to the Guhkamer in your maag ("ga maar lekker logeren!"), or call a guest back to you, wherever you
 * are. Tingeling!
 */
public class GuhbelItem extends Item {
    public GuhbelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(player, player.blockPosition(), GuhkamerFeature.BEL.get(), SoundSource.PLAYERS, 1f, 1f);
        if (player instanceof ServerPlayer sp) {
            GuhkamerPayloads.open(sp);
        }
        player.getCooldowns().addCooldown(stack, 10);
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.guhbel.lore").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.accept(Component.translatable("item.guhs.guhbel.uitleg").withStyle(ChatFormatting.GRAY));
    }
}
