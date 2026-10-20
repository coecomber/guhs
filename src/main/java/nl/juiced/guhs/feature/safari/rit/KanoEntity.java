package nl.juiced.guhs.feature.safari.rit;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.safari.Safari;

/**
 * The canoe of the village pond (S3, design ronde 28): the moored one ({@link #LIG}) lies at the jetty. A click is a
 * guided trip ({@link #RIT}): your own canoe paddles itself up the river (past the hippos, otters and crocodiles) and back
 * to the jetty. Sneak + click is a free canoe ({@link #VRIJ}): you paddle it yourself (W/S, A/D), on the water only, and
 * it is gone when you step out. One guh may ride along in the bow. Never saved.
 */
public class KanoEntity extends Entity implements GeoEntity {
    public static final int LIG = 0, RIT = 1, VRIJ = 2;
    public static final double RIT_SNELHEID = 0.17, VRIJ_TOP = 0.2;
    public static final int INSTAPPEN = 40;
    private static final EntityDataAccessor<Integer> DATA_SOORT = SynchedEntityData.defineId(KanoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_START = SynchedEntityData.defineId(KanoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_VAART = SynchedEntityData.defineId(KanoEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private double snelheid;
    private int leeg;

    public KanoEntity(EntityType<? extends KanoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SOORT, LIG);
        builder.define(DATA_START, 0);
        builder.define(DATA_VAART, 0f);
    }

    public int soort() {
        return entityData.get(DATA_SOORT);
    }

    public void zetSoort(int soort) {
        entityData.set(DATA_SOORT, soort);
    }

    /** How fast it moves now (blocks a tick; the paddle animation). */
    public float vaart() {
        return entityData.get(DATA_VAART);
    }

    /** A guided trip from the jetty, leaving after {@link #INSTAPPEN} ticks. */
    public void begin(long nu) {
        zetSoort(RIT);
        entityData.set(DATA_START, (int) (nu + INSTAPPEN));
        ritPlaats(nu, true);
    }

    private double afstand(long t) {
        return Math.max(0, t - (long) entityData.get(DATA_START)) * RIT_SNELHEID;
    }

    private void ritPlaats(long t, boolean snap) {
        Pad pad = RitData.get().kano;
        if (pad == null) {
            return;
        }
        double a = afstand(t);
        Vec3 p = pad.op(a);
        float yaw = pad.richting(a, getYRot());
        setPos(p);
        if (snap) {
            xo = xOld = p.x;
            yo = yOld = p.y;
            zo = zOld = p.z;
            setYRot(yaw);
            yRotO = yaw;
        } else {
            setYRot(Mth.approachDegrees(getYRot(), yaw, 4f));
        }
    }

    @Override
    public void tick() {
        super.tick();
        int soort = soort();
        if (soort == RIT) {
            long t = level().getGameTime();
            ritPlaats(t, false);
            Pad pad = RitData.get().kano;
            if (level().isClientSide()) {
                if (afstand(t) > 0 && random.nextInt(5) == 0) {
                    spatten();
                }
                return;
            }
            entityData.set(DATA_VAART, afstand(t) > 0 ? (float) RIT_SNELHEID : 0f);
            if (tickCount % 30 == 0 && afstand(t) > 0) {
                level().playSound(null, this, nl.juiced.guhs.feature.safari.spullen.SafariGeluid.KANO.get(), SoundSource.NEUTRAL, 0.6f, 0.85f + random.nextFloat() * 0.3f);
            }
            if (pad == null || afstand(t) >= pad.lengte() - 0.01) {
                eindig();
            } else if (getPassengers().stream().noneMatch(e -> e instanceof Player) && ++leeg > 60) {
                discard();
            }
        } else if (soort == VRIJ) {
            if (level().isClientSide()) {
                interpolation.interpolate();
                if (Math.abs(vaart()) > 0.03 && random.nextInt(4) == 0) {
                    spatten();
                }
                return;
            }
            float vooruit = 0, stuur = 0;
            if (getFirstPassenger() instanceof ServerPlayer rijder) {
                Input in = rijder.getLastClientInput();
                vooruit = (in.forward() ? 1 : 0) - (in.backward() ? 1 : 0);
                stuur = (in.right() ? 1 : 0) - (in.left() ? 1 : 0);
                leeg = 0;
            } else if (++leeg > 100) {
                poef();
                return;
            }
            peddel(vooruit, stuur);
        }
    }

    /** One tick of free paddling: speeds up, turns, and only moves where there is open water. */
    public void peddel(float vooruit, float stuur) {
        if (vooruit > 0.1f) {
            snelheid = Math.min(VRIJ_TOP, snelheid + 0.01);
        } else if (vooruit < -0.1f) {
            snelheid = Math.max(-VRIJ_TOP * 0.4, snelheid - 0.01);
        } else {
            snelheid *= 0.9;
        }
        if (stuur != 0) {
            setYRot(getYRot() + stuur * (Math.abs(snelheid) > 0.02 ? 3.5f : 2f));
        }
        Vec3 r = Vec3.directionFromRotation(0, getYRot()).scale(snelheid);
        Vec3 doel = position().add(r);
        if (openWater(doel)) {
            setPos(doel.x, Safari.WATER + 0.6, doel.z);
        } else {
            snelheid = 0;
        }
        entityData.set(DATA_VAART, (float) snelheid);
        if (tickCount % 34 == 0 && Math.abs(snelheid) > 0.03) {
            level().playSound(null, this, nl.juiced.guhs.feature.safari.spullen.SafariGeluid.KANO.get(), SoundSource.NEUTRAL, 0.5f, 0.9f);
        }
    }

    private boolean openWater(Vec3 p) {
        Level level = level();
        for (double dx : new double[]{-0.6, 0.6}) {
            for (double dz : new double[]{-0.6, 0.6}) {
                BlockPos b = BlockPos.containing(p.x + dx, Safari.WATER, p.z + dz);
                if (!level.getFluidState(b).is(FluidTags.WATER) || !level.getBlockState(b.above()).getCollisionShape(level, b.above()).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void spatten() {
        Vec3 opzij = Vec3.directionFromRotation(0, getYRot() + 90).scale(0.7 * (random.nextBoolean() ? 1 : -1));
        level().addParticle(ParticleTypes.SPLASH, getX() + opzij.x, getY() + 0.2, getZ() + opzij.z, 0, 0.05, 0);
    }

    /** The guided trip is over: everybody out on the jetty, the canoe is gone. */
    private void eindig() {
        Vec3 uit = RitData.get().kanoStart;
        for (Entity e : java.util.List.copyOf(getPassengers())) {
            e.stopRiding();
            if (uit != null) {
                e.teleportTo(uit.x, uit.y + 1.0, uit.z);
            }
            if (e instanceof ServerPlayer p) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.safari.kano.terug"));
            }
        }
        poef();
    }

    private void poef() {
        if (level() instanceof ServerLevel s) {
            s.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 16, 0.8, 0.1, 0.8, 0.05);
        }
        ejectPassengers();
        discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer p) {
            if (soort() == LIG) {
                Ritten.huurKano(p, this, p.isShiftKeyDown());
            } else if (getPassengers().stream().noneMatch(e -> e instanceof Player) && soort() == VRIJ) {
                p.startRiding(this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (soort() == LIG) {
            return false;
        }
        if (passenger instanceof GuhEntity) {
            return getPassengers().stream().noneMatch(e -> e instanceof GuhEntity);
        }
        return passenger instanceof Player && getPassengers().stream().noneMatch(e -> e instanceof Player);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        double z = passenger instanceof GuhEntity ? 0.85 : -0.35;
        return new Vec3(0, -0.15, z).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        float draai = Mth.wrapDegrees(getYRot() - yRotO);
        if (passenger instanceof Player && Math.abs(draai) < 45) {
            passenger.setYRot(passenger.getYRot() + draai);
            passenger.setYHeadRot(passenger.getYHeadRot() + draai);
        } else if (!(passenger instanceof Player)) {
            passenger.setYRot(getYRot());
        }
    }

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            if (soort() != RIT) {
                super.interpolateTo(position, yRot, xRot);
            }
        }
    };

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
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
    public boolean canBeCollidedWith(@Nullable Entity other) {
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
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    private static final RawAnimation PEDDEL = RawAnimation.begin().thenLoop("animation.kano.peddel");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("peddel", 4, state -> Math.abs(vaart()) > 0.02 ? state.setAndContinue(PEDDEL) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
