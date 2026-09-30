package nl.juiced.guhs.feature.elftocht;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
/**
 * Guh-schaatsen, lent by Schaatsmeester Guhglij (for the tour or for free skating). Hold them (in either hand) and step
 * onto the ice: you glide fast, with a skater's sway and the hiss of the blades ({@link ElftochtSchaatsen}); off the ice
 * you just walk. They can't be dropped or stored and go back to Guhglij as soon as you stop skating.
 */
public class SchaatsenItem extends Item {
    public SchaatsenItem(Properties properties) {
        super(properties);
    }

    /** Only skaters keep their skates. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot equipSlot) {
        if (!level.isClientSide() && (!(entity instanceof Player player) || !ElftochtTocht.isBezig(player))) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.discard();
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.guh_schaatsen.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.guh_schaatsen.how").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
