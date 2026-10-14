package nl.juiced.guhs.feature.park;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import nl.juiced.guhs.taal.Tekst;

/**
 * 1.4.2 (Het Knabbelpark): the little statue of your own guh (the parkwachter's reward). Right-click one of your own guhs
 * with it: the guh poses and the statue takes its look (Park.poseer); the tooltip says whom it shows.
 */
public class EigenBeeldItem extends BlockItem {
    public EigenBeeldItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? null : data.copyTag();
        if (tag != null && tag.contains(BeeldBlockEntity.VARIANT)) {
            tooltip.accept(Component.translatable("block.guhs.park_eigen_beeld.van", Tekst.get(tag, BeeldBlockEntity.NAAM)).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.accept(Component.translatable("block.guhs.park_eigen_beeld.lore").withStyle(ChatFormatting.GRAY));
        }
    }
}
