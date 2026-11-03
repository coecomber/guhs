package nl.juiced.guhs.feature.vadsrijk.kern.klasse;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;

/**
 * What a skill gets: the player, their stats now, their class level, the entity under the crosshair (within 24 blocks, or
 * null), the element and the base damage of the weapon in hand (no weapon: GEEN and a weak fist value).
 */
public record SkillContext(ServerPlayer speler, Stats stats, int level, @Nullable LivingEntity doel, Element element, double wapenSchade) {
}
