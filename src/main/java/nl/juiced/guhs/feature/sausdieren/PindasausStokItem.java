package nl.juiced.guhs.feature.sausdieren;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Pindasaus aan een stok (the warped fungus on a stick of the Sausloper): a Sausloper follows whoever holds it, and with a
 * saddle on it goes where its rider looks. Using it while riding makes the Sausloper sprint for a while and licks a bit of
 * the pindasaus off (durability; when it is all gone a fishing rod is left).
 */
public class PindasausStokItem extends Item {
    public static final int LIK = 7;

    public PindasausStokItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (player.getControlledVehicle() instanceof SausloperEntity loper && loper.boost()) {
            ItemStack over = stack.hurtAndConvertOnBreak(LIK, Items.FISHING_ROD, player, hand.asEquipmentSlot());
            loper.playSound(SausdierenFeature.SAUSLOPER_BLIJ.get(), 0.8f, 1.3f);
            return InteractionResult.SUCCESS_SERVER.heldItemTransformedTo(over);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }
}
