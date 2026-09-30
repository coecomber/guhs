package nl.juiced.guhs.feature.kaasmijn;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * The cheese pickaxe (from the Mijnguh, for goudkaas and kaasbrokken): your own pickaxe, iron level but faster and
 * tougher, and twice as fast again on cheese veins. Repaired with kaasbrokken.
 */
public class KaashouweelItem extends PickaxeItem {
    public static final Tier TIER = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 1400, 7.5f, 2.5f, 16,
            () -> Ingredient.of(KaasmijnFeature.KAASBROK.get()));

    public KaashouweelItem(Properties properties) {
        super(TIER, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return state.getBlock() instanceof KaasaderBlock ? speed * 2 : speed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }
}
