package nl.juiced.guhs.feature.beroepen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** The beroepen's small items (lang: the item's key + ".lore"). */
public final class BeroepenItems {
    /** A block item with a line of lore. */
    public static class LoreBlock extends BlockItem {
        public LoreBlock(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * The kaasmelkdrankje (the Apotheek's mengketel): for Snotje - but you may drink one yourself too: no more snotneus
     * (every bad effect gone) and a moment of vahoegheid (a little regeneration). The bottle stays.
     */
    public static class Drankje extends Item {
        public Drankje(Properties properties) {
            super(properties);
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity entity) {
            return 32;
        }

        @Override
        public ItemUseAnimation getUseAnimation(ItemStack stack) {
            return ItemUseAnimation.DRINK;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!level.isClientSide()) {
                entity.removeAllEffects();                     // 26.1: NeoForge effect cures are gone; milk clears all effects
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
                if (entity instanceof Player p) {
                    p.sendOverlayMessage(Component.translatable("item.guhs.kaasmelkdrankje.gedronken").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            if (entity instanceof Player p && p.getAbilities().instabuild) {
                return stack;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            if (entity instanceof Player p) {
                p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }
            return stack;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.kaasmelkdrankje.lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** A snotkruidje: sniff it (right-click) and... HATSJOE! */
    public static class Snotkruidje extends Item {
        public Snotkruidje(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level instanceof ServerLevel server) {
                server.playSound(null, player, BeroepenFeature.HATSJOE.get(), SoundSource.PLAYERS, 0.8f, 0.8f + level.getRandom().nextFloat() * 0.2f);
                var look = player.getLookAngle();
                server.sendParticles(ParticleTypes.SNEEZE, player.getX() + look.x * 0.6, player.getEyeY() - 0.1, player.getZ() + look.z * 0.6, 8,
                        0.1, 0.05, 0.1, 0.03);
            }
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 40);
            return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.snotkruidje.lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private BeroepenItems() {
    }
}
