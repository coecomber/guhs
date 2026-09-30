package nl.juiced.guhs.feature.guheinde;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import nl.juiced.guhs.registry.ModItems;

/**
 * De Guhvleugels (the elytra of the Guheinde): little Enderguh wings, only found on the vetschip at a Mika-vesting on
 * the outer islands. Glide like with an elytra; mend them with Mika's vet. Drawn on your back by GuhvleugelsLayer.
 */
public class GuhvleugelsItem extends ElytraItem {
    public GuhvleugelsItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.MIKA_VET.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.guhvleugels.lore").withStyle(ChatFormatting.GRAY));
    }
}
