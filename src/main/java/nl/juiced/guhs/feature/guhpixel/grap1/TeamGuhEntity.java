package nl.juiced.guhs.feature.guhpixel.grap1;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * A team guh of Bedwars: a stand-in that looks like a guh (model, nightcap, emotes) but is nobody's guh. It is never
 * {@code ModEntities.GUH}: it cannot be tamed, fed, picked up or called, has no hearts and no diary, is never saved and
 * does nothing by itself: {@link BedwarsSessie} walks it over its bridge and puts it to sleep next to the bed. It never
 * hurts anybody (it has no goals at all).
 */
public class TeamGuhEntity extends GuhEntity {
    public TeamGuhEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    /** No wandering, fleeing, following, eating or playing: the game moves it. */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public void onOwnerTap(Player player) {
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
