package nl.juiced.guhs.feature.elftocht;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * Warme chocovet and a kommetje snert (from the koek-en-zopie stalls, {@link KopjesBlock}): drink it (quick) and you're
 * warm inside: a short speed boost (Speed II for {@link #BOOST_TICKS} ticks) and a little cloud of warmth. VAHOEG!
 */
public class WarmDrankjeItem extends Item {
    /** How long the warmth boost lasts. */
    public static final int BOOST_TICKS = 20 * 8;

    public WarmDrankjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
            server.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.9, player.getZ(), 3, 0.3, 0.2, 0.3, 0);
            server.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), player.getY() + 1.5, player.getZ(), 8, 0.25, 0.2, 0.25, 0.01);
            player.sendOverlayMessage(Component.translatable("gui.guhs.elftocht.warm").withStyle(ChatFormatting.GOLD));
        }
        return rest;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }
}
