package nl.juiced.guhs.feature.einde2.spd1stad;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * einde2 "Sponsguh", slice sp_d1_stad: Patguh's kwallennetje (CONTRACT_E2_C 7.4). Patguh gives it at the scene {@code patguh}
 * and you keep it. During the jellyfishing a tap (left or right click) on a kruimelkwal with it in your hand catches that kwal
 * ({@link Kwallen}); outside the story it is a harmless net: a tap makes a kwal bob away, as a hand does.
 */
public class KwallennetItem extends Item {
    public KwallennetItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }
}
