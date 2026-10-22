package nl.juiced.guhs.feature.safari.camera;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * The safari camera (S1, design 1.7): hold right-click to look through it (it zooms like a spyglass, with a photo frame
 * instead of the round scope, see the client side), let go to take the picture. A quick click without zooming is a snap
 * with a wide picture. The photo itself is judged on the server ({@link Fotograaf}).
 */
public class SafariCameraItem extends Item {
    /** Held at least this long (ticks): a zoomed photo (narrow and far), else a snap (wide and close). */
    static final int ZOOM_NA = 6;
    private static final int DUUR = 72000;

    public SafariCameraItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return DUUR;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPYGLASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.playSound(SoundEvents.SPYGLASS_USE, 1.0f, 1.3f);
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (entity instanceof ServerPlayer p) {
            Fotograaf.klik(p, DUUR - remaining >= ZOOM_NA, this instanceof Goud);
        }
        return true;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return stack;
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility ability) {
        return ability == ItemAbilities.SPYGLASS_SCOPE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.safaricamera.uitleg").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.safaricamera.uitleg2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** G1 (design 1.8): the golden safari camera of the full dex. Works like the camera; its photos get golden frames. */
    public static class Goud extends SafariCameraItem {
        public Goud(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.gouden_safaricamera.lore").withStyle(ChatFormatting.GOLD));
            super.appendHoverText(stack, context, display, tooltip, flag);
        }
    }
}
