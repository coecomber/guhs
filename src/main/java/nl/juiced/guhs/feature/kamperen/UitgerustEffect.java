package nl.juiced.guhs.feature.kamperen;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import nl.juiced.guhs.Guhs;

/**
 * Uitgerust: what a whole night in a guh_slaapzak does to you. You feel fresh and bouncy: a little faster, and every
 * few seconds a little better if you were hurt. VAHOEG uitgeslapen!
 */
public class UitgerustEffect extends MobEffect {
    public UitgerustEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF7A8D0);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, Guhs.id("effect.uitgerust"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(net.minecraft.server.level.ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(1f);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 100 == 0;
    }
}
