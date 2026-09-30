package nl.juiced.guhs.feature.gatenkaas;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * guhs:stil: you make no sound at all. Knabbelsensoren don't hear you walk, eat or dig, and the Vadswaker can neither
 * hear nor sniff you ({@link Knabbelgeluid}). From a stille knabbel (and the Sluipknabbel drink of the guhbrouwketel).
 */
public class StilEffect extends MobEffect {
    public StilEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xCDB892);
    }

    /** Is this one inaudible right now? */
    public static boolean isStil(LivingEntity entity) {
        return entity.hasEffect(GatenkaasFeature.STIL);
    }
}
