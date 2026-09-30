package nl.juiced.guhs.feature.kaasmoeras;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.Guhs;

/**
 * Onvahoeg: what a vadsverdrijvend drankje of the Moerasheks-Mika does to you. You feel a little less vahoeg for a
 * while: a bit slower, and your tummy rumbles (some extra exhaustion). A weak debuff, never dangerous.
 */
public class OnvahoegEffect extends MobEffect {
    /** Exhaustion per second while it lasts (4 = one hunger point's worth of saturation). */
    public static final float EXHAUSTION = 0.6f;

    public OnvahoegEffect() {
        super(MobEffectCategory.HARMFUL, 0xB7C44A);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, Guhs.id("effect.onvahoeg"), -0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(net.minecraft.server.level.ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            player.causeFoodExhaustion(EXHAUSTION * (amplifier + 1));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
