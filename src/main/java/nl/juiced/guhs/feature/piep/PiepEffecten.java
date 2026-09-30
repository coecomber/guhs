package nl.juiced.guhs.feature.piep;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.registry.ModItems;

/** Piep's two buffs: "Fris van binnen" (a guh that Poepschilly cleaned) and "Lief kijken" (the roze guh koek). */
public final class PiepEffecten {
    /** How long a guh is fris van binnen, and "Lief kijken" lasts after one koek. */
    public static final int FRIS_TICKS = 20 * 60 * 3, LIEF_TICKS = 20 * 60 * 3;
    /** With "Lief kijken" a kaasknabbel gets an extra chance of 1 in this many to tame a wild guh. */
    public static final int LIEF_TEMKANS = 2;

    /** Fris van binnen: a bit faster, and sparkles now and then. */
    public static class FrisVanBinnen extends MobEffect {
        public FrisVanBinnen() {
            super(MobEffectCategory.BENEFICIAL, 0x8FE8D8);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, Guhs.id("fris_van_binnen"), 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 12 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level() instanceof ServerLevel level) {
                level.sendParticles(PiepFeature.FRIS_SPARKEL.get(), entity.getX(), entity.getY() + entity.getBbHeight() * 0.7, entity.getZ(), 2,
                        entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.01);
            }
            return true;
        }
    }

    /** Lief kijken: the guhs around you make hearts, and taming guhs, muisjes and Poepschilly is easier. */
    public static class LiefKijken extends MobEffect {
        public LiefKijken() {
            super(MobEffectCategory.BENEFICIAL, 0xF080C0);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 40 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level() instanceof ServerLevel level && entity instanceof Player) {
                for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, entity.getBoundingBox().inflate(8))) {
                    if (guh.getRandom().nextInt(3) == 0) {
                        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.2, 0.1, 0.2, 0);
                    }
                }
            }
            return true;
        }
    }

    /** Besties (Schilly and a guh): a little regeneration and now and then a heart. */
    public static class Besties extends MobEffect {
        public Besties() {
            super(MobEffectCategory.BENEFICIAL, 0x9CD86A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 50 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(1f + amplifier);
            }
            if (entity.level() instanceof ServerLevel level && entity.getRandom().nextInt(3) == 0) {
                level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + entity.getBbHeight() + 0.2, entity.getZ(), 1, 0.2, 0.1, 0.2, 0);
            }
            return true;
        }
    }

    /**
     * (GuhHooks.klik) A kaasknabbel for a wild guh while you look lief: an extra chance to tame it right away. When that
     * chance misses, the guh's own taming roll still follows (so it is only ever easier).
     */
    static InteractionResult liefKijkenTemmen(GuhEntity guh, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (guh.isTame() || !stack.is(ModItems.KAAS_KNABBELS.get()) || !player.hasEffect(PiepFeature.LIEF_KIJKEN)) {
            return InteractionResult.PASS;
        }
        if (guh.level().isClientSide() || guh.getRandom().nextInt(LIEF_TEMKANS) != 0
                || net.neoforged.neoforge.event.EventHooks.onAnimalTame(guh, player)) {
            return InteractionResult.PASS;
        }
        stack.consume(1, player);
        guh.tame(player);
        guh.getNavigation().stop();
        guh.setTarget(null);
        guh.level().broadcastEntityEvent(guh, (byte) 7);
        guh.playSound(nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), 1f, guh.getVoicePitch());
        return InteractionResult.SUCCESS;
    }

    private PiepEffecten() {
    }
}
