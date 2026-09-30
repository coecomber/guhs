package nl.juiced.guhs.feature.baltoslee;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A guh-sledehondje: a round little guh with husky ears, a curly tail and a red harness. Never a real entity in the world:
 * the sled renderers make a few of these in the client and draw them in front of the sled (running, trotting, or sitting
 * when the sled stands still). Its look: {@link #soort} (0 = a sledehondje, 1 = one of Steele-Mika's, 2 = the lead dog with
 * a golden bell); its legs follow {@link #loop} (0 stands .. 1 runs flat out) on {@link #fase}.
 */
public class SledehondjeEntity extends Entity implements GeoEntity {
    public static final int GEWOON = 0, STEELE = 1, KOP = 2;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public int soort;
    /** How fast it runs (0..1), where its legs are in their swing (radians), and sitting (0..1). */
    public float loop, fase, zit, blij;

    public SledehondjeEntity(EntityType<? extends SledehondjeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            discard();                                             // (never in the world)
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
