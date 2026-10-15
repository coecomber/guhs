package nl.juiced.guhs.feature.ring;

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
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.ModDimensions;

/**
 * bbq2 (ring-kern): De Knabbelring, a ring-shaped knabbel that makes everybody greedy. One per player, given by the story
 * ({@link Ring#geef}); it is a loaned item (never goes into storage) and Sam-guh hands it back when it got lost.
 * <ul>
 *   <li>In your pockets: it whispers that you could just eat it, wild guhs follow you drooling, and it gets heavier near
 *       the Frituurberg ({@link RingEvents}, {@link Ring#zwaarte}). You can't eat it: it has to be fried and shared.</li>
 *   <li>Use it: you put it on or take it off ({@link Ring#doeOm}). On: nobody sees you and no Mika can pick you as its
 *       target, but the Eye of Sausron sees you all the better and in the Barbecuether the Nine come ({@link Negen}).</li>
 * </ul>
 */
public class KnabbelringItem extends Item {
    public KnabbelringItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer p) {
            boolean om = Ring.doeOm(p, !Ring.om(p));
            p.getCooldowns().addCooldown(stack, 20);
            if (om && p.level().dimension() == ModDimensions.GUHMENSION && Sam.van(p) != null) {
                GuhQuests.say(p, Sam.van(p), "quest.guhs.ring.sam.niet_omdoen");
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore2").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore3").withStyle(ChatFormatting.DARK_RED));
    }
}
