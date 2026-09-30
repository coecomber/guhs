package nl.juiced.guhs.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import nl.juiced.guhs.Guhs;

/**
 * A paxel: pickaxe, axe and shovel in one. Mines everything in {@code #guhs:mineable/paxel} (the pickaxe, axe and
 * shovel tags), and right-click strips logs like an axe or makes paths like a shovel.
 */
public class PaxelItem extends DiggerItem {
    public static final TagKey<Block> MINEABLE = BlockTags.create(Guhs.id("mineable/paxel"));

    public PaxelItem(Tier tier, Properties properties) {
        super(tier, MINEABLE, properties);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(ability) || ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult asAxe = Items.DIAMOND_AXE.useOn(context);
        return asAxe.consumesAction() ? asAxe : Items.DIAMOND_SHOVEL.useOn(context);
    }
}
