package nl.juiced.guhs.feature.guhwinter.hoentjes;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Het alpine hoentje (1.10.9 Guhwinter): a guh-ified rock ptarmigan in its winter plumage. A plump snow-white little bird
 * with a black "eyeliner" stripe from the bill to the eye, a small red comb above each eye, a black-edged tail and big
 * fluffy feathered feet (snowshoes!); plus the guh face (glossy guh eyes, a blush, two little round guh ears). It doesn't
 * walk far: it <b>hops</b> (little jumps while it goes somewhere). Lives high up, on snow and in the cold of the Guhmension
 * (rare, {@link HoentjesSpawner}).
 */
public class AlpinehoenEntity extends Hoentje {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation HOP = RawAnimation.begin().thenLoop("hop");
    /** Ticks between two hops while it is going somewhere. */
    private int hopWacht;

    public AlpinehoenEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    public String soort() {
        return GuhwinterHoentjes.ALPINEHOEN;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        super.registerGoals();
    }

    @Override
    protected void addStrollGoal() {
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8, 0.001f));
    }

    /** While it goes somewhere it hops: a little jump forward every half second or so. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || isInWater()) {
            return;
        }
        if (hopWacht > 0) {
            hopWacht--;
        }
        boolean onderweg = !getNavigation().isDone() || getMoveControl().hasWanted();
        if (onderweg && onGround() && hopWacht <= 0) {
            Vec3 v = getDeltaMovement();
            Vec3 voor = Vec3.directionFromRotation(0, getYRot());
            setDeltaMovement(v.x + voor.x * 0.09, 0.34, v.z + voor.z * 0.09);
            hopWacht = 9 + getRandom().nextInt(5);
        }
    }

    @Override
    protected SoundEvent roep() {
        return GuhwinterHoentjes.ALPINEHOEN_ROEP.get();
    }

    @Override
    protected SoundEvent piep() {
        return GuhwinterHoentjes.ALPINEHOEN_PIEP.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("beweeg", 2, state -> {
            if (!onGround() && !isInWater()) {
                return state.setAndContinue(HOP);
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("roep", RawAnimation.begin().thenPlay("roep")));
    }
}
