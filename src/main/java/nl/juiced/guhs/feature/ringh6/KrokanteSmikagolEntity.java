package nl.juiced.guhs.feature.ringh6;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.ring.SmikagolEntity;

/**
 * bbq2 (ring-h6): Smikagol as he climbs out of the frituur: golden brown, crispy and beaming (entity
 * {@code guhs:ringh6_krokante_smikagol}). It is ring-kern's Smikagol with another coat (the same model and animations, the
 * texture textures/entity/ringh6_krokante_smikagol.png from tools/features/ring_h6.py): an actor of the finale's cutscenes.
 * Nobody owns him, he is never saved; afterwards the player's buddy is the ordinary Smikagol again (the crisp wears off).
 */
public class KrokanteSmikagolEntity extends SmikagolEntity {
    public KrokanteSmikagolEntity(EntityType<? extends KrokanteSmikagolEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
