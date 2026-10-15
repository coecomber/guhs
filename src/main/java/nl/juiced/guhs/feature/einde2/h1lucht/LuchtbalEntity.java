package nl.juiced.guhs.feature.einde2.h1lucht;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * einde2 phase B, slice h1_lucht: the ball of the luchtbal match (a swirl of air). It floats: little gravity, a lot of
 * drag, it bounces off whatever it hits. A player hits it (left click) and it flies where they look; Aang-guh blows it
 * back ({@link Luchtbal}); the power luchtbal blows it hard. One player's ball, never saved.
 * Model: geckolib/models/entity/einde2_h1_lucht_luchtbal.geo.json.
 */
public class LuchtbalEntity extends Entity implements GeoEntity {
    private static final RawAnimation DRAAI = RawAnimation.begin().thenLoop("animation.einde2_h1_lucht_luchtbal.draai");
    private static final double ZWAARTE = 0.012, REMMING = 0.975;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LuchtbalEntity(EntityType<? extends LuchtbalEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 v = getDeltaMovement().add(0, -ZWAARTE, 0).scale(REMMING);
        Vec3 voor = position();
        move(MoverType.SELF, v);
        Vec3 echt = position().subtract(voor);
        // a bounce on every side that stopped it
        double vx = Math.abs(echt.x - v.x) > 1e-4 ? -v.x * 0.7 : v.x;
        double vy = Math.abs(echt.y - v.y) > 1e-4 ? -v.y * 0.6 : v.y;
        double vz = Math.abs(echt.z - v.z) > 1e-4 ? -v.z * 0.7 : v.z;
        setDeltaMovement(vx, vy, vz);
        if (tickCount % 3 == 0) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    /** A push in this direction (strength in blocks per tick). */
    public void duw(Vec3 richting, double kracht) {
        Vec3 r = richting.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : richting.normalize();
        setDeltaMovement(r.scale(kracht).add(0, 0.12, 0));
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 1.0f, 1.6f);
        }
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof ServerPlayer p) {
            duw(p.getLookAngle(), 0.85);
        }
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
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
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, state -> state.setAndContinue(DRAAI)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
