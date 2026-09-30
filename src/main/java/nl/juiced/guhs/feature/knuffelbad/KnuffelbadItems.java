package nl.juiced.guhs.feature.knuffelbad;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;

/** The Knuffelbad's tools: guhshampoo (step 1 of the wash) and the guh-föhn (step 4). */
public final class KnuffelbadItems {
    /** A bottle of pink guh shampoo (16 washes): use it on your own tamed guh next to a wash tub. */
    public static class Guhshampoo extends Item {
        public Guhshampoo(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            if (!(target instanceof GuhEntity guh)) {
                return InteractionResult.PASS;
            }
            if (!player.level().isClientSide() && player instanceof ServerPlayer sp && Wasritueel.inzepen(sp, guh)) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.guhs.guhshampoo.lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The guh-föhn: hold right-click aimed at a rinsed guh in a wash tub until it's dry, fluffy and shiny. */
    public static class GuhFohn extends Item {
        public GuhFohn(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME.heldItemTransformedTo(player.getItemInHand(hand));
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity entity) {
            return 72000;
        }

        @Override
        public ItemUseAnimation getUseAnimation(ItemStack stack) {
            return ItemUseAnimation.BRUSH;
        }

        @Override
        public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
            if (!(user instanceof Player player)) {
                return;
            }
            GuhEntity guh = gericht(player, 5.0);
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            if (level.isClientSide()) {
                // warm air: little puffs blowing where you aim
                if (remaining % 2 == 0) {
                    Vec3 at = eye.add(look.scale(0.9)).add(0, -0.25, 0);
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, at.x, at.y, at.z, look.x * 0.3, look.y * 0.3, look.z * 0.3);
                }
                return;
            }
            if (remaining % 12 == 0) {
                level.playSound(null, player.blockPosition(), KnuffelbadFeature.FOHN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
            }
            if (guh != null && player instanceof ServerPlayer sp) {
                Wasritueel.fohn(sp, guh);
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.guhs.guh_fohn.lore").withStyle(ChatFormatting.GRAY));
        }

        /** The guh the player aims at (within reach, nothing in between). */
        @Nullable
        static GuhEntity gericht(Player player, double reach) {
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getLookAngle().scale(reach));
            AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(reach)).inflate(1.0);
            EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, box,
                    e -> e instanceof GuhEntity, 0.3f);
            if (hit == null || !(hit.getEntity() instanceof GuhEntity guh)) {
                return null;
            }
            HitResult block = player.level().clip(new ClipContext(eye, hit.getLocation(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            return block.getType() == HitResult.Type.MISS || block.getLocation().distanceToSqr(eye) >= hit.getLocation().distanceToSqr(eye) - 0.01 ? guh : null;
        }
    }

    private KnuffelbadItems() {
    }
}
