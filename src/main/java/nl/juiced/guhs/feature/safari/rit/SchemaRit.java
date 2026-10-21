package nl.juiced.guhs.feature.safari.rit;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * A ride that follows the timetable ({@link RitSchema}): the truck and the gondola. Its place is pure arithmetic on the
 * game time, the same on the server and on every client, so it glides without position packets (the client ignores the
 * server's positions). It is never saved: {@link Ritten} keeps the right number of them in the loaded part of the park
 * and makes them again after a restart. Passengers: players and (in their own seats) the players' own guhs.
 */
public abstract class SchemaRit extends Entity implements GeoEntity {
    protected static final EntityDataAccessor<Integer> DATA_K = SynchedEntityData.defineId(SchemaRit.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_N = SynchedEntityData.defineId(SchemaRit.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected SchemaRit(EntityType<? extends SchemaRit> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_K, 0);
        builder.define(DATA_N, 1);
    }

    public int k() {
        return entityData.get(DATA_K);
    }

    public int n() {
        return Math.max(1, entityData.get(DATA_N));
    }

    public void zetSchema(int k, int n) {
        entityData.set(DATA_K, k);
        entityData.set(DATA_N, n);
        plaats(level().getGameTime(), true);
    }

    /** Puts the ride where the timetable says it is at game time t (snap: also the previous position/rotation). */
    protected abstract void plaats(long t, boolean snap);

    protected void zet(Vec3 p, float yaw, boolean snap) {
        setPos(p);
        if (snap) {
            xo = xOld = p.x;
            yo = yOld = p.y;
            zo = zOld = p.z;
            setYRot(yaw);
            yRotO = yaw;
        } else {
            setYRot(Mth.approachDegrees(getYRot(), yaw, 4.0f));
        }
    }

    /** Gametests: a ride that stays where it is put (off its timetable). */
    private boolean vast;

    public void zetVast() {
        vast = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!vast) {
            plaats(level().getGameTime(), false);
        }
    }

    // --- passengers ---------------------------------------------------------------------------------------------------------

    public long spelers() {
        return getPassengers().stream().filter(e -> e instanceof Player).count();
    }

    public long guhs() {
        return getPassengers().stream().filter(e -> e instanceof GuhEntity).count();
    }

    /** The player's own guhs that follow him (tame, not told to sit), close by. */
    public static List<GuhEntity> volgers(ServerPlayer p, double r) {
        UUID id = p.getUUID();
        return p.level().getEntitiesOfClass(GuhEntity.class, p.getBoundingBox().inflate(r),
                g -> g.isTame() && id.equals(g.getOwnerUUID()) && !g.isOrderedToSit() && !g.isPassenger() && g.isAlive());
    }

    /** Takes p's following guhs aboard (as many as there are guh seats). */
    public void neemGuhsMee(ServerPlayer p, int max) {
        for (GuhEntity g : volgers(p, 14)) {
            if (guhs() >= max) {
                break;
            }
            g.startRiding(this, true, true);
        }
    }

    /** Puts the guhs of this player down next to him (he got off). */
    public void zetGuhsAf(Player p) {
        for (Entity e : List.copyOf(getPassengers())) {
            if (e instanceof GuhEntity g && p.getUUID().equals(g.getOwnerUUID())) {
                g.stopRiding();
                g.teleportTo(p.getX() + (random.nextDouble() - 0.5), p.getY(), p.getZ() + (random.nextDouble() - 0.5));
            }
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof Player) {
            float draai = Mth.wrapDegrees(getYRot() - yRotO);
            if (Math.abs(draai) < 45) {
                passenger.setYRot(passenger.getYRot() + draai);
                passenger.setYHeadRot(passenger.getYHeadRot() + draai);
            }
        } else {
            passenger.setYRot(getYRot());
            if (passenger instanceof GuhEntity g) {
                g.yBodyRot = getYRot();
                g.setYHeadRot(getYRot());
            }
        }
    }

    /** A seat in model pixels (x, y = seat surface, z; the model's front is -z) as an offset in the world. */
    protected Vec3 stoel(double x, double y, double z) {
        return new Vec3(-x / 16.0, y / 16.0, -z / 16.0).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    // --- an entity that is only a ride ---------------------------------------------------------------------------------------

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 0) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            // the timetable puts it in place on this side too
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
