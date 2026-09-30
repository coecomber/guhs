package nl.juiced.guhs.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.world.MaagManager;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** Small items with a special right-click: the stomach whistle, the Guhdex and the crystal spyglass. */
public final class QuestItems {

    /** An item with a grey line of explanation under its name. */
    public static class Described extends Item {
        public Described(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The Guh-buikfluitje: to your own stomach from anywhere, and from inside the stomachs back out. */
    public static class Whistle extends Described {
        public Whistle(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer serverPlayer) {
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1f, 1.6f);
                MaagManager.whistle(serverPlayer);
            }
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 40);
            return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
        }
    }

    /** The Guhdex book. */
    public static class Dex extends Described {
        public Dex(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer serverPlayer) {
                GuhDex.open(serverPlayer);
            }
            return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
        }
    }

    /** The guh crystal spyglass: every rare guh variant within 64 blocks lights up for 30 seconds. */
    public static class Spyglass extends Described {
        public static final double RANGE = 64;

        public Spyglass(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide()) {
                List<GuhEntity> rare = level.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(RANGE),
                        g -> g.getVariant() != GuhVariant.NORMAL);
                rare.forEach(g -> g.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, false, false)));
                player.sendOverlayMessage(Component.translatable("item.guhs.guh_kristal_verrekijker.found", rare.size())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.8f);
            }
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 100);
            return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
        }
    }

    private QuestItems() {
    }
}
