package nl.juiced.guhs.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * The Vahoege-vadsschaar (1.3.1): shears of vahoege vads that never break. It IS a {@link ShearsItem} with the vanilla
 * shears tool rules, so everything that works with shears works with it: sheep and the other shearable animals
 * (IShearable, through ShearsItem.interactLivingEntity), leaves, cobweb, wool and vines, pumpkins, tripwire, beehives
 * (the ItemAbilities.DEFAULT_SHEARS_ACTIONS of ShearsItem.canPerformAction) and dispensers ({@link #registerDispenser}).
 * No durability at all (no MAX_DAMAGE, so no bar) and the UNBREAKABLE component like the other vads gear.
 */
public class VadsSchaarItem extends ShearsItem {
    public VadsSchaarItem(Properties properties) {
        super(properties);
    }

    /** No durability, unbreakable, the tool rules of vanilla shears. */
    public static Properties properties(Properties props) {
        return props.stacksTo(1).component(DataComponents.UNBREAKABLE, Unit.INSTANCE).component(DataComponents.TOOL, ShearsItem.createToolProperties());
    }

    /** (Common setup) dispensers use it like vanilla shears. */
    public static void registerDispenser() {
        DispenserBlock.registerBehavior(nl.juiced.guhs.registry.ModItems.VADS_SHEARS.get(), new net.minecraft.core.dispenser.ShearsDispenseItemBehavior());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.vahoege_vads_shears.lore").withStyle(ChatFormatting.GRAY));
    }
}
