package nl.juiced.guhs.feature.wereldleven;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import nl.juiced.guhs.Guhs;

/** The two effects of the kaasijsjes (2.8, wereldleven): blosjes and zweverig. Both only ever nice. */
public final class WereldlevenEffects {
    /** Blushing cheeks: little hearts float up now and then, and a sliver of healing every few seconds. */
    public static class Blosjes extends MobEffect {
        public Blosjes() {
            super(MobEffectCategory.BENEFICIAL, 0xFF9CC4);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 30 == 0;
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            {
                level.sendParticles(WereldlevenFeature.IJSJESHARTJE.get(), entity.getX(), entity.getY() + entity.getBbHeight() * 0.9, entity.getZ(),
                        2, entity.getBbWidth() * 0.4, 0.1, entity.getBbWidth() * 0.4, 0.01);
                if (entity.getHealth() < entity.getMaxHealth() && entity.getRandom().nextInt(3) == 0) {
                    entity.heal(1f);
                }
            }
            return true;
        }
    }

    /** Floaty: gravity much lower and falls never hurt (like a soft ice-cream cloud). */
    public static class Zweverig extends MobEffect {
        public Zweverig() {
            super(MobEffectCategory.BENEFICIAL, 0xBFE8FF);
            addAttributeModifier(Attributes.GRAVITY, Guhs.id("effect.zweverig"), -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.SAFE_FALL_DISTANCE, Guhs.id("effect.zweverig_val"), 16.0, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    private WereldlevenEffects() {
    }
}
