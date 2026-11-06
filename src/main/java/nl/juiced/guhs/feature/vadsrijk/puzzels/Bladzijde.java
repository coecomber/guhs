package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * T-PUZZELS: a "Gescheurde bladzijde" (1..9): a piece of the legend of the Lege Schaal (WERELDVERHAAL 2.2), the lore about the
 * Grauwe Keizer-Mika. Right-click reads it (chat). The nine together are the whole story; they lead to the reveal in Hoogvads.
 */
public class Bladzijde extends Item {
    public final int nummer;

    public Bladzijde(Properties p, int nummer) {
        super(p);
        this.nummer = nummer;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer p) {
            lees(p, nummer);
        }
        return InteractionResult.SUCCESS;
    }

    public static void lees(ServerPlayer p, int n) {
        p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.bladzijde.kop", n, Component.translatable("vadsrijk.puzzels.bladzijde." + n + ".titel"))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.bladzijde." + n + ".tekst").withStyle(ChatFormatting.ITALIC));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("vadsrijk.puzzels.bladzijde." + nummer + ".titel").withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("vadsrijk.puzzels.bladzijde.uitleg").withStyle(ChatFormatting.GRAY));
    }
}
