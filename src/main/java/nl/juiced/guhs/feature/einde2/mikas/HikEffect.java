package nl.juiced.guhs.feature.einde2.mikas;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * De hik (effect guhs:hik): what you get when a Kaasmijt tickles your toes. Every second and a half or so you hiccup: a
 * little hop and a squeaky "hik!". Completely harmless (no damage, nothing slowed), it only looks silly.
 */
public class HikEffect extends MobEffect {
    /** Ticks between two hiccups. */
    public static final int TUSSEN = 30;
    /** How hard a hiccup lifts you (blocks per tick, straight up: a hop of about a third of a block). */
    public static final double HUP = 0.26;

    public HikEffect() {
        super(MobEffectCategory.NEUTRAL, 0xF6D860);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % TUSSEN == TUSSEN - 1;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        hik(level, mob);
        return true;
    }

    /** One hiccup: the squeak, and a hop when it stands on something. */
    public static void hik(ServerLevel level, LivingEntity mob) {
        level.playSound(null, mob.getX(), mob.getEyeY(), mob.getZ(), E2MikasFeature.HIK_GELUID.get(), SoundSource.NEUTRAL, 0.7f,
                0.9f + mob.getRandom().nextFloat() * 0.5f);
        if (mob.onGround() && !mob.isPassenger() && !mob.isSleeping()) {
            Vec3 v = mob.getDeltaMovement();
            mob.setDeltaMovement(v.x, HUP, v.z);
            mob.hurtMarked = true;      // (so a player's client gets the hop)
        }
    }
}
