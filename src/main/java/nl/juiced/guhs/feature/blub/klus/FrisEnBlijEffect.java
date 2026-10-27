package nl.juiced.guhs.feature.blub.klus;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;

/**
 * Fris en blij (1.8.0 ronde 3b): after a bubble bath in a Blub-bubbelbadje. Purely lief: every few seconds a little heart,
 * and now and then a happy emote (hartjes, a twirl). Nothing stronger than that.
 */
public class FrisEnBlijEffect extends MobEffect {
    public FrisEnBlijEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xA8E4F7);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + entity.getBbHeight() + 0.2, entity.getZ(), 1, 0.2, 0.1, 0.2, 0);
        level.sendParticles(ParticleTypes.BUBBLE_POP, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
        if (entity instanceof GuhEntity g && g.emotes.current() == null && level.getRandom().nextInt(4) == 0) {
            g.emotes.start(level.getRandom().nextBoolean() ? Emote.HARTJES : Emote.KNUFFELDANSJE, false, GuhEmotes.Source.SELF);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 80 == 0;
    }
}
