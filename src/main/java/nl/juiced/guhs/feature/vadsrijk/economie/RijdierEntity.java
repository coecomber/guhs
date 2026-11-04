package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * A summoned mount (the Hoedhupper, the Klodderros): the rider steers, jump = a big hop (its jump stat), the speed comes from its
 * level. It is never hurt (a hit makes it poof away), never pushed, never saved in the world (after a restart it is gone; the
 * rijfluitje brings it back) and only lives in the Vadsrijk. Riding gives it xp (server, one per block).
 */
public class RijdierEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(RijdierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(RijdierEntity.class, EntityDataSerializers.INT);

    /** Client only: a preview in the mount screen drawn as a dark silhouette (a mount you don't have yet). */
    public boolean silhouet;
    /** Client only: a preview in a screen (never in the world): always the idle animation, no pop-in. */
    public boolean voorbeeld;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID eigenaar;
    private boolean geladen;
    private int zonderRuiter, springWacht;
    /** The Wolkenschaapje's double jump: used in this jump / jump key released since the last jump. */
    private boolean dubbelGebruikt, springLos;
    private double afstand;

    public RijdierEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributen() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.STEP_HEIGHT, 1.05)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.SAFE_FALL_DISTANCE, 64);
    }

    public RijdierSoort soort() {
        return EcoSpul.soort(getType());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_LEVEL, 1);
        b.define(DATA_KLEUR, 0);
    }

    public int rijLevel() {
        return entityData.get(DATA_LEVEL);
    }

    void zetLevel(int level) {
        entityData.set(DATA_LEVEL, Math.max(1, level));
    }

    public RijdierSoort.PluimKleur kleur() {
        RijdierSoort.PluimKleur[] k = RijdierSoort.PluimKleur.values();
        return k[Math.floorMod(entityData.get(DATA_KLEUR), k.length)];
    }

    public void zetKleur(RijdierSoort.PluimKleur k) {
        entityData.set(DATA_KLEUR, k.ordinal());
    }

    void zetEigenaar(UUID id, int level) {
        eigenaar = id;
        zetLevel(level);
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    // ---- riding -----------------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        float yaw = getYRot() + Mth.wrapDegrees(player.getYRot() - getYRot()) * 0.5f;
        setRot(yaw, 0);
        this.yRotO = this.yBodyRot = this.yHeadRot = yaw;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float vooruit = player.zza <= 0 ? player.zza * 0.35f : player.zza;
        return new Vec3(player.xxa * 0.5f, 0, vooruit);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) soort().snelheid(rijLevel());
    }

    @Override
    public void travel(Vec3 input) {
        if (getControllingPassenger() instanceof Player rider) {
            if (springWacht > 0) {
                springWacht--;
            }
            boolean springt = rider.isJumping();
            if (onGround()) {
                dubbelGebruikt = false;
            }
            if (onGround() && springt && springWacht <= 0) {
                Vec3 dm = getDeltaMovement();
                Vec3 kijk = Vec3.directionFromRotation(0, getYRot()).scale(input.z > 0 ? 0.25 : 0);
                setDeltaMovement(dm.x + kijk.x, soort().sprong(rijLevel()), dm.z + kijk.z);
                springWacht = 8;
                springLos = false;
            } else if (!onGround() && springt && springLos && !dubbelGebruikt && soort().zweeft()) {
                // the Wolkenschaapje: a second little hop in the air (a puff of cloud)
                Vec3 dm = getDeltaMovement();
                setDeltaMovement(dm.x, soort().sprong(rijLevel()) * 0.85, dm.z);
                dubbelGebruikt = true;
                springLos = false;
                for (int i = 0; i < 8; i++) {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, getX() + (random.nextDouble() - 0.5), getY(),
                            getZ() + (random.nextDouble() - 0.5), 0, -0.05, 0);
                }
            }
            if (!springt) {
                springLos = true;
            }
            if (soort().zweeft() && !onGround() && getDeltaMovement().y < -0.08) {
                Vec3 dm = getDeltaMovement();   // it hovers: falls gently
                setDeltaMovement(dm.x, -0.08, dm.z);
            }
        }
        super.travel(input);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float partialTick) {
        return new Vec3(0, soort().zit, soort() == RijdierSoort.SLAKKIE ? -0.28 : 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && player instanceof ServerPlayer sp && sp.getUUID().equals(eigenaar) && !isVehicle()) {
            sp.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    // ---- life -------------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (eigenaar == null && !geladen) {
            return;   // (a summoned one without an owner: just stands there and looks cute; creative/screenshots)
        }
        if (geladen || !Vadsrijk.in(this)) {
            Rijdieren.poef(this);
            return;
        }
        if (getFirstPassenger() instanceof ServerPlayer p) {
            zonderRuiter = 0;
            if (tickCount % 5 == 0 && "arena".equals(Rijdieren.afReden(true, getX(), getY(), getZ()))) {
                Rijdieren.arena(p, this);   // no mounts in a boss arena
                return;
            }
            double dx = getX() - xo, dz = getZ() - zo;
            afstand += Math.min(2, Math.sqrt(dx * dx + dz * dz));
            if (tickCount % 20 == 0 && afstand >= 1) {
                long blokken = (long) afstand;
                afstand -= blokken;
                Rijdieren.geefXp(p, soort(), blokken);
            }
        } else if (++zonderRuiter > 10) {
            Rijdieren.poef(this);   // got off: it hops home
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (getFirstPassenger() instanceof ServerPlayer p) {
            Rijdieren.geraakt(p);
        } else {
            Rijdieren.poef(this);
        }
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Gefloten", eigenaar != null);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        // a whistled mount is never kept over a restart / chunk reload: the rijfluitje brings a new one
        geladen = tag.getBooleanOr("Gefloten", false);
    }

    // ---- GeckoLib ---------------------------------------------------------------------------------------------------------------

    private static final RawAnimation STIL = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation HUP = RawAnimation.begin().thenLoop("hop");
    private static final RawAnimation LUCHT = RawAnimation.begin().thenLoop("sprong");
    private static final RawAnimation REN = RawAnimation.begin().thenLoop("ren");

    /** Horizontal speed (blocks per tick) from the last tick's movement: walk vs run. */
    private double tempo() {
        double dx = getX() - xo, dz = getZ() - zo;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RijdierEntity>("beweeg", 4, state -> {
            if (voorbeeld) {
                return state.setAndContinue(STIL);
            }
            boolean lucht = !onGround() && !isInWater() && Math.abs(getY() - yo) > 0.02;
            double v = tempo();
            return state.setAndContinue(lucht ? LUCHT : v > 0.2 ? REN : v > 0.015 || state.isMoving() ? HUP : STIL);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
