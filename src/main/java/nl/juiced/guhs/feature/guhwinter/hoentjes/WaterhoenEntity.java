package nl.juiced.guhs.feature.guhwinter.hoentjes;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Het waterhoentje (1.10.9 Guhwinter): a guh-ified common moorhen. A round dark-slate little bird with an olive-brown back
 * and a white line along its flank, a red frontal shield with a red-and-yellow bill, a white tail that flicks up and down
 * all the time, and long green toes with a red garter; plus the guh face (glossy guh eyes, a blush, two little round
 * guh ears). It walks and swims (it floats on the water, like the guh-eendje), bobs its head with every step and every
 * paddle stroke. Lives at the water's edge in the Guhmension (rare, {@link HoentjesSpawner}).
 */
public class WaterhoenEntity extends Hoentje {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ZWEM = RawAnimation.begin().thenLoop("zwem");

    public WaterhoenEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0f);
    }

    @Override
    public String soort() {
        return GuhwinterHoentjes.WATERHOEN;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation nav = new GroundPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void addStrollGoal() {
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8, 80));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (isInWater() && getFluidHeight(FluidTags.WATER) > 0.22) {
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x, Math.min(0.08, v.y + 0.05), v.z);   // (it floats: a moorhen bobs on the water)
        }
    }

    @Override
    protected SoundEvent roep() {
        return GuhwinterHoentjes.WATERHOEN_ROEP.get();
    }

    @Override
    protected SoundEvent piep() {
        return GuhwinterHoentjes.WATERHOEN_PIEP.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 3, state -> {
            if (isInWater()) {
                return state.setAndContinue(ZWEM);
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("roep", RawAnimation.begin().thenPlay("roep")));
    }
}
