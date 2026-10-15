package nl.juiced.guhs.feature.einde2.zij;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * einde2 zij: the small rewards of the side places (names and lore lines: {@code item.guhs.<id>} / {@code .lore}).
 */
public final class ZijItems {
    private ZijItems() {
    }

    /** An item with one grey lore line under its name. */
    public static class Lore extends Item {
        public Lore(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Koning Bumi-guh's kristalsnoep-ring (Omashguh): lick it (use) for {@link #HAAST} ticks of Haste; it is never used up,
     * the crystal only needs {@link #RUST} ticks to taste of something again.
     */
    public static class KristalsnoepRing extends Lore {
        public static final int HAAST = 20 * 45, RUST = 20 * 300;

        public KristalsnoepRing(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel) {
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, HAAST, 0, false, true, true));
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.6f, 1.6f);
                player.getCooldowns().addCooldown(stack, RUST);
                player.sendOverlayMessage(Component.translatable("item.guhs.einde2_zij_kristalsnoep_ring.lik").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** The scale of the Leeuwschildpad-guh: whoever carries it is caught above the void of the Guheinde. */
    public static class Zegenschub extends Lore {
        public Zegenschub(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }
    }
}
