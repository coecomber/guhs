package nl.juiced.guhs.feature.guhpixel.blok;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * A block item with one grey tooltip line: lang {@code <description id>.lore}. Register with
 * {@code ITEMS.registerItem(id, p -> new LoreBlockItem(BLOCK.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix())}
 * and write {@code block.guhs.<id>} and {@code block.guhs.<id>.lore}.
 */
public class LoreBlockItem extends BlockItem {
    public LoreBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }
}
