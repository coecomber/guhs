package nl.juiced.guhs.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.registry.ModDataComponents;
import nl.juiced.guhs.storage.BankContents;
import nl.juiced.guhs.storage.BankStorage;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The Bank Guh item: lore, the cap (bbq2: 256 of one kind, or the upgrade that takes the cap away), how much is inside its
 * stomach and how full its fullest kind is ("n/256").
 */
public class BankGuhItem extends BlockItem {
    public BankGuhItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        boolean opgevoerd = stack.getOrDefault(BankFeature.BANK_OPGEVOERD.get(), false);
        tooltip.accept(Component.translatable("block.guhs.bank_guh.lore", BankStorage.CAP).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        if (opgevoerd) {
            tooltip.accept(Component.translatable("block.guhs.bank_guh.opgevoerd").withStyle(ChatFormatting.GOLD));
        }
        BankContents contents = stack.getOrDefault(ModDataComponents.BANK_CONTENTS.get(), BankContents.EMPTY);
        if (!contents.isEmpty()) {
            tooltip.accept(Component.translatable("block.guhs.bank_guh.contents", contents.totalItems(), contents.entries().size())
                    .withStyle(ChatFormatting.GRAY));
            if (!opgevoerd) {
                long volste = contents.entries().stream().mapToLong(BankContents.Entry::count).max().orElse(0);
                tooltip.accept(Component.translatable("block.guhs.bank_guh.volste", volste, BankStorage.CAP)
                        .withStyle(volste >= BankStorage.CAP ? ChatFormatting.RED : ChatFormatting.GRAY));
            }
        }
    }
}
