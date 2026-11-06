package nl.juiced.guhs.feature.vadsrijk.zone4.dieren;

import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ouwe Dobber, the giant turtle of the Lampionkust (T-Z4DIEREN). He swims his slow round past the Schildpadeiland
 * ({@link DobberRoute}: a pure function of the game time, so server and clients agree without position packets) and waits
 * 30 s at his halte next to the island's jetty. Players ride along on his shell: 8 seats (passengers, the robust choice in
 * multiplayer: no moving collision box to slide off or get pushed into). Safe by design:
 * <ul>
 *   <li>seats are 2,9-3,5 blocks above the water, so riders are never under water;</li>
 *   <li>sneak to get off any time: at the halte you land on the jetty, at sea in the water beside him (you can always click
 *       him again to climb back on);</li>
 *   <li>a player struggling in the water near him (air below half) is scooped onto his shell (or, when he is full, gets
 *       water breathing);</li>
 *   <li>he is never saved in a chunk ({@link #shouldBeSaved()}): {@link Z4DierenRegistry} keeps exactly one alive while
 *       someone is near, and logging out on his back puts you on the jetty.</li>
 * </ul>
 * Invulnerable, no AI, no collision.
 */
public class OuweDobber extends Entity implements GeoEntity {
    /** Dev time shift (ticks added to the game time), synced so the client swims the same place. */
    private static final EntityDataAccessor<Integer> DATA_VERSCHUIF = SynchedEntityData.defineId(OuweDobber.class, EntityDataSerializers.INT);
    /** Test mode: stays where it was put (gametests), waiting. */
    private static final EntityDataAccessor<Boolean> DATA_VAST = SynchedEntityData.defineId(OuweDobber.class, EntityDataSerializers.BOOLEAN);

    /** His origin lies this far under the water surface (the belly is in the water). */
    public static final double DIEPTE = 0.5;
    /** The water surface of the Lampionkust (the top water block is ZEE). */
    public static final double OPPERVLAK = nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken.ZEE + 1;
    /** The seats on his shell (x = side, y = up, z = forward; blocks, before turning with his heading). */
    static final Vec3[] ZITJES = {
            new Vec3(0, 3.5, 0.8), new Vec3(-1.3, 3.5, -1.6), new Vec3(1.3, 3.5, -1.6),
            new Vec3(-3.0, 3.0, 1.2), new Vec3(3.0, 3.0, 1.2), new Vec3(0, 3.0, -4.3),
            new Vec3(-2.0, 3.0, 4.2), new Vec3(2.0, 3.0, 4.2)};
    public static final int ZITPLAATSEN = ZITJES.length;
    /** Air below this (of 300) near him: he scoops you up. */
    static final int RED_LUCHT = 150;

    private static final RawAnimation ZWEM = RawAnimation.begin().thenLoop("zwem");
    private static final RawAnimation RUST = RawAnimation.begin().thenLoop("rust");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean wachtteVorige = true;

    public OuweDobber(EntityType<? extends OuweDobber> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VERSCHUIF, 0);
        builder.define(DATA_VAST, false);
    }

    public void setVerschuif(int ticks) {
        entityData.set(DATA_VERSCHUIF, ticks);
    }

    public void setVast(boolean vast) {
        entityData.set(DATA_VAST, vast);
    }

    public boolean vast() {
        return entityData.get(DATA_VAST);
    }

    /** The swim clock (game time + the dev shift). */
    public double tijd(float partial) {
        return level().getGameTime() + partial + entityData.get(DATA_VERSCHUIF);
    }

    public boolean wacht() {
        return vast() || DobberRoute.get().wacht(tijd(0));
    }

    /** Where he is at this time (the route point at the water, with a slow bob). */
    public static Vec3 plek(double tijd) {
        DobberRoute r = DobberRoute.get();
        Vec3 p = r.punt(r.afstand(tijd));
        double bob = Math.sin(tijd * 0.05) * 0.08;
        return new Vec3(p.x, OPPERVLAK - DIEPTE + bob, p.z);
    }

    public static float yaw(double tijd) {
        DobberRoute r = DobberRoute.get();
        return r.yaw(r.afstand(tijd));
    }

    @Override
    public void tick() {
        super.tick();
        if (!vast()) {
            double t = tijd(0);
            Vec3 p = plek(t);
            setPos(p.x, p.y, p.z);
            setYRot(yaw(t));
        }
        if (level().isClientSide()) {
            if (!wacht() && random.nextInt(3) == 0) {     // a little wake behind him
                Vec3 achter = Vec3.directionFromRotation(0, getYRot()).scale(-7.5);
                level().addParticle(ParticleTypes.SPLASH, getX() + achter.x + (random.nextDouble() - 0.5) * 4, getY() + DIEPTE + 0.1,
                        getZ() + achter.z + (random.nextDouble() - 0.5) * 4, 0, 0.05, 0);
            }
            return;
        }
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (!vast() && !Z4DierenRegistry.isOfficieel(this)) {
            for (Entity e : List.copyOf(getPassengers())) {
                e.stopRiding();
            }
            discard();
            return;
        }
        boolean nuWacht = wacht();
        if (nuWacht != wachtteVorige) {
            wachtteVorige = nuWacht;
            meld(sl, nuWacht ? (getPassengers().isEmpty() ? "vadsrijk.z4dieren.dobber.halte" : "vadsrijk.z4dieren.dobber.terug")
                    : "vadsrijk.z4dieren.dobber.vertrek");
            sl.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.TURTLE_AMBIENT_LAND, SoundSource.NEUTRAL, 1.6f, 0.55f);
        }
        if (tickCount % 10 == 0) {
            redden(sl);
        }
    }

    /** Overlay text for riders and players within 40 blocks. */
    private void meld(ServerLevel sl, String key) {
        for (ServerPlayer p : sl.players()) {
            if (p.getVehicle() == this || p.distanceToSqr(this) < 40 * 40) {
                p.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.AQUA));
            }
        }
    }

    /** A player struggling in the water near him is scooped onto his shell (full: water breathing instead). */
    private void redden(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (p.isPassenger() || p.isSpectator() || !p.isInWater() || p.getAirSupply() >= RED_LUCHT) {
                continue;
            }
            if (Math.abs(p.getX() - getX()) > 12 || Math.abs(p.getZ() - getZ()) > 12) {
                continue;
            }
            if (magErbij(getPassengers().size()) && p.startRiding(this, true, true)) {
                p.setAirSupply(p.getMaxAirSupply());
                p.sendOverlayMessage(Component.translatable("vadsrijk.z4dieren.dobber.gered").withStyle(ChatFormatting.AQUA));
            } else {
                p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, false, true));
            }
        }
    }

    /** Clients compute his place themselves: ignore the server's position packets. */
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 0) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            if (vast()) {
                setPos(position);
                setYRot(yRot % 360.0F);
            }
        }
    };

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    // --- riding -------------------------------------------------------------------------------------------------------------

    public static boolean magErbij(int aantal) {
        return aantal < ZITPLAATSEN;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.getVehicle() == this) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            if (!magErbij(getPassengers().size())) {
                sp.sendOverlayMessage(Component.translatable("vadsrijk.z4dieren.dobber.vol").withStyle(ChatFormatting.AQUA));
            } else if (sp.startRiding(this, false, true)) {
                sp.sendOverlayMessage(Component.translatable("vadsrijk.z4dieren.dobber.op").withStyle(ChatFormatting.AQUA));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && magErbij(getPassengers().size());
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int i = Math.max(0, getPassengers().indexOf(passenger)) % ZITPLAATSEN;
        return zitje(i, getYRot());
    }

    /** Seat i turned with his heading (relative to his origin). */
    public static Vec3 zitje(int i, float yaw) {
        return ZITJES[Math.floorMod(i, ZITPLAATSEN)].yRot(-yaw * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        float turn = Mth.wrapDegrees(getYRot() - yRotO);
        if (Math.abs(turn) < 20) {
            passenger.setYRot(passenger.getYRot() + turn);
            passenger.setYHeadRot(passenger.getYHeadRot() + turn);
        }
    }

    /** At the halte: onto the jetty; at sea: in the water beside him (never on top of his head, never under him). */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return afstapPlek(passenger);
    }

    public Vec3 afstapPlek(@Nullable Entity passenger) {
        if (wacht() && !vast() || position().distanceToSqr(DobberRoute.get().halte().add(0, getY(), 0)) < 8 * 8) {
            return new Vec3(Z4DierenPlekken.DOK_X + 0.5, Z4DierenPlekken.DOK_Y, Z4DierenPlekken.DOK_Z + 0.5);
        }
        Vec3 opzij = Vec3.directionFromRotation(0, getYRot() + 90).scale(7.5);
        if (passenger != null && passenger.position().subtract(position()).dot(opzij) < 0) {
            opzij = opzij.scale(-1);
        }
        return new Vec3(getX() + opzij.x, OPPERVLAK - 0.4, getZ() + opzij.z);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && passenger instanceof ServerPlayer sp && !isRemoved()) {
            boolean steiger = afstapPlek(sp).y >= Z4DierenPlekken.DOK_Y - 0.01;
            sp.resetFallDistance();
            sp.sendOverlayMessage(Component.translatable(steiger ? "vadsrijk.z4dieren.dobber.af_steiger" : "vadsrijk.z4dieren.dobber.af_water")
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(5, 1, 5);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    // --- GeckoLib -----------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<OuweDobber>("main", 20, state -> state.setAndContinue(state.animatable().wacht() ? RUST : ZWEM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
