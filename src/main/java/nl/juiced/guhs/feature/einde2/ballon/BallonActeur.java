package nl.juiced.guhs.feature.einde2.ballon;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

/**
 * einde2 phase C, slice trap5_ballon: the Vredesballon as an actor of its launch scene ({@link BallonScene}). Only a scene
 * makes one (client side, never saved, never in the world). The finished balloon of the building site at full size: the
 * model is drawn at a third and scaled by 3 (tools/features/einde2_trap5_ballon_model.py). Its origin is the bottom of the
 * basket's floor. When the scene says "strooi" (and from then on, also while the scene waits for a click) the bags swing and
 * knabbels rain from under the basket; the flame of the burner always flickers and glows.
 */
public class BallonActeur extends Entity implements GeoEntity {
    private static final RawAnimation ZWEVEN = RawAnimation.begin().thenLoop("animation.einde2_trap5_ballon_vredesballon.zweven");
    private static final RawAnimation STROOI = RawAnimation.begin().thenLoop("animation.einde2_trap5_ballon_vredesballon.strooi");
    /** The mouth of the balloon above the origin (blocks) and the half width of the basket. */
    static final double MOND = 12, MAND = 3.2;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Client: once told to scatter, it keeps scattering (the actor lives only as long as its scene). */
    private boolean strooit;

    public BallonActeur(EntityType<? extends BallonActeur> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    public boolean strooit() {
        if (!strooit && level().isClientSide() && "strooi".equals(Cutscenes.animatie(this))) {
            strooit = true;
        }
        return strooit;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            return;
        }
        RandomSource r = getRandom();
        // the burner: a lick of flame in the mouth now and then
        if (tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.FLAME, getX() + (r.nextDouble() - 0.5) * 0.8, getY() + MOND - 1 + r.nextDouble(), getZ() + (r.nextDouble() - 0.5) * 0.8,
                    0, 0.04, 0);
        }
        if (!strooit()) {
            return;
        }
        // knabbels rain from under the basket, and a sparkle here and there
        ItemParticleOption deeltje = new ItemParticleOption(ParticleTypes.ITEM, BuiltInRegistries.ITEM.getValue(Identifier.parse("guhs:kaas_knabbels")));
        for (int i = 0; i < 6; i++) {
            level().addParticle(deeltje, getX() + (r.nextDouble() - 0.5) * 2 * (MAND + 1.5), getY() - 0.2 - r.nextDouble(),
                    getZ() + (r.nextDouble() - 0.5) * 2 * (MAND + 1.5), (r.nextDouble() - 0.5) * 0.15, -0.25 - r.nextDouble() * 0.2,
                    (r.nextDouble() - 0.5) * 0.15);
        }
        if (tickCount % 4 == 0) {
            level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX() + (r.nextDouble() - 0.5) * 8, getY() - 2 - r.nextDouble() * 6,
                    getZ() + (r.nextDouble() - 0.5) * 8, 0, -0.05, 0);
        }
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
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    // --- GeckoLib -------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("zweven", 0, state -> state.setAndContinue(ZWEVEN)));
        controllers.add(new AnimationController<>("strooi", 5, state -> strooit() ? state.setAndContinue(STROOI) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
