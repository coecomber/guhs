package nl.juiced.guhs.feature.meppen;

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
 * The Mika-mephamer: a soft pink mallet the Mepguh lends you for one game of Mika meppen. It only exists while you play:
 * in the inventory of anyone who isn't playing it disappears, and as a dropped item it vanishes at once.
 */
public class MepHamerItem extends Item {
    public MepHamerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player && !MepGame.isPlaying(player)) {
            MepGame.takeBack(player);                 // (all mallets gone, and the item it replaced comes back)
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.mika_mep_hamer.lore").withStyle(ChatFormatting.GRAY));
    }
}
