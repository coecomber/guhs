package nl.juiced.guhs.feature.kaasmoeras;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A vadsverdrijvend drankje: throw it like a splash potion (it makes whoever it hits a little onvahoeg). */
public class VadsverdrijvendDrankjeItem extends Item {
    public VadsverdrijvendDrankjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!level.isClientSide) {
            VadsverdrijvendDrankjeEntity drankje = new VadsverdrijvendDrankjeEntity(level, player);
            drankje.setItem(stack.copyWithCount(1));
            drankje.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0f, 0.5f, 1.0f);
            level.addFreshEntity(drankje);
        }
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.vadsverdrijvend_drankje.lore").withStyle(ChatFormatting.GRAY));
    }
}
