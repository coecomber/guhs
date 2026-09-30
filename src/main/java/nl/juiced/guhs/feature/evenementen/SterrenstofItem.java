package nl.juiced.guhs.feature.evenementen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Sterrenstof, left behind by a falling star: throw it up in the air and make a wish (you see in the dark for a while). */
public class SterrenstofItem extends Item {
    public static final int NIGHT_VISION = 20 * 90;

    public SterrenstofItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            stack.consume(1, player);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION, 0, false, false, true));
            ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 2.2, player.getZ(), 20, 0.4, 0.4, 0.4, 0.06);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.6f);
            player.displayClientMessage(Component.translatable("item.guhs.sterrenstof.wish").withStyle(ChatFormatting.AQUA), true);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.sterrenstof.lore").withStyle(ChatFormatting.GRAY));
    }
}
