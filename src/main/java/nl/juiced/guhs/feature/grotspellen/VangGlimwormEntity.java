package nl.juiced.guhs.feature.grotspellen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * A glimwormpje of Glimwormvangen: the cave glimworm (the safari glimworm's baby model with its glowing tail) fluttering
 * through Juf Glimmie's dark hall. It flies from spot to spot inside the hall ({@link #zet}), flees from a player who
 * comes too close, and some are quick, some hide in the glimplanten until you're right next to them; a golden one is
 * worth five. Only a game makes them and owns them: one without its game is gone at once, and they are never saved.
 * They can't be hurt: a swing of the net (or a tap) catches them ({@link Glimwormvangen}).
 */
public class VangGlimwormEntity extends AmbientCreature implements GeoEntity {
    private static final EntityDataAccessor<Boolean> GOUD = SynchedEntityData.defineId(VangGlimwormEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation VLIEG = RawAnimation.begin().thenLoop("walk");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private AABB zaal;
    private double snelheid = 0.06, schrik = 3.0;
    @Nullable
    private BlockPos plant;
    @Nullable
    private Vec3 doel;
    private int doelTijd, rust;

    public VangGlimwormEntity(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GOUD, false);
    }

    public boolean isGoud() {
        return entityData.get(GOUD);
    }

    /** Sets it up for a game: the hall it flies in, how fast, from how far it gets scared, gold, its hiding plant (or null). */
    public void zet(AABB zaal, double snelheid, double schrik, boolean goud, @Nullable BlockPos plant) {
        this.zaal = zaal;
        this.snelheid = snelheid;
        this.schrik = schrik;
        this.plant = plant;
        entityData.set(GOUD, goud);
    }

    public boolean verstopper() {
        return plant != null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (random.nextInt(isGoud() ? 4 : 10) == 0) {
                level().addParticle(isGoud() ? ParticleTypes.WAX_ON : ParticleTypes.GLOW, getX(), getY() + 0.1, getZ(), 0, 0.01, 0);
            }
            return;
        }
        if (tickCount > 20 && !Glimwormvangen.WORMEN.contains(getUUID())) {
            discard();                                          // (no game owns it: gone)
            return;
        }
        if (zaal == null) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        vlieg((ServerLevel) level());
    }

    private void vlieg(ServerLevel level) {
        Player p = level.getNearestPlayer(this, schrik);
        if (p != null && !p.isSpectator()) {
            Vec3 weg = position().subtract(p.position()).multiply(1, 0.3, 1);
            weg = weg.lengthSqr() < 1e-4 ? new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5) : weg.normalize();
            doel = binnen(position().add(weg.scale(4 + random.nextDouble() * 2)).add(0, random.nextDouble() - 0.3, 0));
            doelTijd = 30;
            rust = 0;
        }
        if (rust > 0) {
            rust--;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (doel == null || --doelTijd <= 0 || position().distanceToSqr(doel) < 0.09) {
            if (doel != null && plant != null && doel.distanceToSqr(Vec3.atCenterOf(plant)) < 0.3) {
                rust = 200;                                     // (it reached its plant: it sits there, glowing quietly)
            }
            doel = nieuwDoel();
            doelTijd = 80;
            if (rust > 0) {
                return;
            }
        }
        Vec3 naar = doel.subtract(position());
        double afstand = naar.length();
        double v = Math.min(afstand, snelheid * (p != null ? 1.6 : 1.0));
        Vec3 stap = afstand < 1e-6 ? Vec3.ZERO : naar.scale(v / afstand);
        // a little flutter up and down
        stap = stap.add(0, Math.sin((tickCount + getId() * 7) * 0.4) * 0.015, 0);
        Vec3 voor = position();
        move(MoverType.SELF, stap);
        setDeltaMovement(Vec3.ZERO);                            // (moved by hand: nothing left for vanilla's travel)
        if (position().distanceToSqr(voor) < stap.lengthSqr() * 0.25) {
            doelTijd = 0;                                       // (bumped into something: somewhere else)
        }
        if (stap.horizontalDistanceSqr() > 1e-6) {
            float yaw = (float) (Math.atan2(stap.z, stap.x) * 180 / Math.PI) - 90f;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
    }

    private Vec3 nieuwDoel() {
        if (plant != null && random.nextInt(3) > 0) {
            return Vec3.atCenterOf(plant).add(0, -0.15, 0);
        }
        AABB z = zaal;
        return new Vec3(z.minX + random.nextDouble() * (z.maxX - z.minX), z.minY + random.nextDouble() * (z.maxY - z.minY),
                z.minZ + random.nextDouble() * (z.maxZ - z.minZ));
    }

    private Vec3 binnen(Vec3 v) {
        AABB z = zaal;
        return new Vec3(Math.max(z.minX, Math.min(z.maxX, v.x)), Math.max(z.minY, Math.min(z.maxY, v.y)), Math.max(z.minZ, Math.min(z.maxZ, v.z)));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("vlieg", 4, state -> state.setAndContinue(VLIEG)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
