package nl.juiced.guhs.feature.snuffel;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A dog that is only a LOOK (entity {@code guhs:snuffel_hond}): what a client draws in place of a player on the island
 * (one per dog-player, never in the level), a dog in a screen (the choice, the Guhstation), and an actor in a cutscene:
 * <pre>
 * .acteur("ik", SnuffelFeature.SNUFFEL_HOND, start, yaw, tag -&gt; tag.putBoolean("Speler", true))           // the viewer's own dog
 * .acteur("pup", SnuffelFeature.SNUFFEL_HOND, start, yaw, tag -&gt; { tag.putBoolean("Speler", true); tag.putBoolean("Pup", true); })
 * .acteur("jutje", SnuffelFeature.SNUFFEL_HOND, start, yaw, tag -&gt; tag.putString("Bewoner", "redder"))     // a named resident
 * .animatie("ik", 40, "snuffel")       // idle, walk, snuffel, snuffel_loop, zit, kwispel, graaf, blaf; "" stops it
 * </pre>
 * It has no behaviour and is never saved; a real resident is a {@link BewonerEntity}.
 */
public class HondEntity extends SnuffelHond {
    public HondEntity(EntityType<? extends HondEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
