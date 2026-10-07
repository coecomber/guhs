package nl.juiced.guhs.feature.snuffel;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The companion's little tree (entity {@code guhs:snuffel_boompje}): one entity on the island's tree spot, the same for
 * everybody. It knows no stage: every client draws it in the stage of ITS OWN player (client.BoompjeRenderer: the ring of
 * stones only, kiem, scheutje, struikje, jong boompje) and lets it pop when that stage goes up ({@link #pop}). Nothing
 * moves, hurts or removes it; {@link Eiland#bewoon} keeps exactly one standing.
 */
public class BoompjeEntity extends Entity implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), GROEI = RawAnimation.begin().thenPlay("groei").thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Client: the tick count until which the growth pop plays. */
    public int popTot = -1;

    public BoompjeEntity(EntityType<? extends BoompjeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    /** (Client) the tree just grew: the pop animation plays. */
    public void pop() {
        popTot = tickCount + 16;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BoompjeEntity>("groei", 0, state -> state.setAndContinue(popTot >= tickCount ? GROEI : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
