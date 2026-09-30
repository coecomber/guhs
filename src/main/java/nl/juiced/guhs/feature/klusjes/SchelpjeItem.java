package nl.juiced.guhs.feature.klusjes;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Een schelpje (klusjes): a shiny pink shell that huisje guhs fish up (chore "vissen") and sometimes dig up. Hold it to
 * your ear (right-click) and you hear the Guhzee, and a little guh thought. Crafted into bone meal.
 */
public class SchelpjeItem extends Item {
    /** How many ear lines there are (item.guhs.klusjes_schelpje.oor.0 ..). */
    public static final int OOR_ZINNEN = 6;

    public SchelpjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.blockPosition(), KlusjesFeature.ZEE.get(), SoundSource.PLAYERS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
            player.displayClientMessage(Component.translatable("item.guhs.klusjes_schelpje.oor." + level.random.nextInt(OOR_ZINNEN))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.klusjes_schelpje.lore").withStyle(ChatFormatting.GRAY));
    }
}
