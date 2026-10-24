package nl.juiced.guhs.feature.mijnhulp;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import nl.juiced.guhs.item.PaxelItem;

/**
 * 1.7.0 "Onder de Wol" (ronde 31): the Vahoege Vadsknakker, five tools in one: pickaxe, axe and shovel (the paxel), hoe and
 * shears. It mines everything those five mine (one TOOL component with all their rules: cobweb, leaves, wool and vines as
 * fast as shears, so leaves and wool come off whole; then the paxel's and the hoe's blocks at the tier's speed), and a
 * right-click is "slim per blok": a log is stripped (axe), grass and dirt become farmland (hoe), with sneak grass becomes a
 * path (shovel), and a sheep (any {@link IShearable}: the wolkenschaapje, a mooshroom...) is sheared like with shears.
 * Guhs are no IShearable (the vads shears leave them alone too): a guh keeps its wool. Unbreakable like all vads gear.
 * Made from the vadspaxel + vahoege vadsschaar + vahoege vads-schoffel, their enchantments merged ({@link VadsknakkerRecept}).
 */
public class VadsknakkerItem extends Item {
    public VadsknakkerItem(Properties properties) {
        super(properties);
    }

    /** The paxel's properties (attributes, enchantability) with one TOOL component for all five tools. */
    public static Properties properties(Properties props, ToolMaterial material, float attackDamage, float attackSpeed) {
        props = PaxelItem.properties(props, material, attackDamage, attackSpeed);
        HolderGetter<Block> blokken = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        List<Tool.Rule> regels = new ArrayList<>();
        regels.add(Tool.Rule.deniesDrops(blokken.getOrThrow(material.incorrectBlocksForDrops())));
        // the shears' rules first (the first rule with a speed wins): leaves and wool as fast as shears
        regels.add(Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0f));
        regels.add(Tool.Rule.overrideSpeed(blokken.getOrThrow(BlockTags.LEAVES), 15.0f));
        regels.add(Tool.Rule.overrideSpeed(blokken.getOrThrow(BlockTags.WOOL), Math.max(5.0f, material.speed())));
        regels.add(Tool.Rule.overrideSpeed(HolderSet.direct(Blocks.VINE.builtInRegistryHolder(), Blocks.GLOW_LICHEN.builtInRegistryHolder()), 2.0f));
        // then the paxel (pickaxe + axe + shovel) and the hoe
        regels.add(Tool.Rule.minesAndDrops(blokken.getOrThrow(PaxelItem.MINEABLE), material.speed()));
        regels.add(Tool.Rule.minesAndDrops(blokken.getOrThrow(BlockTags.MINEABLE_WITH_HOE), material.speed()));
        return props.component(DataComponents.TOOL, new Tool(regels, 1.0f, 1, true));
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(ability) || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability)
                || ItemAbilities.DEFAULT_HOE_ACTIONS.contains(ability) || ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(ability);
    }

    /**
     * Slim per blok. Sneaking: path first (shovel), then strip, then till. Otherwise: strip (axe: logs, copper), till (hoe:
     * grass and dirt), and only then a path (shovel: what can't be tilled, e.g. podzol or mycelium) or putting out a campfire.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player p = context.getPlayer();
        Item[] volgorde = p != null && p.isSecondaryUseActive()
                ? new Item[]{Items.DIAMOND_SHOVEL, Items.DIAMOND_AXE, Items.DIAMOND_HOE}
                : new Item[]{Items.DIAMOND_AXE, Items.DIAMOND_HOE, Items.DIAMOND_SHOVEL};
        for (Item gereedschap : volgorde) {
            InteractionResult r = gereedschap.useOn(context);
            if (r.consumesAction()) {
                return r;
            }
        }
        return InteractionResult.PASS;
    }

    /** Shears on an animal (NeoForge's ShearsItem#interactLivingEntity: any IShearable). Unbreakable: nothing wears off. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof IShearable doel) {
            BlockPos pos = entity.blockPosition();
            if (doel.isShearable(player, stack, entity.level(), pos)) {
                List<ItemStack> buit = doel.onSheared(player, stack, entity.level(), pos);
                if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    for (ItemStack s : buit) {
                        doel.spawnShearedDrop(sl, pos, s);
                    }
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                }
                entity.gameEvent(GameEvent.SHEAR, player);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.vahoege_vadsknakker.lore").withStyle(ChatFormatting.GRAY));
    }
}
