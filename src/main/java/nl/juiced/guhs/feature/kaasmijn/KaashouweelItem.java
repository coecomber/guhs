package nl.juiced.guhs.feature.kaasmijn;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The cheese pickaxe (from the Mijnguh, for goudkaas and kaasbrokken): your own pickaxe, iron level but faster and
 * tougher, and twice as fast again on cheese veins. Repaired with kaasbrokken.
 */
public class KaashouweelItem extends Item {
    /** 1.1.0: a ToolMaterial (was a SimpleTier); repaired with kaasbrokken via {@code repairable(KAASBROK)} in the properties. */
    public static final ToolMaterial TIER = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 1400, 7.5f, 2.5f, 16, ItemTags.IRON_TOOL_MATERIALS);

    public KaashouweelItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return state.getBlock() instanceof KaasaderBlock ? speed * 2 : speed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }
}
