package nl.juiced.guhs.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import nl.juiced.guhs.registry.ModDataComponents;
import nl.juiced.guhs.storage.BankContents;

/** The Bank Guh item: lore + how much is inside its stomach. */
public class BankGuhItem extends BlockItem {
    public BankGuhItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.guhs.bank_guh.lore").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        BankContents contents = stack.getOrDefault(ModDataComponents.BANK_CONTENTS.get(), BankContents.EMPTY);
        if (!contents.isEmpty()) {
            tooltip.add(Component.translatable("block.guhs.bank_guh.contents", contents.totalItems(), contents.entries().size())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
