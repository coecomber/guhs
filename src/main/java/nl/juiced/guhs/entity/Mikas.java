package nl.juiced.guhs.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import nl.juiced.guhs.Guhs;

/**
 * 1.2.8: the one place that knows what a Mika is, and what an aggressive guh fights.
 * <ul>
 *   <li>{@link #MIKAS} (entity type tag {@code guhs:mikas}, data/guhs/tags/entity_type/mikas.json): every Mika you can
 *   fight. A new fightable Mika only has to be added to that tag. The Mikas of a minigame or a job that can't be hurt
 *   (Mika-baas, Kruimel-Mika, Heg-Mika, Mika-pikker, Knabbeldief) are not in it on purpose: they are no monsters either,
 *   so an aggressive guh leaves them alone.</li>
 *   <li>{@link #GEEN_VIJAND} (entity type tag {@code guhs:geen_vijand}): mobs that are technically monsters but sweet
 *   (the pink guh slime): an aggressive guh leaves them alone.</li>
 * </ul>
 */
public final class Mikas {
    public static final TagKey<EntityType<?>> MIKAS = TagKey.create(Registries.ENTITY_TYPE, Guhs.id("mikas"));
    public static final TagKey<EntityType<?>> GEEN_VIJAND = TagKey.create(Registries.ENTITY_TYPE, Guhs.id("geen_vijand"));

    /** A Mika you can fight (the tag {@code guhs:mikas}). */
    public static boolean isMika(Entity entity) {
        return isMika(entity.getType());
    }

    /** Is this entity type a Mika you can fight (the tag {@code guhs:mikas})? */
    public static boolean isMika(EntityType<?> type) {
        return type.builtInRegistryHolder().is(MIKAS);
    }

    /**
     * What an aggressive guh goes after: every fightable Mika and every hostile mob (vanilla {@link Enemy}, so modded monsters
     * too). Never a passive or neutral mob, never a critter.
     */
    public static boolean isVijand(Entity entity) {
        return !entity.getType().builtInRegistryHolder().is(GEEN_VIJAND) && (isMika(entity) || entity instanceof Enemy);
    }

    private Mikas() {
    }
}
