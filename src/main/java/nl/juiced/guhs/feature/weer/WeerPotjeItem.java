package nl.juiced.guhs.feature.weer;

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
 * A weerpotje (1.8.0): right-click (or use it on a Weer vadser) and it is used up; its guh-weertje is unlocked for you for
 * good ({@link WeerUnlocks}), in every Weer vadser. Already unlocked: nothing is used up (give it to a friend).
 */
public class WeerPotjeItem extends Item {
    private final Weertje weertje;

    public WeerPotjeItem(Weertje weertje, Properties properties) {
        super(properties);
        this.weertje = weertje;
    }

    public Weertje weertje() {
        return weertje;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp && WeerUnlocks.ontgrendel(sp, weertje)) {
            stack.consume(1, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.weerpotje_" + weertje.id() + ".lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.weerpotje.gebruik").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
