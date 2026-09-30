package nl.juiced.guhs.feature.mewtwo;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** The kloon-eiland's block items: a grey lore line under the name (lang key {@code <item>.lore}, when there is one). */
public final class MewtwoItems {
    private MewtwoItems() {
    }

    public static class LoreBlock extends BlockItem {
        public LoreBlock(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            String key = getDescriptionId() + ".lore";
            if (Language.getInstance().has(key)) {
                tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
