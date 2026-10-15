package nl.juiced.guhs.feature.guhrio;

import javax.annotation.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A falling block: a deck like a platform (1 to 4 blocks along the lane) that stays where it is until a player of the
 * level stands on it. Then it trembles for a moment and drops out of the level, and is back on its spot a few seconds
 * later. You stand on it like on a block. Never saved, never hurt.
 */
public class ValblokEntity extends LevelWezen {
    public static final int RUST = 0, TRILT = 1, VALT = 2;
    public static final int TRIL_TICKS = 14, WEG_TICKS = 70;
    public static final float DIK = 0.5f;
    private static final EntityDataAccessor<Integer> DATA_STAND = SynchedEntityData.defineId(ValblokEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BREED = SynchedEntityData.defineId(ValblokEntity.class, EntityDataSerializers.INT);

    private int ticks;
    @Nullable
    private Vec3 rustplek;

    public ValblokEntity(EntityType<? extends ValblokEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STAND, RUST);
        builder.define(DATA_BREED, 2);
    }

    /** How wide it is, which way the lane runs and where it rests (its feet: the middle of the deck's underside). */
    public void zetVorm(int breed, net.minecraft.core.Direction langs, Vec3 rustplek) {
        this.entityData.set(DATA_BREED, Math.max(1, Math.min(4, breed)) | (langs.get2DDataValue() & 3) << 3);
        this.rustplek = rustplek;
        this.refreshDimensions();
        this.setPos(rustplek.x, rustplek.y, rustplek.z);
    }

    public int breed() {
        return this.entityData.get(DATA_BREED) & 7;
    }

    public net.minecraft.core.Direction langs() {
        return net.minecraft.core.Direction.from2DDataValue(this.entityData.get(DATA_BREED) >> 3 & 3);
    }

    public int stand() {
        return this.entityData.get(DATA_STAND);
    }

    private void zetStand(int stand) {
        this.entityData.set(DATA_STAND, stand);
        ticks = 0;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(Math.max(1, breed()), DIK);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_BREED.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    @Override
    protected void serverTick(ServerLevel level) {
        ticks++;
        switch (stand()) {
            case RUST -> {
                this.setDeltaMovement(Vec3.ZERO);
                if (iemandErop()) {
                    zetStand(TRILT);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.GRAVEL_HIT, SoundSource.NEUTRAL, 0.7f, 0.8f);
                }
            }
            case TRILT -> {
                if (ticks > TRIL_TICKS) {
                    zetStand(VALT);
                }
            }
            default -> {
                double vy = Math.max(-0.9, this.getDeltaMovement().y - 0.06);
                this.setDeltaMovement(0, vy, 0);
                this.setPos(getX(), getY() + vy, getZ());
                if (baan == null ? ticks > 60 : getY() < baan.onder - 3) {
                    wegVoor(WEG_TICKS);
                }
            }
        }
    }

    private boolean iemandErop() {
        AABB boven = this.getBoundingBox().move(0, DIK, 0).inflate(-0.05, 0.12, -0.05);
        for (ServerPlayer p : spelers()) {
            AABB voeten = p.getBoundingBox();
            if (voeten.minY >= boven.minY - 0.1 && voeten.minY <= boven.maxY && voeten.maxX > boven.minX && voeten.minX < boven.maxX
                    && voeten.maxZ > boven.minZ && voeten.minZ < boven.maxZ) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void terugThuis() {
        if (rustplek != null) {
            this.setPos(rustplek.x, rustplek.y, rustplek.z);
        }
        this.setDeltaMovement(Vec3.ZERO);
        zetStand(RUST);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return !weg();
    }

    // --- GuhrioWezen -----------------------------------------------------------------------------------------------------

    @Override
    public boolean draagt() {
        return !weg();
    }

    @Override
    public boolean stampbaar() {
        return false;
    }

    @Override
    public boolean gevaarlijk() {
        return false;
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
    }
}
