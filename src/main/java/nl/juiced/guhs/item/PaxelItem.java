package nl.juiced.guhs.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import nl.juiced.guhs.Guhs;

/**
 * A paxel: pickaxe, axe and shovel in one. Mines everything in {@code #guhs:mineable/paxel} (the pickaxe, axe and
 * shovel tags), and right-click strips logs like an axe or makes paths like a shovel.
 * 26.1: DiggerItem is gone - the mining behaviour is the TOOL component set by {@link #properties}.
 */
public class PaxelItem extends Item {
    public static final TagKey<Block> MINEABLE = BlockTags.create(Guhs.id("mineable/paxel"));

    public PaxelItem(Properties properties) {
        super(properties);
    }

    /** The old {@code DiggerItem(tier, MINEABLE)} + {@code DiggerItem.createAttributes(tier, damage, speed)}. */
    public static Properties properties(Properties props, ToolMaterial material, float attackDamage, float attackSpeed) {
        return props.tool(material, MINEABLE, attackDamage, attackSpeed, 0.0f);
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability) || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult asAxe = Items.DIAMOND_AXE.useOn(context);
        return asAxe.consumesAction() ? asAxe : Items.DIAMOND_SHOVEL.useOn(context);
    }
}
