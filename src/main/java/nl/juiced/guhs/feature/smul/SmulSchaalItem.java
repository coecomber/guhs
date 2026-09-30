package nl.juiced.guhs.feature.smul;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The smulschaal: the big bowl the Smulguh lends you for a game of {@link SmulGame}. It's never yours: outside a game it
 * vanishes from any inventory it ticks in, and dropped it's gone at once (the game hands a new one back).
 */
public class SmulSchaalItem extends Item {
    public SmulSchaalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && (!(entity instanceof ServerPlayer player) || !SmulGame.isPlaying(player))) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide) {
            entity.discard();
        }
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.smulschaal.lore").withStyle(ChatFormatting.GRAY));
    }
}
