package nl.juiced.guhs.feature.guheinde;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * De Guhvleugels (the elytra of the Guheinde): little Enderguh wings, only found on the vetschip at a Mika-vesting on
 * the outer islands. Glide like with an elytra; mend them with Mika's vet.
 * (1.1.0: gliding, wearing and mending come from the GLIDER / EQUIPPABLE / REPAIRABLE components set in GuheindeFeature;
 * the wings on your back are the equipment asset guhs:guhvleugels, drawn by vanilla's wings layer.)
 */
public class GuhvleugelsItem extends Item {
    public GuhvleugelsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.guhvleugels.lore").withStyle(ChatFormatting.GRAY));
    }
}
