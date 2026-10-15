package nl.juiced.guhs.feature.einde2.h4vuur;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

/**
 * einde2 phase B, slice h4_vuur: an airship of Vuurheer Mikai's fleet (entity {@code guhs:einde2_h4_vuur_luchtschip}; the
 * playable flight of the finale, {@link Luchtvloot}). A long red hull with a black keel, fins and the Vuurnatie flame on
 * its flanks, two chimneys that smoke; seen only by the player whose flight it is. It never hurts: its blasts only shove
 * ({@link VuurbalEntity}), and when Sokka-guh, Toph-guh or Suki-guh have taken it down it sinks slowly, burning and
 * smoking, into the clouds (and is gone; nobody aboard ever gets hurt: the crew floats down under little parachutes).
 * Never saved. Model and texture: tools/features/einde2_h4_vuur.py.
 */
public class LuchtschipEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_ZINKT = SynchedEntityData.defineId(LuchtschipEntity.class, EntityDataSerializers.BOOLEAN);
    /** How much it leans while it sinks (client side, for the renderer). */
    public float helling, hellingO;
    int zinkTicks;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LuchtschipEntity(EntityType<? extends LuchtschipEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DATA_ZINKT, false);
    }

    public boolean zinkt() {
        return entityData.get(DATA_ZINKT);
    }

    /** Taken down: it sinks for a few seconds and is gone. */
    void zink() {
        if (!zinkt()) {
            entityData.set(DATA_ZINKT, true);
            zinkTicks = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        hellingO = helling;
        if (level().isClientSide()) {
            if (zinkt()) {
                helling = Math.min(25f, helling + 0.6f);
                for (int i = 0; i < 4; i++) {
                    level().addParticle(i % 2 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5) * 6,
                            getY() + 1 + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 6, 0, 0.05, 0);
                }
                if (random.nextInt(8) == 0) {
                    level().addParticle(ParticleTypes.CLOUD, getX(), getY() - 1, getZ(), 0, -0.05, 0);
                }
            } else if (random.nextInt(3) == 0) {
                Vec3 achter = Vec3.directionFromRotation(0, getYRot()).scale(-3.5);
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX() + achter.x, getY() + 3.2, getZ() + achter.z, 0, 0.04, 0);
            }
            return;
        }
        Vec3 v = getDeltaMovement();
        if (zinkt()) {
            v = new Vec3(v.x * 0.9, -0.12, v.z * 0.9);
            setDeltaMovement(v);
            if (++zinkTicks > 70) {
                discard();
            }
        }
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
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
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
